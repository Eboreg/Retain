package us.huseli.retain.syncbackend.tasks

import us.huseli.retain.syncbackend.Engine
import java.io.File

/** Down: 1 arbitrary file */
abstract class DownloadFileTask<ET : Engine, RT : OperationTaskResult>(
    engine: ET,
    protected val remotePath: String,
) : OperationTask<ET, RT>(engine) {
    override val successMessageString = "Successfully downloaded $remotePath"

    override fun start(onResult: (RT) -> Unit) {
        engine.downloadFile(remotePath) { result ->
            val localFile = result.localFiles.getOrNull(0)

            @Suppress("UNCHECKED_CAST")
            if (localFile != null) handleDownloadedFile(localFile, result, onResult)
            else onResult(result as RT)
        }
    }

    abstract fun handleDownloadedFile(file: File, result: OperationTaskResult, onResult: (RT) -> Unit)
}
