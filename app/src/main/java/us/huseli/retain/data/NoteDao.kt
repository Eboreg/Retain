package us.huseli.retain.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import us.huseli.retain.data.entities.DeletedNote
import us.huseli.retain.data.entities.Note
import us.huseli.retain.data.entities.NoteCombo
import java.util.UUID

@Dao
interface NoteDao {
    @Query("DELETE FROM note WHERE noteId IN (:ids)")
    suspend fun delete(ids: Collection<UUID>)

    @Transaction
    suspend fun deleteTrashed() {
        val ids = listDeletedIds()
        insertDeletedNotes(ids.map { DeletedNote(it) })
        delete(ids)
    }

    @Query("SELECT * FROM note WHERE noteIsDeleted = 0 ORDER BY notePosition")
    fun flowList(): Flow<List<Note>>

    @Query("SELECT COALESCE(MAX(notePosition), -1) FROM note")
    suspend fun getMaxPosition(): Int

    @Query("SELECT * FROM note WHERE noteId = :id")
    suspend fun getNote(id: UUID): Note?

    @Insert
    suspend fun insert(note: Note)

    @Insert
    suspend fun insert(notes: Collection<Note>)

    @Insert
    suspend fun insertDeletedNotes(objs: Collection<DeletedNote>)

    @Transaction
    @Query("SELECT * FROM note")
    suspend fun listAllCombos(): List<NoteCombo>

    @Query("SELECT noteId FROM note WHERE noteIsDeleted = 1")
    suspend fun listDeletedIds(): List<UUID>

    @Query(
        """
        UPDATE note SET notePosition = notePosition + 1 WHERE noteId != :id AND notePosition >= :position AND 
        EXISTS(SELECT * FROM note WHERE noteId != :id AND notePosition = :position)
        """
    )
    suspend fun makePlaceFor(id: UUID, position: Int)

    @Update
    suspend fun update(note: Note)

    @Update
    suspend fun update(notes: Collection<Note>)

    @Query("UPDATE note SET notePosition = :position WHERE noteId = :id")
    suspend fun updatePosition(id: UUID, position: Int)

    @Transaction
    suspend fun updatePositions(notes: Collection<Note>) {
        notes.forEach { updatePosition(it.id, it.position) }
    }

    @Transaction
    suspend fun upsert(note: Note) {
        makePlaceFor(note.id, note.position)
        getNote(note.id)?.let { update(note) } ?: kotlin.run { insert(note) }
    }
}
