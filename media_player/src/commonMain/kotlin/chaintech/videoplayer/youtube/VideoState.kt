package chaintech.videoplayer.youtube

import kotlin.time.Duration

sealed class VideoState {
    object Idle : VideoState()
    object Initialized : VideoState()

    data class Playing(
        val videoId: String,
        val totalDuration: Duration = Duration.ZERO,
        val currentTime: Duration = Duration.ZERO,
        val isPlaying: Boolean = false,
        val volumeLevel: Float? = null,
        val playBackRate: Float? = null,
        val isMute: Boolean? = null,
        val isPause: Boolean? = null,
        val isFullScreen: Boolean? = null,
        val backHandler: Boolean? = null,
        val playbackStatus: PlayerEvent.State.VideoState = PlayerEvent.State.VideoState.UNSTARTED
    ) : VideoState()

    data class Failed(val errorMessage: String) : VideoState()
}
