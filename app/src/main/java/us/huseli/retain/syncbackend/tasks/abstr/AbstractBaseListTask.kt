package us.huseli.retain.syncbackend.tasks.abstr

import us.huseli.retain.syncbackend.Engine
import us.huseli.retain.syncbackend.tasks.result.TaskResult

/**
 * Default behaviour: Fail immediately when any child task fails. If this is
 * not desired, override isReady() and/or onUnsuccessfulChildTask().
 *
 * By default, this.error will be the same as the error from the latest failed
 * child task, which is not super optimal, but I guess we can live with it.
 */
abstract class AbstractBaseListTask<ET : Engine, RT : TaskResult, CRT : TaskResult, CT : AbstractTask<ET, CRT>, LT>(
    engine: ET,
    protected val objects: Collection<LT>,
) : AbstractTask<ET, RT>(engine) {
    private var onEachCallback: ((LT, CRT) -> Unit)? = null
    protected val successfulObjects = mutableListOf<LT>()
    protected val unsuccessfulObjects = mutableListOf<LT>()
    protected open val failOnUnsuccessfulChildTask = true
    override val isMetaTask = true

    abstract fun getChildTask(obj: LT): CT?
    abstract fun processChildTaskResult(obj: LT, result: CRT, onResult: (RT) -> Unit)
    abstract fun getResultForEmptyList(): RT

    fun run(
        triggerStatus: Int = Engine.Companion.STATUS_OK,
        onEachCallback: ((LT, CRT) -> Unit)?,
        onReadyCallback: ((RT) -> Unit)?
    ) {
        this.onEachCallback = onEachCallback
        super.run(triggerStatus, onReadyCallback)
    }

    override fun start(onResult: (RT) -> Unit) {
        if (objects.isNotEmpty()) {
            objects.forEach { obj ->
                getChildTask(obj)?.run(triggerStatus) { childResult ->
                    if (childResult.success) successfulObjects.add(obj)
                    else unsuccessfulObjects.add(obj)
                    processChildTaskResult(obj, childResult, onResult)
                    onEachCallback?.invoke(obj, childResult)
                }
            }
        } else onResult(getResultForEmptyList())
    }
}