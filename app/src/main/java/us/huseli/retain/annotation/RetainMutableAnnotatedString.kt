package us.huseli.retain.annotation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.text.AnnotatedString
import com.github.difflib.DiffUtils
import com.github.difflib.patch.Chunk
import com.github.difflib.patch.DeltaType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RetainMutableAnnotatedString(achars: List<AnnotatedChar> = emptyList()) :
    CharSequence, IRetainAnnotatedString {
    private val achars: SnapshotStateList<AnnotatedChar> = achars.toMutableStateList()
    private var startStyle: RetainSpanStyle by mutableStateOf(RetainSpanStyle())
    private val mutex = Mutex()

    override var text: String = achars.map { it.char }.joinToString("")

    override val length: Int
        get() = achars.size

    private fun splitFinishedWords(string: String): List<String> {
        // Last word is considered potentially "unfinished" if the whole string ends with a word character.
        return string.split(Regex("\\s+")).let { if (string.matches(Regex("\\w$"))) it.dropLast(1) else it }
    }

    suspend fun applyDiff(revised: String): Boolean {
        return mutex.withLock {
            val diff = DiffUtils.diffInline(text, revised)
            val oldWords = text.split(Regex("\\s+"))
            val oldFinishedWords = splitFinishedWords(text)
            val revisedWords = revised.split(Regex("\\s+"))
            val revisedFinishedWords = splitFinishedWords(revised)
            val whiteSpaceAdded =
                diff?.deltas?.any { delta -> delta?.target?.lines?.any { it.contains(Regex("\\s")) } == true }

            // A word is considered added if new wordlist != old wordlist and target delta contains a non-word char.
            // A word is considered deleted if new wordlist is shorter than old.
            val wordsChanged = (whiteSpaceAdded == true && oldFinishedWords != revisedFinishedWords) ||
                    oldWords.size > revisedWords.size

            diff?.deltas?.filterNotNull()?.forEach { delta ->
                when (delta.type) {
                    DeltaType.CHANGE -> {
                        delta.source?.also { removeRange(it) }
                        delta.target?.also { insertRange(it) }
                    }

                    DeltaType.DELETE -> delta.source?.also { removeRange(it) }
                    DeltaType.INSERT -> delta.target?.also { insertRange(it) }
                    DeltaType.EQUAL -> {}
                }
            }

            wordsChanged
        }
    }

    fun collapseSpanStyles(): List<AnnotatedString.Range<RetainSpanStyle>> =
        if (this.achars.isEmpty()) emptyList()
        else {
            var idx = 0
            val ranges = mutableListOf<AnnotatedString.Range<RetainSpanStyle>>()

            while (idx < this.achars.size) {
                val style = this.achars[idx].style
                val length = this.achars.drop(idx).takeWhile { it.style == style }.size

                ranges.add(AnnotatedString.Range(item = style, start = idx, end = idx + length))
                idx += length
            }
            ranges
        }

    override fun equals(other: Any?): Boolean = other is RetainMutableAnnotatedString &&
        other.achars == achars &&
        other.startStyle == startStyle

    override fun get(index: Int): Char = achars[index].char

    fun getCharStyle(position: Int): RetainSpanStyle {
        if (position == 0 && achars.isEmpty()) return startStyle
        return achars[position].style
    }

    fun getFutureCharStyle(position: Int): RetainSpanStyle {
        if (position == 0) return startStyle
        return achars[position - 1].nextCharStyle ?: achars[position - 1].style
    }

    override fun hashCode(): Int = 31 * achars.hashCode() + startStyle.hashCode()

    suspend fun setStyle(start: Int, end: Int, value: IRetainSpanStyle) {
        mutex.withLock {
            setAttribute(s = start, e = end) { it.merge(value) }
        }
    }

    fun split(position: Int): Pair<RetainMutableAnnotatedString, RetainMutableAnnotatedString> =
        Pair(subSequence(0, position), subSequence(position, text.length))

    override fun subSequence(startIndex: Int, endIndex: Int): RetainMutableAnnotatedString =
        RetainMutableAnnotatedString(
            achars.subList(startIndex, endIndex).apply { lastOrNull()?.nextCharStyle = null }
        )

    override fun toImmutable() = RetainAnnotatedString(text = text, spanStyles = collapseSpanStyles())

    override fun toMutable(): RetainMutableAnnotatedString = this

    private fun insertRange(chunk: Chunk<String>) {
        val style = getFutureCharStyle(chunk.position)
        val newAchars = chunk.lines.flatMap { it.map { char -> AnnotatedChar(char, style) } }

        achars.addAll(chunk.position, newAchars)
        updateText()
    }

    private fun removeRange(chunk: Chunk<String>) {
        val toIndex = chunk.position + chunk.lines.sumOf { it.length }

        achars.removeRange(chunk.position, toIndex)
        if (chunk.position == 0) startStyle = RetainSpanStyle()
        updateText()
    }

    private fun setAttribute(
        s: Int,
        e: Int,
        cp: (RetainSpanStyle) -> RetainSpanStyle,
    ) {
        if (s == e) {
            if (s == 0) startStyle = cp(startStyle)
            else achars.getOrNull(s - 1)?.also { achar ->
                achar.nextCharStyle = cp(achar.nextCharStyle ?: achar.style)
            }
        } else {
            for (idx in s until e) {
                achars.getOrNull(idx)?.also { achar ->
                    achar.style = cp(achar.style)
                }
            }
        }
    }

    private fun updateText() {
        text = achars.map { it.char }.joinToString("")
    }

    companion object {
        fun fromImmutable(value: RetainAnnotatedString) = RetainMutableAnnotatedString(
            value.text.mapIndexed { idx, char ->
                AnnotatedChar(
                    char = char,
                    style = value.spanStyles
                        .filter { it.start <= idx && it.end > idx }
                        .map { it.item }
                        .nonNullableMerge(),
                )
            }
        )
    }
}
