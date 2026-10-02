@file:OptIn(ExperimentalWasmJsInterop::class)

package chaintech.videoplayer.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import chaintech.videoplayer.host.DrmConfig
import chaintech.videoplayer.host.MediaPlayerError
import chaintech.videoplayer.model.MediaEngineConfig
import chaintech.videoplayer.model.PlayerSpeed
import chaintech.videoplayer.model.ScreenResize
import kotlinx.browser.document
import kotlinx.coroutines.await
import org.w3c.dom.HTMLVideoElement

@Composable
internal actual fun CMPPlayer(
    modifier: Modifier,
    url: String,
    isPause: Boolean,
    totalTime: (Float) -> Unit,
    currentTime: (Float) -> Unit,
    isSliding: Boolean,
    seekToTime: Float?,
    speed: Float,
    size: ScreenResize,
    bufferCallback: (Boolean) -> Unit,
    didEndVideo: () -> Unit,
    loop: Boolean,
    volume: Float,
    isLiveStream: Boolean,
    isPipMode: Boolean,
    onPipModeChanged: (Boolean) -> Unit,
    error: (MediaPlayerError) -> Unit,
    headers: Map<String, String>?,
    drmConfig: DrmConfig?,
    selectedQuality: VideoQuality?,
    selectedAudioTrack: AudioTrack?,
    selectedSubTitle: SubtitleTrack?,
    audioList: ((List<AudioTrack>) -> Unit),
    subtitlesList: ((List<SubtitleTrack>) -> Unit),
    qualityList: ((List<VideoQuality>) -> Unit),
    engineConfig: MediaEngineConfig
) {
    val videoElement = remember { mutableStateOf<HTMLVideoElement?>(null) }
    val playerRef = remember { mutableStateOf<shaka.Player?>(null) }

    Box(modifier = modifier) {
        HtmlView(
            factory = { createVideoElement() },
            modifier = Modifier.fillMaxSize(),
            update = { video ->
                videoElement.value = video

                // Initialize player only once
                if (playerRef.value == null) {
                    try {
                        shaka.polyfill.installAll()
                        val player = shaka.Player(video)

                        player.addEventListener("error") { errorEvent ->
                            val shakaError = errorEvent.detail
                            error(MediaPlayerError.PlaybackError("Shaka error: ${shakaError.message}"))
                        }

                        // Video events
                        video.addEventListener("timeupdate") {
                            if (!isSliding) currentTime(video.currentTime.toFloat())
                        }

                        video.addEventListener("loadedmetadata") {
                            totalTime(video.duration.toFloat())
                        }

                        video.addEventListener("ended") {
                            didEndVideo()
                        }
                        video.addEventListener("waiting") {
                            bufferCallback(true)
                        }
                        video.addEventListener("playing") {
                            bufferCallback(false)
                        }
                        video.addEventListener("error") { event ->
                            error(MediaPlayerError.PlaybackError("Video error: ${event.toString()}"))
                        }

                        playerRef.value = player
                    } catch (e: Throwable) {
                        error(MediaPlayerError.ResourceError("Player creation failed: ${e.message}"))
                    }
                }
            }
        )
        LaunchedEffect(url) {
            val player = playerRef.value ?: return@LaunchedEffect
            try {
                bufferCallback(true)

                // Configure headers
                headers?.takeIf { it.isNotEmpty() }?.let {
                    val jsHeaders = createHeadersObject(it)
                    ShakaWasmHelpers.configureHeaders(player, jsHeaders)
                }

                // DRM
                drmConfig?.let { config ->
                    ShakaWasmHelpers.setClearKey(player, config.keyId, config.key)
                }
                player.load(url).await<JsAny?>()
            } catch (e: Throwable) {
                error(MediaPlayerError.ResourceError("Load failed: ${e.message}"))
            } finally {
                bufferCallback(false)
            }
        }


        // Play/pause, seek, loop, volume, speed updates
        LaunchedEffect(isPause, seekToTime, speed, volume, loop, size) {
            val video = videoElement.value ?: return@LaunchedEffect

            if (isPause) video.pause()
            else {
                try {
                    video.muted = false
                    video.play().await<JsAny?>()
                } catch (e: Throwable) {
                    error(MediaPlayerError.PlaybackError("Play failed: ${e.message}"))
                }
            }

            seekToTime?.let { video.currentTime = it.toDouble() }
            video.playbackRate = speed.toDouble()
            video.volume = volume.toDouble()
            video.loop = loop

            when (size) {
                ScreenResize.FIT -> video.style.objectFit = "contain"
                ScreenResize.FILL -> video.style.objectFit = "cover"
            }
        }

        // Subtitles
        LaunchedEffect(selectedSubTitle) {
            val player = playerRef.value ?: return@LaunchedEffect
            if (selectedSubTitle != null) {
                player.setTextTrackVisibility(true)
                val tracks = player.getTextTracks()
                for (i in 0 until tracks.length) {
                    tracks[i]?.let {
                        if (it.language == selectedSubTitle.language) {
                            player.selectTextLanguage(it.language)
                            break
                        }
                    }
                }
            } else {
                player.setTextTrackVisibility(false)
            }
        }

        // Quality selection
        LaunchedEffect(selectedQuality) {
            val player = playerRef.value ?: return@LaunchedEffect
            if (selectedQuality != null) {
                val tracks = player.getVariantTracks()
                for (i in 0 until tracks.length) {
                   tracks[i]?.let {
                        if (it.bandwidth == selectedQuality.bitrate.toInt()) {
                            ShakaWasmHelpers.setABR(player, false)
                            player.selectVariantTrack(it, true, false)
                            break
                        }
                    }
                }
            } else {
                ShakaWasmHelpers.setABR(player, true)
            }
        }

        // Audio track selection
        LaunchedEffect(selectedAudioTrack) {
            val player = playerRef.value ?: return@LaunchedEffect
            if (selectedAudioTrack != null) {
                val tracks = player.getAudioLanguagesAndRoles()
                for (i in 0 until tracks.length) {
                    tracks[i]?.let { it
                        if (it.language == selectedAudioTrack.language) {
                            player.selectAudioLanguage(it.language)
                            break
                        }
                    }
                }
            }
        }
    }

    // Dispose
    DisposableEffect(Unit) {
        onDispose {
            playerRef.value?.runCatching {
                unload()
                destroy()
            }
            videoElement.value?.run {
                pause()
                src = ""
                removeAttribute("src")
                load()
            }
            playerRef.value = null
            videoElement.value = null
        }
    }
}

// Create the video element with basic attributes
private fun createVideoElement(): HTMLVideoElement {
    return (document.createElement("video") as HTMLVideoElement).apply {
        controls = false
        autoplay = true
        muted = true
        setAttribute("playsinline", "")
        style.width = "100%"
        style.height = "100%"
        style.objectFit = "contain"
        style.setProperty("pointer-events", "none")
    }
}