package us.huseli.retain.nextcloud.tasks

import us.huseli.retain.nextcloud.NextCloudEngine

/**
 * Default behaviour: Fail immediately when any child task fails. If this is
 * not desired, override isReady() and/or onUnsuccessfulChildTask().
 *
 * By default, this.error will be the same as the error from the latest failed
 * child task, which is not super optimal, but I guess we can live with it.
 */
abstract class ListTask<RT : TaskResult, CRT : TaskResult, CT : BaseTask<CRT>, LT>(
    engine: NextCloudEngine,
    private val objects: Collection<LT>,
) : BaseTask<RT>(engine) {
    private var onEachCallback: ((LT, CRT) -> Unit)? = null
    private val successfulObjects = mutableListOf<LT>()
    protected val unsuccessfulObjects = mutableListOf<LT>()
    protected open val failOnUnsuccessfulChildTask = true

    abstract fun getChildTask(obj: LT): CT?

    open fun processChildTaskResult(obj: LT, result: CRT) {}

    override fun isReady() =
        (successfulObjects.size + unsuccessfulObjects.size == objects.size) ||
        (failOnUnsuccessfulChildTask && !success)

    fun run(
        triggerStatus: Int = NextCloudEngine.STATUS_OK,
        onEachCallback: ((LT, CRT) -> Unit)?,
        onReadyCallback: ((RT) -> Unit)?
    ) {
        this.onEachCallback = onEachCallback
        super.run(triggerStatus, onReadyCallback)
    }

    override fun start() {
        objects.forEach { obj ->
            getChildTask(obj)?.run(triggerStatus) { result ->
                processChildTaskResult(obj, result)
                if (result.success) {
                    successfulObjects.add(obj)
                } else {
                    unsuccessfulObjects.add(obj)
                    if (failOnUnsuccessfulChildTask) failWithMessage(result.error)
                }
                onEachCallback?.invoke(obj, result)
                notifyIfReady()
            }
        }
        notifyIfReady()
    }
}
