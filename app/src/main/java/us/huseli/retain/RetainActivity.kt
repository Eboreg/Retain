package us.huseli.retain

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.retain.compose.App
import us.huseli.retain.syncbackend.DropboxEngine
import us.huseli.retaintheme.ui.theme.RetainTheme
import javax.inject.Inject

@AndroidEntryPoint
class RetainActivity : ComponentActivity() {
    @Inject
    lateinit var dropboxEngine: DropboxEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            RetainTheme {
                App()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        dropboxEngine.onResume()
    }
}
