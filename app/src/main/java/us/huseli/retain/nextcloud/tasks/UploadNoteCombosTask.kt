package us.huseli.retain.nextcloud.tasks

import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.huseli.retain.Constants
import us.huseli.retain.data.entities.NoteCombo
import us.huseli.retain.nextcloud.NextCloudEngine
import java.io.File
import java.io.FileWriter

class UploadNoteCombosTask(engine: NextCloudEngine, private val combos: Collection<NoteCombo>) : OperationTask(engine) {
    private val filename = "noteCombos.json"
    private val remotePath = engine.getAbsolutePath(Constants.NEXTCLOUD_JSON_SUBDIR, filename)
    private val localFile = File(engine.tempDirUp, filename).apply { deleteOnExit() }

    override val remoteOperation = UploadFileRemoteOperation(
        localFile.absolutePath,
        remotePath,
        "application/json",
        (System.currentTimeMillis() / 1000).toString()
    )

    override fun start() {
        engine.ioScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    FileWriter(localFile).use { it.write(engine.gson.toJson(combos)) }
                    super.start()
                } catch (e: Exception) {
                    failWithMessage(e.toString())
                }
            }
        }
    }
}
