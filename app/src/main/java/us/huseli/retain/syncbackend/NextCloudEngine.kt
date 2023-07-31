package us.huseli.retain.syncbackend

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation
import com.owncloud.android.lib.resources.files.DownloadFileRemoteOperation
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation
import com.owncloud.android.lib.resources.files.RemoveFileRemoteOperation
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.retain.Constants.DEFAULT_NEXTCLOUD_BASE_DIR
import us.huseli.retain.Constants.PREF_NEXTCLOUD_BASE_DIR
import us.huseli.retain.Constants.PREF_NEXTCLOUD_PASSWORD
import us.huseli.retain.Constants.PREF_NEXTCLOUD_URI
import us.huseli.retain.Constants.PREF_NEXTCLOUD_USERNAME
import us.huseli.retain.Constants.PREF_SYNC_BACKEND
import us.huseli.retain.Enums.SyncBackend
import us.huseli.retain.Logger
import us.huseli.retain.syncbackend.tasks.OperationTaskResult
import us.huseli.retain.syncbackend.tasks.RemoteFile
import us.huseli.retain.syncbackend.tasks.TaskResult
import us.huseli.retain.syncbackend.tasks.TestTaskResult
import java.io.File
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NextCloudEngine @Inject constructor(
    @ApplicationContext context: Context,
    ioScope: CoroutineScope,
    override val logger: Logger,
) : SharedPreferences.OnSharedPreferenceChangeListener, Engine(context, ioScope) {
    override val backend: SyncBackend = SyncBackend.NEXTCLOUD

    private val _isTesting = MutableStateFlow(false)

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

    private var baseDir = DEFAULT_NEXTCLOUD_BASE_DIR
        set(value) {
            if (field != value.trimEnd('/')) field = value.trimEnd('/')
        }

    private val client: OwnCloudClient =
        OwnCloudClientFactory.createOwnCloudClient(uri, context, true).apply {
            setDefaultTimeouts(120_000, 120_000)
        }

    val isTesting = _isTesting.asStateFlow()

    init {
        // These must be set here and not inline, because otherwise the set()
        // methods are not run.
        uri = Uri.parse(preferences.getString(PREF_NEXTCLOUD_URI, "") ?: "")
        username = preferences.getString(PREF_NEXTCLOUD_USERNAME, "") ?: ""
        password = preferences.getString(PREF_NEXTCLOUD_PASSWORD, "") ?: ""
        baseDir =
            preferences.getString(PREF_NEXTCLOUD_BASE_DIR, DEFAULT_NEXTCLOUD_BASE_DIR) ?: DEFAULT_NEXTCLOUD_BASE_DIR
        status =
            if (preferences.getString(PREF_SYNC_BACKEND, null) == SyncBackend.NEXTCLOUD.name) STATUS_READY
            else STATUS_DISABLED
        preferences.registerOnSharedPreferenceChangeListener(this)

        ioScope.launch {
            syncBackend.collect {
                if (it != backend) status = STATUS_DISABLED
                else if (status == STATUS_DISABLED) status = STATUS_READY
            }
        }
    }

    private fun resultToStatus(result: RemoteOperationResult<*>): TaskResult.Status =
        if (result.isSuccess) TaskResult.Status.OK
        else if (result.code == RemoteOperationResult.ResultCode.FILE_NOT_FOUND) TaskResult.Status.PATH_NOT_FOUND
        else if (
            result.code == RemoteOperationResult.ResultCode.UNAUTHORIZED ||
            result.code == RemoteOperationResult.ResultCode.FORBIDDEN
        ) TaskResult.Status.AUTH_ERROR
        else if (result.exception is UnknownHostException) TaskResult.Status.UNKNOWN_HOST
        else if (result.code == RemoteOperationResult.ResultCode.FILE_NOT_FOUND) TaskResult.Status.CONNECT_ERROR
        else TaskResult.Status.OTHER_ERROR

    private fun castResult(
        result: RemoteOperationResult<*>,
        status: TaskResult.Status? = null,
        remoteFiles: List<RemoteFile> = emptyList(),
        localFiles: List<File> = emptyList(),
        objects: List<Any> = emptyList(),
    ): OperationTaskResult = OperationTaskResult(
        status = status ?: resultToStatus(result),
        exception = result.exception,
        message = result.message ?: result.logMessage,
        remoteFiles = remoteFiles,
        localFiles = localFiles,
        objects = objects,
    )

    private fun executeRemoteOperation(
        operation: RemoteOperation<*>,
        onResult: (RemoteOperationResult<*>) -> Unit
    ) {
        operation.execute(client, { _, result -> onResult(result) }, listenerHandler)
    }

    private fun updateClient(
        uri: Uri? = null,
        username: String? = null,
        password: String? = null,
        isEnabled: Boolean? = null
    ) {
        log(
            message = "updateClient: uri=$uri, username=$username, password=$password, isEnabled=$isEnabled",
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
        if (isEnabled == false) status = STATUS_DISABLED
        else if (isEnabled == true || status != STATUS_DISABLED) status = STATUS_READY
    }

    fun test(
        uri: Uri,
        username: String,
        password: String,
        baseDir: String,
        onResult: (TestTaskResult) -> Unit
    ) {
        this.uri = uri
        this.username = username
        this.password = password
        this.baseDir = baseDir
        if (this.uri.host != null) {
            _isTesting.value = true
            test { result ->
                _isTesting.value = false
                onResult(result)
            }
        }
    }

    override fun createDir(remoteDir: String, onResult: (OperationTaskResult) -> Unit) =
        executeRemoteOperation(CreateFolderRemoteOperation(remoteDir, true)) { result ->
            val status =
                if (result.code == RemoteOperationResult.ResultCode.FOLDER_ALREADY_EXISTS) TaskResult.Status.OK
                else resultToStatus(result)
            onResult(castResult(result, status = status, objects = listOf(remoteDir)))
        }

    override fun downloadFile(remotePath: String, localFile: File, onResult: (OperationTaskResult) -> Unit) =
        executeRemoteOperation(DownloadFileRemoteOperation(remotePath, tempDirDown.absolutePath + '/')) { result ->
            val tmpFile = File(tempDirDown, remotePath)
            if (result.isSuccess && tmpFile.absolutePath != localFile.absolutePath) {
                File(tempDirDown, remotePath).renameTo(localFile)
            }
            onResult(
                castResult(
                    result,
                    localFiles = listOf(localFile),
                    objects = listOf(remotePath)
                )
            )
        }

    override fun getAbsolutePath(vararg segments: String) =
        super.getAbsolutePath(baseDir.trimEnd('/'), *segments)

    @Suppress("DEPRECATION")
    override fun listFiles(
        remoteDir: String,
        filter: (RemoteFile) -> Boolean,
        onResult: (OperationTaskResult) -> Unit
    ) {
        executeRemoteOperation(ReadFolderRemoteOperation(remoteDir)) { result ->
            onResult(
                castResult(
                    result = result,
                    remoteFiles = result.data
                        .filterIsInstance<com.owncloud.android.lib.resources.files.model.RemoteFile>()
                        .map { RemoteFile(it.remotePath, it.length, it.mimeType == "DIR") }
                        .filter(filter)
                )
            )
        }
    }

    override fun removeFile(remotePath: String, onResult: (OperationTaskResult) -> Unit) =
        executeRemoteOperation(RemoveFileRemoteOperation(remotePath)) { result ->
            onResult(castResult(result, objects = listOf(remotePath)))
        }

    override fun uploadFile(
        localFile: File,
        remotePath: String,
        mimeType: String?,
        onResult: (OperationTaskResult) -> Unit
    ) = executeRemoteOperation(
        UploadFileRemoteOperation(
            localFile.absolutePath,
            remotePath,
            mimeType,
            (System.currentTimeMillis() / 1000).toString()
        )
    ) { result -> onResult(castResult(result, localFiles = listOf(localFile), objects = listOf(remotePath))) }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_NEXTCLOUD_URI -> uri = Uri.parse(preferences.getString(key, "") ?: "")
            PREF_NEXTCLOUD_USERNAME -> username = preferences.getString(key, "") ?: ""
            PREF_NEXTCLOUD_PASSWORD -> password = preferences.getString(key, "") ?: ""
            PREF_NEXTCLOUD_BASE_DIR -> baseDir =
                preferences.getString(key, DEFAULT_NEXTCLOUD_BASE_DIR) ?: DEFAULT_NEXTCLOUD_BASE_DIR
            PREF_SYNC_BACKEND -> updateSyncBackend()
        }
    }
}
