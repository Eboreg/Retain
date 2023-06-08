package us.huseli.retain.nextcloud.tasks

import us.huseli.retain.Constants
import us.huseli.retain.data.entities.Image
import us.huseli.retain.nextcloud.NextCloudEngine
import java.io.File

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
        remotePath = engine.getAbsolutePath(Constants.NEXTCLOUD_IMAGE_SUBDIR, obj.filename),
        tempDir = engine.tempDirDown,
        localFile = File(File(engine.context.filesDir, "images"), obj.filename),
    )
}