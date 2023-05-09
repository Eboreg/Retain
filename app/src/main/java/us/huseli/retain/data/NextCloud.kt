package us.huseli.retain.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation
import com.owncloud.android.lib.resources.files.DownloadFileRemoteOperation
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.huseli.retain.Logger
import us.huseli.retain.LoggingObject
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.Note
import us.huseli.retain.data.entities.NoteCombined
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.lang.reflect.Type
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

class InstantAdapter : JsonSerializer<Instant>, JsonDeserializer<Instant?> {
    override fun serialize(src: Instant?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement =
        src?.let { JsonPrimitive(it.toString()) } ?: JsonNull.INSTANCE

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Instant? =
        json?.asString?.let { Instant.parse(it) }
}

@Singleton
class NextCloud @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ioScope: CoroutineScope,
    override var logger: Logger?,
) : SharedPreferences.OnSharedPreferenceChangeListener, LoggingObject {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val gson = GsonBuilder()
        .registerTypeAdapter(Instant::class.java, InstantAdapter())
        .create()
    private val localDirUp = File(context.cacheDir, "up").also { it.mkdir() }
    private val localDirDown = File(context.cacheDir, "down").also { it.mkdir() }
    private val remoteDir = "/.retain"
    private val listenerHandler = Handler(Looper.getMainLooper())

    private var uri = preferences.getString("nextCloudUri", null)
    private var username = preferences.getString("nextCloudUsername", null)
    private var password = preferences.getString("nextCloudPassword", null)
    private var client: OwnCloudClient? = null

    private val createRemoteDirListener = OnRemoteOperationListener { _, result ->
        if (!result.isSuccess && result.code != RemoteOperationResult.ResultCode.FOLDER_ALREADY_EXISTS) {
            val error = result.exception?.toString() ?: result.logMessage
            logError("Couldn't create remote directory $remoteDir on Nextcloud: $error")
            isReady.value = false
        } else {
            log("createRemoteDirListener: success!")
            isReady.value = true
        }
    }

    private val isReady = MutableStateFlow(false)

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
        initClient()
    }

    fun onReady(callback: () -> Unit) {
        if (isReady.value) callback()
        else ioScope.launch {
            isReady.transformWhile {
                if (it) emit(true)
                !it
            }.collect {
                callback()
            }
        }
    }

    private fun createRemoteDir(listener: OnRemoteOperationListener = createRemoteDirListener) {
        client?.let { client ->
            CreateFolderRemoteOperation(remoteDir, true).execute(client, listener, listenerHandler)
        }
    }

    private fun initClient() {
        val constUri = uri
        val constUsername = username
        val constPassword = password

        if (constUri != null && constUsername != null && constPassword != null) {
            client = OwnCloudClientFactory.createOwnCloudClient(Uri.parse(constUri), context, true).also {
                it.credentials = OwnCloudCredentialsFactory.newBasicCredentials(constUsername, constPassword)
                it.userId = constUsername
                it.setDefaultTimeouts(120000, 120000)
            }
            createRemoteDir()
        }
    }

    internal fun logError(message: String) {
        log(message, Log.ERROR, addToFlow = true)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        log("onSharedPreferenceChanged: key=$key, value=${preferences.getString(key, "")}")
        when (key) {
            "nextCloudUri" -> {
                val newUri = preferences.getString(key, "")
                if (newUri != uri) {
                    uri = newUri
                    initClient()
                }
            }

            "nextCloudUsername" -> {
                val newUsername = preferences.getString(key, "")
                if (newUsername != username) {
                    username = newUsername
                    initClient()
                }
            }

            "nextCloudPassword" -> {
                val newPassword = preferences.getString(key, "")
                if (newPassword != password) {
                    password = newPassword
                    initClient()
                }
            }
        }
    }


    open inner class Task<T : Task<T>>(
        override var logger: Logger? = this@NextCloud.logger
    ) : LoggingObject {
        private var onReady: ((T) -> Unit)? = null

        fun run(onReady: (T) -> Unit) {
            this.onReady = onReady
            this@NextCloud.onReady {
                log("Ready to run!")
                start()
            }
        }

        open fun notifyReady() {
            log("Ready!")
            @Suppress("UNCHECKED_CAST")
            onReady?.invoke(this as T)
        }

        open fun start() {}
    }


    @Suppress("MemberVisibilityCanBePrivate")
    open inner class UpstreamSyncTask<T : UpstreamSyncTask<T>>(
        private val notes: Collection<NoteCombined>,
    ) : Task<T>() {
        var processedFileCount = 0
        val successPaths = mutableSetOf<String>()
        val failPaths = mutableSetOf<String>()
        val successCount: Int
            get() = successPaths.size
        val failCount: Int
            get() = failPaths.size

        override fun start() {
            log("Starting upstream sync of $notes")
            notes.forEach { note -> uploadFile(note) }
        }

        private fun onError(remotePath: String, error: String) {
            failPaths.add(remotePath)
            logError("UpstreamSyncTask $remotePath: $error")
        }

        private fun uploadFile(noteCombined: NoteCombined) {
            ioScope.launch {
                withContext(Dispatchers.IO) {
                    val filename = "note-${noteCombined.id}.json"
                    val remotePath = "$remoteDir/$filename"
                    val localFile = File(localDirUp, filename).apply { deleteOnExit() }

                    log("Uploading file $remotePath from $localFile")

                    try {
                        FileWriter(localFile).use { it.write(gson.toJson(noteCombined)) }
                        client?.let {
                            UploadFileRemoteOperation(
                                localFile.absolutePath,
                                remotePath,
                                "application/json",
                                (System.currentTimeMillis() / 1000).toString()
                            ).execute(
                                client,
                                { _, result ->
                                    uploadFileResult(result, noteCombined, remotePath, localFile)
                                },
                                listenerHandler
                            )
                        }
                    } catch (e: Exception) {
                        uploadFileResult(null, noteCombined, remotePath, localFile, e)
                    }
                }
            }
        }

        private fun uploadFileResult(
            result: RemoteOperationResult<*>?,
            noteCombined: NoteCombined,
            remotePath: String,
            localFile: File,
            exception: Exception? = null,
        ) {
            log("uploadFileResult: result=$result, noteCombined=$noteCombined, remotePath=$remotePath, localFile=$localFile, exception=$exception")
            if (exception != null) onError(remotePath, exception.toString())
            else if (!localFile.isFile) onError(remotePath, "$localFile is not a file")
            else if (result == null) onError(remotePath, "result is null")
            else if (!result.isSuccess) onError(remotePath, result.exception?.toString() ?: result.logMessage)
            else {
                successPaths.add(remotePath)
                log("Successfully saved $noteCombined to $remotePath on Nextcloud")
            }
            if (++processedFileCount == notes.size) notifyReady()
        }
    }


    inner class UploadNoteTask(note: Note, checklistItems: List<ChecklistItem> = emptyList(), databaseVersion: Int) :
        UpstreamSyncTask<UploadNoteTask>(notes = listOf(NoteCombined(note, checklistItems, databaseVersion))) {
        // Tertiary boolean!
        var success: Boolean? = null

        override fun notifyReady() {
            when {
                failPaths.isNotEmpty() -> success = false
                successPaths.isNotEmpty() -> success = true
            }
            log("Ready, success = $success")
            super.notifyReady()
        }
    }


    @Suppress("MemberVisibilityCanBePrivate")
    inner class DownstreamSyncTask : Task<DownstreamSyncTask>() {
        val remoteNotesCombined = mutableListOf<NoteCombined>()
        val remotePaths = mutableListOf<String>()
        var processedFileCount = 0
        val successPaths = mutableSetOf<String>()
        val failPaths = mutableSetOf<String>()

        private val readFolderResult = OnRemoteOperationListener { _, result ->
            if (result == null) logError("readFolderListener: result is null")
            else if (!result.isSuccess) {
                val error = result.exception?.toString() ?: result.logMessage
                logError("Couldn't read $remoteDir on Nextcloud: $error")
            } else {
                @Suppress("DEPRECATION")
                remotePaths.addAll(
                    result.data
                        .filterIsInstance<RemoteFile>()
                        .filter {
                            it.mimeType == "application/json" &&
                            it.remotePath.split("/").last().startsWith("note-")
                        }
                        .map { it.remotePath }
                )
                log("Will start downloading files: $remotePaths")
                remotePaths.forEach { path -> downloadFile(path) }
                // If there are no files, callback still needs to be run:
                if (remotePaths.isEmpty()) notifyReady()
            }
        }

        override fun start() {
            log("Starting downstream sync task")
            readFolder()
        }

        private fun downloadFile(remotePath: String) {
            val localFile = File(localDirDown, remotePath)

            log("Downloading $remotePath to $localFile")

            client?.let {
                DownloadFileRemoteOperation(remotePath, localDirDown.absolutePath).execute(
                    client,
                    { _, result -> downloadFileResult(result, localFile, remotePath) },
                    listenerHandler
                )
            }
        }

        private fun downloadFileResult(result: RemoteOperationResult<*>?, localFile: File, remotePath: String) {
            log("downloadFileResult: result=$result, localFile=$localFile, remotePath=$remotePath")
            if (!localFile.isFile) onError(remotePath, "$localFile is not a file")
            else if (result == null) onError(remotePath, "result is null")
            else if (!result.isSuccess) onError(remotePath, result.exception?.toString() ?: result.logMessage)
            else {
                ioScope.launch {
                    withContext(Dispatchers.IO) {
                        try {
                            val json = FileReader(localFile).use { it.readText() }
                            val noteCombined = gson.fromJson(json, NoteCombined::class.java)
                            if (noteCombined != null) {
                                log("Successfully parsed $noteCombined from Nextcloud")
                                remoteNotesCombined.add(noteCombined)
                                successPaths.add(remotePath)
                            } else onError(remotePath, "noteCombined is null")
                        } catch (e: Exception) {
                            onError(remotePath, e.toString())
                        } finally {
                            if (++processedFileCount == remotePaths.size) notifyReady()
                        }
                    }
                }
            }
        }

        private fun onError(remotePath: String, error: String) {
            failPaths.add(remotePath)
            logError("DownstreamSyncTask $remotePath: $error")
        }

        private fun readFolder() {
            client?.let {
                ReadFolderRemoteOperation(remoteDir).execute(client, readFolderResult, listenerHandler)
            }
        }
    }
}
