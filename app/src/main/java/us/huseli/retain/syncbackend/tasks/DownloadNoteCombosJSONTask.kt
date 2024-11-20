package us.huseli.retain.syncbackend.tasks

import com.google.gson.reflect.TypeToken
import us.huseli.retain.Constants.SYNCBACKEND_JSON_SUBDIR
import us.huseli.retain.dataclasses.NotePojo
import us.huseli.retain.syncbackend.Engine
import us.huseli.retain.syncbackend.tasks.abstr.AbstractDownloadListJSONTask
import java.io.File
import java.util.UUID

class DownloadNoteCombosJSONTask<ET : Engine>(
    engine: ET,
    private val deletedNoteIds: Collection<UUID>
) : AbstractDownloadListJSONTask<ET, NotePojo>(
    engine = engine,
    remotePath = engine.getAbsolutePath(SYNCBACKEND_JSON_SUBDIR, "noteCombos.json"),
    localFile = File(engine.tempDirDown, "noteCombos.json"),
) {
    override fun deserialize(json: String): List<NotePojo>? {
        val listType = object : TypeToken<List<NotePojo>>() {}
        @Suppress("RemoveExplicitTypeArguments")
        return engine.gson.fromJson<List<NotePojo>>(json, listType)?.filter {
            !deletedNoteIds.contains(it.note.id)
        }
    }
}
