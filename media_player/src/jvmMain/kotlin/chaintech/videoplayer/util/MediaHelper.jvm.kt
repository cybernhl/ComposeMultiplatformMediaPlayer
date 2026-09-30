package chaintech.videoplayer.util

import androidx.compose.runtime.Composable


@Composable
internal actual fun FetchTotalDuration(url: String, onDurationRetrieved: (Double) -> Unit) {
    onDurationRetrieved(0.0)
}
