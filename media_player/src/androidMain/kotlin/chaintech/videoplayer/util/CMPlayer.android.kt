package chaintech.videoplayer.util

import android.app.Activity
import android.app.Application
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import chaintech.videoplayer.host.DrmConfig
import chaintech.videoplayer.host.MediaPlayerError
import chaintech.videoplayer.model.ScreenResize
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.concurrent.TimeUnit

@OptIn(UnstableApi::class)
@Composable
actual fun CMPPlayer(
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
    qualityList: ((List<VideoQuality>) -> Unit)
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val lifecycleOwner = LocalLifecycleOwner.current
    val exoPlayer = rememberExoPlayerWithLifecycle(url, context, isPause, isLiveStream, isPipMode, headers, drmConfig, error, selectedQuality, selectedAudioTrack, selectedSubTitle, audioList, subtitlesList, qualityList)
    val playerView = rememberPlayerView(exoPlayer, context)

    var isBuffering by remember { mutableStateOf(false) }

    // Notify buffer state changes
    LaunchedEffect(isBuffering) {
        bufferCallback(isBuffering)
    }

    // Update current time every second
    LaunchedEffect(exoPlayer) {
        while (isActive) {
            currentTime(
                TimeUnit.MILLISECONDS.toSeconds(exoPlayer.currentPosition).coerceAtLeast(0L).toFloat()
            )
            delay(1000) // Delay for 1 second
        }
    }

    // Keep screen on while the player view is active
    LaunchedEffect(playerView) {
        playerView.keepScreenOn = true
    }


    DisposableEffect(activity) {
        if (activity != null && isPipMode) {
            val callback = object : Application.ActivityLifecycleCallbacks {
                override fun onActivityPaused(act: Activity) {
                    if (act == activity && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            val aspectRatio = Rational(16, 9)
                            val params = PictureInPictureParams.Builder()
                                .setAspectRatio(aspectRatio)
                                .build()
                            activity.enterPictureInPictureMode(params)
                            onPipModeChanged(true)
                        } catch (_: Throwable) {}
                    }
                }

                override fun onActivityResumed(act: Activity) {}
                override fun onActivityStarted(act: Activity) {}
                override fun onActivityStopped(act: Activity) {}
                override fun onActivityCreated(act: Activity, savedInstanceState: Bundle?) {}
                override fun onActivitySaveInstanceState(act: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(act: Activity) {}
            }

            activity.application.registerActivityLifecycleCallbacks(callback)

            onDispose {
                activity.application.unregisterActivityLifecycleCallbacks(callback)
            }
        } else {
            onDispose { }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity != null && isPipMode) {
                    val inPip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        activity.isInPictureInPictureMode
                    } else {
                        false
                    }
                    if (!inPip) onPipModeChanged(false)
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box {
        AndroidView(
            factory = { playerView },
            modifier = modifier,
            update = {
                exoPlayer.playWhenReady = !isPause
                exoPlayer.volume = volume
                seekToTime?.let { exoPlayer.seekTo((it * 1000).toLong()) }
                exoPlayer.setPlaybackSpeed(speed)
                playerView.resizeMode = when (size) {
                    ScreenResize.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    ScreenResize.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            }
        )

        // Manage player listener and lifecycle
        DisposableEffect(key1 = exoPlayer) {
            val listener = createPlayerListener(
                isSliding,
                totalTime,
                currentTime,
                loadingState = { isBuffering = it },
                didEndVideo,
                loop,
                exoPlayer,
                error
            )

            exoPlayer.addListener(listener)

            onDispose {
                exoPlayer.removeListener(listener)
                exoPlayer.release()
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                exoPlayer.release()
                playerView.keepScreenOn = false
                CacheManager.release()
            }
        }
    }
}

private fun createPlayerListener(
    isSliding: Boolean,
    totalTime: (Float) -> Unit,
    currentTime: (Float) -> Unit,
    loadingState: (Boolean) -> Unit,
    didEndVideo: () -> Unit,
    loop: Boolean,
    exoPlayer: ExoPlayer,
    error: (MediaPlayerError) -> Unit
): Player.Listener {

    return object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
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
                Player.STATE_BUFFERING -> {
                    loadingState(true)
                }

                Player.STATE_READY -> {
                    loadingState(false)
                }

                Player.STATE_ENDED -> {
                    loadingState(false)
                    didEndVideo()
                    exoPlayer.seekTo(0)
                    if (loop) exoPlayer.play()
                }

                Player.STATE_IDLE -> {
                    loadingState(false)
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            error(MediaPlayerError.PlaybackError(error.message ?: "Unknown playback error"))
        }
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
