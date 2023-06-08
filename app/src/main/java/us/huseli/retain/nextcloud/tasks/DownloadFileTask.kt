package us.huseli.retain.nextcloud.tasks

import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.DownloadFileRemoteOperation
import us.huseli.retain.nextcloud.NextCloudEngine
import java.io.File

/** Down: 1 arbitrary file */
abstract class BaseDownloadFileTask<RT : OperationTaskResult>(
    engine: NextCloudEngine,
    val remotePath: String,
    private val tempDir: File,
) : BaseOperationTask<RT>(engine) {
    override val remoteOperation = DownloadFileRemoteOperation(remotePath, tempDir.absolutePath)

    override fun onSuccessfulRemoteOperation(remoteOperationResult: RemoteOperationResult<*>) {
        val localTempFile = File(tempDir, remotePath)
        if (!localTempFile.isFile)
            failWithMessage("$remotePath: $localTempFile is not a file")
        else
            handleDownloadedFile(localTempFile)
    }

    open fun handleDownloadedFile(file: File) = notifyIfFinished()
}

open class DownloadFileTask(
    engine: NextCloudEngine,
    remotePath: String,
    tempDir: File,
    private val localFile: File,
) : BaseDownloadFileTask<OperationTaskResult>(engine, remotePath, tempDir) {
    override val successMessageString = "Successfully downloaded $remotePath to $localFile"

    override fun getResult() = OperationTaskResult(
        success = success,
        error = error,
        remoteOperationResult = remoteOperationResult,
    )

    override fun handleDownloadedFile(file: File) {
        if (!file.renameTo(localFile))
            failWithMessage("$remotePath: Could not move $file to $localFile")
        else
            notifyIfFinished()
    }
}



