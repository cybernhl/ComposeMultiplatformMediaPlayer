package chaintech.videoplayer.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bytedeco.javacv.FFmpegFrameGrabber

@Composable
internal actual fun FetchTotalDuration(url: String, onDurationRetrieved: (Double) -> Unit) {
    LaunchedEffect(url) {
        if (url.isBlank()) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            val grabber = FFmpegFrameGrabber(url)
            try {
                // 設定超時防止網路影片導致 UI 卡死 (單位為微秒)
                grabber.setOption("timeout", "5000000")
                grabber.start()
                val durationSec = grabber.lengthInTime / 1_000_000.0
                onDurationRetrieved(durationSec)
                grabber.stop()
            } catch (e: Exception) {
                println("⚠️ JavaCv: Failed to fetch duration for $url - ${e.message}")
                onDurationRetrieved(0.0)
            } finally {
                try {
                    grabber.release()
                } catch (e: Exception) {
                    // Ignore release errors
                }
            }
        }
    }
}
