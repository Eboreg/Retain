package us.huseli.retain.data.entities

import androidx.compose.ui.graphics.ImageBitmap
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
    @PrimaryKey
    @ColumnInfo(name = "imageFilename")
    val filename: String,
    @ColumnInfo(name = "imageMimeType") val mimeType: String?,
    @ColumnInfo(name = "imageWidth") val width: Int?,
    @ColumnInfo(name = "imageHeight") val height: Int?,
    @ColumnInfo(name = "imageNoteId", index = true) val noteId: UUID,
    @ColumnInfo(name = "imageAdded") val added: Instant = Instant.now(),
    @ColumnInfo(name = "imageSize") val size: Int,
    @ColumnInfo(name = "imageIsDeleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "imagePosition", defaultValue = "0") val position: Int = 0,
    @Ignore val imageBitmap: Flow<ImageBitmap?> = MutableStateFlow<ImageBitmap?>(null),
) : Comparable<Image> {
    @Ignore
    val ratio: Float = if (width != null && height != null) width.toFloat() / height.toFloat() else 0f

    constructor(
        filename: String,
        mimeType: String?,
        width: Int?,
        height: Int?,
        noteId: UUID,
        added: Instant,
        size: Int,
        isDeleted: Boolean,
        position: Int
    ) :
        this(
            filename = filename,
            mimeType = mimeType,
            width = width,
            height = height,
            noteId = noteId,
            added = added,
            size = size,
            isDeleted = isDeleted,
            position = position,
            imageBitmap = MutableStateFlow<ImageBitmap?>(null)
        )

    override fun equals(other: Any?) =
        other is Image &&
        other.filename == filename &&
        other.mimeType == mimeType &&
        other.width == width &&
        other.height == height &&
        other.noteId == noteId &&
        other.added == added &&
        other.size == size &&
        other.isDeleted == isDeleted &&
        other.position == position

    override fun hashCode() = filename.hashCode()

    override fun compareTo(other: Image) = (added.epochSecond - other.added.epochSecond).toInt()
}
