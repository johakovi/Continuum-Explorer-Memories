package com.troikoss.continuum_explorer.providers

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import com.troikoss.continuum_explorer.managers.ShizukuManager
import com.troikoss.continuum_explorer.model.FileMetadata
import com.troikoss.continuum_explorer.model.ProviderCapabilities
import com.troikoss.continuum_explorer.model.ProviderKind
import com.troikoss.continuum_explorer.model.StorageProvider
import com.troikoss.continuum_explorer.model.UniversalFile
import com.troikoss.continuum_explorer.utils.RestrictedAccessException
import com.troikoss.continuum_explorer.utils.RestrictedCache
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object LocalProvider : StorageProvider {

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    override val kind = ProviderKind.LOCAL
    override val capabilities = ProviderCapabilities(
        canWrite = true,
        canRename = true,
        canDelete = true,
        canRandomAccessRead = true,
        canGetShareableUri = true,
        supportsChildEnumeration = true,
        isRemote = false,
    )

    override fun rootId() = File.separator

    override fun parentId(childId: String): String? {
        val parent = File(childId).parentFile ?: return null
        return parent.absolutePath
    }

    override fun displayName(id: String) = File(id).name.ifEmpty { id }

    override fun exists(id: String): Boolean {
        val f = File(id)
        if (f.exists()) return true
        if (isRestrictedPath(id) && ShizukuManager.hasPermission()) {
            return ShizukuProvider.exists(id)
        }
        return false
    }

    override suspend fun listChildren(id: String): List<UniversalFile> {
        // If we have Shizuku permission and it's a restricted path, FORCE Shizuku
        if (isRestrictedPath(id)) {
            if (ShizukuManager.hasPermission()) {
                return ShizukuProvider.listChildren(id)
            } else {
                throw RestrictedAccessException(id)
            }
        }

        val files = File(id).listFiles()
        return files?.map { it.toUniversalFile() } ?: emptyList()
    }

    override fun getMetadata(id: String): FileMetadata {
        val f = File(id)
        // If file doesn't exist or is in a restricted path, try Shizuku
        if (isRestrictedPath(id) && ShizukuManager.hasPermission()) {
            return ShizukuProvider.getMetadata(id)
        }
        return FileMetadata(f.length(), f.lastModified(), f.isDirectory, null)
    }

    override fun findChild(parentId: String, name: String): UniversalFile? {
        val f = File(parentId, name)
        if (f.exists()) return f.toUniversalFile()
        
        // If standard file check failed but it's a restricted path
        if (isRestrictedPath(parentId) && ShizukuManager.hasPermission()) {
            return ShizukuProvider.findChild(parentId, name)
        }
        return null
    }

    override fun openInput(id: String): InputStream {
        if (isRestrictedPath(id) && ShizukuManager.hasPermission()) {
            return ShizukuProvider.openInput(id)
        }
        return FileInputStream(id)
    }

    override fun openReadFd(id: String): ParcelFileDescriptor? {
        if (isRestrictedPath(id) && ShizukuManager.hasPermission()) {
            return ShizukuProvider.openReadFd(id)
        }
        return ParcelFileDescriptor.open(File(id), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getShareableUri(id: String): Uri? {
        return try {
            FileProvider.getUriForFile(
                appContext,
                appContext.packageName + ".provider",
                File(id)
            )
        } catch (_: Exception) { null }
    }

    override fun createChild(parentId: String, name: String, isDirectory: Boolean): UniversalFile {
        val f = File(parentId, name)
        if (isRestrictedPath(parentId) && ShizukuManager.hasPermission()) {
            return ShizukuProvider.createChild(parentId, name, isDirectory)
        }
        if (isDirectory) f.mkdirs() else f.createNewFile()
        return f.toUniversalFile()
    }

    override fun createAndOpenOutput(parentId: String, name: String): Pair<UniversalFile, OutputStream> {
        val f = File(parentId, name)
        if (isRestrictedPath(parentId) && ShizukuManager.hasPermission()) {
            return ShizukuProvider.createAndOpenOutput(parentId, name)
        }
        return f.toUniversalFile() to FileOutputStream(f)
    }

    override fun delete(id: String): Boolean {
        val f = File(id)
        if (isRestrictedPath(id) && ShizukuManager.hasPermission()) {
            return ShizukuProvider.delete(id)
        }
        return if (f.isDirectory) f.deleteRecursively() else f.delete()
    }

    override fun rename(id: String, newName: String): UniversalFile? {
        val f = File(id)
        if (RestrictedCache.isRestrictedPath(id) && ShizukuManager.hasPermission()) {
            return ShizukuProvider.rename(id, newName)
        }
        val dest = File(f.parent, newName)
        return if (f.renameTo(dest)) dest.toUniversalFile() else null
    }

    override fun move(id: String, destParentId: String, destName: String): UniversalFile? {
        if ((RestrictedCache.isRestrictedPath(id) || RestrictedCache.isRestrictedPath(destParentId)) && ShizukuManager.hasPermission()) {
            return ShizukuProvider.move(id, destParentId, destName)
        }
        val src = File(id)
        val dest = File(destParentId, destName)
        return if (src.renameTo(dest)) dest.toUniversalFile() else null
    }

    override fun copy(id: String, destParentId: String, destName: String): UniversalFile? {
        if ((RestrictedCache.isRestrictedPath(id) || RestrictedCache.isRestrictedPath(destParentId)) && ShizukuManager.hasPermission()) {
            return ShizukuProvider.copy(id, destParentId, destName)
        }
        val src = File(id)
        val dest = File(destParentId, destName)
        return try {
            if (src.isDirectory) src.copyRecursively(dest, overwrite = true)
            else src.copyTo(dest, overwrite = true)
            dest.toUniversalFile()
        } catch (_: Exception) { null }
    }

    private fun isRestrictedPath(path: String): Boolean = RestrictedCache.isRestrictedPath(path)

    fun File.toUniversalFile(): UniversalFile = UniversalFile(
        name = this.name,
        isDirectory = this.isDirectory,
        lastModified = this.lastModified(),
        length = this.length(),
        provider = LocalProvider,
        providerId = this.absolutePath,
        parentId = this.parentFile?.absolutePath,
    )
}
