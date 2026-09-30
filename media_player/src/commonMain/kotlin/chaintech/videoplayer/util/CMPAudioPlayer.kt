package chaintech.videoplayer.util

import androidx.compose.runtime.Composable
import chaintech.videoplayer.host.MediaPlayerError
import chaintech.videoplayer.model.PlayerSpeed
import androidx.compose.ui.Modifier

@Composable
internal expect fun CMPAudioPlayer(
    modifier: Modifier,
    url: String,
    isPause: Boolean,
    totalTime: ((Float) -> Unit),
    currentTime: ((Float) -> Unit),
    isSliding: Boolean,
    seekToTime: Float?,
    loop: Boolean,
    loadingState: (Boolean) -> Unit,
    speed: Float,
    volume: Float,
    didEndAudio: () -> Unit,
    error: (MediaPlayerError) -> Unit,
    headers: Map<String, String>?,
)