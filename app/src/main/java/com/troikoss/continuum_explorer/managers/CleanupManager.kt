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
     * @param includeThumbnails if true, also wipes the thumbnails cache.
     */
    suspend fun clearCache(context: Context, includeThumbnails: Boolean = false) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting cache cleanup (includeThumbnails=$includeThumbnails)...")
            
            // 1. Clear Remote Cache (network files)
            RemoteCache.evictAll(context)
            
            // 2. Clear Thumbnails if requested
            if (includeThumbnails) {
                val thumbDir = File(context.cacheDir, "thumbnails")
                if (thumbDir.exists()) {
                    thumbDir.deleteRecursively()
                }
            }
            
            // 3. Clear .temp directory in external storage
            val tempDir = RestrictedCache.getTempDir()
            if (tempDir.exists()) {
                tempDir.deleteRecursively()
                tempDir.mkdirs() // Recreate empty .temp
            }
            
            // 4. Clear generic cache items
            context.cacheDir.listFiles()?.forEach { file ->
                // Don't delete directories we might want to keep
                if (file.name != "network" && file.name != "thumbnails") {
                    file.deleteRecursively()
                }
            }
            
            Log.d(TAG, "Cache cleanup completed.")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cache cleanup", e)
        }
    }

    /**
     * Specifically clears only thumbnails.
     */
    suspend fun clearThumbnails(context: Context) = withContext(Dispatchers.IO) {
        val thumbDir = File(context.cacheDir, "thumbnails")
        if (thumbDir.exists()) {
            thumbDir.deleteRecursively()
        }
    }
}
