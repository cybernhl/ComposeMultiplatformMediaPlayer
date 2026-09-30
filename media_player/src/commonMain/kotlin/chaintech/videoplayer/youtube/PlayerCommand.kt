package chaintech.videoplayer.youtube

import kotlin.time.Duration

sealed interface PlayerCommand {
    fun toJsCommand(): String

    data class LoadVideo(val videoId: String, val startAt: Duration = Duration.ZERO) : PlayerCommand {
        override fun toJsCommand(): String = "loadVideo('$videoId', ${startAt.inWholeSeconds});"
    }

    object Play : PlayerCommand {
        override fun toJsCommand(): String = "playVideo();"
    }

    object Pause : PlayerCommand {
        override fun toJsCommand(): String = "pauseVideo();"
    }

    data class SeekTo(val position: Duration) : PlayerCommand {
        override fun toJsCommand(): String = "seekTo(${position.inWholeSeconds});"
    }

    data class SeekBy(val offset: Duration) : PlayerCommand {
        override fun toJsCommand(): String = "seekBy(${offset.inWholeSeconds});"
    }

    object Mute : PlayerCommand {
        override fun toJsCommand(): String = "mute();"
    }

    object Unmute : PlayerCommand {
        override fun toJsCommand(): String = "unMute();"
    }

    data class PlaybackSpeed(val rate: Float) : PlayerCommand {
        override fun toJsCommand(): String = "setPlaybackRate($rate);"
    }

    data class Volume(val percent: Int) : PlayerCommand {
        override fun toJsCommand(): String = "setVolume($percent);"
    }

    data class SetFullScreen(val isFullScreen: Boolean) : PlayerCommand {
        override fun toJsCommand(): String = "setFullscreenUI($isFullScreen);"
    }

    data class HideControlsUI(val hideControls: Boolean, val hideBack: Boolean) : PlayerCommand {
        override fun toJsCommand(): String = "setControlsUIHidden($hideControls, $hideBack);"
    }
}