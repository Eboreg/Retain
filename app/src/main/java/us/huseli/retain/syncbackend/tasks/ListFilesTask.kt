package us.huseli.retain.syncbackend.tasks

import us.huseli.retain.syncbackend.Engine

/** List: arbitrary files */
abstract class ListFilesTask<ET : Engine>(
    engine: ET,
    private val remoteDir: String,
    protected val filter: ((RemoteFile) -> Boolean)? = null,
) : OperationTask<ET, OperationTaskResult>(engine) {
    protected val remoteFiles = mutableListOf<String>()

    abstract fun getResultForEmptyList(): OperationTaskResult

    override fun start(onResult: (OperationTaskResult) -> Unit) = engine.listFiles(remoteDir, filter, onResult)
}
