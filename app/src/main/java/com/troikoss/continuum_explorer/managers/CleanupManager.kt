package com.troikoss.continuum_explorer.managers

import android.content.Context
import android.util.Log
import com.troikoss.continuum_explorer.utils.RemoteCache
import com.troikoss.continuum_explorer.utils.RestrictedCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object CleanupManager {
    private const val TAG = "CleanupManager"

    /**
     * Clears targeted temporary data.
     * @param includeThumbnails if true, also wipes the thumbnails and gallery metadata.
     */
    suspend fun clearCache(context: Context, includeThumbnails: Boolean = false) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting cache cleanup (includeThumbnails=$includeThumbnails)...")
            
            // 1. Clear Remote Cache (temporary large files from network)
            RemoteCache.evictAll(context)
            
            // 2. Clear .temp directory in external storage (temporary copies of restricted files)
            val tempDir = RestrictedCache.getTempDir()
            if (tempDir.exists()) {
                tempDir.deleteRecursively()
                tempDir.mkdirs() 
            }

            // 3. Clear Thumbnails and Gallery Metadata ONLY if explicitly requested
            if (includeThumbnails) {
                val thumbDir = File(context.cacheDir, "thumbnails")
                if (thumbDir.exists()) {
                    thumbDir.deleteRecursively()
                }
                GalleryCacheManager.clearCache(context)
            }
            
            // 4. Clear generic cache items, but ALWAYS keep gallery and thumbnail directories
            // unless includeThumbnails is true.
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.name == "network") {
                    // Handled by RemoteCache.evictAll
                } else if (file.name == "thumbnails" || file.name == "gallery_metadata") {
                    if (includeThumbnails) {
                        file.deleteRecursively()
                    }
                } else {
                    file.deleteRecursively()
                }
            }
            
            Log.d(TAG, "Cache cleanup completed.")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cache cleanup", e)
        }
    }

    /**
     * Specifically clears only network cache.
     */
    suspend fun clearNetworkCache(context: Context) = withContext(Dispatchers.IO) {
        RemoteCache.evictAll(context)
    }

    /**
     * Specifically clears only thumbnails and gallery metadata.
     */
    suspend fun clearThumbnails(context: Context) = withContext(Dispatchers.IO) {
        val thumbDir = File(context.cacheDir, "thumbnails")
        if (thumbDir.exists()) {
            thumbDir.deleteRecursively()
        }
        GalleryCacheManager.clearCache(context)
    }
}
