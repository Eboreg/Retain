package us.huseli.retain.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import us.huseli.retain.Constants
import us.huseli.retain.LogInterface
import us.huseli.retain.Logger
import us.huseli.retain.data.entities.Image
import us.huseli.retain.nextcloud.NextCloudEngine
import us.huseli.retain.nextcloud.tasks.DownloadMissingImagesTask
import us.huseli.retain.nextcloud.tasks.DownloadNoteCombosJSONTask
import us.huseli.retain.nextcloud.tasks.DownloadNoteImagesTask
import us.huseli.retain.nextcloud.tasks.RemoveImagesTask
import us.huseli.retain.nextcloud.tasks.RemoveNotesTask
import us.huseli.retain.nextcloud.tasks.RemoveOrphanImagesTask
import us.huseli.retain.nextcloud.tasks.TestNextCloudTaskResult
import us.huseli.retain.nextcloud.tasks.UploadMissingImagesTask
import us.huseli.retain.nextcloud.tasks.UploadNoteCombosTask
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NextCloudRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageDao: ImageDao,
    private val nextCloudEngine: NextCloudEngine,
    private val noteDao: NoteDao,
    private val checklistItemDao: ChecklistItemDao,
    private val ioScope: CoroutineScope,
    private val database: Database,
    override val logger: Logger,
) : SharedPreferences.OnSharedPreferenceChangeListener, LogInterface {
    private val imageDir = File(context.filesDir, Constants.IMAGE_SUBDIR).apply { mkdirs() }
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    init {
        sync()
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == Constants.PREF_NEXTCLOUD_BASE_DIR) sync()
    }

    @Suppress("unused")
    fun deleteNotes(ids: Collection<UUID>, images: Collection<Image>) {
        RemoveNotesTask(nextCloudEngine, ids).run()
        RemoveImagesTask(nextCloudEngine, images).run()
    }

    private suspend fun syncImages() {
        val images = imageDao.listAll()
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

    private fun sync() {
        // First try to download any locally missing images, because this has
        // highest prio:
        ioScope.launch {
            val images = imageDao.listAll()
            val missingImages = images.filter { !File(imageDir, it.filename).exists() }

            DownloadMissingImagesTask(nextCloudEngine, missingImages).run()
        }

        DownloadNoteCombosJSONTask(nextCloudEngine).run { downTaskResult ->
            if (!downTaskResult.success) logError("Failed to download notes from Nextcloud", downTaskResult.error)

            ioScope.launch {
                val remoteCombos = downTaskResult.objects ?: emptyList()
                val localCombos = noteDao.listAllCombos().map {
                    it.copy(databaseVersion = database.openHelper.readableDatabase.version)
                }

                // All notes on remote that either don't exist locally, or
                // have a newer timestamp than their local counterparts:
                @Suppress("destructure")
                val remoteUpdated = remoteCombos.filter { remote ->
                    localCombos
                        .find { it.note.id == remote.note.id }
                        ?.let { local -> local.note < remote.note }
                    ?: true
                }

                // All local notes that either don't exist on remote, or
                // have a newer timestamp than their remote counterparts:
                @Suppress("destructure")
                val localUpdated = localCombos.filter { local ->
                    remoteCombos
                        .find { it.note.id == local.note.id }
                        ?.let { remote -> remote.note < local.note }
                    ?: true
                }

                remoteUpdated.forEach {
                    noteDao.upsert(it.note)
                    checklistItemDao.replace(it.note.id, it.checklistItems)
                    imageDao.replace(it.note.id, it.images)
                    DownloadNoteImagesTask(nextCloudEngine, it).run(
                        onEachCallback = { _, result ->
                            if (!result.success)
                                logError("Failed to download image from Nextcloud", result.error)
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
                UploadNoteCombosTask(nextCloudEngine, localUpdated).run { result ->
                    if (!result.success) logError("Failed to upload notes to Nextcloud", result.error)
                }

                syncImages()
            }
        }
    }

    fun removeImages(images: Collection<Image>) = RemoveImagesTask(nextCloudEngine, images).run()

    fun test(
        uri: Uri,
        username: String,
        password: String,
        baseDir: String,
        onResult: (TestNextCloudTaskResult) -> Unit
    ) = nextCloudEngine.testClient(uri, username, password, baseDir) { result -> onResult(result) }

    suspend fun uploadNotes() {
        val combos = noteDao.listAllCombos()
        UploadNoteCombosTask(
            engine = nextCloudEngine,
            combos = combos.map { it.copy(databaseVersion = database.openHelper.readableDatabase.version) },
        ).run { result ->
            if (!result.success) logError("Failed to upload note(s) to Nextcloud", result.error)
        }
    }
}
