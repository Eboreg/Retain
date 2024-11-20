package us.huseli.retain.syncbackend.tasks

import us.huseli.retain.Constants.SYNCBACKEND_IMAGE_SUBDIR
import us.huseli.retain.dataclasses.entities.Image
import us.huseli.retain.syncbackend.Engine
import us.huseli.retain.syncbackend.tasks.abstr.AbstractListTask
import us.huseli.retain.syncbackend.tasks.result.OperationTaskResult
import us.huseli.retain.syncbackend.tasks.result.TaskResult
import java.io.File

/** Down: 0..n images */
class DownloadImagesTask<ET : Engine>(engine: ET, images: Collection<Image>) :
    AbstractListTask<ET, OperationTaskResult, DownloadFileTask<ET, OperationTaskResult>, Image>(
        engine = engine,
        objects = images
    ) {
    override fun getResultForEmptyList() = TaskResult(status = TaskResult.Status.OK)

    override fun processChildTaskResult(obj: Image, result: OperationTaskResult, onResult: (TaskResult) -> Unit) {
        if (successfulObjects.size + unsuccessfulObjects.size == objects.size) {
            onResult(TaskResult(status = TaskResult.Status.OK))
        }
    }

    override fun getChildTask(obj: Image) = DownloadFileTask<ET, OperationTaskResult>(
        engine = engine,
        remotePath = engine.getAbsolutePath(SYNCBACKEND_IMAGE_SUBDIR, obj.filename),
        localFile = File(File(engine.context.filesDir, "images"), obj.filename),
    )
}
