package us.huseli.retain.nextcloud.tasks

import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation
import us.huseli.retain.nextcloud.NextCloudEngine
import java.io.File

/** Up: 1 arbitrary file */
open class UploadFileTask(
    engine: NextCloudEngine,
    remotePath: String,
    private val localFile: File,
    mimeType: String?,
) : OperationTask(engine) {
    override val successMessageString = "Successfully saved $localFile to $remotePath on Nextcloud"

    override val remoteOperation = UploadFileRemoteOperation(
        localFile.absolutePath,
        remotePath,
        mimeType,
        (System.currentTimeMillis() / 1000).toString()
    )

    override fun start() {
        if (!localFile.isFile) failWithMessage("$localFile is not a file")
        else super.start()
    }
}
