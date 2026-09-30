package chaintech.videoplayer.youtube

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import chaintech.videoplayer.host.MediaPlayerHost
import chaintech.videoplayer.model.VideoPlayerConfig
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState

@Composable
internal fun EmbeddedPlayer(
    modifier: Modifier = Modifier,
    host: VideoPlayerHost,
    onEvent: ((PlayerEvent) -> Unit)? = null,
) {
    val webViewState = rememberWebViewState(
        url = "https://Chetansinh-r.github.io/ytproxy/player.html"
    )

    val navigator = rememberWebViewNavigator()
    val command = host.pendingCommand

    LaunchedEffect(Unit) {
        navigator.reload()
    }

    webViewState.webSettings.apply {
        isJavaScriptEnabled = true
        androidWebSettings.apply {
            isAlgorithmicDarkeningAllowed = true
            safeBrowsingEnabled = false
            domStorageEnabled = true
            supportZoom = false
            hideDefaultVideoPoster = true
            mediaPlaybackRequiresUserGesture = false
        }
        iOSWebSettings.apply {
            backgroundColor = Color.Black
            scrollEnabled = false
            bounces = false
            showHorizontalScrollIndicator = false
            showVerticalScrollIndicator = false
        }
    }

    LaunchedEffect(command) {
        command?.let {
            executeCommand(navigator, it)
            host.complete()
        }
    }

    PlayerEventParser.parse(webViewState.pageTitle)?.let { event ->
        host.update(event)
        onEvent?.invoke(event)
    }

    WebView(
        modifier = modifier.fillMaxSize(),
        state = webViewState,
        navigator = navigator,
    )
}

internal expect fun executeCommand(
    navigator: WebViewNavigator,
    execCommand: PlayerCommand,
)

@Composable
expect fun DesktopYoutubeComposable(
    modifier: Modifier,
    playerHost: MediaPlayerHost,
    playerConfig: VideoPlayerConfig
)