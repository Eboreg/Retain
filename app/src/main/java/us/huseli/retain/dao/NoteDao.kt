package us.huseli.retain.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import us.huseli.retain.dataclasses.NotePojo
import us.huseli.retain.dataclasses.entities.DeletedNote
import us.huseli.retain.dataclasses.entities.Note
import java.util.UUID

@Dao
interface NoteDao {
    @Delete
    suspend fun _delete(notes: Collection<Note>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun _insert(note: Note)

    @Insert
    suspend fun _insertDeletedNotes(objs: Collection<DeletedNote>)

    @Query("SELECT * FROM Note WHERE noteIsDeleted = 1")
    suspend fun _listDeletedNotes(): List<Note>

    @Query(
        """
        UPDATE note SET notePosition = notePosition + 1 WHERE noteId != :id AND notePosition >= :position AND 
        EXISTS(SELECT * FROM note WHERE noteId != :id AND notePosition = :position)
        """
    )
    suspend fun _makePlaceFor(id: UUID, position: Int)

    @Query("UPDATE note SET notePosition = :position WHERE noteId = :id")
    suspend fun _updatePosition(id: UUID, position: Int)

    @Transaction
    suspend fun deleteTrashed() {
        val notes = _listDeletedNotes()
        _insertDeletedNotes(notes.map { DeletedNote(it.id) })
        _delete(notes)
    }

    @Transaction
    @Query("SELECT * FROM Note WHERE noteId = :noteId")
    fun flowNotePojo(noteId: UUID): Flow<NotePojo?>

    @Transaction
    @Query("SELECT * FROM note WHERE noteIsDeleted = 0 ORDER BY notePosition")
    fun flowNotePojoList(): Flow<List<NotePojo>>

    @Query("SELECT COALESCE(MAX(notePosition), -1) FROM note")
    suspend fun getMaxPosition(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notes: Collection<Note>)

    @Transaction
    @Query("SELECT * FROM note")
    suspend fun listNotePojos(): List<NotePojo>

    @Query("SELECT deletedNoteId FROM deletednote")
    suspend fun listDeletedIds(): List<UUID>

    @Update
    suspend fun update(notes: Collection<Note>)

    @Transaction
    suspend fun updatePositions(notes: Collection<Note>) {
        notes.forEach { _updatePosition(it.id, it.position) }
    }

    @Transaction
    suspend fun upsert(note: Note) {
        _makePlaceFor(note.id, note.position)
        _insert(note)
    }
}
