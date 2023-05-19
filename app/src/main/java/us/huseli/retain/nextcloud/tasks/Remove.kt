package us.huseli.retain.nextcloud.tasks

import com.owncloud.android.lib.resources.files.RemoveFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import okhttp3.internal.toImmutableList
import us.huseli.retain.Constants
import us.huseli.retain.Constants.NEXTCLOUD_JSON_DIR
import us.huseli.retain.data.entities.Image
import us.huseli.retain.nextcloud.NextCloudEngine
import java.util.UUID

/** Remove: 1 arbitrary file */
open class RemoveFileTask(engine: NextCloudEngine, remotePath: String) : OperationTask(engine) {
    override val remoteOperation = RemoveFileRemoteOperation(remotePath)
    override val successMessageString = "Successfully removed $remotePath from Nextcloud"
}

/** Remove: 1 image file */
class RemoveImageTask(engine: NextCloudEngine, image: Image) :
    RemoveFileTask(engine, "${Constants.NEXTCLOUD_IMAGE_DIR}/${image.filename}")


/** Remove: 0..n note JSON file */
class RemoveNotesTask(engine: NextCloudEngine, noteIds: Collection<UUID>) :
    ListTask<TaskResult, OperationTaskResult, RemoveFileTask, UUID>(engine = engine, objects = noteIds) {
    override val failOnUnsuccessfulChildTask = false

    override fun getChildTask(obj: UUID) =
        RemoveFileTask(engine = engine, remotePath = "$NEXTCLOUD_JSON_DIR/note-$obj.json")

    override fun getResult() = TaskResult(success, error)
}


/** Remove: 0..n image files */
class RemoveImagesTask(engine: NextCloudEngine, images: Collection<Image>) :
    ListTask<TaskResult, OperationTaskResult, RemoveImageTask, Image>(engine = engine, objects = images) {
    override val failOnUnsuccessfulChildTask = false

    override fun getChildTask(obj: Image) = RemoveImageTask(engine = engine, image = obj)

    override fun getResult() = TaskResult(success, error)
}


class RemoveOrphanImagesTask(
    engine: NextCloudEngine,
    private val keep: List<String>
) : ListFilesListTask<ListFilesTaskResult, OperationTaskResult, RemoveFileTask>(
    engine = engine,
    remoteDir = Constants.NEXTCLOUD_IMAGE_DIR,
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
