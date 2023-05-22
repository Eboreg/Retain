package us.huseli.retain.data.entities

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import us.huseli.retain.Constants.IMAGE_SUBDIR
import java.io.File
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
) : Comparable<Image> {
    @Ignore
    var ratio: Float = if (width != null && height != null) width.toFloat() / height.toFloat() else 0f

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

    @Suppress("SameReturnValue")
    fun toBitmapImage(context: Context): BitmapImage? {
        val imageDir = File(context.filesDir, IMAGE_SUBDIR).apply { mkdirs() }
        val imageFile = File(imageDir, filename)

        if (imageFile.isFile) {
            Uri.fromFile(imageFile)?.let { uri ->
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                bitmap?.let { return BitmapImage(this, it.asImageBitmap()) }
            }
        }
        return null
    }
}

data class BitmapImage(val image: Image, val imageBitmap: ImageBitmap) {
    override fun toString() = "<BitmapImage ${image.filename}>"
}
