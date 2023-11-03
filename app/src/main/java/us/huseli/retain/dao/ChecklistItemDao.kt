package us.huseli.retain.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import us.huseli.retain.dataclasses.entities.ChecklistItem
import java.util.UUID

@Dao
interface ChecklistItemDao {
    @Query("DELETE FROM checklistitem WHERE checklistItemNoteId=:noteId AND checklistItemId NOT IN (:except)")
    suspend fun _deleteByNoteId(noteId: UUID, except: Collection<UUID> = emptyList())

    @Transaction
    suspend fun replace(noteId: UUID, items: Collection<ChecklistItem>) {
        _deleteByNoteId(noteId, except = items.map { it.id })
        upsert(items)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: Collection<ChecklistItem>)
}
