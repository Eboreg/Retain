package us.huseli.retain.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import us.huseli.retain.data.entities.Note
import us.huseli.retain.data.entities.NoteCombo
import java.time.Instant
import java.util.UUID

@Dao
interface NoteDao {
    @Query("UPDATE note SET noteIsDeleted=1, noteUpdated=:updated WHERE noteId IN (:ids)")
    suspend fun delete(ids: Collection<UUID>, updated: Instant = Instant.now())

    @Transaction
    @Query("SELECT * FROM note WHERE noteIsDeleted = 0 ORDER BY notePosition")
    fun flowListCombos(): Flow<List<NoteCombo>>

    @Query("SELECT * FROM note WHERE noteId = :id AND noteIsDeleted = 0")
    suspend fun getCombo(id: UUID): NoteCombo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note)

    @Query("SELECT * FROM note")
    suspend fun listAllCombos(): List<NoteCombo>

    @Query(
        """
        UPDATE note SET notePosition = notePosition + 1 WHERE noteId != :id AND notePosition >= :position AND 
        EXISTS(SELECT * FROM note WHERE noteId != :id AND notePosition = :position)
        """
    )
    suspend fun makePlaceFor(id: UUID, position: Int)

    @Update
    suspend fun update(notes: Collection<Note>)

    @Query("UPDATE note SET notePosition = :position WHERE noteId = :id")
    suspend fun updatePosition(id: UUID, position: Int)

    @Transaction
    suspend fun updatePositions(notes: Collection<Note>) {
        notes.forEach { updatePosition(it.id, it.position) }
    }

    suspend fun upsert(note: Note) {
        makePlaceFor(note.id, note.position)
        insert(note)
    }
}
