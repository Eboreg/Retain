package us.huseli.retain.viewmodels

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.Color
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
import us.huseli.retain.Constants
import us.huseli.retain.Constants.DEFAULT_MIN_COLUMN_WIDTH
import us.huseli.retain.Constants.NEXTCLOUD_BASE_DIR
import us.huseli.retain.Constants.PREF_MIN_COLUMN_WIDTH
import us.huseli.retain.Constants.PREF_NEXTCLOUD_BASE_DIR
import us.huseli.retain.Constants.PREF_NEXTCLOUD_PASSWORD
import us.huseli.retain.Constants.PREF_NEXTCLOUD_URI
import us.huseli.retain.Constants.PREF_NEXTCLOUD_USERNAME
import us.huseli.retain.Enums
import us.huseli.retain.LogInterface
import us.huseli.retain.Logger
import us.huseli.retain.copyFileToLocal
import us.huseli.retain.data.NextCloudRepository
import us.huseli.retain.data.NoteRepository
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.Note
import us.huseli.retain.data.entities.NoteCombo
import us.huseli.retain.nextcloud.tasks.TestNextCloudTaskResult
import us.huseli.retain.readTextFileFromZip
import us.huseli.retain.uriToImage
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.util.zip.ZipFile
import javax.inject.Inject

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
    private val _statusBarColor = MutableStateFlow<Color?>(null)
    private val _navigationBarColor = MutableStateFlow<Color?>(null)
    private val _keepImportActionCount = MutableStateFlow(0)
    private val _keepImportCurrentAction = MutableStateFlow("")
    private val _keepImportCurrentActionIndex = MutableStateFlow(0)
    private val _keepImportIsActive = MutableStateFlow(false)
    private var _keepImportJob: Job? = null

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
    val statusBarColor = _statusBarColor.asStateFlow()
    val navigationBarColor = _navigationBarColor.asStateFlow()
    val keepImportActionCount = _keepImportActionCount.asStateFlow()
    val keepImportCurrentAction = _keepImportCurrentAction.asStateFlow()
    val keepImportCurrentActionIndex = _keepImportCurrentActionIndex.asStateFlow()
    val keepImportIsActive = _keepImportIsActive.asStateFlow()

    fun cancelKeepImport() {
        _keepImportJob?.cancel()
        _keepImportIsActive.value = false
    }

    @Suppress("destructure")
    fun keepImport(zipUri: Uri, context: Context) {
        _keepImportIsActive.value = true

        _keepImportJob = viewModelScope.launch {
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
                            else if (Files.probeContentType(Paths.get(it.name)).startsWith("image/")) imageCount++
                        }
                    }
                }

                _keepImportActionCount.value = ((noteCount + imageCount) * 2) + if (noteCount > 0) 1 else 0

                zipFile.entries().iterator().forEach { zipEntry ->
                    zipEntry?.let {
                        if (zipEntry.name.startsWith("Takeout/Keep/")) {
                            if (zipEntry.name.endsWith(".json")) {
                                _keepImportCurrentActionIndex.value++
                                _keepImportCurrentAction.value = "Extracting ${zipEntry.name}"
                                val json = readTextFileFromZip(zipFile, zipEntry)
                                gson.fromJson(json, GoogleNoteEntry::class.java)?.let { entries.add(it) }
                            } else if (Files.probeContentType(Paths.get(zipEntry.name)).startsWith("image/")) {
                                _keepImportCurrentActionIndex.value++
                                _keepImportCurrentAction.value = "Extracting ${zipEntry.name}"
                                val buffer = ByteArray(Constants.ZIP_BUFFER_SIZE)
                                val imageFile = File(tempDir, zipEntry.name.substringAfterLast('/'))
                                FileOutputStream(imageFile).use { outputStream ->
                                    val inputStream = zipFile.getInputStream(zipEntry)
                                    var length: Int
                                    while (inputStream.read(buffer).also { length = it } > 0) {
                                        outputStream.write(buffer, 0, length)
                                    }
                                }
                            }
                        }
                    }
                }

                entries.forEachIndexed { noteIndex, noteEntry ->
                    _keepImportCurrentActionIndex.value++
                    _keepImportCurrentAction.value =
                        if (noteEntry.title != null) "Converting note ${noteEntry.title}" else "Converting note"

                    val note = Note(
                        color = noteEntry.color ?: "DEFAULT",
                        title = noteEntry.title ?: "",
                        text = noteEntry.textContent ?: "",
                        type = if (noteEntry.listContent != null) Enums.NoteType.CHECKLIST else Enums.NoteType.TEXT,
                        isDeleted = noteEntry.isTrashed,
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
                        _keepImportCurrentActionIndex.value++
                        _keepImportCurrentAction.value = "Moving ${imageEntry.filePath}"
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
                    _keepImportCurrentActionIndex.value++
                    _keepImportCurrentAction.value = "Saving to database"
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

    fun setSystemBarColors(statusBar: Color, navigationBar: Color) {
        _statusBarColor.value = statusBar
        _navigationBarColor.value = navigationBar
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
