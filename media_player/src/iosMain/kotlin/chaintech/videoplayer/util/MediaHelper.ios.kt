package chaintech.videoplayer.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVKeyValueStatusLoaded
import platform.AVFoundation.AVURLAsset
import platform.CoreMedia.CMTimeGetSeconds


@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun FetchTotalDuration(url: String, onDurationRetrieved: (Double) -> Unit) {
    val updatedCallback = rememberUpdatedState(onDurationRetrieved)

    val urlObject = remember(url) { createUrl(url) }
    if (urlObject == null) {
        LaunchedEffect(Unit) { updatedCallback.value(0.0) } // Ensure callback is invoked
        return
    }

    LaunchedEffect(urlObject) {
        val asset = AVURLAsset(uRL = urlObject, options = null)
        asset.loadValuesAsynchronouslyForKeys(listOf("duration")) {
            val durationStatus = asset.statusOfValueForKey("duration", null)
            val duration = if (durationStatus == AVKeyValueStatusLoaded) {
                CMTimeGetSeconds(asset.duration)
            } else {
                0.0 // Return 0 if fetching fails
            }
            updatedCallback.value(duration)
        }
    }
}
