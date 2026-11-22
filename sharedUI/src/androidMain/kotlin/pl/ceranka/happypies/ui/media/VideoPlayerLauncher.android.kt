package pl.ceranka.happypies.ui.media

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
actual fun rememberVideoPlayerLauncher(): VideoPlayerLauncher {
    var playingUrl by rememberSaveable { mutableStateOf<String?>(null) }

    playingUrl?.let { url ->
        FullScreenVideoDialog(url, onDismiss = { playingUrl = null })
    }

    return remember { VideoPlayerLauncher { playingUrl = it } }
}

@Composable
private fun FullScreenVideoDialog(url: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        HideSystemBars()
        VideoPlayer(url, Modifier.fillMaxSize().background(Color.Black))
    }
}

@Composable
private fun HideSystemBars() {
    val window = (LocalView.current.parent as DialogWindowProvider).window
    DisposableEffect(window) {
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {}
    }
}

@Composable
private fun VideoPlayer(url: String, modifier: Modifier) {
    val context = LocalContext.current
    var positionMs by rememberSaveable { mutableLongStateOf(0L) }
    var playWhenReady by rememberSaveable { mutableStateOf(true) }

    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url), positionMs)
            prepare()
        }
    }

    // ON_PAUSE comes before the state is saved, so the position survives a rotation.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        positionMs = player.currentPosition
        playWhenReady = player.playWhenReady
        player.pause()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        player.playWhenReady = playWhenReady
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }

    AndroidView(
        factory = { PlayerView(it).apply { this.player = player; keepScreenOn = true } },
        modifier = modifier
    )
}