package chaintech.videoplayer.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import chaintech.videoplayer.host.MediaPlayerError
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
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemFailedToPlayToEndTimeNotification
import platform.AVFoundation.AVQueuePlayer
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.isPlayable
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.playbackLikelyToKeepUp
import platform.AVFoundation.rate
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.volume
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSError
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSelectorFromString
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun CMPAudioPlayer(
    modifier: Modifier,
    url: String,
    isPause: Boolean,
    totalTime: (Float) -> Unit,
    currentTime: (Float) -> Unit,
    isSliding: Boolean,
    seekToTime: Float?,
    loop: Boolean,
    loadingState: (Boolean) -> Unit,
    speed: Float,
    volume: Float,
    didEndAudio: () -> Unit,
    error: (MediaPlayerError) -> Unit,
    headers: Map<String, String>?,
) {
    val playerItem = remember { mutableStateOf<AVPlayerItem?>(null) }
    val player = remember { AVQueuePlayer() }
    var repeatStatus by remember { mutableStateOf(loop) }

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

    fun setPlayerRate(speed: Float) {
        player.rate = speed
    }

    // Load the audio item when the URL changes
    LaunchedEffect(url) {
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
            playerItem.value = newItem
            player.replaceCurrentItemWithPlayerItem(newItem)

            if (isPause) {
                player.pause()
            } else {
                player.play()
                setPlayerRate(speed)
            }
        }
    }

    // Update repeat status when it changes
    LaunchedEffect(loop) {
        repeatStatus = loop
    }

    LaunchedEffect(speed) {
        setPlayerRate(speed)
    }
    LaunchedEffect(volume) {
        player.volume = volume
    }

    // Handle play/pause and seeking when isPause or sliderTime changes
    LaunchedEffect(isPause, seekToTime) {
        if (isPause) {
            player.pause()
        } else {
            player.play()
            setPlayerRate(speed)
        }
        seekToTime?.let {
            val time = CMTimeMakeWithSeconds(
                it.toDouble(),
                1000
            )
            player.seekToTime(
                time,
                toleranceBefore = CMTimeMakeWithSeconds(0.0, 1),
                toleranceAfter = CMTimeMakeWithSeconds(0.0, 1)
            )
        }
    }

    DisposableEffect(Unit) {
        // Observer for when the audio item finishes playing
        val observerObject = object : NSObject() {
            @ObjCAction
            fun onPlayerItemDidPlayToEndTime() {
                player.currentItem?.let { item ->
                    player.seekToTime(CMTimeMakeWithSeconds(0.0, 1))
                    player.removeItem(item)
                    player.insertItem(item, afterItem = null)
                    player.pause()
                }
                if (repeatStatus) {
                    player.play()
                }
                didEndAudio()
            }

            @ObjCAction
            fun onPlayerError() {
                val errorMessage =
                    player.currentItem?.error?.localizedDescription ?: "Unknown playback error"
                error(MediaPlayerError.PlaybackError(errorMessage))
            }
        }

        // Periodic time observer to update current and total time
        val timeObserver = player.addPeriodicTimeObserverForInterval(
            CMTimeMakeWithSeconds(1.0, 1),
            null
        ) {
            if (!isSliding) {
                val current = CMTimeGetSeconds(player.currentTime())
                currentTime(current.toFloat())
                loadingState(player.currentItem?.playbackLikelyToKeepUp?.not() ?: false)
            }
        }

        // Add observer for player item end notification
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

        onDispose {
            player.pause()
            player.replaceCurrentItemWithPlayerItem(null)
            NSNotificationCenter.defaultCenter().removeObserver(observerObject)
            player.removeTimeObserver(timeObserver)
        }
    }
}