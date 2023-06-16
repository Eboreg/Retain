package us.huseli.retain.syncbackend.tasks

import us.huseli.retain.Constants
import us.huseli.retain.data.entities.Image
import us.huseli.retain.syncbackend.Engine

/**
 * Up: 0..n image files
 * Cannot extend ListTask, because this one iterates over a subset of images,
 * and that subset isn't known until ListFilesTask has been run.
 */
class UploadMissingImagesTask<ET : Engine>(
    engine: ET,
    private val images: Collection<Image>
) : ListFilesTask<ET>(
    engine,
    engine.getAbsolutePath(Constants.SYNCBACKEND_IMAGE_SUBDIR)
) {
    private var processedFiles = 0
    private var missingImages = mutableListOf<Image>()

    override fun getResultForEmptyList() = OperationTaskResult(status = TaskResult.Status.OK)

    override val isMetaTask = true

    override fun start(onResult: (OperationTaskResult) -> Unit) {
        super.start { result ->
            missingImages.addAll(
                if (result.success) {
                    // First list current images and their sizes:
                    val remoteImageLengths = result.remoteFiles.associate {
                        it.name.split('/').last() to it.size
                    }
                    // Then filter DB images for those where the corresponding
                    // remote images either don't exist or have different size:
                    images.filter { image ->
                        remoteImageLengths[image.filename]?.let { image.size.toLong() != it } ?: true
                    }
                } else images.toList()
            )

            missingImages.forEach { image ->
                UploadImageTask(engine, image).run(triggerStatus) { childResult ->
                    processedFiles++
                    if (childResult.hasNetworkError || processedFiles == missingImages.size) onResult(childResult)
                }
            }

            if (missingImages.isEmpty()) onResult(getResultForEmptyList())
        }
    }
}
