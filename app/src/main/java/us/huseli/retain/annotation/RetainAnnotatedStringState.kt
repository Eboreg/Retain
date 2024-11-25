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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import us.huseli.retain.Logger
import us.huseli.retain.interfaces.ILogger
import us.huseli.retain.limit

@Stable
class RetainAnnotatedStringState(
    initialValue: IRetainAnnotatedString,
    // selection: TextRange? = null,
    textFieldValue: TextFieldValue? = null,
) : ILogger {
    // private var composition: TextRange? by mutableStateOf(composition)
    // private var text: String by mutableStateOf(initialValue.text)
    private val mutex = Mutex()

    private var mutableString: RetainMutableAnnotatedString by mutableStateOf(initialValue.toMutable())

    // var selection: TextRange by mutableStateOf(selection ?: TextRange(initialValue.text.length))

    val selectionStartStyle: RetainSpanStyle by derivedStateOf {
        // if (this.selection.length == 0) mutableString.getFutureCharStyle(this.selection.start)
        // else mutableString.getCharStyle(this.selection.start)
        if (textFieldValue2.selection.length == 0) mutableString.getFutureCharStyle(textFieldValue2.selection.start)
        else mutableString.getCharStyle(textFieldValue2.selection.start)
    }

    var spanStyles: List<Range<RetainSpanStyle>> by mutableStateOf(initialValue.toImmutable().spanStyles)
        private set

    // val textFieldValue: TextFieldValue by derivedStateOf {
    //     TextFieldValue(text = text, selection = this.selection, composition = this.composition)
    // }

    var textFieldValue2: TextFieldValue by mutableStateOf(
        textFieldValue ?: TextFieldValue(
            text = initialValue.text,
            selection = TextRange(initialValue.text.length),
        )
    )
        private set

    var baseFontSize: TextUnit by mutableStateOf(TextUnit.Unspecified)

    val nativeAnnotatedString: AnnotatedString by derivedStateOf {
        AnnotatedString(
            text = textFieldValue2.text,
            spanStyles = spanStyles.limit(textFieldValue2.text.length).map { range ->
                Range(item = range.item.toNative(baseFontSize), start = range.start, end = range.end)
            },
        )
    }

    fun jumpToLast() {
        // selection = TextRange(text.length)
        textFieldValue2 = textFieldValue2.copy(selection = TextRange(textFieldValue2.text.length))
    }

    suspend fun onTextFieldValueChange(value: TextFieldValue) {
        textFieldValue2 = value
        mutex.withLock {
            if (value.text != mutableString.text) {
                mutableString.applyDiff(value.text)
                updateSpanStyles()
            }
            // if (value.selection != selection) selection = value.selection
            // if (value.composition != composition) composition = value.composition
        }
    }

    suspend fun setStyle(value: NullableRetainSpanStyle) {
        mutex.withLock {
            // mutableString.setStyle(selection.start, selection.end, value)
            mutableString.setStyle(textFieldValue2.selection.start, textFieldValue2.selection.end, value)
            updateSpanStyles()
        }
    }

    suspend fun splitAtSelectionStart(): Pair<RetainMutableAnnotatedString, RetainMutableAnnotatedString> {
        // return toAnnotatedString().split(selection.start).also { (head, _) ->
        return mutableString.split(textFieldValue2.selection.start).also { (head, _) ->
            mutex.withLock {
                mutableString = head
                textFieldValue2 = textFieldValue2.copy(text = mutableString.text)
                updateSpanStyles()
            }
        }
    }

    fun toAnnotatedString(): RetainAnnotatedString = mutableString.toImmutable()

    suspend fun update(annotatedString: IRetainAnnotatedString) {
        mutex.withLock {
            mutableString = annotatedString.toMutable()
            textFieldValue2 = TextFieldValue(text = mutableString.text)
            updateSpanStyles()
        }
    }

    private fun updateSpanStyles() {
        // val annotatedString = mutableString.toImmutable()

        // text = mutableString.text
        spanStyles = mutableString.collapseSpanStyles()
    }

    private fun serialize() = mutableString.toImmutable().serialize()

    companion object {
        val Saver: Saver<RetainAnnotatedStringState, *> = mapSaver(
            save = { value ->
                mapOf(
                    "serializedValue" to value.serialize(),
                    "textFieldValue" to with(TextFieldValue.Saver) { this@mapSaver.save(value.textFieldValue2) },
                )
            },
            restore = { value ->
                /*
                val selectionStart = value["selectionStart"] as? Int
                val selectionEnd = value["selectionEnd"] as? Int

                val selection = if (selectionStart != null && selectionEnd != null)
                    TextRange(selectionStart, selectionEnd)
                else null
                 */
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
    return rememberSaveable(saver = RetainAnnotatedStringState.Saver) {
        Logger.log("AnnotatedString", "rememberRetainAnnotatedStringState: json=$json")

        RetainAnnotatedStringState(initialValue = RetainAnnotatedString.deserialize(json))
    }
}
