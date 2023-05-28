package us.huseli.retain.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import us.huseli.retain.data.entities.Image
import java.util.UUID

@Dao
interface ImageDao {
    @Query("UPDATE image SET imageIsDeleted = 1 WHERE imageNoteId=:noteId AND imageFilename NOT IN (:except)")
    suspend fun deleteByNoteId(noteId: UUID, except: Collection<String> = emptyList())

    @Query("SELECT * FROM image WHERE imageIsDeleted = 0 ORDER BY imageNoteId, imagePosition")
    fun flowList(): Flow<List<Image>>

    @Query("SELECT COALESCE(MAX(imagePosition), -1) FROM image WHERE imageNoteId = :noteId")
    suspend fun getMaxPosition(noteId: UUID): Int

    @Query("SELECT * FROM image WHERE imageNoteId IN (:noteIds) AND imageIsDeleted = 0 ORDER BY imagePosition")
    suspend fun listByNoteIds(noteIds: Collection<UUID>): List<Image>

    @Query("SELECT * FROM image ORDER BY imagePosition")
    suspend fun listAll(): List<Image>

    @Transaction
    suspend fun replace(noteId: UUID, images: Collection<Image>) {
        deleteByNoteId(noteId, except = images.map { it.filename })
        upsert(images)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(images: Collection<Image>)
}
