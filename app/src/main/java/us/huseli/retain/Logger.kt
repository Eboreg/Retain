package us.huseli.retain

import android.os.Parcelable
import android.util.Log
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.parcelize.Parcelize
import us.huseli.retaintheme.snackbar.SnackbarEngine
import java.time.Instant
import javax.inject.Singleton

fun logLevelToString(level: Int): String {
    return when (level) {
        Log.DEBUG -> "DEBUG"
        Log.INFO -> "INFO"
        Log.WARN -> "WARN"
        Log.ERROR -> "ERROR"
        Log.VERBOSE -> "VERBOSE"
        Log.ASSERT -> "ASSERT"
        else -> "UNKNOWN"
    }
}

@Parcelize
data class LogMessage(
    val timestamp: Instant = Instant.now(),
    val level: Int,
    val tag: String,
    val thread: String,
    val message: String
) : Parcelable {
    fun levelToString() = logLevelToString(level)
    override fun toString() = "$timestamp ${levelToString()} $tag [$thread] $message"
    override fun equals(other: Any?) = other is LogMessage && other.timestamp == timestamp
    override fun hashCode(): Int = 31 * timestamp.hashCode() + message.hashCode()
}

@Singleton
class Logger {
    private val _logMessages = MutableSharedFlow<LogMessage?>(
        replay = 500,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val logMessages = _logMessages.asSharedFlow()

    private fun addMessage(logMessage: LogMessage, showInSnackbar: Boolean = false) {
        if (showInSnackbar) {
            when (logMessage.level) {
                Log.ERROR -> SnackbarEngine.addError(logMessage.message)
                else -> SnackbarEngine.addInfo(logMessage.message)
            }
        }
        if (BuildConfig.DEBUG) {
            Log.println(
                logMessage.level,
                logMessage.tag,
                "[${logMessage.thread}] ${logMessage.message}"
            )
            _logMessages.tryEmit(logMessage)
        }
    }

    fun log(logMessage: LogMessage, showInSnackbar: Boolean = false) = addMessage(logMessage, showInSnackbar)
}

interface LogInterface {
    val logger: Logger

    fun log(message: String, level: Int = Log.INFO, showInSnackbar: Boolean = false) =
        logger.log(createLogMessage(message, level), showInSnackbar)

    fun showError(message: String) = log(message, Log.ERROR, true)

    fun showError(prefix: String, exception: Exception?) {
        if (exception != null) showError("$prefix: $exception")
        else showError(prefix)
    }

    private fun createLogMessage(message: String, level: Int = Log.INFO): LogMessage {
        return LogMessage(
            level = level,
            tag = "${javaClass.simpleName}<${System.identityHashCode(this)}>",
            thread = Thread.currentThread().name,
            message = message
        )
    }
}
