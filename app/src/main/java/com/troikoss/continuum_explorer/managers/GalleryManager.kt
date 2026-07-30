package com.troikoss.continuum_explorer.managers

import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import com.troikoss.continuum_explorer.model.UniversalFile
import com.troikoss.continuum_explorer.model.ProviderKind
import com.troikoss.continuum_explorer.model.StorageProvider
import com.troikoss.continuum_explorer.providers.LocalProvider
import com.troikoss.continuum_explorer.providers.SafProvider
import com.troikoss.continuum_explorer.providers.StorageProviders
import com.troikoss.continuum_explorer.providers.ShizukuProvider
import com.troikoss.continuum_explorer.utils.AppConfigurations
import java.io.File

object GalleryManager {
    // Returns only the media files directly in dirPath (no subdirectories).
    suspend fun getAlbumContents(context: Context, dirPath: String, provider: StorageProvider? = null): List<UniversalFile> {
        if (provider != null && provider !is LocalProvider) {
            return try {
                provider.listChildren(dirPath).filter { !it.isDirectory && isImageOrVideoFile(it.name) }
            } catch (e: Exception) {
                emptyList()
            }
        }

        val items = mutableListOf<UniversalFile>()

        val projection = arrayOf(
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.SIZE
        )

        val normalizedDir = dirPath.trimEnd('/')

        val selection = "(${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ?) AND " +
                "${MediaStore.Files.FileColumns.DATA} LIKE ? AND " +
                "${MediaStore.Files.FileColumns.DATA} NOT LIKE ? AND " +
                "${MediaStore.Files.FileColumns.DATA} NOT LIKE ?"

        val selectionArgs = arrayOf("image/%", "video/%", "$normalizedDir/%", "$normalizedDir/%/%", "%/.%")

        try {
            val queryUri = MediaStore.Files.getContentUri("external")
            val queryArgs = Bundle().apply {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC")
            }

            context.contentResolver.query(queryUri, projection, queryArgs, null)?.use { c ->
                val dataIndex = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val nameIndex = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dateIndex = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val sizeIndex = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)

                while (c.moveToNext()) {
                    val path = c.getString(dataIndex) ?: continue
                    items.add(
                        UniversalFile(
                            name = c.getString(nameIndex) ?: File(path).name,
                            isDirectory = false,
                            lastModified = c.getLong(dateIndex) * 1000,
                            length = c.getLong(sizeIndex),
                            provider = LocalProvider,
                            providerId = path,
                            parentId = File(path).parentFile?.absolutePath,
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return items
    }

    private fun getProvider(context: Context, kind: String?, connectionId: String?): StorageProvider {
        if (kind == null || kind == ProviderKind.LOCAL.name) return LocalProvider
        if (kind == ProviderKind.SAF.name) return SafProvider
        if (kind == ProviderKind.SHIZUKU.name) return ShizukuProvider
        
        if (connectionId != null) {
            val appConfigs = AppConfigurations(context)
            val conn = appConfigs.networkConnections.find { it.id == connectionId }
            if (conn != null) {
                return StorageProviders.network(conn)
            }
        }
        return LocalProvider
    }

    suspend fun getGalleryFiles(context: Context, allowedFolders: Set<String> = emptySet()): List<UniversalFile> {
        val files = mutableListOf<UniversalFile>()
        val appConfigs = AppConfigurations(context)

        // 1. Scan remote/allowed folders
        allowedFolders.forEach { folderKey ->
            when {
                folderKey.startsWith("network:") -> {
                    val parts = folderKey.split(":", limit = 4)
                    if (parts.size == 4) {
                        val kindName = parts[1]
                        val connId = parts[2]
                        val path = parts[3]
                        val conn = appConfigs.networkConnections.find { it.id == connId }
                        if (conn != null) {
                            val provider = StorageProviders.network(conn)
                            scanRemoteGalleryFolder(provider, path, kindName, connId, files)
                        }
                    }
                }
                folderKey.startsWith("content://") -> {
                    scanRemoteGalleryFolder(SafProvider, folderKey, ProviderKind.SAF.name, null, files)
                }
                else -> {
                    // Local folders are handled by MediaStore below with allowedFolders filter
                }
            }
        }

        // 2. Scan local files via MediaStore
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE
        )

        var selection = "(${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ?) AND " +
                "${MediaStore.Files.FileColumns.DATA} NOT LIKE ? AND " +
                "${MediaStore.Files.FileColumns.DATA} NOT LIKE ?"

        val baseArgs = mutableListOf("image/%", "video/%", "%/Android/%", "%/.%")

        val localAllowedFolders = allowedFolders.filter { !it.contains("://") && !it.startsWith("network:") }

        if (localAllowedFolders.isNotEmpty()) {
            val folderPlaceholders = localAllowedFolders.joinToString(" OR ") { "${MediaStore.Files.FileColumns.DATA} LIKE ?" }
            selection += " AND ($folderPlaceholders)"
            localAllowedFolders.forEach { baseArgs.add("$it%") }
        }

        try {
            val queryUri = MediaStore.Files.getContentUri("external")
            val queryArgs = Bundle().apply {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, baseArgs.toTypedArray())
                putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC")
            }

            context.contentResolver.query(queryUri, projection, queryArgs, null)?.use { c ->
                val dataIndex = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val nameIndex = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dateIndex = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val sizeIndex = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)

                while (c.moveToNext()) {
                    val path = c.getString(dataIndex) ?: continue
                    val file = File(path)
                    if (file.exists() && !file.isDirectory) {
                        files.add(
                            UniversalFile(
                                name = c.getString(nameIndex) ?: file.name,
                                isDirectory = false,
                                lastModified = c.getLong(dateIndex) * 1000,
                                length = c.getLong(sizeIndex),
                                provider = LocalProvider,
                                providerId = path,
                                parentId = file.parentFile?.absolutePath,
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return files
    }

    private suspend fun scanRemoteGalleryFolder(
        provider: StorageProvider,
        parentId: String,
        kindName: String,
        connectionId: String?,
        resultList: MutableList<UniversalFile>
    ) {
        val children = try { provider.listChildren(parentId) } catch (e: Exception) { emptyList() }
        children.forEach { child ->
            if (child.isDirectory) {
                scanRemoteGalleryFolder(provider, child.providerId, kindName, connectionId, resultList)
            } else if (isImageOrVideoFile(child.name)) {
                resultList.add(child)
            }
        }
    }

    suspend fun getGalleryAlbums(context: Context, allowedFolders: Set<String> = emptySet()): List<UniversalFile> {
        val albums = mutableListOf<UniversalFile>()
        val appConfigs = AppConfigurations(context)

        // 1. Scan remote folders
        allowedFolders.forEach { folderKey ->
            if (folderKey.startsWith("network:") || folderKey.startsWith("content://")) {
                val provider: StorageProvider?
                val path: String
                val kind: String
                val connId: String?

                if (folderKey.startsWith("network:")) {
                    val parts = folderKey.split(":", limit = 4)
                    kind = parts[1]
                    connId = parts[2]
                    path = parts[3]
                    val conn = appConfigs.networkConnections.find { it.id == connId }
                    provider = conn?.let { StorageProviders.network(it) }
                } else {
                    provider = SafProvider
                    path = folderKey
                    kind = ProviderKind.SAF.name
                    connId = null
                }

                if (provider != null) {
                    scanForRemoteAlbums(provider, path, kind, connId, albums)
                }
            }
        }

        // 2. Scan local folders
        val localAlbums = linkedMapOf<String, File>()
        fun scanForLocalAlbums(folder: File) {
            if (!folder.exists() || !folder.isDirectory || folder.name.startsWith(".")) return
            localAlbums[folder.absolutePath] = folder
            folder.listFiles()?.forEach { if (it.isDirectory) scanForLocalAlbums(it) }
        }

        val localAllowedFolders = allowedFolders.filter { !it.contains("://") && !it.startsWith("network:") }
        if (localAllowedFolders.isNotEmpty()) {
            localAllowedFolders.forEach { scanForLocalAlbums(File(it)) }
        } else {
            listOf("DCIM", "Pictures").forEach { scanForLocalAlbums(File(Environment.getExternalStorageDirectory(), it)) }
        }

        // Query MediaStore for local albums
        val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
        var selection = "(${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ?) AND " +
                "${MediaStore.Files.FileColumns.DATA} NOT LIKE ? AND " +
                "${MediaStore.Files.FileColumns.DATA} NOT LIKE ?"
        val baseArgs = mutableListOf("image/%", "video/%", "%/Android/%", "%/.%")
        if (localAllowedFolders.isNotEmpty()) {
            val folderPlaceholders = localAllowedFolders.joinToString(" OR ") { "${MediaStore.Files.FileColumns.DATA} LIKE ?" }
            selection += " AND ($folderPlaceholders)"
            localAllowedFolders.forEach { baseArgs.add("$it%") }
        }

        try {
            context.contentResolver.query(MediaStore.Files.getContentUri("external"), projection, selection, baseArgs.toTypedArray(), null)?.use { c ->
                val dataIndex = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                while (c.moveToNext()) {
                    val path = c.getString(dataIndex) ?: continue
                    val parentDir = File(path).parentFile
                    if (parentDir != null && parentDir.absolutePath !in localAlbums) {
                        localAlbums[parentDir.absolutePath] = parentDir
                    }
                }
            }
        } catch (_: Exception) {}

        localAlbums.values.sortedBy { it.name.lowercase() }.forEach { dir ->
            albums.add(UniversalFile(
                name = dir.name, isDirectory = true, lastModified = dir.lastModified(),
                length = 0, provider = LocalProvider, providerId = dir.absolutePath,
                parentId = "virtual://gallery"
            ))
        }

        return albums
    }

    private suspend fun scanForRemoteAlbums(
        provider: StorageProvider,
        parentId: String,
        kind: String,
        connId: String?,
        resultList: MutableList<UniversalFile>
    ) {
        val children = try { provider.listChildren(parentId) } catch (e: Exception) { return }
        val hasMedia = children.any { !it.isDirectory && isImageOrVideoFile(it.name) }
        if (hasMedia) {
            resultList.add(UniversalFile(
                name = provider.displayName(parentId),
                isDirectory = true, lastModified = 0, length = 0,
                provider = provider, providerId = parentId,
                parentId = "virtual://gallery"
            ))
        }
        children.filter { it.isDirectory }.forEach { scanForRemoteAlbums(provider, it.providerId, kind, connId, resultList) }
    }

    private fun isImageOrVideoFile(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "mp4", "mkv", "mov", "avi", "3gp")
    }
}
