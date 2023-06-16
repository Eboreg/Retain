package us.huseli.retain.syncbackend.tasks

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.huseli.retain.Constants
import us.huseli.retain.data.entities.NoteCombo
import us.huseli.retain.syncbackend.Engine
import java.io.File
import java.io.FileWriter

class UploadNoteCombosTask<ET : Engine>(
    engine: ET,
    private val combos: Collection<NoteCombo>
) : OperationTask<ET, OperationTaskResult>(engine) {
    private val filename = "noteCombos.json"
    private val remotePath = engine.getAbsolutePath(Constants.SYNCBACKEND_JSON_SUBDIR, filename)
    private val localFile = File(engine.tempDirUp, filename).apply { deleteOnExit() }

    override fun start(onResult: (OperationTaskResult) -> Unit) {
        engine.ioScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    FileWriter(localFile).use { it.write(engine.gson.toJson(combos)) }
                    engine.uploadFile(localFile, remotePath, "application/json", onResult)
                } catch (e: Exception) {
                    onResult(OperationTaskResult(status = TaskResult.Status.OTHER_ERROR, exception = e))
                }
            }
        }
    }
}
