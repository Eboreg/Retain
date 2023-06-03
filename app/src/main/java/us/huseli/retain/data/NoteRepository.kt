package us.huseli.retain.data

import android.content.Context
import android.net.Uri
import android.os.FileObserver
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import us.huseli.retain.Constants.IMAGE_SUBDIR
import us.huseli.retain.LogInterface
import us.huseli.retain.Logger
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.Image
import us.huseli.retain.data.entities.Note
import us.huseli.retain.data.entities.NoteCombo
import us.huseli.retain.fileToImageBitmap
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteDao: NoteDao,
    private val checklistItemDao: ChecklistItemDao,
    private val imageDao: ImageDao,
    override val logger: Logger,
    private val database: Database,
) : LogInterface {
    private val _eventTypeMask = FileObserver.CLOSE_WRITE or FileObserver.MOVED_TO
    private val _imageDir = File(context.filesDir, IMAGE_SUBDIR).apply { mkdirs() }
    private val _imageBitmaps = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())
    private val _imageDirObserver = object : FileObserver(_imageDir) {
        override fun onEvent(event: Int, path: String?) {
            if (event and _eventTypeMask != 0 && path != null) {
                try {
                    addImageBitmap(path)
                } catch (e: Exception) {
                    val eventType = when (event) {
                        CLOSE_WRITE -> "CLOSE_WRITE"
                        MOVED_TO -> "MOVED_TO"
                        else -> event.toString()
                    }
                    log("_imageDirObserver.onEvent($eventType, $path): could not process: $e", level = Log.ERROR)
                    log(e.stackTraceToString(), level = Log.ERROR)
                }
            }
        }
    }

    val nextCloudNeedsTesting = MutableStateFlow(true)
    val checklistItems: Flow<List<ChecklistItem>> = checklistItemDao.flowList()
    val images: Flow<List<Image>> = imageDao.flowList().map { images ->
        images.map { it.copy(imageBitmap = getImageBitmap(it.filename)) }
    }
    val notes: Flow<List<Note>> = noteDao.flowList()

    init {
        _imageDirObserver.startWatching()
        _imageDir.listFiles()?.forEach { file -> addImageBitmap(file) }
    }

    private fun addImageBitmap(file: File) {
        try {
            fileToImageBitmap(file, context)?.let {
                _imageBitmaps.value = _imageBitmaps.value.toMutableMap().apply { set(file.name, it) }
            }
        } catch (e: Exception) {
            log("Error on processing ${file.name}: $e", level = Log.ERROR, showInSnackbar = true)
        }
    }

    internal fun addImageBitmap(filename: String) = addImageBitmap(File(_imageDir, filename))

    suspend fun archiveNotes(notes: Collection<Note>) = noteDao.update(notes.map { it.copy(isArchived = true) })

    suspend fun getCombo(noteId: UUID): NoteCombo? =
        noteDao.getNote(noteId)?.let { note ->
            NoteCombo(
                note = note,
                checklistItems = checklistItemDao.listByNoteId(noteId),
                images = imageDao.listByNoteIds(listOf(noteId)).map {
                    it.copy(imageBitmap = getImageBitmap(it.filename))
                },
                databaseVersion = database.openHelper.readableDatabase.version,
            )
        }

    private fun getImageBitmap(filename: String): Flow<ImageBitmap?> = _imageBitmaps.map { it[filename] }

    suspend fun getMaxNotePosition() = noteDao.getMaxPosition()

    suspend fun insertCombos(combos: List<NoteCombo>) {
        noteDao.insert(combos.map { it.note })
        checklistItemDao.upsert(combos.flatMap { it.checklistItems })
        imageDao.upsert(combos.flatMap { it.images })
    }

    suspend fun trashNotes(notes: Collection<Note>) = noteDao.update(notes.map { it.copy(isDeleted = true) })

    suspend fun unarchiveNotes(notes: Collection<Note>) = noteDao.update(notes.map { it.copy(isArchived = false) })

    suspend fun updateNotePositions(notes: Collection<Note>) = noteDao.updatePositions(notes)

    suspend fun upsertChecklistItems(items: Collection<ChecklistItem>) = checklistItemDao.upsert(items)

    suspend fun upsertImages(images: Collection<Image>) = imageDao.upsert(images)

    suspend fun upsertNote(note: Note) = noteDao.upsert(note)

    suspend fun updateNotes(notes: Collection<Note>) = noteDao.update(notes)

    suspend fun uriToImage(uri: Uri, noteId: UUID): Image? =
        us.huseli.retain.uriToImage(context, uri, noteId)?.let {
            it.copy(
                position = imageDao.getMaxPosition(noteId) + 1,
                imageBitmap = getImageBitmap(it.filename),
            )
        }
}
