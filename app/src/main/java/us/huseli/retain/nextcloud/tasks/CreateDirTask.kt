package us.huseli.retain.nextcloud.tasks

import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation
import us.huseli.retain.nextcloud.NextCloudEngine

/** Create: 1 arbitrary directory */
class CreateDirTask(engine: NextCloudEngine, remoteDir: String) : OperationTask(engine) {
    override val remoteOperation = CreateFolderRemoteOperation(remoteDir, true)

    override fun isRemoteOperationSuccessful(remoteOperationResult: RemoteOperationResult<*>): Boolean {
        /** A little more lax than parent implementation. */
        return remoteOperationResult.isSuccess || remoteOperationResult.code == RemoteOperationResult.ResultCode.FOLDER_ALREADY_EXISTS
    }
}
