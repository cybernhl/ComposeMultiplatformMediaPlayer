package chaintech.videoplayer.ui.reel

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import chaintech.videoplayer.host.MediaPlayerEvent
import chaintech.videoplayer.host.MediaPlayerHost
import chaintech.videoplayer.model.ScreenResize
import chaintech.videoplayer.model.VideoPlayerConfig
import chaintech.videoplayer.ui.video.VideoPlayerWithControl
import chaintech.videoplayer.util.isMobile
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun ReelsPlayerComposable(
    modifier: Modifier = Modifier, // Modifier for the composable
    urls: List<String>, // List of video URLs
    playerConfig: VideoPlayerConfig = VideoPlayerConfig(), // Configuration for the player
    currentItemIndex: ((Int) -> Unit)? = null
) {
    playerConfig.isZoomEnabled = false
    playerConfig.isGestureVolumeControlEnabled = false

    val pagerState = rememberPagerState(pageCount = { urls.size })

    val playerHosts = remember(key1 = urls) {
        urls.map { url -> MediaPlayerHost(mediaUrl = url) }
    }

    // Play/Pause control based on the visible page
    LaunchedEffect(pagerState.currentPage) {
        val currentPage = pagerState.currentPage
        playerHosts.forEachIndexed { index, playerHost ->
            if (index == currentPage) playerHost.play() else playerHost.pause()
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                currentItemIndex?.let {
                    it(page)
                }
            }
    }

    val coroutineScope = rememberCoroutineScope()
    fun handelReelEnd(page: Int) {
        if (playerConfig.autoPlayNextReel) {
            val nextPage = page + 1
            if (nextPage < urls.size && pagerState.currentPage == page) {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(nextPage)
                }
            }
        }
    }

    LaunchedEffect(playerConfig.autoPlayNextReel, pagerState.currentPage) {
        val playerHost = playerHosts[pagerState.currentPage]
        playerHost.setLooping(!(playerConfig.autoPlayNextReel && pagerState.currentPage < urls.lastIndex))
    }

    @Composable
    fun ReelPage(page: Int) {
        val playerHost = remember(playerHosts[page]) { playerHosts[page] }
        if (!isMobile()) { playerHost.videoFitMode = ScreenResize.FIT }
        playerHost.onEvent = { if (it == MediaPlayerEvent.MediaEnd) handelReelEnd(page) }

        VideoPlayerWithControl(
            modifier = modifier,
            playerConfig = playerConfig,
            playerHost = playerHost
        )
    }

    // Render vertical pager if enabled, otherwise render horizontal pager
    if (playerConfig.reelVerticalScrolling) {
        VerticalPager(
            state = pagerState,
        ) { page ->
            ReelPage(page)
        }
    } else {
        HorizontalPager(
            state = pagerState
        ) { page ->
            ReelPage(page)
        }
    }
}

