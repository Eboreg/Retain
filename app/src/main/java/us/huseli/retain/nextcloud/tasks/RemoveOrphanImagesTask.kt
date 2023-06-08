package us.huseli.retain.nextcloud.tasks

import com.owncloud.android.lib.resources.files.model.RemoteFile
import okhttp3.internal.toImmutableList
import us.huseli.retain.Constants
import us.huseli.retain.nextcloud.NextCloudEngine

class RemoveOrphanImagesTask(
    engine: NextCloudEngine,
    private val keep: List<String>
) : ListFilesListTask<ListFilesTaskResult, OperationTaskResult, RemoveFileTask>(
    engine = engine,
    remoteDir = engine.getAbsolutePath(Constants.NEXTCLOUD_IMAGE_SUBDIR),
    filter = { remoteFile ->
        remoteFile.mimeType != "DIR" &&
        !keep.contains(remoteFile.remotePath.split("/").last())
    },
) {
    override val failOnUnsuccessfulChildTask = false

    override fun getChildTask(remoteFile: RemoteFile) = RemoveFileTask(engine, remoteFile.remotePath)

    override fun getResult() = ListFilesTaskResult(
        success = success,
        error = error,
        remoteOperationResult = remoteOperationResult,
        remoteFiles = remoteFiles.toImmutableList(),
    )
}
