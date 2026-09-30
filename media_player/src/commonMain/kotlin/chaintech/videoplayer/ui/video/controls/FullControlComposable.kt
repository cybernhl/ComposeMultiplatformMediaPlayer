package chaintech.videoplayer.ui.video.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import chaintech.videoplayer.host.MediaPlayerHost
import chaintech.videoplayer.model.PlayerOption
import chaintech.videoplayer.model.ScreenResize
import chaintech.videoplayer.model.VideoPlayerConfig
import chaintech.videoplayer.ui.component.LiveStreamComposable

@Composable
internal fun FullControlComposable(
    playerHost: MediaPlayerHost,
    playerConfig: VideoPlayerConfig,
    showControls: Boolean,
    isScreenLocked: Boolean,
    showVolumeControl: Boolean = false,
    activeOption: PlayerOption,
    activeOptionCallBack: ((PlayerOption) -> Unit) = {},
    onBackwardToggle: (() -> Unit),
    onForwardToggle: (() -> Unit),
    onChangeSliderTime: ((Float?) -> Unit),
    onLockScreenToggle: ((Boolean) -> Unit),
    userInteractionCallback: (() -> Unit),
) {
    val insets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    val topPadding = insets.asPaddingValues().calculateTopPadding()
    val bottomPadding = insets.asPaddingValues().calculateBottomPadding()
    val startPadding = insets.asPaddingValues().calculateStartPadding(LayoutDirection.Ltr)
    val endPadding = insets.asPaddingValues().calculateEndPadding(LayoutDirection.Ltr)

    Box {
        AnimatedVisibility(
            modifier = Modifier,
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                Modifier.fillMaxSize().background(
                    Color.Black.copy(
                        alpha = playerConfig.backdropAlpha
                    )
                )
            )
        }
        Box(
            modifier = Modifier
                .then(if(playerHost.isFullScreen) Modifier.padding(top = topPadding, bottom = bottomPadding) else Modifier)
        ) {
            if (isScreenLocked.not()) {
                // Top control view for playback speed and mute/unMute
                TopControls(
                    playerConfig = playerConfig,
                    paddingValues = if(playerHost.isFullScreen) PaddingValues(start = startPadding, end = endPadding) else PaddingValues(),
                    isMute = playerHost.isMuted,
                    onMuteToggle = {
                        playerHost.toggleMuteUnmute()
                        userInteractionCallback()
                    }, // Toggle mute/unMute
                    showControls = showControls, // Pass show/hide controls state
                    onLockScreenToggle = {
                        onLockScreenToggle(!isScreenLocked)
                        userInteractionCallback()
                    },
                    onResizeScreenToggle = {
                        playerHost.setVideoFitMode(
                            when (playerHost.videoFitMode) {
                                ScreenResize.FIT -> ScreenResize.FILL
                                ScreenResize.FILL -> ScreenResize.FIT
                            }
                        )
                        userInteractionCallback()
                    },
                    selectedSize = playerHost.videoFitMode,
                    showQualityControl = playerHost.qualityOptions.isNotEmpty(),
                    showAudioControl =  playerHost.audioTrackOptions.isNotEmpty(),
                    showSubTitleControl = playerHost.subTitlesOptions.isNotEmpty(),
                    activeOption = activeOption,
                    activeOptionCallBack = activeOptionCallBack
                )

                // Center control view for pause/resume and fast forward/backward actions
                CenterControls(
                    playerConfig = playerConfig,
                    isPause = playerHost.isPaused,
                    onPauseToggle = {
                        playerHost.togglePlayPause()
                        userInteractionCallback()
                    },
                    onBackwardToggle = {
                        onBackwardToggle()
                        userInteractionCallback()
                    },
                    onForwardToggle = {
                        onForwardToggle()
                        userInteractionCallback()
                    },
                    showControls = showControls
                )

                if (playerConfig.isLiveStream) {
                    LiveStreamComposable(
                        playerConfig = playerConfig,
                        paddingValues = if(playerHost.isFullScreen) PaddingValues(start = startPadding, end = endPadding) else PaddingValues(),
                        isFullScreen = playerHost.isFullScreen,
                        onFullScreenToggle = { playerHost.toggleFullScreen() },
                    )
                } else {
                    // Bottom control view for seek bar and time duration display
                    BottomControls(
                        playerConfig = playerConfig,
                        paddingValues = if(playerHost.isFullScreen) PaddingValues(start = startPadding, end = endPadding) else PaddingValues(),
                        currentTime = playerHost.currentTime, // Pass current playback time
                        totalTime = playerHost.totalTime, // Pass total duration of the video
                        showControls = showControls, // Pass show/hide controls state
                        isFullScreen = playerHost.isFullScreen,
                        onFullScreenToggle = {
                            playerHost.toggleFullScreen()
                            userInteractionCallback()
                        },
                        onChangeSliderTime = onChangeSliderTime, // Update seek bar slider time
                        onChangeCurrentTime = { playerHost.updateCurrentTime(it) }, // Update current playback time
                        onChangeSliding = { // Update seek bar sliding state
                            playerHost.isSliding = it
                            userInteractionCallback()
                        }
                    )
                }

                // Volume Control UI (visible only while dragging)
                if (showVolumeControl) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart) // Centered on the left
                            .padding(start = 16.dp) // Padding from left
                            .then(if(playerHost.isFullScreen) Modifier.padding(start = startPadding) else Modifier)
                            .width(40.dp)
                            .height(160.dp) // Increased height for spacing
                            .background(Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(20.dp)) // Capsule shape
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween, // Ensure icons are properly placed
                            modifier = Modifier.fillMaxHeight() // Make sure it takes full height inside the Box
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Volume Up",
                                tint = Color.White
                            )
                            Box(
                                modifier = Modifier
                                    .height(80.dp)
                                    .width(8.dp)
                                    .background(Color.Gray, shape = RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height((80 * playerHost.volumeLevel).dp) // Dynamic volume level
                                        .background(Color.White, shape = RoundedCornerShape(4.dp))
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeMute,
                                contentDescription = "Volume Mute",
                                tint = Color.White
                            )
                        }
                    }
                }
            } else {
                if (playerConfig.isScreenLockEnabled) {
                    LockScreenComponent(
                        playerConfig = playerConfig,
                        showControls = showControls,
                        onLockScreenToggle = { onLockScreenToggle(!isScreenLocked) },
                        paddingValues = if(playerHost.isFullScreen) PaddingValues(start = startPadding, end = endPadding) else PaddingValues()
                    )
                }
            }
        }

        SelectionOverlays(
            playerHost = playerHost,
            playerConfig = playerConfig,
            activeOption = activeOption,
            activeOptionCallBack = activeOptionCallBack,
            paddingValues = PaddingValues(start = startPadding, end = endPadding)
        )
    }
}

