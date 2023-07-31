package us.huseli.retain.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.ChecklistItemWithNote
import java.util.UUID

@Dao
interface ChecklistItemDao {
    @Query("DELETE FROM checklistitem WHERE checklistItemId IN (:ids)")
    suspend fun delete(ids: Collection<UUID>)

    @Query("DELETE FROM checklistitem WHERE checklistItemNoteId=:noteId AND checklistItemId NOT IN (:except)")
    suspend fun deleteByNoteId(noteId: UUID, except: Collection<UUID> = emptyList())

    @Query("SELECT * FROM checklistitem INNER JOIN note ON checklistItemNoteId = noteId ORDER BY checklistItemNoteId, checklistItemChecked, checklistItemPosition")
    fun flowListWithNote(): Flow<List<ChecklistItemWithNote>>

    @Query("SELECT * FROM checklistitem WHERE checklistItemNoteId = :noteId ORDER BY checklistItemPosition")
    suspend fun listByNoteId(noteId: UUID): List<ChecklistItem>

    @Transaction
    suspend fun replace(noteId: UUID, items: Collection<ChecklistItem>) {
        deleteByNoteId(noteId, except = items.map { it.id })
        upsert(items)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: Collection<ChecklistItem>)
}
