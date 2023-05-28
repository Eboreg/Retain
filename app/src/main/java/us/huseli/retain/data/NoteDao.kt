package us.huseli.retain.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import us.huseli.retain.data.entities.Note
import us.huseli.retain.data.entities.NoteCombo
import java.util.UUID

@Dao
interface NoteDao {
    @Query("SELECT * FROM note WHERE noteIsDeleted = 0 ORDER BY notePosition")
    fun flowList(): Flow<List<Note>>

    @Query("SELECT * FROM note WHERE noteId = :id")
    suspend fun getNote(id: UUID): Note?

    @Insert
    suspend fun insert(note: Note)

    @Transaction
    @Query("SELECT * FROM note")
    suspend fun listAllCombos(): List<NoteCombo>

    @Transaction
    @Query("SELECT * FROM note WHERE noteId IN (:ids)")
    suspend fun listCombos(ids: Collection<UUID>): List<NoteCombo>

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
