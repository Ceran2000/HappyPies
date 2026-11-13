package pl.ceranka.happypies.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.play
import platform.AVKit.AVPlayerViewController
import platform.Foundation.NSURL
import platform.UIKit.UIViewController

@Composable
actual fun rememberVideoPlayerLauncher(): VideoPlayerLauncher {
    val viewController = LocalUIViewController.current
    return remember(viewController) { VideoPlayerLauncher { url -> viewController.presentVideoPlayer(url) } }
}

@OptIn(ExperimentalForeignApi::class)
private fun UIViewController.presentVideoPlayer(url: String) {
    val player = AVPlayer(uRL = requireNotNull(NSURL.URLWithString(url)) { "Invalid video URL" })
    // Without the playback category the silent switch mutes the video.
    AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryPlayback, null)
    AVAudioSession.sharedInstance().setActive(true, null)
    val playerController = AVPlayerViewController().apply { this.player = player }
    presentViewController(playerController, animated = true) { player.play() }
}