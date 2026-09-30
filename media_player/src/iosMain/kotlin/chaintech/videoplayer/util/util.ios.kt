package chaintech.videoplayer.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import chaintech.videoplayer.model.Platform
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.IO
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.stringWithFormat
import platform.UIKit.UIApplication
import platform.UIKit.UIInterfaceOrientationMaskLandscapeRight
import platform.UIKit.UIInterfaceOrientationMaskPortrait
import platform.UIKit.UIStatusBarAnimation
import platform.UIKit.setStatusBarHidden

actual fun formatMinSec(value: Float): String {
    val hour = (value / 3600).toInt()
    val remainingSecondsAfterHours = (value % 3600).toInt()
    val minutes = remainingSecondsAfterHours / 60
    val seconds = remainingSecondsAfterHours % 60

    val strHour: String = if (hour > 0) {
        NSString.stringWithFormat(format = "%02d:", hour)
    } else {
        ""
    }
    val strMinutes: String = NSString.stringWithFormat(format = "%02d:", minutes)
    val strSeconds: String = NSString.stringWithFormat(format = "%02d", seconds)

    return "${strHour}${strMinutes}${strSeconds}"
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun LandscapeOrientation(
    enableFullEdgeToEdge: Boolean,
    isLandscape: Boolean,
    content: @Composable () -> Unit
) {
    val systemVersion = NSProcessInfo.processInfo.operatingSystemVersion.useContents {
        "${this.majorVersion}.${this.minorVersion}".toDoubleOrNull() ?: 16.0
    }
    if (systemVersion >= 16.0) {
        val windowScene = remember { UIApplication.sharedApplication.keyWindow?.windowScene }
        DisposableEffect(isLandscape) {
            val orientation = if (isLandscape) {
                UIInterfaceOrientationMaskLandscapeRight
            } else {
                UIInterfaceOrientationMaskPortrait
            }
            windowScene?.requestGeometryUpdateWithPreferences(platform.UIKit.UIWindowSceneGeometryPreferencesIOS().apply {
                    interfaceOrientations = orientation
                }, errorHandler = null)
            UIApplication.sharedApplication.setStatusBarHidden(isLandscape, UIStatusBarAnimation.UIStatusBarAnimationSlide)

            onDispose {
                windowScene?.requestGeometryUpdateWithPreferences(platform.UIKit.UIWindowSceneGeometryPreferencesIOS().apply {
                        interfaceOrientations = UIInterfaceOrientationMaskPortrait
                    }, errorHandler = null)
                UIApplication.sharedApplication.setStatusBarHidden(false, UIStatusBarAnimation.UIStatusBarAnimationSlide)
            }
        }
    }
    content()
}

actual val youtubeProgressColor: Color
    get() = Color(0xFFdddddd)

internal fun isLocal(url: String): Boolean {
    return if (url.startsWith("http://") || url.startsWith("https://")) {
        false
    } else {
        true
    }
}

internal fun createUrl(url: String): NSURL? {
    return if (isLocal(url)) {
        NSURL.fileURLWithPath(url)
    } else {
        NSURL.URLWithString(url)
    }
}

actual fun isPlatform(): Platform {
    return Platform.Ios
}

actual fun provideDispatcher(): CoroutineDispatcher = Dispatchers.IO
