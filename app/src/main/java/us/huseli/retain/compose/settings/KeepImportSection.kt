package us.huseli.retain.compose.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.huseli.retain.R
import us.huseli.retain.viewmodels.SettingsViewModel

@Composable
fun KeepImportSection(modifier: Modifier = Modifier, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val zipFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.keepImport(uri, context)
    }

    BaseSettingsSection(
        modifier = modifier,
        key = "keepImport",
        viewModel = viewModel,
        isShownDefault = false,
        title = stringResource(R.string.google_keep_import)
    ) {
        Text(
            text = stringResource(R.string.google_keep_import_about),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedButton(
            onClick = { zipFilePicker.launch(arrayOf("application/zip")) },
            shape = ShapeDefaults.ExtraSmall,
        ) {
            Text(stringResource(R.string.select_zip_file))
        }
    }
}