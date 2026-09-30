package chaintech.videoplayer.ui.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import chaintech.videoplayer.host.MediaPlayerHost
import chaintech.videoplayer.model.PlayerSpeed
import chaintech.videoplayer.util.CMPAudioPlayer

@Composable
fun AudioPlayer(
    playerHost: MediaPlayerHost
) {
    LaunchedEffect(playerHost.totalTime) {
        if (playerHost.totalTime > 0) {
            playerHost.playFromTime?.let {
                playerHost.isSliding = true
                playerHost.seekToTime = it
                playerHost.isSliding = false
                playerHost.playFromTime = null
            }
        }
    }

    CMPAudioPlayer(
        modifier = Modifier,
        url = playerHost.url,
        isPause = playerHost.isPaused,
        totalTime = { playerHost.updateTotalTime(it)}, // Update total time of the audio
        currentTime = {
            if (!playerHost.isSliding) {
                playerHost.updateCurrentTime(it)
                playerHost.seekToTime = null
            }
        },
        isSliding = playerHost.isSliding, // Pass seek bar sliding state
        seekToTime = playerHost.seekToTime, // Pass seek bar slider time
        loop = playerHost.isLooping,
        loadingState = { playerHost.setBufferingStatus(it) },
        speed = when (playerHost.speed) {
            PlayerSpeed.X0_5 -> 0.5f
            PlayerSpeed.X1   -> 1.0f
            PlayerSpeed.X1_5 -> 1.5f
            PlayerSpeed.X2   -> 2.0f
        },
        volume = playerHost.volumeLevel,
        didEndAudio = {
            playerHost.triggerMediaEnd()
        },
        error = { playerHost.triggerError(it) },
        headers = playerHost.headers
    )
}