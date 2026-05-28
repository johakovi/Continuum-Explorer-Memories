package com.troikoss.continuum_explorer.services

import com.troikoss.continuum_explorer.IFileService
import com.troikoss.continuum_explorer.model.ShizukuFileInfo
import java.io.File
import android.os.ParcelFileDescriptor

class ShizukuFileService : IFileService.Stub() {
    override fun destroy() {
        System.exit(0)
    }

    override fun getDetailedList(path: String): List<ShizukuFileInfo> {
        val folder = File(path)
        val files = folder.listFiles() ?: return emptyList()
        return files.map { file ->
            ShizukuFileInfo(
                name = file.name,
                isDirectory = file.isDirectory,
                size = if (file.isDirectory) 0L else file.length(),
                lastModified = file.lastModified()
            )
        }
    }

    override fun isDirectory(path: String): Boolean = File(path).isDirectory
    override fun getLength(path: String): Long = File(path).length()
    override fun getLastModified(path: String): Long = File(path).lastModified()
    override fun exists(path: String): Boolean = File(path).exists()
    
    override fun delete(path: String): Boolean {
        val file = File(path)
        return if (file.isDirectory) file.deleteRecursively() else file.delete()
    }
    
    override fun rename(path: String, newName: String): Boolean {
        val file = File(path)
        return file.renameTo(File(file.parent, newName))
    }

    override fun mkdir(path: String): Boolean = File(path).mkdirs()
    override fun createNewFile(path: String): Boolean = File(path).createNewFile()

    override fun openFile(path: String, mode: String): ParcelFileDescriptor? {
        val flags = when(mode) {
            "r" -> ParcelFileDescriptor.MODE_READ_ONLY
            "w", "wt" -> ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE
            "wa" -> ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_APPEND
            "rw" -> ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE
            else -> ParcelFileDescriptor.MODE_READ_ONLY
        }
        return try {
            ParcelFileDescriptor.open(File(path), flags)
        } catch (_: Exception) {
            null
        }
    }

    override fun copyFile(sourcePath: String, destPath: String): Boolean {
        return try {
            val src = File(sourcePath)
            val dest = File(destPath)
            if (src.isDirectory) src.copyRecursively(dest, overwrite = true)
            else {
                src.copyTo(dest, overwrite = true)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    override fun moveFile(sourcePath: String, destPath: String): Boolean {
        return try {
            val src = File(sourcePath)
            val dest = File(destPath)
            if (src.renameTo(dest)) true
            else {
                // Fallback for cross-volume move
                if (src.isDirectory) {
                    if (src.copyRecursively(dest, overwrite = true)) {
                        src.deleteRecursively()
                    } else false
                } else {
                    src.copyTo(dest, overwrite = true)
                    src.delete()
                }
            }
        } catch (_: Exception) {
            false
        }
    }
}
