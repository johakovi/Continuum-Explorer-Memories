package com.troikoss.continuum_explorer.utils

import android.content.Context
import android.os.Environment
import com.troikoss.continuum_explorer.managers.ShizukuManager
import com.troikoss.continuum_explorer.model.UniversalFile
import com.troikoss.continuum_explorer.providers.LocalProvider
import com.troikoss.continuum_explorer.providers.ShizukuProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

object RestrictedCache {

    fun getTempDir(): File {
        val dir = File(Environment.getExternalStorageDirectory(), ".temp")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun isRestricted(file: UniversalFile): Boolean {
        if (file.provider !is LocalProvider && file.provider !is ShizukuProvider) return false
        return isRestrictedPath(file.providerId)
    }

    fun isRestrictedPath(path: String): Boolean {
        val p = path.replace("//", "/").lowercase()
        
        // Common restricted paths across different Android versions/mount points
        val restrictedPatterns = listOf(
            "/android/data",
            "/android/obb",
            "/android/obj",
            "/data/user/",
            "/data/data/"
        )
        
        if (restrictedPatterns.any { p.contains(it) }) return true
        
        // Also check for trailing /android or /android/ which can be restricted on some versions
        if (p.endsWith("/android") || p.endsWith("/android/")) return true

        return false
    }

    fun getCachedFile(file: UniversalFile): File {
        val hash = sha1(file.providerId)
        return File(File(getTempDir(), hash), file.name)
    }

    suspend fun cache(
        context: Context,
        file: UniversalFile,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
    ): File = withContext(Dispatchers.IO) {
        val target = getCachedFile(file)
        
        // If it's already cached and has same size/mtime, maybe skip?
        // But for restricted files, we might always want to refresh if we suspect changes.
        // For now, let's just copy if it doesn't exist or size differs.
        if (target.exists() && target.length() == file.length) return@withContext target
        
        target.parentFile?.mkdirs()
        
        if (!ShizukuManager.hasPermission()) {
            throw IllegalStateException("Shizuku permission required for restricted paths")
        }

        file.provider.openInput(file.providerId).use { input ->
            FileOutputStream(target).use { output ->
                val buf = ByteArray(64 * 1024)
                var copied = 0L
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    output.write(buf, 0, n)
                    copied += n
                    onProgress(copied, file.length)
                }
            }
        }
        target
    }

    suspend fun pushBack(context: Context, tempFile: File, originalPath: String): Boolean = withContext(Dispatchers.IO) {
        if (!ShizukuManager.hasPermission()) return@withContext false
        
        try {
            val service = ShizukuManager.getService() ?: return@withContext false
            // Use shell copy/move via Shizuku service, which bypasses mount-masking issues
            // that affect direct FileDescriptor writing on newer Android versions.
            service.copyFile(tempFile.absolutePath, originalPath)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun sha1(input: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val bytes = digest.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
