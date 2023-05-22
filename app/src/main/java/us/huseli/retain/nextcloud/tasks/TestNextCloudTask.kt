package us.huseli.retain.nextcloud.tasks

import android.content.Context
import android.os.Parcelable
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import kotlinx.parcelize.Parcelize
import us.huseli.retain.Constants.NEXTCLOUD_IMAGE_SUBDIR
import us.huseli.retain.Constants.NEXTCLOUD_JSON_SUBDIR
import us.huseli.retain.LogMessage
import us.huseli.retain.R
import us.huseli.retain.nextcloud.NextCloudEngine
import java.net.UnknownHostException
import java.time.Instant

@Parcelize
class TestNextCloudTaskResult(
    override val success: Boolean,
    override val error: LogMessage?,
    val status: Int,
    val isUrlFail: Boolean = false,
    val isCredentialsFail: Boolean = false,
    val resultCode: RemoteOperationResult.ResultCode? = null,
    val resultErrorMessage: String? = null,
    val timestamp: Instant = Instant.now(),
) : TaskResult(success, error), Parcelable {
    override fun equals(other: Any?) = other is TestNextCloudTaskResult && other.timestamp == timestamp

    override fun hashCode() = timestamp.hashCode()

    fun getErrorMessage(context: Context): String {
        val error = if (resultCode != null) {
            when (resultCode) {
                RemoteOperationResult.ResultCode.UNAUTHORIZED -> context.getString(R.string.server_reported_authorization_error)
                RemoteOperationResult.ResultCode.FILE_NOT_FOUND -> context.getString(R.string.server_reported_file_not_found)
                else -> resultErrorMessage
            }
        } else if (status == NextCloudEngine.STATUS_AUTH_ERROR)
            context.getString(R.string.server_reported_authorization_error)
        else
            context.getString(R.string.unkown_error)

        return "${context.getString(R.string.failed_to_connect_to_nextcloud)}: $error"
    }
}


class TestNextCloudTask(engine: NextCloudEngine) :
    BaseListTask<TestNextCloudTaskResult, OperationTaskResult, CreateDirTask, String>(
        engine = engine,
        objects = listOf(engine.getAbsolutePath(NEXTCLOUD_IMAGE_SUBDIR), engine.getAbsolutePath(NEXTCLOUD_JSON_SUBDIR))
    ) {
    private var remoteOperationResult: RemoteOperationResult<*>? = null

    override fun getChildTask(obj: String) = CreateDirTask(engine, obj)

    override fun processChildTaskResult(obj: String, result: OperationTaskResult) {
        // Only set this.remoteOperationResult if it is null or was successful,
        // thereby giving priority to unsuccessful results:
        result.remoteOperationResult?.let {
            if (remoteOperationResult == null || remoteOperationResult?.isSuccess == true) {
                remoteOperationResult = it
            }
        }
    }

    override fun getResult(): TestNextCloudTaskResult {
        val status =
            if (success) NextCloudEngine.STATUS_OK
            else if (
                remoteOperationResult?.code == RemoteOperationResult.ResultCode.UNAUTHORIZED ||
                remoteOperationResult?.code == RemoteOperationResult.ResultCode.FORBIDDEN
            ) NextCloudEngine.STATUS_AUTH_ERROR
            else NextCloudEngine.STATUS_ERROR

        val isUrlFail =
            !success &&
            (
                remoteOperationResult?.exception is UnknownHostException ||
                remoteOperationResult?.code == RemoteOperationResult.ResultCode.FILE_NOT_FOUND
            )

        val isCredentialsFail = !success && remoteOperationResult?.code == RemoteOperationResult.ResultCode.UNAUTHORIZED

        return TestNextCloudTaskResult(
            success = success,
            error = error,
            status = status,
            isUrlFail = isUrlFail,
            isCredentialsFail = isCredentialsFail,
            resultCode = remoteOperationResult?.code,
            resultErrorMessage = remoteOperationResult?.exception?.message ?: remoteOperationResult?.logMessage,
        )
    }
}
