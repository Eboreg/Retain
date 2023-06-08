package us.huseli.retain.nextcloud.tasks

import us.huseli.retain.Constants
import us.huseli.retain.data.entities.Image
import us.huseli.retain.data.entities.NoteCombo
import us.huseli.retain.nextcloud.NextCloudEngine
import java.io.File

/** Down: 0..n image files */
class DownloadNoteImagesTask(
    engine: NextCloudEngine,
    noteCombo: NoteCombo
) : ListTask<OperationTaskResult, DownloadFileTask, Image>(
    engine = engine,
    objects = noteCombo.images
) {
    override val startMessageString = "Starting download of ${noteCombo.images}"
    override val failOnUnsuccessfulChildTask = false

    override fun getChildTask(obj: Image) = DownloadFileTask(
        engine = engine,
        remotePath = engine.getAbsolutePath(Constants.NEXTCLOUD_IMAGE_SUBDIR, obj.filename),
        tempDir = engine.tempDirDown,
        localFile = File(File(engine.context.filesDir, "images"), obj.filename),
    )
}