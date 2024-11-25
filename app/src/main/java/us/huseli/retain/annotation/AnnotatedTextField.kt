package us.huseli.retain.annotation

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import kotlinx.coroutines.launch
import us.huseli.retain.Logger
import us.huseli.retain.outlinedTextFieldColors

internal suspend fun onTextFieldValueChange(
    value: TextFieldValue,
    state: RetainAnnotatedStringState,
    callback: (RetainAnnotatedString) -> Unit,
) {
    if (state.textFieldValue2 != value) {
        Logger.log(
            "AnnotatedTextField",
            "onValueChange: " +
                "value.text=${value.text}, " +
                "textState.textFieldValue.text=${state.textFieldValue2.text}, " +
                "value.selection = ${value.selection}, " +
                "textState.textFieldValue.selection = ${state.textFieldValue2.selection}, " +
                "value.composition = ${value.composition}, " +
                "textState.textFieldValue.composition = ${state.textFieldValue2.composition}"
        )

        state.onTextFieldValueChange(value)
        callback(state.toAnnotatedString())
    }
}

internal fun getVisualTransformation(
    annotatedString: AnnotatedString,
    textState: RetainAnnotatedStringState,
    textStyle: TextStyle,
): TransformedText {
    Logger.log(
        "getVisualTransformation",
        "annotatedString.text=${annotatedString.text}, " +
            "annotatedString.spanStyles=${annotatedString.spanStyles}, " +
            "textState.spanStyles=${textState.spanStyles}",
    )

    textState.baseFontSize = textStyle.fontSize

    return TransformedText(
        // RetainAnnotatedString(annotatedString.text, textState.spanStyles).toNative(textStyle.fontSize),
        textState.nativeAnnotatedString,
        OffsetMapping.Identity,
    )
}

@Composable
fun AnnotatedTextField(
    state: RetainAnnotatedStringState,
    onValueChange: (RetainAnnotatedString) -> Unit = {},
    modifier: Modifier = Modifier,
    cursorBrush: Brush = SolidColor(Color.Black),
    enabled: Boolean = true,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    readOnly: Boolean = false,
    singleLine: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
) {
    val scope = rememberCoroutineScope()

    BasicTextField(
        value = state.textFieldValue2,
        modifier = modifier,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        enabled = enabled,
        textStyle = textStyle,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        visualTransformation = { getVisualTransformation(it, state, textStyle) },
        onValueChange = { scope.launch { onTextFieldValueChange(it, state, onValueChange) } },
        cursorBrush = cursorBrush,
    )
}

@Composable
fun OutlinedAnnotatedTextField(
    state: RetainAnnotatedStringState,
    onValueChange: (RetainAnnotatedString) -> Unit = {},
    modifier: Modifier = Modifier,
    colors: TextFieldColors = outlinedTextFieldColors(),
    enabled: Boolean = true,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    placeholder: @Composable (() -> Unit)? = null,
    readOnly: Boolean = false,
    singleLine: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
) {
    val scope = rememberCoroutineScope()

    OutlinedTextField(
        value = state.textFieldValue2,
        modifier = modifier,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        enabled = enabled,
        textStyle = textStyle,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        visualTransformation = { getVisualTransformation(it, state, textStyle) },
        onValueChange = { scope.launch { onTextFieldValueChange(it, state, onValueChange) } },
        placeholder = placeholder,
        colors = colors,
    )
}
