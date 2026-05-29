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
        return try {
            val process = ProcessBuilder("rm", "-rf", path).start()
            process.waitFor() == 0
        } catch (_: Exception) {
            val file = File(path)
            if (file.isDirectory) file.deleteRecursively() else file.delete()
        }
    }
    
    override fun rename(path: String, newName: String): Boolean {
        val file = File(path)
        val dest = File(file.parent, newName)
        return try {
            ProcessBuilder("mv", path, dest.absolutePath).start().waitFor() == 0
        } catch (_: Exception) {
            file.renameTo(dest)
        }
    }

    override fun mkdir(path: String): Boolean {
        return try {
            ProcessBuilder("mkdir", "-p", path).start().waitFor() == 0
        } catch (_: Exception) {
            File(path).mkdirs()
        }
    }
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
            val process = ProcessBuilder("cp", "-r", sourcePath, destPath).start()
            if (process.waitFor() == 0) true
            else throw Exception("cp failed")
        } catch (_: Exception) {
            try {
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
    }

    override fun moveFile(sourcePath: String, destPath: String): Boolean {
        return try {
            val process = ProcessBuilder("mv", sourcePath, destPath).start()
            if (process.waitFor() == 0) true
            else throw Exception("mv failed")
        } catch (_: Exception) {
            try {
                val src = File(sourcePath)
                val dest = File(destPath)
                if (src.renameTo(dest)) true
                else {
                    // Fallback for cross-volume move
                    if (src.isDirectory) {
                        if (src.copyRecursively(dest, overwrite = true)) {
                            delete(sourcePath)
                        } else false
                    } else {
                        src.copyTo(dest, overwrite = true)
                        delete(sourcePath)
                    }
                }
            } catch (_: Exception) {
                false
            }
        }
    }
}
