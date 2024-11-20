package us.huseli.retain.syncbackend.tasks

import us.huseli.retain.Constants.SYNCBACKEND_IMAGE_SUBDIR
import us.huseli.retain.syncbackend.Engine
import us.huseli.retain.syncbackend.tasks.abstr.AbstractListFilesListTask
import us.huseli.retain.syncbackend.tasks.result.OperationTaskResult
import us.huseli.retain.syncbackend.tasks.result.TaskResult

class RemoveOrphanImagesTask<ET : Engine>(engine: ET, private val keep: List<String>) :
    AbstractListFilesListTask<ET, OperationTaskResult, RemoveFileTask<ET>>(
        engine = engine,
        remoteDir = engine.getAbsolutePath(SYNCBACKEND_IMAGE_SUBDIR),
        filter = { (name, _, isDirectory) -> !isDirectory && !keep.contains(name.split("/").last()) },
    ) {
    override val failOnUnsuccessfulChildTask = false

    override fun getChildTask(remoteFile: RemoteFile) = RemoveFileTask(engine, remoteFile.name)

    override fun processChildTaskResult(
        remoteFile: RemoteFile,
        result: OperationTaskResult,
        childResult: OperationTaskResult,
        onResult: (OperationTaskResult) -> Unit
    ) {
        if (successfulRemoteFiles.size + unsuccessfulRemoteFiles.size == result.remoteFiles.size) onResult(result)
    }

    override fun getResultForEmptyList() = OperationTaskResult(status = TaskResult.Status.OK)
}
