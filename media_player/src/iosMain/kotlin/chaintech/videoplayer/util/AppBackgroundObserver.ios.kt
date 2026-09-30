package chaintech.videoplayer.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationWillResignActiveNotification
import platform.darwin.NSObjectProtocol

internal actual class AppBackgroundObserver {
    private val notificationCenter = NSNotificationCenter.defaultCenter()
    private var observerDidEnterBackground: NSObjectProtocol? = null
    private var observerDidEnterForeground: NSObjectProtocol? = null

    actual fun observe(callback: (isBackground: Boolean) -> Unit) {
        removeObserver()

        observerDidEnterBackground = notificationCenter.addObserverForName(
            name = UIApplicationWillResignActiveNotification,
            `object` = null,
            queue = null
        ) { _ ->
            callback(true)
        }

        observerDidEnterForeground = notificationCenter.addObserverForName(
            name = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = null
        ) { _ ->
            callback(false) // app is back to foreground
        }
    }

    actual fun removeObserver() {
        observerDidEnterBackground?.let {
            notificationCenter.removeObserver(it)
            observerDidEnterBackground = null
        }
        observerDidEnterForeground?.let {
            notificationCenter.removeObserver(it)
            observerDidEnterForeground = null
        }
    }
}

@Composable
internal actual fun rememberAppBackgroundObserver(): AppBackgroundObserver {
    return remember { AppBackgroundObserver() }
}