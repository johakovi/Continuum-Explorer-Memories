package com.troikoss.continuum_explorer.services

import com.troikoss.continuum_explorer.managers.ShizukuManager
import com.troikoss.continuum_explorer.model.UniversalFile
import com.troikoss.continuum_explorer.providers.ShizukuProvider
import com.troikoss.continuum_explorer.providers.StorageProviders
import kotlinx.coroutines.runBlocking
import org.apache.ftpserver.ftplet.FileSystemView
import org.apache.ftpserver.ftplet.FtpFile
import org.apache.ftpserver.ftplet.User
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.URLDecoder

class ShizukuFileSystemView(private val user: User, private val homeDir: String) : FileSystemView {
    private var currDir = "/"

    override fun getHomeDirectory(): FtpFile {
        val provider = if (homeDir.contains("/Android/data", ignoreCase = true)) ShizukuProvider else StorageProviders.providerFor(com.troikoss.continuum_explorer.model.ProviderKind.LOCAL)
        val meta = provider.getMetadata(homeDir)
        val uf = UniversalFile(File(homeDir).name, true, meta.lastModified, 0, provider, homeDir)
        return UniversalFtpFile(uf, "/", user)
    }

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
        val virtualPath = getNormalizedVirtualPath(file)
        if (virtualPath == "/") return getHomeDirectory()

        val root = getHomeDirectory() as UniversalFtpFile
        val rootUf = root.universalFile
        
        val segments = virtualPath.split("/").filter { it.isNotEmpty() }
        var currentUf = rootUf
        
        for (segment in segments) {
            currentUf = currentUf.provider.findChild(currentUf.providerId, segment) 
                ?: return NonExistentFtpFile(virtualPath)
        }
        
        return UniversalFtpFile(currentUf, virtualPath, user)
    }

    override fun isRandomAccessible(): Boolean = true
    override fun dispose() {}

    private fun getNormalizedVirtualPath(path: String): String {
        var p = try { URLDecoder.decode(path, "UTF-8") } catch (_: Exception) { path }
        if (!p.startsWith("/")) {
            p = if (currDir.endsWith("/")) currDir + p else "$currDir/$p"
        }
        val segments = p.split("/").filter { it.isNotEmpty() && it != "." }
        val resolved = mutableListOf<String>()
        for (segment in segments) {
            if (segment == "..") {
                if (resolved.isNotEmpty()) resolved.removeAt(resolved.size - 1)
            } else {
                resolved.add(segment)
            }
        }
        return "/" + resolved.joinToString("/")
    }
}

class GamesFileSystemView(private val context: android.content.Context, private val user: User) : FileSystemView {
    private var currDir = "/"
    private val games by lazy {
        val appConfigs = com.troikoss.continuum_explorer.utils.AppConfigurations(context)
        com.troikoss.continuum_explorer.managers.GamesManager.getGames(context, appConfigs)
    }

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
        val virtualPath = getNormalizedVirtualPath(file)
        if (virtualPath == "/" || virtualPath.isEmpty()) return VirtualRootFtpFile(games, user)
        
        val segments = virtualPath.split("/").filter { it.isNotEmpty() }
        val gameName = segments[0]
        
        // Robust matching for game names, including potential shortening or modified names by clients
        val game = games.find { it.name.equals(gameName, ignoreCase = true) }
            ?: games.find { it.name.replace(": ", ":").equals(gameName, ignoreCase = true) }
            ?: games.find { it.name.substringBefore(":").trim().equals(gameName.trim(), ignoreCase = true) }
            ?: games.find { it.name.substringAfterLast(":").trim().equals(gameName.trim(), ignoreCase = true) }
            ?: return NonExistentFtpFile(virtualPath)
        
        val effectiveVirtualPath = if (segments.size == 1) "/${game.name}" else virtualPath
        
        if (segments.size == 1) {
            return UniversalFtpFile(game, effectiveVirtualPath, user)
        }
        
        var currentUf = game
        for (i in 1 until segments.size) {
            currentUf = currentUf.provider.findChild(currentUf.providerId, segments[i])
                ?: return NonExistentFtpFile(virtualPath)
        }
        
