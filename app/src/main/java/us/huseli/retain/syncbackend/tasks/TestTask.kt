package us.huseli.retain.syncbackend.tasks

import us.huseli.retain.Constants.SYNCBACKEND_IMAGE_SUBDIR
import us.huseli.retain.Constants.SYNCBACKEND_JSON_SUBDIR
import us.huseli.retain.syncbackend.Engine
import us.huseli.retain.syncbackend.tasks.abstr.AbstractBaseListTask
import us.huseli.retain.syncbackend.tasks.result.OperationTaskResult
import us.huseli.retain.syncbackend.tasks.result.TaskResult
import us.huseli.retain.syncbackend.tasks.result.TestTaskResult


class TestTask<ET : Engine>(engine: ET) :
    AbstractBaseListTask<ET, TestTaskResult, OperationTaskResult, CreateDirTask<ET>, String>(
        engine = engine,
        objects = listOf(
            engine.getAbsolutePath(SYNCBACKEND_IMAGE_SUBDIR),
            engine.getAbsolutePath(SYNCBACKEND_JSON_SUBDIR)
        )
    ) {

    override fun getChildTask(obj: String) = CreateDirTask(engine, obj)

    override fun processChildTaskResult(obj: String, result: OperationTaskResult, onResult: (TestTaskResult) -> Unit) {
        if (!result.success || successfulObjects.size == objects.size) onResult(TestTaskResult.Companion.fromTaskResult(result))
    }

    override fun getResultForEmptyList() = TestTaskResult(status = TaskResult.Status.OK)
}
