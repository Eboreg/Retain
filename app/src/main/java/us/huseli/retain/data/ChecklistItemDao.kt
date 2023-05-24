package us.huseli.retain.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import us.huseli.retain.data.entities.ChecklistItem
import java.util.UUID

@Dao
interface ChecklistItemDao {
    @Query("UPDATE checklistitem SET checklistItemIsDeleted = 1 WHERE checklistItemNoteId=:noteId AND checklistItemId NOT IN (:except)")
    suspend fun deleteByNoteId(noteId: UUID, except: Collection<UUID> = emptyList())

    suspend fun replace(noteId: UUID, items: Collection<ChecklistItem>) {
        deleteByNoteId(noteId, except = items.map { it.id })
        upsert(items)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: Collection<ChecklistItem>)
}
