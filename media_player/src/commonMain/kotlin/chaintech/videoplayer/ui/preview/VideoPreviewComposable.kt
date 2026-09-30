package chaintech.videoplayer.ui.preview

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import chaintech.videoplayer.util.extractFrames
import chaintech.videoplayer.util.rememberAppBackgroundObserver
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun VideoPreviewComposable(
    url: String,
    loadingIndicatorColor: Color = Color.White,
    frameCount: Int = 5,
    contentScale: ContentScale = ContentScale.Crop
) {
    val coroutineScope = rememberCoroutineScope()
    val frames = remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }
    var currentJob by remember { mutableStateOf<Job?>(null) }


    // Launch asynchronous task to extract frames
    LaunchedEffect(url, frameCount) {
        isLoading.value = true
        currentJob?.cancel()
        currentJob = coroutineScope.launch {
            try {
                val extractedFrames = extractFrames(url, frameCount)
                frames.value = extractedFrames
            } catch (e: Exception) {
                // Handle any error that may occur
                e.printStackTrace()
                frames.value = emptyList()
            } finally {
                isLoading.value = false
            }
        }
    }

    DisposableEffect(url) {
        onDispose {
            currentJob?.cancel()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading.value) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = loadingIndicatorColor
            )
        } else {
            if (frames.value.isNotEmpty()) {
                if (frames.value.size > 1) {
                    VideoPreview(frames = frames.value, scale = contentScale)
                } else {
                    Image(
                        bitmap = frames.value.first(),
                        contentDescription = "Static Thumbnail",
                        contentScale = contentScale,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}


@Composable
private fun VideoPreview(frames: List<ImageBitmap>, scale: ContentScale) {
    val frameIndex = remember { Animatable(0f) }
    val appBackgroundObserver = rememberAppBackgroundObserver()
    var isRunning by remember { mutableStateOf(true) }

    // Start/Stop animation on background/foreground
    LaunchedEffect(Unit) {
        appBackgroundObserver.observe { isBackground ->
            isRunning = !isBackground
        }
    }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            frameIndex.animateTo(
                targetValue = frames.size.toFloat(),
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            appBackgroundObserver.removeObserver()
        }
    }
    Image(
        bitmap = frames[frameIndex.value.toInt() % frames.size],
        contentDescription = "Preview Frame",
        contentScale = scale,
        modifier = Modifier.fillMaxSize()
    )
}
