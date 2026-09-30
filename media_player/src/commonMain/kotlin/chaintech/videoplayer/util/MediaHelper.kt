package chaintech.videoplayer.util

import androidx.compose.runtime.Composable


@Composable
fun RetrieveMediaDuration(url: String, onDurationRetrieved: (Double) -> Unit) {
    FetchTotalDuration(url, onDurationRetrieved)
}

@Composable
internal expect fun FetchTotalDuration(url: String, onDurationRetrieved: (Double) -> Unit)
