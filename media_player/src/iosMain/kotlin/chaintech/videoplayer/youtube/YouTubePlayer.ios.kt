package chaintech.videoplayer.youtube

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chaintech.videoplayer.host.MediaPlayerHost
import chaintech.videoplayer.model.VideoPlayerConfig
import com.multiplatform.webview.web.WebViewNavigator

internal actual fun executeCommand(
    navigator: WebViewNavigator,
    execCommand: PlayerCommand
) {
    navigator.evaluateJavaScript(execCommand.toJsCommand())
}

@Composable
actual fun DesktopYoutubeComposable(
    modifier: Modifier,
    playerHost: MediaPlayerHost,
    playerConfig: VideoPlayerConfig

) {
}