package us.huseli.retain.syncbackend.tasks

import us.huseli.retain.syncbackend.Engine
import java.io.File

/** Down: 1 arbitrary file */
open class DownloadFileTask<ET : Engine, RT : OperationTaskResult>(
    engine: ET,
    protected val remotePath: String,
    protected val localFile: File,
) : OperationTask<ET, RT>(engine) {
    override val successMessageString = "Successfully downloaded $remotePath"

    override fun start(onResult: (RT) -> Unit) {
        engine.downloadFile(remotePath, localFile) { result ->
            handleDownloadedFile(localFile, result, onResult)
        }
    }

    open fun handleDownloadedFile(file: File, result: OperationTaskResult, onResult: (RT) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        onResult(result as RT)
    }
}
