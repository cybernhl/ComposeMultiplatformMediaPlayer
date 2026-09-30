package chaintech.videoplayer.util

import java.security.MessageDigest
import java.util.prefs.Preferences
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class PlaybackPreference {
    private val prefs: Preferences = Preferences.userRoot().node("playback_prefs")

    actual fun savePlaybackPosition(videoUrl: String, position: Float) {
        runBlocking {
            val hashedKey = hashKey(videoUrl)
            withContext(Dispatchers.IO) {
                try {
                    prefs.putFloat(hashedKey, position)
                } catch (e: Exception) {
                    e.printStackTrace() // Log error
                }
            }
        }
    }

    actual fun getPlaybackPosition(videoUrl: String): Float {
        val hashedKey = hashKey(videoUrl)
        return runBlocking {
            withContext(Dispatchers.IO) {
                try {
                    prefs.getFloat(hashedKey, 0f)
                } catch (e: Exception) {
                    e.printStackTrace() // Log error
                    0f // Return default value if any error occurs
                }
            }
        }
    }

    actual companion object {
        private var instance: PlaybackPreference? = null

        actual fun getInstance(): PlaybackPreference? {
            return instance ?: PlaybackPreference().also { instance = it }
        }
    }

    private fun hashKey(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }.take(32) // Take only first 32 chars
    }
}
