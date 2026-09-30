package chaintech.videoplayer.ui.video.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chaintech.videoplayer.extension.formatMinSec
import chaintech.videoplayer.model.VideoPlayerConfig
import chaintech.videoplayer.ui.component.AnimatedClickableIcon
import chaintech.videoplayer.ui.component.CustomSeekBar

@Composable
internal fun BottomControls(
    playerConfig: VideoPlayerConfig, // Configuration object for the player, includes styling options
    paddingValues: PaddingValues,
    currentTime: Float, // Current playback time in seconds
    totalTime: Float, // Total duration of the media in seconds
    showControls: Boolean, // Flag to determine if controls should be visible
    isFullScreen: Boolean,
    onFullScreenToggle: (() -> Unit),
    onChangeSliderTime: ((Float?) -> Unit), // Callback for slider value change
    onChangeCurrentTime: ((Float) -> Unit), // Callback for current time change
    onChangeSliding: ((Boolean) -> Unit) // Callback for slider sliding state change
) {
    var slideTime = currentTime
    // Only display the seek bar if specified in the player configuration
    if (playerConfig.isSeekBarVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart) // Align the column to the bottom
                    .padding(paddingValues)
                    .padding(bottom = if (isFullScreen) playerConfig.seekBarBottomPaddingInFullScreen else playerConfig.seekBarBottomPadding)
            ) {
                // Show controls with animation based on the visibility flag
                AnimatedVisibility(
                    modifier = Modifier,
                    visible = showControls,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f) // Occupy remaining horizontal space
                        ) {
                            if (playerConfig.isDurationVisible || playerConfig.isFullScreenEnabled) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically, // Ensures vertical alignment
                                    horizontalArrangement = Arrangement.Start // Distribute space evenly between the child components
                                ) {
                                    if (playerConfig.isDurationVisible) {
                                        val activeChapter = playerConfig.chapters
                                            ?.sortedByDescending { it.startTime }
                                            ?.firstOrNull { currentTime * 1000 >= it.startTime }

                                        val durationText = buildAnnotatedString {
                                            append("${currentTime.formatMinSec()}/${totalTime.formatMinSec()}")
                                            if (activeChapter != null) {
                                                append(" • ${activeChapter.title}")
                                            }
                                        }

                                        // Display the current playback time
                                        Text(
                                            text = durationText, // Format the current time to "MM:SS" format
                                            color = playerConfig.durationTextColor,
                                            style = playerConfig.durationTextStyle,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }

                                    if (playerConfig.isFullScreenEnabled) {
                                        AnimatedClickableIcon(
                                            imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                            contentDescription = "FullScreen",
                                            tint = playerConfig.iconsTintColor,
                                            iconSize = playerConfig.topControlSize,
                                            animationDuration = playerConfig.controlClickAnimationDuration,
                                            onClick = { onFullScreenToggle() } // Toggle mute/unMute on click
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            CustomSeekBar(
                                modifier = Modifier.fillMaxWidth(),
                                progress = currentTime,
                                maxProgress = totalTime,
                                onValueChange = {
                                    slideTime = it
                                    onChangeSliding(true)
                                    onChangeSliderTime(null)
                                    onChangeCurrentTime(it)
                                },
                                onValueChangeFinished = {
                                    onChangeSliding(false)
                                    onChangeSliderTime(slideTime)
                                },
                                thumbRadius = playerConfig.seekBarThumbRadius,
                                trackHeight = playerConfig.seekBarTrackHeight,
                                activeTrackColor = playerConfig.seekBarActiveTrackColor, // Active color
                                inactiveTrackColor = playerConfig.seekBarInactiveTrackColor, // Inactive color
                                thumbColor = playerConfig.seekBarThumbColor, // Thumb color,
                                chapters = playerConfig.chapters
                            )
                        }
                    }
                }
            }
        }
    }
}
