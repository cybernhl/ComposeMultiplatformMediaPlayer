package chaintech.videoplayer.ui.video.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import chaintech.videoplayer.model.PlayerOption
import chaintech.videoplayer.model.VideoPlayerConfig
import chaintech.videoplayer.model.gradientBGColors
import chaintech.videoplayer.model.selectedSpeedButtonColor
import chaintech.videoplayer.model.selectedTextColor
import chaintech.videoplayer.model.unselectedSpeedButtonColor
import chaintech.videoplayer.model.unselectedTextColor
import chaintech.videoplayer.ui.component.PlayerSpeedButton
import chaintech.videoplayer.util.AudioTrack
import kotlinx.coroutines.delay

@Composable
internal fun AudioTrackSelectionOverlay(
    paddingValues: PaddingValues,
    playerConfig: VideoPlayerConfig,
    audioTrackOptions: List<AudioTrack>,
    selectedAudioTrack: AudioTrack?,
    selectedAudioTrackCallback: (AudioTrack?) -> Unit,
    activeOption: PlayerOption,
    activeOptionCallBack: ((PlayerOption) -> Unit) = {},
) {
    LaunchedEffect(activeOption) {
        if (activeOption == PlayerOption.AUDIO_TRACK) {
            delay(5000)
            activeOptionCallBack(PlayerOption.NONE)
        }
    }


    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AnimatedVisibility(
            visible = activeOption == PlayerOption.AUDIO_TRACK,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(durationMillis = 700))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = Brush.horizontalGradient(gradientBGColors))
                    .pointerInput(Unit) {
                        detectTapGestures { activeOptionCallBack(PlayerOption.NONE) }
                    }
            )
        }

        AnimatedVisibility(
            visible = activeOption == PlayerOption.AUDIO_TRACK,
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 500)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 500)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 35.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center, // Ensures centering
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f) // Takes full height, centering LazyColumn
                        ) {
                            LazyColumn(
                                modifier = Modifier.align(Alignment.Center).padding(paddingValues),
                                verticalArrangement = Arrangement.spacedBy(15.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                contentPadding = PaddingValues(vertical = 20.dp)
                            ) {
                                items(audioTrackOptions) { track ->
                                    AudioTrackSelectionButton(
                                        audioTrack = track,
                                        selectedAudioTrack = selectedAudioTrack,
                                        playerConfig = playerConfig,
                                        onSelect = {
                                            selectedAudioTrackCallback(it)
                                            activeOptionCallBack(PlayerOption.NONE)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun WasmJsAudioTrackSelectionOverlay(
    paddingValues: PaddingValues,
    playerConfig: VideoPlayerConfig,
    audioTrackOptions: List<AudioTrack>,
    selectedAudioTrack: AudioTrack?,
    selectedAudioTrackCallback: (AudioTrack?) -> Unit,
    activeOption: PlayerOption,
    activeOptionCallBack: ((PlayerOption) -> Unit) = {},
) {
    LaunchedEffect(activeOption) {
        if (activeOption == PlayerOption.AUDIO_TRACK) {
            delay(7000)
            activeOptionCallBack(PlayerOption.NONE)
        }
    }


    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = activeOption == PlayerOption.AUDIO_TRACK,
            enter = slideInVertically(
                initialOffsetY = { it }, // slide up from bottom
                animationSpec = tween(durationMillis = 300)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it }, // slide down
                animationSpec = tween(durationMillis = 300)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp)
                    .background(Color.Black)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(paddingValues),
                    horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 0.dp,
                        bottom = 0.dp
                    )
                ) {
                    items(audioTrackOptions) { track ->
                        AudioTrackSelectionButton(
                            audioTrack = track,
                            selectedAudioTrack = selectedAudioTrack,
                            playerConfig = playerConfig,
                            onSelect = {
                                selectedAudioTrackCallback(it)
                                activeOptionCallBack(PlayerOption.NONE)
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
internal fun AudioTrackSelectionButton(
    audioTrack: AudioTrack,
    selectedAudioTrack: AudioTrack?,
    playerConfig: VideoPlayerConfig,
    onSelect: (AudioTrack) -> Unit
) {
    val isSelected = selectedAudioTrack?.let {
        it == audioTrack
    } ?: audioTrack.isDefault

    PlayerSpeedButton(
        title = audioTrack.name,
        size = (playerConfig.topControlSize * 1.25f),
        backgroundColor = if (isSelected) selectedSpeedButtonColor else unselectedSpeedButtonColor,
        titleColor = if (isSelected) selectedTextColor else unselectedTextColor,
        onClick = { onSelect(audioTrack) }
    )
}