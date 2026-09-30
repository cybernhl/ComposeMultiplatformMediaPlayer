package chaintech.videoplayer.ui.youtube

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chaintech.videoplayer.host.MediaPlayerHost
import chaintech.videoplayer.model.Platform
import chaintech.videoplayer.model.VideoPlayerConfig
import chaintech.videoplayer.util.LandscapeOrientation
import chaintech.videoplayer.util.isPlatform
import chaintech.videoplayer.youtube.DesktopYoutubeComposable

@Composable
fun YouTubePlayerComposable(
    modifier: Modifier = Modifier, // Modifier for the composable
    playerHost: MediaPlayerHost,
    playerConfig: VideoPlayerConfig = VideoPlayerConfig() // Configuration for the player
) {
    LandscapeOrientation(
        enableFullEdgeToEdge = playerConfig.enableFullEdgeToEdge,
        isLandscape = playerHost.isFullScreen
    ) {
        when(isPlatform()) {
            Platform.Ios, Platform.Android ->
                YoutubePlayerWithControl(
                    modifier = if (playerHost.isFullScreen) {
                        Modifier.fillMaxSize()
                    } else {
                        modifier
                    },
                    playerHost = playerHost,
                    playerConfig = playerConfig
                )
            Platform.Desktop, Platform.Wasm -> {
                DesktopYoutubeComposable(
                    modifier = modifier,
                    playerHost = playerHost,
                    playerConfig = playerConfig
                )
            }
        }
    }
}