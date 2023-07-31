package us.huseli.retain.syncbackend

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.dropbox.core.AccessErrorException
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.InvalidAccessTokenException
import com.dropbox.core.NetworkIOException
import com.dropbox.core.RateLimitException
import com.dropbox.core.android.Auth
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.CreateFolderErrorException
import com.dropbox.core.v2.files.DownloadErrorException
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.FolderMetadata
import com.dropbox.core.v2.files.WriteMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.retain.BuildConfig
import us.huseli.retain.Constants.PREF_DROPBOX_CREDENTIAL
import us.huseli.retain.Constants.PREF_SYNC_BACKEND
import us.huseli.retain.Enums.SyncBackend
import us.huseli.retain.Logger
import us.huseli.retain.syncbackend.tasks.OperationTaskResult
import us.huseli.retain.syncbackend.tasks.RemoteFile
import us.huseli.retain.syncbackend.tasks.TaskResult
import us.huseli.retain.syncbackend.tasks.TestTaskResult
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DropboxEngine @Inject constructor(
    @ApplicationContext context: Context,
    ioScope: CoroutineScope,
    override val logger: Logger,
) : Engine(context, ioScope), SharedPreferences.OnSharedPreferenceChangeListener {
    override val backend: SyncBackend = SyncBackend.DROPBOX

    private val scopes = listOf(
        "files.metadata.write",
        "files.metadata.read",
        "files.content.write",
        "files.content.read",
        "account_info.read",
    )
    private val credential = MutableStateFlow<DbxCredential?>(null)
    private val requestConfig = DbxRequestConfig.newBuilder("retain/${BuildConfig.VERSION_NAME}").build()
    private var isAwaitingResult = false
    private var client: DbxClientV2 = DbxClientV2(requestConfig, "")
    private val _isTesting = MutableStateFlow(false)
    private val _accountEmail = MutableStateFlow("")

    val isTesting = _isTesting.asStateFlow()
    val isAuthenticated = credential.map { it != null }
    val accountEmail = _accountEmail.asStateFlow()

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
        preferences.getString(PREF_DROPBOX_CREDENTIAL, null)?.let { updateClient(it) }
        status =
            if (preferences.getString(PREF_SYNC_BACKEND, null) == SyncBackend.DROPBOX.name) STATUS_READY
            else STATUS_DISABLED

        ioScope.launch {
            syncBackend.collect {
                if (it != backend) status = STATUS_DISABLED
                else if (status == STATUS_DISABLED) status = STATUS_READY
            }
        }
    }

    fun authenticate() {
        Auth.startOAuth2PKCE(context, BuildConfig.dropboxAppKey, requestConfig, scopes)
        isAwaitingResult = true
    }

    fun revoke() {
        ioScope.launch(Dispatchers.IO) {
            try {
                wrapRequest { client.auth().tokenRevoke() }
            } catch (e: Exception) {
                log("DropboxEnging.revoke(): $e", level = Log.ERROR)
            }
        }
        preferences.edit().putString(PREF_DROPBOX_CREDENTIAL, null).apply()
    }

    fun onResume() {
        if (isAwaitingResult) {
            val credential = Auth.getDbxCredential()
            isAwaitingResult = false
            if (credential != null) {
                preferences
                    .edit()
                    .putString(PREF_DROPBOX_CREDENTIAL, DbxCredential.Writer.writeToString(credential))
                    .apply()
            }
        }
    }

    private fun exceptionToResult(
        exception: Exception,
        localFiles: List<File> = emptyList(),
        objects: List<Any> = emptyList()
    ): OperationTaskResult {
        val status = when (exception) {
            is InvalidAccessTokenException -> TaskResult.Status.AUTH_ERROR
            is NetworkIOException -> TaskResult.Status.CONNECT_ERROR
            is AccessErrorException -> TaskResult.Status.AUTH_ERROR
            else -> TaskResult.Status.OTHER_ERROR
        }
        return OperationTaskResult(status = status, exception = exception, localFiles = localFiles, objects = objects)
    }

    private fun getAccountEmail() = ioScope.launch(Dispatchers.IO) {
        try {
            _accountEmail.value = wrapRequest { client.users().currentAccount.email }
        } catch (e: Exception) {
            showError("DropboxEngine.getAccountEmail()", e)
        }
    }

    private fun updateClient(credentialBody: String?) {
        if (credentialBody == null) credential.value = null
        else {
            try {
                credential.value = DbxCredential.Reader.readFully(credentialBody).also {
                    client = DbxClientV2(requestConfig, it)
                    getAccountEmail()
                }
            } catch (e: Exception) {
                showError("DropboxEngine.updateClient()", e)
            }
        }
    }

    private suspend fun <T> wrapRequest(request: () -> T): T {
        var retries = 0
        while (true) {
            try {
                return request()
            } catch (e: Exception) {
                if (e is RateLimitException) {
                    if (++retries == 5) throw e
                    else delay(e.backoffMillis)
                } else throw e
            }
        }
    }

    override fun createDir(
        remoteDir: String,
        onResult: (OperationTaskResult) -> Unit
    ) = ioScope.launch(Dispatchers.IO) {
        val okResult = OperationTaskResult(status = TaskResult.Status.OK, objects = listOf(remoteDir))

        try {
            wrapRequest { client.files().createFolderV2(remoteDir) }
            onResult(okResult)
        } catch (e: Exception) {
            if (e is CreateFolderErrorException && e.errorValue.pathValue.isConflict)
                onResult(okResult)
            else
                onResult(exceptionToResult(e, objects = listOf(remoteDir)))
        }
    }

    override fun downloadFile(
        remotePath: String,
        localFile: File,
        onResult: (OperationTaskResult) -> Unit
    ) = ioScope.launch(Dispatchers.IO) {
        try {
            val fileMeta: FileMetadata

            FileOutputStream(localFile).use { outputStream ->
                fileMeta = wrapRequest { client.files().download(remotePath).download(outputStream) }
            }
            onResult(
                OperationTaskResult(
                    status = TaskResult.Status.OK,
                    remoteFiles = listOf(
                        RemoteFile(
                            name = fileMeta.pathLower,
                            size = fileMeta.size,
                            isDirectory = false
                        )
                    ),
                    localFiles = listOf(localFile),
                )
            )
        } catch (e: Exception) {
            if (e is DownloadErrorException && e.errorValue.pathValue.isNotFound)
                onResult(
                    OperationTaskResult(
                        status = TaskResult.Status.PATH_NOT_FOUND,
                        exception = e,
                        localFiles = listOf(localFile)
                    )
                )
            else
                onResult(exceptionToResult(e, localFiles = listOf(localFile)))
        }
    }

    override fun listFiles(
        remoteDir: String,
        filter: (RemoteFile) -> Boolean,
        onResult: (OperationTaskResult) -> Unit
    ) = ioScope.launch(Dispatchers.IO) {
        try {
            onResult(
                OperationTaskResult(
                    status = TaskResult.Status.OK,
                    remoteFiles = wrapRequest { client.files().listFolder(remoteDir) }.entries.map {
                        RemoteFile(
                            name = it.pathLower,
                            size = (it as? FileMetadata)?.size ?: 0,
                            isDirectory = it is FolderMetadata,
                        )
                    }.filter(filter)
                )
            )
        } catch (e: Exception) {
            onResult(exceptionToResult(e, objects = listOf(remoteDir)))
        }
    }

    override fun removeFile(
        remotePath: String,
        onResult: (OperationTaskResult) -> Unit
    ) = ioScope.launch(Dispatchers.IO) {
        try {
            wrapRequest { client.files().deleteV2(remotePath) }
            onResult(OperationTaskResult(status = TaskResult.Status.OK, objects = listOf(remotePath)))
        } catch (e: Exception) {
            onResult(exceptionToResult(e, objects = listOf(remotePath)))
        }
    }

    override fun uploadFile(
        localFile: File,
        remotePath: String,
        mimeType: String?,
        onResult: (OperationTaskResult) -> Unit
    ) = ioScope.launch(Dispatchers.IO) {
        try {
            FileInputStream(localFile).use { inputStream ->
                wrapRequest {
                    client.files()
                        .uploadBuilder(remotePath)
                        .withMode(WriteMode.OVERWRITE)
                        .uploadAndFinish(inputStream)
                }
            }
            onResult(
                OperationTaskResult(
                    status = TaskResult.Status.OK,
                    localFiles = listOf(localFile),
                    objects = listOf(remotePath),
                )
            )
        } catch (e: Exception) {
            onResult(
                exceptionToResult(
                    exception = e,
                    localFiles = listOf(localFile),
                    objects = listOf(remotePath)
                )
            )
        }
    }

    override fun getAbsolutePath(vararg segments: String) = '/' + super.getAbsolutePath(*segments)

    override fun test(onResult: (TestTaskResult) -> Unit) {
        _isTesting.value = true
        super.test { result ->
            _isTesting.value = false
            onResult(result)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_DROPBOX_CREDENTIAL -> updateClient(preferences.getString(key, null))
            PREF_SYNC_BACKEND -> updateSyncBackend()
        }
    }
}
