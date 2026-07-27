package com.troikoss.continuum_explorer.managers

import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import com.troikoss.continuum_explorer.model.UniversalFile
import com.troikoss.continuum_explorer.providers.LocalProvider
import java.io.File

object GalleryManager {
    // Returns only the media files directly in dirPath (no subdirectories).
    fun getAlbumContents(context: Context, dirPath: String): List<UniversalFile> {
        val items = mutableListOf<UniversalFile>()

        val projection = arrayOf(
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.SIZE
        )

        val normalizedDir = dirPath.trimEnd('/')

        // Match files directly in dirPath: path like 'dir/%' but NOT 'dir/%/%'
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

    fun getGalleryFiles(context: Context, allowedFolders: Set<String> = emptySet()): List<UniversalFile> {
        val files = mutableListOf<UniversalFile>()

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

        val baseArgs = mutableListOf(
            "image/%",
            "video/%",
            "%/Android/%",
            "%/.%"
        )

        if (allowedFolders.isNotEmpty()) {
            val folderPlaceholders = allowedFolders.joinToString(" OR ") { "${MediaStore.Files.FileColumns.DATA} LIKE ?" }
            selection += " AND ($folderPlaceholders)"
            allowedFolders.forEach { baseArgs.add("$it%") }
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

    fun getGalleryAlbums(context: Context, allowedFolders: Set<String> = emptySet()): List<UniversalFile> {
        // Flat list of all folders containing media or subfolders, keyed by path to avoid duplicates.
        val albums = linkedMapOf<String, File>()

        fun scanForAlbums(folder: File) {
            if (!folder.exists() || !folder.isDirectory || folder.name.startsWith(".")) return
            
            // Add this folder as an album
            albums[folder.absolutePath] = folder
            
            // Recursively scan subdirectories
            folder.listFiles()?.forEach { sub ->
                if (sub.isDirectory) {
                    scanForAlbums(sub)
                }
            }
        }

        // 1. Scan allowed folders or default roots
        if (allowedFolders.isNotEmpty()) {
            allowedFolders.forEach { path ->
                scanForAlbums(File(path))
            }
        } else {
            val roots = listOf(
                File(Environment.getExternalStorageDirectory(), "DCIM"),
                File(Environment.getExternalStorageDirectory(), "Pictures")
            )
            roots.forEach { root ->
                scanForAlbums(root)
            }
        }

        // 2. Also query MediaStore to catch albums outside our common roots
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        var selection = "(${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ?) AND " +
                "${MediaStore.Files.FileColumns.DATA} NOT LIKE ? AND " +
                "${MediaStore.Files.FileColumns.DATA} NOT LIKE ?"

        val baseArgs = mutableListOf("image/%", "video/%", "%/Android/%", "%/.%")

        if (allowedFolders.isNotEmpty()) {
            val folderPlaceholders = allowedFolders.joinToString(" OR ") { "${MediaStore.Files.FileColumns.DATA} LIKE ?" }
            selection += " AND ($folderPlaceholders)"
            allowedFolders.forEach { baseArgs.add("$it%") }
        }

        try {
            val queryUri = MediaStore.Files.getContentUri("external")
            val queryArgs = Bundle().apply {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, baseArgs.toTypedArray())
            }

            context.contentResolver.query(queryUri, projection, queryArgs, null)?.use { c ->
                val dataIndex = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

                while (c.moveToNext()) {
                    val path = c.getString(dataIndex) ?: continue
                    val parentDir = File(path).parentFile
                    if (parentDir != null && parentDir.absolutePath !in albums) {
                        albums[parentDir.absolutePath] = parentDir
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return albums.entries.sortedBy { it.value.name.lowercase() }.map { (path, dir) ->
            UniversalFile(
                name = dir.name,
                isDirectory = true,
                lastModified = dir.lastModified(),
                length = 0,
                provider = LocalProvider,
                providerId = path,
                parentId = "virtual://gallery",
            )
        }
    }
}
