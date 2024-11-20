package us.huseli.retain.syncbackend.tasks

import us.huseli.retain.Constants.SYNCBACKEND_JSON_SUBDIR
import us.huseli.retain.dataclasses.NotePojo
import us.huseli.retain.syncbackend.Engine
import us.huseli.retain.syncbackend.tasks.abstr.AbstractOperationTask
import us.huseli.retain.syncbackend.tasks.result.OperationTaskResult
import us.huseli.retain.syncbackend.tasks.result.TaskResult
import java.io.File
import java.io.FileWriter

class UploadNoteCombosTask<ET : Engine>(
    engine: ET,
    private val combos: Collection<NotePojo>
) : AbstractOperationTask<ET, OperationTaskResult>(engine) {
    private val filename = "noteCombos.json"
    private val remotePath = engine.getAbsolutePath(SYNCBACKEND_JSON_SUBDIR, filename)
    private val localFile = File(engine.tempDirUp, filename).apply { deleteOnExit() }

    override fun start(onResult: (OperationTaskResult) -> Unit) {
        engine.launch {
            try {
                FileWriter(localFile).use { it.write(engine.gson.toJson(combos)) }
                engine.uploadFile(localFile, remotePath, "application/json", onResult)
            } catch (e: Exception) {
                onResult(OperationTaskResult(status = TaskResult.Status.OTHER_ERROR, exception = e))
            }
        }
    }
}
