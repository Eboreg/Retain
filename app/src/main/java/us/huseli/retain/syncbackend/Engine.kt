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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import us.huseli.retain.Constants.PREF_SYNC_BACKEND
import us.huseli.retain.Enums.SyncBackend
import us.huseli.retain.ILogger
import us.huseli.retain.InstantAdapter
import us.huseli.retain.syncbackend.tasks.RemoteFile
import us.huseli.retain.syncbackend.tasks.TestTask
import us.huseli.retain.syncbackend.tasks.abstr.AbstractTask
import us.huseli.retain.syncbackend.tasks.result.OperationTaskResult
import us.huseli.retain.syncbackend.tasks.result.TaskResult
import us.huseli.retain.syncbackend.tasks.result.TestTaskResult
import us.huseli.retaintheme.utils.AbstractScopeHolder
import java.io.File
import java.time.Instant

abstract class Engine(internal val context: Context) : ILogger, AbstractScopeHolder() {
    abstract val backend: SyncBackend

    protected val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    protected val tasks = MutableStateFlow<List<AbstractTask<*, *>>>(emptyList())
    protected var status = STATUS_DISABLED
    protected val syncBackend: MutableStateFlow<SyncBackend> = MutableStateFlow(
        preferences.getString(PREF_SYNC_BACKEND, null)?.let { SyncBackend.valueOf(it) } ?: SyncBackend.NONE
    )

    internal val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Instant::class.java, InstantAdapter())
        .create()
    internal val tempDirUp = File(context.cacheDir, "up").apply { mkdirs() }
    internal val tempDirDown = File(context.cacheDir, "down").apply { mkdirs() }
    internal val listenerHandler = Handler(Looper.getMainLooper())

    private val runningTasks: List<AbstractTask<*, *>>
        get() = tasks.value.filter { it.status.value == AbstractTask.STATUS_RUNNING }

    private val runningNonMetaTasks: List<AbstractTask<*, *>>
        get() = runningTasks.filter { !it.isMetaTask }

    private val waitingTasks: List<AbstractTask<*, *>>
        get() = tasks.value.filter { it.status.value == AbstractTask.STATUS_WAITING }

    val isSyncing = MutableStateFlow(false)

    protected fun updateSyncBackend() {
        syncBackend.value =
            preferences.getString(PREF_SYNC_BACKEND, null)?.let { SyncBackend.valueOf(it) } ?: SyncBackend.NONE
    }

    abstract fun removeFile(remotePath: String, onResult: (OperationTaskResult) -> Unit): Any
    abstract fun createDir(remoteDir: String, onResult: (OperationTaskResult) -> Unit): Any
    abstract fun downloadFile(remotePath: String, localFile: File, onResult: (OperationTaskResult) -> Unit): Any

    abstract fun listFiles(
        remoteDir: String,
        filter: (RemoteFile) -> Boolean,
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

    fun cancelTasks() {
        tasks.value.forEach { task -> task.cancel() }
    }

    open fun getAbsolutePath(vararg segments: String) = segments.joinToString("/") { it.trim('/') }

    fun registerTask(task: AbstractTask<*, *>, triggerStatus: Int, startTask: () -> Unit) {
        log(
            message = "registerTask: task=${task.javaClass.simpleName}, triggerStatus=$triggerStatus, status=$status",
            priority = Log.DEBUG,
        )
        tasks.value = tasks.value.toMutableList().apply { add(task) }
        task.addOnFinishedListener { logTasks() }
        if (status >= triggerStatus && runningNonMetaTasks.size < 3) startTask()
        else launchOnIOThread {
            while (status < triggerStatus || runningNonMetaTasks.size >= 3) delay(1_000)
            startTask()
        }
        logTasks()
    }

    open fun test(onResult: (TestTaskResult) -> Unit) {
        if (status > STATUS_TESTING) {
            status = STATUS_TESTING
            TestTask(this).run(STATUS_TESTING) { result ->
                status = when (result.status) {
                    TaskResult.Status.OK -> STATUS_OK
                    TaskResult.Status.AUTH_ERROR -> STATUS_AUTH_ERROR
                    else -> STATUS_ERROR
                }
                onResult(result)
            }
        }
    }

    fun launch(block: suspend CoroutineScope.() -> Unit) = launchOnIOThread(block)

    companion object {
        const val STATUS_DISABLED = 0
        const val STATUS_TESTING = 1
        const val STATUS_READY = 2
        const val STATUS_ERROR = 3
        const val STATUS_AUTH_ERROR = 4
        const val STATUS_OK = 5
    }
}
