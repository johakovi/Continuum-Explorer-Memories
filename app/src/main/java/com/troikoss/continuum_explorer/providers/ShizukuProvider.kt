package com.troikoss.continuum_explorer.providers

import android.net.Uri
import android.os.ParcelFileDescriptor
import com.troikoss.continuum_explorer.managers.ShizukuManager
import com.troikoss.continuum_explorer.model.*
import java.io.InputStream
import java.io.OutputStream
import java.io.File

object ShizukuProvider : StorageProvider {
    override val kind = ProviderKind.SHIZUKU
    override val capabilities = ProviderCapabilities(
        canWrite = true,
        canRename = true,
        canDelete = true,
        canRandomAccessRead = true,
        canGetShareableUri = false,
        supportsChildEnumeration = true,
        isRemote = false,
    )

    override fun rootId(): String = "/"

    override fun parentId(childId: String): String? {
        if (childId == "/" || childId.isEmpty()) return null
        val file = File(childId)
        return file.parent ?: "/"
    }

    override fun displayName(id: String): String = File(id).name.ifEmpty { id }

    override fun exists(id: String): Boolean = ShizukuManager.getServiceBlocking()?.exists(id) ?: false

    override suspend fun listChildren(id: String): List<UniversalFile> {
        val service = ShizukuManager.getService() ?: return emptyList()

        val infoList = try { service.getDetailedList(id) } catch (_: Exception) { null } ?: return emptyList()
        return infoList.map { info ->
            val path = if (id.endsWith("/")) "$id${info.name}" else "$id/${info.name}"
            UniversalFile(
                name = info.name,
                isDirectory = info.isDirectory,
                lastModified = info.lastModified,
                length = info.size,
                provider = this,
                providerId = path,
                parentId = id
            )
        }
    }

    override fun getMetadata(id: String): FileMetadata {
        val service = ShizukuManager.getServiceBlocking()
        return FileMetadata(
            size = service?.getLength(id) ?: 0L,
            lastModified = service?.getLastModified(id) ?: 0L,
            isDirectory = service?.isDirectory(id) ?: false,
            mimeType = null
        )
    }

    override fun findChild(parentId: String, name: String): UniversalFile? {
        val path = if (parentId.endsWith("/")) "$parentId$name" else "$parentId/$name"
        val service = ShizukuManager.getServiceBlocking() ?: return null
        if (!try { service.exists(path) } catch (_: Exception) { false }) return null
        return UniversalFile(
            name = name,
            isDirectory = service.isDirectory(path),
            lastModified = service.getLastModified(path),
            length = if (service.isDirectory(path)) 0L else service.getLength(path),
            provider = this,
            providerId = path,
            parentId = parentId
        )
    }

    override fun openInput(id: String): InputStream {
        val fd = openReadFd(id) ?: throw java.io.IOException("Failed to open $id")
        return ParcelFileDescriptor.AutoCloseInputStream(fd)
    }

    override fun openReadFd(id: String): ParcelFileDescriptor? = 
        ShizukuManager.getServiceBlocking()?.openFile(id, "r")

    override fun getShareableUri(id: String): Uri? = null

    override fun createChild(parentId: String, name: String, isDirectory: Boolean): UniversalFile {
        val path = if (parentId.endsWith("/")) "$parentId$name" else "$parentId/$name"
        val service = ShizukuManager.getServiceBlocking() ?: throw java.io.IOException("Shizuku service unavailable")
        if (isDirectory) service.mkdir(path) else service.createNewFile(path)
        return findChild(parentId, name) ?: throw java.io.IOException("Failed to create $path")
    }

    override fun createAndOpenOutput(parentId: String, name: String): Pair<UniversalFile, OutputStream> {
        val path = if (parentId.endsWith("/")) "$parentId$name" else "$parentId/$name"
        val service = ShizukuManager.getServiceBlocking() ?: throw java.io.IOException("Shizuku service unavailable")
        service.createNewFile(path)
        val fd = service.openFile(path, "w") ?: throw java.io.IOException("Failed to open $path for writing")
        return findChild(parentId, name)!! to ParcelFileDescriptor.AutoCloseOutputStream(fd)
    }

    override fun delete(id: String): Boolean = ShizukuManager.getServiceBlocking()?.delete(id) ?: false

    override fun rename(id: String, newName: String): UniversalFile? {
        val service = ShizukuManager.getServiceBlocking() ?: return null
        if (service.rename(id, newName)) {
            val parent = parentId(id) ?: return null
            return findChild(parent, newName)
        }
        return null
    }

    override fun move(id: String, destParentId: String, destName: String): UniversalFile? {
        val destPath = if (destParentId.endsWith("/")) "$destParentId$destName" else "$destParentId/$destName"
        if (ShizukuManager.getServiceBlocking()?.moveFile(id, destPath) == true) {
            return findChild(destParentId, destName)
        }
        return null
    }

    override fun copy(id: String, destParentId: String, destName: String): UniversalFile? {
        val destPath = if (destParentId.endsWith("/")) "$destParentId$destName" else "$destParentId/$destName"
        if (ShizukuManager.getServiceBlocking()?.copyFile(id, destPath) == true) {
            return findChild(destParentId, destName)
        }
        return null
    }
}
