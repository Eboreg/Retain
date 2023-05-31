package us.huseli.retain.nextcloud.tasks

import com.google.gson.reflect.TypeToken
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.DownloadFileRemoteOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.huseli.retain.Constants.NEXTCLOUD_IMAGE_SUBDIR
import us.huseli.retain.Constants.NEXTCLOUD_JSON_SUBDIR
import us.huseli.retain.LogMessage
import us.huseli.retain.data.entities.Image
import us.huseli.retain.data.entities.NoteCombo
import us.huseli.retain.nextcloud.NextCloudEngine
import java.io.File
import java.io.FileReader

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


/**
 * Down: 0..n images
 *
 * We're more lax with the success status here, because the whole operation shouldn't be considered a failure if
 * one of 10 locally missing images was also missing on remote.
 */
class DownloadMissingImagesTask(
    engine: NextCloudEngine,
    missingImages: Collection<Image>
) : ListTask<OperationTaskResult, DownloadFileTask, Image>(
    engine = engine,
    objects = missingImages
) {
    override val failOnUnsuccessfulChildTask = false

    override fun getChildTask(obj: Image) = DownloadFileTask(
        engine = engine,
        remotePath = engine.getAbsolutePath(NEXTCLOUD_IMAGE_SUBDIR, obj.filename),
        tempDir = engine.tempDirDown,
        localFile = File(File(engine.context.filesDir, "images"), obj.filename),
    )
}


/** Down: 0..n image files */
class DownloadNoteImagesTask(
    engine: NextCloudEngine,
    noteCombo: NoteCombo
) : ListTask<OperationTaskResult, DownloadFileTask, Image>(
    engine = engine,
    objects = noteCombo.images
) {
    override val startMessageString = "Starting download of ${noteCombo.images}"
    override val failOnUnsuccessfulChildTask = false

    override fun getChildTask(obj: Image) = DownloadFileTask(
        engine = engine,
        remotePath = engine.getAbsolutePath(NEXTCLOUD_IMAGE_SUBDIR, obj.filename),
        tempDir = engine.tempDirDown,
        localFile = File(File(engine.context.filesDir, "images"), obj.filename),
    )
}


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


class DownloadNoteCombosJSONTask(engine: NextCloudEngine) : DownloadListJSONTask<NoteCombo>(
    engine = engine,
    remotePath = engine.getAbsolutePath(NEXTCLOUD_JSON_SUBDIR, "noteCombos.json")
) {
    override fun deserialize(json: String): Collection<NoteCombo>? {
        val listType = object : TypeToken<Collection<NoteCombo>>() {}
        @Suppress("RemoveExplicitTypeArguments")
        return engine.gson.fromJson<Collection<NoteCombo>>(json, listType)
    }
}
