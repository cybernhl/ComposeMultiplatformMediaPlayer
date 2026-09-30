package chaintech.videoplayer.util

import platform.Foundation.NSUserDefaults

actual class PlaybackPreference {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun savePlaybackPosition(videoUrl: String, position: Float) {
        defaults.setFloat(position, forKey = videoUrl)
    }

    actual fun getPlaybackPosition(videoUrl: String): Float {
        return defaults.floatForKey(videoUrl)
    }

    actual companion object {
        actual fun getInstance(): PlaybackPreference? {
            return PlaybackPreference()
        }
    }
}
