package us.huseli.retain.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import us.huseli.retain.dataclasses.entities.ChecklistItem
import java.util.UUID

@Dao
abstract class ChecklistItemDao {
    @Query("DELETE FROM checklistitem WHERE checklistItemNoteId=:noteId AND checklistItemId NOT IN (:except)")
    protected abstract suspend fun _deleteByNoteId(noteId: UUID, except: Collection<UUID> = emptyList())

    @Query("DELETE FROM checklistitem WHERE checklistItemId IN (:itemIds)")
    abstract suspend fun delete(vararg itemIds: UUID)

    @Query("SELECT * FROM checklistitem WHERE checklistItemNoteId=:noteId ORDER BY checklistItemChecked, checklistItemPosition")
    abstract suspend fun listByNoteId(noteId: UUID): List<ChecklistItem>

    @Transaction
    open suspend fun replace(noteId: UUID, items: Collection<ChecklistItem>) {
        _deleteByNoteId(noteId, except = items.map { it.id })
        upsert(*items.toTypedArray())
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(vararg items: ChecklistItem)
}
