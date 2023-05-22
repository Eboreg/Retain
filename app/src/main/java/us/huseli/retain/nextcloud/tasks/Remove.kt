package us.huseli.retain.nextcloud.tasks

import com.owncloud.android.lib.resources.files.RemoveFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import okhttp3.internal.toImmutableList
import us.huseli.retain.Constants.NEXTCLOUD_IMAGE_SUBDIR
import us.huseli.retain.Constants.NEXTCLOUD_JSON_SUBDIR
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
    RemoveFileTask(engine, engine.getAbsolutePath(NEXTCLOUD_IMAGE_SUBDIR, image.filename))


/** Remove: 0..n note JSON file */
class RemoveNotesTask(engine: NextCloudEngine, noteIds: Collection<UUID>) :
    ListTask<OperationTaskResult, RemoveFileTask, UUID>(engine = engine, objects = noteIds) {
    override val failOnUnsuccessfulChildTask = false

    override fun getChildTask(obj: UUID) =
        RemoveFileTask(engine = engine, remotePath = engine.getAbsolutePath(NEXTCLOUD_JSON_SUBDIR, "note-$obj.json"))
}


/** Remove: 0..n image files */
class RemoveImagesTask(engine: NextCloudEngine, images: Collection<Image>) :
    ListTask<OperationTaskResult, RemoveImageTask, Image>(engine = engine, objects = images) {
    override val failOnUnsuccessfulChildTask = false

    override fun getChildTask(obj: Image) = RemoveImageTask(engine = engine, image = obj)
}


class RemoveOrphanImagesTask(
    engine: NextCloudEngine,
    private val keep: List<String>
) : ListFilesListTask<ListFilesTaskResult, OperationTaskResult, RemoveFileTask>(
    engine = engine,
    remoteDir = engine.getAbsolutePath(NEXTCLOUD_IMAGE_SUBDIR),
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
