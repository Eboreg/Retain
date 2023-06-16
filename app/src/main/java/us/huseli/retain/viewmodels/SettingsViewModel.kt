package us.huseli.retain.viewmodels

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jcraft.jsch.JSch
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.toImmutableList
import org.jsoup.Jsoup
import us.huseli.retain.Constants.DEFAULT_MIN_COLUMN_WIDTH
import us.huseli.retain.Constants.NEXTCLOUD_BASE_DIR
import us.huseli.retain.Constants.PREF_MIN_COLUMN_WIDTH
import us.huseli.retain.Constants.PREF_NEXTCLOUD_BASE_DIR
import us.huseli.retain.Constants.PREF_NEXTCLOUD_PASSWORD
import us.huseli.retain.Constants.PREF_NEXTCLOUD_URI
import us.huseli.retain.Constants.PREF_NEXTCLOUD_USERNAME
import us.huseli.retain.Constants.PREF_SFTP_BASE_DIR
import us.huseli.retain.Constants.PREF_SFTP_HOSTNAME
import us.huseli.retain.Constants.PREF_SFTP_PASSWORD
import us.huseli.retain.Constants.PREF_SFTP_PORT
import us.huseli.retain.Constants.PREF_SFTP_USERNAME
import us.huseli.retain.Constants.PREF_SYNC_BACKEND
import us.huseli.retain.Constants.SFTP_BASE_DIR
import us.huseli.retain.Enums.NoteType
import us.huseli.retain.Enums.SyncBackend
import us.huseli.retain.LogInterface
import us.huseli.retain.Logger
import us.huseli.retain.copyFileToLocal
import us.huseli.retain.data.NoteRepository
import us.huseli.retain.data.SyncBackendRepository
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.Image
import us.huseli.retain.data.entities.Note
import us.huseli.retain.data.entities.NoteCombo
import us.huseli.retain.extractFileFromZip
import us.huseli.retain.isImageFile
import us.huseli.retain.readTextFileFromZip
import us.huseli.retain.syncbackend.NextCloudEngine
import us.huseli.retain.syncbackend.SFTPEngine
import us.huseli.retain.syncbackend.tasks.TaskResult
import us.huseli.retain.syncbackend.tasks.TestTaskResult
import us.huseli.retain.uriToImage
import java.io.File
import java.time.Instant
import java.util.zip.ZipFile
import javax.inject.Inject

data class QuickNoteTodoList(
    val todo: Collection<String>? = null,
    val done: Collection<String>? = null,
)

data class QuickNoteEntry(
    val creation_date: Long? = null,
    val title: String? = null,
    val last_modification_date: Long? = null,
    val color: String? = null,
    val todolists: Collection<QuickNoteTodoList>? = null,
)

data class GoogleNoteListContent(
    val text: String,
    val isChecked: Boolean,
)

data class GoogleNoteAttachment(
    val filePath: String,
    val mimetype: String,
)

