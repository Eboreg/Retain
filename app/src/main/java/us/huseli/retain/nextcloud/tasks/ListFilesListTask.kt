package us.huseli.retain.nextcloud.tasks

import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.model.RemoteFile
import us.huseli.retain.nextcloud.NextCloudEngine

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
