package chaintech.videoplayer.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chaintech.videoplayer.util.PlatformDrawable
import chaintech.videoplayer.util.isMobile

data class VideoPlayerConfig(
    var showControls: Boolean = true,
    var isPauseResumeEnabled: Boolean = true,
    var isSeekBarVisible: Boolean = true,
    var isDurationVisible: Boolean = true,
    var seekBarThumbColor: Color = Color.Red,
    var seekBarActiveTrackColor: Color = Color.White,
    var seekBarInactiveTrackColor: Color = Color.Black.copy(alpha = 0.4f),
    var seekBarThumbRadius: Dp = 6.dp,
    var seekBarTrackHeight: Dp = 4.dp,
    var durationTextColor: Color = Color.White,
    var durationTextStyle: TextStyle = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal
    ),
    var seekBarBottomPadding: Dp = 10.dp,
    var seekBarBottomPaddingInFullScreen: Dp = 30.dp,
    var playIconResource: PlatformDrawable? = null,
    var pauseIconResource: PlatformDrawable? = null,
    var pauseResumeIconSize: Dp = if (!isMobile()) {
        30.dp
    } else {
        40.dp
    },
    var reelVerticalScrolling: Boolean = true,
    var isAutoHideControlEnabled: Boolean = true,
    var controlHideIntervalSeconds: Int = 3,  //Seconds
    var isFastForwardBackwardEnabled: Boolean = true,
    var fastForwardBackwardIconSize: Dp = if (!isMobile()) {
        30.dp
    } else {
        40.dp
    },
    var fastForwardIconResource: PlatformDrawable? = null,
    var fastBackwardIconResource: PlatformDrawable? = null,
    var fastForwardBackwardIntervalSeconds: Float = 10f,  //Seconds
    var isMuteControlEnabled: Boolean = true,
    var unMuteIconResource: PlatformDrawable? = null,
    var muteIconResource: PlatformDrawable? = null,
    var topControlSize: Dp = 30.dp,
    var isSpeedControlEnabled: Boolean = true,
    var speedIconResource: PlatformDrawable? = null,
    var isFullScreenEnabled: Boolean = true,
    var controlTopPadding: Dp = 15.dp,
    var isScreenLockEnabled: Boolean = true,
    var iconsTintColor: Color = Color.White,
    var isScreenResizeEnabled: Boolean = true,
    var loadingIndicatorColor: Color = Color.White,
    var loaderView: (@Composable () -> Unit)? = null,
    var isLiveStream: Boolean = false,
    var controlClickAnimationDuration: Int = 300, //Millis
    var backdropAlpha: Float = 0.25f,
    var autoPlayNextReel: Boolean = true,
    var enableResumePlayback: Boolean = true, //If true, resume from last saved position
    var isZoomEnabled: Boolean = true,
    var isGestureVolumeControlEnabled: Boolean = true,
    var showVideoQualityOptions: Boolean = true,
    var showAudioTracksOptions: Boolean = true,
    var showSubTitlesOptions: Boolean = true,
    var watermarkConfig: WatermarkConfig? = null,
    var chapters: List<Chapter>? = null,
    var enableFullEdgeToEdge: Boolean = true,
    var enableBackButton: Boolean = false,
    var backIconResource: PlatformDrawable? = null,
    var backActionCallback: (() -> Unit)? = null,
    var enablePIPControl: Boolean = true,
    var enableLongPressFastForward: Boolean = false,
    var longPressPlaybackSpeed: Float = 2.0f,
    var showControlsOverride: Boolean? = null
){
    internal val resolvedShowControls: Boolean
        get() = showControlsOverride ?: showControls
}

data class WatermarkConfig(
    val content: @Composable () -> Unit,
    val stayDelay: Long = 0L,
    val hideDelay: Long = 0L
)

data class Chapter(
    val startTime: Long = 0L,
    val title: String = "",
    val markColor: Color = Color(0xFF00BCD4),
    val markWidth: Dp = 3.dp
)