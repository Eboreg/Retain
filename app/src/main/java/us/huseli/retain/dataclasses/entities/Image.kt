package us.huseli.retain.dataclasses.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import us.huseli.retain.dataclasses.uistate.IImageUiState
import java.time.Instant
import java.util.UUID

@Entity(
    foreignKeys = [ForeignKey(
        entity = Note::class,
        parentColumns = ["noteId"],
        childColumns = ["imageNoteId"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE,
    )]
)
data class Image(
    @ColumnInfo(name = "imageFilename") @PrimaryKey override val filename: String,
    @ColumnInfo(name = "imageMimeType") val mimeType: String?,
    @ColumnInfo(name = "imageWidth") override val width: Int?,
    @ColumnInfo(name = "imageHeight") override val height: Int?,
    @ColumnInfo(name = "imageNoteId", index = true) val noteId: UUID,
    @ColumnInfo(name = "imageAdded") val added: Instant = Instant.now(),
    @ColumnInfo(name = "imageSize") val size: Int,
    @ColumnInfo(name = "imagePosition", defaultValue = "0") override val position: Int = 0,
) : IImageUiState {
    @Ignore
    override val isSelected: Boolean = false

    override fun equals(other: Any?) = other is Image &&
        other.filename == filename &&
        other.mimeType == mimeType &&
        other.width == width &&
        other.height == height &&
        other.noteId == noteId &&
        other.size == size &&
        other.position == position

    override fun hashCode() = filename.hashCode()
}
