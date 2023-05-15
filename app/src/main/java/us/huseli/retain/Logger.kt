package us.huseli.retain

import android.os.Parcelable
import android.util.Log
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.parcelize.Parcelize
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
    override fun hashCode() = timestamp.hashCode()
}

@Singleton
class Logger {
    private val _logMessages = MutableSharedFlow<LogMessage?>(
        replay = 500,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val _snackbarMessage = MutableSharedFlow<LogMessage>(extraBufferCapacity = 5, replay = 1000)

    val logMessages = _logMessages.asSharedFlow()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    private fun addMessage(logMessage: LogMessage, showInSnackbar: Boolean = false) {
        if (showInSnackbar) _snackbarMessage.tryEmit(logMessage)
        if (BuildConfig.DEBUG) {
            Log.println(
                logMessage.level,
                logMessage.tag,
                "[${logMessage.thread}] ${logMessage.message}"
            )
        }
        _logMessages.tryEmit(logMessage)
    }

    fun log(logMessage: LogMessage, showInSnackbar: Boolean = false) = addMessage(logMessage, showInSnackbar)
}

interface LogInterface {
    val logger: Logger

    fun log(message: String, level: Int = Log.INFO, showInSnackbar: Boolean = false) {
        log(createLogMessage(message, level), showInSnackbar)
    }

    fun log(logMessage: LogMessage, showInSnackbar: Boolean = false) = logger.log(logMessage, showInSnackbar)

    fun createLogMessage(message: String, level: Int = Log.INFO): LogMessage {
        return LogMessage(
            level = level,
            tag = "${javaClass.simpleName}<${System.identityHashCode(this)}>",
            thread = Thread.currentThread().name,
            message = message
        )
    }
}
