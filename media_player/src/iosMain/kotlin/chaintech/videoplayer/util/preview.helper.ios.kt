package chaintech.videoplayer.util

import androidx.compose.ui.graphics.ImageBitmap
import chaintech.videoplayer.extension.toImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVAsset
import platform.AVFoundation.AVAssetImageGenerator
import platform.AVFoundation.CMTimeValue
import platform.AVFoundation.valueWithCMTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSValue
import platform.UIKit.UIImage


@OptIn(ExperimentalForeignApi::class)
actual suspend fun extractFrames(
    videoPath: String,
    frameCount: Int
): List<ImageBitmap> {
    return withContext(Dispatchers.IO) {
        val frames = mutableListOf<ImageBitmap>()
        if(frameCount <= 0) return@withContext frames

        val asset = AVAsset.assetWithURL(createUrl(url = videoPath)!!)
        val generator = AVAssetImageGenerator.assetImageGeneratorWithAsset(asset)
        generator.requestedTimeToleranceBefore = CMTimeMake(0, 600)
        generator.requestedTimeToleranceAfter = CMTimeMake(0, 600)

        val duration = CMTimeGetSeconds(asset.duration)
        if (duration <= 0.0) return@withContext frames

        val times: List<NSValue> = when {
            frameCount == 1 -> {
                val safeOffset = minOf(duration * 0.1, duration - 0.5).coerceAtLeast(0.0)
                listOf(
                    NSValue.valueWithCMTime(
                        CMTimeMakeWithSeconds(safeOffset, preferredTimescale = 600)
                    )
                )
            }
            else -> {
                val interval = duration / frameCount.toDouble()
                List(frameCount) { i ->
                    val seconds = i * interval
                    val time = CMTimeMakeWithSeconds(seconds, preferredTimescale = 600)
                    NSValue.valueWithCMTime(time)
                }
            }
        }

        for (time in times) {
            try {
                val cgImage = generator.copyCGImageAtTime(time.CMTimeValue, actualTime = null, error = null)
                cgImage?.let {
                    val uiImage = UIImage.imageWithCGImage(cgImage = it)
                    val bitmap = uiImage.toImageBitmap() // Convert UIImage to ImageBitmap
                    frames.add(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return@withContext frames
    }
}