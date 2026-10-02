package chaintech.videoplayer.util

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import chaintech.videoplayer.host.DrmConfig
import chaintech.videoplayer.host.MediaPlayerError
import chaintech.videoplayer.model.HardwareDecoderMode
import chaintech.videoplayer.model.MediaEngineConfig
import chaintech.videoplayer.model.RtspTransport
import chaintech.videoplayer.model.ScreenResize
import idv.neo.ffmpeg.media.player.core.JavaCvPlayer
import idv.neo.ffmpeg.media.player.core.audio.JvmAudioSink
import idv.neo.ffmpeg.media.player.core.video.VideoSink
import idv.neo.ffmpeg.media.player.core.video.skia.SkiaVideoSink
import idv.neo.ffmpeg.media.player.ui.compose.VideoPlayerCanvas
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
internal actual fun CMPPlayer(
    modifier: Modifier,
    url: String,
    isPause: Boolean,
    totalTime: (Float) -> Unit,
    currentTime: (Float) -> Unit,
    isSliding: Boolean,
    seekToTime: Float?,
    speed: Float,
    size: ScreenResize,
    bufferCallback: (Boolean) -> Unit,
    didEndVideo: () -> Unit,
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
    qualityList: ((List<VideoQuality>) -> Unit),
    engineConfig: MediaEngineConfig,
    retryToken: Long
) {
    val player = remember(engineConfig) {
        println("Initializing JavaCvPlayer (FFmpeg) for Desktop")
        val videoSink = SkiaVideoSink()
        val audioSink = JvmAudioSink()
        val ffmpegOptions = buildJvmFfmpegOptions(engineConfig)
        JavaCvPlayer.Builder()
            .setVideoSink(videoSink)
            .setAudioSink(audioSink)
            .setFFmpegOptions(ffmpegOptions)
            .build()
    }

    val effectiveUrl = selectedQuality?.url?.takeIf { it.isNotBlank() } ?: url

    LaunchedEffect(effectiveUrl, retryToken) {
        if (effectiveUrl.isNotBlank()) {
            val mediaItem = MediaItem.Builder()
                .setUri(effectiveUrl)
                .build()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }

    LaunchedEffect(isPause) {
        player.playWhenReady = !isPause
    }

    LaunchedEffect(volume) {
        player.volume = volume
    }

    LaunchedEffect(speed) {
        player.playbackParameters = PlaybackParameters(speed)
    }

    LaunchedEffect(seekToTime) {
        seekToTime?.let {
            player.seekTo((it * 1000).toLong())
        }
    }

    LaunchedEffect(loop) {
        player.repeatMode = if (loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
    }

    // Sync state back to lambdas
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                bufferCallback(playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_ENDED) {
                    didEndVideo()
                }
            }

            override fun onEvents(player: Player, events: Player.Events) {
                totalTime(player.duration.toFloat() / 1000f)
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (isActive) {
            if (player.isPlaying) {
                currentTime(player.currentPosition.toFloat() / 1000f)
            }
            delay(500)
        }
    }

    VideoPlayerCanvas(
        player = player,
        modifier = modifier,
        renderMode = VideoSink.RenderMode.SKIA,
        contentScale = when (size) {
            ScreenResize.FIT -> ContentScale.Fit
            ScreenResize.FILL -> ContentScale.Crop
        }
    )
}

/**
 * 根據 OS 平台自動選擇對應的硬體加速解碼器：
 * - macOS: VideoToolbox ("h264_videotoolbox")
 * - Windows: NVIDIA NVDEC ("h264_nvdec")
 * - Linux / Intel: QuickSync ("h264_qsv")
 */
private fun getPlatformHardwareDecoderOption(): Map<String, String> {
    val osName = System.getProperty("os.name", "").lowercase()
    val vcodec = when {
        osName.contains("mac") || osName.contains("darwin") -> "h264_videotoolbox"
        osName.contains("win") -> "h264_nvdec"
        osName.contains("nux") || osName.contains("nix") -> "h264_qsv"
        else -> null
    }
    return if (vcodec != null) mapOf("vcodec" to vcodec) else emptyMap()
}

private fun buildJvmFfmpegOptions(engineConfig: MediaEngineConfig): Map<String, String> {
    val defaultHw = when (engineConfig.hardwareDecoderMode) {
        HardwareDecoderMode.FORCE_SOFTWARE -> emptyMap()
        else -> getPlatformHardwareDecoderOption()
    }

    val connectTimeoutUs = (engineConfig.connectTimeoutSeconds * 1_000_000).toString()
    val readTimeoutUs = (engineConfig.readTimeoutSeconds * 1_000_000).toString()

    val options = mutableMapOf<String, String>()
    options.putAll(defaultHw)

    when (engineConfig.preferredRtspTransport) {
        RtspTransport.TCP -> options["rtsp_transport"] = "tcp"
        RtspTransport.UDP -> options["rtsp_transport"] = "udp"
        RtspTransport.AUTO -> {}
    }

    options["stimeout"] = connectTimeoutUs
    options["timeout"] = connectTimeoutUs
    options["rw_timeout"] = readTimeoutUs
    options["probesize"] = "1000000"
    options["analyzeduration"] = "1000000"

    if (engineConfig.enableAutoReconnect) {
        options["reconnect"] = "1"
        options["reconnect_streamed"] = "1"
        options["reconnect_delay_max"] = "5"
    }

    val defaultUserAgent = "Mozilla/5.0 (Linux; Android 16; Pixel 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Mobile Safari/537.36"
    options["user_agent"] = engineConfig.userAgent ?: defaultUserAgent

    if (engineConfig.lowLatencyMode) {
        options["fflags"] = "nobuffer"
    }

    engineConfig.escapeHatch?.customFfmpegOptions?.let { customMap ->
        options.putAll(customMap)
    }

    return options
}
