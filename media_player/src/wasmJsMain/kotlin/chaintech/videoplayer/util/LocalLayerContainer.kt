package chaintech.videoplayer.util

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.browser.document
import org.w3c.dom.Element

val LocalLayerContainer = staticCompositionLocalOf<Element> {
    document.body ?: error("Document body unavailable")
}

