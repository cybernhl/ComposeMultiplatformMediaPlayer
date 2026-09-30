package chaintech.videoplayer.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

internal actual class AppBackgroundObserver {
    private val lifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get()
    private val lifecycle = lifecycleOwner.lifecycle
    private var observer: LifecycleEventObserver? = null

    actual fun observe(callback: (isBackground: Boolean) -> Unit) {
        removeObserver()
        observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> callback(false)
                Lifecycle.Event.ON_STOP -> callback(true)
                else -> Unit
            }
        }
        lifecycle.addObserver(observer!!)
    }

    actual fun removeObserver() {
        observer?.let {
            lifecycle.removeObserver(it)
            observer = null
        }
    }
}

@Composable
internal actual fun rememberAppBackgroundObserver(): AppBackgroundObserver {
    return remember { AppBackgroundObserver() }
}