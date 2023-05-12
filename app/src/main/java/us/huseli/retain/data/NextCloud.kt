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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.huseli.retain.Constants.IMAGE_SUBDIR
import us.huseli.retain.Constants.NEXTCLOUD_IMAGE_DIR
import us.huseli.retain.Constants.NEXTCLOUD_JSON_DIR
import us.huseli.retain.Constants.PREF_NEXTCLOUD_PASSWORD
import us.huseli.retain.Constants.PREF_NEXTCLOUD_URI
import us.huseli.retain.Constants.PREF_NEXTCLOUD_USERNAME
import us.huseli.retain.Logger
import us.huseli.retain.LoggingObject
import us.huseli.retain.data.entities.Image
import us.huseli.retain.data.entities.NoteCombined
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.lang.reflect.Type
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

@Singleton
class NextCloud @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ioScope: CoroutineScope,
    override var logger: Logger?,
) : SharedPreferences.OnSharedPreferenceChangeListener, LoggingObject {
    data class Credentials(val uri: Uri, val username: String, val password: String)

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val gson = GsonBuilder()
        .registerTypeAdapter(Instant::class.java, InstantAdapter())
        .create()
    private val tempDirUp = File(context.cacheDir, "up").also { it.mkdir() }
    private val tempDirDown = File(context.cacheDir, "down").also { it.mkdir() }
    private val listenerHandler = Handler(Looper.getMainLooper())
    private var client: OwnCloudClient? = null

    private val isReady = MutableStateFlow(false)
    private val uri = MutableStateFlow(preferences.getString(PREF_NEXTCLOUD_URI, null))
    private val username = MutableStateFlow(preferences.getString(PREF_NEXTCLOUD_USERNAME, null))
    private val password = MutableStateFlow(preferences.getString(PREF_NEXTCLOUD_PASSWORD, null))
    private val credentials = combine(uri, username, password) { _uri, _username, _password ->
        if (_uri != null && _username != null && _password != null) {
            Credentials(Uri.parse(_uri), _username, _password)
        } else null
    }.filterNotNull()

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
        ioScope.launch { credentials.collect { initClient(it) } }
    }

    internal fun onReady(callback: () -> Unit) {
        if (isReady.value) callback()
        else ioScope.launch {
            isReady.transformWhile {
                if (it) emit(true)
                !it
            }.collect { callback() }
        }
    }

    private fun createRemoteDir(remoteDir: String) = CreateDirTask(remoteDir).run { isReady.value = it.success }

    private fun initClient(credentials: Credentials) {
        client = OwnCloudClientFactory.createOwnCloudClient(credentials.uri, context, true).also {
            it.credentials = OwnCloudCredentialsFactory.newBasicCredentials(credentials.username, credentials.password)
            it.userId = credentials.username
            it.setDefaultTimeouts(120000, 120000)
        }
        createRemoteDir(NEXTCLOUD_JSON_DIR)
        createRemoteDir(NEXTCLOUD_IMAGE_DIR)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        log("onSharedPreferenceChanged: key=$key, value=${preferences.getString(key, "")}")
        when (key) {
            PREF_NEXTCLOUD_URI -> uri.value = preferences.getString(key, "")
            PREF_NEXTCLOUD_USERNAME -> username.value = preferences.getString(key, "")
            PREF_NEXTCLOUD_PASSWORD -> password.value = preferences.getString(key, "")
        }
    }


    abstract inner class Task<T : Task<T>>(
        override var logger: Logger? = this@NextCloud.logger,
    ) : LoggingObject {
        protected var onReadyCallback: ((T) -> Unit)? = null
        protected var _success: Boolean? = null
        private var _hasNotified = false
        val success: Boolean
            get() = _success ?: false

        abstract fun start()

        open fun isReady(): Boolean = true

        open fun onReady() {}

        open fun run(onReadyCallback: ((T) -> Unit)? = null) {
            this.onReadyCallback = onReadyCallback
            this@NextCloud.onReady {
                log("Ready to run!", level = Log.DEBUG)
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

        fun readyWithError(message: String) {
            _success = false
            log(message, Log.ERROR, addToFlow = true)
            notifyIfReady()
        }
    }


    /** force = don't wait for this@NextCloud.onReady, just go. */
    abstract inner class OperationTask<T : OperationTask<T>>(
        private val force: Boolean = false
    ) : Task<T>() {
        abstract val operation: RemoteOperation<*>

        abstract fun handleResult(result: RemoteOperationResult<*>)

        override fun run(onReadyCallback: ((T) -> Unit)?) {
            this.onReadyCallback = onReadyCallback
            if (force) {
                log("Ready to run!", level = Log.DEBUG)
                start()
            } else {
                this@NextCloud.onReady {
                    log("Ready to run!", level = Log.DEBUG)
                    start()
                }
            }
        }

        open fun resultIsOK(result: RemoteOperationResult<*>?): Boolean {
            if (result == null) {
                readyWithError("Result is null")
                return false
            } else if (!result.isSuccess) {
                readyWithError(result.exception?.toString() ?: result.logMessage)
                return false
            }
            return true
        }

        override fun start() {
            operation.execute(
                client,
                { _, result -> if (resultIsOK(result)) handleResult(result) },
                listenerHandler
            )
        }
    }


    /** Create: 1 arbitrary directory */
    inner class CreateDirTask(
        private val remoteDir: String,
        force: Boolean = true
    ) : OperationTask<CreateDirTask>(force = force) {
        override val operation = CreateFolderRemoteOperation(remoteDir, true)

        override fun handleResult(result: RemoteOperationResult<*>) {
            log("Success for $remoteDir!")
            notifyIfReady()
        }

        override fun resultIsOK(result: RemoteOperationResult<*>?): Boolean {
            /** A little more lax than parent implementation. */
            if (result == null) {
                readyWithError("Result for $remoteDir is null")
                return false
            } else if (!result.isSuccess && result.code != RemoteOperationResult.ResultCode.FOLDER_ALREADY_EXISTS) {
                val error = result.exception?.toString() ?: result.logMessage
                readyWithError("Couldn't create remote directory $remoteDir on Nextcloud: $error")
                return false
            }
            return true
        }
    }


    /** Up: 1 arbitrary file */
    open inner class UploadFileTask<T : UploadFileTask<T>>(
        private val remotePath: String,
        private val localFile: File,
        mimeType: String?,
    ) : OperationTask<T>() {
        override val operation = UploadFileRemoteOperation(
            localFile.absolutePath,
            remotePath,
            mimeType,
            (System.currentTimeMillis() / 1000).toString()
        )

        override fun handleResult(result: RemoteOperationResult<*>) {
            if (!localFile.isFile) readyWithError("$localFile is not a file")
            else log("Successfully saved $localFile to $remotePath on Nextcloud")
            notifyIfReady()
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
        private lateinit var missingImages: List<Image>

        override fun isReady() = processedFiles == missingImages.size

        override fun start() {
            ListFilesTask(NEXTCLOUD_IMAGE_DIR) { remoteFile -> remoteFile.mimeType != "DIR" }.run { task ->
                missingImages = if (task.success) {
                    // First list current images and their sizes:
                    val remoteImageLengths = task.remoteFiles.associate {
                        Pair(it.remotePath.split("/").last(), it.length.toInt())
                    }
                    // Then filter DB images for those where the corresponding
                    // remote images either don't exist or have different size:
                    images.filter { image ->
                        remoteImageLengths[image.filename]?.let { image.size != it } ?: true
                    }
                } else images.toList()

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
            else if (!result.isSuccess) readyWithError("$noteCombined: ${result.exception?.toString() ?: result.logMessage}")
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
                    addToFlow = true,
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
    open inner class RemoveFileTask<T : RemoveFileTask<T>>(private val remotePath: String) : OperationTask<T>() {
        override val operation = RemoveFileRemoteOperation(remotePath)

        override fun handleResult(result: RemoteOperationResult<*>) {
            log("Successfully removed $remotePath from Nextcloud")
            notifyIfReady()
        }
    }


    /** Remove: 1 image file */
    inner class RemoveImageTask(image: Image) :
        RemoveFileTask<RemoveImageTask>("$NEXTCLOUD_IMAGE_DIR/${image.filename}")


    /** List: arbitrary files */
    open inner class ListFilesTask<T : ListFilesTask<T>>(
        remoteDir: String,
        private val filter: (RemoteFile) -> Boolean,
    ) : OperationTask<T>() {
        lateinit var remoteFiles: List<RemoteFile>
        lateinit var remotePaths: List<String>
        override val operation = ReadFolderRemoteOperation(remoteDir)

        override fun handleResult(result: RemoteOperationResult<*>) {
            @Suppress("DEPRECATION")
            remoteFiles = result.data.filterIsInstance<RemoteFile>().filter(filter)
            remotePaths = remoteFiles.map { it.remotePath }
            remotePaths.forEach { handleRemotePath(it) }
            notifyIfReady()
        }

        open fun handleRemotePath(path: String) {}
    }


    /** Remove: 0..n image files */
    inner class RemoveOrphanImagesTask(private val keep: List<String>) : Task<RemoveOrphanImagesTask>() {
        private var processedFiles = 0
        private lateinit var remotePaths: List<String>

        override fun isReady() = processedFiles == remotePaths.size

        override fun start() {
            log("Starting RemoveOrphanImagesTask")
            ListFilesTask(NEXTCLOUD_IMAGE_DIR) { remoteFile ->
                remoteFile.mimeType != "DIR" &&
                !keep.contains(remoteFile.remotePath.split("/").last())
            }.run { task ->
                if (task.success) {
                    remotePaths = task.remotePaths.toList()
                    removeFiles()
                } else readyWithError("Error on reading $NEXTCLOUD_IMAGE_DIR")
            }
        }

        private fun removeFiles() {
            remotePaths.forEach { path ->
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

        override fun handleResult(result: RemoteOperationResult<*>) {
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

        override fun isReady() = processedFiles == remotePaths.size

        override fun handleRemotePath(path: String) {
            DownloadNoteTask(path).run { task ->
                if (task.success) task.remoteNoteCombined?.let { remoteNotesCombined.add(it) }
                processedFiles++
                notifyIfReady()
            }
        }
    }
}
