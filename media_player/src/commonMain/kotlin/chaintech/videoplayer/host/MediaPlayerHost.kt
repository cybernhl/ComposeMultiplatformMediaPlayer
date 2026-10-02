package chaintech.videoplayer.host

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import chaintech.videoplayer.model.MediaEngineConfig
import chaintech.videoplayer.model.PlayerSpeed
import chaintech.videoplayer.model.ScreenResize
import chaintech.videoplayer.util.AudioTrack
import chaintech.videoplayer.util.M3U8Helper
import chaintech.videoplayer.util.SubtitleTrack
import chaintech.videoplayer.util.VideoQuality
import chaintech.videoplayer.util.isHlsUrl
import chaintech.videoplayer.util.provideDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaPlayerHost(
    mediaUrl: String = "",
    autoPlay: Boolean = true,
    isMuted: Boolean = false,
    initialSpeed: PlayerSpeed = PlayerSpeed.X1,
    initialVideoFitMode: ScreenResize = ScreenResize.FILL,
    isLooping: Boolean = true,
    startTimeInSeconds: Float? = null,
    isFullScreen: Boolean = false,
    headers: Map<String, String>? = null,
    drmConfig: DrmConfig? = null,
    engineConfig: MediaEngineConfig = MediaEngineConfig()
) {
    // Internal states
    internal var url by mutableStateOf(mediaUrl)
    internal var speed by mutableStateOf(initialSpeed)
    internal var videoFitMode by mutableStateOf(initialVideoFitMode)
    internal var seekToTime: Float? by mutableStateOf(null)
    internal var isSliding by mutableStateOf(false)
    internal var isPaused by mutableStateOf(autoPlay.not())
    internal var isMuted by mutableStateOf(isMuted)
    internal var isLooping by mutableStateOf(isLooping)
    internal var totalTime: Float by mutableStateOf(0f) // Total video duration
    internal var currentTime: Float by mutableStateOf(0f) // Current playback position
    internal var isBuffering by mutableStateOf(true)
    internal var playFromTime: Float? by mutableStateOf(startTimeInSeconds)
    internal var volumeLevel by mutableStateOf(if (isMuted) 0f else 1f) // Range 0.0 to 1.0
    internal var isFullScreen by mutableStateOf(isFullScreen)
    internal var isPIP by mutableStateOf(false)
    internal var headers by mutableStateOf(headers)
    internal var drmConfig by mutableStateOf(drmConfig)
    var engineConfig by mutableStateOf(engineConfig)
    internal var retryToken by mutableStateOf(0L)
    var qualityOptions by mutableStateOf(emptyList<VideoQuality>())
    var selectedQuality by mutableStateOf<VideoQuality?>(null)
    var audioTrackOptions by mutableStateOf(emptyList<AudioTrack>())
    var selectedAudioTrack by mutableStateOf<AudioTrack?>(null)
    var subTitlesOptions by mutableStateOf(emptyList<SubtitleTrack>())
    var selectedsubTitle by mutableStateOf<SubtitleTrack?>(null)

    private var lastVolumeLevel by mutableStateOf(1f)

    private val m3u8Helper = M3U8Helper()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var onEvent: ((MediaPlayerEvent) -> Unit)? = null
    var onError: ((MediaPlayerError) -> Unit)? = null

    init {
        fetchAndUpdateMediaInfo(url)
    }

    // Public actions
    fun loadUrl(mediaUrl: String, headers: Map<String, String>? = null, drmConfig: DrmConfig? = null) {
        if (url != mediaUrl) {
            url = mediaUrl
            fetchAndUpdateMediaInfo(mediaUrl)
        }
        this.headers = headers
        this.drmConfig = drmConfig
    }

    fun play() {
        isPaused = false
        onEvent?.invoke(MediaPlayerEvent.PauseChange(isPaused))
    }

    fun pause() {
        isPaused = true
        onEvent?.invoke(MediaPlayerEvent.PauseChange(isPaused))
    }

    fun togglePlayPause() {
        isPaused = !isPaused
        onEvent?.invoke(MediaPlayerEvent.PauseChange(isPaused))
    }

    fun mute() {
        if (!isMuted) {
            lastVolumeLevel = volumeLevel // Store current volume before muting
            volumeLevel = 0f
            isMuted = true
            onEvent?.invoke(MediaPlayerEvent.MuteChange(isMuted))
        }
    }

    fun unmute() {
        if (isMuted) {
            volumeLevel = lastVolumeLevel // Restore previous volume
            isMuted = false
            onEvent?.invoke(MediaPlayerEvent.MuteChange(isMuted))
        }
    }

    fun toggleMuteUnmute() {
        if (isMuted) {
            unmute()
        } else {
            mute()
        }
    }

    fun setSpeed(speed: PlayerSpeed) {
        this.speed = speed
    }

    @Deprecated(
        message = "Use seekTo(seconds: Float?) instead for better precision.",
        replaceWith = ReplaceWith("seekTo(seconds.toFloat())")
    )
    fun seekTo(seconds: Int?) {
        isSliding = true
        seekToTime = seconds?.toFloat()
        isSliding = false
    }

    fun seekTo(seconds: Float?) {
        isSliding = true
        seekToTime = seconds
        isSliding = false
    }

    fun setVideoFitMode(mode: ScreenResize) {
        videoFitMode = mode
    }

    fun setLooping(isLooping: Boolean) {
        this.isLooping = isLooping
    }

    fun toggleLoop() {
        this.isLooping = !this.isLooping
    }

    fun setVolume(level: Float) {
        volumeLevel = level.coerceIn(0f, 1f)
        if (!isMuted) {
            lastVolumeLevel = volumeLevel // Update last volume only if not muted
        }
    }

    fun setFullScreen(isFullScreen: Boolean) {
        this.isFullScreen = isFullScreen
        onEvent?.invoke(MediaPlayerEvent.FullScreenChange(isFullScreen))
    }

    fun toggleFullScreen() {
        this.isFullScreen = !this.isFullScreen
        onEvent?.invoke(MediaPlayerEvent.FullScreenChange(this.isFullScreen))
    }

    internal fun enterPIP() {
        this.isPIP = true
        onEvent?.invoke(MediaPlayerEvent.PIPChange(this.isPIP))
    }

    internal fun exitPIP() {
        this.isPIP = false
        onEvent?.invoke(MediaPlayerEvent.PIPChange(this.isPIP))
    }

    fun setVideoQuality(quality: VideoQuality?) {
        this.selectedQuality = quality
    }

    fun updateVideoQualityOptions(options: List<VideoQuality>) {
        this.qualityOptions = options
    }

    fun setAudioTrack(track: AudioTrack?) {
        this.selectedAudioTrack = track
    }

    fun updateAudioTrackOptions(options: List<AudioTrack>) {
        this.audioTrackOptions = options
        val default = options.firstOrNull{ it.isDefault }
        default?.let {
            setAudioTrack(it)
        }
    }

    fun setSubTitle(subTitle: SubtitleTrack?) {
        this.selectedsubTitle = subTitle
    }

    fun updateSubTitleOptions(options: List<SubtitleTrack>) {
        this.subTitlesOptions = options
        val default = options.firstOrNull{ it.isDefault }
        default?.let {
            setSubTitle(it)
        }
    }

    internal fun setBufferingStatus(isBuffering: Boolean) {
        this.isBuffering = isBuffering
        onEvent?.invoke(MediaPlayerEvent.BufferChange(isBuffering))
    }

    // Internal-only setters for time values
    internal fun updateTotalTime(time: Float) {
        if (totalTime != time) {
            totalTime = time
            onEvent?.invoke(MediaPlayerEvent.TotalTimeChange(totalTime))
        }
    }

    internal fun updateCurrentTime(time: Float) {
        if(currentTime != time) {
            currentTime = time
            onEvent?.invoke(MediaPlayerEvent.CurrentTimeChange(currentTime))
        }
    }

    internal fun triggerMediaEnd() {
        onEvent?.invoke(MediaPlayerEvent.MediaEnd)
    }

    internal fun triggerError(error: MediaPlayerError) {
        onError?.invoke(error)
    }

    fun fetchAndUpdateMediaInfo(videoUrl: String) {
        resetMetadata()
        if (isHlsUrl(videoUrl)) {
            scope.launch(provideDispatcher()) {
                try {
                    val m3u8Data = m3u8Helper.fetchM3U8Data(videoUrl)
                    withContext(Dispatchers.Main) {
                        updateVideoQualityOptions(m3u8Data.videoQualities)
                        updateAudioTrackOptions(m3u8Data.audioTracks)
                        updateSubTitleOptions(m3u8Data.subtitleTracks)
                    }
                } catch (e: Exception) {
                    println("⚠️ Failed to fetch M3U8 data: ${e.message}")
                }
            }
        }
    }

    fun resetMetadata() {
        setVideoQuality(null)
        setAudioTrack(null)
        setSubTitle(null)
        updateVideoQualityOptions(emptyList())
        updateAudioTrackOptions(emptyList())
        updateSubTitleOptions(emptyList())
    }

    fun reload() {
        resetMetadata()
        fetchAndUpdateMediaInfo(url)
        isPaused = false
        setBufferingStatus(true)
        retryToken++
    }
}
