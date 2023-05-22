package us.huseli.retain.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import us.huseli.retain.data.entities.ChecklistItem
import java.util.UUID

@Dao
interface ChecklistItemDao {
    @Query("UPDATE checklistitem SET checklistItemIsDeleted = 1 WHERE checklistItemNoteId=:noteId AND checklistItemId NOT IN (:except)")
    suspend fun deleteByNoteId(noteId: UUID, except: Collection<UUID> = emptyList())

    @Query("SELECT * FROM checklistitem WHERE checklistItemIsDeleted = 0 ORDER BY checklistItemChecked, checklistItemPosition")
    fun flowList(): Flow<List<ChecklistItem>>

    @Query("SELECT * FROM checklistitem WHERE checklistItemIsDeleted = 0 ORDER BY checklistItemNoteId, checklistItemChecked, checklistItemPosition")
    suspend fun list(): List<ChecklistItem>

    @Query("SELECT * FROM checklistitem WHERE checklistItemNoteId = :noteId AND checklistItemIsDeleted = 0 ORDER BY checklistItemChecked, checklistItemPosition")
    suspend fun listByNoteId(noteId: UUID): List<ChecklistItem>

    suspend fun replace(noteId: UUID, items: Collection<ChecklistItem>) {
        deleteByNoteId(noteId, except = items.map { it.id })
        upsert(items)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: Collection<ChecklistItem>)
}
