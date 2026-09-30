package chaintech.videoplayer.youtube

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import chaintech.videoplayer.host.MediaPlayerHost
import chaintech.videoplayer.model.PlayerSpeed
import chaintech.videoplayer.model.VideoPlayerConfig
import chaintech.videoplayer.util.extractYouTubeVideoId
import chaintech.videoplayer.util.getSeekTime
import chaintech.videoplayer.util.saveCurrentPosition
import com.multiplatform.webview.web.WebViewNavigator
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

internal actual fun executeCommand(
    navigator: WebViewNavigator,
    execCommand: PlayerCommand
) {

}

@OptIn(FlowPreview::class)
@Suppress("SetJavaScriptEnabled")
@Composable
actual fun DesktopYoutubeComposable(
    modifier: Modifier,
    playerHost: MediaPlayerHost,
    playerConfig: VideoPlayerConfig

) {
    playerConfig.isScreenResizeEnabled = false
    var isCooldownActive by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val hostState = remember { VideoPlayerHost() }
    var isVideoStarted by remember { mutableStateOf(false) }


    LaunchedEffect(hostState.volumeLevel) {
        hostState.volumeLevel?.let { playerHost.setVolume(it) }
    }
    LaunchedEffect(hostState.rate) {
        hostState.rate?.let {
            val speed = when(it) {
                0.5f -> PlayerSpeed.X0_5
                1.0f -> PlayerSpeed.X1
                1.5f -> PlayerSpeed.X1_5
                2.0f -> PlayerSpeed.X2
                else -> PlayerSpeed.X1
            }
            playerHost.setSpeed(speed)
        }
    }
    LaunchedEffect(hostState.isMute) {
        hostState.isMute?.let { if(it) { playerHost.mute() } else { playerHost.unmute() } }
    }
    LaunchedEffect(hostState.isPause) {
        hostState.isPause?.let { if(it) { playerHost.pause() } else { playerHost.play() } }
    }
    LaunchedEffect(hostState.isFullScreen) {
        hostState.isFullScreen?.let { playerHost.toggleFullScreen() }
    }
    LaunchedEffect(hostState.backAction) {
        hostState.backAction?.let { playerConfig.backActionCallback?.invoke() }
    }

    // Load video (same as mobile)
    LaunchedEffect(playerHost.url) {
        isVideoStarted = false
        val videoId = extractYouTubeVideoId(playerHost.url) ?: playerHost.url
        hostState.load(videoId)
    }

    LaunchedEffect(playerHost.isPaused) {
        coroutineScope.launch {
            if (playerHost.isPaused) hostState.pause() else hostState.play()
        }
    }

    LaunchedEffect(playerHost.isFullScreen) {
        coroutineScope.launch {
            hostState.setFullScreen(playerHost.isFullScreen)
        }
    }
    LaunchedEffect(playerConfig.resolvedShowControls) {
        coroutineScope.launch {
            hostState.setControls(!playerConfig.resolvedShowControls, !playerConfig.enableBackButton)
        }
    }

    LaunchedEffect(playerHost.isMuted) {
        coroutineScope.launch {
            if (playerHost.isMuted) hostState.mute() else hostState.unmute()
        }
    }
    LaunchedEffect(hostState.isBuffering) {
        playerHost.setBufferingStatus(hostState.isBuffering)
    }

    LaunchedEffect(playerHost.seekToTime) {
        playerHost.seekToTime?.let {
            coroutineScope.launch {
                hostState.seekTo(it.toDouble().seconds)
                playerHost.seekToTime = null
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { playerHost.volumeLevel }
            .distinctUntilChanged()
            .debounce(120)
            .collect { volume ->
                coroutineScope.launch {
                    if (hostState.playerState is VideoState.Playing) {
                        val normalizedVolume = (playerHost.volumeLevel.coerceIn(0f, 1f) * 100).toInt()
                        hostState.setVolume(normalizedVolume)
                    }
                }
            }
    }

    fun handleStartTime(
        playerHost: MediaPlayerHost,
        playerConfig: VideoPlayerConfig
    ) {
        getSeekTime(playerHost, playerConfig)?.let {
            playerHost.seekToTime = it
        }
    }

    fun handleEndVideo(status: PlayerEvent.State.VideoState) {
        if (status != PlayerEvent.State.VideoState.ENDED) return
        if (isCooldownActive) return

        isCooldownActive = true
        playerHost.triggerMediaEnd()

        coroutineScope.launch {
            if (playerHost.isLooping) {
                hostState.seekTo(0.seconds)
                hostState.play()
            } else {
                hostState.seekTo(0.seconds)
                if (!playerHost.isPaused) {
                    playerHost.togglePlayPause()
                }
            }

            delay(1000)
            isCooldownActive = false
        }
    }

    fun setSpeed() {
        coroutineScope.launch {
            val speed = when (playerHost.speed) {
                PlayerSpeed.X0_5 -> 0.5f
                PlayerSpeed.X1 -> 1.0f
                PlayerSpeed.X1_5 -> 1.5f
                PlayerSpeed.X2 -> 2.0f
            }
            hostState.setSpeed(speed)
        }
    }

    LaunchedEffect(playerHost.speed) {
        setSpeed()
    }

    when (val state = hostState.playerState) {
        is VideoState.Failed -> { }

        VideoState.Idle -> { }

        VideoState.Initialized -> {
            coroutineScope.launch {
                val videoId = extractYouTubeVideoId(playerHost.url)
                    ?: playerHost.url
                hostState.load(videoId)
                hostState.setControls(!playerConfig.resolvedShowControls,!playerConfig.enableBackButton)
            }
        }

        is VideoState.Playing -> {
            playerHost.updateTotalTime(state.totalDuration.inWholeSeconds.toFloat())
            if (!playerHost.isSliding) {
                playerHost.updateCurrentTime(state.currentTime.inWholeSeconds.toFloat())
            }
            handleEndVideo(state.playbackStatus)
            handleStartTime(playerHost, playerConfig)

            if (!isVideoStarted) {
                isVideoStarted = true

                coroutineScope.launch {
                    if (playerHost.isMuted) hostState.mute()
                    else hostState.unmute()

                    if (playerHost.isPaused) hostState.pause()
                    else hostState.play()
                }

                setSpeed()
            }
        }
    }


    LaunchedEffect(playerHost.totalTime) {
        getSeekTime(playerHost, playerConfig)?.let {
            playerHost.isSliding = true
            playerHost.seekToTime = minOf(playerHost.totalTime, it)
            playerHost.isSliding = false
        }
    }


    var previousUrl by remember { mutableStateOf(playerHost.url) }
    DisposableEffect(playerHost.url) {
        onDispose {
            if (playerConfig.enableResumePlayback) {
                saveCurrentPosition(playerHost, previousUrl)
                previousUrl = playerHost.url
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        DesktopEmbeddedPlayer(
            modifier = Modifier
                .fillMaxSize(),
            host = hostState
        )
    }
}