package us.huseli.retain.interfaces

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntSize

@Stable
interface IImage : Comparable<IImage> {
    val filename: String
    val position: Int
    val isSelected: Boolean
    val width: Int?
    val height: Int?

    val dimensions: IntSize?
        get() {
            val width = this.width
            val height = this.height

            return if (width != null && height != null) IntSize(width, height) else null
        }

    val ratio: Float
        get() = dimensions?.let { it.width.toFloat() / it.height.toFloat() } ?: 0f

    override fun compareTo(other: IImage): Int = position - other.position
}
