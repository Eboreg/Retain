package us.huseli.retain.annotation

import androidx.compose.ui.text.AnnotatedString.Range
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import us.huseli.retain.limit

class RetainAnnotatedString(
    override val text: String,
    spanStyles: List<Range<RetainSpanStyle>> = listOf(),
) : CharSequence, IRetainAnnotatedString {
    val spanStyles: List<Range<RetainSpanStyle>> = spanStyles.limit(text.length)

    override val length: Int
        get() = text.length

    override fun equals(other: Any?): Boolean =
        other is RetainAnnotatedString && other.text == text && other.spanStyles == spanStyles

    override fun get(index: Int): Char = text[index]

    override fun hashCode(): Int = 31 * text.hashCode() + spanStyles.hashCode()

    override fun subSequence(startIndex: Int, endIndex: Int): RetainAnnotatedString {
        if (startIndex == 0 && endIndex == text.length) return this

        return RetainAnnotatedString(
            text = text.substring(startIndex, endIndex),
            spanStyles = spanStyles.filter { maxOf(startIndex, it.start) <= minOf(endIndex, it.end) }.map {
                Range(
                    item = it.item,
                    start = maxOf(startIndex, it.start) - startIndex,
                    end = minOf(endIndex, it.end) - startIndex,
                )
            },
        )
    }

    fun serialize(): String = gson.toJson(this)

    override fun toImmutable(): RetainAnnotatedString = this

    override fun toMutable(): RetainMutableAnnotatedString = RetainMutableAnnotatedString.fromImmutable(this)

    override fun toString(): String = "RetainAnnotatedString[text=$text, spanStyles=$spanStyles]"

    companion object {
        val gson: Gson = GsonBuilder()
            .registerTypeAdapter(RetainSpanStyle::class.java, RetainSpanStyle.TypeAdapter())
            .create()

        fun deserialize(source: String): RetainAnnotatedString {
            return try {
                gson.fromJson(source, RetainAnnotatedString::class.java) ?: RetainAnnotatedString(source)
            } catch (_: Throwable) {
                RetainAnnotatedString(source)
            }
        }
    }
}
