package us.huseli.retain.syncbackend.tasks.abstr

import us.huseli.retain.syncbackend.Engine
import us.huseli.retain.syncbackend.tasks.DownloadFileTask
import us.huseli.retain.syncbackend.tasks.result.DownloadListJSONTaskResult
import us.huseli.retain.syncbackend.tasks.result.OperationTaskResult
import us.huseli.retain.syncbackend.tasks.result.TaskResult
import java.io.File
import java.io.FileReader


abstract class AbstractDownloadListJSONTask<ET : Engine, LT : Any>(
    engine: ET,
    remotePath: String,
    localFile: File
) : DownloadFileTask<ET, DownloadListJSONTaskResult<LT>>(
    engine = engine,
    remotePath = remotePath,
    localFile = localFile,
) {
    private var _finished = false

    abstract fun deserialize(json: String): List<LT>?

    @Suppress("SameParameterValue")
    private fun castResult(
        result: OperationTaskResult,
        status: TaskResult.Status = result.status,
        exception: Exception? = result.exception,
        message: String? = exception?.message ?: result.message,
        objects: List<LT> = emptyList(),
    ) = DownloadListJSONTaskResult(
        status = status,
        message = message,
        exception = exception,
        remoteFiles = result.remoteFiles,
        objects = objects
    )

    override fun handleDownloadedFile(
        file: File,
        result: OperationTaskResult,
        onResult: (DownloadListJSONTaskResult<LT>) -> Unit
    ) {
        if (!result.success) {
            onResult(castResult(result))
        } else engine.launch {
            try {
                val json = FileReader(file).use { it.readText() }
                val objects = deserialize(json)
                if (objects != null) onResult(castResult(result, objects = objects))
                else onResult(
                    castResult(
                        result = result,
                        status = TaskResult.Status.OTHER_ERROR,
                        message = "$remotePath: result is null",
                    )
                )
            } catch (e: Exception) {
                onResult(
                    castResult(
                        result = result,
                        status = TaskResult.Status.OTHER_ERROR,
                        exception = e
                    )
                )
            } finally {
                file.delete()
            }
        }
    }
}
