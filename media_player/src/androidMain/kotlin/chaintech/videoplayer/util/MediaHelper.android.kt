package chaintech.videoplayer.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer


@Composable
internal actual fun FetchTotalDuration(url: String, onDurationRetrieved: (Double) -> Unit) {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }

    LaunchedEffect(url) {
        val mediaItem = MediaItem.fromUri(url) // Create a new MediaItem when url changes
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    val totalDuration = player.duration / 1000.0
                    onDurationRetrieved(totalDuration)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                onDurationRetrieved(0.0) // Return 0 on error
            }
        }

        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            player.release() // Release player only when Composable is removed
        }
    }
}

