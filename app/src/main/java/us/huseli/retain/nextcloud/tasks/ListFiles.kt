package us.huseli.retain.nextcloud.tasks

import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import us.huseli.retain.LogMessage
import us.huseli.retain.nextcloud.NextCloudEngine

open class ListFilesTaskResult(
    success: Boolean,
    error: LogMessage?,
    remoteOperationResult: RemoteOperationResult<*>?,
    val remoteFiles: List<RemoteFile>,
) : OperationTaskResult(success, error, remoteOperationResult)

/** List: arbitrary files */
abstract class BaseListFilesTask<RT : ListFilesTaskResult>(
    engine: NextCloudEngine,
    remoteDir: String,
    protected val filter: (RemoteFile) -> Boolean,
) : BaseOperationTask<RT>(engine) {
    override val remoteOperation = ReadFolderRemoteOperation(remoteDir)
    protected val remoteFiles = mutableListOf<RemoteFile>()

    override fun onSuccessfulRemoteOperation(remoteOperationResult: RemoteOperationResult<*>) {
        @Suppress("DEPRECATION")
        remoteFiles.addAll(remoteOperationResult.data.filterIsInstance<RemoteFile>().filter(filter))
        super.onSuccessfulRemoteOperation(remoteOperationResult)
    }
}

open class ListFilesTask(engine: NextCloudEngine, remoteDir: String, filter: (RemoteFile) -> Boolean) :
    BaseListFilesTask<ListFilesTaskResult>(engine, remoteDir, filter) {
    override fun getResult() = ListFilesTaskResult(
        success = success,
        error = error,
        remoteOperationResult = remoteOperationResult,
        remoteFiles = remoteFiles,
    )
}


abstract class ListFilesListTask<RT : ListFilesTaskResult, CRT : TaskResult, CT : BaseTask<CRT>>(
    engine: NextCloudEngine,
    remoteDir: String,
    filter: (RemoteFile) -> Boolean
) : BaseListFilesTask<RT>(engine, remoteDir, filter) {
    private var onEachCallback: ((RemoteFile, CRT) -> Unit)? = null
    private val successfulRemoteFiles = mutableListOf<RemoteFile>()
    private val unsuccessfulRemoteFiles = mutableListOf<RemoteFile>()
    protected open val failOnUnsuccessfulChildTask = true
    override val isMetaTask = true

    abstract fun getChildTask(remoteFile: RemoteFile): CT?

    override fun isFinished() =
        super.isFinished() &&
        (
            (successfulRemoteFiles.size + unsuccessfulRemoteFiles.size == remoteFiles.size) ||
            (failOnUnsuccessfulChildTask && !success)
        )

    @Suppress("unused")
    fun run(
        triggerStatus: Int = NextCloudEngine.STATUS_OK,
        onEachCallback: ((RemoteFile, CRT) -> Unit)?,
        onReadyCallback: ((RT) -> Unit)?
    ) {
        this.onEachCallback = onEachCallback
        super.run(triggerStatus, onReadyCallback)
    }

    override fun onSuccessfulRemoteOperation(remoteOperationResult: RemoteOperationResult<*>) {
        @Suppress("DEPRECATION")
        remoteFiles.addAll(remoteOperationResult.data.filterIsInstance<RemoteFile>().filter(filter))
        remoteFiles.forEach { remoteFile ->
            getChildTask(remoteFile)?.run(triggerStatus) { result ->
                if (result.success) {
                    successfulRemoteFiles.add(remoteFile)
                } else {
                    unsuccessfulRemoteFiles.add(remoteFile)
                    if (failOnUnsuccessfulChildTask) failWithMessage(result.error)
                }
                onEachCallback?.invoke(remoteFile, result)
                notifyIfFinished()
            }
        }
        notifyIfFinished()
    }
}