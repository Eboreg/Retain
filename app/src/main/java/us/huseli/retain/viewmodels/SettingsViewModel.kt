package us.huseli.retain.viewmodels

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.internal.toImmutableList
import org.jsoup.Jsoup
import us.huseli.retain.Constants.DEFAULT_MIN_COLUMN_WIDTH
import us.huseli.retain.Constants.PREF_MIN_COLUMN_WIDTH
import us.huseli.retain.Constants.PREF_SYNC_BACKEND
import us.huseli.retain.Enums.NoteType
import us.huseli.retain.Enums.SyncBackend
import us.huseli.retain.ILogger
import us.huseli.retain.copyFileToLocal
import us.huseli.retain.dataclasses.ImageData
import us.huseli.retain.dataclasses.NotePojo
import us.huseli.retain.dataclasses.QuickNoteEntry
import us.huseli.retain.dataclasses.entities.ChecklistItem
import us.huseli.retain.dataclasses.entities.Note
import us.huseli.retain.dataclasses.google.GoogleNoteEntry
import us.huseli.retain.extractFile
import us.huseli.retain.isImageFile
import us.huseli.retain.readTextFile
import us.huseli.retain.repositories.NoteRepository
import us.huseli.retain.repositories.SyncBackendRepository
import us.huseli.retain.uriToImageData
import us.huseli.retaintheme.extensions.launchOnIOThread
import java.io.File
import java.time.Instant
import java.util.zip.ZipFile
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: NoteRepository,
    private val syncBackendRepository: SyncBackendRepository,
) : ViewModel(), SharedPreferences.OnSharedPreferenceChangeListener, ILogger {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private var importJob: Job? = null
    private val sectionsShown = mutableMapOf<String, MutableState<Boolean>>()
    private var originalSyncBackend = preferences.getString(PREF_SYNC_BACKEND, null)?.let { SyncBackend.valueOf(it) }

    private val _importActionCount = MutableStateFlow<Int?>(null)
    private val _importCurrentAction = MutableStateFlow("")
    private val _importCurrentActionIndex = MutableStateFlow(0)
    private val _keepImportIsActive = MutableStateFlow(false)
    private val _minColumnWidth = MutableStateFlow(preferences.getInt(PREF_MIN_COLUMN_WIDTH, DEFAULT_MIN_COLUMN_WIDTH))
    private val _quickNoteImportIsActive = MutableStateFlow(false)
    private val _syncBackend = MutableStateFlow(originalSyncBackend)

    val importActionCount = _importActionCount.asStateFlow()
    val importCurrentAction = _importCurrentAction.asStateFlow()
    val importCurrentActionIndex = _importCurrentActionIndex.asStateFlow()
    val isSyncBackendEnabled = _syncBackend.map { it != null && it != SyncBackend.NONE }
    val keepImportIsActive = _keepImportIsActive.asStateFlow()
    val minColumnWidth = _minColumnWidth.asStateFlow()
    val quickNoteImportIsActive = _quickNoteImportIsActive.asStateFlow()
    val syncBackend = _syncBackend.asStateFlow()

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    fun cancelImport() {
        importJob?.cancel()
        _quickNoteImportIsActive.value = false
        _keepImportIsActive.value = false
    }

    fun getSectionShown(key: String, default: Boolean): State<Boolean> {
        return sectionsShown[key] ?: mutableStateOf(default).also { sectionsShown[key] = it }
    }

    @Suppress("destructure")
    fun keepImport(zipUri: Uri, context: Context) {
        _keepImportIsActive.value = true
        _importCurrentActionIndex.value = 0
        _importCurrentAction.value = ""

        importJob = launchOnIOThread {
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
                val pojos = mutableListOf<NotePojo>()
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
                                val json = zipFile.readTextFile(zipEntry)
                                gson.fromJson(json, GoogleNoteEntry::class.java)?.let {
                                    if (!it.isTrashed) entries.add(it)
                                }
                            } else if (isImageFile(zipEntry.name)) {
                                updateCurrentAction("Extracting ${zipEntry.name}")
                                val imageFile =
                                    File(tempDir, zipEntry.name.substringAfterLast('/')).apply { deleteOnExit() }
                                zipFile.extractFile(zipEntry, imageFile)
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
                        created = noteEntry.createdTimestampUsec
                            ?.let { Instant.ofEpochMilli(it / 1000) }
                            ?: Instant.now(),
                        updated = noteEntry.userEditedTimestampUsec
                            ?.let { Instant.ofEpochMilli(it / 1000) }
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
                        if (imageFile.exists()) uriToImageData(context, imageFile.toUri()) else null
                    }

                    pojos.add(
                        NotePojo(
                            note = note,
                            checklistItems = checklistItems ?: emptyList(),
                            // images = images ?: emptyList(),
                            images = emptyList(), // TODO: fix
                        )
                    )
                }

                if (pojos.isNotEmpty()) {
                    updateCurrentAction("Saving to database")
                    repository.insertNotePojos(pojos)
                }
                log("Imported ${pojos.size} notes.", showSnackbar = true)
            } catch (e: Exception) {
                logError("Error: $e", e, showSnackbar = true)
            } finally {
                _keepImportIsActive.value = false
            }
        }
    }

    fun quickNoteImport(zipUri: Uri, context: Context) {
        _quickNoteImportIsActive.value = true
        _importCurrentActionIndex.value = 0
        _importCurrentAction.value = ""

        launchOnIOThread {
            try {
                val tempDir = File(context.cacheDir, "quicknote").apply { mkdirs() }
                val quickNoteFile = File(
                    tempDir,
                    zipUri.lastPathSegment?.substringAfterLast('/') ?: "quicknote.zip"
                ).apply { deleteOnExit() }

                updateCurrentAction("Copying and opening ${quickNoteFile.name}")
                copyFileToLocal(context, zipUri, quickNoteFile)

                val zipFile = withContext(Dispatchers.IO) { ZipFile(quickNoteFile) }
                val pojos = mutableListOf<NotePojo>()
                val sqdDirs = mutableListOf<String>()
                val sqdDirRegex = Regex("^.*\\.sqd/$")
                var notePosition = repository.getMaxNotePosition() + 1

                zipFile.entries().iterator().forEach { zipEntry ->
                    if (zipEntry != null) {
                        if (zipEntry.name.endsWith(".sqd") && !zipEntry.isDirectory) {
                            val sqdFile = File(tempDir, zipEntry.name.substringAfterLast('/'))
                            updateCurrentAction("Extracting ${sqdFile.name}")
                            zipFile.extractFile(zipEntry, sqdFile)
                            extractQuickNoteSqd(sqdFile, context, notePosition)?.let {
                                pojos.add(it)
                                notePosition++
                            }
                        } else sqdDirRegex.find(zipEntry.name)?.let { result ->
                            result.groups[0]?.value?.let { dirname -> sqdDirs.add(dirname) }
                        }
                    }
                }

                sqdDirs.forEach { dirname ->
                    extractQuickNoteSqd(quickNoteFile, context, notePosition, dirname)?.let {
                        pojos.add(it)
                        notePosition++
                    }
                }

                if (pojos.isNotEmpty()) {
                    updateCurrentAction("Saving to database")
                    repository.insertNotePojos(pojos)
                }
                log("Imported ${pojos.size} notes.", showSnackbar = true)
            } catch (e: Exception) {
                logError("Error: $e", e, showSnackbar = true)
            } finally {
                _quickNoteImportIsActive.value = false
            }
        }
    }

    fun save() {
        syncBackendRepository.save()
        preferences.edit()
            .putInt(PREF_MIN_COLUMN_WIDTH, _minColumnWidth.value)
            .putString(PREF_SYNC_BACKEND, _syncBackend.value?.name)
            .apply()
        if (!listOf(SyncBackend.NONE, originalSyncBackend, null).contains(_syncBackend.value)) {
            originalSyncBackend = _syncBackend.value
            launchOnIOThread { syncBackendRepository.sync() }
        }
    }

    fun toggleSectionShown(key: String) {
        sectionsShown.getOrPut(key) { mutableStateOf(true) }.apply { value = !value }
    }

    fun updateField(field: String, value: Any) {
        when (field) {
            PREF_MIN_COLUMN_WIDTH -> _minColumnWidth.value = value as Int
            PREF_SYNC_BACKEND -> {
                if (value != _syncBackend.value) {
                    _syncBackend.value = value as SyncBackend
                    preferences.edit().putString(PREF_SYNC_BACKEND, _syncBackend.value?.name).apply()
                }
            }
        }
    }

    @Suppress("SameReturnValue")
    private suspend fun extractQuickNoteSqd(
        file: File,
        context: Context,
        notePosition: Int,
        baseDir: String = "",
    ): NotePojo? {
        val zipFile = withContext(Dispatchers.IO) { ZipFile(file) }
        var quickNoteEntry: QuickNoteEntry? = null
        var text = ""
        val gson: Gson = GsonBuilder().create()

        zipFile.getEntry("${baseDir}metadata.json")?.let { zipEntry ->
            updateCurrentAction("Parsing ${zipEntry.name}")
            val json = zipFile.readTextFile(zipEntry)
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
                val html = zipFile.readTextFile(zipEntry)
                val doc = Jsoup.parseBodyFragment(html)
                text = doc.body().wholeText().trim().replace("\n\n", "\n")
            }

            val checklistItems = mutableListOf<ChecklistItem>()
            val images = mutableListOf<ImageData>()
            var imagePosition = 0
            var checklistItemPosition = 0
            val title = entry.title
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
                        zipFile.extractFile(zipEntry, imageFile)
                        if (imageFile.exists()) {
                            updateCurrentAction("Copying ${imageFile.name}")
                            uriToImageData(context, imageFile.toUri())?.let { image ->
                                images.add(image)
                            }
                        }
                    }
                }
            }

            return NotePojo(
                note = note,
                checklistItems = checklistItems.toImmutableList(),
                images = emptyList(), // TODO: fix
                // images = images.toImmutableList(),
            )
        }

        return null
    }

    private fun updateCurrentAction(action: String) {
        _importCurrentActionIndex.value++
        _importCurrentAction.value = action
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_MIN_COLUMN_WIDTH -> _minColumnWidth.value = preferences.getInt(key, DEFAULT_MIN_COLUMN_WIDTH)
            PREF_SYNC_BACKEND -> _syncBackend.value =
                preferences.getString(key, null)?.let { SyncBackend.valueOf(it) }
        }
    }
}
