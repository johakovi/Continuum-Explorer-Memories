package com.troikoss.continuum_explorer.services

import com.troikoss.continuum_explorer.managers.ShizukuManager
import com.troikoss.continuum_explorer.providers.ShizukuProvider
import org.apache.ftpserver.ftplet.FileSystemView
import org.apache.ftpserver.ftplet.FtpFile
import org.apache.ftpserver.ftplet.User
import java.io.InputStream
import java.io.OutputStream
import java.io.File

class ShizukuFileSystemView(private val user: User, private val homeDir: String) : FileSystemView {
    private var currDir = "/"

    override fun getHomeDirectory(): FtpFile = ShizukuFtpFile(homeDir, "/", user)
    override fun getWorkingDirectory(): FtpFile = getFile(currDir)

    override fun changeWorkingDirectory(dir: String): Boolean {
        val file = getFile(dir)
        if (file.isDirectory) {
            currDir = file.absolutePath
            return true
        }
        return false
    }

    override fun getFile(file: String): FtpFile {
        val virtualPath = getVirtualPath(file)
        val realPath = if (virtualPath == "/") homeDir else {
            val suffix = if (virtualPath.startsWith("/")) virtualPath.substring(1) else virtualPath
            if (homeDir.endsWith("/")) "$homeDir$suffix" else "$homeDir/$suffix"
        }
        return ShizukuFtpFile(realPath, virtualPath, user)
    }

    override fun isRandomAccessible(): Boolean = true
    override fun dispose() {}

    private fun getVirtualPath(path: String): String {
        var p = path
        if (!p.startsWith("/")) {
            p = if (currDir.endsWith("/")) currDir + p else "$currDir/$p"
        }
        val normalized = try { java.net.URI(p).normalize().path } catch (_: Exception) { p }
        return if (normalized.isNullOrEmpty()) "/" else normalized
    }
}

class ShizukuFtpFile(
    private val realPath: String,
    private val virtualPath: String,
    private val user: User
) : FtpFile {
    
    private val metadata by lazy { ShizukuProvider.getMetadata(realPath) }

    override fun getAbsolutePath(): String = virtualPath
    override fun getName(): String = if (virtualPath == "/") "/" else File(virtualPath).name
    override fun isHidden(): Boolean = getName().startsWith(".")
    override fun isDirectory(): Boolean = metadata.isDirectory
    override fun isFile(): Boolean = !metadata.isDirectory && doesExist()
    override fun doesExist(): Boolean = ShizukuProvider.exists(realPath)
    override fun isReadable(): Boolean = true
    override fun isWritable(): Boolean = true
    override fun isRemovable(): Boolean = virtualPath != "/"
    override fun getOwnerName(): String = "admin"
    override fun getGroupName(): String = "admin"
    override fun getLinkCount(): Int = if (isDirectory) 3 else 1
    override fun getLastModified(): Long = metadata.lastModified
    override fun setLastModified(time: Long): Boolean = false
    override fun getSize(): Long = metadata.size
    override fun getPhysicalFile(): Any? = null

    override fun mkdir(): Boolean {
        return try {
            ShizukuProvider.createChild(File(realPath).parent ?: "/", getName(), true)
            true
        } catch (_: Exception) { false }
    }

    override fun delete(): Boolean = ShizukuProvider.delete(realPath)

    override fun move(destination: FtpFile): Boolean = false // Complex to implement across virtual/real boundaries

    override fun listFiles(): List<FtpFile> {
        if (!isDirectory) return emptyList()
        val service = ShizukuManager.getServiceBlocking() ?: return emptyList()
        val list = try { service.getDetailedList(realPath) } catch (_: Exception) { null } ?: return emptyList()
        return list.map { info ->
            val childRealPath = if (realPath.endsWith("/")) "$realPath${info.name}" else "$realPath/${info.name}"
            val childVirtualPath = if (virtualPath.endsWith("/")) "$virtualPath${info.name}" else "$virtualPath/${info.name}"
            ShizukuFtpFile(childRealPath, childVirtualPath, user)
        }
    }

    override fun createOutputStream(offset: Long): OutputStream {
        val parent = File(realPath).parent ?: "/"
        val name = getName()
        val (_, stream) = ShizukuProvider.createAndOpenOutput(parent, name)
        return stream
    }

    override fun createInputStream(offset: Long): InputStream {
        val stream = ShizukuProvider.openInput(realPath)
        if (offset > 0) stream.skip(offset)
        return stream
    }
}

