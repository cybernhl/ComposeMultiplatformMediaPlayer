package chaintech.videoplayer.util

import chaintech.videoplayer.host.MediaPlayerHost
import chaintech.videoplayer.model.VideoPlayerConfig

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class PlaybackPreference {
    fun savePlaybackPosition(videoUrl: String, position: Float)
    fun getPlaybackPosition(videoUrl: String): Float

    companion object {
        fun getInstance(): PlaybackPreference?
    }
}

internal fun saveCurrentPosition(host: MediaPlayerHost, url: String) {
    val currentTime = host.currentTime
    val totalTime = host.totalTime
    val threshold = when {
        totalTime >= 3600 -> 0.95
        totalTime <= 60 -> 0.85
        else -> 0.9
    }

    if (currentTime > 0 && currentTime < (totalTime * threshold)) {
        PlaybackPreference.getInstance()
            ?.savePlaybackPosition(url, currentTime)
    }
}

internal fun getSeekTime(host: MediaPlayerHost, config: VideoPlayerConfig): Float? {
    if (host.totalTime > 0 && !config.isLiveStream) {
        if(host.playFromTime != null) {
            val time = host.playFromTime
            host.playFromTime = null
            PlaybackPreference.getInstance()?.savePlaybackPosition(host.url, 0f)
            return time
        } else {
            if(config.enableResumePlayback) {
                val lastTime =
                    PlaybackPreference.getInstance()?.getPlaybackPosition(host.url)
                lastTime?.let {
                    PlaybackPreference.getInstance()?.savePlaybackPosition(host.url, 0f)
                    if (lastTime > 0) {
                        return lastTime
                    }
                }
            }
        }
    }
    return null
}