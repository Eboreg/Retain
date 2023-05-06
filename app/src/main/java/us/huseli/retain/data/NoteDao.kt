package us.huseli.retain.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import us.huseli.retain.Enums
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.Note
import java.util.Date
import java.util.UUID

@Dao
interface NoteDao {
    /** Note related stuff */

    @Delete
    suspend fun deleteNotes(notes: Collection<Note>)

    @Query("SELECT * FROM note WHERE noteId = :id")
    fun getNote(id: UUID): Flow<Note?>

    @Query("SELECT * FROM note ORDER BY notePosition")
    fun loadNotes(): Flow<List<Note>>

    @Query(
        """
        INSERT INTO note (noteId, noteType, noteTitle, noteText, notePosition, noteShowChecked, noteCreated, noteUpdated) 
        VALUES (:id, :type, :title, :text, :showChecked, :position, :created, :updated)
        """
    )
    suspend fun insertNote(
        id: UUID,
        type: Enums.NoteType,
        title: String,
        text: String,
        showChecked: Boolean,
        position: Int = 0,
        created: Date = Date(),
        updated: Date = Date(),
    )

    @Query(
        """
        UPDATE note SET notePosition = notePosition + 1 WHERE noteId != :id AND notePosition >= :position AND 
        EXISTS(SELECT * FROM note WHERE noteId != :id AND notePosition = :position)
        """
    )
    suspend fun makePlaceForNote(id: UUID, position: Int)

    @Query("UPDATE note SET noteTitle=:title, noteText=:text, noteShowChecked=:showChecked, noteUpdated=:updated WHERE noteId=:id")
    suspend fun updateNote(id: UUID, title: String, text: String, showChecked: Boolean, updated: Date = Date())

    suspend fun upsertChecklistNote(id: UUID, title: String, showChecked: Boolean) {
        try {
            makePlaceForNote(id, 0)
            insertNote(id, Enums.NoteType.CHECKLIST, title, "", showChecked)
        } catch (e: SQLiteConstraintException) {
            updateNote(id, title, "", showChecked)
        }
    }

    suspend fun upsertTextNote(id: UUID, title: String, text: String) {
        try {
            makePlaceForNote(id, 0)
            insertNote(id, Enums.NoteType.TEXT, title, text, true)
        } catch (e: SQLiteConstraintException) {
            updateNote(id, title, text, true)
        }
    }

    /** ChecklistItem related stuff */

    @Delete
    suspend fun deleteChecklistItem(item: ChecklistItem)

    @Query(
        """
        INSERT INTO checklistitem (checklistItemId, checklistItemNoteId, checklistItemText, checklistItemChecked, checklistItemPosition)
        VALUES (:id, :noteId, :text, :checked, :position)
        """
    )
    suspend fun insertChecklistItem(id: UUID, noteId: UUID, text: String, checked: Boolean, position: Int)

    @Query("SELECT * FROM checklistitem ORDER BY checklistItemPosition")
    fun loadChecklistItems(): Flow<List<ChecklistItem>>

    @Query("SELECT * FROM checklistitem WHERE checklistItemNoteId = :noteId ORDER BY checklistItemChecked, checklistItemPosition")
    fun loadChecklistItems(noteId: UUID): Flow<List<ChecklistItem>>

    @Query(
        """
        UPDATE checklistitem SET checklistItemPosition = checklistItemPosition + 1 
        WHERE checklistItemId != :id AND checklistItemNoteId == :noteId AND checklistItemPosition >= :position AND 
        EXISTS(SELECT * FROM checklistitem WHERE checklistItemId != :id AND checklistItemNoteId = :noteId AND checklistItemPosition = :position)
        """
    )
    suspend fun makePlaceForChecklistItem(id: UUID, noteId: UUID, position: Int)

    @Query(
        """
        UPDATE checklistitem SET checklistItemPosition = checklistItemPosition + 1 
        WHERE checklistItemNoteId == :noteId AND checklistItemPosition >= :position AND 
        EXISTS(SELECT * FROM checklistitem WHERE checklistItemNoteId = :noteId AND checklistItemPosition = :position)
        """
    )
    suspend fun makePlaceForChecklistItem(noteId: UUID, position: Int)

    @Query("UPDATE checklistitem SET checklistItemText=:text, checklistItemChecked=:checked, checklistItemPosition=:position WHERE checklistItemId=:id")
    suspend fun updateChecklistItem(id: UUID, text: String, checked: Boolean, position: Int)
}
