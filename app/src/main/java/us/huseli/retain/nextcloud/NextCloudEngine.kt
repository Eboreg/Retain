package us.huseli.retain.nextcloud

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import us.huseli.retain.Constants.NEXTCLOUD_BASE_DIR
import us.huseli.retain.Constants.PREF_NEXTCLOUD_BASE_DIR
import us.huseli.retain.Constants.PREF_NEXTCLOUD_ENABLED
import us.huseli.retain.Constants.PREF_NEXTCLOUD_PASSWORD
import us.huseli.retain.Constants.PREF_NEXTCLOUD_URI
import us.huseli.retain.Constants.PREF_NEXTCLOUD_USERNAME
import us.huseli.retain.InstantAdapter
import us.huseli.retain.LogInterface
import us.huseli.retain.Logger
import us.huseli.retain.nextcloud.tasks.BaseTask
import us.huseli.retain.nextcloud.tasks.TestNextCloudTask
import us.huseli.retain.nextcloud.tasks.TestNextCloudTaskResult
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NextCloudEngine @Inject constructor(
    @ApplicationContext internal val context: Context,
    internal val ioScope: CoroutineScope,
    override val logger: Logger,
) : SharedPreferences.OnSharedPreferenceChangeListener, LogInterface {
    internal val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Instant::class.java, InstantAdapter())
        .create()
    internal val tempDirUp = File(context.cacheDir, "up").apply { mkdirs() }
    internal val tempDirDown = File(context.cacheDir, "down").apply { mkdirs() }
    internal val listenerHandler = Handler(Looper.getMainLooper())

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private var tasks: List<BaseTask<*>> = listOf()

    private var isTestScheduled = false
    private var status = STATUS_DISABLED

    private var isEnabled: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                updateClient(isEnabled = value)
            }
        }

    private var uri: Uri = Uri.EMPTY
        set(value) {
            if (field != value) {
                field = value
                updateClient(uri = value)
            }
        }

    private var username = ""
        set(value) {
            if (field != value) {
                field = value
                updateClient(username = value)
            }
        }

    private var password = ""
        set(value) {
            if (field != value) {
                field = value
                updateClient(password = value)
            }
        }

    private var baseDir = NEXTCLOUD_BASE_DIR
        set(value) {
            if (field != value.trimEnd('/')) field = value.trimEnd('/')
        }

    internal val client: OwnCloudClient =
        OwnCloudClientFactory.createOwnCloudClient(uri, context, true).apply {
            setDefaultTimeouts(120_000, 120_000)
        }

    private val runningTasks: List<BaseTask<*>>
        get() = tasks.filter { it.status == BaseTask.STATUS_RUNNING }

    private val runningNonMetaTasks: List<BaseTask<*>>
        get() = runningTasks.filter { !it.isMetaTask }

    private val waitingTasks: List<BaseTask<*>>
        get() = tasks.filter { it.status == BaseTask.STATUS_WAITING }

    init {
        // These must be set here and not inline, because otherwise the set()
        // methods are not run.
        uri = Uri.parse(preferences.getString(PREF_NEXTCLOUD_URI, "") ?: "")
        username = preferences.getString(PREF_NEXTCLOUD_USERNAME, "") ?: ""
        password = preferences.getString(PREF_NEXTCLOUD_PASSWORD, "") ?: ""
        baseDir = preferences.getString(PREF_NEXTCLOUD_BASE_DIR, NEXTCLOUD_BASE_DIR) ?: NEXTCLOUD_BASE_DIR
        isEnabled = preferences.getBoolean(PREF_NEXTCLOUD_ENABLED, false)
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    fun getAbsolutePath(vararg segments: String) =
        listOf(
            baseDir.trimEnd('/'),
            *segments.map { it.trim('/') }.toTypedArray()
        ).joinToString("/")

    private fun logTasks() {
        log("Running tasks: ${runningTasks.map { it.javaClass.simpleName }}")
        log("Waiting tasks: ${waitingTasks.map { it.javaClass.simpleName }}")
    }

    fun registerTask(task: BaseTask<*>, triggerStatus: Int, callback: () -> Unit) {
        log(
            "registerTask: task=${task.javaClass.simpleName}, triggerStatus=$triggerStatus, status=$status",
            level = Log.DEBUG
        )
        tasks = tasks.toMutableList().apply { add(task) }
        task.addOnFinishedListener { logTasks() }
        if (status >= triggerStatus && runningNonMetaTasks.size < 3) callback()
        else {
            ioScope.launch {
                while (status < triggerStatus || runningNonMetaTasks.size >= 3) delay(1_000)
                callback()
            }
        }
        logTasks()
    }

    fun testClient(
        uri: Uri,
        username: String,
        password: String,
        baseDir: String,
        callback: ((TestNextCloudTaskResult) -> Unit)? = null
    ) {
        this.uri = uri
        this.username = username
        this.password = password
        this.baseDir = baseDir
        if (this.uri.host != null) testClient(callback)
    }

    private fun testClient(callback: ((TestNextCloudTaskResult) -> Unit)? = null) {
        if (status == STATUS_TESTING) {
            ioScope.launch {
                while (status == STATUS_TESTING) delay(100)
                testClient(callback)
            }
        } else if (status == STATUS_DISABLED) {
            ioScope.launch {
                while (status == STATUS_DISABLED) delay(10_000)
                testClient(callback)
            }
        } else if (status < STATUS_AUTH_ERROR) {
            // On auth error, don't even try anything until URL/username/PW has changed.
            status = STATUS_TESTING
            TestNextCloudTask(this).run(STATUS_TESTING) { result ->
                status = result.status
                callback?.invoke(result)

                // Schedule low-frequency retries for as long as needed:
                if (status < STATUS_AUTH_ERROR && !isTestScheduled) {
                    ioScope.launch {
                        isTestScheduled = true
                        while (status < STATUS_AUTH_ERROR) {
                            delay(30_000)
                            testClient()
                        }
                        isTestScheduled = false
                    }
                }
            }
        } else callback?.invoke(
            TestNextCloudTaskResult(
                success = status == STATUS_OK,
                error = null,
                status = status,
            )
        )
    }

    private fun updateClient(
        uri: Uri? = null,
        username: String? = null,
        password: String? = null,
        isEnabled: Boolean? = null
    ) {
        log(
            "updateClient: uri=$uri, username=$username, password=$password, isEnabled=$isEnabled",
            level = Log.DEBUG
        )
        if (uri != null) client.baseUri = uri
        if (username != null || password != null) {
            client.credentials = OwnCloudCredentialsFactory.newBasicCredentials(
                username ?: this.username,
                password ?: this.password
            )
            if (username != null) client.userId = username
        }
        if (isEnabled != null) {
            status = if (isEnabled) STATUS_READY else STATUS_DISABLED
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_NEXTCLOUD_URI -> uri = Uri.parse(preferences.getString(key, "") ?: "")
            PREF_NEXTCLOUD_USERNAME -> username = preferences.getString(key, "") ?: ""
            PREF_NEXTCLOUD_PASSWORD -> password = preferences.getString(key, "") ?: ""
            PREF_NEXTCLOUD_BASE_DIR -> baseDir = preferences.getString(key, NEXTCLOUD_BASE_DIR) ?: NEXTCLOUD_BASE_DIR
            PREF_NEXTCLOUD_ENABLED -> isEnabled = preferences.getBoolean(key, false)
        }
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
