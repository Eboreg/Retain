package us.huseli.retain

import android.util.Log
import java.time.Instant

interface LoggingObject {
    var logger: Logger?

    fun log(message: String, level: Int = Log.INFO, addToFlow: Boolean = false) {
        val logMessage = LogMessage(
            timestamp = Instant.now(),
            level = level,
            tag = "${javaClass.simpleName}<${System.identityHashCode(this)}>",
            thread = Thread.currentThread().name,
            message = message
        )
        log(logMessage, addToFlow)
    }

    fun log(logMessage: LogMessage, addToFlow: Boolean = false) {
        logger?.addMessage(logMessage, addToFlow) ?: run {
            if (BuildConfig.DEBUG) {
                Log.println(
                    logMessage.level,
                    logMessage.tag,
                    "[${logMessage.thread}] ${logMessage.message}"
                )
            }
        }
    }
}