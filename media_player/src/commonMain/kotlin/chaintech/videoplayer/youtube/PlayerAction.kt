package chaintech.videoplayer.youtube

internal enum class PlayerAction(val action: String) {
    READY("onReady"),
    ERROR("onError"),
    VIDEO_LENGTH("onVideoDuration"),
    STATE_UPDATE("onStateChange"),
    PROGRESS("onCurrentTimeChange"),
    VIDEO_ID("onVideoId"),
    RATE_UPDATE("onPlaybackRateChange"),
    MUTE_UPDATE("onMuteChange"),
    PAUSE_CHANGE("onPauseChange"),
    FULLSCREEN_TOGGLE("onFullscreenToggle"),
    BACK_ACTION("onBackPressed"),
    VOLUME_CHANGE("onVolumeChange");



    companion object {
        fun fromAction(name: String): PlayerAction? {
            return entries.firstOrNull { it.action == name }
        }
    }
}