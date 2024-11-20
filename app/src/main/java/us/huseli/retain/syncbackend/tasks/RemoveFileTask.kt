package us.huseli.retain.syncbackend.tasks

import us.huseli.retain.syncbackend.Engine
import us.huseli.retain.syncbackend.tasks.abstr.AbstractOperationTask
import us.huseli.retain.syncbackend.tasks.result.OperationTaskResult

/** Remove: 1 arbitrary file */
open class RemoveFileTask<ET : Engine>(engine: ET, private val remotePath: String) :
    AbstractOperationTask<ET, OperationTaskResult>(engine) {
    override val successMessageString = "Successfully removed $remotePath"
    override fun start(onResult: (OperationTaskResult) -> Unit) = engine.removeFile(remotePath, onResult)
}
