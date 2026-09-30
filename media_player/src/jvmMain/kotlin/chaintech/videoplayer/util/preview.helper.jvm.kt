package chaintech.videoplayer.util

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun extractFrames(
    videoPath: String,
    frameCount: Int
): List<ImageBitmap> = withContext(Dispatchers.IO) {
    val frames = mutableListOf<ImageBitmap>()
    return@withContext frames
}
