package us.huseli.retain.annotation

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.AnnotatedString.Range
import androidx.compose.ui.unit.TextUnit

interface IRetainAnnotatedString : CharSequence {
    val text: String
    // val spanStyles: List<Range<RetainSpanStyle>>

    fun toImmutable(): RetainAnnotatedString

    fun toMutable(): RetainMutableAnnotatedString

    fun toNative(baseFontSize: TextUnit): AnnotatedString {
        val immutable = toImmutable()

        return AnnotatedString(
            text = text,
            spanStyles = immutable.spanStyles.map { range ->
                Range(item = range.item.toNative(baseFontSize), start = range.start, end = range.end)
            },
        )
    }
}
