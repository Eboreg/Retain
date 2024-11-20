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
import java.time.Instant
import java.util.UUID

@Dao
abstract class NoteDao {
    @Delete
    protected abstract suspend fun _delete(notes: Collection<Note>)

    @Query("SELECT EXISTS(SELECT * FROM note WHERE noteId = :noteId)")
    protected abstract suspend fun _exists(noteId: UUID): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun _insert(note: Note)

    @Insert
    protected abstract suspend fun _insertDeletedNotes(objs: Collection<DeletedNote>)

    @Query("SELECT * FROM Note WHERE noteIsDeleted = 1")
    protected abstract suspend fun _listDeletedNotes(): List<Note>

    @Query(
        """
        UPDATE note SET notePosition = notePosition + 1 WHERE noteId != :id AND notePosition >= :position AND 
        EXISTS(SELECT * FROM note WHERE noteId != :id AND notePosition = :position)
        """
    )
    protected abstract suspend fun _makePlaceFor(id: UUID, position: Int)

    @Update
    protected abstract suspend fun _update(vararg notes: Note)

    @Query("UPDATE note SET notePosition = :position WHERE noteId = :id")
    protected abstract suspend fun _updatePosition(id: UUID, position: Int)

    @Transaction
    open suspend fun deleteTrashed() {
        val notes = _listDeletedNotes()
        _insertDeletedNotes(notes.map { DeletedNote(it.id) })
        _delete(notes)
    }

    @Query("SELECT * FROM Note WHERE noteId = :noteId")
    abstract fun flowNote(noteId: UUID): Flow<Note>

    @Transaction
    @Query("SELECT * FROM Note WHERE noteId = :noteId")
    abstract fun flowNotePojo(noteId: UUID): Flow<NotePojo?>

    @Transaction
    @Query("SELECT * FROM note WHERE noteIsDeleted = 0 ORDER BY notePosition")
    abstract fun flowNotePojoList(): Flow<List<NotePojo>>

    @Query("SELECT COALESCE(MAX(notePosition), -1) FROM note")
    abstract suspend fun getMaxPosition(): Int

    @Query("SELECT * FROM Note WHERE noteId = :noteId")
    abstract suspend fun getNote(noteId: UUID): Note

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(notes: Collection<Note>)

    @Transaction
    @Query("SELECT * FROM note")
    abstract suspend fun listNotePojos(): List<NotePojo>

    @Query("SELECT deletedNoteId FROM deletednote")
    abstract suspend fun listDeletedIds(): List<UUID>

    suspend fun update(notes: Collection<Note>) {
        _update(*notes.map { it.copy(updated = Instant.now()) }.toTypedArray())
    }

    @Transaction
    open suspend fun updatePositions(notes: Collection<Note>) {
        notes.forEach { _updatePosition(it.id, it.position) }
    }

    @Transaction
    open suspend fun upsert(note: Note): Note {
        _makePlaceFor(note.id, note.position)
        if (_exists(note.id)) update(listOf(note))
        else _insert(note)
        return getNote(note.id)
    }
}
