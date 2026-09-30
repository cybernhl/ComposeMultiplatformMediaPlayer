package chaintech.videoplayer.ui.video.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import chaintech.videoplayer.model.WatermarkConfig
import kotlinx.coroutines.delay

@Composable
internal fun MovingWatermark(
    modifier: Modifier = Modifier,
    config: WatermarkConfig
) {
    var parentSize by remember { mutableStateOf(IntSize.Zero) }
    var watermarkSize by remember { mutableStateOf(IntSize(0, 0)) }
    var offset by remember { mutableStateOf(IntOffset.Zero) }
    var visible by remember { mutableStateOf(true) }

    // Updates offset to a random position within parent bounds
    fun randomOffset(): IntOffset {
        val maxX = (parentSize.width - watermarkSize.width).coerceAtLeast(0)
        val maxY = (parentSize.height - watermarkSize.height).coerceAtLeast(0)

        return IntOffset(
            x = (0..maxX).random(),
            y = (0..maxY).random()
        )
    }

    // Animation logic
    LaunchedEffect(config) {
        while (true) {
            visible = true
            offset = randomOffset()
            delay(config.stayDelay)
            visible = false
            delay(config.hideDelay)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { parentSize = it }
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .offset { offset }
                    .onSizeChanged { watermarkSize = it }
                    .zIndex(999f)
            ) {
                config.content()
            }
        }
    }
}