package com.troikoss.continuum_explorer.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.storage.StorageManager
import android.provider.Settings
import com.troikoss.continuum_explorer.managers.ShizukuManager
import com.troikoss.continuum_explorer.model.ProviderKind
import com.troikoss.continuum_explorer.model.UniversalFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

object FileSystemUtils {

    data class MountEntry(
        val device: String,
        val mountPoint: String,
        val fsType: String,
        val options: String
    )

    private fun parseProcMounts(): List<MountEntry> {
        val mountsFile = File("/proc/mounts")
        if (!mountsFile.canRead()) {
            return emptyList()
        }
        val entries = mutableListOf<MountEntry>()
        try {
            mountsFile.forEachLine { line ->
                val parts = line.split("\\s+".toRegex())
                if (parts.size >= 4) {
                    val device = parts[0]
                    val mountPoint = parts[1].replace("\\040", " ")
                    val fsType = parts[2]
                    val options = parts[3]
                    entries.add(MountEntry(device, mountPoint, fsType, options))
                }
            }
        } catch (_: Exception) {}
        return entries
    }

    fun getFilesystemType(file: UniversalFile, context: Context): String {
        if (file.provider.kind == ProviderKind.NETWORK_FTP) return "FTP"
        if (file.provider.kind == ProviderKind.NETWORK_SFTP) return "SFTP"
        if (file.provider.kind == ProviderKind.NETWORK_SMB) return "SMB"
        if (file.provider.kind == ProviderKind.NETWORK_WEBDAV) return "WebDAV"
        if (file.provider.kind == ProviderKind.ARCHIVE) return "Archive"

        val path = file.fileRef?.absolutePath ?: file.absolutePath
        return getFilesystemTypeForPath(path, context)
    }

    fun getFilesystemTypeForPath(path: String, context: Context): String {
        if (path.isEmpty()) return "Unknown"

        val normalizedPath = try {
            File(path).canonicalPath
        } catch (_: Exception) {
            path
        }

        val mounts = parseProcMounts()
        if (mounts.isEmpty()) return "Unknown"

        var bestMatch: MountEntry? = null
        var bestLength = -1

        for (entry in mounts) {
            val mp = entry.mountPoint
            if (normalizedPath == mp || normalizedPath.startsWith("$mp/")) {
                if (mp.length > bestLength) {
                    bestLength = mp.length
                    bestMatch = entry
                }
            }
        }

        var rawType = bestMatch?.fsType?.lowercase(Locale.ROOT) ?: "unknown"

        if (rawType == "fuse" || rawType == "sdcardfs" || bestMatch?.mountPoint == "/storage/emulated") {
            val mediaRwMount = mounts.find { it.mountPoint.startsWith("/mnt/media_rw/") && (normalizedPath.contains(it.mountPoint.removePrefix("/mnt/media_rw/")) || bestMatch?.device?.contains(it.mountPoint.removePrefix("/mnt/media_rw/")) == true) }
            if (mediaRwMount != null) {
                rawType = mediaRwMount.fsType.lowercase(Locale.ROOT)
            } else {
                val dataMount = mounts.find { it.mountPoint == "/data" }
                if (dataMount != null) {
                    rawType = dataMount.fsType.lowercase(Locale.ROOT)
                }
            }
        }

        return formatFsName(rawType)
    }

    private fun formatFsName(raw: String): String {
        return when (raw) {
            "vfat", "fat32", "fat", "msdos" -> "FAT32"
            "exfat", "sdfat", "texfat" -> "exFAT"
            "f2fs" -> "F2FS"
            "ext4" -> "ext4"
            "ext3" -> "ext3"
            "ext2" -> "ext2"
            "ntfs", "ntfs3" -> "NTFS"
            "apfs" -> "APFS"
            "hfsplus", "hfs" -> "HFS+"
            "tmpfs" -> "tmpfs"
            "iso9660", "udf" -> "ISO / UDF"
            "unknown" -> "Unknown"
            else -> raw.uppercase(Locale.ROOT)
        }
    }

    fun isExternalStorage(file: UniversalFile, context: Context): Boolean {
        if (file.provider.kind != ProviderKind.LOCAL && file.provider.kind != ProviderKind.SHIZUKU && file.provider.kind != ProviderKind.SAF) {
            return false
        }
        val path = file.fileRef?.absolutePath ?: file.absolutePath
        return isExternalStoragePath(path, context)
    }

    fun isExternalStoragePath(path: String, context: Context): Boolean {
        if (path.isEmpty()) return false
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val volumes = storageManager.storageVolumes
            for (vol in volumes) {
                if (vol.isRemovable) {
                    val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) vol.directory else null
                    if (dir != null && (path == dir.absolutePath || path.startsWith("${dir.absolutePath}/"))) {
                        return true
                    }
                    val uuid = vol.uuid
                    if (!uuid.isNullOrEmpty() && path.contains(uuid)) {
                        return true
                    }
                }
            }
        }

        if (path.startsWith("/storage/") && !path.startsWith("/storage/emulated") && !path.startsWith("/storage/self")) {
            val parts = path.removePrefix("/storage/").split("/")
            if (parts.isNotEmpty() && parts[0].isNotEmpty() && parts[0] != "emulated" && parts[0] != "self") {
                return true
            }
        }
        if (path.startsWith("/mnt/media_rw/")) {
            return true
        }

        return false
    }

    fun openSystemStorageSettings(context: Context, volumePath: String = "") {
        try {
            val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_MEMORY_CARD_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }
    }

    fun formatStorageVolume(
        context: Context,
        volumePath: String,
        label: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            var success = false
            var errorMsg: String? = null

            if (ShizukuManager.isAvailable() && ShizukuManager.hasPermission()) {
                try {
                    val service = ShizukuManager.getServiceBlocking()
                    if (service != null) {
                        success = service.formatVolume(volumePath, label)
                    }
                } catch (e: Exception) {
                    errorMsg = e.localizedMessage
                }
            }

            if (!success) {
                try {
                    val dir = File(volumePath)
                    if (dir.exists() && dir.isDirectory) {
                        dir.listFiles()?.forEach { child ->
                            if (child.isDirectory) {
                                child.deleteRecursively()
                            } else {
                                child.delete()
                            }
                        }
                        success = true
                    }
                } catch (e: Exception) {
                    errorMsg = e.localizedMessage
                }
            }

            withContext(Dispatchers.Main) {
                GlobalEvents.triggerRefresh()
                onComplete(success, errorMsg)
            }
        }
    }
}
