package chaintech.videoplayer.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import chaintech.videoplayer.host.MediaPlayerError
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent

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
    headers: Map<String, String>?,
) {
    if(!NativeDiscovery().discover()) {
        error(MediaPlayerError.VlcNotFound)
        return
    }
    var repeatStatus by remember { mutableStateOf(loop) }
    val mediaPlayerComponent = remember {
        try {
            AudioPlayerComponent()
        } catch (e: Exception) {
            error(MediaPlayerError.InitializationError("Failed to initialize VLC MediaPlayerComponent: ${e.message}"))
            return@remember null
        }
    } ?: return

    val mediaPlayer = remember {
        try {
            mediaPlayerComponent.mediaPlayer()
        } catch (e: Exception) {
            error(MediaPlayerError.InitializationError("Failed to create VLC media player instance: ${e.message}"))
            return@remember null
        }
    } ?: return

    var time by remember { mutableStateOf(0f) }

    val listener = remember {
        object : MediaPlayerEventAdapter() {
            override fun finished(mediaPlayer: MediaPlayer) {
                currentTime(0f)
                mediaPlayer.submit {
                    mediaPlayer.controls().play()
                    if(!repeatStatus) {
                        mediaPlayer.controls().pause()
                    }
                    didEndAudio()
                }
            }

            override fun buffering(mediaPlayer: MediaPlayer?, newCache: Float) {
                loadingState(newCache != 100f)
                super.buffering(mediaPlayer, newCache)
            }

            override fun lengthChanged(mediaPlayer: MediaPlayer?, newLength: Long) {
                time = (newLength / 1000L).toFloat()
                totalTime(time)
                super.lengthChanged(mediaPlayer, newLength)
            }

            override fun timeChanged(mediaPlayer: MediaPlayer?, newTime: Long) {
                currentTime((newTime / 1000L).toFloat())
                super.timeChanged(mediaPlayer, newTime)
            }
        }
    }

    // Add event listener only once
    LaunchedEffect(Unit) {
        try {
            mediaPlayer.events().addMediaPlayerEventListener(listener)
        } catch (e: Exception) {
            error(MediaPlayerError.ResourceError("Failed to add media player event listener: ${e.message}"))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer.events().removeMediaPlayerEventListener(listener)
                mediaPlayer.release()
            } catch (e: Exception) {
                error(MediaPlayerError.ResourceError("Error while releasing media player: ${e.message}"))
            }
        }
    }

    LaunchedEffect(speed) {
        try {
            mediaPlayer.controls().setRate(speed)
        } catch (e: Exception) {
            error(MediaPlayerError.PlaybackError("Failed to set playback speed: ${e.message}"))
        }
    }
    // Update repeat status when it changes
    LaunchedEffect(loop) {
        repeatStatus = loop
    }
    LaunchedEffect(volume) {
        try {
            mediaPlayer.audio().setVolume((volume * 100).toInt())
        } catch (e: Exception) {
            error(MediaPlayerError.PlaybackError("Failed to change volume: ${e.message}"))
        }
    }
    LaunchedEffect(seekToTime) {
        try {
            seekToTime?.let {
                val per = (it * 100 / time) / 100
                mediaPlayer.controls().setPosition(per)
            }
        } catch (e: Exception) {
            error(MediaPlayerError.PlaybackError("Failed to seek to position: ${e.message}"))
        }
    }
    LaunchedEffect(url) {
        try {
            val headerOptions = headers?.flatMap { (key, value) ->
                when (key.lowercase()) {
                    "user-agent" -> listOf(":http-user-agent=$value")
                    "referer" -> listOf(":http-referrer=$value")
                    else -> emptyList()
                }
            }?.toMutableList() ?: mutableListOf()

            val customHeaders = headers
                ?.filterKeys { it.lowercase() !in listOf("user-agent", "referer") }
                ?.map { "${it.key}: ${it.value}" }
                ?.joinToString(", ")

            if (!customHeaders.isNullOrEmpty()) {
                headerOptions.add(":http-header-fields=$customHeaders")
            }
            val options = headerOptions.toTypedArray()

            mediaPlayer.media().play(url, *options)
        } catch (e: Exception) {
            error(MediaPlayerError.PlaybackError("Failed to play media: ${e.message}"))
        }
    }

    LaunchedEffect(isPause) {
        try {
            mediaPlayer.controls().setPause(isPause)
        } catch (e: Exception) {
            error(MediaPlayerError.PlaybackError("Failed to pause/resume playback: ${e.message}"))
        }
    }
}