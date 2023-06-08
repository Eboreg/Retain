package us.huseli.retain.nextcloud.tasks

import us.huseli.retain.Constants
import us.huseli.retain.data.entities.Image
import us.huseli.retain.nextcloud.NextCloudEngine
import java.io.File

/** Up: 1 image file */
class UploadImageTask(engine: NextCloudEngine, image: Image) : UploadFileTask(
    engine = engine,
    remotePath = engine.getAbsolutePath(Constants.NEXTCLOUD_IMAGE_SUBDIR, image.filename),
    localFile = File(File(engine.context.filesDir, Constants.IMAGE_SUBDIR), image.filename),
    mimeType = image.mimeType
)