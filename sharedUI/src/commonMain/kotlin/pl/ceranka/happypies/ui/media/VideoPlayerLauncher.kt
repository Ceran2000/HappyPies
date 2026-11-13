package pl.ceranka.happypies.ui.media

import androidx.compose.runtime.Composable

fun interface VideoPlayerLauncher {
    fun play(url: String)
}

@Composable
expect fun rememberVideoPlayerLauncher(): VideoPlayerLauncher