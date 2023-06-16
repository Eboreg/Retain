package us.huseli.retain.data

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import us.huseli.retain.Constants
import us.huseli.retain.Constants.PREF_SYNC_BACKEND
import us.huseli.retain.Enums.SyncBackend
import us.huseli.retain.LogInterface
import us.huseli.retain.Logger
import us.huseli.retain.data.entities.Image
import us.huseli.retain.syncbackend.Engine
import us.huseli.retain.syncbackend.NextCloudEngine
import us.huseli.retain.syncbackend.SFTPEngine
import us.huseli.retain.syncbackend.tasks.RemoveImagesTask
import us.huseli.retain.syncbackend.tasks.SyncTask
import us.huseli.retain.syncbackend.tasks.UploadNoteCombosTask
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncBackendRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val nextCloudEngine: NextCloudEngine,
    private val sftpEngine: SFTPEngine,
    override val logger: Logger,
    private val noteDao: NoteDao,
    private val database: Database,
    private val ioScope: CoroutineScope,
    private val checklistItemDao: ChecklistItemDao,
    private val imageDao: ImageDao,
) : SharedPreferences.OnSharedPreferenceChangeListener, LogInterface {
    private val imageDir = File(context.filesDir, Constants.IMAGE_SUBDIR).apply { mkdirs() }
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val _syncBackend: MutableStateFlow<SyncBackend?> = MutableStateFlow(
        preferences.getString(PREF_SYNC_BACKEND, null)?.let { SyncBackend.valueOf(it) }
    )
    private val engine: Engine?
        get() = when (_syncBackend.value) {
            SyncBackend.NEXTCLOUD -> nextCloudEngine
            SyncBackend.SFTP -> sftpEngine
            else -> null
        }

    val hasActiveTasks: Flow<Boolean> = engine?.hasActiveTasks ?: flowOf(false)
    val needsTesting = MutableStateFlow(true)
    val syncBackend = _syncBackend.asStateFlow()

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
        ioScope.launch { sync() }
    }

    fun removeImages(images: Collection<Image>) = engine?.let { RemoveImagesTask(it, images).run() }

    suspend fun sync() {
        @Suppress("Destructure")
        engine?.let {
            SyncTask(
                engine = it,
                localCombos = noteDao.listAllCombos().map { combo ->
                    combo.copy(databaseVersion = database.openHelper.readableDatabase.version)
                },
                onRemoteComboUpdated = { combo ->
                    ioScope.launch {
                        noteDao.upsert(combo.note)
                        checklistItemDao.replace(combo.note.id, combo.checklistItems)
                        imageDao.replace(combo.note.id, combo.images)
                    }
                },
                localImageDir = imageDir,
                deletedNoteIds = noteDao.listDeletedIds(),
            ).run()
        }
    }

    suspend fun uploadNotes() {
        engine?.let {
            val combos = noteDao.listAllCombos()
            UploadNoteCombosTask(
                engine = it,
                combos = combos.map { combo ->
                    combo.copy(databaseVersion = database.openHelper.readableDatabase.version)
                },
            ).run { result ->
                if (!result.success) logError("Failed to upload note(s) to Nextcloud: ${result.message}")
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == PREF_SYNC_BACKEND)
            _syncBackend.value = preferences.getString(key, null)?.let { SyncBackend.valueOf(it) }
    }
}
