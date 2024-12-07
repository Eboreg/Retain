package us.huseli.retain.interfaces

import androidx.compose.runtime.Stable
import us.huseli.retain.annotation.RetainAnnotatedString
import java.util.UUID

@Stable
interface IChecklistItem : Comparable<IChecklistItem> {
    val isChecked: Boolean
    val isFocused: Boolean
    val isDragging: Boolean
    val id: UUID
    val position: Int
    val annotatedText: RetainAnnotatedString
    val serializedText: String

    val alpha: Float
        get() = if (isChecked) 0.5f else 1f

    val compareValue: Int
        get() = position + if (isChecked) 100_000 else 0

    val showAutocomplete: Boolean
        get() = isFocused && !isDragging && !isChecked

    override fun compareTo(other: IChecklistItem): Int = compareValue - other.compareValue
}
