package chaintech.videoplayer.youtube

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import java.awt.BorderLayout
import javax.swing.JPanel

private val fxInitLock = Any()
private var fxInitialized = false

private fun ensureJavaFx() {
    synchronized(fxInitLock) {
        if (!fxInitialized) {
            JFXPanel() // <-- THIS initializes JavaFX safely
            Platform.setImplicitExit(false)
            fxInitialized = true
        }
    }
}

@Suppress("SetJavaScriptEnabled")
@Composable
internal fun DesktopEmbeddedPlayer(
    modifier: Modifier = Modifier,
    host: VideoPlayerHost
) {
    val jPanel = remember { JPanel() }
    val jfxPanel = remember { JFXPanel() }
    val webViewState = remember { mutableStateOf<WebView?>(null) }

    val command = host.pendingCommand

    LaunchedEffect(Unit) {
        ensureJavaFx()

        Platform.runLater {
            val webView = WebView().apply {
                prefWidth = 1920.0
                prefHeight = 1080.0
                style = "-fx-background-color: black;"
            }
            webView.engine.apply {
                isJavaScriptEnabled = true
                userAgent =
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome Safari"

                titleProperty().addListener { _, _, newTitle ->
                    PlayerEventParser.parse(newTitle)?.let {
                        host.update(it)
                    }
                }

                load("https://Chetansinh-r.github.io/ytproxyDesktop/player.html")
            }
            val scene = Scene(webView).apply {
                fill = javafx.scene.paint.Color.BLACK
            }

            webView.prefWidthProperty().bind(scene.widthProperty())
            webView.prefHeightProperty().bind(scene.heightProperty())

            jfxPanel.scene = scene
            webViewState.value = webView
        }
    }


    LaunchedEffect(command) {
        command?.let {
            Platform.runLater {
                webViewState.value
                    ?.engine
                    ?.executeScript(it.toJsCommand())
            }
            host.complete()
        }
    }

    SwingPanel(
        modifier = modifier,
        factory = {
            jPanel.apply {
                layout = BorderLayout()
                add(jfxPanel, BorderLayout.CENTER)
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            Platform.runLater {
                webViewState.value?.engine?.load("")
                webViewState.value = null
                jfxPanel.scene = null
                jPanel.remove(jfxPanel)
            }
        }
    }
}