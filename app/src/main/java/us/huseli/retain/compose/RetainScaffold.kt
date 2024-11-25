package us.huseli.retain.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.preference.PreferenceManager
import us.huseli.retain.Constants.PREF_SYSTEM_BAR_COLOR_KEY
import us.huseli.retain.Logger
import us.huseli.retain.ui.theme.NoteColorKey
import us.huseli.retaintheme.compose.SnackbarHosts

@Composable
fun RetainScaffold(
    modifier: Modifier = Modifier,
    systemBarColorKey: NoteColorKey = NoteColorKey.DEFAULT,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(systemBarColorKey) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)

        Logger.log("RetainScaffold", "colorKey=$systemBarColorKey")
        preferences.edit().putString(PREF_SYSTEM_BAR_COLOR_KEY, systemBarColorKey.name).apply()
    }

    Scaffold(
        modifier = modifier,
        topBar = topBar,
        snackbarHost = { SnackbarHosts(modifier = Modifier.zIndex(2f)) },
        content = content,
        bottomBar = bottomBar,
    )
}
