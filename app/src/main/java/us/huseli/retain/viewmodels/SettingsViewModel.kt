package us.huseli.retain.viewmodels

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import us.huseli.retain.Enums.NoteType
import us.huseli.retain.LogInterface
import us.huseli.retain.Logger
import us.huseli.retain.copyFileToLocal
import us.huseli.retain.data.NextCloudRepository
import us.huseli.retain.data.NoteRepository
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.Image
import us.huseli.retain.data.entities.Note
import us.huseli.retain.data.entities.NoteCombo
import us.huseli.retain.extractFileFromZip
import us.huseli.retain.isImageFile
import us.huseli.retain.nextcloud.tasks.TestNextCloudTaskResult
import us.huseli.retain.readTextFileFromZip
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
    private val nextCloudRepository: NextCloudRepository,
    override val logger: Logger,
) : ViewModel(), SharedPreferences.OnSharedPreferenceChangeListener, LogInterface {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val _importActionCount = MutableStateFlow<Int?>(null)
    private val _importCurrentAction = MutableStateFlow("")
    private val _importCurrentActionIndex = MutableStateFlow(0)
    private val _keepImportIsActive = MutableStateFlow(false)
    private val _quickNoteImportIsActive = MutableStateFlow(false)
    private var _importJob: Job? = null

    val nextCloudUri = MutableStateFlow(preferences.getString(PREF_NEXTCLOUD_URI, "") ?: "")
    val nextCloudUsername = MutableStateFlow(preferences.getString(PREF_NEXTCLOUD_USERNAME, "") ?: "")
    val nextCloudPassword = MutableStateFlow(preferences.getString(PREF_NEXTCLOUD_PASSWORD, "") ?: "")
    val nextCloudBaseDir = MutableStateFlow(preferences.getString(PREF_NEXTCLOUD_BASE_DIR, NEXTCLOUD_BASE_DIR) ?: "")
    val minColumnWidth = MutableStateFlow(preferences.getInt(PREF_MIN_COLUMN_WIDTH, DEFAULT_MIN_COLUMN_WIDTH))
    val isNextCloudTesting = MutableStateFlow(false)
    val isNextCloudWorking = MutableStateFlow<Boolean?>(null)
    val isNextCloudUrlFail = MutableStateFlow(false)
    val isNextCloudCredentialsFail = MutableStateFlow(false)
    val nextCloudNeedsTesting = repository.nextCloudNeedsTesting.asStateFlow()
    val importActionCount = _importActionCount.asStateFlow()
    val importCurrentAction = _importCurrentAction.asStateFlow()
    val importCurrentActionIndex = _importCurrentActionIndex.asStateFlow()
    val keepImportIsActive = _keepImportIsActive.asStateFlow()
    val quickNoteImportIsActive = _quickNoteImportIsActive.asStateFlow()

    fun cancelImport() {
        _importJob?.cancel()
        _quickNoteImportIsActive.value = false
        _keepImportIsActive.value = false
    }

    private fun updateCurrentAction(action: String) {
        _importCurrentActionIndex.value++
        _importCurrentAction.value = action
    }

    fun quickNoteImport(zipUri: Uri, context: Context) {
        _quickNoteImportIsActive.value = true
        _importCurrentActionIndex.value = 0
        _importCurrentAction.value = ""

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
                            extractSqd(sqdFile, context, notePosition)?.let {
                                combos.add(it)
                                notePosition++
                            }
                        } else sqdDirRegex.find(zipEntry.name)?.let { result ->
                            result.groups[0]?.value?.let { dirname -> sqdDirs.add(dirname) }
                        }
                    }
                }

                sqdDirs.forEach { dirname ->
                    extractSqd(quickNoteFile, context, notePosition, dirname)?.let {
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
                _quickNoteImportIsActive.value = false
            }
        }
    }

    @Suppress("SameReturnValue")
    private suspend fun extractSqd(file: File, context: Context, notePosition: Int, baseDir: String = ""): NoteCombo? {
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

    @Suppress("destructure")
    fun keepImport(zipUri: Uri, context: Context) {
        _keepImportIsActive.value = true
        _importCurrentActionIndex.value = 0
        _importCurrentAction.value = ""

        _importJob = viewModelScope.launch {
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

                _importActionCount.value = ((noteCount + imageCount) * 2) + if (noteCount > 0) 1 else 0

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
                _keepImportIsActive.value = false
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
            .apply()
        repository.nextCloudNeedsTesting.value = true
    }

    fun testNextCloud(onResult: (TestNextCloudTaskResult) -> Unit) = viewModelScope.launch {
        repository.nextCloudNeedsTesting.value = false
        isNextCloudTesting.value = true
        nextCloudRepository.test(
            Uri.parse(nextCloudUri.value),
            nextCloudUsername.value,
            nextCloudPassword.value,
            nextCloudBaseDir.value,
        ) { result ->
            isNextCloudTesting.value = false
            isNextCloudWorking.value = result.success
            isNextCloudUrlFail.value = result.isUrlFail
            isNextCloudCredentialsFail.value = result.isCredentialsFail
            onResult(result)
        }
    }

    fun updateField(field: String, value: Any) {
        when (field) {
            PREF_NEXTCLOUD_URI -> nextCloudUri.value = value as String
            PREF_NEXTCLOUD_USERNAME -> nextCloudUsername.value = value as String
            PREF_NEXTCLOUD_PASSWORD -> nextCloudPassword.value = value as String
            PREF_NEXTCLOUD_BASE_DIR -> nextCloudBaseDir.value = value as String
            PREF_MIN_COLUMN_WIDTH -> minColumnWidth.value = value as Int
        }
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_NEXTCLOUD_URI -> nextCloudUri.value = preferences.getString(key, "") ?: ""
            PREF_NEXTCLOUD_USERNAME -> nextCloudUsername.value = preferences.getString(key, "") ?: ""
            PREF_NEXTCLOUD_PASSWORD -> nextCloudPassword.value = preferences.getString(key, "") ?: ""
            PREF_MIN_COLUMN_WIDTH -> minColumnWidth.value = preferences.getInt(key, DEFAULT_MIN_COLUMN_WIDTH)
            PREF_NEXTCLOUD_BASE_DIR -> nextCloudBaseDir.value =
                preferences.getString(key, NEXTCLOUD_BASE_DIR) ?: NEXTCLOUD_BASE_DIR
        }
    }
}
