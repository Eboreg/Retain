package us.huseli.retain.syncbackend.tasks

import us.huseli.retain.syncbackend.Engine

class CreateDirTask<ET : Engine>(engine: ET, private val remoteDir: String) :
    OperationTask<ET, OperationTaskResult>(engine) {
    override fun start(onResult: (OperationTaskResult) -> Unit) = engine.createDir(remoteDir, onResult)
}
