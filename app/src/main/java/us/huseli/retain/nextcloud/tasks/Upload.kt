package us.huseli.retain.nextcloud.tasks

import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.huseli.retain.Constants
import us.huseli.retain.Constants.NEXTCLOUD_IMAGE_SUBDIR
import us.huseli.retain.Constants.NEXTCLOUD_JSON_SUBDIR
import us.huseli.retain.data.entities.Image
import us.huseli.retain.data.entities.NoteCombo
import us.huseli.retain.nextcloud.NextCloudEngine
import java.io.File
import java.io.FileWriter

/** Up: 1 arbitrary file */
open class UploadFileTask(
    engine: NextCloudEngine,
    remotePath: String,
    private val localFile: File,
    mimeType: String?,
) : OperationTask(engine) {
    override val successMessageString = "Successfully saved $localFile to $remotePath on Nextcloud"

    override val remoteOperation = UploadFileRemoteOperation(
        localFile.absolutePath,
        remotePath,
        mimeType,
        (System.currentTimeMillis() / 1000).toString()
    )

    override fun start() {
        if (!localFile.isFile) failWithMessage("$localFile is not a file")
        else super.start()
    }
}


/** Up: 1 image file */
class UploadImageTask(engine: NextCloudEngine, image: Image) : UploadFileTask(
    engine = engine,
    remotePath = engine.getAbsolutePath(NEXTCLOUD_IMAGE_SUBDIR, image.filename),
    localFile = File(File(engine.context.filesDir, Constants.IMAGE_SUBDIR), image.filename),
    mimeType = image.mimeType
)


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
            remoteDir = engine.getAbsolutePath(NEXTCLOUD_IMAGE_SUBDIR),
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


class UploadNoteCombosTask(engine: NextCloudEngine, private val combos: Collection<NoteCombo>) : OperationTask(engine) {
    private val filename = "noteCombos.json"
    private val remotePath = engine.getAbsolutePath(NEXTCLOUD_JSON_SUBDIR, filename)
    private val localFile = File(engine.tempDirUp, filename).apply { deleteOnExit() }

    override val remoteOperation = UploadFileRemoteOperation(
        localFile.absolutePath,
        remotePath,
        "application/json",
        (System.currentTimeMillis() / 1000).toString()
    )

    override fun start() {
        engine.ioScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    FileWriter(localFile).use { it.write(engine.gson.toJson(combos)) }
                    super.start()
                } catch (e: Exception) {
                    failWithMessage(e.toString())
                }
            }
        }
    }
}
