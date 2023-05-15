package us.huseli.retain.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
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
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation
import com.owncloud.android.lib.resources.files.DownloadFileRemoteOperation
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation
import com.owncloud.android.lib.resources.files.RemoveFileRemoteOperation
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import us.huseli.retain.Constants.IMAGE_SUBDIR
import us.huseli.retain.Constants.NEXTCLOUD_IMAGE_DIR
import us.huseli.retain.Constants.NEXTCLOUD_JSON_DIR
import us.huseli.retain.Constants.PREF_NEXTCLOUD_PASSWORD
import us.huseli.retain.Constants.PREF_NEXTCLOUD_URI
import us.huseli.retain.Constants.PREF_NEXTCLOUD_USERNAME
import us.huseli.retain.LogInterface
import us.huseli.retain.LogMessage
import us.huseli.retain.Logger
import us.huseli.retain.R
import us.huseli.retain.data.entities.Image
import us.huseli.retain.data.entities.NoteCombined
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.lang.reflect.Type
import java.net.UnknownHostException
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

class InstantAdapter : JsonSerializer<Instant>, JsonDeserializer<Instant?> {
    override fun serialize(src: Instant?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement =
        src?.let { JsonPrimitive(it.toString()) } ?: JsonNull.INSTANCE

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Instant? =
        json?.asString?.let { Instant.parse(it) }
}

@Parcelize
data class NextCloudTestResult(
    val success: Boolean,
    val result: RemoteOperationResult<*>?,
    val status: Int,
    val timestamp: Instant = Instant.now(),
    val isUrlFail: Boolean =
        !success &&
        (
            result?.exception is UnknownHostException ||
            result?.code == RemoteOperationResult.ResultCode.FILE_NOT_FOUND
        ),
    val isCredentialsFail: Boolean = !success && result?.code == RemoteOperationResult.ResultCode.UNAUTHORIZED,
) : Parcelable {
    override fun equals(other: Any?) = other is NextCloudTestResult && other.timestamp == timestamp

    override fun hashCode() = timestamp.hashCode()

    fun getErrorMessage(context: Context): String {
        val error = if (result != null) {
            when (result.code) {
                RemoteOperationResult.ResultCode.UNAUTHORIZED -> context.getString(R.string.server_reported_authorization_error)
                RemoteOperationResult.ResultCode.FILE_NOT_FOUND -> context.getString(R.string.server_reported_file_not_found)
                else -> result.exception?.message ?: result.logMessage
            }
        } else if (status == NextCloud.STATUS_AUTH_ERROR)
            context.getString(R.string.server_reported_authorization_error)
        else
            context.getString(R.string.unkown_error)

        return "${context.getString(R.string.failed_to_connect_to_nextcloud)}: $error"
    }
}

@Singleton
class NextCloud @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ioScope: CoroutineScope,
    override val logger: Logger,
) : SharedPreferences.OnSharedPreferenceChangeListener, LogInterface {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val gson = GsonBuilder()
        .registerTypeAdapter(Instant::class.java, InstantAdapter())
        .create()
    private val tempDirUp = File(context.cacheDir, "up").also { it.mkdir() }
    private val tempDirDown = File(context.cacheDir, "down").also { it.mkdir() }
    private val listenerHandler = Handler(Looper.getMainLooper())

    private var isTestScheduled = false
    private var status = STATUS_READY

    internal var uri: Uri = Uri.EMPTY
        set(value) {
            if (field != value) {
                field = value
                updateClient(uri = value)
            }
        }

    internal var username = ""
        set(value) {
            if (field != value) {
                field = value
                updateClient(username = value)
            }
        }

    internal var password = ""
        set(value) {
            if (field != value) {
                field = value
                updateClient(password = value)
            }
        }

    private val client = OwnCloudClientFactory.createOwnCloudClient(uri, context, true).apply {
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


    fun testClient(uri: Uri, username: String, password: String, callback: ((NextCloudTestResult) -> Unit)? = null) {
        this.uri = uri
        this.username = username
        this.password = password
        testClient(callback)
    }

    private fun testClient(callback: ((NextCloudTestResult) -> Unit)? = null) {
        if (status == STATUS_TESTING) {
            ioScope.launch {
                while (status == STATUS_TESTING) delay(100)
                testClient(callback)
            }
        } else if (status < STATUS_AUTH_ERROR) {
            // On auth error, don't even try anything until URL/username/PW has changed.
            status = STATUS_TESTING
            CreateAppDirsTask(triggerStatus = STATUS_TESTING).run { task ->
                @Suppress("LiftReturnOrAssignment")
                if (!task.success) {
                    if (
                        task.result?.code == RemoteOperationResult.ResultCode.UNAUTHORIZED ||
                        task.result?.code == RemoteOperationResult.ResultCode.FORBIDDEN
                    ) status = STATUS_AUTH_ERROR
                    else status = STATUS_ERROR
                } else status = STATUS_OK
                callback?.invoke(NextCloudTestResult(success = task.success, result = task.result, status = status))
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
        } else callback?.invoke(NextCloudTestResult(success = status == STATUS_OK, result = null, status = status))
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
        log("onSharedPreferenceChanged: key=$key, value=${preferences.getString(key, "")}")

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


    abstract inner class Task<T : Task<T>>(private val triggerStatus: Int = STATUS_OK) : LogInterface {
        override val logger: Logger = this@NextCloud.logger
        private var onReadyCallback: ((T) -> Unit)? = null
        protected var _success: Boolean? = null
        private var _error: LogMessage? = null
        private var _hasNotified = false
        val success: Boolean
            get() = _success ?: false
        val error: LogMessage?
            get() = _error

        abstract fun start()

        open fun isReady(): Boolean = true

        open fun onReady() {}

        open fun run(onReadyCallback: ((T) -> Unit)? = null) {
            this.onReadyCallback = onReadyCallback
            awaitStatus(triggerStatus) {
                log("Running ${javaClass.simpleName}")
                start()
            }
        }

        fun notifyIfReady() {
            if (isReady() && !_hasNotified) {
                log("Ready!", level = Log.DEBUG)
                if (_success == null) _success = true
                _hasNotified = true
                onReady()
                @Suppress("UNCHECKED_CAST")
                onReadyCallback?.invoke(this as T)
            }
        }

        fun readyWithError(logMessage: LogMessage?, showInSnackbar: Boolean = false) {
            _success = false
            if (logMessage != null) {
                _error = logMessage
                log(logMessage, showInSnackbar = showInSnackbar)
            }
            notifyIfReady()
        }

        fun readyWithError(message: String?, showInSnackbar: Boolean = false) {
            readyWithError(
                logMessage = if (message != null) createLogMessage(message) else null,
                showInSnackbar = showInSnackbar
            )
        }

        override fun createLogMessage(message: String, level: Int): LogMessage {
            return super.createLogMessage("$message (uri=$uri, username=$username, password=$password)", level)
        }
    }


    abstract inner class OperationTask<T : OperationTask<T>>(triggerStatus: Int = STATUS_OK) :
        Task<T>(triggerStatus = triggerStatus) {
        abstract val operation: RemoteOperation<*>
        var result: RemoteOperationResult<*>? = null
        open val successMessage: String? = null

        open fun handleSuccessfulResult(result: RemoteOperationResult<*>) {
            successMessage?.let { log(it) }
            notifyIfReady()
        }

        open fun handleUnsuccessfulResult(result: RemoteOperationResult<*>) {
            readyWithError(result.logMessage ?: result.message)
        }

        open fun isResultSuccessful(result: RemoteOperationResult<*>): Boolean = result.isSuccess

        override fun start() {
            operation.execute(
                client,
                { _, result ->
                    this.result = result
                    if (isResultSuccessful(result)) handleSuccessfulResult(result)
                    else handleUnsuccessfulResult(result)
                },
                listenerHandler
            )
        }
    }


    /** Create: 1 arbitrary directory */
    inner class CreateDirTask(remoteDir: String, triggerStatus: Int = STATUS_OK) :
        OperationTask<CreateDirTask>(triggerStatus) {
        override val operation = CreateFolderRemoteOperation(remoteDir, true)

        override fun isResultSuccessful(result: RemoteOperationResult<*>): Boolean {
            /** A little more lax than parent implementation. */
            return result.isSuccess || result.code == RemoteOperationResult.ResultCode.FOLDER_ALREADY_EXISTS
        }
    }

    abstract inner class ListTask<T : ListTask<T, CT, LT>, CT : Task<CT>, LT : Any>(
        triggerStatus: Int = STATUS_OK,
        private val objects: List<LT>,
    ) : Task<T>(triggerStatus) {
        private var successfulTasks = 0
        private var unsuccessfulTasks = 0
        private val processedTasks
            get() = successfulTasks + unsuccessfulTasks

        abstract fun getChildTask(obj: LT): CT

        open fun onChildTaskReady(task: CT) {}

        override fun isReady() = processedTasks == objects.size

        override fun start() {
            objects.forEach { obj -> runChildTask(obj) }
        }

        private fun runChildTask(obj: LT) {
            getChildTask(obj).run { task ->
                if (!task.success) {
                    unsuccessfulTasks++
                    readyWithError(task.error)
                } else {
                    successfulTasks++
                    notifyIfReady()
                }
                onChildTaskReady(task)
            }
        }
    }

    inner class CreateAppDirsTask(private val triggerStatus: Int = STATUS_OK) :
        ListTask<CreateAppDirsTask, CreateDirTask, String>(
            triggerStatus,
            listOf(NEXTCLOUD_IMAGE_DIR, NEXTCLOUD_JSON_DIR)
        ) {
        var result: RemoteOperationResult<*>? = null

        override fun getChildTask(obj: String) = CreateDirTask(obj, triggerStatus)

        override fun onChildTaskReady(task: CreateDirTask) {
            result = task.result
        }
    }


    inner class CreateAppDirsTaskOld(private val triggerStatus: Int = STATUS_OK) :
        Task<CreateAppDirsTask>(triggerStatus) {
        private var processedDirs = 0
        private val remoteDirs = listOf(NEXTCLOUD_IMAGE_DIR, NEXTCLOUD_JSON_DIR)
        var result: RemoteOperationResult<*>? = null

        override fun isReady() = processedDirs == remoteDirs.size

        override fun start() {
            remoteDirs.forEach { remoteDir ->
                CreateDirTask(remoteDir, triggerStatus).run { task ->
                    result = task.result
                    processedDirs++
                    log("CreateAppDirsTask: remoteDir=$remoteDir, success=${task.success}, error=${task.error}")
                    if (!task.success) readyWithError(task.error)
                    notifyIfReady()
                }
            }
        }
    }


    /** Up: 1 arbitrary file */
    open inner class UploadFileTask<T : UploadFileTask<T>>(
        remotePath: String,
        private val localFile: File,
        mimeType: String?,
    ) : OperationTask<T>() {
        override val successMessage = "Successfully saved $localFile to $remotePath on Nextcloud"

        override val operation = UploadFileRemoteOperation(
            localFile.absolutePath,
            remotePath,
            mimeType,
            (System.currentTimeMillis() / 1000).toString()
        )

        override fun start() {
            if (!localFile.isFile) readyWithError("$localFile is not a file")
            else super.start()
        }
    }


    /** Up: 1 image file */
    inner class UploadImageTask(image: Image) : UploadFileTask<UploadImageTask>(
        remotePath = "$NEXTCLOUD_IMAGE_DIR/${image.filename}",
        localFile = File(File(context.filesDir, IMAGE_SUBDIR), image.filename),
        mimeType = image.mimeType
    )


    /** Up: 0..n image files */
    inner class UploadMissingImagesTask(private val images: Collection<Image>) : Task<UploadMissingImagesTask>() {
        private var successCount = 0
        private var failCount = 0
        private var processedFiles = 0
        private var missingImages: List<Image>? = null

        override fun isReady() = missingImages?.let { it.size == processedFiles } ?: false

        override fun start() {
            ListFilesTask(
                remoteDir = NEXTCLOUD_IMAGE_DIR,
                filter = { remoteFile -> remoteFile.mimeType != "DIR" }
            ).run { task ->
                val missingImages = if (task.success) {
                    // First list current images and their sizes:
                    val remoteImageLengths = task.remoteFiles?.associate {
                        Pair(it.remotePath.split("/").last(), it.length.toInt())
                    }
                    // Then filter DB images for those where the corresponding
                    // remote images either don't exist or have different size:
                    images.filter { image ->
                        remoteImageLengths?.get(image.filename)?.let { image.size != it } ?: true
                    }
                } else images.toList()

                this.missingImages = missingImages

                missingImages.forEach { image ->
                    UploadImageTask(image).run { task ->
                        if (!task.success) {
                            _success = false
                            failCount++
                        } else successCount++
                        processedFiles++
                        notifyIfReady()
                    }
                }
                notifyIfReady()
            }
        }
    }


    /** Up: 1 note JSON file */
    inner class UploadNoteTask(private val noteCombined: NoteCombined) : Task<UploadNoteTask>() {
        private val filename = "note-${noteCombined.id}.json"
        private val remotePath = "$NEXTCLOUD_JSON_DIR/$filename"
        private val localFile = File(tempDirUp, filename).apply { deleteOnExit() }

        override fun start() = uploadFile()

        private fun uploadFile() {
            ioScope.launch {
                withContext(Dispatchers.IO) {
                    log("Uploading file $remotePath from $localFile")

                    try {
                        FileWriter(localFile).use { it.write(gson.toJson(noteCombined)) }
                    } catch (e: Exception) {
                        readyWithError(e.toString())
                        return@withContext
                    }
                    UploadFileRemoteOperation(
                        localFile.absolutePath,
                        remotePath,
                        "application/json",
                        (System.currentTimeMillis() / 1000).toString()
                    ).execute(client, uploadFileResult, listenerHandler)
                }
            }
        }

        private val uploadFileResult = OnRemoteOperationListener { _, result ->
            if (!localFile.isFile) readyWithError("$noteCombined: $localFile is not a file")
            else if (result == null) readyWithError("result for $noteCombined is null")
            else if (!result.isSuccess) readyWithError("$noteCombined: ${result.logMessage ?: result.message}")
            else log("Successfully saved $noteCombined to $remotePath on Nextcloud")
            notifyIfReady()
        }
    }


    /** Up: 0..n note JSON files */
    inner class UpstreamSyncTask(private val notes: Collection<NoteCombined>) : Task<UpstreamSyncTask>() {
        private var successCount = 0
        private var failCount = 0
        private var processedFiles = 0

        override fun isReady() = processedFiles == notes.size

        override fun onReady() {
            if (failCount > 0) {
                log(
                    message = "Failed to sync $failCount notes to Nextcloud.",
                    level = Log.ERROR,
                    showInSnackbar = true,
                )
            }
        }

        override fun start() {
            log("Starting upstream sync of $notes")

            notes.forEach { note ->
                UploadNoteTask(note).run { task ->
                    if (task.success) successCount++
                    else {
                        failCount++
                        _success = false
                    }
                    processedFiles++
                    notifyIfReady()
                }
            }
        }
    }


    /** Remove: 1 arbitrary file */
    open inner class RemoveFileTask<T : RemoveFileTask<T>>(remotePath: String) : OperationTask<T>() {
        override val operation = RemoveFileRemoteOperation(remotePath)
        override val successMessage = "Successfully removed $remotePath from Nextcloud"
    }


    /** Remove: 1 image file */
    inner class RemoveImageTask(image: Image) :
        RemoveFileTask<RemoveImageTask>("$NEXTCLOUD_IMAGE_DIR/${image.filename}")


    /** List: arbitrary files */
    open inner class ListFilesTask<T : ListFilesTask<T>>(
        remoteDir: String,
        private val filter: (RemoteFile) -> Boolean,
    ) : OperationTask<T>() {
        var remoteFiles: List<RemoteFile>? = null
        var remotePaths: List<String>? = null
        override val operation = ReadFolderRemoteOperation(remoteDir)

        override fun handleSuccessfulResult(result: RemoteOperationResult<*>) {
            @Suppress("DEPRECATION")
            remoteFiles = result.data.filterIsInstance<RemoteFile>().filter(filter)
            remotePaths = remoteFiles?.map { it.remotePath }
            remotePaths?.forEach { handleRemotePath(it) }
            super.handleSuccessfulResult(result)
        }

        open fun handleRemotePath(path: String) {}
    }


    /** Remove: 0..n image files */
    inner class RemoveOrphanImagesTask(private val keep: List<String>) : Task<RemoveOrphanImagesTask>() {
        private var processedFiles = 0
        private var remotePaths: List<String>? = null

        override fun isReady() = remotePaths?.let { it.size == processedFiles } ?: false

        override fun start() {
            log("Starting RemoveOrphanImagesTask")
            ListFilesTask(NEXTCLOUD_IMAGE_DIR) { remoteFile ->
                remoteFile.mimeType != "DIR" &&
                !keep.contains(remoteFile.remotePath.split("/").last())
            }.run { task ->
                if (task.success) {
                    remotePaths = task.remotePaths?.toList()
                    removeFiles()
                } else readyWithError("Error on reading $NEXTCLOUD_IMAGE_DIR")
            }
        }

        private fun removeFiles() {
            remotePaths?.forEach { path ->
                RemoveFileTask(path).run { task ->
                    processedFiles++
                    if (!task.success) readyWithError("Error on deleting $path")
                    else notifyIfReady()
                }
            }
            notifyIfReady()
        }
    }


    /** Remove: 0..n note JSON file, 0..n image files */
    inner class RemoveNotesAndImagesTask(
        private val noteIds: Collection<UUID>,
        private val images: Collection<Image>
    ) : Task<RemoveNotesAndImagesTask>() {
        private var processedFiles = 0

        override fun isReady() = processedFiles == images.size + noteIds.size

        override fun start() {
            log("Starting remove note task for notes=$noteIds, images=$images")

            noteIds.forEach { noteId ->
                RemoveFileTask("$NEXTCLOUD_JSON_DIR/note-$noteId.json").run { processedFiles++ }
            }
            images.forEach { image -> RemoveImageTask(image).run { processedFiles++ } }
        }
    }


    /** Down: 1 arbitrary file */
    open inner class DownloadFileTask<T : DownloadFileTask<T>>(
        val remotePath: String,
        localDir: File
    ) : OperationTask<T>() {
        internal val localFile = File(localDir, remotePath)
        override val operation = DownloadFileRemoteOperation(remotePath, localDir.absolutePath)

        override fun handleSuccessfulResult(result: RemoteOperationResult<*>) {
            if (!localFile.isFile) readyWithError("$remotePath: $localFile is not a file")
            else {
                log("Successfully downloaded $remotePath from Nextcloud to $localFile")
                handleDownloadedFile()
            }
        }

        open fun handleDownloadedFile() = notifyIfReady()
    }


    /** Down: 0..n image files */
    inner class DownloadNoteImagesTask(private val noteCombined: NoteCombined) : Task<DownloadNoteImagesTask>() {
        private var processedFiles = 0

        override fun isReady() = processedFiles == noteCombined.images.size

        override fun start() {
            @Suppress("Destructure")
            noteCombined.images.forEach { image ->
                DownloadFileTask(
                    remotePath = "$NEXTCLOUD_IMAGE_DIR/${image.filename}",
                    localDir = tempDirDown,
                ).run { task ->
                    if (!task.success) _success = false
                    else {
                        val finalLocalPath = File(File(context.filesDir, "images"), image.filename)

                        if (!task.localFile.renameTo(finalLocalPath))
                            readyWithError("Could not move ${task.localFile} to $finalLocalPath")
                        else log("Moved ${task.localFile} to $finalLocalPath")
                    }
                    processedFiles++
                    notifyIfReady()
                }
            }
        }
    }


    /**
     * Down: 0..n images
     *
     * We're more lax with the success status here, because the whole operation shouldn't be considered a failure if
     * one of 10 locally missing images was also missing on remote.
     */
    inner class DownloadMissingImagesTask(
        private val missingImages: Collection<Image>
    ) : Task<DownloadMissingImagesTask>() {
        private var processedFiles = 0

        override fun isReady() = processedFiles == missingImages.size

        override fun start() {
            @Suppress("Destructure")
            missingImages.forEach { image ->
                DownloadFileTask(
                    remotePath = "$NEXTCLOUD_IMAGE_DIR/${image.filename}",
                    localDir = tempDirDown
                ).run { task ->
                    if (task.success) {
                        val finalLocalPath = File(File(context.filesDir, "images"), image.filename)

                        if (!task.localFile.renameTo(finalLocalPath))
                            log("Could not move ${task.localFile} to $finalLocalPath", level = Log.ERROR)
                        else log("Moved ${task.localFile} to $finalLocalPath")
                    }
                    processedFiles++
                    notifyIfReady()
                }
            }
        }
    }


    /** Down: 1 note JSON file */
    inner class DownloadNoteTask(remotePath: String) : DownloadFileTask<DownloadNoteTask>(
        remotePath = remotePath,
        localDir = tempDirDown,
    ) {
        var remoteNoteCombined: NoteCombined? = null

        override fun handleDownloadedFile() {
            ioScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val json = FileReader(localFile).use { it.readText() }

                        remoteNoteCombined = gson.fromJson(json, NoteCombined::class.java)
                        if (remoteNoteCombined != null) {
                            log("Successfully parsed $remoteNoteCombined from Nextcloud")
                            notifyIfReady()
                        } else readyWithError("$remotePath: noteCombined is null")
                    } catch (e: Exception) {
                        readyWithError("$remotePath: $e")
                    } finally {
                        localFile.delete()
                    }
                }
            }
        }
    }


    /** Down: 0..n note JSON files */
    inner class DownstreamSyncTask : ListFilesTask<DownstreamSyncTask>(
        remoteDir = NEXTCLOUD_JSON_DIR,
        filter = { remoteFile ->
            remoteFile.mimeType == "application/json" &&
            remoteFile.remotePath.split("/").last().startsWith("note-")
        }
    ) {
        val remoteNotesCombined = mutableListOf<NoteCombined>()
        private var processedFiles = 0

        override fun isReady() = remotePaths?.let { it.size == processedFiles } ?: false

        override fun handleRemotePath(path: String) {
            DownloadNoteTask(path).run { task ->
                if (task.success) task.remoteNoteCombined?.let { remoteNotesCombined.add(it) }
                processedFiles++
                notifyIfReady()
            }
        }
    }
}
