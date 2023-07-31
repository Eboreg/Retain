package us.huseli.retain.syncbackend.tasks

import us.huseli.retain.data.entities.NoteCombo
import us.huseli.retain.syncbackend.Engine
import java.io.File
import java.util.UUID

data class TaskLog(val simpleName: String, val totalCount: Int = 1, var finishedCount: Int = 0) {
    val isFinished: Boolean
        get() = finishedCount >= totalCount
}

class SyncTask<ET : Engine>(
    engine: ET,
    private val localCombos: Collection<NoteCombo>,
    private val deletedNoteIds: Collection<UUID>,
    private val onRemoteComboUpdated: (NoteCombo) -> Unit,
    private val localImageDir: File,
) : Task<ET, TaskResult>(engine = engine) {
    private var isCancelled = false
    private var remoteUpdatedCombos: List<NoteCombo> = emptyList()
    private val finishedTasks = listOf(
        TaskLog(DownloadImagesTask::class.java.simpleName, 2),
        TaskLog(DownloadNoteCombosJSONTask::class.java.simpleName),
        TaskLog(UploadNoteCombosTask::class.java.simpleName),
        TaskLog(UploadMissingImagesTask::class.java.simpleName),
        TaskLog(RemoveOrphanImagesTask::class.java.simpleName),
    )
    private var onResult: (TaskResult) -> Unit = {}

    private fun <CRT : TaskResult> runChildTask(
        task: Task<ET, CRT>,
        onChildResult: ((CRT) -> Unit)? = null,
    ) {
        if (!isCancelled) {
            task.run { result ->
                finishedTasks.find { it.simpleName == task.javaClass.simpleName }?.apply { finishedCount++ }
                if (result.status != TaskResult.Status.OK && result.status != TaskResult.Status.PATH_NOT_FOUND) {
                    isCancelled = true
                    engine.isSyncing.value = false
                    onResult(result)
                } else {
                    onChildResult?.invoke(result)
                    if (finishedTasks.all { it.isFinished }) {
                        engine.isSyncing.value = false
                        onResult(result)
                    }
                }
            }
        }
    }

    override fun start(onResult: (TaskResult) -> Unit) {
        this.onResult = onResult
        engine.isSyncing.value = true
        val images = localCombos.flatMap { it.images }.toMutableList()

        runChildTask(DownloadImagesTask(engine, images.filter { !File(localImageDir, it.filename).exists() }))

        runChildTask(DownloadNoteCombosJSONTask(engine, deletedNoteIds)) { downTaskResult ->
            // All notes on remote that either don't exist locally, or
            // have a newer timestamp than their local counterparts:
            @Suppress("destructure")
            remoteUpdatedCombos = downTaskResult.objects.filter { remote ->
                localCombos
                    .find { it.note.id == remote.note.id }
                    ?.let { local -> local.note < remote.note }
                ?: true
            }
            remoteUpdatedCombos.forEach { combo ->
                images.addAll(combo.images)
                // This will save combo to DB:
                onRemoteComboUpdated(combo)
            }
            if (remoteUpdatedCombos.isNotEmpty()) {
                log(
                    message = "${remoteUpdatedCombos.size} new or updated notes synced from ${engine.backend.displayName}.",
                    showInSnackbar = true,
                )
            }
            runChildTask(DownloadImagesTask(engine, remoteUpdatedCombos.flatMap { it.images }))

            // Now upload all notes (i.e. the pre-existing local notes joined with the updated remote ones):
            val combos = localCombos.toSet().union(remoteUpdatedCombos.toSet())
            runChildTask(UploadNoteCombosTask(engine, combos))

            // Upload any images that are missing/wrong on remote:
            val imageFilenames = images.map { it.filename }
            runChildTask(UploadMissingImagesTask(engine, images))

            // Delete any orphan image files, both locally and on Nextcloud:
            localImageDir.listFiles()?.forEach { file ->
                if (!imageFilenames.contains(file.name)) file.delete()
            }
            runChildTask(RemoveOrphanImagesTask(engine, keep = imageFilenames))
        }
    }
}
