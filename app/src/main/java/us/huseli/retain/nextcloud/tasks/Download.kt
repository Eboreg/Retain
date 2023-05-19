package us.huseli.retain.nextcloud.tasks

import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.DownloadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.toImmutableList
import us.huseli.retain.Constants
import us.huseli.retain.LogMessage
import us.huseli.retain.data.entities.Image
import us.huseli.retain.data.entities.NoteCombined
import us.huseli.retain.nextcloud.NextCloudEngine
import java.io.File
import java.io.FileReader

/** Down: 1 arbitrary file */
abstract class BaseDownloadFileTask<RT : OperationTaskResult>(
    engine: NextCloudEngine,
    val remotePath: String,
    tempDir: File,
) : BaseOperationTask<RT>(engine) {
    internal val localTempFile = File(tempDir, remotePath)
    override val remoteOperation = DownloadFileRemoteOperation(remotePath, tempDir.absolutePath)

    override fun onSuccessfulRemoteOperation(remoteOperationResult: RemoteOperationResult<*>) {
        if (!localTempFile.isFile)
            failWithMessage("$remotePath: $localTempFile is not a file")
        else
            handleDownloadedFile()
    }

    open fun handleDownloadedFile() = notifyIfReady()
}

open class DownloadFileTask(
    engine: NextCloudEngine,
    remotePath: String,
    tempDir: File,
    private val localFile: File,
) : BaseDownloadFileTask<OperationTaskResult>(engine, remotePath, tempDir) {
    override val successMessageString = "Successfully downloaded $localTempFile to $localFile"

    override fun getResult() = OperationTaskResult(
        success = success,
        error = error,
        remoteOperationResult = remoteOperationResult,
    )

    override fun handleDownloadedFile() {
        if (!localTempFile.renameTo(localFile))
            failWithMessage("$remotePath: Could not move $localTempFile to $localFile")
        else
            notifyIfReady()
    }
}


/**
 * Down: 0..n images
 *
 * We're more lax with the success status here, because the whole operation shouldn't be considered a failure if
 * one of 10 locally missing images was also missing on remote.
 */
class DownloadMissingImagesTask(
    engine: NextCloudEngine,
    missingImages: Collection<Image>
) : ListTask<TaskResult, OperationTaskResult, DownloadFileTask, Image>(
    engine = engine,
    objects = missingImages
) {
    override val failOnUnsuccessfulChildTask = false

    override fun getResult() = TaskResult(success, error)

    override fun getChildTask(obj: Image) = DownloadFileTask(
        engine = engine,
        remotePath = "${Constants.NEXTCLOUD_IMAGE_DIR}/${obj.filename}",
        tempDir = engine.tempDirDown,
        localFile = File(File(engine.context.filesDir, "images"), obj.filename),
    )
}


/** Down: 0..n image files */
class DownloadNoteImagesTask(
    engine: NextCloudEngine,
    noteCombined: NoteCombined
) : ListTask<TaskResult, OperationTaskResult, DownloadFileTask, Image>(
    engine = engine,
    objects = noteCombined.images
) {
    override val startMessageString = "Starting download of ${noteCombined.images}"
    override val failOnUnsuccessfulChildTask = false

    override fun getChildTask(obj: Image) = DownloadFileTask(
        engine = engine,
        remotePath = "${Constants.NEXTCLOUD_IMAGE_DIR}/${obj.filename}",
        tempDir = engine.tempDirDown,
        localFile = File(File(engine.context.filesDir, "images"), obj.filename),
    )

    override fun getResult() = TaskResult(success, error)
}


class DownloadNoteTaskResult(
    success: Boolean,
    error: LogMessage?,
    remoteOperationResult: RemoteOperationResult<*>?,
    val remoteNoteCombined: NoteCombined?
) : OperationTaskResult(success, error, remoteOperationResult)

/** Down: 1 note JSON file */
class DownloadNoteTask(engine: NextCloudEngine, remotePath: String) : BaseDownloadFileTask<DownloadNoteTaskResult>(
    engine = engine,
    remotePath = remotePath,
    tempDir = File(engine.context.cacheDir, "down").also { it.mkdir() },
) {
    private var remoteNoteCombined: NoteCombined? = null

    override fun getResult() =
        DownloadNoteTaskResult(success, error, remoteOperationResult, remoteNoteCombined)

    override fun handleDownloadedFile() {
        engine.ioScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val json = FileReader(localTempFile).use { it.readText() }

                    remoteNoteCombined = engine.gson.fromJson(json, NoteCombined::class.java)
                    if (remoteNoteCombined != null) {
                        notifyIfReady("Successfully parsed $remoteNoteCombined from Nextcloud")
                    } else failWithMessage("$remotePath: noteCombined is null")
                } catch (e: Exception) {
                    failWithMessage("$remotePath: $e")
                } finally {
                    localTempFile.delete()
                }
            }
        }
    }
}


class DownstreamSyncTaskResult(
    success: Boolean,
    error: LogMessage?,
    remoteOperationResult: RemoteOperationResult<*>?,
    remoteFiles: List<RemoteFile>,
    val remoteNotesCombined: List<NoteCombined>,
) : ListFilesTaskResult(success, error, remoteOperationResult, remoteFiles)

/** Down: 0..n note JSON files */
class DownstreamSyncTask(engine: NextCloudEngine) :
    ListFilesListTask<DownstreamSyncTaskResult, DownloadNoteTaskResult, DownloadNoteTask>(
        engine = engine,
        remoteDir = Constants.NEXTCLOUD_JSON_DIR,
        filter = { remoteFile ->
            remoteFile.mimeType == "application/json" &&
            remoteFile.remotePath.split("/").last().startsWith("note-")
        }
    ) {
    private val remoteNotesCombined = mutableListOf<NoteCombined>()
    override val failOnUnsuccessfulChildTask = false

    override fun getChildTask(remoteFile: RemoteFile) = DownloadNoteTask(engine, remoteFile.remotePath)

    override fun processChildTaskResult(remoteFile: RemoteFile, result: DownloadNoteTaskResult) {
        if (result.success && result.remoteNoteCombined != null)
            remoteNotesCombined.add(result.remoteNoteCombined)
    }

    override fun getResult() = DownstreamSyncTaskResult(
        success = success,
        error = error,
        remoteOperationResult = remoteOperationResult,
        remoteFiles = remoteFiles.toImmutableList(),
        remoteNotesCombined = remoteNotesCombined.toImmutableList(),
    )
}
