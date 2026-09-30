package chaintech.videoplayer.ui.video

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chaintech.videoplayer.host.MediaPlayerHost
import chaintech.videoplayer.model.Platform
import chaintech.videoplayer.model.VideoPlayerConfig
import chaintech.videoplayer.ui.youtube.YouTubePlayerComposable
import chaintech.videoplayer.util.LandscapeOrientation
import chaintech.videoplayer.util.extractYouTubeVideoId
import chaintech.videoplayer.util.isPlatform

@Composable
fun VideoPlayerComposable(
    modifier: Modifier = Modifier, // Modifier for the composable
    playerHost: MediaPlayerHost,
    playerConfig: VideoPlayerConfig = VideoPlayerConfig() // Configuration for the player
) {
    val videoUrl = extractYouTubeVideoId(playerHost.url)
    if (videoUrl == null) {
        LandscapeOrientation(
            enableFullEdgeToEdge = playerConfig.enableFullEdgeToEdge,
            isLandscape = playerHost.isFullScreen
        ) {
            when(isPlatform()) {
                Platform.Ios, Platform.Android ->
                    VideoPlayerWithControl(
                        modifier = if (playerHost.isFullScreen) {
                            Modifier.fillMaxSize()
                        } else {
                            modifier
                        },
                        playerHost = playerHost,
                        playerConfig = playerConfig
                    )
                Platform.Desktop ->
                    DesktopVideoPlayer(
                        modifier = if (playerHost.isFullScreen) {
                            Modifier.fillMaxSize()
                        } else {
                            modifier
                        },
                        playerHost = playerHost,
                        playerConfig = playerConfig
                    )
                Platform.Wasm ->
                    WasmJSVideoPlayer(
                        modifier = if (playerHost.isFullScreen) {
                            Modifier.fillMaxSize()
                        } else {
                            modifier
                        },
                        playerHost = playerHost,
                        playerConfig = playerConfig
                    )
            }
        }
    } else {
        YouTubePlayerComposable(
            modifier = modifier,
            playerHost = playerHost,
            playerConfig = playerConfig
        )
    }
}








