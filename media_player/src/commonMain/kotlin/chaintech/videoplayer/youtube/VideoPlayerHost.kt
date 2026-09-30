package chaintech.videoplayer.youtube

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.time.Duration


// Class to manage YouTube host state
@Stable
internal class VideoPlayerHost {
    private val commandLock = Mutex()

    var playerState by mutableStateOf<VideoState>(VideoState.Idle)
        private set

    var isBuffering by mutableStateOf(true)
        private set

    var volumeLevel: Float? by mutableStateOf(null)
        private set

    var rate: Float? by mutableStateOf(null)
        private set

    var isMute: Boolean? by mutableStateOf(null)
        private set

    var isPause: Boolean? by mutableStateOf(null)
        private set

    var isFullScreen: Boolean? by mutableStateOf(null)
        private set

    var backAction: Boolean? by mutableStateOf(null)
        private set

    internal var pendingCommand by mutableStateOf<PlayerCommand?>(null)
    private var commandContinuation: CancellableContinuation<Unit>? = null

    suspend fun load(videoId: String) = executeCommand(PlayerCommand.LoadVideo(videoId))
    suspend fun play() = executeCommand(PlayerCommand.Play)
    suspend fun pause() = executeCommand(PlayerCommand.Pause)
    suspend fun seekTo(position: Duration) = executeCommand(PlayerCommand.SeekTo(position))
    suspend fun mute() = executeCommand(PlayerCommand.Mute)
    suspend fun unmute() = executeCommand(PlayerCommand.Unmute)
    suspend fun setSpeed(rate: Float) = executeCommand(PlayerCommand.PlaybackSpeed(rate))
    suspend fun setVolume(percent: Int) = executeCommand(PlayerCommand.Volume(percent))
    suspend fun setFullScreen(isFullScreen: Boolean) = executeCommand(PlayerCommand.SetFullScreen(isFullScreen))
    suspend fun setControls(hide: Boolean, hideBack: Boolean) = executeCommand(PlayerCommand.HideControlsUI(hide, hideBack))

    private suspend fun executeCommand(command: PlayerCommand) {
        commandLock.withLock {
            try {
                suspendCancellableCoroutine {
                    this.commandContinuation = it
                    this.pendingCommand = command
                }
            } finally {
                this.pendingCommand = null
                this.commandContinuation = null
            }
        }
    }

    internal fun complete() {
        commandContinuation?.resume(Unit)
    }

    internal fun update(event: PlayerEvent) {
        playerState = when (event) {
            is PlayerEvent.Error -> {
                isBuffering = false
                VideoState.Failed(event.reason)
            }

            is PlayerEvent.VideoId -> {
                isBuffering = false
                (playerState as? VideoState.Playing)?.copy(videoId = event.id)
                    ?: VideoState.Playing(event.id)
            }
            is PlayerEvent.VolumeChange -> {
                volumeLevel = event.volume / 100
                (playerState as? VideoState.Playing)?.copy(volumeLevel = event.volume / 100) ?: VideoState.Failed("Invalid state")
            }
            is PlayerEvent.RateChange -> {
                rate = event.rate
                (playerState as? VideoState.Playing)?.copy(playBackRate = event.rate) ?: VideoState.Failed("Invalid state")
            }
            is PlayerEvent.MuteChange -> {
                isMute = event.isMute
                (playerState as? VideoState.Playing)?.copy(isMute = event.isMute) ?: VideoState.Failed("Invalid state")
            }
            is PlayerEvent.PauseChange -> {
                isPause = event.isPause
                (playerState as? VideoState.Playing)?.copy(isPause = event.isPause) ?: VideoState.Failed("Invalid state")
            }

            is PlayerEvent.FullScreenToggle -> {
                isFullScreen = this.isFullScreen?.let { !it } ?: false
                (playerState as? VideoState.Playing)?.copy(isFullScreen = isFullScreen) ?: VideoState.Failed("Invalid state")
            }
            is PlayerEvent.BackButtonAction -> {
                backAction = this.backAction?.let { !it } ?: false
                (playerState as? VideoState.Playing)?.copy(backHandler = backAction) ?: VideoState.Failed("Invalid state")
            }

            PlayerEvent.Ready -> {
                VideoState.Initialized
            }

            is PlayerEvent.State -> {
                isBuffering = event.status == PlayerEvent.State.VideoState.BUFFERING
                (playerState as? VideoState.Playing)?.copy(
                    isPlaying = event.status == PlayerEvent.State.VideoState.PLAYING,
                    playbackStatus = event.status
                ) ?: VideoState.Failed("Invalid state")
            }

            is PlayerEvent.Progress -> {
                isBuffering = false
                (playerState as? VideoState.Playing)?.copy(currentTime = event.position)
                    ?: VideoState.Failed("Invalid state")
            }

            is PlayerEvent.DurationChanged -> {
                isBuffering = false
                (playerState as? VideoState.Playing)?.copy(totalDuration = event.duration)
                    ?: VideoState.Playing(videoId = "", totalDuration = event.duration)
            }
        }
    }
}