package chaintech.videoplayer.ui.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.RepeatOneOn
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ShuffleOn
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import chaintech.videoplayer.extension.formatMinSec
import chaintech.videoplayer.host.MediaPlayerHost
import chaintech.videoplayer.model.AudioFile
import chaintech.videoplayer.model.AudioPlayerConfig
import chaintech.videoplayer.model.PlayerSpeed
import chaintech.videoplayer.ui.component.AnimatedClickableIcon
import chaintech.videoplayer.ui.component.CustomSeekBar
import chaintech.videoplayer.util.CMPAudioPlayer
import chaintech.videoplayer.util.ImageFromUrl
import kotlin.random.Random


@Composable
fun AudioPlayerComposable(
    modifier: Modifier = Modifier, // Modifier for the composable
    audios: List<AudioFile>, // URL of the video
    playerHost: MediaPlayerHost,
    audioPlayerConfig: AudioPlayerConfig = AudioPlayerConfig(), // Configuration for the player
    currentItemIndex: ((Int) -> Unit)? = null
) {
    var currentIndex by remember { mutableStateOf(0) }
    var isShuffle by remember { mutableStateOf(false) } // State for Shuffle

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

    fun changeAudio(isNext: Boolean) {
        // Function to get a random index for shuffling
        fun getNextShuffleIndex(): Int {
            if (audios.size <= 1) return Random.nextInt(0, audios.size)

            var newIndex: Int
            do {
                newIndex = Random.nextInt(0, audios.size)
            } while (newIndex == currentIndex)

            return newIndex
        }

        // Change audio index based on whether moving to the next or previous audio
        if (isNext) {
            currentIndex = if (isShuffle) {
                getNextShuffleIndex() // Get a random index if shuffling
            } else {
                (currentIndex + 1) % audios.size // Move to the next audio
            }
        } else {
            // Move to the previous audio or reset slider time if at the start
            if (currentIndex > 0) {
                currentIndex -= 1 // Move to previous audio
            } else {
                playerHost.seekTo(0f)
            }
        }

        // Update player state
        playerHost.play()
        playerHost.setBufferingStatus(true)
    }

    LaunchedEffect(currentIndex) {
        playerHost.loadUrl(audios[currentIndex].audioUrl)
        currentItemIndex?.let {
            it(currentIndex)
        }
    }

    // Container for the audio player and control components
    Box(
        modifier = modifier
    ) {
        // Check if there are any audios to play
        if (audios.isNotEmpty()) {
            // Audio player component
            CMPAudioPlayer(
                modifier = modifier,
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
                    if (!playerHost.isLooping) {
                        if (audios.size > 1) {
                            changeAudio(true)
                        } else {
                            playerHost.togglePlayPause()
                        }
                    }
                    playerHost.triggerMediaEnd()
                },
                error = { playerHost.triggerError(it)},
                headers = playerHost.headers
            )
        }
        if(audioPlayerConfig.showControl) {
            Box(modifier = Modifier.fillMaxSize()
                .background(audioPlayerConfig.backgroundColor)
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart) // Align the column to the bottom
                        .padding(bottom = audioPlayerConfig.controlsBottomPadding),
                    verticalArrangement = Arrangement.spacedBy(30.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.5f)
                            .padding(horizontal = 25.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.weight(0.25f))

                        // Display album art
                        AlbumArt(
                            audioPlayerConfig = audioPlayerConfig,
                            thumbnailUrl = audios[currentIndex].thumbnailUrl
                        )

                        // Display audio title if it's not empty
                        if (audios[currentIndex].audioTitle.isNotEmpty()) {
                            Text(
                                modifier = Modifier.padding(top = 25.dp),
                                text = audios[currentIndex].audioTitle,
                                color = audioPlayerConfig.fontColor,
                                style = audioPlayerConfig.titleTextStyle,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.weight(0.05f))
                    }

                    // Show controls if they are visible in configuration
                    if (audioPlayerConfig.isControlsVisible) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1f) // Occupy remaining horizontal space
                            ) {
                                CustomSeekBar(
                                    modifier = Modifier.fillMaxWidth(),
                                    progress = playerHost.currentTime,
                                    maxProgress = playerHost.totalTime,
                                    onValueChange = {
                                        playerHost.updateCurrentTime(it)
                                        playerHost.isSliding = true
                                        playerHost.seekToTime = null
                                    },
                                    onValueChangeFinished = {
                                        playerHost.isSliding = false
                                        playerHost.seekToTime = playerHost.currentTime
                                    },
                                    thumbRadius = audioPlayerConfig.seekBarThumbRadius,
                                    trackHeight = audioPlayerConfig.seekBarTrackHeight,
                                    activeTrackColor = audioPlayerConfig.seekBarActiveTrackColor, // Active color
                                    inactiveTrackColor = audioPlayerConfig.seekBarInactiveTrackColor, // Inactive color
                                    thumbColor = audioPlayerConfig.seekBarThumbColor,
                                    showThumbAlways = true
                                )

                                // Display time details (current and total time)
                                TimeDetails(
                                    audioPlayerConfig = audioPlayerConfig,
                                    currentTime = playerHost.currentTime,
                                    totalTime = playerHost.totalTime
                                )
                            }
                        }

                        // Control panel for playback controls (play, pause, next, previous, shuffle, repeat)
                        ControlPanel(
                            audioPlayerConfig = audioPlayerConfig,
                            isPause = playerHost.isPaused,
                            isRepeat = playerHost.isLooping,
                            isLoading = playerHost.isBuffering,
                            isShuffle = isShuffle,
                            onPlayPauseClick = { playerHost.togglePlayPause() },
                            onNextClick = { changeAudio(isNext = true) },
                            onPreviousClick = { changeAudio(isNext = false) },
                            onShuffleClick = { isShuffle = !isShuffle },
                            onRepeatClick = { playerHost.toggleLoop() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumArt(
    audioPlayerConfig: AudioPlayerConfig,
    thumbnailUrl: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.7f)
            .clip(RoundedCornerShape(10.dp))
            .background(
                color = audioPlayerConfig.coverBackground,
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Display music note icon as a fallback
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = "Music Note",
            tint = Color.White,
            modifier = Modifier.fillMaxSize(0.8f) // Scale down the icon size
        )

        // Load the album thumbnail if the URL is not empty
        if (thumbnailUrl.isNotEmpty()) {
            ImageFromUrl(
                modifier = Modifier.fillMaxSize(),
                data = thumbnailUrl,
                contentScale = ContentScale.Crop // Crop the image to fill the box
            )
        }
    }
}