        return UniversalFtpFile(currentUf, effectiveVirtualPath, user)
    }

    override fun isRandomAccessible(): Boolean = true
    override fun dispose() {}

    private fun getNormalizedVirtualPath(path: String): String {
        var p = try { URLDecoder.decode(path, "UTF-8") } catch (_: Exception) { path }
        if (!p.startsWith("/")) {
            p = if (currDir.endsWith("/")) currDir + p else "$currDir/$p"
        }
        val segments = p.split("/").filter { it.isNotEmpty() && it != "." }
        val resolved = mutableListOf<String>()
        for (segment in segments) {
            if (segment == "..") {
                if (resolved.isNotEmpty()) resolved.removeAt(resolved.size - 1)
            } else {
                resolved.add(segment)
            }
        }
        return "/" + resolved.joinToString("/")
    }
}

class UniversalFtpFile(
    val universalFile: UniversalFile,
    private val virtualPath: String,
    private val user: User
) : FtpFile {

    override fun getAbsolutePath(): String = virtualPath
    override fun getName(): String = if (virtualPath == "/") "/" else virtualPath.substringAfterLast('/')
    override fun isHidden(): Boolean = getName().startsWith(".")
    override fun isDirectory(): Boolean = universalFile.isDirectory
    override fun isFile(): Boolean = !universalFile.isDirectory
    override fun doesExist(): Boolean = true
    override fun isReadable(): Boolean = true
    override fun isWritable(): Boolean = true
    override fun isRemovable(): Boolean = false
    override fun getOwnerName(): String = "admin"
    override fun getGroupName(): String = "admin"
    override fun getLinkCount(): Int = if (isDirectory) 3 else 1
    override fun getLastModified(): Long = universalFile.lastModified
    override fun setLastModified(time: Long): Boolean = false
    override fun getSize(): Long = universalFile.length
    override fun getPhysicalFile(): Any? = null

    override fun mkdir(): Boolean = try {
        val parentId = universalFile.provider.parentId(universalFile.providerId)
        if (parentId != null) {
            universalFile.provider.createChild(parentId, getName(), true)
            true
        } else false
    } catch (_: Exception) { false }

    override fun delete(): Boolean = try { universalFile.provider.delete(universalFile.providerId) } catch (_: Exception) { false }
    override fun move(destination: FtpFile): Boolean = false

    override fun listFiles(): List<FtpFile> {
        if (!isDirectory) return emptyList()
        val children = runBlocking {
            try { universalFile.provider.listChildren(universalFile.providerId) } catch (_: Exception) { emptyList() }
        }
        return children.map { child ->
            val childVirtualPath = if (virtualPath.endsWith("/")) "$virtualPath${child.name}" else "$virtualPath/${child.name}"
            UniversalFtpFile(child, childVirtualPath, user)
        }
    }

    override fun createOutputStream(offset: Long): OutputStream {
        val parentId = universalFile.provider.parentId(universalFile.providerId) ?: throw java.io.IOException("No parent")
        val (_, stream) = universalFile.provider.createAndOpenOutput(parentId, getName())
        return stream
    }

    override fun createInputStream(offset: Long): InputStream {
        val stream = universalFile.provider.openInput(universalFile.providerId)
        if (offset > 0) stream.skip(offset)
        return stream
    }
}

class VirtualRootFtpFile(private val games: List<UniversalFile>, private val user: User) : FtpFile {
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
    override fun listFiles(): List<FtpFile> = games.map { UniversalFtpFile(it, "/${it.name}", user) }
    override fun createOutputStream(offset: Long): OutputStream = throw java.io.IOException("Read only")
    override fun createInputStream(offset: Long): InputStream = throw java.io.IOException("Is directory")
}

class NonExistentFtpFile(private val virtualPath: String) : FtpFile {
    override fun getAbsolutePath(): String = virtualPath
    override fun getName(): String = virtualPath.substringAfterLast('/')
    override fun isHidden(): Boolean = false
    override fun isDirectory(): Boolean = false
    override fun isFile(): Boolean = false
    override fun doesExist(): Boolean = false
    override fun isReadable(): Boolean = false
    override fun isWritable(): Boolean = false
    override fun isRemovable(): Boolean = false
    override fun getOwnerName(): String = ""
    override fun getGroupName(): String = ""
    override fun getLinkCount(): Int = 0
    override fun getLastModified(): Long = 0L
    override fun setLastModified(time: Long): Boolean = false
    override fun getSize(): Long = 0L
    override fun getPhysicalFile(): Any? = null
    override fun mkdir(): Boolean = false
    override fun delete(): Boolean = false
    override fun move(destination: FtpFile): Boolean = false
    override fun listFiles(): List<FtpFile> = emptyList()
    override fun createOutputStream(offset: Long): OutputStream = throw java.io.IOException("Not found")
    override fun createInputStream(offset: Long): InputStream = throw java.io.IOException("Not found")
}
