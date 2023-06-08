package us.huseli.retain.nextcloud.tasks

import us.huseli.retain.Constants
import us.huseli.retain.data.entities.Image
import us.huseli.retain.nextcloud.NextCloudEngine

/** Remove: 1 image file */
class RemoveImageTask(engine: NextCloudEngine, image: Image) :
    RemoveFileTask(engine, engine.getAbsolutePath(Constants.NEXTCLOUD_IMAGE_SUBDIR, image.filename))
