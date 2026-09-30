package chaintech.videoplayer.youtube

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chaintech.videoplayer.host.MediaPlayerHost
import chaintech.videoplayer.model.VideoPlayerConfig
import com.multiplatform.webview.web.WebViewNavigator

private const val COMMAND_EXECUTOR_PATTERN = "javascript:%s"

internal actual fun executeCommand(
    navigator: WebViewNavigator,
    execCommand: PlayerCommand
) {
    navigator.loadUrl(COMMAND_EXECUTOR_PATTERN.format(execCommand.toJsCommand()))
}

@Composable
actual fun DesktopYoutubeComposable(
    modifier: Modifier,
    playerHost: MediaPlayerHost,
    playerConfig: VideoPlayerConfig

) {
}