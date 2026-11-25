package pl.ceranka.happypies.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pl.ceranka.happypies.App
import pl.ceranka.happypies.BuildConfig
import pl.ceranka.happypies.infrastructure.app_links.AppLinks

class MainActivity : ComponentActivity(), KoinComponent {

    private val appLinks: AppLinks by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            openLink(intent)
        }

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openLink(intent)
    }

    private fun openLink(intent: Intent) {
        val uri = intent.data ?: return
        val path = uri.path ?: return
        if (intent.action == Intent.ACTION_VIEW && isAppLink(uri)) {
            appLinks.open(path)
        }
    }

    private fun isAppLink(uri: Uri): Boolean = uri.scheme == "https" && uri.host == BuildConfig.APP_LINK_HOST
}