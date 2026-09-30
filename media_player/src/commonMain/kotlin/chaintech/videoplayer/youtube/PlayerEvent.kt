package chaintech.videoplayer.youtube

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

sealed class PlayerEvent {
    object Ready : PlayerEvent()

    data class Error(val reason: String) : PlayerEvent()

    data class DurationChanged(val duration: Duration) : PlayerEvent() {
        companion object {
            fun parse(value: String) = value.toDoubleOrNull()?.toDuration(DurationUnit.SECONDS)?.let(::DurationChanged)
        }
    }

    data class State(val status: VideoState) : PlayerEvent() {
        enum class VideoState(val label: String) {
            UNSTARTED("UNSTARTED"),
            ENDED("ENDED"),
            PLAYING("PLAYING"),
            PAUSED("PAUSED"),
            BUFFERING("BUFFERING"),
            QUEUED("CUED");
        }
        companion object {
            fun parse(value: String) = VideoState.entries.firstOrNull { it.label == value }?.let(::State)
        }
    }

    data class Progress(val position: Duration) : PlayerEvent() {
        companion object {
            fun parse(value: String) = value.toDoubleOrNull()?.toDuration(DurationUnit.SECONDS)?.let(::Progress)
        }
    }

    data class VolumeChange(val volume: Float) : PlayerEvent() {
        companion object {
            fun parse(value: String) = value.toFloatOrNull()?.let(::VolumeChange)
        }
    }

    data class RateChange(val rate: Float) : PlayerEvent() {
        companion object {
            fun parse(value: String) = value.toFloatOrNull()?.let(::RateChange)
        }
    }
    data class MuteChange(val isMute: Boolean) : PlayerEvent() {
        companion object {
            fun parse(value: String) = value.toBooleanStrictOrNull()?.let(::MuteChange)
        }
    }
    data class PauseChange(val isPause: Boolean) : PlayerEvent() {
        companion object {
            fun parse(value: String) = value.toBooleanStrictOrNull()?.let(::PauseChange)
        }
    }

    object FullScreenToggle : PlayerEvent()

    object BackButtonAction : PlayerEvent()
    data class VideoId(val id: String) : PlayerEvent() {
        companion object {
            fun parse(value: String?) = value?.let(::VideoId)
        }
    }

    companion object {
        internal fun create(event: PlayerAction?, data: String) = when (event) {
            PlayerAction.READY -> Ready
            PlayerAction.ERROR -> Error(data)
            PlayerAction.VIDEO_LENGTH -> DurationChanged.parse(data)
            PlayerAction.STATE_UPDATE -> State.parse(data)
            PlayerAction.PROGRESS -> Progress.parse(data)
            PlayerAction.VIDEO_ID -> VideoId.parse(data)
            PlayerAction.VOLUME_CHANGE -> VolumeChange.parse(data)
            PlayerAction.RATE_UPDATE -> RateChange.parse(data)
            PlayerAction.MUTE_UPDATE -> MuteChange.parse(data)
            PlayerAction.PAUSE_CHANGE -> PauseChange.parse(data)
            PlayerAction.FULLSCREEN_TOGGLE -> FullScreenToggle
            PlayerAction.BACK_ACTION -> BackButtonAction
            null -> null
        }
    }
}
