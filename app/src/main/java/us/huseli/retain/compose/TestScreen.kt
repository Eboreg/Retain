package us.huseli.retain.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retain.viewmodels.TestViewModel

@Composable
fun TestScreen(viewModel: TestViewModel = hiltViewModel()) {
    val testObjects by viewModel.testObjects.collectAsStateWithLifecycle()
    val undoStateIdx by viewModel.undoStateIdx.collectAsStateWithLifecycle()
    val isRedoPossible by viewModel.isRedoPossible.collectAsStateWithLifecycle()
    val isUndoPossible by viewModel.isUndoPossible.collectAsStateWithLifecycle()

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 10.dp)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            for (obj in testObjects) {
                TextField(value = obj.text, onValueChange = { obj.text = it })
            }
            Text("undoStateIdx = $undoStateIdx")
            Button(onClick = { viewModel.addObject() }) { Text("Add object") }
            Button(onClick = { viewModel.saveUndoState() }) { Text("Save") }
            Button(onClick = { viewModel.undo() }, enabled = isUndoPossible) { Text("Undo") }
            Button(onClick = { viewModel.redo() }, enabled = isRedoPossible) { Text("Redo") }

            ColorRow(MaterialTheme.colorScheme.background, "background")
            ColorRow(MaterialTheme.colorScheme.error, "error")
            ColorRow(MaterialTheme.colorScheme.errorContainer, "errorContainer")
            ColorRow(MaterialTheme.colorScheme.inverseOnSurface, "inverseOnSurface")
            ColorRow(MaterialTheme.colorScheme.inversePrimary, "inversePrimary")
            ColorRow(MaterialTheme.colorScheme.inverseSurface, "inverseSurface")
            ColorRow(MaterialTheme.colorScheme.onBackground, "onBackground")
            ColorRow(MaterialTheme.colorScheme.onError, "onError")
            ColorRow(MaterialTheme.colorScheme.onErrorContainer, "onErrorContainer")
            ColorRow(MaterialTheme.colorScheme.onPrimary, "onPrimary")
            ColorRow(MaterialTheme.colorScheme.onPrimaryContainer, "onPrimaryContainer")
            ColorRow(MaterialTheme.colorScheme.onSecondary, "onSecondary")
            ColorRow(MaterialTheme.colorScheme.onSecondaryContainer, "onSecondaryContainer")
            ColorRow(MaterialTheme.colorScheme.onSurface, "onSurface")
            ColorRow(MaterialTheme.colorScheme.onSurfaceVariant, "onSurfaceVariant")
            ColorRow(MaterialTheme.colorScheme.onTertiary, "onTertiary")
            ColorRow(MaterialTheme.colorScheme.onTertiaryContainer, "onTertiaryContainer")
            ColorRow(MaterialTheme.colorScheme.outline, "outline")
            ColorRow(MaterialTheme.colorScheme.outlineVariant, "outlineVariant")
            ColorRow(MaterialTheme.colorScheme.primary, "primary")
            ColorRow(MaterialTheme.colorScheme.primaryContainer, "primaryContainer")
            ColorRow(MaterialTheme.colorScheme.scrim, "scrim")
            ColorRow(MaterialTheme.colorScheme.secondary, "secondary")
            ColorRow(MaterialTheme.colorScheme.secondaryContainer, "secondaryContainer")
            ColorRow(MaterialTheme.colorScheme.surface, "surface")
            ColorRow(MaterialTheme.colorScheme.surfaceBright, "surfaceBright")
            ColorRow(MaterialTheme.colorScheme.surfaceContainer, "surfaceContainer")
            ColorRow(MaterialTheme.colorScheme.surfaceContainerHigh, "surfaceContainerHigh")
            ColorRow(MaterialTheme.colorScheme.surfaceContainerHighest, "surfaceContainerHighest")
            ColorRow(MaterialTheme.colorScheme.surfaceContainerLow, "surfaceContainerLow")
            ColorRow(MaterialTheme.colorScheme.surfaceContainerLowest, "surfaceContainerLowest")
            ColorRow(MaterialTheme.colorScheme.surfaceDim, "surfaceDim")
            ColorRow(MaterialTheme.colorScheme.surfaceTint, "surfaceTint")
            ColorRow(MaterialTheme.colorScheme.surfaceVariant, "surfaceVariant")
            ColorRow(MaterialTheme.colorScheme.tertiary, "tertiary")
            ColorRow(MaterialTheme.colorScheme.tertiaryContainer, "tertiaryContainer")
        }
    }
}

@Composable
fun ColorRow(color: Color, name: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(30.dp),
            content = {},
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        )
        Text(name)
    }
}
