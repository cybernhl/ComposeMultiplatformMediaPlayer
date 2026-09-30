package chaintech.videoplayer.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import chaintech.videoplayer.model.Platform
import kotlinx.coroutines.CoroutineDispatcher

expect fun formatMinSec(value: Float): String

internal expect val youtubeProgressColor: Color

@Composable
internal expect fun LandscapeOrientation(
    enableFullEdgeToEdge: Boolean,
    isLandscape: Boolean,
    content: @Composable () -> Unit
)

internal fun extractYouTubeVideoId(url: String): String? {
    val regex = Regex(
        "https?:\\/\\/(?:www\\.|m\\.)?youtu(?:\\.be\\/|be\\.com\\/(?:watch\\?v=|embed\\/|v\\/|e\\/|live\\/|shorts\\/|user\\/))([^&#?\\n]+)"
    )
    return regex.find(url)?.groups?.get(1)?.value
}

fun isMobile(): Boolean {
    return isPlatform() == Platform.Ios || isPlatform() == Platform.Android
}
fun isDesktop(): Boolean {
    return isPlatform() == Platform.Desktop
}

fun isHlsUrl(url: String): Boolean {
    return url.trim().lowercase().endsWith(".m3u8")
}

expect fun isPlatform(): Platform

expect fun provideDispatcher(): CoroutineDispatcher
