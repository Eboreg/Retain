package us.huseli.retain.syncbackend.tasks.result

import android.content.Context
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import us.huseli.retain.R
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
        message += "\n\n" + context.getString(R.string.the_error_was) + ' '
        return message
    }

    companion object {
        fun fromTaskResult(result: TaskResult) =
            TestTaskResult(status = result.status, message = result.message, exception = result.exception)
    }
}
