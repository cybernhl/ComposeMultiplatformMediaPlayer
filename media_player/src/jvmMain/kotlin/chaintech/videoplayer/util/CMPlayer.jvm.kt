package chaintech.videoplayer.util

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import chaintech.videoplayer.host.DrmConfig
import chaintech.videoplayer.host.MediaPlayerError
import chaintech.videoplayer.model.ScreenResize
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.media.TrackType
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapters

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
    if(!NativeDiscovery().discover()) {
        error(MediaPlayerError.VlcNotFound)
        return
    }
    var repeatStatus by remember { mutableStateOf(loop) }

    val mediaPlayerComponent = remember {
        try {
            initializeMediaPlayerComponent()
        } catch (e: Exception) {
            error(MediaPlayerError.InitializationError("Failed to initialize VLC MediaPlayerComponent: ${e.message}"))
            return@remember null
        }
    } ?: return

    val mediaPlayer = remember {
        try {
            mediaPlayerComponent.mediaPlayer()
        } catch (e: Exception) {
            error(MediaPlayerError.InitializationError("Failed to create VLC media player instance: ${e.message}"))
            return@remember null
        }
    } ?: return

    val adapter = remember { RenderCallbackAdapter() }
    val videoSurface = remember {
        CallbackVideoSurface(adapter, adapter, true, VideoSurfaceAdapters.getVideoSurfaceAdapter())
    }

    // Assign video surface
    LaunchedEffect(Unit) {
        mediaPlayer.videoSurface().set(videoSurface)
    }
    var lastPlayedPosition by remember { mutableStateOf(0L) }
    var lastVideoUrl by remember { mutableStateOf<String?>(null) }
    var time by remember { mutableStateOf(0f) }

