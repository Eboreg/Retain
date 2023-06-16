package us.huseli.retain.syncbackend.tasks

import us.huseli.retain.data.entities.NoteCombo
import us.huseli.retain.syncbackend.Engine
import java.io.File
import java.util.UUID

class SyncTask<ET : Engine>(
    engine: ET,
    private val localCombos: Collection<NoteCombo>,
    private val deletedNoteIds: Collection<UUID>,
    private val onRemoteComboUpdated: (NoteCombo) -> Unit,
    private val localImageDir: File,
) : Task<ET, TaskResult>(engine = engine) {
    private val images = mutableListOf(*localCombos.map { it.images }.flatten().toTypedArray())
    private var downloadNoteCombosJSONTaskFinished = false
    private var downloadMissingImagesTaskFinished = false
    private var downloadNoteImagesTasksFinished = 0
    private var uploadNoteCombosTaskFinished = false
    private var uploadMissingImagesTaskFinished = false
    private var removeOrphanImagesTaskFinished = false
    private var remoteUpdatedCombos: List<NoteCombo> = emptyList()

    private fun isFinished() =
        downloadNoteImagesTasksFinished == remoteUpdatedCombos.size &&
        downloadNoteCombosJSONTaskFinished &&
        uploadNoteCombosTaskFinished &&
        uploadMissingImagesTaskFinished &&
        removeOrphanImagesTaskFinished &&
        downloadMissingImagesTaskFinished

    private fun notifyIfFinished(onResult: (TaskResult) -> Unit) {
        if (isFinished()) onResult(TaskResult(status = TaskResult.Status.OK))
    }

    override fun start(onResult: (TaskResult) -> Unit) {
        DownloadImagesTask(engine, images.filter { !File(localImageDir, it.filename).exists() }).run {
            downloadMissingImagesTaskFinished = true
            notifyIfFinished(onResult)
        }

        DownloadNoteCombosJSONTask(engine, deletedNoteIds).run { downTaskResult ->
            val remoteCombos = downTaskResult.objects
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
                DownloadImagesTask(engine, combo.images).run(
                    onEachCallback = { _, result ->
                        if (!result.success) logError("Failed to download image: ${result.message}")
                    },
                    onReadyCallback = {
                        downloadNoteImagesTasksFinished++
                        notifyIfFinished(onResult)
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
                if (!result.success) logError("Failed to upload notes: ${result.message}")
                uploadNoteCombosTaskFinished = true
                notifyIfFinished(onResult)
            }

            // Upload any images that are missing/wrong on remote:
            val imageFilenames = images.map { it.filename }

            UploadMissingImagesTask(engine, images).run { result ->
                if (!result.success) logError("Failed to upload image: ${result.message}")
                uploadMissingImagesTaskFinished = true
                notifyIfFinished(onResult)
            }

            // Delete any orphan image files, both locally and on Nextcloud:
            localImageDir.listFiles()?.forEach { file ->
                if (!imageFilenames.contains(file.name)) file.delete()
            }
            RemoveOrphanImagesTask(engine, keep = imageFilenames).run {
                removeOrphanImagesTaskFinished = true
                notifyIfFinished(onResult)
            }

            notifyIfFinished(onResult)
        }
    }
}
