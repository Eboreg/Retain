package us.huseli.retain.syncbackend.tasks

import us.huseli.retain.Constants.SYNCBACKEND_IMAGE_SUBDIR
import us.huseli.retain.dataclasses.entities.Image
import us.huseli.retain.syncbackend.Engine
import us.huseli.retain.syncbackend.tasks.abstr.AbstractListTask
import us.huseli.retain.syncbackend.tasks.result.OperationTaskResult
import us.huseli.retain.syncbackend.tasks.result.TaskResult

/** Remove: 0..n image files */
class RemoveImagesTask<ET : Engine>(engine: ET, images: Collection<Image>) :
    AbstractListTask<ET, OperationTaskResult, RemoveFileTask<ET>, Image>(engine = engine, objects = images) {
    override val failOnUnsuccessfulChildTask = false
    override fun getResultForEmptyList() = TaskResult(status = TaskResult.Status.OK)

    override fun processChildTaskResult(obj: Image, result: OperationTaskResult, onResult: (TaskResult) -> Unit) {
        if (successfulObjects.size + unsuccessfulObjects.size == objects.size)
            onResult(TaskResult(status = TaskResult.Status.OK))
    }

    override fun getChildTask(obj: Image) = RemoveFileTask(
        engine = engine,
        remotePath = engine.getAbsolutePath(SYNCBACKEND_IMAGE_SUBDIR, obj.filename),
    )
}
