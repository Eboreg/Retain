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


