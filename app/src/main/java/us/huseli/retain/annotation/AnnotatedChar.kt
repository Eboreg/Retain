package us.huseli.retain.annotation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AnnotatedChar(val char: Char, style: RetainSpanStyle) {
    var style: RetainSpanStyle by mutableStateOf(style)
    var nextCharStyle: RetainSpanStyle? by mutableStateOf(null)

    override fun equals(other: Any?): Boolean = other is AnnotatedChar &&
        other.char == char &&
        other.style == style &&
        other.nextCharStyle == nextCharStyle

    override fun hashCode(): Int {
        var result = char.hashCode()

        result = 31 * result + style.hashCode()
        result = 31 * result + (nextCharStyle?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        val values = mutableListOf<String>(char.toString(), style.toString())

        nextCharStyle?.also { values.add("nextCharStyle[${it}]") }
        return "AnnotatedChar[${values.joinToString(", ")}]"
    }
}
