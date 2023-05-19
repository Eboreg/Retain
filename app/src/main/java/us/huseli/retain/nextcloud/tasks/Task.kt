package us.huseli.retain.nextcloud.tasks

import android.util.Log
import us.huseli.retain.LogInterface
import us.huseli.retain.LogMessage
import us.huseli.retain.Logger
import us.huseli.retain.nextcloud.NextCloudEngine

val runningTaskNames = mutableListOf<String>()

open class TaskResult(open val success: Boolean, open val error: LogMessage? = null)

abstract class BaseTask<RT : TaskResult>(protected val engine: NextCloudEngine) : LogInterface {
    override val logger: Logger = engine.logger

    private var hasNotified = false
    private var onReadyCallback: ((RT) -> Unit)? = null

    protected var success: Boolean = true
    protected var error: LogMessage? = null
    protected var triggerStatus: Int = NextCloudEngine.STATUS_OK

    open val startMessageString: String? = null
    open val successMessageString: String? = null

    abstract fun start()
    abstract fun getResult(): RT
    abstract fun isReady(): Boolean

    open fun run(triggerStatus: Int = NextCloudEngine.STATUS_OK, onReadyCallback: ((RT) -> Unit)? = null) {
        this.triggerStatus = triggerStatus
        this.onReadyCallback = onReadyCallback
        runningTaskNames.add(javaClass.simpleName)
        engine.awaitStatus(triggerStatus) {
            log("START", level = Log.DEBUG)
            startMessageString?.let { log(it) }
            start()
            notifyIfReady()
        }
    }

    fun notifyIfReady(successMessage: String? = null) {
        if (isReady() && !hasNotified) {
            val result = getResult()

            hasNotified = true
            if (success) {
                (successMessage ?: successMessageString)?.let { log(it) }
                log("FINISH SUCCESSFULLY", level = Log.DEBUG)
            } else log("FINISH FAILINGLY", level = Log.ERROR)

            runningTaskNames.remove(javaClass.simpleName)
            log("RUNNING TASKS: $runningTaskNames", level = Log.DEBUG)

            onReadyCallback?.invoke(result)
        }
    }

    fun failWithMessage(logMessage: LogMessage?) {
        success = false
        error = logMessage ?: createLogMessage("Unknown error")
        if (logMessage != null) log(logMessage)
        notifyIfReady()
    }

    fun failWithMessage(message: String?) {
        failWithMessage(logMessage = if (message != null) createLogMessage(message, level = Log.ERROR) else null)
    }

    /*
    override fun createLogMessage(message: String, level: Int): LogMessage {
        return super.createLogMessage(
            message = "$message (uri=${engine.uri}, username=${engine.username}, password=${engine.password})",
            level = level
        )
    }
     */
}

abstract class Task(engine: NextCloudEngine) : BaseTask<TaskResult>(engine) {
    override fun getResult() = TaskResult(success, error)
}
