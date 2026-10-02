package chaintech.videoplayer.ui.video

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
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
import chaintech.videoplayer.util.CMPPlayer
import chaintech.videoplayer.util.getSeekTime
import chaintech.videoplayer.util.saveCurrentPosition
import kotlinx.coroutines.delay
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@Composable
internal fun VideoPlayerWithControl(
    modifier: Modifier,
    playerHost: MediaPlayerHost,
    playerConfig: VideoPlayerConfig
) {
    var isScreenLocked by remember { mutableStateOf(false) }
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

    LaunchedEffect(playerHost.isPIP) {
        if(playerHost.isPIP) {
            showControls = false
        }
    }

    LaunchedEffect(playerConfig.showControlsOverride) {
        playerConfig.showControlsOverride?.let { showControls = it }
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
                showVolumeControl = false // Hide immediately when finger is lifted
            }
        )
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
                            isLongPressActive = false
                        }
                        break
                    }
                }
            }
        }
    }

    //Get Last saved time from preference for resume video
    LaunchedEffect(playerHost.totalTime) {
        getSeekTime(playerHost, playerConfig)?.let {
            playerHost.isSliding = true
            playerHost.seekToTime = it
            playerHost.isSliding = false
        }
    }

    //Save video last position into preference
    var previousUrl by remember { mutableStateOf(playerHost.url) }
    DisposableEffect(playerHost.url) {
        onDispose {
            if(playerConfig.enableResumePlayback) {
                saveCurrentPosition(playerHost, previousUrl)
                previousUrl = playerHost.url
            }
        }
    }

    val zoomState = rememberZoomState(maxScale = 3f)
    LaunchedEffect(playerHost.videoFitMode) {
        zoomState.reset()
    }

    // Container for the video player and control components
    Box(
        modifier = modifier
            .clipToBounds()
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
                        }
                    }
                )
                .then(longPressSpeedModifier)
        ){
            // Video player component
            CMPPlayer(
                modifier = Modifier
                    .fillMaxSize(),
                url = playerHost.url,
                isPause = playerHost.isPaused,
                totalTime = { playerHost.updateTotalTime(it) }, // Update total time of the video
                currentTime = {
                    if (playerHost.isSliding.not()) {
                        playerHost.updateCurrentTime(it)  // Update current playback time
                        playerHost.seekToTime = null // Reset slider time if not sliding
                    }
                },
                isSliding = playerHost.isSliding, // Pass seek bar sliding state
                seekToTime = playerHost.seekToTime, // Pass seek bar slider time
                speed = if (isLongPressActive) {
                    playerConfig.longPressPlaybackSpeed
                } else {
                    when (playerHost.speed) {
                        PlayerSpeed.X0_5 -> 0.5f
                        PlayerSpeed.X1   -> 1.0f
                        PlayerSpeed.X1_5 -> 1.5f
                        PlayerSpeed.X2   -> 2.0f
                    }
                },
                size = playerHost.videoFitMode,
                bufferCallback = { playerHost.setBufferingStatus(it) },
                didEndVideo = {
                    playerHost.triggerMediaEnd()
                    if (!playerHost.isLooping) {
                        playerHost.togglePlayPause()
                    }
                },
                loop = playerHost.isLooping,
                volume = playerHost.volumeLevel,
                isLiveStream = playerConfig.isLiveStream,
                isPipMode = playerConfig.enablePIPControl,
                onPipModeChanged = { enable ->
                    if(enable) {
                        playerHost.enterPIP()
                    } else playerHost.exitPIP()
                },
                error = { playerHost.triggerError(it)},
                headers = playerHost.headers,
                drmConfig = playerHost.drmConfig,
                selectedQuality = playerHost.selectedQuality,
                selectedAudioTrack = playerHost.selectedAudioTrack,
                selectedSubTitle = playerHost.selectedsubTitle,
                audioList = { playerHost.updateAudioTrackOptions(it) },
                subtitlesList = { playerHost.updateSubTitleOptions(it) },
                qualityList = { playerHost.updateVideoQualityOptions(it) },
                engineConfig = playerConfig.engineConfig
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
                playerHost.isSliding = true
                playerHost.seekToTime =
                    maxOf(0f, playerHost.currentTime - playerConfig.fastForwardBackwardIntervalSeconds)
                playerHost.isSliding = false
            },
            onForwardToggle = {
                playerHost.isSliding = true
                playerHost.seekToTime = minOf(
                    playerHost.totalTime,
                    playerHost.currentTime + playerConfig.fastForwardBackwardIntervalSeconds
                )
                playerHost.isSliding = false
            },
            onChangeSliderTime = {
                playerHost.seekToTime = it
            },
            onLockScreenToggle = { isScreenLocked = it },
            userInteractionCallback = { handleControlInteraction() }
        )

        if (playerHost.isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (playerConfig.loaderView != null) {
                    playerConfig.loaderView?.invoke()
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                            .size(playerConfig.pauseResumeIconSize),
                        color = playerConfig.loadingIndicatorColor
                    )
                }
            }
        }
    }
}