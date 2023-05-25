package us.huseli.retain.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.scale
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.huseli.retain.Constants
import us.huseli.retain.Constants.IMAGE_SUBDIR
import us.huseli.retain.Enums.NoteType
import us.huseli.retain.LogInterface
import us.huseli.retain.Logger
import us.huseli.retain.copyFileToLocal
import us.huseli.retain.data.entities.BitmapImage
import us.huseli.retain.data.entities.Image
import us.huseli.retain.data.entities.Note
import us.huseli.retain.data.entities.NoteCombo
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

@Singleton
class NoteRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteDao: NoteDao,
    private val checklistItemDao: ChecklistItemDao,
    private val imageDao: ImageDao,
    ioScope: CoroutineScope,
    override val logger: Logger,
    private val nextCloudRepository: NextCloudRepository,
) : LogInterface {
    private val imageDir = File(context.filesDir, IMAGE_SUBDIR).apply { mkdirs() }

    val combos: Flow<List<NoteCombo>> = noteDao.flowListCombos().map { combos ->
        combos.map { combo ->
            combo.copy(
                checklistItems = combo.checklistItems.sortedWith(
                    compareBy({ it.checked }, { it.position })
                )
            )
        }
    }
    val nextCloudNeedsTesting = MutableStateFlow(true)
    val bitmapImages = MutableStateFlow<List<BitmapImage>>(emptyList())

    init {
        ioScope.launch {
            nextCloudRepository.bitmapImages.collect { bitmapImages ->
                this@NoteRepository.bitmapImages.value = bitmapImages
            }
        }

        ioScope.launch {
            imageDao.flowList().collect { images ->
                val newBitmapImages = bitmapImages.value.toMutableList()
                val currentImages = newBitmapImages.map { it.image }

                images.forEach { image ->
                    // Match by content (no point in updating if old and new
                    // contents are identical):
                    if (!currentImages.contains(image)) {
                        image.toBitmapImage(context)?.let { bitmapImage ->
                            newBitmapImages.removeIf { it.image.filename == image.filename }
                            newBitmapImages.add(bitmapImage)
                        }
                    }
                }
                newBitmapImages.removeIf { !images.contains(it.image) }
                bitmapImages.value = newBitmapImages
            }
        }
    }

    @Suppress("unused")
    suspend fun deleteNotes(ids: Collection<UUID>) {
        val images = imageDao.listByNoteIds(ids)
        @Suppress("Destructure")
        images.forEach { image ->
            File(imageDir, image.filename).delete()
        }
        noteDao.delete(ids)
        nextCloudRepository.deleteNotes(ids, images)
    }

    suspend fun getCombo(id: UUID): NoteCombo? = noteDao.getCombo(id)

    suspend fun trashNotes(combos: Collection<NoteCombo>) {
        val trashedCombos = combos.map { it.copy(note = it.note.copy(isDeleted = true)) }

        noteDao.update(trashedCombos.map { it.note })
        nextCloudRepository.upload(trashedCombos)
    }

    suspend fun updateNotePositions(notes: Collection<Note>) = noteDao.updatePositions(notes)

    suspend fun upsertNoteCombo(combo: NoteCombo) {
        noteDao.upsert(combo.note)
        if (combo.note.type == NoteType.CHECKLIST) checklistItemDao.replace(combo.note.id, combo.checklistItems)
        imageDao.replace(combo.note.id, combo.images)
        nextCloudRepository.upload(combo)
    }

    suspend fun upsertNotes(combos: Collection<NoteCombo>) {
        noteDao.update(combos.map { it.note })
        nextCloudRepository.upload(combos)
    }

    suspend fun uriToBitmapImage(uri: Uri, noteId: UUID): BitmapImage? {
        val basename: String
        val mimeType: String?
        val imageFile: File

        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }

        if (
            bitmap != null &&
            (bitmap.width > Constants.DEFAULT_MAX_IMAGE_DIMEN || bitmap.height > Constants.DEFAULT_MAX_IMAGE_DIMEN)
        ) {
            val factor = Constants.DEFAULT_MAX_IMAGE_DIMEN.toFloat() / max(bitmap.width, bitmap.height)
            val width = (bitmap.width * factor).roundToInt()
            val height = (bitmap.height * factor).roundToInt()
            basename = "${UUID.randomUUID()}.png"
            mimeType = "image/png"
            val resized = bitmap.scale(width = width, height = height)
            imageFile = File(imageDir, basename)

            withContext(Dispatchers.IO) {
                FileOutputStream(imageFile).use { outputStream ->
                    resized.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
            }
        } else {
            val extension = context.contentResolver.getType(uri)?.split("/")?.last()
            basename = UUID.randomUUID().toString() + if (extension != null) ".$extension" else ""
            mimeType = context.contentResolver.getType(uri)
            imageFile = File(imageDir, basename)
            copyFileToLocal(context, uri, imageFile)
        }

        if (bitmap != null) {
            val image = Image(
                filename = basename,
                mimeType = mimeType,
                width = bitmap.width,
                height = bitmap.height,
                size = imageFile.length().toInt(),
                noteId = noteId,
                position = bitmapImages.value
                               .filter { it.image.noteId == noteId }
                               .maxOfOrNull { it.image.position } ?: 0,
            )
            return BitmapImage(image, bitmap.asImageBitmap())
        }
        return null
    }
}
