package us.huseli.retain.syncbackend

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.SftpException
import com.jcraft.jsch.UserInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.retain.Constants.PREF_SFTP_BASE_DIR
import us.huseli.retain.Constants.PREF_SFTP_HOSTNAME
import us.huseli.retain.Constants.PREF_SFTP_PASSWORD
import us.huseli.retain.Constants.PREF_SFTP_PORT
import us.huseli.retain.Constants.PREF_SFTP_USERNAME
import us.huseli.retain.Constants.PREF_SYNC_BACKEND
import us.huseli.retain.Constants.SFTP_BASE_DIR
import us.huseli.retain.Enums
import us.huseli.retain.Logger
import us.huseli.retain.syncbackend.tasks.OperationTaskResult
import us.huseli.retain.syncbackend.tasks.RemoteFile
import us.huseli.retain.syncbackend.tasks.TaskResult
import us.huseli.retain.syncbackend.tasks.TestTaskResult
import java.io.File
import java.net.ConnectException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SFTPEngine @Inject constructor(
    @ApplicationContext context: Context,
    override val logger: Logger,
    ioScope: CoroutineScope,
) : SharedPreferences.OnSharedPreferenceChangeListener, Engine(context, ioScope) {
    override val backend: Enums.SyncBackend = Enums.SyncBackend.SFTP
    private val jsch = JSch()
    private val knownHostsFile = File(context.filesDir, "known_hosts")
    private val _isTesting = MutableStateFlow(false)

    private val baseDir = MutableStateFlow(preferences.getString(PREF_SFTP_BASE_DIR, SFTP_BASE_DIR) ?: SFTP_BASE_DIR)
    private val hostname = MutableStateFlow(preferences.getString(PREF_SFTP_HOSTNAME, "") ?: "")
    private val password = MutableStateFlow(preferences.getString(PREF_SFTP_PASSWORD, "") ?: "")
    private val port = MutableStateFlow(preferences.getInt(PREF_SFTP_PORT, 22))
    private val username = MutableStateFlow(preferences.getString(PREF_SFTP_USERNAME, "") ?: "")
    private val isKeyApproved = MutableStateFlow(false)

    val promptYesNo = MutableStateFlow<String?>(null)
    val isTesting = _isTesting.asStateFlow()

    private val userInfo = object : UserInfo {
        override fun getPassphrase() = null
        override fun getPassword() = this@SFTPEngine.password.value
        override fun promptPassword(message: String?) = false
        override fun promptPassphrase(message: String?) = false
        override fun showMessage(message: String?) = Unit

        override fun promptYesNo(message: String?): Boolean {
            log("promptYesNo: message=$message")
            if (!isKeyApproved.value) promptYesNo.value = message
            return isKeyApproved.value
        }
    }

    init {
        status =
            if (preferences.getString(PREF_SYNC_BACKEND, null) == Enums.SyncBackend.SFTP.name) STATUS_READY
            else STATUS_DISABLED
        preferences.registerOnSharedPreferenceChangeListener(this)
        jsch.setKnownHosts(knownHostsFile.absolutePath)
    }

    fun approveKey() {
        isKeyApproved.value = true
        promptYesNo.value = null
    }

    fun denyKey() {
        isKeyApproved.value = false
        promptYesNo.value = null
    }

    private fun exceptionToResult(exception: Exception, objects: List<Any> = emptyList()): OperationTaskResult {
        val status =
            if (exception is JSchException) {
                when (exception.cause) {
                    is UnknownHostException -> TaskResult.Status.UNKNOWN_HOST
                    is ConnectException -> TaskResult.Status.CONNECT_ERROR
                    else -> TaskResult.Status.AUTH_ERROR
                }
            } else TaskResult.Status.OTHER_ERROR
        return OperationTaskResult(status = status, exception = exception, objects = objects)
    }

    private fun runCommand(
        onResult: ((OperationTaskResult) -> Unit)? = null,
        command: ChannelSftp.() -> Unit,
    ) {
        val session = jsch.getSession(username.value, hostname.value, port.value).apply { setPassword(password.value) }
        session.userInfo = userInfo

        try {
            session.connect()
            val channel = session.openChannel("sftp") as ChannelSftp
            channel.connect()
            channel.apply { command() }
            session.disconnect()
            if (onResult != null) onResult(OperationTaskResult(status = TaskResult.Status.OK))
        } catch (e: Exception) {
            log("runCommand: error=$e, session=$session", level = Log.ERROR)
            if (onResult != null) onResult(exceptionToResult(e))
            else throw e
        }
    }

    fun test(
        hostname: String,
        username: String,
        password: String,
        baseDir: String,
        onResult: ((TestTaskResult) -> Unit)? = null
    ) {
        this.hostname.value = hostname
        this.username.value = username
        this.password.value = password
        this.baseDir.value = baseDir
        if (this.hostname.value.isNotEmpty() && this.username.value.isNotEmpty() && this.password.value.isNotEmpty()) {
            _isTesting.value = true
            test { result ->
                _isTesting.value = false
                onResult?.invoke(result)
            }
        }
    }

    override fun createDir(remoteDir: String, onResult: (OperationTaskResult) -> Unit) = ioScope.launch {
        runCommand(
            command = {
                val dirs = remoteDir.split('/')
                dirs.forEachIndexed { index, _ ->
                    try {
                        mkdir(dirs.subList(0, index + 1).joinToString("/"))
                    } catch (e: SftpException) {
                        // It seems like id==4 means the directory already exists:
                        if (e.id != 4) throw e
                    }
                }
            },
            onResult = { result -> onResult(result.copy(objects = listOf(remoteDir))) },
        )
    }

    override fun downloadFile(remotePath: String, onResult: (OperationTaskResult) -> Unit) = ioScope.launch {
        runCommand(
            command = { get(remotePath, tempDirDown.absolutePath) },
            onResult = { result ->
                onResult(
                    result.copy(
                        localFiles = listOf(File(tempDirDown, remotePath.split('/').last())),
                        objects = listOf(remotePath)
                    )
                )
            },
        )
    }

    override fun getAbsolutePath(vararg segments: String) =
        super.getAbsolutePath(baseDir.value.trimEnd('/'), *segments)

    override fun listFiles(
        remoteDir: String,
        filter: ((RemoteFile) -> Boolean)?,
        onResult: (OperationTaskResult) -> Unit
    ) = ioScope.launch {
        try {
            runCommand {
                val lsResult = ls(remoteDir)
                onResult(
                    OperationTaskResult(
                        status = TaskResult.Status.OK,
                        remoteFiles = lsResult
                            .map { entry -> RemoteFile(entry.filename, entry.attrs.size, entry.attrs.isDir) }
                            .filter(filter ?: { true }),
                        objects = lsResult,
                    )
                )
            }
        } catch (e: Exception) {
            onResult(exceptionToResult(e, objects = listOf(remoteDir)))
        }
    }

    override fun removeFile(remotePath: String, onResult: (OperationTaskResult) -> Unit) = ioScope.launch {
        runCommand(
            command = { rm(remotePath) },
            onResult = { result -> onResult(result.copy(objects = listOf(remotePath))) },
        )
    }

    override fun uploadFile(
        localFile: File,
        remotePath: String,
        mimeType: String?,
        onResult: (OperationTaskResult) -> Unit
    ) = ioScope.launch {
        runCommand(
            command = { put(localFile.absolutePath, remotePath) },
            onResult = { result ->
                onResult(result.copy(localFiles = listOf(localFile), objects = listOf(remotePath)))
            },
        )
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_SFTP_BASE_DIR -> baseDir.value = preferences.getString(key, SFTP_BASE_DIR) ?: SFTP_BASE_DIR
            PREF_SFTP_HOSTNAME -> hostname.value = preferences.getString(key, "") ?: ""
            PREF_SFTP_PASSWORD -> password.value = preferences.getString(key, "") ?: ""
            PREF_SFTP_PORT -> port.value = preferences.getInt(key, 22)
            PREF_SFTP_USERNAME -> username.value = preferences.getString(key, "") ?: ""
            PREF_SYNC_BACKEND -> {
                if (preferences.getString(key, null) == Enums.SyncBackend.SFTP.name) {
                    if (status == STATUS_DISABLED) status = STATUS_READY
                } else status = STATUS_DISABLED
            }
        }
    }
}
