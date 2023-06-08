package us.huseli.retain.nextcloud.tasks

import com.google.gson.reflect.TypeToken
import us.huseli.retain.Constants
import us.huseli.retain.data.entities.NoteCombo
import us.huseli.retain.nextcloud.NextCloudEngine
import java.util.UUID

class DownloadNoteCombosJSONTask(
    engine: NextCloudEngine,
    private val deletedNoteIds: Collection<UUID>
) : DownloadListJSONTask<NoteCombo>(
    engine = engine,
    remotePath = engine.getAbsolutePath(Constants.NEXTCLOUD_JSON_SUBDIR, "noteCombos.json")
) {
    override fun deserialize(json: String): Collection<NoteCombo>? {
        val listType = object : TypeToken<Collection<NoteCombo>>() {}
        @Suppress("RemoveExplicitTypeArguments")
        return engine.gson.fromJson<Collection<NoteCombo>>(json, listType)?.filter {
            !deletedNoteIds.contains(it.note.id)
        }
    }
}
