package us.huseli.retain.syncbackend.tasks

import android.content.Context
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import us.huseli.retain.Constants.SYNCBACKEND_IMAGE_SUBDIR
import us.huseli.retain.Constants.SYNCBACKEND_JSON_SUBDIR
import us.huseli.retain.R
import us.huseli.retain.syncbackend.Engine
import java.time.Instant

@Parcelize
class TestTaskResult(
    override val status: Status,
    override val message: String? = null,
    override val exception: Exception? = null,
    val timestamp: Instant = Instant.now(),
) : TaskResult(status, message, exception), Parcelable {
    override fun equals(other: Any?) = other is TestTaskResult && other.timestamp == timestamp

    override fun hashCode() = timestamp.hashCode()

    fun getErrorMessage(context: Context): String {
        var message = when (status) {
            Status.UNKNOWN_HOST -> context.getString(R.string.unknown_host)
            Status.AUTH_ERROR -> context.getString(R.string.server_reported_authorization_error)
            Status.CONNECT_ERROR -> context.getString(R.string.connect_error)
            else -> context.getString(R.string.an_error_occurred)
        }
        if (this.message != null || exception != null) {
            message += "\n\n" + context.getString(R.string.the_error_was) + ' '
            message += this.message ?: exception.toString()
        }
        return message
    }

    companion object {
        fun fromTaskResult(result: TaskResult) =
            TestTaskResult(status = result.status, message = result.message, exception = result.exception)
    }
}


class TestTask<ET : Engine>(engine: ET) :
    BaseListTask<ET, TestTaskResult, OperationTaskResult, CreateDirTask<ET>, String>(
        engine = engine,
        objects = listOf(
            engine.getAbsolutePath(SYNCBACKEND_IMAGE_SUBDIR),
            engine.getAbsolutePath(SYNCBACKEND_JSON_SUBDIR)
        )
    ) {

    override fun getChildTask(obj: String) = CreateDirTask(engine, obj)

    override fun processChildTaskResult(obj: String, result: OperationTaskResult, onResult: (TestTaskResult) -> Unit) {
        if (!result.success || successfulObjects.size == objects.size) onResult(TestTaskResult.fromTaskResult(result))
    }

    override fun getResultForEmptyList() = TestTaskResult(status = TaskResult.Status.OK)
}
