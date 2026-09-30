package chaintech.videoplayer.util

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import chaintech.videoplayer.host.MediaPlayerError
import idv.neo.ffmpeg.media.player.core.JavaCvPlayer
import idv.neo.ffmpeg.media.player.core.audio.JvmAudioSink
import idv.neo.ffmpeg.media.player.core.video.skia.SkiaVideoSink
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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
    val player = remember {
        val videoSink = SkiaVideoSink()
        val audioSink = JvmAudioSink()
        JavaCvPlayer.Builder()
            .setVideoSink(videoSink)
            .setAudioSink(audioSink)
            .build()
    }

    LaunchedEffect(url) {
        if (url.isNotBlank()) {
            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .build()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }

    LaunchedEffect(isPause) {
        player.playWhenReady = !isPause
    }

    LaunchedEffect(volume) {
        player.volume = volume
    }

    LaunchedEffect(speed) {
        player.playbackParameters = PlaybackParameters(speed)
    }

    LaunchedEffect(seekToTime) {
        seekToTime?.let {
            player.seekTo((it * 1000).toLong())
        }
    }

    LaunchedEffect(loop) {
        player.repeatMode = if (loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
    }

    // Sync state back to lambdas
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                loadingState(playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_ENDED) {
                    didEndAudio()
                }
            }

            override fun onEvents(player: Player, events: Player.Events) {
                totalTime(player.duration.toFloat() / 1000f)
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (isActive) {
            if (player.isPlaying) {
                currentTime(player.currentPosition.toFloat() / 1000f)
            }
            delay(500)
        }
    }
}
