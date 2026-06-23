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
        val files = folder.listFiles()
        if (files != null) {
            return files.map { file ->
                ShizukuFileInfo(
                    name = file.name,
                    isDirectory = file.isDirectory,
                    size = if (file.isDirectory) 0L else file.length(),
                    lastModified = file.lastModified()
                )
            }
        }

        // Fallback to shell ls if listFiles() fails (common on Android 14+ for restricted paths)
        return try {
            val process = ProcessBuilder("sh", "-c", "ls -F \"$1\"", "sh", path).start()
            val output = process.inputStream.bufferedReader().readLines()
            process.waitFor()
            
            output.mapNotNull { line ->
                if (line.isEmpty()) return@mapNotNull null
                val name = line.trimEnd('/', '*', '@', '=', '|')
                val isDir = line.endsWith('/')
                val fullChildPath = if (path.endsWith("/")) "$path$name" else "$path/$name"
                
                ShizukuFileInfo(
                    name = name,
                    isDirectory = isDir,
                    size = if (isDir) 0L else getLength(fullChildPath),
                    lastModified = getLastModified(fullChildPath)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun isDirectory(path: String): Boolean = try {
        val file = File(path)
        if (file.exists()) file.isDirectory
        else runShell("test -d \"$1\"", path)
    } catch (_: Exception) {
        runShell("test -d \"$1\"", path)
    }

    override fun getLength(path: String): Long = try {
        val file = File(path)
        if (file.exists()) file.length()
        else runShellOutput("stat -c %s \"$1\"", path).toLongOrNull() ?: 0L
    } catch (_: Exception) {
        runShellOutput("stat -c %s \"$1\"", path).toLongOrNull() ?: 0L
    }

    override fun getLastModified(path: String): Long = try {
        val file = File(path)
        if (file.exists()) file.lastModified()
        else (runShellOutput("stat -c %Y \"$1\"", path).toLongOrNull() ?: 0L) * 1000L
    } catch (_: Exception) {
        (runShellOutput("stat -c %Y \"$1\"", path).toLongOrNull() ?: 0L) * 1000L
    }

    override fun exists(path: String): Boolean = try {
        val file = File(path)
        if (file.exists()) true
        else runShell("test -e \"$1\"", path)
    } catch (_: Exception) {
        runShell("test -e \"$1\"", path)
    }
    
    override fun delete(path: String): Boolean {
        return try {
            // Using sh -c with argument passing ($1) is the most robust way to execute
            // privileged commands on restricted paths in modern Android.
            // We use rm -rf to ensure recursive deletion of directories as expected by FTP clients.
            val process = ProcessBuilder("sh", "-c", "rm -rf -- \"$1\"", "sh", path).start()
            if (process.waitFor() == 0) {
                true
            } else {
                // Fallback to direct delete if shell failed
                val file = File(path)
                if (file.isDirectory) file.deleteRecursively() else file.delete()
            }
        } catch (_: Exception) {
            val file = File(path)
            if (file.isDirectory) file.deleteRecursively() else file.delete()
        }
    }
    
    override fun rename(path: String, newName: String): Boolean {
        val file = File(path)
        val dest = File(file.parent, newName)
        return try {
            ProcessBuilder("sh", "-c", "mv -- \"$1\" \"$2\"", "sh", path, dest.absolutePath).start().waitFor() == 0
        } catch (_: Exception) {
            file.renameTo(dest)
        }
    }

    override fun mkdir(path: String): Boolean {
        return try {
            ProcessBuilder("sh", "-c", "mkdir -p -- \"$1\"", "sh", path).start().waitFor() == 0
        } catch (_: Exception) {
            File(path).mkdirs()
        }
    }
    
    override fun createNewFile(path: String): Boolean {
        return try {
            ProcessBuilder("sh", "-c", "touch -- \"$1\"", "sh", path).start().waitFor() == 0
        } catch (_: Exception) {
            File(path).createNewFile()
        }
    }

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
            val process = ProcessBuilder("sh", "-c", "cp -rf -- \"$1\" \"$2\"", "sh", sourcePath, destPath).start()
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
            val process = ProcessBuilder("sh", "-c", "mv -- \"$1\" \"$2\"", "sh", sourcePath, destPath).start()
            if (process.waitFor() == 0) true
            else throw Exception("mv failed")
        } catch (_: Exception) {
            // Shell-based fallback for cross-volume moves (e.g., from .temp to /Android/data)
            if (copyFile(sourcePath, destPath)) {
                delete(sourcePath)
            } else {
                false
            }
        }
    }

    override fun setLastModified(path: String, time: Long): Boolean {
        return try {
            val dateStr = java.text.SimpleDateFormat("yyyyMMddHHmm.ss", java.util.Locale.US).format(java.util.Date(time))
            ProcessBuilder("sh", "-c", "touch -t $dateStr -- \"$1\"", "sh", path).start().waitFor() == 0
        } catch (_: Exception) {
            File(path).setLastModified(time)
        }
    }

    private fun runShell(command: String, vararg args: String): Boolean {
        return try {
            val process = ProcessBuilder("sh", "-c", command, "sh", *args).start()
            process.waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }

    private fun runShellOutput(command: String, vararg args: String): String {
        return try {
            val process = ProcessBuilder("sh", "-c", command, "sh", *args).start()
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            output
        } catch (_: Exception) {
            ""
        }
    }
}
