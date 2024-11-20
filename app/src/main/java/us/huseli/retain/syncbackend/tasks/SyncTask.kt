package us.huseli.retain.syncbackend.tasks

import us.huseli.retain.dataclasses.NotePojo
import us.huseli.retain.syncbackend.Engine
import us.huseli.retain.syncbackend.tasks.abstr.AbstractTask
import us.huseli.retain.syncbackend.tasks.result.TaskResult
import java.io.File
import java.util.UUID

class SyncTask<ET : Engine>(
    engine: ET,
    private val localPojos: Collection<NotePojo>,
    private val deletedNoteIds: Collection<UUID>,
    private val onRemotePojoUpdated: (NotePojo) -> Unit,
    private val localImageDir: File,
) : AbstractTask<ET, TaskResult>(engine = engine) {
    private var isCancelled = false
    private var remoteUpdatedPojos: List<NotePojo> = emptyList()
    private val finishedTasks = listOf(
        TaskLog(DownloadImagesTask::class.java.simpleName, 2),
        TaskLog(DownloadNoteCombosJSONTask::class.java.simpleName),
        TaskLog(UploadNoteCombosTask::class.java.simpleName),
        TaskLog(UploadMissingImagesTask::class.java.simpleName),
        TaskLog(RemoveOrphanImagesTask::class.java.simpleName),
    )
    private var onResult: (TaskResult) -> Unit = {}

    private fun <CRT : TaskResult> runChildTask(
        task: AbstractTask<ET, CRT>,
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
        val images = localPojos.flatMap { it.images }.toMutableList()

        runChildTask(DownloadImagesTask(engine, images.filter { !File(localImageDir, it.filename).exists() }))

        runChildTask(DownloadNoteCombosJSONTask(engine, deletedNoteIds)) { downTaskResult ->
            // All notes on remote that either don't exist locally, or
            // have a newer timestamp than their local counterparts:
            @Suppress("destructure")
            remoteUpdatedPojos = downTaskResult.objects.filter { remote ->
                localPojos
                    .find { it.note.id == remote.note.id }
                    ?.let { local -> local.note < remote.note } != false
            }
            remoteUpdatedPojos.forEach { pojo ->
                images.addAll(pojo.images)
                // This will save pojo to DB:
                onRemotePojoUpdated(pojo)
            }
            if (remoteUpdatedPojos.isNotEmpty()) {
                log(
                    message = "${remoteUpdatedPojos.size} new or updated notes synced from ${engine.backend.displayName}.",
                    showSnackbar = true,
                )
            }
            runChildTask(DownloadImagesTask(engine, remoteUpdatedPojos.flatMap { it.images }))

            // Now upload all notes (i.e. the pre-existing local notes joined with the updated remote ones):
            val pojos = localPojos.toSet().union(remoteUpdatedPojos.toSet())
            runChildTask(UploadNoteCombosTask(engine, pojos))

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
