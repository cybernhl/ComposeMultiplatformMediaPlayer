package chaintech.videoplayer.util

import android.media.MediaMetadataRetriever
import androidx.annotation.OptIn
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
actual suspend fun extractFrames(
    videoPath: String,
    frameCount: Int
): List<ImageBitmap> {
    return withContext(Dispatchers.IO) {
        val frames = mutableListOf<ImageBitmap>()
        if (frameCount <= 0) {
            return@withContext frames
        }

        val retriever = MediaMetadataRetriever()
        try {
            // Attempt to set the data source and handle any exceptions gracefully
            retriever.setDataSource(videoPath)

            // Get video duration and calculate the time interval for frame extraction
            val duration =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            if (duration == 0L) {
                // Handle case where the video duration couldn't be retrieved (invalid URL or empty video)
                return@withContext frames
            }
            val timesInMs = when {
                frameCount == 1 -> {
                    // Skip the first 0–1 second for better thumbnail quality
                    val safeOffset = (duration * 0.1).toLong().coerceAtMost(duration - 500)
                    listOf(safeOffset)
                }

                else -> {
                    val interval = duration / frameCount.toDouble()
                    List(frameCount) { i -> (i * interval).toLong() }
                }
            }

            for (timeInMs in timesInMs) {
                try {
                    val bitmap = retriever.getFrameAtTime(
                        timeInMs * 1000, // microseconds
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                    bitmap?.let { frames.add(it.asImageBitmap()) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            // Handle any exceptions during the setup phase (e.g., invalid URL)
            e.printStackTrace()
        } finally {
            // Ensure that the retriever is released regardless of success or failure
            retriever.release()
        }
        return@withContext frames
    }
}

