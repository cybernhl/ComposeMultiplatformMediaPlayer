package chaintech.videoplayer.util

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import chaintech.videoplayer.host.DrmConfig
import chaintech.videoplayer.host.MediaPlayerError
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
    qualityList: ((List<VideoQuality>) -> Unit)
) {
    val player = remember {
        println("Initializing JavaCvPlayer (FFmpeg) for Desktop")
        val videoSink = SkiaVideoSink()
        val audioSink = JvmAudioSink()
        val ffmpegOptions = getPlatformHardwareDecoderOption() + mapOf(
//            "rtsp_transport" to "tcp",     // 強制使用 TCP，避免 UDP 丟包花屏
            "rtsp_transport" to "udp",
            "stimeout" to "5000000",        // 連線超時時間 (微秒: 5 秒)
            "probesize" to "1000000",       // 減少探測標頭大小，加快首幀開播
            "analyzeduration" to "1000000", // 減少分析時間
            "timeout" to "10000000",        // 注意：HTTP 使用 "timeout" 而不是 "stimeout"
            "reconnect" to "1",             // 斷線自動重連
            "reconnect_streamed" to "1",
            "reconnect_delay_max" to "5",
            "user_agent" to "Mozilla/5.0 (Linux; Android 16; Pixel 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Mobile Safari/537.36",
            "fflags" to "nobuffer"          // 不緩衝，達到即時低延遲
        )
        JavaCvPlayer.Builder()
            .setVideoSink(videoSink)
            .setAudioSink(audioSink)
            .setFFmpegOptions(ffmpegOptions)
            .build()
    }

    val effectiveUrl = selectedQuality?.url?.takeIf { it.isNotBlank() } ?: url

    LaunchedEffect(effectiveUrl) {
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
