package chaintech.videoplayer.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import chaintech.videoplayer.model.Chapter

@Composable
fun CustomSeekBar(
    modifier: Modifier = Modifier,
    progress: Float,
    maxProgress: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    thumbRadius: Dp,
    trackHeight: Dp,
    activeTrackColor: Color = Color.Red,
    inactiveTrackColor: Color = Color.Gray,
    thumbColor: Color = Color.White,
    rippleColor: Color = Color.White.copy(alpha = 0.3f), // Soft glow effect
    showThumbAlways: Boolean = false,
    chapters: List<Chapter>? = null
) {
    val density = LocalDensity.current
    var isDragging by remember { mutableStateOf(false) }
    var localProgress by remember { mutableStateOf(progress) }
    var trackWidth by remember { mutableStateOf(1f) }
    var dragStartOffsetX by remember { mutableStateOf(0f) }
    var initialProgress by remember { mutableStateOf(0f) }

    // Animate ripple radius when dragging
    val rippleRadius by animateFloatAsState(
        targetValue = if (isDragging) with(density) { (thumbRadius * 2f).toPx() } else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "Ripple Animation"
    )

    LaunchedEffect(progress, maxProgress) {
        if (!isDragging) {
            localProgress = progress
        }
    }

    Box(
        modifier = modifier
            .height(thumbRadius * 2)
            .onSizeChanged { newSize -> trackWidth = newSize.width.toFloat() }
            .pointerInput(maxProgress) {
                detectTapGestures { offset ->
                    val newValue = (offset.x / trackWidth) * maxProgress
                    localProgress = newValue.coerceIn(0f, maxProgress)
                    onValueChange(localProgress)
                    onValueChangeFinished()
                }
            }
            .pointerInput(maxProgress) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragStartOffsetX = offset.x
                        initialProgress = (dragStartOffsetX / trackWidth) * maxProgress
                        localProgress = initialProgress.coerceIn(0f, maxProgress)
                        onValueChange(localProgress)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val dragDelta = (dragAmount.x / trackWidth) * maxProgress
                        localProgress = (localProgress + dragDelta).coerceIn(0f, maxProgress)
                        onValueChange(localProgress)
                    },
                    onDragEnd = {
                        isDragging = false
                        onValueChangeFinished()
                    },
                    onDragCancel = { isDragging = false }
                )
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .align(Alignment.CenterStart)
        ) {
            val trackWidthPx = size.width
            val trackHeightPx = with(density) { trackHeight.toPx() }
            val thumbPx = with(density) { thumbRadius.toPx() }

            val progressX = (localProgress / maxProgress) * trackWidthPx
            val centerY = size.height / 2

            /* ---------- CASE 1: No chapters → normal seekbar ---------- */
            if (chapters.isNullOrEmpty()) {

                // Inactive track
                drawLine(
                    color = inactiveTrackColor,
                    start = Offset(0f, centerY),
                    end = Offset(trackWidthPx, centerY),
                    strokeWidth = trackHeightPx
                )

                // Active track
                drawLine(
                    color = activeTrackColor,
                    start = Offset(0f, centerY),
                    end = Offset(progressX, centerY),
                    strokeWidth = trackHeightPx
                )
            }
            /* ---------- CASE 2: Chapters present → segmented ---------- */
            else {

                val gapPx = trackHeightPx * 0.8f
                val cornerRadiusPx = trackHeightPx / 2f

                // Build safe boundaries
                val chapterPoints = buildList {
                    add(0f)
                    chapters.forEach { add(it.startTime / 1000f) }
                    add(maxProgress)
                }.distinct().sorted()

                val segments = chapterPoints.zipWithNext()

                segments.forEach { (start, end) ->
                    val startX = (start / maxProgress) * trackWidthPx
                    val endX = (end / maxProgress) * trackWidthPx

                    val left = startX + gapPx / 2
                    val right = endX - gapPx / 2
                    val width = (right - left).coerceAtLeast(0f)

                    if (width <= 0f) return@forEach

                    // Inactive segment
                    drawRoundRect(
                        color = inactiveTrackColor,
                        topLeft = Offset(left, centerY - trackHeightPx / 2),
                        size = Size(width, trackHeightPx),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                    )

                    // Active overlay
                    val activeWidth = (progressX - left).coerceIn(0f, width)
                    if (activeWidth > 0f) {
                        drawRoundRect(
                            color = activeTrackColor,
                            topLeft = Offset(left, centerY - trackHeightPx / 2),
                            size = Size(activeWidth, trackHeightPx),
                            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                        )
                    }
                }
            }

            /* ---------- Ripple ---------- */
            if (isDragging && rippleRadius > 0f) {
                drawCircle(
                    color = rippleColor,
                    radius = rippleRadius,
                    center = Offset(progressX, centerY)
                )
            }

            /* ---------- Thumb ---------- */
            if (isDragging || showThumbAlways) {
                drawCircle(
                    color = thumbColor,
                    radius = thumbPx,
                    center = Offset(progressX, centerY)
                )
            }
        }
    }
}


