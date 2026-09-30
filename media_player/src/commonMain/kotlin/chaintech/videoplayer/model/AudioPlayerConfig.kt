package chaintech.videoplayer.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chaintech.videoplayer.util.PlatformDrawable


data class AudioPlayerConfig(
    var showControl: Boolean = true,
    var isControlsVisible: Boolean = true,
    var backgroundColor: Color = Color(0xFF1A1A2E),
    var coverBackground: Color = Color(0xFF16213E),
    var seekBarThumbColor: Color = Color(0xFF00bfa6),
    var seekBarActiveTrackColor: Color = Color(0xFF00bfa6),
    var seekBarInactiveTrackColor: Color = Color(0xFF2E2E3A),
    var seekBarThumbRadius: Dp = 6.dp,
    var seekBarTrackHeight: Dp = 4.dp,
    var fontColor: Color = Color.White,
    var durationTextStyle: TextStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal
    ),
    var titleTextStyle: TextStyle = TextStyle(
        fontSize = 25.sp,
        fontWeight = FontWeight.Medium
    ),
    var controlsBottomPadding: Dp = 90.dp,
    var playIconResource: PlatformDrawable? = null,
    var pauseIconResource: PlatformDrawable? = null,
    var pauseResumeIconSize: Dp = 40.dp,
    var previousNextIconSize: Dp = 40.dp,
    var previousIconResource: PlatformDrawable? = null,
    var nextIconResource: PlatformDrawable? = null,
    var iconsTintColor: Color = Color.White,
    var loadingIndicatorColor: Color = Color.White,
    var shuffleOnIconResource: PlatformDrawable? = null,
    var shuffleOffIconResource: PlatformDrawable? = null,
    var advanceControlIconSize: Dp = 20.dp,
    var repeatOnIconResource: PlatformDrawable? = null,
    var repeatOffIconResource: PlatformDrawable? = null,
    var controlClickAnimationDuration: Int = 300 //Millis
)