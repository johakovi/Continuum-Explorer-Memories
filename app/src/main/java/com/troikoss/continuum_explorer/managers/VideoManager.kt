package com.troikoss.continuum_explorer.managers

import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import com.troikoss.continuum_explorer.model.UniversalFile
import com.troikoss.continuum_explorer.model.StorageProvider
import com.troikoss.continuum_explorer.providers.LocalProvider
import com.troikoss.continuum_explorer.providers.SafProvider
import com.troikoss.continuum_explorer.providers.StorageProviders
import com.troikoss.continuum_explorer.utils.AppConfigurations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

object VideoManager {
    suspend fun getVideoFiles(context: Context, allowedFolders: Set<String> = emptySet(), forceRefresh: Boolean = false, useCacheOnly: Boolean = false, onUpdate: ((List<UniversalFile>) -> Unit)? = null): List<UniversalFile> = withContext(Dispatchers.IO) {
        val dataMap = java.util.Collections.synchronizedMap(mutableMapOf<String, List<UniversalFile>>())

        fun emit() {
            onUpdate?.invoke(dataMap.values.flatten().sortedByDescending { it.lastModified })
        }

        coroutineScope {
            if (!forceRefresh) {
                allowedFolders.forEach { folderKey ->
                    if (folderKey.startsWith("network:") || folderKey.startsWith("content://")) {
                        launch {
                            val cached = GalleryCacheManager.loadCache(context, "video_files:$folderKey")
                            if (cached != null) {
                                dataMap["cache:$folderKey"] = cached
                                emit()
                            }
                        }
                    }
                }
            }

            launch {
                dataMap["local"] = getLocalVideoFiles(context, allowedFolders)
                emit()
            }

            if (useCacheOnly) return@coroutineScope

            allowedFolders.forEach { folderKey ->
                if (folderKey.startsWith("network:") || folderKey.startsWith("content://")) {
                    launch {
                        val folderFiles = mutableListOf<UniversalFile>()
                        val (provider, path) = resolveFolderKey(context, folderKey)
                        if (provider !is LocalProvider) {
                            scanRemoteVideoFolder(provider, path, folderFiles)
                        }
                        if (folderFiles.isNotEmpty()) {
                            GalleryCacheManager.saveCache(context, "video_files:$folderKey", folderFiles)
                            dataMap.remove("cache:$folderKey")
                            dataMap["fresh:$folderKey"] = folderFiles
                            emit()
                        }
                    }
                }
            }
        }

        dataMap.values.flatten().sortedByDescending { it.lastModified }
    }

    private fun getLocalVideoFiles(context: Context, allowedFolders: Set<String>): List<UniversalFile> {
        val files = mutableListOf<UniversalFile>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE
        )

        var selection = "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? AND " +
                "${MediaStore.Files.FileColumns.DATA} NOT LIKE ? AND " +
                "${MediaStore.Files.FileColumns.DATA} NOT LIKE ?"

        val baseArgs = mutableListOf("video/%", "%/Android/%", "%/.%")

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

    private suspend fun scanRemoteVideoFolder(
        provider: StorageProvider,
        parentId: String,
        resultList: MutableList<UniversalFile>
    ) {
        val children = try { provider.listChildren(parentId) } catch (_: Exception) { emptyList() }
        children.forEach { child ->
            if (child.isDirectory) {
                scanRemoteVideoFolder(provider, child.providerId, resultList)
            } else if (isVideoFile(child.name)) {
                resultList.add(child)
            }
        }
    }

    suspend fun getVideoAlbums(context: Context, allowedFolders: Set<String> = emptySet(), forceRefresh: Boolean = false, useCacheOnly: Boolean = false, onUpdate: ((List<UniversalFile>) -> Unit)? = null): List<UniversalFile> = withContext(Dispatchers.IO) {
        val folders = if (allowedFolders.isNotEmpty()) allowedFolders.toList() else listOf(File(Environment.getExternalStorageDirectory(), "Movies").absolutePath)
        
        if (folders.size == 1) {
            val (provider, path) = resolveFolderKey(context, folders[0])
            return@withContext getVideoAlbumContents(context, path, provider, forceRefresh, useCacheOnly, onUpdate)
        }

        val roots = folders.map { key ->
            val (provider, path) = resolveFolderKey(context, key)
            UniversalFile(
                name = provider.displayName(path),
                isDirectory = true,
                lastModified = 0L,
                length = 0,
                provider = provider,
                providerId = path,
                parentId = "virtual://videos"
            )
        }
        onUpdate?.invoke(roots)
        roots
    }

    suspend fun getVideoAlbumContents(context: Context, dirPath: String, provider: StorageProvider? = null, forceRefresh: Boolean = false, useCacheOnly: Boolean = false, onUpdate: ((List<UniversalFile>) -> Unit)? = null): List<UniversalFile> = withContext(Dispatchers.IO) {
        val targetProvider = provider ?: resolveFolderKey(context, dirPath).first
        
        if (targetProvider !is LocalProvider) {
            val cached = if (!forceRefresh) GalleryCacheManager.loadCache(context, "video_dir:$dirPath") else null
            if (cached != null) {
                onUpdate?.invoke(cached)
            }
            if (useCacheOnly) return@withContext cached ?: emptyList()
            
            return@withContext try {
                val files = targetProvider.listChildren(dirPath).filter { it.isDirectory || isVideoFile(it.name) }
                if (files.isNotEmpty()) {
                    GalleryCacheManager.saveCache(context, "video_dir:$dirPath", files)
                }
                onUpdate?.invoke(files)
                files
            } catch (e: Exception) {
                cached ?: emptyList()
            }
        }

        val items = targetProvider.listChildren(dirPath).filter { it.isDirectory || isVideoFile(it.name) }
        onUpdate?.invoke(items)
        items
    }

    private fun resolveFolderKey(context: Context, folderKey: String): Pair<StorageProvider, String> {
        return when {
            folderKey.startsWith("network:") -> {
                val parts = folderKey.split(":", limit = 4)
                if (parts.size == 4) {
                    val connId = parts[2]
                    val path = parts[3]
                    val appConfigs = AppConfigurations(context)
                    val conn = appConfigs.networkConnections.find { it.id == connId }
                    if (conn != null) return StorageProviders.network(conn) to path
                }
                LocalProvider to folderKey
            }
            folderKey.startsWith("content://") -> SafProvider to folderKey
            else -> LocalProvider to folderKey
        }
    }

    private fun isVideoFile(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in listOf("mp4", "mkv", "mov", "avi", "3gp", "webm", "flv", "wmv", "mpg", "mpeg")
    }
}
