package us.huseli.retain.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import us.huseli.retain.Constants
import us.huseli.retain.LogInterface
import us.huseli.retain.Logger
import us.huseli.retain.data.entities.Image
import us.huseli.retain.nextcloud.NextCloudEngine
import us.huseli.retain.nextcloud.tasks.RemoveImagesTask
import us.huseli.retain.nextcloud.tasks.SyncTask
import us.huseli.retain.nextcloud.tasks.TestNextCloudTaskResult
import us.huseli.retain.nextcloud.tasks.UploadNoteCombosTask
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NextCloudRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageDao: ImageDao,
    private val nextCloudEngine: NextCloudEngine,
    private val noteDao: NoteDao,
    private val checklistItemDao: ChecklistItemDao,
    private val ioScope: CoroutineScope,
    private val database: Database,
    override val logger: Logger,
) : SharedPreferences.OnSharedPreferenceChangeListener, LogInterface {
    private val imageDir = File(context.filesDir, Constants.IMAGE_SUBDIR).apply { mkdirs() }
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    val hasActiveTasks = nextCloudEngine.hasActiveTasks

    init {
        sync()
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == Constants.PREF_NEXTCLOUD_BASE_DIR) sync()
    }

    fun sync() {
        ioScope.launch {
            @Suppress("Destructure")
            SyncTask(
                engine = nextCloudEngine,
                localCombos = noteDao.listAllCombos().map {
                    it.copy(databaseVersion = database.openHelper.readableDatabase.version)
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

    fun removeImages(images: Collection<Image>) = RemoveImagesTask(nextCloudEngine, images).run()

    fun test(
        uri: Uri,
        username: String,
        password: String,
        baseDir: String,
        onResult: (TestNextCloudTaskResult) -> Unit
    ) = nextCloudEngine.testClient(uri, username, password, baseDir) { result -> onResult(result) }

    suspend fun uploadNotes() {
        val combos = noteDao.listAllCombos()
        UploadNoteCombosTask(
            engine = nextCloudEngine,
            combos = combos.map { it.copy(databaseVersion = database.openHelper.readableDatabase.version) },
        ).run { result ->
            if (!result.success) logError("Failed to upload note(s) to Nextcloud", result.error)
        }
    }
}
