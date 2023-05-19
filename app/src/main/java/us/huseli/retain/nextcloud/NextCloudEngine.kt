package us.huseli.retain.nextcloud

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Handler
import android.os.Looper
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
import us.huseli.retain.Constants.PREF_NEXTCLOUD_PASSWORD
import us.huseli.retain.Constants.PREF_NEXTCLOUD_URI
import us.huseli.retain.Constants.PREF_NEXTCLOUD_USERNAME
import us.huseli.retain.LogInterface
import us.huseli.retain.Logger
import us.huseli.retain.helpers.InstantAdapter
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
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    internal val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Instant::class.java, InstantAdapter())
        .create()
    internal val tempDirUp = File(context.cacheDir, "up").also { it.mkdir() }
    internal val tempDirDown = File(context.cacheDir, "down").also { it.mkdir() }
    internal val listenerHandler = Handler(Looper.getMainLooper())

    private var isTestScheduled = false
    private var status = STATUS_READY

    internal var uri: Uri = Uri.EMPTY
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

    internal val client: OwnCloudClient = OwnCloudClientFactory.createOwnCloudClient(uri, context, true).apply {
        setDefaultTimeouts(120_000, 120_000)
    }

    init {
        uri = Uri.parse(preferences.getString(PREF_NEXTCLOUD_URI, "") ?: "")
        username = preferences.getString(PREF_NEXTCLOUD_USERNAME, "") ?: ""
        preferences.registerOnSharedPreferenceChangeListener(this)
        password = preferences.getString(PREF_NEXTCLOUD_PASSWORD, "") ?: ""
    }

    fun awaitStatus(value: Int, callback: () -> Unit) {
        if (status >= value) callback()
        else {
            ioScope.launch {
                while (status < value) delay(1_000)
                callback()
            }
        }
    }

    fun testClient(
        uri: Uri,
        username: String,
        password: String,
        callback: ((TestNextCloudTaskResult) -> Unit)? = null
    ) {
        this.uri = uri
        this.username = username
        this.password = password
        if (this.uri.host != null) testClient(callback)
    }

    private fun testClient(callback: ((TestNextCloudTaskResult) -> Unit)? = null) {
        if (status == STATUS_TESTING) {
            ioScope.launch {
                while (status == STATUS_TESTING) delay(100)
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

    private fun updateClient(uri: Uri? = null, username: String? = null, password: String? = null) {
        if (username != null || password != null || uri != null) {
            if (uri != null) client.baseUri = uri
            if (username != null || password != null) {
                client.credentials = OwnCloudCredentialsFactory.newBasicCredentials(
                    username ?: this.username,
                    password ?: this.password
                )
                if (username != null) client.userId = username
            }
            status = STATUS_READY
            log("Client updated: baseUri=${client.baseUri}, userId=${client.userId}")
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_NEXTCLOUD_URI -> this.uri = Uri.parse(preferences.getString(key, "") ?: "")
            PREF_NEXTCLOUD_USERNAME -> this.username = preferences.getString(key, "") ?: ""
            PREF_NEXTCLOUD_PASSWORD -> this.password = preferences.getString(key, "") ?: ""
        }
    }

    companion object {
        const val STATUS_TESTING = 1
        const val STATUS_READY = 2
        const val STATUS_ERROR = 3
        const val STATUS_AUTH_ERROR = 4
        const val STATUS_OK = 5
    }
}
