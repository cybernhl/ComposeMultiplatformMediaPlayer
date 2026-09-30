@file:OptIn(ExperimentalWasmJsInterop::class)

package chaintech.videoplayer.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import chaintech.videoplayer.host.MediaPlayerError
import chaintech.videoplayer.model.PlayerSpeed
import kotlinx.browser.document
import kotlinx.coroutines.await
import org.w3c.dom.HTMLAudioElement


@Composable
internal actual fun CMPAudioPlayer(
    modifier: Modifier,
    url: String,
    isPause: Boolean,
    totalTime: (Float) -> Unit,
    currentTime: (Float) -> Unit,
    isSliding: Boolean,
    seekToTime: Float?,
    loop: Boolean,
    loadingState: (Boolean) -> Unit,
    speed: Float,
    volume: Float,
    didEndAudio: () -> Unit,
    error: (MediaPlayerError) -> Unit,
    headers: Map<String, String>?
) {
    val playerRef = remember { mutableStateOf<shaka.Player?>(null) }
    val audioElementRef = remember { mutableStateOf<HTMLAudioElement?>(null) }

    DisposableEffect(Unit) {
        val audio = document.createElement("audio") as HTMLAudioElement
        audio.style.display = "none"
        audio.setAttribute("playsinline", "")
        audio.autoplay = true
        audio.muted = true
        document.body?.appendChild(audio)
        audioElementRef.value = audio

        try {
            shaka.polyfill.installAll()
            val player = shaka.Player(audio)
            audio.addEventListener("loadedmetadata") {
                totalTime(audio.duration.toFloat())
            }
            audio.addEventListener("timeupdate") {
                if (!isSliding) {  // Don't update during seeking
                    currentTime(audio.currentTime.toFloat())
                }
            }
            audio.addEventListener("ended") {
                didEndAudio()
            }
            audio.addEventListener("waiting") {
                loadingState(true)
            }
            audio.addEventListener("playing") {
                loadingState(false)
            }
            audio.addEventListener("error") { event ->
                error(MediaPlayerError.PlaybackError("Video error: ${event.toString()}"))
            }
            playerRef.value = player
        } catch (e: Throwable) {
            error(MediaPlayerError.ResourceError("Shaka player creation failed: ${e.message}"))
        }

        onDispose {
            playerRef.value?.unload()
            playerRef.value?.destroy()
            audio.pause()
            document.body?.removeChild(audio)
        }
    }

    LaunchedEffect(url) {
        val player = playerRef.value ?: return@LaunchedEffect
        try {
            loadingState(true)
            if (!headers.isNullOrEmpty()) {
                val jsHeaders = createHeadersObject(headers)
                ShakaWasmHelpers.configureHeaders(player, jsHeaders)
            }
            player.load(url).await<JsAny?>()
        } catch (e: Throwable) {
            error(MediaPlayerError.ResourceError("Load failed: ${e.message}"))
        } finally {
            loadingState(false)
        }
    }

    LaunchedEffect(isPause, seekToTime, speed, volume, loop) {
        val audio = audioElementRef.value ?: return@LaunchedEffect

        if (isPause) {
            audio.pause()
        } else {
            try {
                audio.muted = false
                audio.play().await<JsAny?>()
            } catch (e: Throwable) {
                error(MediaPlayerError.PlaybackError("Play failed: ${e.message}"))
            }
        }

        seekToTime?.let {
            audio.currentTime = it.toDouble()
        }

        audio.playbackRate = speed.toDouble()

        audio.volume = volume.toDouble()
        audio.loop = loop
    }
}
