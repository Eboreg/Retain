package us.huseli.retain.syncbackend.tasks

import us.huseli.retain.syncbackend.Engine

/** Remove: 1 arbitrary file */
open class RemoveFileTask<ET : Engine>(engine: ET, private val remotePath: String) :
    OperationTask<ET, OperationTaskResult>(engine) {
    override val successMessageString = "Successfully removed $remotePath from Nextcloud"
    override fun start(onResult: (OperationTaskResult) -> Unit) = engine.removeFile(remotePath, onResult)
}
