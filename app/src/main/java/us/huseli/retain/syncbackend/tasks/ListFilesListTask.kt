package us.huseli.retain.syncbackend.tasks

import us.huseli.retain.syncbackend.Engine
import us.huseli.retain.syncbackend.Engine.Companion.STATUS_OK

abstract class ListFilesListTask<ET : Engine, CRT : OperationTaskResult, CT : Task<ET, CRT>>(
    engine: ET,
    remoteDir: String,
    filter: (RemoteFile) -> Boolean,
) : ListFilesTask<ET>(engine, remoteDir, filter) {
    private var onEachCallback: ((RemoteFile, CRT) -> Unit)? = null
    protected val successfulRemoteFiles = mutableListOf<RemoteFile>()
    protected val unsuccessfulRemoteFiles = mutableListOf<RemoteFile>()
    protected open val failOnUnsuccessfulChildTask = true
    override val isMetaTask = true

    abstract fun getChildTask(remoteFile: RemoteFile): CT?
    abstract fun processChildTaskResult(
        remoteFile: RemoteFile,
        result: OperationTaskResult,
        childResult: CRT,
        onResult: (OperationTaskResult) -> Unit
    )

    override fun start(onResult: (OperationTaskResult) -> Unit) {
        super.start { result ->
            if (result.remoteFiles.isNotEmpty()) {
                result.remoteFiles.forEach { remoteFile ->
                    getChildTask(remoteFile)?.run { childResult ->
                        if (childResult.success) successfulRemoteFiles.add(remoteFile)
                        else unsuccessfulRemoteFiles.add(remoteFile)
                        processChildTaskResult(remoteFile, result, childResult, onResult)
                        onEachCallback?.invoke(remoteFile, childResult)
                    }
                }
            } else onResult(getResultForEmptyList())
        }
    }

    fun run(
        triggerStatus: Int = STATUS_OK,
        onEachCallback: ((RemoteFile, CRT) -> Unit)?,
        onReadyCallback: ((OperationTaskResult) -> Unit)?
    ) {
        this.onEachCallback = onEachCallback
        super.run(triggerStatus, onReadyCallback)
    }
}