// Store listener to avoid garbage collection
    val listener = remember {
        object : MediaPlayerEventAdapter() {
            override fun finished(mediaPlayer: MediaPlayer) {
                currentTime(0f)
                mediaPlayer.submit {
                    mediaPlayer.controls().play()
                    if(!repeatStatus) {
                        mediaPlayer.controls().pause()
                    }
                    didEndVideo()
                }
            }

            override fun buffering(mediaPlayer: MediaPlayer?, newCache: Float) {
                bufferCallback(newCache != 100f)
                super.buffering(mediaPlayer, newCache)
            }

            override fun lengthChanged(mediaPlayer: MediaPlayer?, newLength: Long) {
                time = (newLength / 1000L).toFloat()
                totalTime(time)
                super.lengthChanged(mediaPlayer, newLength)
            }

            override fun timeChanged(mediaPlayer: MediaPlayer?, newTime: Long) {
                currentTime((newTime / 1000L).toFloat())
                lastPlayedPosition = newTime
                super.timeChanged(mediaPlayer, newTime)
            }

            override fun elementaryStreamAdded(mediaPlayer: MediaPlayer?, type: TrackType?, id: Int) {
                if (type == TrackType.AUDIO && mediaPlayer != null && !isHlsUrl(url)) {
                    val tracks = mediaPlayer.audio().trackDescriptions()?.filterNotNull() ?: emptyList()
                    val currentTrackId = mediaPlayer.audio().track()
                    val audioTracks = tracks
                        .filter { it.id() != -1 }
                        .distinctBy { it.id() }
                        .map { track ->
                            val desc = track.description() ?: ""
                            val name = Regex("\\[(.*?)\\]").find(desc)?.groupValues?.get(1) ?: desc
                            AudioTrack(
                                language = name.lowercase(), // optional, you can keep ISO code if you have mapping
                                name = name,
                                groupId = "audio",
                                url = "",
                                isDefault = track.id() == currentTrackId
                            )
                        }

                    if (audioTracks.size > 1) {
                        audioList(audioTracks)
                    }
                }
            }

        }
    }

    // Add event listener only once
    LaunchedEffect(Unit) {
        try {
            mediaPlayer.events().addMediaPlayerEventListener(listener)
        } catch (e: Exception) {
            error(MediaPlayerError.ResourceError("Failed to add media player event listener: ${e.message}"))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer.events().removeMediaPlayerEventListener(listener)
                mediaPlayer.release()
            } catch (e: Exception) {
                error(MediaPlayerError.ResourceError("Error while releasing media player: ${e.message}"))
            }
        }
    }

    LaunchedEffect(selectedAudioTrack) {
        if(isHlsUrl(url)) {
            val tracks = mediaPlayer.audio().trackDescriptions()
            if (tracks.isNotEmpty()) {
                // Filter the tracks based on the description that matches groupId and name
                val filteredTrack = tracks.find { track ->
                    val description = track.description()
                    val groupMatch =
                        selectedAudioTrack?.groupId?.let {
                            description.startsWith(
                                it,
                                ignoreCase = true
                            )
                        } ?: false
                    val nameMatch = selectedAudioTrack?.name?.let {
                        description.contains(
                            it,
                            ignoreCase = true
                        )
                    } ?: false

                    groupMatch && nameMatch
                }
                if (filteredTrack != null) {
                    mediaPlayer.audio().setTrack(filteredTrack.id())
                }
            }
        } else {
            val tracks = mediaPlayer.audio().trackDescriptions()?.filterNotNull() ?: emptyList()

            // Deduplicate by ID
            val uniqueTracks = tracks.distinctBy { it.id() }

            val filteredTrack = uniqueTracks.find { track ->
                selectedAudioTrack?.name?.let { track.description()?.contains(it, ignoreCase = true) } ?: false
            }

            filteredTrack?.let { mediaPlayer.audio().setTrack(it.id()) }
        }
    }

    LaunchedEffect(url, selectedQuality) {
        try {
            val headerOptions = headers?.flatMap { (key, value) ->
                when (key.lowercase()) {
                    "user-agent" -> listOf(":http-user-agent=$value")
                    "referer" -> listOf(":http-referrer=$value")
                    else -> emptyList()
                }
            }?.toMutableList() ?: mutableListOf()

            val customHeaders = headers
                ?.filterKeys { it.lowercase() !in listOf("user-agent", "referer") }
                ?.map { "${it.key}: ${it.value}" }
                ?.joinToString(", ")

            if (!customHeaders.isNullOrEmpty()) {
                headerOptions.add(":http-header-fields=$customHeaders")
            }
            val options = headerOptions.toTypedArray()

            val effectiveUrl = selectedQuality?.url?.takeIf { it.isNotBlank() } ?: url
            if (volume == 0f) {
                mediaPlayer.audio().setMute(true)
            }
            mediaPlayer.media().play(effectiveUrl, *options)
            mediaPlayer.audio().setVolume((volume * 100).toInt())

            if (lastVideoUrl == url && lastPlayedPosition > 0) {
                mediaPlayer.controls().setTime(lastPlayedPosition) // Seek to last position
            }

            lastVideoUrl = url
        } catch (e: Exception) {
            error(MediaPlayerError.PlaybackError("Failed to play media: ${e.message}"))
        }
    }

    LaunchedEffect(speed) {
        try {
            mediaPlayer.controls().setRate(speed)
        } catch (e: Exception) {
            error(MediaPlayerError.PlaybackError("Failed to set playback speed: ${e.message}"))
        }
    }
    LaunchedEffect(loop) {
        repeatStatus = loop
    }

    LaunchedEffect(volume) {
        try {
            if (volume == 0f) {
                mediaPlayer.audio().setMute(true)
            } else {
                mediaPlayer.audio().setMute(false)
                mediaPlayer.audio().setVolume((volume * 100).toInt())
            }
        } catch (e: Exception) {
            error(MediaPlayerError.PlaybackError("Failed to change volume: ${e.message}"))
        }
    }
    LaunchedEffect(isPause) {
        try {
            mediaPlayer.controls().setPause(isPause)
        } catch (e: Exception) {
            error(MediaPlayerError.PlaybackError("Failed to pause/resume playback: ${e.message}"))
        }
    }

    LaunchedEffect(seekToTime) {
        try {
            seekToTime?.let {
                val per = (it * 100 / time) / 100
                mediaPlayer.controls().setPosition(per)
            }
        } catch (e: Exception) {
            error(MediaPlayerError.PlaybackError("Failed to seek to position: ${e.message}"))
        }
    }
    VideoFrame(adapter.imageBitmap, modifier, size)
}

@Composable
private fun VideoFrame(
    imageBitmap: ImageBitmap?,
    modifier: Modifier = Modifier,
    scaleType: ScreenResize
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap != null) {
            val imageAspectRatio = imageBitmap.width.toFloat() / imageBitmap.height
            val parentWidth = constraints.maxWidth.toFloat()
            val parentHeight = constraints.maxHeight.toFloat()
            val parentAspectRatio = parentWidth / parentHeight

            val imageModifier = when (scaleType) {
                ScreenResize.FILL -> Modifier.fillMaxSize()
                ScreenResize.FIT -> {
                    if (parentAspectRatio > imageAspectRatio) {
                        // Parent wider: constrain height
                        Modifier
                            .fillMaxHeight()
                            .aspectRatio(imageAspectRatio)
                    } else {
                        // Parent narrower: constrain width
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(imageAspectRatio)
                    }
                }
            }

            Image(
                bitmap = imageBitmap,
                contentDescription = "Video Frame",
                contentScale = when (scaleType) {
                    ScreenResize.FILL -> ContentScale.Crop
                    ScreenResize.FIT -> ContentScale.Fit
                },
                modifier = imageModifier.clip(RectangleShape)
            )
        }
    }
}
