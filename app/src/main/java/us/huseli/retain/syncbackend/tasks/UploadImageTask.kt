package us.huseli.retain.syncbackend.tasks

import us.huseli.retain.Constants
import us.huseli.retain.data.entities.Image
import us.huseli.retain.syncbackend.Engine
import java.io.File

/** Up: 1 image file */
class UploadImageTask<ET : Engine>(engine: ET, image: Image) : UploadFileTask<ET>(
    engine = engine,
    remotePath = engine.getAbsolutePath(Constants.SYNCBACKEND_IMAGE_SUBDIR, image.filename),
    localFile = File(File(engine.context.filesDir, Constants.IMAGE_SUBDIR), image.filename),
)
