package us.huseli.retain.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import us.huseli.retain.data.entities.ChecklistItem
import java.util.UUID

@Dao
interface ChecklistItemDao {
    @Delete
    suspend fun delete(item: ChecklistItem)

    @Query("DELETE FROM checklistitem WHERE checklistItemNoteId=:noteId")
    suspend fun deleteByNoteId(noteId: UUID)

    @Query("SELECT * FROM checklistitem ORDER BY checklistItemChecked, checklistItemPosition")
    fun flowList(): Flow<List<ChecklistItem>>

    @Query(
        """
        INSERT INTO checklistitem (checklistItemId, checklistItemNoteId, checklistItemText, checklistItemChecked, checklistItemPosition)
        VALUES (:id, :noteId, :text, :checked, :position)
        """
    )
    suspend fun insert(id: UUID, noteId: UUID, text: String, checked: Boolean, position: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(items: List<ChecklistItem>)

    @Query("SELECT * FROM checklistitem ORDER BY checklistItemNoteId, checklistItemChecked, checklistItemPosition")
    suspend fun list(): List<ChecklistItem>

    @Query("SELECT * FROM checklistitem WHERE checklistItemNoteId = :noteId ORDER BY checklistItemChecked, checklistItemPosition")
    suspend fun listByNoteId(noteId: UUID): List<ChecklistItem>

    @Query(
        """
        UPDATE checklistitem SET checklistItemPosition = checklistItemPosition + 1 
        WHERE checklistItemNoteId == :noteId AND checklistItemPosition >= :position AND 
        EXISTS(SELECT * FROM checklistitem WHERE checklistItemNoteId = :noteId AND checklistItemPosition = :position)
        """
    )
    suspend fun makePlaceFor(noteId: UUID, position: Int)

    suspend fun replace(noteId: UUID, items: List<ChecklistItem>) {
        deleteByNoteId(noteId)
        insert(items)
    }

    @Update
    suspend fun update(items: Collection<ChecklistItem>)
}
