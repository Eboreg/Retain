package us.huseli.retain.data

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.graphics.asImageBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.retain.Constants.IMAGE_SUBDIR
import us.huseli.retain.Enums.NoteType
import us.huseli.retain.LogInterface
import us.huseli.retain.Logger
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.Image
import us.huseli.retain.data.entities.ImageWithBitmap
import us.huseli.retain.data.entities.Note
import us.huseli.retain.data.entities.NoteCombined
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
    private val nextCloud: NextCloud,
    private val ioScope: CoroutineScope,
    override val logger: Logger,
    private val database: Database,
) : LogInterface {
    val imageDir = File(context.filesDir, IMAGE_SUBDIR).apply { mkdirs() }
    val notes: Flow<List<Note>> = noteDao.flowList()
    val checklistItems: Flow<List<ChecklistItem>> = checklistItemDao.flowList()
    val nextCloudNeedsTesting = MutableStateFlow(true)

    init {
        syncNextCloud()
    }

    suspend fun deleteChecklistItem(item: ChecklistItem) {
        checklistItemDao.delete(item)
        noteDao.touch(item.noteId)
    }

    suspend fun deleteImage(image: Image) {
        imageDao.delete(image)
        noteDao.touch(image.noteId)
        File(imageDir, image.filename).delete()
        nextCloud.RemoveImageTask(image).run()
    }

    suspend fun deleteNotes(ids: Collection<UUID>) {
        val images = imageDao.list(ids)
        @Suppress("Destructure")
        images.forEach { image ->
            File(imageDir, image.filename).delete()
        }
        noteDao.delete(ids)
        nextCloud.RemoveNotesAndImagesTask(ids, images).run()
    }

    fun flowImagesWithBitmap(noteId: UUID? = null): Flow<List<ImageWithBitmap>> {
        val flowList = if (noteId != null) imageDao.flowList(noteId) else imageDao.flowList()

        return flowList.map { images ->
            val imagesWithBitmap = mutableListOf<ImageWithBitmap>()

            images.forEach { image ->
                Uri.fromFile(File(imageDir, image.filename))?.let { uri ->
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                    bitmap?.let {
                        imagesWithBitmap.add(ImageWithBitmap(image, it.asImageBitmap()))
                    }
                }
            }
            imagesWithBitmap
        }
    }

    fun flowNote(id: UUID): Flow<Note?> = noteDao.flow(id)

    suspend fun insertChecklistItem(noteId: UUID, text: String, checked: Boolean, position: Int) {
        checklistItemDao.makePlaceFor(noteId, position)
        checklistItemDao.insert(UUID.randomUUID(), noteId, text, checked, position)
        noteDao.touch(noteId)
    }

    suspend fun insertImage(noteId: UUID, filename: String, mimeType: String?, width: Int?, height: Int?, size: Int) {
        imageDao.insert(noteId, filename, mimeType, width, height, size)
        noteDao.touch(noteId)
        imageDao.get(filename)?.let { image ->
            nextCloud.UploadImageTask(image).run()
        }
    }

    suspend fun listChecklistItems(noteId: UUID): List<ChecklistItem> = checklistItemDao.listByNoteId(noteId)

    private fun syncNextCloud() {
        nextCloud.DownstreamSyncTask().run { downTask ->
            // Remote files have been fetched and parsed; now update DB where needed.
            ioScope.launch {
                val remoteNotesMap = downTask.remoteNotesCombined.associateBy { it.id }
                val notes = noteDao.list()
                val checklistItems = checklistItemDao.list()
                val images = imageDao.list()

                val localNotesMap = notes.map { note ->
                    NoteCombined(
                        note = note,
                        checklistItems = checklistItems.filter { it.noteId == note.id },
                        images = images.filter { it.noteId == note.id },
                        databaseVersion = database.openHelper.readableDatabase.version,
                    )
                }.associateBy { it.id }

                // All notes on remote that either don't exist locally, or
                // have a newer timestamp than their local counterparts:
                val remoteUpdated = remoteNotesMap.filter { (id, remote) ->
                    localNotesMap[id]?.let { local -> local < remote } ?: true
                }.values

                // All local notes that either don't exist on remote, or
                // have a newer timestamp than their remote counterparts:
                val localUpdated = localNotesMap.filter { (id, local) ->
                    remoteNotesMap[id]?.let { remote -> remote < local } ?: true
                }.values

                log("New or updated on remote: $remoteUpdated")
                log("New or updated locally: $localUpdated")

                remoteUpdated.forEach {
                    noteDao.upsert(it)
                    checklistItemDao.replace(it.id, it.checklistItems)
                    imageDao.replace(it.id, it.images)
                    nextCloud.DownloadNoteImagesTask(it).run()
                }
                if (remoteUpdated.isNotEmpty()) {
                    log(
                        message = "${remoteUpdated.size} new or updated notes synced from Nextcloud.",
                        showInSnackbar = true,
                    )
                }

                // Now upload all notes that are new or updated locally:
                nextCloud.UpstreamSyncTask(localUpdated).run()

                syncImages()
            }
        }
    }

    private suspend fun syncImages() {
        val images = imageDao.list()
        val imageFilenames = images.map { it.filename }

        // Upload any images that are missing/wrong on remote:
        nextCloud.UploadMissingImagesTask(images).run()
        // Also download any locally missing images:
        val missingImages = images.filter { !File(imageDir, it.filename).exists() }
        nextCloud.DownloadMissingImagesTask(missingImages).run()
        // Delete any orphan image files, both locally and on Nextcloud:
        imageDir.listFiles()?.forEach { file ->
            if (!imageFilenames.contains(file.name)) {
                file.delete()
            }
        }
        nextCloud.RemoveOrphanImagesTask(keep = imageFilenames).run()
    }

    fun testNextcloud(uri: Uri, username: String, password: String, onResult: (NextCloudTestResult) -> Unit) {
        nextCloud.testClient(uri, username, password) { result -> onResult(result) }
    }

    suspend fun updateChecklistItems(items: Collection<ChecklistItem>) = checklistItemDao.update(items)

    suspend fun upsertNote(id: UUID, type: NoteType, title: String, text: String, showChecked: Boolean, colorIdx: Int) {
        noteDao.upsert(id, type, title, text, showChecked, colorIdx)
        noteDao.get(id)?.let { note ->
            nextCloud.UploadNoteTask(
                noteCombined = NoteCombined(
                    note = note,
                    checklistItems = checklistItemDao.listByNoteId(id),
                    images = imageDao.list(id),
                    databaseVersion = database.openHelper.readableDatabase.version
                )
            ).run()
        }
    }
}
