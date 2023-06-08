package us.huseli.retain.nextcloud.tasks

import us.huseli.retain.data.entities.Image
import us.huseli.retain.nextcloud.NextCloudEngine

/** Remove: 0..n image files */
class RemoveImagesTask(engine: NextCloudEngine, images: Collection<Image>) :
    ListTask<OperationTaskResult, RemoveImageTask, Image>(engine = engine, objects = images) {
    override val failOnUnsuccessfulChildTask = false

    override fun getChildTask(obj: Image) = RemoveImageTask(engine = engine, image = obj)
}
