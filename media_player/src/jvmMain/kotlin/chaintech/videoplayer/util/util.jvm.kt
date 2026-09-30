package chaintech.videoplayer.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import chaintech.videoplayer.model.Platform
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Suppress("DefaultLocale")
actual fun formatMinSec(value: Float): String {
    return if (value == 0f) {
        "00:00"
    } else {
        val hour = (value / 3600).toInt()
        val remainingSecondsAfterHours = (value % 3600).toInt()
        val minutes = remainingSecondsAfterHours / 60
        val seconds = remainingSecondsAfterHours % 60

        // Format the output string
        return if (hour > 0) {
            String.format("%02d:%02d:%02d", hour, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}

internal actual val youtubeProgressColor: Color
    get() = Color(0xFFdddddd)


val LocalWindowState = staticCompositionLocalOf<WindowState?> {
    null
}

@Composable
internal actual fun LandscapeOrientation(
    enableFullEdgeToEdge: Boolean,
    isLandscape: Boolean,
    content: @Composable () -> Unit
) {
    val windowState = LocalWindowState.current

    if (windowState != null) {
        val currentPlacement = windowState.placement

        when {
            // 1. If the user maximized the window, DO NOT override it.
            currentPlacement == WindowPlacement.Maximized -> {
                // Respect the user's choice → do nothing
            }

            // 2. If landscape mode is active and not already fullscreen, enter fullscreen.
            isLandscape && currentPlacement != WindowPlacement.Fullscreen -> {
                windowState.placement = WindowPlacement.Fullscreen
            }

            // 3. If portrait mode AND current state is not user-controlled and not floating, reset to floating.
            !isLandscape && currentPlacement != WindowPlacement.Floating -> {
                // Only apply if not maximized (already handled) or fullscreen
                windowState.placement = WindowPlacement.Floating
            }
        }
    }

    content()
}

actual fun isPlatform(): Platform {
    return Platform.Desktop
}

actual fun provideDispatcher(): CoroutineDispatcher = Dispatchers.IO
