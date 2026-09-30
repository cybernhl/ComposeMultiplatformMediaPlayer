package chaintech.videoplayer.ui.video.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import chaintech.videoplayer.model.VideoPlayerConfig
import chaintech.videoplayer.ui.component.AnimatedClickableIcon

@Composable
internal fun CenterControls(
    playerConfig: VideoPlayerConfig, // Configuration object for the player, includes styling options
    isPause: Boolean, // Flag indicating whether the media is paused
    onPauseToggle: (() -> Unit), // Callback for toggling pause/resume
    onBackwardToggle: (() -> Unit), // Callback for backward seek
    onForwardToggle: (() -> Unit), // Callback for forward seek
    showControls: Boolean, // Flag indicating whether controls should be shown
) {
    // Show controls with animation based on the visibility flag
    AnimatedVisibility(
        modifier = Modifier,
        visible = showControls,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        // Center the controls within a Box
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Row to contain control icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Spacer(modifier = Modifier.weight(2f))
                // If fast backward is enabled and an icon is provided, show the backward icon
                if (playerConfig.isFastForwardBackwardEnabled && !playerConfig.isLiveStream) {
                    AnimatedClickableIcon(
                        painterRes = playerConfig.fastBackwardIconResource,
                        imageVector = Icons.Filled.FastRewind,
                        contentDescription = "Fast Backward",
                        tint = playerConfig.iconsTintColor,
                        iconSize = playerConfig.fastForwardBackwardIconSize,
                        animationDuration = playerConfig.controlClickAnimationDuration,
                        onClick = { onBackwardToggle() }
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // If pause/resume is enabled and icons are provided, show the appropriate icon
                if (playerConfig.isPauseResumeEnabled) {
                    AnimatedClickableIcon(
                        painterRes = if (isPause) playerConfig.playIconResource else playerConfig.pauseIconResource,
                        imageVector = if (isPause) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = "Play/Pause",
                        tint = playerConfig.iconsTintColor,
                        iconSize = playerConfig.pauseResumeIconSize,
                        animationDuration = playerConfig.controlClickAnimationDuration,
                        onClick = { onPauseToggle() }
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // If fast forward is enabled and an icon is provided, show the forward icon
                if (playerConfig.isFastForwardBackwardEnabled && !playerConfig.isLiveStream) {
                    AnimatedClickableIcon(
                        painterRes = playerConfig.fastForwardIconResource,
                        imageVector = Icons.Filled.FastForward,
                        contentDescription = "Fast Forward",
                        tint = playerConfig.iconsTintColor,
                        iconSize = playerConfig.fastForwardBackwardIconSize,
                        animationDuration = playerConfig.controlClickAnimationDuration,
                        onClick = { onForwardToggle() }
                    )
                }
                Spacer(modifier = Modifier.weight(2f))
            }
        }
    }
}


