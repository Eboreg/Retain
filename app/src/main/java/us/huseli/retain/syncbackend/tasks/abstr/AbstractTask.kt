package us.huseli.retain.syncbackend.tasks.abstr

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.retain.ILogger
import us.huseli.retain.syncbackend.Engine
import us.huseli.retain.syncbackend.Engine.Companion.STATUS_OK
import us.huseli.retain.syncbackend.tasks.result.TaskResult

abstract class AbstractTask<ET : Engine, RT : TaskResult>(protected val engine: ET) : ILogger {
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
                log("${javaClass.simpleName}: START", priority = Log.DEBUG)
                startMessageString?.let { log(it) }
                start { result ->
                    _status.value = STATUS_FINISHED
                    if (result.success) {
                        successMessageString?.let { log(it) }
                        log("${javaClass.simpleName}: FINISH SUCCESSFULLY", priority = Log.DEBUG)
                    } else {
                        result.message?.let { log(it) }
                        log("${javaClass.simpleName}: FINISH FAILINGLY", priority = Log.ERROR)
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
