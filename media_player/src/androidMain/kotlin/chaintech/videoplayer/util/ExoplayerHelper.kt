package chaintech.videoplayer.util

import android.content.Context
import android.media.MediaDrm
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.LocalMediaDrmCallback
import androidx.media3.exoplayer.drm.UnsupportedDrmException
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import chaintech.videoplayer.host.DrmConfig
import chaintech.videoplayer.host.MediaPlayerError
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun rememberPlayerView(exoPlayer: ExoPlayer, context: Context): PlayerView {
    val playerView = remember(context) {
        PlayerView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
        }
    }
    val currentPlayer by rememberUpdatedState(exoPlayer)

    LaunchedEffect(currentPlayer) {
        playerView.player = currentPlayer
    }

    DisposableEffect(playerView) {
        onDispose {
            playerView.player = null
        }
    }
    return playerView
}

@OptIn(UnstableApi::class)
@Composable
fun rememberExoPlayerWithLifecycle(
    url: String,
    context: Context,
    isPause: Boolean,
    isLiveStream: Boolean,
    isPipMode: Boolean,
    headers: Map<String, String>?,
    drmConfig: DrmConfig?,
    error: (MediaPlayerError) -> Unit,
    selectedQuality: VideoQuality?,
    selectedAudioTrack: AudioTrack?,
    selectedSubtitleTrack: SubtitleTrack?,
    audioList: ((List<AudioTrack>) -> Unit),
    subtitlesList: ((List<SubtitleTrack>) -> Unit),
    qualityList: ((List<VideoQuality>) -> Unit)
): ExoPlayer {
    val lifecycleOwner = LocalLifecycleOwner.current
    var cache by remember { mutableStateOf<SimpleCache?>(null) }

    LaunchedEffect(context) {
        cache = CacheManager.getCache(context)
    }
    val trackSelector = remember { DefaultTrackSelector(context) }

    val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            2500,
            30000,
            500,
            1000
        )
        .build()

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .build().apply {
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                repeatMode = Player.REPEAT_MODE_OFF
                setHandleAudioBecomingNoisy(true)
            }
    }

    LaunchedEffect(exoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                if (!isHlsUrl(url)) {
                    val (videoQualities, audioTracks, subtitleTracks) = exoPlayer.getAvailableTracks()
                    audioList(audioTracks)
                    subtitlesList(subtitleTracks)
                    qualityList(videoQualities)
                }
            }
        })
    }

    LaunchedEffect(selectedQuality) {
        applyQualitySelection(trackSelector, selectedQuality)
    }
    LaunchedEffect(selectedAudioTrack) {
        applyAudioTrackSelection(trackSelector, selectedAudioTrack)
    }
    LaunchedEffect(selectedSubtitleTrack) {
        applySubTitleTrackSelection(trackSelector, selectedSubtitleTrack)
    }

    LaunchedEffect(url, cache) {
        if (cache == null) return@LaunchedEffect
        try {
            val mediaItem = MediaItem.fromUri(url.toUri())

            val mediaSource = when {
                drmConfig != null -> createHlsMediaSourceWithDrm(mediaItem, headers, drmConfig)
                isLiveStream || isHlsUrl(url) -> createHlsMediaSource(mediaItem, headers)
                else -> createProgressiveMediaSource(mediaItem, cache!!, context, headers)
            }

            exoPlayer.apply {
                stop()
                clearMediaItems()
                setMediaSource(mediaSource)
                prepare()
                seekTo(0, 0)
            }
        } catch (e: Exception) {
            error(MediaPlayerError.PlaybackError(e.message ?: "Failed to load media"))
        }
    }

    var appInBackground by remember {
        mutableStateOf(false)
    }

    DisposableEffect(key1 = lifecycleOwner, appInBackground, isPipMode) {
        val lifecycleObserver =
            getExoPlayerLifecycleObserver(exoPlayer, isPause, isPipMode,appInBackground) {
                appInBackground = it
            }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }
    return exoPlayer
}

@OptIn(UnstableApi::class)
private fun applyQualitySelection(
    trackSelector: DefaultTrackSelector,
    selectedQuality: VideoQuality?
) {
    trackSelector.setParameters(
        trackSelector.buildUponParameters().apply {
            selectedQuality?.let {
                setMaxVideoBitrate(it.bitrate.toInt())
                setMinVideoBitrate(it.bitrate.toInt())
            } ?: setMaxVideoBitrate(Int.MAX_VALUE)
        }
    )
}

@OptIn(UnstableApi::class)
private fun applyAudioTrackSelection(trackSelector: DefaultTrackSelector, audioTrack: AudioTrack?) {
    trackSelector.setParameters(
        trackSelector.buildUponParameters()
            .setPreferredAudioLanguage(audioTrack?.language)
    )
}

@OptIn(UnstableApi::class)
private fun applySubTitleTrackSelection(
    trackSelector: DefaultTrackSelector,
    subtitleTrack: SubtitleTrack?
) {
    trackSelector.setParameters(
        trackSelector.buildUponParameters().apply {
            if (subtitleTrack != null) {
                setPreferredTextLanguage(subtitleTrack.language)
                setRendererDisabled(C.TRACK_TYPE_TEXT, false)
                setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            } else {
                setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                setRendererDisabled(C.TRACK_TYPE_TEXT, true)
                setPreferredTextLanguage(null)
                setSelectUndeterminedTextLanguage(false)
            }
        }
    )
}


