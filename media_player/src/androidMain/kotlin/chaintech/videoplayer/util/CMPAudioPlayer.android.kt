package chaintech.videoplayer.util

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import chaintech.videoplayer.host.MediaPlayerError
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.concurrent.TimeUnit

@Composable
actual fun CMPAudioPlayer(
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
    val context = LocalContext.current
    val exoPlayer = rememberExoPlayer(url, context, error, headers)

    // Update repeat mode based on isRepeat state
    LaunchedEffect(loop) {
        exoPlayer.repeatMode = if (loop) {
            Player.REPEAT_MODE_ONE
        } else {
            Player.REPEAT_MODE_OFF
        }
    }

    // Update current time every second
    LaunchedEffect(exoPlayer) {
        while (isActive) {
            currentTime(
                TimeUnit.MILLISECONDS.toSeconds(exoPlayer.currentPosition).coerceAtLeast(0L).toFloat()
            )
            delay(1000)
        }
    }

    // Manage player listener and lifecycle
    DisposableEffect(key1 = exoPlayer) {
        val listener = createPlayerListener(
            totalTime,
            currentTime,
            loadingState,
            didEndAudio,
            isSliding,
            error
        )

        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Control playback based on isPause state
    LaunchedEffect(isPause) {
        exoPlayer.playWhenReady = !isPause
    }

    LaunchedEffect(volume) {
        exoPlayer.volume = volume
    }

    // Seek to slider time if provided
    seekToTime?.let { time ->
        LaunchedEffect(time) {
            exoPlayer.seekTo((time * 1000).toLong())
        }
    }

    LaunchedEffect(speed) {
        exoPlayer.setPlaybackSpeed(speed)
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun rememberExoPlayer(
    url: String,
    context: Context,
    error: (MediaPlayerError) -> Unit,
    headers: Map<String, String>?
): ExoPlayer {
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            setHandleAudioBecomingNoisy(true)
        }
    }

    // Prepare media source when URL changes
    LaunchedEffect(url) {
        try {
            val mediaItem = MediaItem.fromUri(url.toUri())
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15_000)
                .setReadTimeoutMs(15_000)
                .apply {
                    headers?.let {
                        setDefaultRequestProperties(it)
                    }
                }

            val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

            val mediaSource =
                ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)

            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            exoPlayer.seekTo(0)
        } catch (e: Exception) {
            error(MediaPlayerError.PlaybackError(e.message ?: "Failed to load media"))
        }
    }

    return exoPlayer
}

private fun createPlayerListener(
    totalTime: (Float) -> Unit,
    currentTime: (Float) -> Unit,
    loadingState: (Boolean) -> Unit,
    didEndAudio: () -> Unit,
    isSliding: Boolean,
    error: (MediaPlayerError) -> Unit
): Player.Listener {

    return object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            super.onEvents(player, events)
            if (!isSliding) {
                totalTime(
                    TimeUnit.MILLISECONDS.toSeconds(player.duration).coerceAtLeast(0L).toFloat()
                )
                currentTime(
                    TimeUnit.MILLISECONDS.toSeconds(player.currentPosition).coerceAtLeast(0L)
                        .toFloat()
                )
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> loadingState(true)
                Player.STATE_READY -> loadingState(false)
                Player.STATE_ENDED -> {
                    didEndAudio()
                }

                Player.STATE_IDLE -> { /* No-op */
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            error(MediaPlayerError.PlaybackError(error.message ?: "Unknown playback error"))
        }
    }
}
