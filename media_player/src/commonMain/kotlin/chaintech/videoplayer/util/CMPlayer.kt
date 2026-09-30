package chaintech.videoplayer.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chaintech.videoplayer.host.DrmConfig
import chaintech.videoplayer.host.MediaPlayerError
import chaintech.videoplayer.model.ScreenResize

@Composable
internal expect fun CMPPlayer(
    modifier: Modifier,
    url: String,
    isPause: Boolean,
    totalTime: ((Float) -> Unit),
    currentTime: ((Float) -> Unit),
    isSliding: Boolean,
    seekToTime: Float?,
    speed: Float,
    size: ScreenResize,
    bufferCallback: ((Boolean) -> Unit),
    didEndVideo: (() -> Unit),
    loop: Boolean,
    volume: Float,
    isLiveStream: Boolean,
    isPipMode: Boolean,
    onPipModeChanged: (Boolean) -> Unit,
    error: (MediaPlayerError) -> Unit,
    headers: Map<String, String>?,
    drmConfig: DrmConfig?,
    selectedQuality: VideoQuality?,
    selectedAudioTrack: AudioTrack?,
    selectedSubTitle: SubtitleTrack?,
    audioList: ((List<AudioTrack>) -> Unit),
    subtitlesList: ((List<SubtitleTrack>) -> Unit),
    qualityList: ((List<VideoQuality>) -> Unit)
)