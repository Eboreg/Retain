package us.huseli.retain.syncbackend.tasks

import us.huseli.retain.syncbackend.Engine
import us.huseli.retain.syncbackend.tasks.abstr.AbstractOperationTask
import us.huseli.retain.syncbackend.tasks.result.OperationTaskResult

class CreateDirTask<ET : Engine>(engine: ET, private val remoteDir: String) :
    AbstractOperationTask<ET, OperationTaskResult>(engine) {
    override fun start(onResult: (OperationTaskResult) -> Unit) = engine.createDir(remoteDir, onResult)
}