@Composable
private fun SelectionOverlays(
    playerHost: MediaPlayerHost,
    playerConfig: VideoPlayerConfig,
    activeOption: PlayerOption,
    activeOptionCallBack: ((PlayerOption) -> Unit),
    paddingValues: PaddingValues
) {
    when (activeOption) {
        PlayerOption.SPEED ->
            SpeedSelectionOverlay(
                paddingValues = paddingValues,
                playerConfig = playerConfig,
                selectedSpeed = playerHost.speed,
                selectedSpeedCallback = { playerHost.setSpeed(it) },
                activeOption = activeOption,
                activeOptionCallBack = activeOptionCallBack
            )
        PlayerOption.QUALITY ->
            QualitySelectionOverlay(
                paddingValues = paddingValues,
                playerConfig = playerConfig,
                qualityOptions = playerHost.qualityOptions,
                selectedQuality = playerHost.selectedQuality,
                selectedQualityCallback = { playerHost.setVideoQuality(it) },
                activeOption = activeOption,
                activeOptionCallBack = activeOptionCallBack
            )
        PlayerOption.AUDIO_TRACK ->
            AudioTrackSelectionOverlay(
                paddingValues = paddingValues,
                playerConfig = playerConfig,
                audioTrackOptions = playerHost.audioTrackOptions,
                selectedAudioTrack = playerHost.selectedAudioTrack,
                selectedAudioTrackCallback = { playerHost.setAudioTrack(it) },
                activeOption = activeOption,
                activeOptionCallBack = activeOptionCallBack
            )
        PlayerOption.SUBTITLES ->
            SubTitlesSelectionOverlay(
                paddingValues = paddingValues,
                playerConfig = playerConfig,
                subTitlesOptions = playerHost.subTitlesOptions,
                selectedSubTitle = playerHost.selectedsubTitle,
                selectedSubTitleCallback = { playerHost.setSubTitle(it) },
                activeOption = activeOption,
                activeOptionCallBack = activeOptionCallBack
            )
        PlayerOption.NONE -> {}
    }
}
