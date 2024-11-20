package us.huseli.retain

import okhttp3.internal.toImmutableList
import us.huseli.retain.dataclasses.uistate.IImageUiState

class ImageIterator(
    private val objects: List<IImageUiState>,
    private val maxRows: Int = Int.MAX_VALUE
) : Iterator<List<IImageUiState>> {
    private var currentIndex = 0
    private var currentRow = 0

    override fun hasNext() = currentIndex < objects.size && currentRow < maxRows

    override fun next(): List<IImageUiState> {
        val result = mutableListOf<IImageUiState>()
        var collectedRatio = 0f

        for (i in currentIndex until objects.size) {
            if (collectedRatio > 0 && collectedRatio + objects[i].ratio > 5) break
            result.add(objects[i])
            collectedRatio += objects[i].ratio
            currentIndex++
            if (currentIndex == 1) break
        }

        currentRow++
        return result.toImmutableList()
    }
}