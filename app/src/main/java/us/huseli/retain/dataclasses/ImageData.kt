package us.huseli.retain.dataclasses

import us.huseli.retain.dataclasses.entities.Image
import java.util.UUID

data class ImageData(
    val filename: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val size: Int,
) {
    fun toImage(noteId: UUID, position: Int) = Image(
        filename = filename,
        mimeType = mimeType,
        width = width,
        height = height,
        noteId = noteId,
        size = size,
        position = position,
    )
}