@Composable
private fun TimeDetails(
    audioPlayerConfig: AudioPlayerConfig,
    currentTime: Float,
    totalTime: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 5.dp), // Add padding to the top
        horizontalArrangement = Arrangement.SpaceBetween // Distribute space evenly between child components
    ) {
        // Display the current playback time
        Text(
            text = currentTime.formatMinSec(), // Format the current time to "MM:SS" format
            color = audioPlayerConfig.fontColor,
            style = audioPlayerConfig.durationTextStyle
        )

        Spacer(modifier = Modifier.weight(1f)) // Add a spacer to push the total time to the right

        // Display the total duration of the media
        Text(
            text = totalTime.formatMinSec(), // Format the total time to "MM:SS" format
            color = audioPlayerConfig.fontColor,
            style = audioPlayerConfig.durationTextStyle
        )
    }
}

@Composable
private fun ControlPanel(
    audioPlayerConfig: AudioPlayerConfig,
    isPause: Boolean,
    isRepeat: Boolean,
    isLoading: Boolean,
    isShuffle: Boolean,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit
) {
    // Define the height of the control panel based on icon sizes
    val controlPanelHeight = max(
        audioPlayerConfig.pauseResumeIconSize,
        audioPlayerConfig.previousNextIconSize
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(controlPanelHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        // Repeat Icon
        AnimatedClickableIcon(
            painterRes = if (isRepeat) audioPlayerConfig.repeatOnIconResource else audioPlayerConfig.repeatOffIconResource,
            imageVector = if (isRepeat) Icons.Filled.RepeatOneOn else Icons.Filled.RepeatOne,
            contentDescription = "Repeat",
            tint = audioPlayerConfig.iconsTintColor,
            iconSize = audioPlayerConfig.advanceControlIconSize,
            animationDuration = audioPlayerConfig.controlClickAnimationDuration,
            onClick = {
                onRepeatClick()
            }
        )

        // Previous Icon
        AnimatedClickableIcon(
            painterRes = audioPlayerConfig.previousIconResource,
            imageVector = Icons.Filled.SkipPrevious,
            contentDescription = "Previous",
            tint = audioPlayerConfig.iconsTintColor,
            iconSize = audioPlayerConfig.previousNextIconSize,
            animationDuration = audioPlayerConfig.controlClickAnimationDuration,
            onClick = {
                onPreviousClick()
            }
        )

        // Play/Pause Icon with Loading Indicator
        Box {
            AnimatedClickableIcon(
                painterRes = if (isPause) audioPlayerConfig.playIconResource else audioPlayerConfig.pauseIconResource,
                imageVector = if (isPause) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                contentDescription = "Play/Pause",
                tint = audioPlayerConfig.iconsTintColor,
                iconSize = audioPlayerConfig.pauseResumeIconSize,
                animationDuration = audioPlayerConfig.controlClickAnimationDuration,
                onClick = { onPlayPauseClick() }
            )

            if (isLoading) {
                Box(modifier = Modifier.size(audioPlayerConfig.pauseResumeIconSize)) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center).fillMaxSize(),
                        color = audioPlayerConfig.loadingIndicatorColor
                    )
                }
            }
        }

        // Next Icon
        AnimatedClickableIcon(
            painterRes = audioPlayerConfig.nextIconResource,
            imageVector = Icons.Filled.SkipNext,
            contentDescription = "Next",
            tint = audioPlayerConfig.iconsTintColor,
            iconSize = audioPlayerConfig.previousNextIconSize,
            animationDuration = audioPlayerConfig.controlClickAnimationDuration,
            onClick = {
                onNextClick()
            }
        )

        // Shuffle Icon
        AnimatedClickableIcon(
            painterRes = if (isShuffle) audioPlayerConfig.shuffleOnIconResource else audioPlayerConfig.shuffleOffIconResource,
            imageVector = if (isShuffle) Icons.Filled.ShuffleOn else Icons.Filled.Shuffle,
            contentDescription = "Shuffle",
            tint = audioPlayerConfig.iconsTintColor,
            iconSize = audioPlayerConfig.advanceControlIconSize,
            animationDuration = audioPlayerConfig.controlClickAnimationDuration,
            onClick = { onShuffleClick() }
        )
    }
}