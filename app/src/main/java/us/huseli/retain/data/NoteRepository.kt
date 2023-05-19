package us.huseli.retain.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import us.huseli.retain.Constants.IMAGE_SUBDIR
import us.huseli.retain.Enums.NoteType
import us.huseli.retain.LogInterface
import us.huseli.retain.Logger
import us.huseli.retain.data.entities.BitmapImage
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.Image
import us.huseli.retain.data.entities.Note
import us.huseli.retain.data.entities.NoteCombined
import us.huseli.retain.nextcloud.NextCloudEngine
import us.huseli.retain.nextcloud.tasks.DownloadMissingImagesTask
import us.huseli.retain.nextcloud.tasks.DownloadNoteImagesTask
import us.huseli.retain.nextcloud.tasks.DownstreamSyncTask
import us.huseli.retain.nextcloud.tasks.RemoveImageTask
import us.huseli.retain.nextcloud.tasks.RemoveImagesTask
import us.huseli.retain.nextcloud.tasks.RemoveNotesTask
import us.huseli.retain.nextcloud.tasks.RemoveOrphanImagesTask
import us.huseli.retain.nextcloud.tasks.TestNextCloudTaskResult
import us.huseli.retain.nextcloud.tasks.UploadImageTask
import us.huseli.retain.nextcloud.tasks.UploadMissingImagesTask
import us.huseli.retain.nextcloud.tasks.UploadNoteTask
import us.huseli.retain.nextcloud.tasks.UpstreamSyncTask
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
    private val nextCloudEngine: NextCloudEngine,
    private val ioScope: CoroutineScope,
    override val logger: Logger,
    private val database: Database,
) : LogInterface {
    val imageDir = File(context.filesDir, IMAGE_SUBDIR).apply { mkdirs() }
    val notes: Flow<List<Note>> = noteDao.flowList()
    val checklistItems: Flow<List<ChecklistItem>> = checklistItemDao.flowList()
    val nextCloudNeedsTesting = MutableStateFlow(true)
    val bitmapImages = MutableStateFlow<List<BitmapImage>>(emptyList())

    init {
        syncNextCloud()

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

    private fun addDownloadedImage(image: Image) {
        image.toBitmapImage(context)?.let {
            val newBitmapImages = bitmapImages.value.toMutableList()

            newBitmapImages.add(it)
            bitmapImages.value = newBitmapImages
        }
    }

    suspend fun deleteChecklistItem(item: ChecklistItem) {
        checklistItemDao.delete(item)
        noteDao.touch(item.noteId)
    }

    suspend fun deleteImage(image: Image) {
        imageDao.delete(image)
        noteDao.touch(image.noteId)
        File(imageDir, image.filename).delete()
        RemoveImageTask(nextCloudEngine, image).run()
    }

    suspend fun deleteNotes(ids: Collection<UUID>) {
        val images = imageDao.list(ids)
        @Suppress("Destructure")
        images.forEach { image ->
            File(imageDir, image.filename).delete()
        }
        noteDao.delete(ids)
        RemoveNotesTask(nextCloudEngine, ids).run()
        RemoveImagesTask(nextCloudEngine, images).run()
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
            UploadImageTask(nextCloudEngine, image).run { result ->
                if (!result.success) logError("Failed to upload image to Nextcloud", result.error)
            }
        }
    }

    suspend fun listChecklistItems(noteId: UUID): List<ChecklistItem> = checklistItemDao.listByNoteId(noteId)

    private fun syncNextCloud() {
        // First try to download any locally missing images, because this has
        // highest prio:
        ioScope.launch {
            val images = imageDao.list()
            val missingImages = images.filter { !File(imageDir, it.filename).exists() }

            DownloadMissingImagesTask(nextCloudEngine, missingImages).run(
                onEachCallback = { image, result -> if (result.success) addDownloadedImage(image) },
                onReadyCallback = null,
            )
        }

        DownstreamSyncTask(nextCloudEngine).run { downTaskResult ->
            if (!downTaskResult.success) logError("Failed to download notes from Nextcloud", downTaskResult.error)

            // Remote files have been fetched and parsed; now update DB where needed.
            ioScope.launch {
                val remoteNotesMap = downTaskResult.remoteNotesCombined.associateBy { it.id }
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
                    DownloadNoteImagesTask(nextCloudEngine, it).run(
                        onEachCallback = { image, result ->
                            if (result.success) addDownloadedImage(image)
                            else logError("Failed to download image from Nextcloud", result.error)
                        },
                        onReadyCallback = null
                    )
                }
                if (remoteUpdated.isNotEmpty()) {
                    log(
                        message = "${remoteUpdated.size} new or updated notes synced from Nextcloud.",
                        showInSnackbar = true,
                    )
                }

                // Now upload all notes that are new or updated locally:
                UpstreamSyncTask(nextCloudEngine, localUpdated).run { result ->
                    if (!result.success) {
                        if (result.unsuccessfulCount > 0)
                            logError("Failed to upload ${result.unsuccessfulCount} notes to Nextcloud", result.error)
                        else
                            logError("Failed to upload notes to Nextcloud", result.error)
                    }
                }

                syncImages()
            }
        }
    }

    private suspend fun syncImages() {
        val images = imageDao.list()
        val imageFilenames = images.map { it.filename }

        // Upload any images that are missing/wrong on remote:
        UploadMissingImagesTask(nextCloudEngine, images).run { result ->
            if (!result.success) logError("Failed to upload image to Nextcloud", result.error)
        }
        // Delete any orphan image files, both locally and on Nextcloud:
        imageDir.listFiles()?.forEach { file ->
            if (!imageFilenames.contains(file.name)) file.delete()
        }
        RemoveOrphanImagesTask(nextCloudEngine, keep = imageFilenames).run()
    }

    fun testNextcloud(uri: Uri, username: String, password: String, onResult: (TestNextCloudTaskResult) -> Unit) {
        nextCloudEngine.testClient(uri, username, password) { result -> onResult(result) }
    }

    suspend fun updateChecklistItems(items: Collection<ChecklistItem>) = checklistItemDao.update(items)

    suspend fun upsertNote(id: UUID, type: NoteType, title: String, text: String, showChecked: Boolean, colorIdx: Int) {
        noteDao.upsert(id, type, title, text, showChecked, colorIdx)
        noteDao.get(id)?.let { note ->
            UploadNoteTask(
                nextCloudEngine,
                noteCombined = NoteCombined(
                    note = note,
                    checklistItems = checklistItemDao.listByNoteId(id),
                    images = imageDao.list(id),
                    databaseVersion = database.openHelper.readableDatabase.version
                )
            ).run { result ->
                if (!result.success) logError("Failed to upload note to Nextcloud", result.error)
            }
        }
    }
}
