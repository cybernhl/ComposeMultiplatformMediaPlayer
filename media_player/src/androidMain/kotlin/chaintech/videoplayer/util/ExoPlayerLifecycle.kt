package chaintech.videoplayer.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.exoplayer.ExoPlayer

fun getExoPlayerLifecycleObserver(
    exoPlayer: ExoPlayer,
    isPause: Boolean,
    isPipMode: Boolean,
    wasAppInBackground: Boolean,
    setWasAppInBackground: (Boolean) -> Unit
): LifecycleEventObserver {
    return LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> handleOnResume(
                exoPlayer,
                isPause,
                wasAppInBackground,
                setWasAppInBackground
            )

            Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> handleOnPause(exoPlayer, isPipMode,setWasAppInBackground)
            Lifecycle.Event.ON_DESTROY -> handleOnDestroy(exoPlayer)
            else -> { /* No-op */
            }
        }
    }
}

private fun handleOnResume(
    exoPlayer: ExoPlayer,
    isPause: Boolean,
    wasAppInBackground: Boolean,
    setWasAppInBackground: (Boolean) -> Unit
) {
    if (wasAppInBackground) {
        exoPlayer.playWhenReady = !isPause
    }
    setWasAppInBackground(false)
}

private fun handleOnPause(
    exoPlayer: ExoPlayer,
    isPipMode: Boolean,
    setWasAppInBackground: (Boolean) -> Unit
) {
    if(!isPipMode) {
        exoPlayer.playWhenReady = false
        setWasAppInBackground(true)
    }
}

private fun handleOnDestroy(
    exoPlayer: ExoPlayer
) {
    exoPlayer.release()
}