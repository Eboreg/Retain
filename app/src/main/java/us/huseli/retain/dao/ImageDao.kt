package us.huseli.retain.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import us.huseli.retain.dataclasses.entities.Image
import java.util.UUID

@Dao
abstract class ImageDao {
    @Query("DELETE FROM image WHERE imageNoteId=:noteId AND imageFilename NOT IN (:except)")
    protected abstract suspend fun _deleteByNoteId(noteId: UUID, except: Collection<String> = emptyList())

    @Query("DELETE FROM Image WHERE imageFilename IN (:filenames)")
    abstract suspend fun delete(vararg filenames: String)

    @Query("SELECT * FROM image WHERE imageNoteId = :noteId ORDER BY imagePosition")
    abstract suspend fun listByNoteId(noteId: UUID): List<Image>

    @Transaction
    open suspend fun replace(noteId: UUID, images: Collection<Image>) {
        _deleteByNoteId(noteId, except = images.map { it.filename })
        upsert(images)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(images: Collection<Image>)
}
