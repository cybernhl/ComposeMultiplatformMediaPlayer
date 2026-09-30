package chaintech.videoplayer.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import chaintech.videoplayer.model.Platform
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.w3c.dom.HTMLElement

@Suppress("DefaultLocale")
actual fun formatMinSec(value: Float): String {
    if (value == 0f) return "00:00"

    val hours = (value / 3600).toInt()
    val minutes = ((value % 3600) / 60).toInt()
    val seconds = (value % 60).toInt()

    fun pad(number: Int): String = if (number < 10) "0$number" else number.toString()

    return if (hours > 0) {
        "${pad(hours)}:${pad(minutes)}:${pad(seconds)}"
    } else {
        "${pad(minutes)}:${pad(seconds)}"
    }
}


internal actual val youtubeProgressColor: Color
    get() = Color(0xFFdddddd)

@Composable
internal actual fun LandscapeOrientation(
    enableFullEdgeToEdge: Boolean,
    isLandscape: Boolean,
    content: @Composable () -> Unit
) {
    content()
    val rootElement = document.getElementsByTagName("html").item(0) as? HTMLElement
    rootElement?.let {
        LaunchedEffect(isLandscape) {
            if (isLandscape) {
                ShakaWasmHelpers.requestFullscreen(rootElement)
            } else {
                ShakaWasmHelpers.exitFullscreen()
            }
        }
    }
}

actual fun isPlatform(): Platform {
    return Platform.Wasm
}

actual fun provideDispatcher(): CoroutineDispatcher = Dispatchers.Default
