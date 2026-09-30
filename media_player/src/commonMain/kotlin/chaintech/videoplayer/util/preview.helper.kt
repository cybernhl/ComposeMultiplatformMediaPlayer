package chaintech.videoplayer.util

import androidx.compose.ui.graphics.ImageBitmap

expect suspend fun extractFrames(videoPath: String, frameCount: Int): List<ImageBitmap>