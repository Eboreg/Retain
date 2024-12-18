package us.huseli.retain.compose.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import us.huseli.retain.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopAppBar(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(stringResource(R.string.settings))
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Sharp.ArrowBack,
                    contentDescription = stringResource(R.string.close)
                )
            }
        },
    )
}
