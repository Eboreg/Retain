package us.huseli.retain.syncbackend.tasks

import us.huseli.retain.syncbackend.Engine
import java.io.File

data class RemoteFile(val name: String, val size: Long, val isDirectory: Boolean)

open class OperationTaskResult(
    status: Status,
    message: String? = null,
    exception: Exception? = null,
    val remoteFiles: List<RemoteFile> = emptyList(),
    val localFiles: List<File> = emptyList(),
    open val objects: List<Any> = emptyList(),
) : TaskResult(status, message, exception) {
    fun copy(
        status: Status = this.status,
        message: String? = this.message,
        exception: Exception? = this.exception,
        remoteFiles: List<RemoteFile> = this.remoteFiles,
        localFiles: List<File> = this.localFiles,
        objects: List<Any> = this.objects,
    ) = OperationTaskResult(status, message, exception, remoteFiles, localFiles, objects)
}


abstract class OperationTask<ET : Engine, RT : OperationTaskResult>(engine: ET) : Task<ET, RT>(engine)
