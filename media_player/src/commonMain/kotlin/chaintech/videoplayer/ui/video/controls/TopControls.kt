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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import chaintech.videoplayer.model.PlayerOption
import chaintech.videoplayer.model.ScreenResize
import chaintech.videoplayer.model.VideoPlayerConfig
import chaintech.videoplayer.ui.component.AnimatedClickableIcon
import chaintech.videoplayer.util.ComposeResourceDrawable
import reelsdemo.composemultiplatformmediaplayer.generated.resources.Res
import reelsdemo.composemultiplatformmediaplayer.generated.resources.resize_fill
import reelsdemo.composemultiplatformmediaplayer.generated.resources.resize_fit


@Composable
internal fun TopControls(
    playerConfig: VideoPlayerConfig, // Configuration object for the player, includes styling options
    paddingValues: PaddingValues,
    isMute: Boolean, // Flag indicating whether the audio is muted
    onMuteToggle: (() -> Unit), // Callback for toggling mute/unMute
    showControls: Boolean, // Flag indicating whether controls should be shown
    onLockScreenToggle: (() -> Unit),
    onResizeScreenToggle: (() -> Unit),
    selectedSize: ScreenResize? = null,
    showQualityControl: Boolean,
    showAudioControl: Boolean,
    showSubTitleControl: Boolean,
    activeOption: PlayerOption,
    activeOptionCallBack: ((PlayerOption) -> Unit) = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(paddingValues)
                .padding(top = playerConfig.controlTopPadding) // Add padding to the top
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
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(
                        15.dp,
                        alignment = Alignment.End
                    ) // Spacing between items with end alignment
                ) {

                    if (playerConfig.enableBackButton) {
                        AnimatedClickableIcon(
                            painterRes = playerConfig.backIconResource,
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = playerConfig.iconsTintColor,
                            iconSize = playerConfig.topControlSize,
                            animationDuration = playerConfig.controlClickAnimationDuration,
                            onClick = { playerConfig.backActionCallback?.invoke() }
                        )
                        Spacer(Modifier.weight(1f))
                    }
                    if (showSubTitleControl && playerConfig.showSubTitlesOptions) {
                        AnimatedClickableIcon(
                            imageVector = Icons.Default.Subtitles,
                            contentDescription = "SubTitles",
                            tint = playerConfig.iconsTintColor,
                            iconSize = playerConfig.topControlSize,
                            animationDuration = playerConfig.controlClickAnimationDuration,
                            onClick = {
                                activeOptionCallBack(if (activeOption == PlayerOption.NONE) PlayerOption.SUBTITLES else PlayerOption.NONE)
                            } // Toggle Lock on click
                        )
                    }

                    if (showAudioControl && playerConfig.showAudioTracksOptions) {
                        AnimatedClickableIcon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = "Audio",
                            tint = playerConfig.iconsTintColor,
                            iconSize = playerConfig.topControlSize,
                            animationDuration = playerConfig.controlClickAnimationDuration,
                            onClick = {
                                activeOptionCallBack(if (activeOption == PlayerOption.NONE) PlayerOption.AUDIO_TRACK else PlayerOption.NONE)
                            } // Toggle Lock on click
                        )
                    }
                    if (showQualityControl && playerConfig.showVideoQualityOptions) {
                        AnimatedClickableIcon(
                            imageVector = Icons.Default.HighQuality,
                            contentDescription = "Quality",
                            tint = playerConfig.iconsTintColor,
                            iconSize = playerConfig.topControlSize,
                            animationDuration = playerConfig.controlClickAnimationDuration,
                            onClick = {
                                activeOptionCallBack(if (activeOption == PlayerOption.NONE) PlayerOption.QUALITY else PlayerOption.NONE)
                            } // Toggle Lock on click
                        )
                    }
                    // If speed control is enabled, show the speed control button
                    if (playerConfig.isSpeedControlEnabled && !playerConfig.isLiveStream) {
                        AnimatedClickableIcon(
                            painterRes = playerConfig.speedIconResource,
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Speed",
                            tint = playerConfig.iconsTintColor,
                            iconSize = playerConfig.topControlSize,
                            animationDuration = playerConfig.controlClickAnimationDuration,
                            onClick = {
                                activeOptionCallBack(if (activeOption == PlayerOption.NONE) PlayerOption.SPEED else PlayerOption.NONE)
                            } // Toggle Speed on click
                        )
                    }

                    // If mute control is enabled, show the mute/unMute button
                    if (playerConfig.isMuteControlEnabled) {
                        AnimatedClickableIcon(
                            painterRes = if (isMute) playerConfig.unMuteIconResource else playerConfig.muteIconResource,
                            imageVector = if (isMute) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Mute/UnMute",
                            tint = playerConfig.iconsTintColor,
                            iconSize = playerConfig.topControlSize,
                            animationDuration = playerConfig.controlClickAnimationDuration,
                            onClick = { onMuteToggle() } // Toggle mute/unMute on click
                        )
                    }

                    if (playerConfig.isScreenResizeEnabled) {
                        selectedSize?.let {
                            AnimatedClickableIcon(
                                painterRes = when (selectedSize) {
                                    ScreenResize.FIT -> ComposeResourceDrawable(Res.drawable.resize_fit)
                                    ScreenResize.FILL -> ComposeResourceDrawable(Res.drawable.resize_fill)
                                },
                                contentDescription = "Resize",
                                tint = playerConfig.iconsTintColor,
                                iconSize = playerConfig.topControlSize,
                                colorFilter = ColorFilter.tint(playerConfig.iconsTintColor),
                                animationDuration = playerConfig.controlClickAnimationDuration,
                                onClick = { onResizeScreenToggle() }
                            )
                        }
                    }
                    if (playerConfig.isScreenLockEnabled) {
                        AnimatedClickableIcon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = "Lock",
                            tint = playerConfig.iconsTintColor,
                            iconSize = playerConfig.topControlSize,
                            animationDuration = playerConfig.controlClickAnimationDuration,
                            onClick = { onLockScreenToggle() } // Toggle Lock on click
                        )
                    }
                }
            }
        }
    }
}


