package us.huseli.retain.dataclasses.uistate

import androidx.compose.runtime.Stable

@Stable
data class ChecklistItemUiState(
    val text: String,
    val isChecked: Boolean,
    val isFocused: Boolean,
    val isDragging: Boolean,
) {
    val showAutocomplete: Boolean
        get() = isFocused && !isDragging && !isChecked

    val alpha: Float
        get() = if (isChecked) 0.5f else 1f
}
