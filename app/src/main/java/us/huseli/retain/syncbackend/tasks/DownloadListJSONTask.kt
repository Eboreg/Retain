package us.huseli.retain.syncbackend.tasks

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.huseli.retain.syncbackend.Engine
import java.io.File
import java.io.FileReader

class DownloadListJSONTaskResult<T : Any>(
    status: Status,
    message: String? = null,
    exception: Exception? = null,
    remoteFiles: List<RemoteFile> = emptyList(),
    override val objects: List<T> = emptyList(),
) : OperationTaskResult(status, message, exception, remoteFiles, objects = objects)


abstract class DownloadListJSONTask<ET : Engine, LT : Any>(
    engine: ET,
    remotePath: String
) : DownloadFileTask<ET, DownloadListJSONTaskResult<LT>>(
    engine = engine,
    remotePath = remotePath,
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
        } else engine.ioScope.launch {
            withContext(Dispatchers.IO) {
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
}
