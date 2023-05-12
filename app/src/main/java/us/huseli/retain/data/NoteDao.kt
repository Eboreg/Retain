package us.huseli.retain.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import us.huseli.retain.Enums
import us.huseli.retain.data.entities.Note
import java.time.Instant
import java.util.UUID

@Dao
interface NoteDao {
    @Query("DELETE FROM note WHERE noteId IN (:ids)")
    suspend fun delete(ids: Collection<UUID>)

    @Query("SELECT * FROM note WHERE noteId = :id")
    fun flow(id: UUID): Flow<Note?>

    @Query("SELECT * FROM note ORDER BY notePosition")
    fun flowList(): Flow<List<Note>>

    @Query("SELECT * FROM note WHERE noteId = :id")
    suspend fun get(id: UUID): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note)

    @Query(
        """
        INSERT INTO note (noteId, noteType, noteTitle, noteText, notePosition, noteShowChecked, noteColorIdx, noteCreated, noteUpdated) 
        VALUES (:id, :type, :title, :text, :position, :showChecked, :colorIdx, :created, :updated)
        """
    )
    suspend fun insert(
        id: UUID,
        type: Enums.NoteType,
        title: String,
        text: String,
        showChecked: Boolean,
        position: Int = 0,
        colorIdx: Int = 0,
        created: Instant = Instant.now(),
        updated: Instant = Instant.now(),
    )

    @Query("SELECT * FROM note")
    suspend fun list(): List<Note>

    @Query(
        """
        UPDATE note SET notePosition = notePosition + 1 WHERE noteId != :id AND notePosition >= :position AND 
        EXISTS(SELECT * FROM note WHERE noteId != :id AND notePosition = :position)
        """
    )
    suspend fun makePlaceFor(id: UUID, position: Int)

    @Query("UPDATE note SET noteUpdated=:updated WHERE noteId=:id")
    suspend fun touch(id: UUID, updated: Instant = Instant.now())

    @Query("UPDATE note SET noteTitle=:title, noteText=:text, noteShowChecked=:showChecked, noteColorIdx=:colorIdx, noteUpdated=:updated WHERE noteId=:id")
    suspend fun update(
        id: UUID,
        title: String,
        text: String,
        showChecked: Boolean,
        colorIdx: Int,
        updated: Instant = Instant.now()
    )

    suspend fun upsert(note: Note) {
        makePlaceFor(note.id, note.position)
        insert(note)
    }

    suspend fun upsert(id: UUID, title: String, text: String, showChecked: Boolean, colorIdx: Int) {
        try {
            insert(id, Enums.NoteType.TEXT, title, text, showChecked, colorIdx = colorIdx)
            makePlaceFor(id, 0)
        } catch (e: SQLiteConstraintException) {
            update(id, title, text, showChecked, colorIdx)
        }
    }
}
