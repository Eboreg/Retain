package us.huseli.retain.nextcloud.tasks

import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import us.huseli.retain.LogMessage
import us.huseli.retain.nextcloud.NextCloudEngine

open class OperationTaskResult(
    success: Boolean,
    error: LogMessage? = null,
    val remoteOperationResult: RemoteOperationResult<*>? = null
) : TaskResult(success, error)

abstract class BaseOperationTask<RT : OperationTaskResult>(engine: NextCloudEngine) : BaseTask<RT>(engine) {
    abstract val remoteOperation: RemoteOperation<*>
    var remoteOperationResult: RemoteOperationResult<*>? = null

    open fun onSuccessfulRemoteOperation(remoteOperationResult: RemoteOperationResult<*>) {
        notifyIfReady()
    }

    open fun onUnsuccessfulRemoteOperation(remoteOperationResult: RemoteOperationResult<*>) {
        failWithMessage(remoteOperationResult.logMessage ?: remoteOperationResult.message)
    }

    override fun isReady() = remoteOperationResult != null

    open fun isRemoteOperationSuccessful(remoteOperationResult: RemoteOperationResult<*>) =
        remoteOperationResult.isSuccess

    override fun start() {
        remoteOperation.execute(
            engine.client,
            { _, result ->
                this.remoteOperationResult = result
                if (isRemoteOperationSuccessful(result))
                    onSuccessfulRemoteOperation(result)
                else
                    onUnsuccessfulRemoteOperation(result)
            },
            engine.listenerHandler
        )
    }
}

abstract class OperationTask(engine: NextCloudEngine) : BaseOperationTask<OperationTaskResult>(engine) {
    override fun getResult() = OperationTaskResult(success, error, remoteOperationResult)
}
