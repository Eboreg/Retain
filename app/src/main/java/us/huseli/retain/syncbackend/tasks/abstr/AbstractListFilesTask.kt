package us.huseli.retain.syncbackend.tasks.abstr

import us.huseli.retain.syncbackend.Engine
import us.huseli.retain.syncbackend.tasks.RemoteFile
import us.huseli.retain.syncbackend.tasks.result.OperationTaskResult

/** List: arbitrary files */
abstract class AbstractListFilesTask<ET : Engine>(
    engine: ET,
    private val remoteDir: String,
    protected val filter: (RemoteFile) -> Boolean,
) : AbstractOperationTask<ET, OperationTaskResult>(engine) {
    protected val remoteFiles = mutableListOf<String>()

    abstract fun getResultForEmptyList(): OperationTaskResult

    override fun start(onResult: (OperationTaskResult) -> Unit) = engine.listFiles(remoteDir, filter, onResult)
}
