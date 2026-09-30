package chaintech.videoplayer.ui.youtube

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chaintech.videoplayer.host.MediaPlayerHost
import chaintech.videoplayer.model.PlayerOption
import chaintech.videoplayer.model.PlayerSpeed
import chaintech.videoplayer.model.VideoPlayerConfig
import chaintech.videoplayer.ui.video.controls.FullControlComposable
import chaintech.videoplayer.ui.video.controls.MovingWatermark
import chaintech.videoplayer.util.extractYouTubeVideoId
import chaintech.videoplayer.util.getSeekTime
import chaintech.videoplayer.util.rememberAppBackgroundObserver
import chaintech.videoplayer.util.saveCurrentPosition
import chaintech.videoplayer.util.youtubeProgressColor
import chaintech.videoplayer.youtube.EmbeddedPlayer
import chaintech.videoplayer.youtube.PlayerEvent
import chaintech.videoplayer.youtube.VideoPlayerHost
import chaintech.videoplayer.youtube.VideoState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource


@OptIn(ExperimentalTime::class, FlowPreview::class)
@Composable
internal fun YoutubePlayerWithControl(
    modifier: Modifier,
    playerHost: MediaPlayerHost,
    playerConfig: VideoPlayerConfig
) {
    val coroutineScope = rememberCoroutineScope()
    val hostState = remember { VideoPlayerHost() }

    var isScreenLocked by remember { mutableStateOf(false) }
    var isInitializing by remember { mutableStateOf(true) }
    var pause by remember { mutableStateOf(playerHost.isPaused) }
    var isVideoStarted by remember { mutableStateOf(false) }
    var isCooldownActive by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(playerConfig.resolvedShowControls) } // State for showing/hiding controls
    var showVolumeControl by remember { mutableStateOf(false) }
    var volumeDragAmount by remember { mutableStateOf(0f) }
    var initialVolume by remember { mutableStateOf(0f) }
    var activeOption by remember { mutableStateOf(PlayerOption.NONE) }
    var isLongPressActive by remember { mutableStateOf(false) }

    val timeSource = remember { TimeSource.Monotonic }
    var lastInteractionMark by remember { mutableStateOf(timeSource.markNow()) }

    val handleControlInteraction = {
        if (playerConfig.resolvedShowControls) {
            lastInteractionMark = timeSource.markNow()  // Reset the interaction timer
            showControls = true
        }
    }
    LaunchedEffect(playerConfig.showControlsOverride) {
        playerConfig.showControlsOverride?.let { showControls = it }
    }

    // Auto-hide controls if enabled
    if (playerConfig.resolvedShowControls && playerConfig.isAutoHideControlEnabled) {
        LaunchedEffect(showControls, lastInteractionMark) {
            if (showControls) {
                val timeoutDuration = playerConfig.controlHideIntervalSeconds.seconds
                delay(timeoutDuration.inWholeMilliseconds) // Delay hiding controls
                if (!playerHost.isSliding && lastInteractionMark.elapsedNow() >= timeoutDuration) {
                    showControls = false // Hide controls if seek bar is not being slid
                }
            }
        }
    }

    pause = playerHost.isPaused
    playerConfig.isScreenResizeEnabled = false

    // Observe app background state
    val appBackgroundObserver = rememberAppBackgroundObserver()
    LaunchedEffect(Unit) {
        appBackgroundObserver.observe { enterBackground ->
            if (enterBackground && !pause) {
                playerHost.togglePlayPause()
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { appBackgroundObserver.removeObserver() }
    }

    // React to URL changes
    LaunchedEffect(playerHost.url) {
        isVideoStarted = false
        val videoId = extractYouTubeVideoId(playerHost.url) ?: playerHost.url
        coroutineScope.launch { hostState.load(videoId) }
    }
// Update player callbacks
    LaunchedEffect(hostState.isBuffering) {
        playerHost.setBufferingStatus(hostState.isBuffering)
    }

    LaunchedEffect(playerHost.seekToTime) {
        playerHost.seekToTime?.let {
            coroutineScope.launch {
                hostState.seekTo(it.toInt().seconds)
                playerHost.seekToTime = null
            }
        }
    }
    fun handleEndVideo(status: PlayerEvent.State.VideoState) {
        if (status == PlayerEvent.State.VideoState.ENDED) {
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
    }

    fun handleStartTime() {
        getSeekTime(playerHost, playerConfig)?.let {
            coroutineScope.launch {
                hostState.seekTo(it.toInt().seconds)
            }
        }
    }

    var previousUrl by remember { mutableStateOf(playerHost.url) }
    DisposableEffect(playerHost.url) {
        onDispose {
            if(playerConfig.enableResumePlayback) {
                saveCurrentPosition(playerHost, previousUrl)
                previousUrl = playerHost.url
            }
        }
    }

    val volumeDragModifier = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onDragStart = {
                showVolumeControl = true
                volumeDragAmount = 0f
                initialVolume = playerHost.volumeLevel
            },
            onVerticalDrag = { _, dragAmount ->
                volumeDragAmount += dragAmount

                // Convert drag distance to volume delta
                val delta = volumeDragAmount / 600f   // lower = more sensitive
                val newVolume = (initialVolume - delta).coerceIn(0f, 1f)

                if (newVolume != playerHost.volumeLevel) {
                    playerHost.setVolume(newVolume)
                    if (newVolume > 0f) playerHost.unmute() else playerHost.mute()
                }
            },
            onDragEnd = {
                showVolumeControl = false
            }
        )
    }

    LaunchedEffect(Unit) {
        snapshotFlow { playerHost.volumeLevel }
            .distinctUntilChanged()
            .debounce(120)
            .collect { volume ->
                if (hostState.playerState is VideoState.Playing) {
                    val normalized = (volume * 100).toInt()
                    hostState.setVolume(normalized)
                }
            }
    }

    fun setSpeed() {
        coroutineScope.launch {
            if (hostState.playerState is VideoState.Playing) {
                hostState.setSpeed(
                    when (playerHost.speed) {
                        PlayerSpeed.X0_5 -> 0.5f
                        PlayerSpeed.X1 -> 1f
                        PlayerSpeed.X1_5 -> 1.5f
                        PlayerSpeed.X2 -> 2f
                    }
                )
            }
        }
    }

    val longPressSpeedModifier = Modifier.pointerInput(
        playerConfig.enableLongPressFastForward,
        isScreenLocked,
        playerConfig.longPressPlaybackSpeed
    ) {
        if (!playerConfig.enableLongPressFastForward || isScreenLocked) return@pointerInput
        while (true) {
            awaitPointerEventScope {
                isLongPressActive = false
                while (true) {
                    val event = awaitPointerEvent()
                    // finger lifted or left screen
                    if (event.type == PointerEventType.Release ||
                        event.type == PointerEventType.Exit
                    ) {
                        if (isLongPressActive) {
                            setSpeed() // reset to normal speed
                            isLongPressActive = false
                        }
                        break
                    }
                }
            }
        }
    }

    when (val state = hostState.playerState) {
        is VideoState.Failed -> isInitializing = false

        VideoState.Idle -> isInitializing = true

        is VideoState.Playing -> {
            playerHost.updateTotalTime(state.totalDuration.inWholeSeconds.toFloat())
            if (!playerHost.isSliding) {
                playerHost.updateCurrentTime(state.currentTime.inWholeSeconds.toFloat())
            }
            handleEndVideo(state.playbackStatus)
            handleStartTime()
            if (!isVideoStarted) {
                isVideoStarted = true
                coroutineScope.launch {
                    if (playerHost.isMuted) { hostState.mute() } else { hostState.unmute() }
                    if (playerHost.isPaused) { hostState.pause()  } else { hostState.play() }
                }
                setSpeed()
            }
            isInitializing = false
        }

        VideoState.Initialized -> coroutineScope.launch {
            val videoId = extractYouTubeVideoId(playerHost.url) ?: playerHost.url
            hostState.load(videoId)
        }
    }

    // React to state changes
    LaunchedEffect(playerHost.isPaused) {
        coroutineScope.launch {
            if (hostState.playerState is VideoState.Playing) {
                if (playerHost.isPaused) {
                    hostState.pause()
                } else {
                    hostState.play()
                }
            }
        }
    }
    LaunchedEffect(playerHost.isMuted) {
        coroutineScope.launch {
            if (hostState.playerState is VideoState.Playing) {
                if (playerHost.isMuted) {
                    hostState.mute()
                } else {
                    hostState.unmute()
                }
            }
        }
    }

    LaunchedEffect(playerHost.speed) {
        setSpeed()
    }

    val zoomState = rememberZoomState(maxScale = 3f)
    LaunchedEffect(playerHost.videoFitMode) {
        zoomState.reset()
    }

    // Container for the video player and control components
    Box(
        modifier = modifier
            .clipToBounds() // Ensures the zoomed content stays within the bo
    ) {
        Box(
            modifier = modifier
                .zoomable(
                    zoomState = zoomState,
                    zoomEnabled = (!isScreenLocked && playerConfig.isZoomEnabled),
                    enableOneFingerZoom = false,
                    onTap = {
                        if(playerConfig.resolvedShowControls) {
                            showControls = !showControls // Toggle show/hide controls on tap
                            activeOption = PlayerOption.NONE
                        }
                    },
                    onLongPress = {
                        if(playerConfig.enableLongPressFastForward && !isScreenLocked) {
                            isLongPressActive = true
                            showControls = false
                            coroutineScope.launch {
                                if (hostState.playerState is VideoState.Playing) {
                                    hostState.setSpeed(playerConfig.longPressPlaybackSpeed)
                                }
                            }
                        }
                    }
                )
                .then(longPressSpeedModifier)
        ){
            // Youtube player component
            EmbeddedPlayer(
                modifier = modifier,
                host = hostState,
            )

            playerConfig.watermarkConfig?.let {
                MovingWatermark(
                    config = it,
                    modifier = Modifier.matchParentSize()
                )
            }

            Box(
                modifier = Modifier
                    .matchParentSize() // Covers the WebView entirely
                    .background(Color.Transparent) // Invisible but blocks touch
                    .pointerInput(Unit) {} // Consumes all touch events, blocking WebView
            )

            if (!isScreenLocked && playerConfig.isGestureVolumeControlEnabled) {
                // Detect right-side drag gestures
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.3f) // Occupy 30% of the right side dynamically
                        .align(Alignment.CenterEnd)
                        .then(volumeDragModifier) // Apply drag gesture detection only on the right side
                )
            }

            if (isInitializing) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black)
                        .wrapContentSize(align = Alignment.Center)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(1.5f)
                            .wrapContentSize(align = Alignment.Center), // Center the progress indicator
                        color = youtubeProgressColor
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isLongPressActive,
            enter = fadeIn(animationSpec = tween(150)) +
                    scaleIn(initialScale = 0.7f, animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(200)) +
                    scaleOut(targetScale = 0.7f, animationSpec = tween(200)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)   // YouTube-style top offset
        ) {
            Row(
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.70f),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.FastForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                val speedLabel = if (playerConfig.longPressPlaybackSpeed % 1f == 0f) {
                    playerConfig.longPressPlaybackSpeed.toInt().toString()
                } else {
                    playerConfig.longPressPlaybackSpeed.toString()
                }

                Text(
                    text = "${speedLabel}x",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        FullControlComposable(
            playerHost = playerHost,
            playerConfig = playerConfig,
            showControls = showControls,
            isScreenLocked = isScreenLocked,
            showVolumeControl = showVolumeControl,
            activeOption = activeOption,
            activeOptionCallBack = { activeOption = it },
            onBackwardToggle = {
                coroutineScope.launch {
                    hostState.seekTo(
                        maxOf(
                            0f,
                            playerHost.currentTime - playerConfig.fastForwardBackwardIntervalSeconds
                        ).toDouble().seconds
                    )
                }
            },
            onForwardToggle = {
                coroutineScope.launch {
                    hostState.seekTo(
                        minOf(
                            playerHost.totalTime,
                            playerHost.currentTime + playerConfig.fastForwardBackwardIntervalSeconds
                        ).toDouble().seconds
                    )
                }
            },
            onChangeSliderTime = {
                it?.let {
                    coroutineScope.launch {
                        hostState.seekTo(it.toDouble().seconds)
                    }
                }
            },
            onLockScreenToggle = { isScreenLocked = it },
            userInteractionCallback = { handleControlInteraction() }
        )
    }
}