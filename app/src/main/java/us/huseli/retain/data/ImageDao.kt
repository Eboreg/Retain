package us.huseli.retain.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import us.huseli.retain.data.entities.Image
import java.time.Instant
import java.util.UUID

@Dao
interface ImageDao {
    @Delete
    suspend fun delete(image: Image)

    @Query("DELETE FROM image WHERE imageNoteId=:noteId")
    suspend fun deleteByNoteId(noteId: UUID)

    @Query("SELECT * FROM image ORDER BY imageNoteId")
    fun flowList(): Flow<List<Image>>

    @Query("SELECT * FROM image WHERE imageNoteId=:noteId")
    fun flowList(noteId: UUID): Flow<List<Image>>

    @Query("SELECT * FROM Image WHERE imageFilename=:filename")
    suspend fun get(filename: String): Image?

    @Query(
        """
        INSERT INTO image (imageNoteId, imageFilename, imageMimeType, imageWidth, imageHeight, imageAdded, imageSize)
        VALUES (:noteId, :filename, :mimeType, :width, :height, :added, :size)
        """
    )
    suspend fun insert(
        noteId: UUID,
        filename: String,
        mimeType: String?,
        width: Int?,
        height: Int?,
        size: Int,
        added: Instant = Instant.now(),
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(images: Collection<Image>)

    @Query("SELECT * FROM image WHERE imageNoteId in (:noteIds)")
    suspend fun list(noteIds: Collection<UUID>): List<Image>

    @Query("SELECT * FROM image WHERE imageNoteId=:noteId")
    suspend fun list(noteId: UUID): List<Image>

    @Query("SELECT * FROM image")
    suspend fun list(): List<Image>

    suspend fun replace(noteId: UUID, images: Collection<Image>) {
        deleteByNoteId(noteId)
        insert(images)
    }
}
