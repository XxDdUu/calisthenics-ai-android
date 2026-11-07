package com.calisthenics.ai.ui.media

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(UnstableApi::class)
object ExoPlayerCacheManager {

    private const val TAG = "ExoPlayerCacheManager"
    const val MAX_CACHE_SIZE_BYTES = 150L * 1024L * 1024L // 150 MB

    @Volatile
    private var simpleCache: SimpleCache? = null

    @Synchronized
    fun getSimpleCache(context: Context): SimpleCache {
        return simpleCache ?: synchronized(this) {
            simpleCache ?: run {
                val cacheDir = File(context.applicationContext.cacheDir, "exercise_media_cache")
                val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE_BYTES)
                val databaseProvider = StandaloneDatabaseProvider(context.applicationContext)
                SimpleCache(cacheDir, evictor, databaseProvider).also {
                    simpleCache = it
                }
            }
        }
    }

    fun buildCacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        val cache = getSimpleCache(context)
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(8_000)
            .setReadTimeoutMs(15_000)
            .setUserAgent("CalisthenicsAI-Android")

        val upstreamFactory = DefaultDataSource.Factory(context.applicationContext, httpDataSourceFactory)

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /**
     * Optimized LoadControl for fast video startup:
     * - Minimum buffer before playback starts: 500ms (default is 2500ms)
     * - Minimum buffer after re-buffering: 1000ms (default is 5000ms)
     * - Buffer limits: 15s min to 30s max
     */
    fun createFastStartupLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 30_000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    fun createMediaSourceFactory(context: Context): DefaultMediaSourceFactory {
        return DefaultMediaSourceFactory(context)
            .setDataSourceFactory(buildCacheDataSourceFactory(context))
    }

    suspend fun preloadVideo(
        context: Context,
        url: String,
        bytesToPreload: Long = 1024L * 1024L // 1 MB initial buffer
    ) = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext
        try {
            val uri = Uri.parse(url)
            val dataSpec = DataSpec.Builder()
                .setUri(uri)
                .setLength(bytesToPreload)
                .build()
            val dataSource = buildCacheDataSourceFactory(context).createDataSource()
            val cacheWriter = CacheWriter(
                dataSource,
                dataSpec,
                null,
                null
            )
            cacheWriter.cache()
            Log.d(TAG, "Successfully preloaded $bytesToPreload bytes for $url")
        } catch (e: Exception) {
            Log.d(TAG, "Preload skipped or finished for $url: ${e.message}")
        }
    }

    fun isCached(context: Context, url: String, position: Long = 0, length: Long = 512 * 1024): Boolean {
        if (url.isBlank()) return false
        return try {
            getSimpleCache(context).isCached(url, position, length)
        } catch (e: Exception) {
            false
        }
    }
}
