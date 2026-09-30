package chaintech.videoplayer.util

import androidx.compose.runtime.Composable

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal expect class AppBackgroundObserver {
    fun observe(callback: (isBackground: Boolean) -> Unit)
    fun removeObserver()
}

@Composable
internal expect fun rememberAppBackgroundObserver(): AppBackgroundObserver