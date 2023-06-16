package us.huseli.retain.syncbackend.tasks

import us.huseli.retain.syncbackend.Engine
import java.io.File

/** Up: 1 arbitrary file */
open class UploadFileTask<ET : Engine>(
    engine: ET,
    private val remotePath: String,
    private val localFile: File,
    private val mimeType: String? = null,
) : OperationTask<ET, OperationTaskResult>(engine) {
    override val successMessageString = "Successfully saved $localFile to $remotePath on Nextcloud"

    override fun start(onResult: (OperationTaskResult) -> Unit) {
        if (!localFile.isFile)
            onResult(OperationTaskResult(status = TaskResult.Status.OTHER_ERROR, message = "$localFile is not a file"))
        else
            engine.uploadFile(localFile, remotePath, mimeType, onResult)
    }
}
