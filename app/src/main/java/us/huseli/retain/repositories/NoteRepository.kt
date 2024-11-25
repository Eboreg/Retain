package us.huseli.retain.repositories

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import us.huseli.retain.Constants.IMAGE_SUBDIR
import us.huseli.retain.dao.ChecklistItemDao
import us.huseli.retain.dao.ImageDao
import us.huseli.retain.dao.NoteDao
import us.huseli.retain.dataclasses.ImageData
import us.huseli.retain.dataclasses.NotePojo
import us.huseli.retain.dataclasses.entities.ChecklistItem
import us.huseli.retain.dataclasses.entities.Image
import us.huseli.retain.dataclasses.entities.Note
import us.huseli.retain.dataclasses.uistate.NoteUiState
import us.huseli.retain.toBitmap
import us.huseli.retaintheme.utils.AbstractScopeHolder
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
) : AbstractScopeHolder() {
    private val _imageDir = File(context.filesDir, IMAGE_SUBDIR).apply { mkdirs() }
    private val _imageBitmaps = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())

    val pojos: Flow<List<NotePojo>> = noteDao.flowNotePojoList().map { pojos ->
        pojos.map { pojo -> pojo.copy(checklistItems = pojo.checklistItems.sorted()) }
    }

    suspend fun archiveNotes(notes: Collection<Note>) = noteDao.update(notes.map { it.copy(isArchived = true) })

    suspend fun deleteChecklistItem(itemId: UUID) = checklistItemDao.delete(itemId)

    suspend fun deleteChecklistItems(itemIds: Collection<UUID>) = checklistItemDao.delete(*itemIds.toTypedArray())

    suspend fun deleteImages(filenames: Collection<String>) = imageDao.delete(*filenames.toTypedArray())

    suspend fun deleteTrashedNotes() = noteDao.deleteTrashed()

    suspend fun getImageBitmap(filename: String): ImageBitmap? {
        return onIOThread {
            _imageBitmaps.value[filename] ?: File(_imageDir, filename).toBitmap()?.asImageBitmap()
                ?.also { _imageBitmaps.value += filename to it }
        }
    }

    suspend fun getMaxNotePosition(): Int = noteDao.getMaxPosition()

    suspend fun getNote(noteId: UUID): Note = noteDao.getNote(noteId)

    suspend fun insertNotePojos(pojos: List<NotePojo>) {
        noteDao.insert(pojos.map { it.note })
        checklistItemDao.upsert(*pojos.flatMap { it.checklistItems }.toTypedArray())
        imageDao.upsert(pojos.flatMap { it.images })
    }

    suspend fun listChecklistItemsByNoteId(noteId: UUID): List<ChecklistItem> = checklistItemDao.listByNoteId(noteId)

    suspend fun listImagesByNoteId(noteId: UUID): List<Image> = imageDao.listByNoteId(noteId)

    suspend fun saveChecklistItem(item: ChecklistItem) {
        onIOThread { checklistItemDao.upsert(item) }
    }

    suspend fun saveChecklistItems(items: Collection<ChecklistItem>) {
        onIOThread { checklistItemDao.upsert(*items.toTypedArray()) }
    }

    suspend fun saveImages(images: Collection<Image>) {
        onIOThread { imageDao.upsert(images) }
    }

    suspend fun saveNoteUiState(state: NoteUiState) {
        if (state.shouldSave) {
            val note = state.toNote()
            val savedNote = onIOThread { noteDao.upsert(note) }

            onMainThread {
                state.onNoteUpdated(savedNote)
                state.status = NoteUiState.Status.REGULAR
            }
        }
    }

    suspend fun trashNotes(notes: Collection<Note>) = noteDao.update(notes.map { it.copy(isDeleted = true) })

    suspend fun unarchiveNotes(notes: Collection<Note>) = noteDao.update(notes.map { it.copy(isArchived = false) })

    suspend fun updateNotePositions(notes: Collection<Note>) = noteDao.updatePositions(notes)

    suspend fun updateNotes(notes: Collection<Note>) = noteDao.update(notes)

    suspend fun uriToImageData(uri: Uri): ImageData? = us.huseli.retain.uriToImageData(context, uri)
}
