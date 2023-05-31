package us.huseli.retain.nextcloud.tasks

import android.util.Log
import us.huseli.retain.LogInterface
import us.huseli.retain.LogMessage
import us.huseli.retain.Logger
import us.huseli.retain.nextcloud.NextCloudEngine

open class TaskResult(open val success: Boolean, open val error: LogMessage? = null)

abstract class BaseTask<RT : TaskResult>(protected val engine: NextCloudEngine) : LogInterface {
    override val logger: Logger = engine.logger

    private var hasNotified = false
    private var _status = STATUS_WAITING
    private val onFinishedListeners = mutableListOf<(RT) -> Unit>()

    protected var success: Boolean = true
    protected var error: LogMessage? = null
    protected var triggerStatus: Int = NextCloudEngine.STATUS_OK

    open val startMessageString: String? = null
    open val successMessageString: String? = null
    open val isMetaTask: Boolean = false

    val status: Int
        get() = _status

    abstract fun start()
    abstract fun getResult(): RT
    abstract fun isFinished(): Boolean

    fun addOnFinishedListener(listener: (RT) -> Unit) = onFinishedListeners.add(listener)

    open fun run(triggerStatus: Int = NextCloudEngine.STATUS_OK, onFinishedListener: ((RT) -> Unit)? = null) {
        this.triggerStatus = triggerStatus
        if (onFinishedListener != null) onFinishedListeners.add(onFinishedListener)
        engine.registerTask(this, triggerStatus) {
            _status = STATUS_RUNNING
            log("${javaClass.simpleName}: START", level = Log.DEBUG)
            startMessageString?.let { log(it) }
            start()
            notifyIfFinished()
        }
    }

    fun notifyIfFinished(successMessage: String? = null) {
        if (isFinished() && !hasNotified) {
            _status = STATUS_FINISHED
            val result = getResult()

            hasNotified = true
            if (success) {
                (successMessage ?: successMessageString)?.let { log(it) }
                log("${javaClass.simpleName}: FINISH SUCCESSFULLY", level = Log.DEBUG)
            } else log("${javaClass.simpleName}: FINISH FAILINGLY", level = Log.ERROR)

            onFinishedListeners.forEach { it.invoke(result) }
        }
    }

    fun failWithMessage(logMessage: LogMessage?) {
        success = false
        error = logMessage ?: createLogMessage("Unknown error")
        if (logMessage != null) log(logMessage)
        notifyIfFinished()
    }

    fun failWithMessage(message: String?) {
        failWithMessage(logMessage = if (message != null) createLogMessage(message, level = Log.ERROR) else null)
    }

    companion object {
        const val STATUS_WAITING = 0
        const val STATUS_RUNNING = 1
        const val STATUS_FINISHED = 2
    }
}

abstract class Task(engine: NextCloudEngine) : BaseTask<TaskResult>(engine) {
    override fun getResult() = TaskResult(success, error)
}
