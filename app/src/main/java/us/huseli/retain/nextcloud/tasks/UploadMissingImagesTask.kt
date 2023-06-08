package us.huseli.retain.nextcloud.tasks

import us.huseli.retain.Constants
import us.huseli.retain.data.entities.Image
import us.huseli.retain.nextcloud.NextCloudEngine

/**
 * Up: 0..n image files
 * Cannot extend ListTask, because this one iterates over a subset of images,
 * and that subset isn't known until ListFilesTask has been run.
 */
class UploadMissingImagesTask(engine: NextCloudEngine, private val images: Collection<Image>) : Task(engine) {
    private var processedFiles = 0
    private var missingImages = mutableListOf<Image>()

    override fun isFinished() = !success || missingImages.size == processedFiles
    override val isMetaTask = true

    override fun start() {
        ListFilesTask(
            engine = engine,
            remoteDir = engine.getAbsolutePath(Constants.NEXTCLOUD_IMAGE_SUBDIR),
            filter = { remoteFile -> remoteFile.mimeType != "DIR" },
        ).run(triggerStatus) { result ->
            missingImages.addAll(
                if (result.success) {
                    // First list current images and their sizes:
                    val remoteImageLengths = result.remoteFiles.associate {
                        Pair(it.remotePath.split("/").last(), it.length.toInt())
                    }
                    // Then filter DB images for those where the corresponding
                    // remote images either don't exist or have different size:
                    images.filter { image ->
                        remoteImageLengths[image.filename]?.let { image.size != it } ?: true
                    }
                } else images.toList()
            )

            missingImages.forEach { image ->
                UploadImageTask(engine, image).run(triggerStatus) { task ->
                    if (!task.success) success = false
                    processedFiles++
                    notifyIfFinished()
                }
            }
            notifyIfFinished()
        }
    }
}