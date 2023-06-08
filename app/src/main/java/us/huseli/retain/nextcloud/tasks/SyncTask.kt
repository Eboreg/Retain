package us.huseli.retain.nextcloud.tasks

import us.huseli.retain.data.entities.NoteCombo
import us.huseli.retain.nextcloud.NextCloudEngine
import java.io.File
import java.util.UUID

class SyncTask(
    engine: NextCloudEngine,
    private val localCombos: Collection<NoteCombo>,
    private val deletedNoteIds: Collection<UUID>,
    private val onRemoteComboUpdated: (NoteCombo) -> Unit,
    private val localImageDir: File,
) : BaseTask<TaskResult>(engine = engine) {
    private val images = mutableListOf(*localCombos.map { it.images }.flatten().toTypedArray())
    private var downloadNoteCombosJSONTaskFinished = false
    private var downloadMissingImagesTaskFinished = false
    private var downloadNoteImagesTasksFinished = 0
    private var uploadNoteCombosTaskFinished = false
    private var uploadMissingImagesTaskFinished = false
    private var removeOrphanImagesTaskFinished = false
    private var remoteUpdatedCombos: List<NoteCombo> = emptyList()

    override fun getResult() = TaskResult(success, error)

    override fun isFinished() =
        downloadNoteImagesTasksFinished == remoteUpdatedCombos.size &&
        downloadNoteCombosJSONTaskFinished &&
        uploadNoteCombosTaskFinished &&
        uploadMissingImagesTaskFinished &&
        removeOrphanImagesTaskFinished &&
        downloadMissingImagesTaskFinished

    override fun start() {
        DownloadMissingImagesTask(engine, images.filter { !File(localImageDir, it.filename).exists() }).run {
            downloadMissingImagesTaskFinished = true
        }

        DownloadNoteCombosJSONTask(engine, deletedNoteIds).run { downTaskResult ->
            val remoteCombos = downTaskResult.objects ?: emptyList()
            downloadNoteCombosJSONTaskFinished = true

            // All notes on remote that either don't exist locally, or
            // have a newer timestamp than their local counterparts:
            @Suppress("destructure")
            remoteUpdatedCombos = remoteCombos.filter { remote ->
                localCombos
                    .find { it.note.id == remote.note.id }
                    ?.let { local -> local.note < remote.note }
                ?: true
            }

            remoteUpdatedCombos.forEach { combo ->
                images.addAll(combo.images)
                onRemoteComboUpdated(combo)
                DownloadNoteImagesTask(engine, combo).run(
                    onEachCallback = { _, result ->
                        if (!result.success) logError("Failed to download image from Nextcloud", result.error)
                    },
                    onReadyCallback = {
                        downloadNoteImagesTasksFinished++
                        notifyIfFinished()
                    }
                )
            }

            if (remoteUpdatedCombos.isNotEmpty()) {
                log(
                    message = "${remoteUpdatedCombos.size} new or updated notes synced from Nextcloud.",
                    showInSnackbar = true,
                )
            }

            // Now upload all notes (i.e. the pre-existing local notes joined with the updated remote ones):
            val combos = localCombos.toSet().union(remoteUpdatedCombos.toSet())
            UploadNoteCombosTask(engine, combos).run { result ->
                if (!result.success) logError("Failed to upload notes to Nextcloud", result.error)
                uploadNoteCombosTaskFinished = true
                notifyIfFinished()
            }

            // Upload any images that are missing/wrong on remote:
            val imageFilenames = images.map { it.filename }

            UploadMissingImagesTask(engine, images).run { result ->
                if (!result.success) logError("Failed to upload image to Nextcloud", result.error)
                uploadMissingImagesTaskFinished = true
                notifyIfFinished()
            }

            // Delete any orphan image files, both locally and on Nextcloud:
            localImageDir.listFiles()?.forEach { file ->
                if (!imageFilenames.contains(file.name)) file.delete()
            }
            RemoveOrphanImagesTask(engine, keep = imageFilenames).run {
                removeOrphanImagesTaskFinished = true
                notifyIfFinished()
            }
        }
    }
}
