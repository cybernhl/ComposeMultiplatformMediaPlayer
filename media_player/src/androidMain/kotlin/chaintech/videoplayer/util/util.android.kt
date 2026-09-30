package chaintech.videoplayer.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Base64
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import chaintech.videoplayer.host.DrmConfig
import chaintech.videoplayer.model.Platform
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.nio.charset.StandardCharsets

@SuppressLint("DefaultLocale")
actual fun formatMinSec(value: Float): String {
    return if (value == 0f) {
        "00:00"
    } else {
        // Calculate hours, minutes, and seconds
        val hours = (value / 3600).toInt()
        val minutes = ((value % 3600) / 60).toInt()
        val seconds = (value % 60).toInt()

        // Format the output string
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
actual fun LandscapeOrientation(
    enableFullEdgeToEdge: Boolean,
    isLandscape: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity ?: run {
        content()
        return
    }

    val window by lazy { activity.window }
    val windowInsetsController by lazy {
        window?.let { WindowCompat.getInsetsController(it, it.decorView) }
    }
    val originalOrientation = remember {
        activity.requestedOrientation
    }

    fun applyEdgeToEdgeSettings() {
        if (!enableFullEdgeToEdge) return
        window?.let { WindowCompat.setDecorFitsSystemWindows(it, false) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window?.attributes = window?.attributes?.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        windowInsetsController?.apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun reset() {
        if (!enableFullEdgeToEdge) return
        window?.let { WindowCompat.setDecorFitsSystemWindows(it, true) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window?.attributes = window?.attributes?.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            }
        }
        windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
    }

    LaunchedEffect(isLandscape) {
        if (isLandscape) {
            if (enableFullEdgeToEdge) applyEdgeToEdgeSettings()
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity.requestedOrientation = originalOrientation
            reset()
        }
    }

    DisposableEffect(isLandscape) {
        onDispose {
            activity.requestedOrientation = originalOrientation
            reset()
        }
    }
    content()
}

actual val youtubeProgressColor: Color
    get() = Color(0xFF343434)


actual fun isPlatform(): Platform {
    return Platform.Android
}

internal object VideoUtils {
    private fun encodeBase64(input: String): String {
        val bytes = input.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    fun createDrmJson(drmConfig: DrmConfig): ByteArray {
        val key = encodeBase64(drmConfig.key)
        val keyId = encodeBase64(drmConfig.keyId)
        val json = """{"keys":[{"kty":"oct","k":"$key","kid":"$keyId"}],"type":"temporary"}"""
        return json.toByteArray(StandardCharsets.UTF_8)
    }
}

actual fun provideDispatcher(): CoroutineDispatcher = Dispatchers.IO
