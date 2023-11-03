package us.huseli.retain.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import us.huseli.retain.dataclasses.entities.Image
import java.util.UUID

@Dao
interface ImageDao {
    @Query("DELETE FROM image WHERE imageNoteId=:noteId AND imageFilename NOT IN (:except)")
    suspend fun _deleteByNoteId(noteId: UUID, except: Collection<String> = emptyList())

    @Query("SELECT COALESCE(MAX(imagePosition), -1) FROM image WHERE imageNoteId = :noteId")
    suspend fun getMaxPosition(noteId: UUID): Int

    @Query("SELECT * FROM image WHERE imageNoteId = :noteId ORDER BY imagePosition")
    suspend fun listByNoteId(noteId: UUID): List<Image>

    @Transaction
    suspend fun replace(noteId: UUID, images: Collection<Image>) {
        _deleteByNoteId(noteId, except = images.map { it.filename })
        upsert(images)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(images: Collection<Image>)
}
