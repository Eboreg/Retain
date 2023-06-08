package us.huseli.retain.nextcloud.tasks

import com.owncloud.android.lib.resources.files.RemoveFileRemoteOperation
import us.huseli.retain.nextcloud.NextCloudEngine

/** Remove: 1 arbitrary file */
open class RemoveFileTask(engine: NextCloudEngine, remotePath: String) : OperationTask(engine) {
    override val remoteOperation = RemoveFileRemoteOperation(remotePath)
    override val successMessageString = "Successfully removed $remotePath from Nextcloud"
}
