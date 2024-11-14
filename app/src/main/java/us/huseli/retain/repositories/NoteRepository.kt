package us.huseli.retain.repositories

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.retain.Constants.IMAGE_SUBDIR
import us.huseli.retain.Database
import us.huseli.retain.LogInterface
import us.huseli.retain.Logger
import us.huseli.retain.dao.ChecklistItemDao
import us.huseli.retain.dao.ImageDao
import us.huseli.retain.dao.NoteDao
import us.huseli.retain.dataclasses.NotePojo
import us.huseli.retain.dataclasses.entities.Image
import us.huseli.retain.dataclasses.entities.Note
import us.huseli.retain.toBitmap
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
    private val ioScope: CoroutineScope,
) : LogInterface {
    private val _imageDir = File(context.filesDir, IMAGE_SUBDIR).apply { mkdirs() }
    private val _imageBitmaps = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())

    val pojos: Flow<List<NotePojo>> = noteDao.flowNotePojoList()

    suspend fun archiveNotes(notes: Collection<Note>) = noteDao.update(notes.map { it.copy(isArchived = true) })

    suspend fun deleteTrashedNotes() = noteDao.deleteTrashed()

    fun flowNotePojo(noteId: UUID) = noteDao.flowNotePojo(noteId).map { pojo ->
        pojo?.let { it.copy(checklistItems = it.checklistItems.sorted(), images = it.images.sorted()) }
    }

    fun getImageBitmap(filename: String): Flow<ImageBitmap?> = _imageBitmaps.map { imageBitmapMap ->
        imageBitmapMap[filename].let { imageBitmap ->
            imageBitmap ?: File(_imageDir, filename).toBitmap()
                ?.asImageBitmap()
                ?.also { _imageBitmaps.value += filename to it }
        }
    }

    suspend fun getMaxNotePosition(): Int = noteDao.getMaxPosition()

    suspend fun insertNotePojos(pojos: List<NotePojo>) {
        noteDao.insert(pojos.map { it.note })
        checklistItemDao.upsert(pojos.flatMap { it.checklistItems })
        imageDao.upsert(pojos.flatMap { it.images })
    }

    suspend fun listImagesByNoteId(noteId: UUID): List<Image> = imageDao.listByNoteId(noteId)

    fun saveNotePojo(pojo: NotePojo, vararg components: NotePojo.Component) {
        /**
         * Not a suspend function, since it should not be tied to the scope of
         * any single viewmodel or composition.
         *
         * Reindexes ChecklistItems and Images before saving.
         */
        ioScope.launch {
            database.withTransaction {
                if (components.isNotEmpty()) noteDao.upsert(pojo.note)
                if (components.contains(NotePojo.Component.CHECKLIST_ITEMS)) {
                    val checkedItems = pojo.checklistItems.filter { it.checked }
                        .mapIndexed { index, item -> item.copy(position = index) }
                    val uncheckedItems = pojo.checklistItems.filter { !it.checked }
                        .mapIndexed { index, item -> item.copy(position = index) }

                    checklistItemDao.replace(pojo.note.id, uncheckedItems + checkedItems)
                }
                if (components.contains(NotePojo.Component.IMAGES)) {
                    imageDao.replace(
                        pojo.note.id,
                        pojo.images.mapIndexed { index, image -> image.copy(position = index) },
                    )
                }
            }
        }
    }

    suspend fun trashNotes(notes: Collection<Note>) = noteDao.update(notes.map { it.copy(isDeleted = true) })

    suspend fun unarchiveNotes(notes: Collection<Note>) = noteDao.update(notes.map { it.copy(isArchived = false) })

    suspend fun updateNotePositions(notes: Collection<Note>) = noteDao.updatePositions(notes)

    suspend fun updateNotes(notes: Collection<Note>) = noteDao.update(notes)

    suspend fun uriToImage(uri: Uri, noteId: UUID): Image? =
        us.huseli.retain.uriToImage(context, uri, noteId)?.copy(
            position = imageDao.getMaxPosition(noteId) + 1,
        )
}
