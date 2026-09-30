package chaintech.videoplayer.youtube

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chaintech.videoplayer.host.MediaPlayerHost
import chaintech.videoplayer.model.VideoPlayerConfig
import chaintech.videoplayer.util.HtmlView
import chaintech.videoplayer.util.extractYouTubeVideoId
import com.multiplatform.webview.web.WebViewNavigator
import kotlinx.browser.document
import org.w3c.dom.HTMLIFrameElement

@Composable
actual fun DesktopYoutubeComposable(
    modifier: Modifier,
    playerHost: MediaPlayerHost,
    playerConfig: VideoPlayerConfig

) {
    Box(modifier = modifier) {
        HtmlView(
            factory = {
                val iframe = document.createElement("iframe") as HTMLIFrameElement
                iframe.width = "100%"
                iframe.height = "100%"
                iframe.style.border = "none"
                iframe.allowFullscreen = true
                iframe.setAttribute("allow", "accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share")
                iframe
            },
            update = { iframe ->
                val videoId = extractYouTubeVideoId(playerHost.url) ?: playerHost.url
                iframe.src = "https://www.youtube.com/embed/$videoId?autoplay=1&rel=0&showinfo=0"
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}


internal actual fun executeCommand(
    navigator: WebViewNavigator,
    execCommand: PlayerCommand
) {
}