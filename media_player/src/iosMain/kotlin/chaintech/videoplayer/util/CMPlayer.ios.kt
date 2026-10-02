package chaintech.videoplayer.util

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import chaintech.videoplayer.host.DrmConfig
import chaintech.videoplayer.host.MediaPlayerError
import chaintech.videoplayer.model.ScreenResize
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionModeMoviePlayback
import platform.AVFAudio.setActive
import platform.AVFoundation.AVKeyValueStatusLoaded
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaCharacteristicAudible
import platform.AVFoundation.AVMediaCharacteristicLegible
import platform.AVFoundation.AVMediaSelectionGroup
import platform.AVFoundation.AVMediaSelectionOption
import platform.AVFoundation.AVPlayerActionAtItemEndNone
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemFailedToPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerLayer
import platform.AVFoundation.AVQueuePlayer
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.actionAtItemEnd
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.asset
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.isPlayable
import platform.AVFoundation.mediaSelectionGroupForMediaCharacteristic
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.playbackLikelyToKeepUp
import platform.AVFoundation.preferredPeakBitRate
import platform.AVFoundation.rate
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.selectMediaOption
import platform.AVFoundation.volume
import platform.AVKit.AVPictureInPictureController
import platform.AVKit.AVPictureInPictureControllerDelegateProtocol
import platform.AVKit.AVPlayerViewController
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSError
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import chaintech.videoplayer.model.MediaEngineConfig
import platform.UIKit.UIView
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun CMPPlayer(
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
    qualityList: ((List<VideoQuality>) -> Unit),
    engineConfig: MediaEngineConfig
) {
    /* -------------------- Player -------------------- */
    val player: AVQueuePlayer by remember { mutableStateOf(
        AVQueuePlayer(null).apply {
            this.actionAtItemEnd = AVPlayerActionAtItemEndNone // Prevent automatic replay
        }
    ) }

    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var pause by remember { mutableStateOf(isPause) }

    /* -------------------- Observers refs -------------------- */
    val observerRef = remember { mutableStateOf<NSObject?>(null) }
    val timeObserverRef = remember { mutableStateOf<Any?>(null) }
    val enterForegroundObserverRef = remember { mutableStateOf<NSObject?>(null) }
    val pipDelegateRef = remember { mutableStateOf<NSObject?>(null) }

    /* -------------------- Audio session -------------------- */
    LaunchedEffect(Unit) {
        try {
            val audioSession = AVAudioSession.sharedInstance()

            memScoped {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                val success = audioSession.setCategory(
                    category = AVAudioSessionCategoryPlayback,
                    mode = AVAudioSessionModeMoviePlayback,
                    options = 0u,
                    error = errorPtr.ptr
                )
                if (!success) {
                    println("Failed to set AVAudioSession category: ${errorPtr.value?.localizedDescription}")
                } else {
                    audioSession.setActive(true, null)
                }
            }
        } catch (e: Exception) {
            println("Failed to set AVAudioSession: ${e.message}")
        }
    }

    /* -------------------- Player VC -------------------- */
    val avPlayerViewController = remember {
        AVPlayerViewController().apply {
            showsPlaybackControls = false
            entersFullScreenWhenPlaybackBegins = false
        }
    }
    avPlayerViewController.player = player
    avPlayerViewController.allowsPictureInPicturePlayback = isPipMode
    avPlayerViewController.videoGravity = when (size) {
        ScreenResize.FIT -> AVLayerVideoGravityResizeAspect
        ScreenResize.FILL -> AVLayerVideoGravityResizeAspectFill
    }

//    player.volume = volume

    /* -------------------- PiP -------------------- */
    val pipController = remember {
        if (AVPictureInPictureController.isPictureInPictureSupported() && isPipMode) {
            AVPictureInPictureController(AVPlayerLayer().apply { this.player = player })
        } else null
    }

    /* -------------------- Idle timer -------------------- */

    LaunchedEffect(isPause) {
        UIApplication.sharedApplication.idleTimerDisabled = !isPause
        pause = isPause
    }

    /* -------------------- Buffer callback -------------------- */
    LaunchedEffect(isLoading) {  bufferCallback(isLoading) }

    /* -------------------- Media helpers -------------------- */
    LaunchedEffect(volume) { player.volume = volume }

    fun setPlayerRate(speed: Float) { player.rate = speed }


    fun setQuality(quality: VideoQuality?) {
        quality?.bitrate?.let { bitrate ->
            player.currentItem?.preferredPeakBitRate = bitrate
        } ?: run {
            player.currentItem?.preferredPeakBitRate =
                0.0 // Allows AVPlayer to auto-adjust quality
        }
    }

    fun selectMediaOption(group: AVMediaSelectionGroup, match: (AVMediaSelectionOption) -> Boolean) {
        val option = (group.options as? List<AVMediaSelectionOption>)?.firstOrNull(match)
        player.currentItem?.selectMediaOption(option, group)
    }

    fun setAudioTrack(audioTrack: AudioTrack?) {
        val group = player.currentItem?.asset?.mediaSelectionGroupForMediaCharacteristic(AVMediaCharacteristicAudible) ?: return
        selectMediaOption(group) {
            it.extendedLanguageTag?.equals(audioTrack?.language, true) == true ||
                    it.displayName.equals(audioTrack?.name, true)
        }
    }

    fun setSubTitle(track: SubtitleTrack?) {
        val group = player.currentItem?.asset?.mediaSelectionGroupForMediaCharacteristic(AVMediaCharacteristicLegible) ?: return
        selectMediaOption(group) {
            it.extendedLanguageTag?.equals(track?.language, true) == true ||
                    it.displayName.equals(track?.name, true)
        }
    }

    fun getAvailableSubtitles(): List<SubtitleTrack> {
        val group = player.currentItem?.asset
            ?.mediaSelectionGroupForMediaCharacteristic(AVMediaCharacteristicLegible)
        return (group?.options as? List<AVMediaSelectionOption>)?.map { option ->
            SubtitleTrack(
                language = option.extendedLanguageTag ?: "",
                name = option.displayName,
                groupId = "subtitles", // group identifier
                url = "",
                isDefault = option == group.defaultOption
            )
        }?.distinctBy { it.language + it.name } ?: return emptyList()
    }

    fun getAvailableAudioTracks(): List<AudioTrack> {
        val group = player.currentItem?.asset
            ?.mediaSelectionGroupForMediaCharacteristic(AVMediaCharacteristicAudible)

        val audios = (group?.options as? List<AVMediaSelectionOption>)?.map { option ->
            AudioTrack(
                language = option.extendedLanguageTag ?: "",
                name = option.displayName,
                groupId = "audio",
                url = "",
                isDefault = option == group.defaultOption
            )
        }?.distinctBy { it.language + it.name } ?: emptyList()
        return if (audios.size > 1) audios else emptyList()
    }

    /* -------------------- Observers -------------------- */
    fun cleanupObservers() {
        // Remove time observer first
        timeObserverRef.value?.let { timeObserver ->
            player.removeTimeObserver(timeObserver)
            timeObserverRef.value = null
        }

        // Remove notification observers
        observerRef.value?.let { observer ->
            NSNotificationCenter.defaultCenter().removeObserver(observer)
            observerRef.value = null
        }

        enterForegroundObserverRef.value?.let { foregroundObserver ->
            NSNotificationCenter.defaultCenter().removeObserver(foregroundObserver)
            enterForegroundObserverRef.value = null
        }

        // Cleanup PiP
        pipDelegateRef.value = null
        pipController?.delegate = null
        pipController?.stopPictureInPicture()
    }

    fun setupObservers() {
        val observerObject = object : NSObject() {
            @ObjCAction
            fun onPlayerItemDidPlayToEndTime() {
                player.currentItem?.let { item ->
                    if (loop) {
                        player.seekToTime(CMTimeMakeWithSeconds(0.0, 1))
                        player.play()
                        setPlayerRate(speed)
                    } else {
                        player.pause()

                        player.replaceCurrentItemWithPlayerItem(item)
                        player.seekToTime(CMTimeMakeWithSeconds(0.001, 1000))
                    }
                    didEndVideo()
                }
            }

            @ObjCAction
            fun onPlayerError() {
                val errorMessage =
                    player.currentItem?.error?.localizedDescription ?: "Unknown playback error"
                error(MediaPlayerError.PlaybackError(errorMessage))
            }
        }

        observerRef.value = observerObject

        val enterForegroundObject = object : NSObject() {
            @ObjCAction
            fun willEnterForeground() { if (!pause) {
                player.play()
                setPlayerRate(speed)
            } }
        }

        enterForegroundObserverRef.value = enterForegroundObject

        // Add time observer
        val timeObserver = player.addPeriodicTimeObserverForInterval(
            CMTimeMakeWithSeconds(1.0, 1),
            null
        ) { _ ->
            if (!isSliding && isPlaying) {
                val current = CMTimeGetSeconds(player.currentTime())
                currentTime(current.toFloat())
                val buffering = player.currentItem?.playbackLikelyToKeepUp?.not() ?: false
                if (isLoading != buffering) {
                    isLoading = buffering
                }
            }
        }

        timeObserverRef.value = timeObserver

        // Add notification observers
        NSNotificationCenter.defaultCenter().addObserver(
            observerObject,
            NSSelectorFromString("onPlayerItemDidPlayToEndTime"),
            AVPlayerItemDidPlayToEndTimeNotification,
            player.currentItem
        )

        // Handle playback errors
        NSNotificationCenter.defaultCenter().addObserver(
            observerObject,
            NSSelectorFromString("onPlayerError"),
            AVPlayerItemFailedToPlayToEndTimeNotification,
            player.currentItem
        )

        NSNotificationCenter.defaultCenter().addObserver(
            enterForegroundObject,
            NSSelectorFromString("willEnterForeground"),
            UIApplicationWillEnterForegroundNotification,
            null
        )

        if (isPipMode) {
            val delegate = object : NSObject(), AVPictureInPictureControllerDelegateProtocol {
                override fun pictureInPictureControllerDidStartPictureInPicture(
                    pictureInPictureController: AVPictureInPictureController
                ) {
                    onPipModeChanged(true)
                }

                override fun pictureInPictureControllerDidStopPictureInPicture(
                    pictureInPictureController: AVPictureInPictureController
                ) {
                    onPipModeChanged(false)
                }
            }

            pipDelegateRef.value = delegate
            pipController?.delegate = delegate
        }
    }

    /* -------------------- Load URL -------------------- */
    LaunchedEffect(url) {
        cleanupObservers()
        isPlaying = false
        val urlObject = createUrl(url) ?: run {
            error(MediaPlayerError.ResourceError("Invalid URL"))
            return@LaunchedEffect
        }

        val options: Map<Any?, *>? = headers?.takeIf { it.isNotEmpty() }?.let {
            mapOf("AVURLAssetHTTPHeaderFieldsKey" as Any to it)
        }
        val asset = AVURLAsset(uRL = urlObject, options = options)

        // Load essential keys before using AVPlayerItem
        asset.loadValuesAsynchronouslyForKeys(listOf("duration", "playable")) {
            val playableStatus = asset.statusOfValueForKey("playable", null)
            if (playableStatus != AVKeyValueStatusLoaded || !asset.isPlayable()) {
                error(MediaPlayerError.ResourceError("Invalid URL or unsupported format"))
                return@loadValuesAsynchronouslyForKeys
            }

            val durationStatus = asset.statusOfValueForKey("duration", null)
            if (durationStatus == AVKeyValueStatusLoaded) {
                val duration = CMTimeGetSeconds(asset.duration).toFloat()
                totalTime(duration)
            }

            val newItem = AVPlayerItem(asset = asset)
            player.replaceCurrentItemWithPlayerItem(newItem)
            setupObservers()

            // Ensure playback starts only after everything is ready
            if (isPause) {
                player.pause()
            } else {
                player.play()
                setPlayerRate(speed)
            }
            setQuality(selectedQuality)
            isPlaying = true
            if (!isHlsUrl(url)) {
                asset.loadValuesAsynchronouslyForKeys(listOf("availableMediaCharacteristicsWithMediaSelectionOptions")) {
                    val status = asset.statusOfValueForKey("availableMediaCharacteristicsWithMediaSelectionOptions", null)
                    if (status == AVKeyValueStatusLoaded){
                        audioList(getAvailableAudioTracks())
                        subtitlesList(getAvailableSubtitles())
                    }
                }
            }
        }
    }

    /* -------------------- Reactive updates -------------------- */
    LaunchedEffect(selectedQuality) { if (isPlaying){  setQuality(selectedQuality) } }
    LaunchedEffect(selectedAudioTrack) { if (isPlaying){ setAudioTrack(selectedAudioTrack) } }
    LaunchedEffect(selectedSubTitle, isPlaying) { if (isPlaying){ setSubTitle(selectedSubTitle) } }
    LaunchedEffect(speed) { if (isPlaying){ setPlayerRate(speed) } }

    /* -------------------- UI -------------------- */
    val playerContainer = UIView()
    Box {
        androidx.compose.ui.viewinterop.UIKitView(
            factory = {
                playerContainer.addSubview(avPlayerViewController.view)
                avPlayerViewController.view.setFrame(playerContainer.frame)
                playerContainer
            },
            modifier = modifier,
            update = {
                if(isPlaying) {
                    if (isPause) {
                        player.pause()
                    } else {
                        player.play()
                        setPlayerRate(speed)
                    }

//                    UIApplication.sharedApplication.idleTimerDisabled = !pause
                    seekToTime?.let {
                        player.seekToTime(
                            CMTimeMakeWithSeconds(it.toDouble(), 1000),
                            toleranceBefore = CMTimeMakeWithSeconds(0.0, 1),
                            toleranceAfter = CMTimeMakeWithSeconds(0.0, 1)
                        )
                        setPlayerRate(speed)
                    }
                }
            }
        )
    }

    /* -------------------- Dispose -------------------- */
    DisposableEffect(Unit) {
        onDispose {
            cleanupObservers()

            pipController?.stopPictureInPicture()
            UIApplication.sharedApplication.idleTimerDisabled = false

            player.pause()
            avPlayerViewController.player = null


            avPlayerViewController.view.removeFromSuperview()

            // Clear references
            isPlaying = false
            isLoading = false
        }
    }
}