@OptIn(UnstableApi::class)
private fun createHlsMediaSource(mediaItem: MediaItem, headers: Map<String, String>?): MediaSource {
    val headersMap = headers ?: emptyMap()
    val dataSourceFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(15_000)
        .setDefaultRequestProperties(headersMap)

    return HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
}

@OptIn(UnstableApi::class)
private fun createProgressiveMediaSource(
    mediaItem: MediaItem,
    cache: Cache,
    context: Context,
    headers: Map<String, String>?
): MediaSource {
    val headersMap = headers ?: emptyMap()
    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(15_000)
        .setDefaultRequestProperties(headersMap)

    val dataSourceFactory = CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context, httpDataSourceFactory))
        .setCacheWriteDataSinkFactory(null)
        .setFlags(
            CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR or
                    CacheDataSource.FLAG_BLOCK_ON_CACHE or
                    CacheDataSource.FLAG_IGNORE_CACHE_FOR_UNSET_LENGTH_REQUESTS
        )

    return ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
}

@OptIn(UnstableApi::class)
private fun createHlsMediaSourceWithDrm(
    mediaItem: MediaItem,
    headers: Map<String, String>?,
    drmConfig: DrmConfig
): MediaSource {
    val headersMap = headers ?: emptyMap()
    val dataSourceFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(15_000)
        .setDefaultRequestProperties(headersMap)

    val drmSessionManager = try {
        DefaultDrmSessionManager.Builder()
            .setUuidAndExoMediaDrmProvider(C.CLEARKEY_UUID) { FrameworkMediaDrm.newInstance(C.CLEARKEY_UUID) }
            .build(LocalMediaDrmCallback(VideoUtils.createDrmJson(drmConfig)))
    } catch (e: UnsupportedDrmException) {
        throw RuntimeException("Unsupported DRM scheme: ${e.message}", e)
    } catch (e: MediaDrm.MediaDrmStateException) {
        throw RuntimeException("DRM state issue: ${e.message}", e)
    } catch (e: Exception) {
        throw RuntimeException("Failed to create DRM session manager: ${e.message}", e)
    }

    return HlsMediaSource.Factory(dataSourceFactory)
        .setDrmSessionManagerProvider { drmSessionManager }
        .createMediaSource(mediaItem)
}

@OptIn(UnstableApi::class)
fun ExoPlayer.getAvailableTracks(): Triple<List<VideoQuality>, List<AudioTrack>, List<SubtitleTrack>> {
    val videoTracks = mutableListOf<VideoQuality>()
    val audioTracks = mutableListOf<AudioTrack>()
    val subtitleTracks = mutableListOf<SubtitleTrack>()

    currentTracks.groups.forEach { group ->
        val trackType = group.type

        for (i in 0 until group.mediaTrackGroup.length) {
            val format = group.mediaTrackGroup.getFormat(i)

            when (trackType) {
                C.TRACK_TYPE_VIDEO -> {
                    videoTracks.add(
                        VideoQuality(
                            bitrate = format.bitrate.toDouble(),
                            resolution = format.toResolutionString(),
                            url = ""
                        )
                    )
                }

                C.TRACK_TYPE_AUDIO -> {
                    val languageCode = format.language ?: "und"
                    val languageName = try {
                        Locale.forLanguageTag(languageCode).getDisplayName(Locale.getDefault()).replaceFirstChar { it.uppercase() }
                    } catch (e: Exception) {
                        languageCode // fallback to code if mapping fails
                    }

                    audioTracks.add(
                        AudioTrack(
                            language = format.language ?: "und",
                            name = format.label ?: languageName,
                            groupId = "audio",
                            url = "",
                            isDefault = group.isSelected
                        )
                    )
                }

                C.TRACK_TYPE_TEXT -> {
                    subtitleTracks.add(
                        SubtitleTrack(
                            language = format.language ?: "und",
                            name = format.label ?: format.language ?: "Subtitle",
                            groupId = "subtitle",
                            url = "",
                            isDefault = group.isSelected
                        )
                    )
                }
            }
        }
    }

    // ✅ Deduplicate
    val uniqueVideoTracks = videoTracks.distinctBy { it.resolution }
    val uniqueAudioTracks = audioTracks.distinctBy { it.language + it.name }
    val uniqueSubtitleTracks = subtitleTracks.distinctBy { it.language + it.name }

    // ✅ Sort video by resolution ASCENDING (Auto always first)
    val sortedVideoTracks = uniqueVideoTracks
        .sortedWith(compareBy { it.bitrate }) // ascending
        .toMutableList()
        .apply {
            val auto = firstOrNull { it.resolution == "Auto" }
            if (auto != null) {
                remove(auto)
                add(0, auto)
            }
        }

    // ✅ Sort video by resolution ASCENDING (Auto always first)
    val filteredVideoTracks = if (sortedVideoTracks.size > 1) sortedVideoTracks else emptyList()
    val filteredAudioTracks = if (uniqueAudioTracks.size > 1) uniqueAudioTracks else emptyList()

    return Triple(filteredVideoTracks, filteredAudioTracks, uniqueSubtitleTracks)
}

private fun Format.toResolutionString(): String {
    return when {
        height >= 2160 -> "2160p"
        height >= 1440 -> "1440p"
        height >= 1080 -> "1080p"
        height >= 720 -> "720p"
        height >= 480 -> "480p"
        height > 0 -> "${height}p"
        else -> "Auto"
    }
}
