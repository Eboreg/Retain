package us.huseli.retain.syncbackend.tasks

import com.google.gson.reflect.TypeToken
import us.huseli.retain.Constants.SYNCBACKEND_JSON_SUBDIR
import us.huseli.retain.data.entities.NoteCombo
import us.huseli.retain.syncbackend.Engine
import java.io.File
import java.util.UUID

class DownloadNoteCombosJSONTask<ET : Engine>(
    engine: ET,
    private val deletedNoteIds: Collection<UUID>
) : DownloadListJSONTask<ET, NoteCombo>(
    engine = engine,
    remotePath = engine.getAbsolutePath(SYNCBACKEND_JSON_SUBDIR, "noteCombos.json"),
    localFile = File(engine.tempDirDown, "noteCombos.json"),
) {
    override fun deserialize(json: String): List<NoteCombo>? {
        val listType = object : TypeToken<List<NoteCombo>>() {}
        @Suppress("RemoveExplicitTypeArguments")
        return engine.gson.fromJson<List<NoteCombo>>(json, listType)?.filter {
            !deletedNoteIds.contains(it.note.id)
        }
    }
}
