package us.huseli.retain.syncbackend.tasks.result

import us.huseli.retain.syncbackend.tasks.RemoteFile
import java.io.File

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