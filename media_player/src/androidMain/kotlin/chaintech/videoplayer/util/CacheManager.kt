package chaintech.videoplayer.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DefaultDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@UnstableApi
internal object CacheManager {

    private var cache: SimpleCache? = null
    private var databaseProvider: DefaultDatabaseProvider? = null
    private var databaseHelper: SQLiteOpenHelper? = null
    private var activePlayers = 0

    private val lock = Any()

    suspend fun getCache(context: Context): SimpleCache =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                if (cache == null) {
                    cache = createCache(context.applicationContext)
                }
                activePlayers++
                cache!!
            }
        }

    fun release() {
        synchronized(lock) {
            activePlayers--
            if (activePlayers <= 0) {
                try {
                    cache?.release()
                } catch (_: Exception) {
                } finally {
                    cache = null
                    databaseHelper?.close()
                    databaseHelper = null
                    databaseProvider = null
                }
            }
        }
    }

    private fun createCache(context: Context): SimpleCache {
        val cacheSize = 100L * 1024 * 1024 // 100 MB
        val cacheDir = File(context.cacheDir, "video_cache").apply {
            if (!exists()) mkdirs()
        }

        if (databaseHelper == null) {
            databaseHelper = createSQLiteOpenHelper(context)
        }

        if (databaseProvider == null) {
            databaseProvider = DefaultDatabaseProvider(databaseHelper!!)
        }

        return SimpleCache(
            cacheDir,
            LeastRecentlyUsedCacheEvictor(cacheSize),
            databaseProvider!!
        )
    }

    private fun createSQLiteOpenHelper(context: Context): SQLiteOpenHelper =
        object : SQLiteOpenHelper(context, "media3_cache.db", null, 1) {

            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS cache_metadata (" +
                            "id INTEGER PRIMARY KEY, key TEXT, value TEXT)"
                )
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                db.execSQL("DROP TABLE IF EXISTS cache_metadata")
                onCreate(db)
            }
        }
}
