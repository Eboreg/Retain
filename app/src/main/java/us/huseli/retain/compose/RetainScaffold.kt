package us.huseli.retain.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import us.huseli.retaintheme.compose.SnackbarHosts

@Composable
fun RetainScaffold(
    modifier: Modifier = Modifier,
    statusBarColor: Color = MaterialTheme.colorScheme.surface,
    navigationBarColor: Color = MaterialTheme.colorScheme.background,
    topBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val systemUiController = rememberSystemUiController()

    LaunchedEffect(statusBarColor) {
        systemUiController.setStatusBarColor(statusBarColor)
    }

    LaunchedEffect(navigationBarColor) {
        systemUiController.setNavigationBarColor(navigationBarColor)
    }

    Scaffold(
        modifier = modifier,
        topBar = topBar,
        snackbarHost = { SnackbarHosts(modifier = Modifier.zIndex(2f)) },
        content = content,
    )
}
