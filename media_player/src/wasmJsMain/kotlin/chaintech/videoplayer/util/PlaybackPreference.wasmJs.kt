package chaintech.videoplayer.util

import kotlinx.browser.window

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class PlaybackPreference {
    actual fun savePlaybackPosition(videoUrl: String, position: Float) = try {
        window.localStorage.setItem(videoUrl, position.toString())
    } catch (_: Throwable) {
    }

    actual fun getPlaybackPosition(videoUrl: String): Float {
        return try {
            window.localStorage.getItem(videoUrl)?.toFloatOrNull() ?: 0f
        } catch (_: Throwable) {
            0f
        }
    }

    actual companion object {
        private var instance: PlaybackPreference? = null

        actual fun getInstance(): PlaybackPreference? {
            return instance ?: PlaybackPreference().also { instance = it }
        }
    }

}