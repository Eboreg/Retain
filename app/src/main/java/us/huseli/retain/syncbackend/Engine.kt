package us.huseli.retain.syncbackend

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.launch
import us.huseli.retain.Enums.SyncBackend
import us.huseli.retain.InstantAdapter
import us.huseli.retain.LogInterface
import us.huseli.retain.syncbackend.tasks.OperationTaskResult
import us.huseli.retain.syncbackend.tasks.RemoteFile
import us.huseli.retain.syncbackend.tasks.Task
import us.huseli.retain.syncbackend.tasks.TaskResult
import us.huseli.retain.syncbackend.tasks.TestTask
import us.huseli.retain.syncbackend.tasks.TestTaskResult
import java.io.File
import java.time.Instant

@OptIn(FlowPreview::class)
abstract class Engine(internal val context: Context, internal val ioScope: CoroutineScope) : LogInterface {
    abstract val backend: SyncBackend
    private var isTestScheduled = false

    protected val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    protected val tasks = MutableStateFlow<List<Task<*, *>>>(emptyList())
    protected var status = STATUS_DISABLED

    internal val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Instant::class.java, InstantAdapter())
        .create()
    internal val tempDirUp = File(context.cacheDir, "up").apply { mkdirs() }
    internal val tempDirDown = File(context.cacheDir, "down").apply { mkdirs() }
    internal val listenerHandler = Handler(Looper.getMainLooper())

    private val runningTasks: List<Task<*, *>>
        get() = tasks.value.filter { it.status.value == Task.STATUS_RUNNING }

    private val runningNonMetaTasks: List<Task<*, *>>
        get() = runningTasks.filter { !it.isMetaTask }

    private val waitingTasks: List<Task<*, *>>
        get() = tasks.value.filter { it.status.value == Task.STATUS_WAITING }

    val hasActiveTasks = tasks.flatMapMerge { tasks ->
        combine(*tasks.map { it.status }.toTypedArray()) { statuses ->
            statuses.any { it != Task.STATUS_FINISHED }
        }
    }

    abstract fun removeFile(remotePath: String, onResult: (OperationTaskResult) -> Unit): Any
    abstract fun createDir(remoteDir: String, onResult: (OperationTaskResult) -> Unit): Any
    abstract fun downloadFile(remotePath: String, onResult: (OperationTaskResult) -> Unit): Any

    abstract fun listFiles(
        remoteDir: String,
        filter: ((RemoteFile) -> Boolean)? = null,
        onResult: (OperationTaskResult) -> Unit
    ): Any

    abstract fun uploadFile(
        localFile: File,
        remotePath: String,
        mimeType: String?,
        onResult: (OperationTaskResult) -> Unit
    ): Any

    private fun logTasks() {
        log("Running tasks: ${runningTasks.map { it.javaClass.simpleName }}")
        log("Waiting tasks: ${waitingTasks.map { it.javaClass.simpleName }}")
    }

    open fun getAbsolutePath(vararg segments: String) = segments.joinToString("/") { it.trim('/') }

    fun registerTask(task: Task<*, *>, triggerStatus: Int, callback: () -> Unit) {
        log(
            "registerTask: task=${task.javaClass.simpleName}, triggerStatus=$triggerStatus, status=$status",
            level = Log.DEBUG
        )
        tasks.value = tasks.value.toMutableList().apply { add(task) }
        task.addOnFinishedListener { logTasks() }
        if (status >= triggerStatus && runningNonMetaTasks.size < 3) callback()
        else ioScope.launch {
            while (status < triggerStatus || runningNonMetaTasks.size >= 3) delay(1_000)
            callback()
        }
        logTasks()
    }

    protected fun test(onResult: ((TestTaskResult) -> Unit)? = null) {
        if (status == STATUS_TESTING) {
            ioScope.launch {
                while (status == STATUS_TESTING) delay(100)
                test(onResult)
            }
        } else if (status == STATUS_DISABLED) {
            ioScope.launch {
                while (status == STATUS_DISABLED) delay(10_000)
                test(onResult)
            }
        } else if (status < STATUS_AUTH_ERROR) {
            // On auth error, don't even try anything until URL/username/PW has changed.
            status = STATUS_TESTING
            TestTask(this).run(STATUS_TESTING) { result ->
                status = when (result.status) {
                    TaskResult.Status.OK -> STATUS_OK
                    TaskResult.Status.AUTH_ERROR -> STATUS_AUTH_ERROR
                    else -> STATUS_ERROR
                }
                onResult?.invoke(result)

                // Schedule low-frequency retries for as long as needed:
                if (status < STATUS_AUTH_ERROR && !isTestScheduled) {
                    ioScope.launch {
                        isTestScheduled = true
                        while (status < STATUS_AUTH_ERROR) {
                            delay(30_000)
                            test()
                        }
                        isTestScheduled = false
                    }
                }
            }
        } else onResult?.invoke(
            TestTaskResult(
                status = when (status) {
                    STATUS_OK -> TaskResult.Status.OK
                    STATUS_AUTH_ERROR -> TaskResult.Status.AUTH_ERROR
                    else -> TaskResult.Status.OTHER_ERROR
                }
            )
        )
    }

    companion object {
        const val STATUS_DISABLED = 0
        const val STATUS_TESTING = 1
        const val STATUS_READY = 2
        const val STATUS_ERROR = 3
        const val STATUS_AUTH_ERROR = 4
        const val STATUS_OK = 5
    }
}
