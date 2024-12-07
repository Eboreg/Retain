package us.huseli.retain.annotation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.AnnotatedString.Range
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.TextUnit
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import us.huseli.retain.Logger
import us.huseli.retain.limit

@Stable
class RetainAnnotatedStringState(
    initialValue: IRetainAnnotatedString,
    textFieldValue: TextFieldValue? = null,
) {
    data class Change(val wholeWords: Boolean = false, val style: Boolean = false)

    private val mutex = Mutex()
    private var mutableString: RetainMutableAnnotatedString by mutableStateOf(initialValue.toMutable())
    val changeChan = Channel<Change>(capacity = RENDEZVOUS, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val selectionStartStyle: RetainSpanStyle by derivedStateOf {
        if (this.textFieldValue.selection.length == 0)
            mutableString.getFutureCharStyle(this.textFieldValue.selection.start)
        else mutableString.getCharStyle(this.textFieldValue.selection.start)
    }

    var spanStyles: List<Range<RetainSpanStyle>> by mutableStateOf(initialValue.toImmutable().spanStyles)
        private set

    var textFieldValue: TextFieldValue by mutableStateOf(
        textFieldValue ?: TextFieldValue(
            text = initialValue.text,
            selection = TextRange(initialValue.text.length),
        )
    )
        private set

    var baseFontSize: TextUnit by mutableStateOf(TextUnit.Unspecified)

    val nativeAnnotatedString: AnnotatedString by derivedStateOf {
        AnnotatedString(
            text = this.textFieldValue.text,
            spanStyles = spanStyles.limit(this.textFieldValue.text.length).map { range ->
                Range(item = range.item.toNative(baseFontSize), start = range.start, end = range.end)
            },
        )
    }

    fun getAnnotatedString(): RetainAnnotatedString = mutableString.toImmutable()

    fun jumpToLast() {
        textFieldValue = textFieldValue.copy(selection = TextRange(textFieldValue.text.length))
    }

    suspend fun onTextFieldValueChange(value: TextFieldValue) {
        textFieldValue = value
        mutex.withLock {
            if (value.text != mutableString.text) {
                val wordsChanged = mutableString.applyDiff(value.text)

                updateSpanStyles()
                changeChan.send(Change(wholeWords = wordsChanged))
            }
        }
    }

    suspend fun setAnnotatedString(annotatedString: IRetainAnnotatedString) {
        mutex.withLock {
            val wordsChanged = annotatedString.text != mutableString.text

            mutableString = annotatedString.toMutable()
            textFieldValue = TextFieldValue(text = mutableString.text)

            val stylesChanged = updateSpanStyles()

            changeChan.send(Change(wholeWords = wordsChanged, style = stylesChanged))
        }
    }

    suspend fun setCurrentSelectionStyle(value: NullableRetainSpanStyle) {
        mutex.withLock {
            mutableString.setStyle(textFieldValue.selection.start, textFieldValue.selection.end, value)
            if (updateSpanStyles()) changeChan.send(Change(style = true))
        }
    }

    suspend fun splitAtSelectionStart(): Pair<RetainMutableAnnotatedString, RetainMutableAnnotatedString> {
        return mutableString.split(textFieldValue.selection.start).also { (head, _) ->
            mutex.withLock {
                val wordsChanged = mutableString.applyDiff(head.text)

                textFieldValue = textFieldValue.copy(text = mutableString.text)
                updateSpanStyles()
                changeChan.send(Change(wholeWords = wordsChanged))
            }
        }
    }

    private fun updateSpanStyles(): Boolean {
        /** Returns true if styles changed. */
        val newSpanStyles = mutableString.collapseSpanStyles()

        if (newSpanStyles != spanStyles) {
            spanStyles = newSpanStyles
            return true
        }
        return false
    }

    private fun serialize() = mutableString.toImmutable().serialize()

    companion object {
        val Saver: Saver<RetainAnnotatedStringState, *> = mapSaver(
            save = { value ->
                mapOf(
                    "serializedValue" to value.serialize(),
                    "textFieldValue" to with(TextFieldValue.Saver) { this@mapSaver.save(value.textFieldValue) },
                )
            },
            restore = { value ->
                val textFieldValue = value["textFieldValue"]?.let { with(TextFieldValue.Saver) { restore(it) } }

                RetainAnnotatedStringState(
                    initialValue = RetainAnnotatedString.deserialize(value["serializedValue"] as String),
                    textFieldValue = textFieldValue,
                )
            },
        )
    }
}

@Composable
fun rememberRetainAnnotatedStringState(json: String): RetainAnnotatedStringState {
    return rememberSaveable(json, saver = RetainAnnotatedStringState.Saver) {
        Logger.log("AnnotatedString", "rememberRetainAnnotatedStringState: json=$json")

        RetainAnnotatedStringState(initialValue = RetainAnnotatedString.deserialize(json))
    }
}
