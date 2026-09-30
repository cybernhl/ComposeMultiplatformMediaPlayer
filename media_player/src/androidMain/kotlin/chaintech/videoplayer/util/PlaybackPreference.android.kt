package chaintech.videoplayer.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import java.security.MessageDigest

private val Context.dataStore by preferencesDataStore(name = "playback_prefs")

actual class PlaybackPreference(private val context: Context) {

    actual fun savePlaybackPosition(videoUrl: String, position: Float) {
        val key = getHashedKey(videoUrl)
        runBlocking {
            try {
                context.dataStore.edit { prefs ->
                    prefs[floatPreferencesKey(key)] = position
                }
            } catch (e: Exception) {
                e.printStackTrace() // Log the error
            }
        }
    }

    actual fun getPlaybackPosition(videoUrl: String): Float {
        val key = getHashedKey(videoUrl)
        return runBlocking {
            try {
                context.dataStore.data.firstOrNull()?.get(floatPreferencesKey(key)) ?: 0f
            } catch (e: Exception) {
                e.printStackTrace() // Log the error
                0f // Return default value to prevent crashes
            }
        }
    }

    actual companion object {
        private var instance: PlaybackPreference? = null

        actual fun getInstance(): PlaybackPreference? {
            return instance
        }

        fun initialize(context: Context) {
            instance = PlaybackPreference(context.applicationContext)
        }
    }

    private fun getHashedKey(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hashBytes = md.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }.take(32) // Limit to 32 chars
    }
}