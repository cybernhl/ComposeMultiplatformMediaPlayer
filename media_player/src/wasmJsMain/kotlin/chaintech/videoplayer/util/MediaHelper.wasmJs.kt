package chaintech.videoplayer.util

import androidx.compose.runtime.Composable
import kotlinx.browser.document
import org.w3c.dom.HTMLAudioElement
import org.w3c.dom.HTMLVideoElement

@Composable
internal actual fun FetchTotalDuration(
    url: String,
    onDurationRetrieved: (Double) -> Unit
) {
    val supportedAudioExtensions = setOf(
        "mp3", "flac", "wav", "aac", "aif", "alac", "ogg", "opus", "m4a"
    )
    val element = if (supportedAudioExtensions.any { url.lowercase().endsWith(".$it") }) {
        document.createElement("audio") as HTMLAudioElement
    } else {
        document.createElement("video") as HTMLVideoElement
    }

    element.preload = "metadata"
    element.src = url
    element.style.display = "none"

    fun cleanup() {
        element.pause()
        element.src = ""
        element.removeAttribute("src")
        element.remove()
    }

    val onLoadedMetadata = {
        onDurationRetrieved(element.duration)
        cleanup()
    }

    val onError = {
        onDurationRetrieved(0.0)
        cleanup()
    }

    element.addEventListener("loadedmetadata") { onLoadedMetadata() }
    element.addEventListener("error") { onError() }

    // Append temporarily to body so it starts loading metadata
    document.body?.appendChild(element)
}