class GamesFileSystemView(private val context: android.content.Context, private val user: User) : FileSystemView {
    private var currDir = "/"
    private val games by lazy { com.troikoss.continuum_explorer.managers.GamesManager.getGames(context) }

    override fun getHomeDirectory(): FtpFile = VirtualRootFtpFile(games, user)
    override fun getWorkingDirectory(): FtpFile = getFile(currDir)

    override fun changeWorkingDirectory(dir: String): Boolean {
        val file = getFile(dir)
        if (file.isDirectory) {
            currDir = file.absolutePath
            return true
        }
        return false
    }

    override fun getFile(file: String): FtpFile {
        val path = getAbsolutePath(file)
        if (path == "/") return VirtualRootFtpFile(games, user)
        
        val segments = path.split("/").filter { it.isNotEmpty() }
        val gameName = segments[0]
        val game = games.find { it.name == gameName } ?: return ShizukuFtpFile("/non-existent", "/non-existent", user)
        
        val realBase = game.providerId
        if (segments.size == 1) {
            return VirtualGameFolderFile(path, realBase, user)
        }
        
        val subPath = segments.drop(1).joinToString("/")
        val realPath = if (realBase.endsWith("/")) "$realBase$subPath" else "$realBase/$subPath"
        return ShizukuFtpFile(realPath, path, user)
    }

    override fun isRandomAccessible(): Boolean = true
    override fun dispose() {}

    private fun getAbsolutePath(path: String): String {
        var p = path
        if (!p.startsWith("/")) {
            p = if (currDir.endsWith("/")) currDir + p else "$currDir/$p"
        }
        val normalized = try { java.net.URI(p).normalize().path } catch (_: Exception) { p }
        return if (normalized.isNullOrEmpty()) "/" else normalized
    }
}

class VirtualRootFtpFile(private val games: List<com.troikoss.continuum_explorer.model.UniversalFile>, private val user: User) : FtpFile {
    override fun getAbsolutePath(): String = "/"
    override fun getName(): String = "/"
    override fun isHidden(): Boolean = false
    override fun isDirectory(): Boolean = true
    override fun isFile(): Boolean = false
    override fun doesExist(): Boolean = true
    override fun isReadable(): Boolean = true
    override fun isWritable(): Boolean = false
    override fun isRemovable(): Boolean = false
    override fun getOwnerName(): String = "admin"
    override fun getGroupName(): String = "admin"
    override fun getLinkCount(): Int = games.size + 2
    override fun getLastModified(): Long = 0L
    override fun setLastModified(time: Long): Boolean = false
    override fun getSize(): Long = 0L
    override fun getPhysicalFile(): Any? = null
    override fun mkdir(): Boolean = false
    override fun delete(): Boolean = false
    override fun move(destination: FtpFile): Boolean = false
    override fun listFiles(): List<FtpFile> = games.map { VirtualGameFolderFile("/${it.name}", it.providerId, user) }
    override fun createOutputStream(offset: Long): OutputStream = throw java.io.IOException("Read only")
    override fun createInputStream(offset: Long): InputStream = throw java.io.IOException("Is directory")
}

class VirtualGameFolderFile(private val virtualPath: String, private val realPath: String, private val user: User) : FtpFile {
    private val wrapped = ShizukuFtpFile(realPath, virtualPath, user)

    override fun getAbsolutePath(): String = virtualPath
    override fun getName(): String = if (virtualPath == "/") "/" else File(virtualPath).name
    override fun isHidden(): Boolean = false
    override fun isDirectory(): Boolean = true
    override fun isFile(): Boolean = false
    override fun doesExist(): Boolean = true
    override fun isReadable(): Boolean = true
    override fun isWritable(): Boolean = true
    override fun isRemovable(): Boolean = false
    override fun getOwnerName(): String = "admin"
    override fun getGroupName(): String = "admin"
    override fun getLinkCount(): Int = 3
    override fun getLastModified(): Long = wrapped.lastModified
    override fun setLastModified(time: Long): Boolean = false
    override fun getSize(): Long = 0L
    override fun getPhysicalFile(): Any? = null
    override fun mkdir(): Boolean = false
    override fun delete(): Boolean = false
    override fun move(destination: FtpFile): Boolean = false
    override fun listFiles(): List<FtpFile> = wrapped.listFiles()
    override fun createOutputStream(offset: Long): OutputStream = throw java.io.IOException("Is directory")
    override fun createInputStream(offset: Long): InputStream = throw java.io.IOException("Is directory")
}
