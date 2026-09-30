package chaintech.videoplayer.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chaintech.videoplayer.extension.formatMinSec
import chaintech.videoplayer.host.MediaPlayerHost
import chaintech.videoplayer.model.PlayerOption
import chaintech.videoplayer.model.PlayerSpeed
import chaintech.videoplayer.model.VideoPlayerConfig
import chaintech.videoplayer.ui.component.AnimatedClickableIcon
import chaintech.videoplayer.ui.component.CustomSeekBar
import chaintech.videoplayer.ui.video.controls.AudioTrackSelectionOverlay
import chaintech.videoplayer.ui.video.controls.DesktopControlPanel
import chaintech.videoplayer.ui.video.controls.MovingWatermark
import chaintech.videoplayer.ui.video.controls.QualitySelectionOverlay
import chaintech.videoplayer.ui.video.controls.SpeedSelectionOverlay
import chaintech.videoplayer.util.CMPPlayer
import chaintech.videoplayer.util.getSeekTime
import chaintech.videoplayer.util.saveCurrentPosition
import kotlinx.coroutines.delay

@Composable
internal fun DesktopVideoPlayer(
    modifier: Modifier,
    playerHost: MediaPlayerHost,
    playerConfig: VideoPlayerConfig, // Configuration for the player
) {
    var sTime = playerHost.currentTime
    var showControls by remember { mutableStateOf(playerConfig.resolvedShowControls) } // State for showing/hiding controls

    var activeOption by remember { mutableStateOf(PlayerOption.NONE) }
    // Auto-hide controls if enabled
    if (playerConfig.resolvedShowControls && playerConfig.isAutoHideControlEnabled) {
        LaunchedEffect(showControls) {
            if (showControls) {
                delay(timeMillis = (playerConfig.controlHideIntervalSeconds * 1000).toLong()) // Delay hiding controls
                if (!playerHost.isSliding) {
                    showControls = false // Hide controls if seek bar is not being slid
                }
            }
        }
    }
    LaunchedEffect(playerConfig.showControlsOverride) {
        playerConfig.showControlsOverride?.let { showControls = it }
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
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    if (playerConfig.resolvedShowControls) {
                        showControls = !showControls
                    }
                }
            )
        }
    ) {
        // Video player component
        CMPPlayer(
            modifier = Modifier.fillMaxSize(),
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
            speed = when (playerHost.speed) {
                PlayerSpeed.X0_5 -> 0.5f
                PlayerSpeed.X1   -> 1.0f
                PlayerSpeed.X1_5 -> 1.5f
                PlayerSpeed.X2   -> 2.0f
            }, // Pass selected playback speed
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
            isPipMode = false,
            onPipModeChanged = { enable ->
                if(enable) {
                    playerHost.enterPIP()
                } else playerHost.exitPIP()
            },
            error = { playerHost.triggerError(it) },
            headers = playerHost.headers,
            drmConfig = playerHost.drmConfig,
            selectedQuality = playerHost.selectedQuality,
            selectedAudioTrack = playerHost.selectedAudioTrack,
            selectedSubTitle = playerHost.selectedsubTitle,
            audioList = { playerHost.updateAudioTrackOptions(it) },
            subtitlesList = { playerHost.updateSubTitleOptions(it) },
            qualityList = { playerHost.updateVideoQualityOptions(it) }
        )

        playerConfig.watermarkConfig?.let {
            MovingWatermark(
                config = it,
                modifier = Modifier.matchParentSize()
            )
        }

        if (showControls) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (playerConfig.enableBackButton) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(
                                Color.Black.copy(
                                    alpha = playerConfig.backdropAlpha
                                )
                            )
                            .padding(vertical = 16.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        AnimatedClickableIcon(
                            painterRes = playerConfig.backIconResource,
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = playerConfig.iconsTintColor,
                            iconSize = playerConfig.topControlSize,
                            animationDuration = playerConfig.controlClickAnimationDuration,
                            onClick = { playerConfig.backActionCallback?.invoke() }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.Black.copy(
                                alpha = playerConfig.backdropAlpha
                            )
                        )
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    if (!playerConfig.isLiveStream && playerConfig.isSeekBarVisible) {
                        if (playerConfig.isDurationVisible && (playerConfig.chapters != null)) {
                            playerConfig.chapters
                                ?.sortedByDescending { it.startTime }
                                ?.firstOrNull { playerHost.currentTime * 1000 >= it.startTime }
                                ?.let { activeChapter ->
                                    Text(
                                        text = "${playerHost.currentTime.formatMinSec()}/${playerHost.totalTime.formatMinSec()} • ${activeChapter.title}",
                                        style = playerConfig.durationTextStyle,
                                        color = playerConfig.durationTextColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(bottom = 10.dp)
                                            .padding(horizontal = 16.dp)
                                    )
                                }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (playerConfig.isDurationVisible && (playerConfig.chapters == null)) {
                                Text(
                                    modifier = Modifier,
                                    text = playerHost.currentTime.formatMinSec(), // Format the current time to "MM:SS" format
                                    color = playerConfig.durationTextColor,
                                    style = playerConfig.durationTextStyle
                                )
                            }
                            CustomSeekBar(
                                modifier = Modifier.weight(1f),
                                progress = playerHost.currentTime,
                                maxProgress = playerHost.totalTime,
                                onValueChange = {
                                    sTime = it
                                    playerHost.isSliding = true
                                    playerHost.seekToTime = null
                                    playerHost.updateCurrentTime(it)
                                },
                                onValueChangeFinished = {
                                    playerHost.isSliding = false
                                    playerHost.seekToTime = sTime
                                },
                                thumbRadius = playerConfig.seekBarThumbRadius,
                                trackHeight = playerConfig.seekBarTrackHeight,
                                activeTrackColor = playerConfig.seekBarActiveTrackColor, // Active color
                                inactiveTrackColor = playerConfig.seekBarInactiveTrackColor, // Inactive color
                                thumbColor = playerConfig.seekBarThumbColor, // Thumb color
                                showThumbAlways = true,
                                chapters = playerConfig.chapters
                            )

                            if (playerConfig.isDurationVisible && (playerConfig.chapters == null)) {
                                Text(
                                    modifier = Modifier,
                                    text = playerHost.totalTime.formatMinSec(), // Format the total time to "MM:SS" format
                                    color = playerConfig.durationTextColor,
                                    style = playerConfig.durationTextStyle
                                )
                            }
                        }
                    }
                    if (playerConfig.isLiveStream) {
                        LiveStreamComponent(playerConfig)
                    }

                    DesktopControlPanel(
                        playerHost = playerHost,
                        playerConfig = playerConfig,
                        volume = playerHost.volumeLevel,
                        volumeCallback = { playerHost.setVolume(it) },
                        saveVolumeCallback = {
                            playerHost.setVolume(it)
                            if (playerHost.volumeLevel > 0) playerHost.unmute() else playerHost.mute()
                        },
                        activeOption = activeOption,
                        activeOptionCallBack = { activeOption = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }


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

        SpeedSelectionOverlay(
            paddingValues = PaddingValues(),
            playerConfig = playerConfig,
            selectedSpeed = playerHost.speed,
            selectedSpeedCallback = { playerHost.setSpeed(it) },
            activeOption = activeOption,
            activeOptionCallBack = { activeOption = it }
        )

        QualitySelectionOverlay(
            paddingValues = PaddingValues(),
            playerConfig = playerConfig,
            qualityOptions = playerHost.qualityOptions,
            selectedQuality = playerHost.selectedQuality,
            selectedQualityCallback = { playerHost.setVideoQuality(it) },
            activeOption = activeOption,
            activeOptionCallBack = { activeOption = it }
        )

        AudioTrackSelectionOverlay(
            paddingValues = PaddingValues(),
            playerConfig = playerConfig,
            audioTrackOptions = playerHost.audioTrackOptions,
            selectedAudioTrack = playerHost.selectedAudioTrack,
            selectedAudioTrackCallback = { playerHost.setAudioTrack(it) },
            activeOption = activeOption,
            activeOptionCallBack = { activeOption = it }
        )
    }
}

@Composable
private fun LiveStreamComponent(
    playerConfig: VideoPlayerConfig // Configuration object for styling options
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp), // Horizontal padding for the row
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp) // Space between elements
    ) {
        Spacer(modifier = Modifier.weight(1f)) // Spacer to push content to the right

        // Live indicator container
        Row(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.2f), shape = RoundedCornerShape(3.dp))
                .padding(vertical = 4.dp, horizontal = 8.dp), // Padding for better appearance
            horizontalArrangement = Arrangement.spacedBy(5.dp), // Space between elements
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live dot indicator
            Box(
                modifier = Modifier
                    .size(10.dp) // Size of the dot
                    .background(Color.Red, shape = CircleShape) // Red color and circular shape
            )

            // Display "Live" text
            Text(
                text = "Live", // Indicate that this is a live stream
                color = playerConfig.durationTextColor,
                style = playerConfig.durationTextStyle,
                modifier = Modifier.padding(end = 6.dp) // Padding at the end for spacing
            )
        }
    }
}