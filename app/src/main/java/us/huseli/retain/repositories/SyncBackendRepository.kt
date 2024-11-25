package us.huseli.retain.repositories

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import us.huseli.retain.Constants
import us.huseli.retain.Constants.PREF_SYNC_BACKEND
import us.huseli.retain.Database
import us.huseli.retain.Enums.SyncBackend
import us.huseli.retain.dao.ChecklistItemDao
import us.huseli.retain.dao.ImageDao
import us.huseli.retain.dao.NoteDao
import us.huseli.retain.dataclasses.entities.Image
import us.huseli.retain.interfaces.ILogger
import us.huseli.retain.syncbackend.DropboxEngine
import us.huseli.retain.syncbackend.Engine
import us.huseli.retain.syncbackend.NextCloudEngine
import us.huseli.retain.syncbackend.SFTPEngine
import us.huseli.retain.syncbackend.tasks.RemoveImagesTask
import us.huseli.retain.syncbackend.tasks.SyncTask
import us.huseli.retain.syncbackend.tasks.UploadNoteCombosTask
import us.huseli.retain.syncbackend.tasks.result.OperationTaskResult
import us.huseli.retaintheme.utils.AbstractScopeHolder
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class SyncBackendRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val nextCloudEngine: NextCloudEngine,
    private val sftpEngine: SFTPEngine,
    private val dropboxEngine: DropboxEngine,
    private val noteDao: NoteDao,
    private val database: Database,
    private val checklistItemDao: ChecklistItemDao,
    private val imageDao: ImageDao,
) : SharedPreferences.OnSharedPreferenceChangeListener, ILogger, AbstractScopeHolder() {
    private val imageDir = File(context.filesDir, Constants.IMAGE_SUBDIR).apply { mkdirs() }
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val _syncBackend: MutableStateFlow<SyncBackend> = MutableStateFlow(
        preferences.getString(PREF_SYNC_BACKEND, null)?.let { SyncBackend.valueOf(it) } ?: SyncBackend.NONE
    )
    private val engine = MutableStateFlow<Engine?>(null).apply {
        launchOnIOThread {
            _syncBackend.collect {
                value = syncBackendToEngine(it)
            }
        }
    }
    private val onSaveListeners = mutableListOf<() -> Unit>()

    val isSyncing: Flow<Boolean> = engine.flatMapLatest { it?.isSyncing ?: flowOf(false) }
    val needsTesting = MutableStateFlow(true)
    val syncBackend = _syncBackend.asStateFlow()

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
        engine.value = syncBackendToEngine(_syncBackend.value)
        launchOnIOThread { sync() }
    }

    private fun syncBackendToEngine(syncBackend: SyncBackend): Engine? = when (syncBackend) {
        SyncBackend.NEXTCLOUD -> nextCloudEngine
        SyncBackend.SFTP -> sftpEngine
        SyncBackend.DROPBOX -> dropboxEngine
        else -> null
    }

    fun addOnSaveListener(listener: () -> Unit) = onSaveListeners.add(listener)

    fun removeImages(images: Collection<Image>) = engine.value?.let { RemoveImagesTask(it, images).run() }

    fun save() = onSaveListeners.forEach { it.invoke() }

    private suspend fun _sync() {
        @Suppress("Destructure")
        engine.value?.let {
            SyncTask(
                engine = it,
                localPojos = noteDao.listNotePojos().map { combo ->
                    combo.copy(databaseVersion = database.openHelper.readableDatabase.version)
                },
                onRemotePojoUpdated = { combo ->
                    launchOnIOThread {
                        noteDao.upsert(combo.note)
                        checklistItemDao.replace(combo.note.id, combo.checklistItems)
                        imageDao.replace(combo.note.id, combo.images)
                    }
                },
                localImageDir = imageDir,
                deletedNoteIds = noteDao.listDeletedIds(),
            ).run { result ->
                if (!result.success) logError(
                    message = "Sync with ${it.backend.displayName} failed",
                    exception = result.exception,
                )
            }
        }
    }

    suspend fun sync() {
        if (needsTesting.value) {
            needsTesting.value = false
            engine.value?.test { result ->
                if (result.success) launchOnIOThread { _sync() }
            }
        } else _sync()
    }

    suspend fun uploadNotes(onResult: ((OperationTaskResult) -> Unit)? = null) {
        engine.value?.let {
            val pojos = noteDao.listNotePojos()
            UploadNoteCombosTask(
                engine = it,
                combos = pojos.map { combo ->
                    combo.copy(databaseVersion = database.openHelper.readableDatabase.version)
                },
            ).run { result -> onResult?.invoke(result) }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_SYNC_BACKEND -> {
                val value = preferences.getString(key, null)?.let { SyncBackend.valueOf(it) } ?: SyncBackend.NONE
                if (value != _syncBackend.value) {
                    engine.value?.cancelTasks()
                    _syncBackend.value = value
                    needsTesting.value = true
                }
            }
        }
    }
}
