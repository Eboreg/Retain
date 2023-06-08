package us.huseli.retain.nextcloud.tasks

import com.owncloud.android.lib.common.operations.RemoteOperationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.huseli.retain.LogMessage
import us.huseli.retain.nextcloud.NextCloudEngine
import java.io.File
import java.io.FileReader

class DownloadListJSONTaskResult<T>(
    success: Boolean,
    error: LogMessage?,
    remoteOperationResult: RemoteOperationResult<*>?,
    val objects: Collection<T>?
) : OperationTaskResult(success, error, remoteOperationResult)

abstract class DownloadListJSONTask<T>(
    engine: NextCloudEngine,
    remotePath: String
) : BaseDownloadFileTask<DownloadListJSONTaskResult<T>>(
    engine = engine,
    remotePath = remotePath,
    tempDir = File(engine.context.cacheDir, "down").apply { mkdirs() },
) {
    private var _finished = false
    private var objects: Collection<T>? = null

    override fun isFinished() = _finished || !success
    override fun getResult() = DownloadListJSONTaskResult(
        success = success,
        error = error,
        remoteOperationResult = remoteOperationResult,
        objects = objects,
    )

    abstract fun deserialize(json: String): Collection<T>?

    override fun handleDownloadedFile(file: File) {
        engine.ioScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val json = FileReader(file).use { it.readText() }
                    objects = deserialize(json)
                    _finished = true
                    if (objects != null)
                        notifyIfFinished("Successfully parsed $remotePath; result=$objects")
                    else failWithMessage("$remotePath: result is null")
                } catch (e: Exception) {
                    failWithMessage("$remotePath: $e")
                } finally {
                    file.delete()
                }
            }
        }
    }
}