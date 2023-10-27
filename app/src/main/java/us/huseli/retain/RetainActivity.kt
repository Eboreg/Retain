package us.huseli.retain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.retain.compose.App
import us.huseli.retain.syncbackend.DropboxEngine
import javax.inject.Inject

@AndroidEntryPoint
class RetainActivity : ComponentActivity() {
    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var dropboxEngine: DropboxEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        setContent {
            App(logger)
        }
    }

    override fun onResume() {
        super.onResume()
        dropboxEngine.onResume()
    }
}