data class GoogleNoteEntry(
    val attachments: List<GoogleNoteAttachment>? = null,
    val color: String? = null,
    val isTrashed: Boolean = false,
    val isArchived: Boolean = false,
    val listContent: List<GoogleNoteListContent>? = null,
    val title: String? = null,
    val userEditedTimestampUsec: Long? = null,
    val createdTimestampUsec: Long? = null,
    val textContent: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: NoteRepository,
    private val syncBackendRepository: SyncBackendRepository,
    private val sftpEngine: SFTPEngine,
    private val nextCloudEngine: NextCloudEngine,
    override val logger: Logger,
) : ViewModel(), SharedPreferences.OnSharedPreferenceChangeListener, LogInterface {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private var importJob: Job? = null
    private val sectionsShown = mutableMapOf<String, MutableState<Boolean>>()
    private var syncBackendDataChanged = false
    private val jsch = JSch()
    private val knownHostsFile = File(context.filesDir, "known_hosts")
    private var originalSyncBackend = preferences.getString(PREF_SYNC_BACKEND, null)?.let { SyncBackend.valueOf(it) }

    val minColumnWidth = MutableStateFlow(preferences.getInt(PREF_MIN_COLUMN_WIDTH, DEFAULT_MIN_COLUMN_WIDTH))
    val syncBackend = MutableStateFlow(originalSyncBackend)

    val importActionCount = MutableStateFlow<Int?>(null)
    val importCurrentAction = MutableStateFlow("")
    val importCurrentActionIndex = MutableStateFlow(0)
    val keepImportIsActive = MutableStateFlow(false)
    val quickNoteImportIsActive = MutableStateFlow(false)
    val syncBackendNeedsTesting = syncBackendRepository.needsTesting.asStateFlow()
    val isSyncBackendEnabled = syncBackend.map { it != null && it != SyncBackend.NONE }

    val isNextCloudAuthError = MutableStateFlow(false)
    val isNextCloudTesting = nextCloudEngine.isTesting
    val isNextCloudUrlError = MutableStateFlow(false)
    val isNextCloudWorking = MutableStateFlow<Boolean?>(null)
    val nextCloudBaseDir = MutableStateFlow(
        preferences.getString(PREF_NEXTCLOUD_BASE_DIR, NEXTCLOUD_BASE_DIR) ?: NEXTCLOUD_BASE_DIR
    )
    val nextCloudPassword = MutableStateFlow(preferences.getString(PREF_NEXTCLOUD_PASSWORD, "") ?: "")
    val nextCloudUri = MutableStateFlow(preferences.getString(PREF_NEXTCLOUD_URI, "") ?: "")
    val nextCloudUsername = MutableStateFlow(preferences.getString(PREF_NEXTCLOUD_USERNAME, "") ?: "")

    val isSFTPTesting = sftpEngine.isTesting
    val isSFTPWorking = MutableStateFlow<Boolean?>(null)
    val sftpHostname = MutableStateFlow(preferences.getString(PREF_SFTP_HOSTNAME, "") ?: "")
    val sftpPassword = MutableStateFlow(preferences.getString(PREF_SFTP_PASSWORD, "") ?: "")
    val sftpPort = MutableStateFlow(preferences.getInt(PREF_SFTP_PORT, 22))
    val sftpUsername = MutableStateFlow(preferences.getString(PREF_SFTP_USERNAME, "") ?: "")
    val sftpBaseDir = MutableStateFlow(preferences.getString(PREF_SFTP_BASE_DIR, SFTP_BASE_DIR) ?: SFTP_BASE_DIR)
    val sftpPromptYesNo = sftpEngine.promptYesNo

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
        jsch.setKnownHosts(knownHostsFile.absolutePath)
    }

    fun approveSFTPKey() = sftpEngine.approveKey()

    fun cancelImport() {
        importJob?.cancel()
        quickNoteImportIsActive.value = false
        keepImportIsActive.value = false
    }

    fun denySFTPKey() = sftpEngine.denyKey()

    fun getSectionShown(key: String, default: Boolean): State<Boolean> {
        return sectionsShown[key] ?: mutableStateOf(default).also { sectionsShown[key] = it }
    }

    @Suppress("destructure")
    fun keepImport(zipUri: Uri, context: Context) {
        keepImportIsActive.value = true
        importCurrentActionIndex.value = 0
        importCurrentAction.value = ""

        importJob = viewModelScope.launch {
            try {
                val tempDir = File(context.cacheDir, "keep").apply { mkdirs() }
                val keepFile = File(
                    tempDir,
                    zipUri.lastPathSegment?.substringAfterLast('/') ?: "keep.zip"
                ).apply { deleteOnExit() }

                copyFileToLocal(context, zipUri, keepFile)

                val gson: Gson = GsonBuilder().create()
                val zipFile = withContext(Dispatchers.IO) { ZipFile(keepFile) }
                val entries = mutableListOf<GoogleNoteEntry>()
                val combos = mutableListOf<NoteCombo>()
                val startPosition = repository.getMaxNotePosition() + 1
                var noteCount = 0
                var imageCount = 0

                zipFile.entries().iterator().forEach { zipEntry ->
                    zipEntry?.let {
                        if (it.name.startsWith("Takeout/Keep/")) {
                            if (it.name.endsWith(".json")) noteCount++
                            else if (isImageFile(it.name)) imageCount++
                        }
                    }
                }

                importActionCount.value = ((noteCount + imageCount) * 2) + if (noteCount > 0) 1 else 0

                zipFile.entries().iterator().forEach { zipEntry ->
                    zipEntry?.let {
                        if (zipEntry.name.startsWith("Takeout/Keep/")) {
                            if (zipEntry.name.endsWith(".json")) {
                                updateCurrentAction("Extracting ${zipEntry.name}")
                                val json = readTextFileFromZip(zipFile, zipEntry)
                                gson.fromJson(json, GoogleNoteEntry::class.java)?.let {
                                    if (!it.isTrashed) entries.add(it)
                                }
                            } else if (isImageFile(zipEntry.name)) {
                                updateCurrentAction("Extracting ${zipEntry.name}")
                                val imageFile =
                                    File(tempDir, zipEntry.name.substringAfterLast('/')).apply { deleteOnExit() }
                                extractFileFromZip(zipFile, zipEntry, imageFile)
                            }
                        }
                    }
                }

                entries.forEachIndexed { noteIndex, noteEntry ->
                    updateCurrentAction(
                        if (noteEntry.title != null) "Converting note: ${noteEntry.title}" else "Converting note"
                    )

                    val note = Note(
                        color = noteEntry.color ?: "DEFAULT",
                        title = noteEntry.title ?: "",
                        text = noteEntry.textContent ?: "",
                        type = if (noteEntry.listContent != null) NoteType.CHECKLIST else NoteType.TEXT,
                        isArchived = noteEntry.isArchived,
                        created = noteEntry.createdTimestampUsec?.let { Instant.ofEpochMilli(it / 1000) }
                                  ?: Instant.now(),
                        updated = noteEntry.userEditedTimestampUsec?.let { Instant.ofEpochMilli(it / 1000) }
                                  ?: Instant.now(),
                        position = startPosition + noteIndex,
                    )
                    val checklistItems = noteEntry.listContent?.mapIndexed { checklistItemIndex, checklistItemEntry ->
                        ChecklistItem(
                            text = checklistItemEntry.text,
                            checked = checklistItemEntry.isChecked,
                            noteId = note.id,
                            position = checklistItemIndex,
                        )
                    }

                    val images = noteEntry.attachments?.mapIndexedNotNull { imageIndex, imageEntry ->
                        updateCurrentAction("Moving ${imageEntry.filePath}")
                        val imageFile = File(tempDir, imageEntry.filePath)
                        if (imageFile.exists()) {
                            uriToImage(context, imageFile.toUri(), note.id)?.copy(position = imageIndex)
                        } else null
                    }

                    combos.add(
                        NoteCombo(
                            note = note,
                            checklistItems = checklistItems ?: emptyList(),
                            images = images ?: emptyList()
                        )
                    )
                }

                if (combos.isNotEmpty()) {
                    updateCurrentAction("Saving to database")
                    repository.insertCombos(combos)
                }
                log("Imported ${combos.size} notes.", showInSnackbar = true)
            } catch (e: Exception) {
                log("Error: $e", level = Log.ERROR, showInSnackbar = true)
            } finally {
                keepImportIsActive.value = false
            }
        }
    }

    fun quickNoteImport(zipUri: Uri, context: Context) {
        quickNoteImportIsActive.value = true
        importCurrentActionIndex.value = 0
        importCurrentAction.value = ""

        viewModelScope.launch {
            try {
                val tempDir = File(context.cacheDir, "quicknote").apply { mkdirs() }
                val quickNoteFile = File(
                    tempDir,
                    zipUri.lastPathSegment?.substringAfterLast('/') ?: "quicknote.zip"
                ).apply { deleteOnExit() }

                updateCurrentAction("Copying and opening ${quickNoteFile.name}")
                copyFileToLocal(context, zipUri, quickNoteFile)

                val zipFile = withContext(Dispatchers.IO) { ZipFile(quickNoteFile) }
                val combos = mutableListOf<NoteCombo>()
                val sqdDirs = mutableListOf<String>()
                val sqdDirRegex = Regex("^.*\\.sqd/$")
                var notePosition = repository.getMaxNotePosition() + 1

                zipFile.entries().iterator().forEach { zipEntry ->
                    if (zipEntry != null) {
                        if (zipEntry.name.endsWith(".sqd") && !zipEntry.isDirectory) {
                            val sqdFile = File(tempDir, zipEntry.name.substringAfterLast('/'))
                            updateCurrentAction("Extracting ${sqdFile.name}")
                            extractFileFromZip(zipFile, zipEntry, sqdFile)
                            extractQuickNoteSqd(sqdFile, context, notePosition)?.let {
                                combos.add(it)
                                notePosition++
                            }
                        } else sqdDirRegex.find(zipEntry.name)?.let { result ->
                            result.groups[0]?.value?.let { dirname -> sqdDirs.add(dirname) }
                        }
                    }
                }

                sqdDirs.forEach { dirname ->
                    extractQuickNoteSqd(quickNoteFile, context, notePosition, dirname)?.let {
                        combos.add(it)
                        notePosition++
                    }
                }

                if (combos.isNotEmpty()) {
                    updateCurrentAction("Saving to database")
                    repository.insertCombos(combos)
                }
                log("Imported ${combos.size} notes.", showInSnackbar = true)
            } catch (e: Exception) {
                log("Error: $e", level = Log.ERROR, showInSnackbar = true)
            } finally {
                quickNoteImportIsActive.value = false
            }
        }
    }

    fun save() {
        preferences.edit()
            .putString(PREF_NEXTCLOUD_URI, nextCloudUri.value)
            .putString(PREF_NEXTCLOUD_USERNAME, nextCloudUsername.value)
            .putString(PREF_NEXTCLOUD_PASSWORD, nextCloudPassword.value)
            .putString(PREF_NEXTCLOUD_BASE_DIR, nextCloudBaseDir.value)
            .putInt(PREF_MIN_COLUMN_WIDTH, minColumnWidth.value)
            .putString(PREF_SYNC_BACKEND, syncBackend.value?.name)
            .putString(PREF_SFTP_HOSTNAME, sftpHostname.value)
            .putInt(PREF_SFTP_PORT, sftpPort.value)
            .putString(PREF_SFTP_USERNAME, sftpUsername.value)
            .putString(PREF_SFTP_PASSWORD, sftpPassword.value)
            .putString(PREF_SFTP_BASE_DIR, sftpBaseDir.value)
            .apply()
        if (syncBackendDataChanged) {
            syncBackendRepository.needsTesting.value = true
            syncBackendDataChanged = false
        }
        if (!listOf(SyncBackend.NONE, originalSyncBackend, null).contains(syncBackend.value)) {
            syncBackendRepository.needsTesting.value = true
            originalSyncBackend = syncBackend.value
            viewModelScope.launch {
                syncBackendRepository.sync()
            }
        }
    }

    fun testSyncBackend(onResult: (TestTaskResult) -> Unit) {
        when (syncBackend.value) {
            SyncBackend.NEXTCLOUD -> testNextCloud(onResult)
            SyncBackend.SFTP -> testSFTP(onResult)
            else -> {}
        }
    }

    fun testNextCloud(onResult: (TestTaskResult) -> Unit) = viewModelScope.launch {
        if (syncBackend.value == SyncBackend.NEXTCLOUD) {
            syncBackendRepository.needsTesting.value = false
            nextCloudEngine.test(
                Uri.parse(nextCloudUri.value),
                nextCloudUsername.value,
                nextCloudPassword.value,
                nextCloudBaseDir.value,
            ) { result ->
                isNextCloudWorking.value = result.success
                isNextCloudUrlError.value =
                    result.status == TaskResult.Status.UNKNOWN_HOST || result.status == TaskResult.Status.CONNECT_ERROR
                isNextCloudAuthError.value = result.status == TaskResult.Status.AUTH_ERROR
                onResult(result)
            }
        }
    }

    fun testSFTP(onResult: (TestTaskResult) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        if (syncBackend.value == SyncBackend.SFTP) {
            syncBackendRepository.needsTesting.value = false
            sftpEngine.test(
                sftpHostname.value,
                sftpUsername.value,
                sftpPassword.value,
                sftpBaseDir.value,
            ) { result ->
                isSFTPWorking.value = result.success
                onResult(result)
            }
        }
    }

    fun toggleSectionShown(key: String) {
        sectionsShown.getOrPut(key) { mutableStateOf(true) }.apply { value = !value }
    }

    fun updateField(field: String, value: Any) {
        when (field) {
            PREF_NEXTCLOUD_URI -> {
                if (value != nextCloudUri.value) {
                    nextCloudUri.value = value as String
                    syncBackendDataChanged = true
                    resetNextCloudStatus()
                }
            }
            PREF_NEXTCLOUD_USERNAME -> {
                if (value != nextCloudUsername.value) {
                    nextCloudUsername.value = value as String
                    syncBackendDataChanged = true
                    resetNextCloudStatus()
                }
            }
            PREF_NEXTCLOUD_PASSWORD -> {
                if (value != nextCloudPassword.value) {
                    nextCloudPassword.value = value as String
                    syncBackendDataChanged = true
                    resetNextCloudStatus()
                }
            }
            PREF_MIN_COLUMN_WIDTH -> minColumnWidth.value = value as Int
            PREF_NEXTCLOUD_BASE_DIR -> nextCloudBaseDir.value = value as String
            PREF_SFTP_BASE_DIR -> {
                if (value != sftpBaseDir.value) {
                    sftpBaseDir.value = value as String
                    syncBackendDataChanged = true
                    resetSFTPStatus()
                }
            }
            PREF_SFTP_HOSTNAME -> {
                if (value != sftpHostname.value) {
                    sftpHostname.value = value as String
                    syncBackendDataChanged = true
                    resetSFTPStatus()
                }
            }
            PREF_SFTP_PASSWORD -> {
                if (value != sftpPassword.value) {
                    sftpPassword.value = value as String
                    syncBackendDataChanged = true
                    resetSFTPStatus()
                }
            }
            PREF_SFTP_PORT -> {
                if (value != sftpPort.value) {
                    sftpPort.value = value as Int
                    syncBackendDataChanged = true
                    resetSFTPStatus()
                }
            }
            PREF_SFTP_USERNAME -> {
                if (value != sftpUsername.value) {
                    sftpUsername.value = value as String
                    syncBackendDataChanged = true
                    resetSFTPStatus()
                }
            }
            PREF_SYNC_BACKEND -> {
                if (value != syncBackend.value) {
                    syncBackend.value = value as SyncBackend
                    preferences.edit().putString(PREF_SYNC_BACKEND, syncBackend.value?.name).apply()
                }
            }
        }
    }

    @Suppress("SameReturnValue")
    private suspend fun extractQuickNoteSqd(
        file: File,
        context: Context,
        notePosition: Int,
        baseDir: String = ""
    ): NoteCombo? {
        val zipFile = withContext(Dispatchers.IO) { ZipFile(file) }
        var quickNoteEntry: QuickNoteEntry? = null
        var text = ""
        val gson: Gson = GsonBuilder().create()

        zipFile.getEntry("${baseDir}metadata.json")?.let { zipEntry ->
            updateCurrentAction("Parsing ${zipEntry.name}")
            val json = readTextFileFromZip(zipFile, zipEntry)
            gson.fromJson(json, QuickNoteEntry::class.java)?.let {
                quickNoteEntry = it
                log(quickNoteEntry.toString())
            }
        }

        // red none green yellow blue orange teal purple violet pink
        @Suppress("destructure")
        quickNoteEntry?.let { entry ->
            zipFile.getEntry("${baseDir}index.html")?.let { zipEntry ->
                updateCurrentAction("Parsing ${zipEntry.name}")
                val html = readTextFileFromZip(zipFile, zipEntry)
                val doc = Jsoup.parseBodyFragment(html)
                text = doc.body().wholeText().trim().replace("\n\n", "\n")
            }

            val checklistItems = mutableListOf<ChecklistItem>()
            val images = mutableListOf<Image>()
            var imagePosition = 0
            var checklistItemPosition = 0
            val title =
                entry.title
                ?: (if (baseDir.isNotBlank()) Regex("(?:.*/)?([^/.]+?)(?:\\.sqd)?/?$").find(baseDir)?.groupValues?.last() else null)
                ?: file.nameWithoutExtension

            updateCurrentAction("Converting note: $title")

            val note = Note(
                type = if (entry.todolists?.isNotEmpty() == true) NoteType.CHECKLIST else NoteType.TEXT,
                title = title,
                text = text,
                created = if (entry.creation_date != null) Instant.ofEpochMilli(entry.creation_date) else Instant.now(),
                updated = if (entry.last_modification_date != null) Instant.ofEpochMilli(entry.last_modification_date) else Instant.now(),
                position = notePosition,
                color = when (entry.color) {
                    "red" -> "RED"
                    "green" -> "GREEN"
                    "yellow" -> "YELLOW"
                    "blue" -> "BLUE"
                    "orange" -> "ORANGE"
                    "teal" -> "TEAL"
                    "purple" -> "PURPLE"
                    "violet" -> "PURPLE"
                    "pink" -> "PINK"
                    else -> "DEFAULT"
                },
            )

            entry.todolists?.flatMap { it.todo ?: emptyList() }?.forEach {
                checklistItems.add(ChecklistItem(text = it, noteId = note.id, position = checklistItemPosition++))
            }
            entry.todolists?.flatMap { it.done ?: emptyList() }?.forEach {
                checklistItems.add(
                    ChecklistItem(text = it, checked = true, noteId = note.id, position = checklistItemPosition++)
                )
            }

            zipFile.entries().iterator().forEach { zipEntry ->
                if (zipEntry != null && zipEntry.name.startsWith("${baseDir}data/") && isImageFile(zipEntry.name)) {
                    val basename = zipEntry.name.substringAfterLast('/')
                    if (!basename.startsWith("preview_")) {
                        val tempDir = File(
                            context.cacheDir,
                            if (baseDir.isNotEmpty()) "quicknote/${file.name}-images/$baseDir"
                            else "quicknote/${file.name}-images"
                        ).apply { mkdirs() }
                        val imageFile = File(tempDir, basename).apply { deleteOnExit() }
                        updateCurrentAction("Extracting ${zipEntry.name}")
                        extractFileFromZip(zipFile, zipEntry, imageFile)
                        if (imageFile.exists()) {
                            updateCurrentAction("Copying ${imageFile.name}")
                            uriToImage(context, imageFile.toUri(), note.id)?.let { image ->
                                images.add(image.copy(position = imagePosition++))
                            }
                        }
                    }
                }
            }

            return NoteCombo(
                note = note,
                checklistItems = checklistItems.toImmutableList(),
                images = images.toImmutableList()
            )
        }

        return null
    }

    private fun resetNextCloudStatus() {
        isNextCloudWorking.value = null
        isNextCloudUrlError.value = false
        isNextCloudAuthError.value = false
    }

    private fun resetSFTPStatus() {
        isSFTPWorking.value = null
    }

    private fun updateCurrentAction(action: String) {
        importCurrentActionIndex.value++
        importCurrentAction.value = action
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_MIN_COLUMN_WIDTH -> minColumnWidth.value = preferences.getInt(key, DEFAULT_MIN_COLUMN_WIDTH)
            PREF_NEXTCLOUD_BASE_DIR -> nextCloudBaseDir.value =
                preferences.getString(key, NEXTCLOUD_BASE_DIR) ?: NEXTCLOUD_BASE_DIR
            PREF_NEXTCLOUD_PASSWORD -> nextCloudPassword.value = preferences.getString(key, "") ?: ""
            PREF_NEXTCLOUD_URI -> nextCloudUri.value = preferences.getString(key, "") ?: ""
            PREF_NEXTCLOUD_USERNAME -> nextCloudUsername.value = preferences.getString(key, "") ?: ""
            PREF_SFTP_BASE_DIR -> sftpBaseDir.value = preferences.getString(key, SFTP_BASE_DIR) ?: SFTP_BASE_DIR
            PREF_SFTP_HOSTNAME -> sftpHostname.value = preferences.getString(key, "") ?: ""
            PREF_SFTP_PASSWORD -> sftpPassword.value = preferences.getString(key, "") ?: ""
            PREF_SFTP_PORT -> sftpPort.value = preferences.getInt(key, 22)
            PREF_SFTP_USERNAME -> sftpUsername.value = preferences.getString(key, "") ?: ""
            PREF_SYNC_BACKEND -> syncBackend.value =
                preferences.getString(key, null)?.let { SyncBackend.valueOf(it) }
        }
    }
}
