package us.huseli.retain.syncbackend.tasks

import us.huseli.retain.Constants
import us.huseli.retain.Constants.SYNCBACKEND_IMAGE_SUBDIR
import us.huseli.retain.data.entities.Image
import us.huseli.retain.syncbackend.Engine
import java.io.File

/**
 * Up: 0..n image files
 * Cannot extend ListTask, because this one iterates over a subset of images,
 * and that subset isn't known until ListFilesTask has been run.
 */
class UploadMissingImagesTask<ET : Engine>(engine: ET, private val images: Collection<Image>) :
    ListFilesTask<ET>(engine, engine.getAbsolutePath(SYNCBACKEND_IMAGE_SUBDIR), { true }) {
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

            @Suppress("Destructure")
            missingImages.forEach { image ->
                UploadFileTask(
                    engine = engine,
                    remotePath = engine.getAbsolutePath(SYNCBACKEND_IMAGE_SUBDIR, image.filename),
                    localFile = File(File(engine.context.filesDir, Constants.IMAGE_SUBDIR), image.filename),
                ).run(triggerStatus) { childResult ->
                    processedFiles++
                    if (childResult.hasNetworkError || processedFiles == missingImages.size) onResult(childResult)
                }
            }

            if (missingImages.isEmpty()) onResult(getResultForEmptyList())
        }
    }
}
