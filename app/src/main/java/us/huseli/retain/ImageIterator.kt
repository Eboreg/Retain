package us.huseli.retain

import okhttp3.internal.toImmutableList
import us.huseli.retain.dataclasses.entities.Image

class ImageIterator(
    private val objects: List<Image>,
    private val maxRows: Int = Int.MAX_VALUE
) : Iterator<List<Image>> {
    private var currentIndex = 0
    private var currentRow = 0

    override fun hasNext() = currentIndex < objects.size && currentRow < maxRows

    override fun next(): List<Image> {
        val result = mutableListOf<Image>()
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