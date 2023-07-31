package us.huseli.retain.compose.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retain.Constants.PREF_MIN_COLUMN_WIDTH
import us.huseli.retain.R
import us.huseli.retain.viewmodels.SettingsViewModel
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun GeneralSection(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel,
) {
    val minColumnWidth by viewModel.minColumnWidth.collectAsStateWithLifecycle()
    var minColumnWidthSliderPos by remember { mutableStateOf(minColumnWidth) }
    val maxScreenWidth = max(LocalConfiguration.current.screenHeightDp, LocalConfiguration.current.screenWidthDp)
    val maxColumnWidth = (maxScreenWidth - 24) / 10 * 10
    val columnWidthSteps = (maxColumnWidth - 51) / 10

    BaseSettingsSection(
        modifier = modifier,
        key = "general",
        viewModel = viewModel,
        title = stringResource(R.string.general)
    ) {
        Text(
            text = stringResource(R.string.minimum_column_width_on_home_screen),
            style = MaterialTheme.typography.bodySmall,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                modifier = Modifier.weight(0.9f),
                value = minColumnWidthSliderPos.toFloat(),
                steps = columnWidthSteps,
                onValueChange = { minColumnWidthSliderPos = it.roundToInt() },
                onValueChangeFinished = { viewModel.updateField(PREF_MIN_COLUMN_WIDTH, minColumnWidthSliderPos) },
                valueRange = 50f..maxColumnWidth.toFloat(),
            )
            Column(modifier = Modifier.fillMaxWidth(0.1f)) {
                Text(
                    text = minColumnWidthSliderPos.toString(),
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}