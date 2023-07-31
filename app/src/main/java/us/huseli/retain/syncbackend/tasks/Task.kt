package us.huseli.retain.syncbackend.tasks

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.retain.LogInterface
import us.huseli.retain.Logger
import us.huseli.retain.syncbackend.Engine
import us.huseli.retain.syncbackend.Engine.Companion.STATUS_OK

open class TaskResult(
    open val status: Status,
    open val message: String? = null,
    open val exception: Exception? = null,
) {
    enum class Status { OK, UNKNOWN_HOST, CONNECT_ERROR, AUTH_ERROR, PATH_NOT_FOUND, OTHER_ERROR }

    val success
        get() = status == Status.OK
    val hasNetworkError
        get() = listOf(Status.CONNECT_ERROR, Status.AUTH_ERROR, Status.UNKNOWN_HOST).contains(status)

    fun copy(status: Status = this.status, message: String? = this.message, exception: Exception? = this.exception) =
        TaskResult(status, message, exception)
}

abstract class Task<ET : Engine, RT : TaskResult>(protected val engine: ET) : LogInterface {
    override val logger: Logger = engine.logger

    private val _status = MutableStateFlow(STATUS_WAITING)
    private val isCancelled = MutableStateFlow(false)
    private val onFinishedListeners = mutableListOf<(RT) -> Unit>()

    protected var triggerStatus: Int = STATUS_OK

    open val startMessageString: String? = null
    open val successMessageString: String? = null
    open val isMetaTask: Boolean = false

    val status = _status.asStateFlow()

    abstract fun start(onResult: (RT) -> Unit): Any

    fun addOnFinishedListener(listener: (RT) -> Unit) = onFinishedListeners.add(listener)

    fun cancel() {
        isCancelled.value = true
    }

    open fun run(triggerStatus: Int = STATUS_OK, onFinishedListener: ((RT) -> Unit)? = null) {
        this.triggerStatus = triggerStatus
        if (onFinishedListener != null) onFinishedListeners.add(onFinishedListener)

        engine.registerTask(this, triggerStatus) {
            if (isCancelled.value) {
                _status.value = STATUS_CANCELLED
            } else {
                _status.value = STATUS_RUNNING
                log("${javaClass.simpleName}: START", level = Log.DEBUG)
                startMessageString?.let { log(it) }
                start { result ->
                    _status.value = STATUS_FINISHED
                    if (result.success) {
                        successMessageString?.let { log(it) }
                        log("${javaClass.simpleName}: FINISH SUCCESSFULLY", level = Log.DEBUG)
                    } else {
                        result.message?.let { log(it) }
                        log("${javaClass.simpleName}: FINISH FAILINGLY", level = Log.ERROR)
                    }
                    onFinishedListeners.forEach { it.invoke(result) }
                }
            }
        }
    }

    companion object {
        const val STATUS_WAITING = 0
        const val STATUS_RUNNING = 1
        const val STATUS_CANCELLED = 2
        const val STATUS_FINISHED = 3
    }
}
