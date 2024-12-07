package us.huseli.retain.compose.notescreen.bars

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material.icons.sharp.FormatBold
import androidx.compose.material.icons.sharp.FormatClear
import androidx.compose.material.icons.sharp.FormatItalic
import androidx.compose.material.icons.sharp.FormatUnderlined
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import us.huseli.retain.R
import us.huseli.retain.annotation.NullableRetainSpanStyle
import us.huseli.retain.annotation.RetainAnnotatedStringState
import us.huseli.retain.annotation.RetainSpanStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextFormatTopAppBar(
    state: RetainAnnotatedStringState,
    backgroundColor: Color,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    TopAppBar(
        title = {},
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor),
        windowInsets = WindowInsets.statusBars,
        navigationIcon = {
            IconButton(onClick = onDismissRequest) {
                Icon(Icons.Sharp.Close, stringResource(R.string.exit_selection_mode))
            }
        },
        actions = {
            val textStyle = LocalTextStyle.current

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextFormatToggleButton(
                    checked = state.selectionStartStyle.size == RetainSpanStyle.Size.Small,
                    onCheckedChange = {
                        if (it) scope.launch { state.setCurrentSelectionStyle(NullableRetainSpanStyle(size = RetainSpanStyle.Size.Small)) }
                    },
                    content = { Text("Aa", style = TextStyle(fontSize = textStyle.fontSize * 0.75)) },
                )
                TextFormatToggleButton(
                    checked = state.selectionStartStyle.size == RetainSpanStyle.Size.Normal,
                    onCheckedChange = {
                        if (it) scope.launch { state.setCurrentSelectionStyle(NullableRetainSpanStyle(size = RetainSpanStyle.Size.Normal)) }
                    },
                    content = { Text("Aa") },
                )
                TextFormatToggleButton(
                    checked = state.selectionStartStyle.size == RetainSpanStyle.Size.Large,
                    onCheckedChange = {
                        if (it) scope.launch { state.setCurrentSelectionStyle(NullableRetainSpanStyle(size = RetainSpanStyle.Size.Large)) }
                    },
                    content = { Text("Aa", style = TextStyle(fontSize = textStyle.fontSize * 1.25)) },
                )

                VerticalDivider(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(40.dp)
                )

                TextFormatToggleButton(
                    checked = state.selectionStartStyle.isBold,
                    onCheckedChange = { scope.launch { state.setCurrentSelectionStyle(NullableRetainSpanStyle(isBold = it)) } },
                    content = { Icon(Icons.Sharp.FormatBold, stringResource(R.string.bold)) },
                )
                TextFormatToggleButton(
                    checked = state.selectionStartStyle.isItalic,
                    onCheckedChange = { scope.launch { state.setCurrentSelectionStyle(NullableRetainSpanStyle(isItalic = it)) } },
                    content = { Icon(Icons.Sharp.FormatItalic, stringResource(R.string.italic)) },
                )
                TextFormatToggleButton(
                    checked = state.selectionStartStyle.isUnderlined,
                    onCheckedChange = { scope.launch { state.setCurrentSelectionStyle(NullableRetainSpanStyle(isUnderlined = it)) } },
                    content = { Icon(Icons.Sharp.FormatUnderlined, stringResource(R.string.underlined)) },
                )
                IconButton(
                    onClick = {
                        scope.launch {
                            state.setCurrentSelectionStyle(
                                NullableRetainSpanStyle(isBold = false, isUnderlined = false, isItalic = false)
                            )
                        }
                    },
                    content = { Icon(Icons.Sharp.FormatClear, stringResource(R.string.clear_formatting)) },
                )
            }
        },
    )
}
