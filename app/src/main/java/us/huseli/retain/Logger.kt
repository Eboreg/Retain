package us.huseli.retain

import android.util.Log
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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

data class LogMessage(
    val timestamp: Instant,
    val level: Int,
    val tag: String,
    val thread: String,
    val message: String
) {
    fun levelToString() = logLevelToString(level)
    override fun toString() = "$timestamp ${levelToString()} $tag [$thread] $message"
}

@Singleton
class Logger {
    val logMessages = MutableSharedFlow<LogMessage?>(replay = 500, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val latestLogMessage = MutableStateFlow<LogMessage?>(null)

    fun addMessage(logMessage: LogMessage, addToFlow: Boolean = false) {
        logMessages.tryEmit(logMessage)
        if (addToFlow) {
            latestLogMessage.value = logMessage
        }
        if (BuildConfig.DEBUG) {
            Log.println(
                logMessage.level,
                logMessage.tag,
                "[${logMessage.thread}] ${logMessage.message}"
            )
        }
    }
}