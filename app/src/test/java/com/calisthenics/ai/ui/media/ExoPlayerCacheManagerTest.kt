package com.calisthenics.ai.ui.media

import android.content.Context
import androidx.media3.exoplayer.DefaultLoadControl
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExoPlayerCacheManagerTest {

    @Test
    fun maxCacheSizeBytes_is150MB() {
        assertEquals(150L * 1024L * 1024L, ExoPlayerCacheManager.MAX_CACHE_SIZE_BYTES)
    }

    @Test
    fun createFastStartupLoadControl_returnsDefaultLoadControl() {
        val loadControl = ExoPlayerCacheManager.createFastStartupLoadControl()
        assertNotNull(loadControl)
        assertTrue(loadControl is DefaultLoadControl)
    }

    @Test
    fun preloadVideo_blankUrl_doesNotThrow() = runTest {
        val mockContext = mockk<Context>(relaxed = true)
        ExoPlayerCacheManager.preloadVideo(mockContext, "")
        ExoPlayerCacheManager.preloadVideo(mockContext, "   ")
    }

    @Test
    fun isCached_blankUrl_returnsFalseImmediately() {
        val mockContext = mockk<Context>(relaxed = true)
        assertFalse(ExoPlayerCacheManager.isCached(mockContext, ""))
        assertFalse(ExoPlayerCacheManager.isCached(mockContext, "   "))
    }
}
