package com.troikoss.continuum_explorer.managers

import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import com.troikoss.continuum_explorer.model.UniversalFile
import com.troikoss.continuum_explorer.providers.LocalProvider
import java.io.File

object MusicManager {
    fun getMusicFiles(context: Context, allowedFolders: Set<String> = emptySet()): List<UniversalFile> {
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

        val baseArgs = mutableListOf(
            "audio/%",
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
                    if (!file.isDirectory) {
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

    fun getMusicAlbums(context: Context, allowedFolders: Set<String> = emptySet()): List<UniversalFile> {
        val albums = linkedMapOf<String, File>()

        val projection = arrayOf(
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM
        )

        var selection = "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? AND " +
                "${MediaStore.Files.FileColumns.DATA} NOT LIKE ? AND " +
                "${MediaStore.Files.FileColumns.DATA} NOT LIKE ?"

        val baseArgs = mutableListOf("audio/%", "%/Android/%", "%/.%")

        if (allowedFolders.isNotEmpty()) {
            val folderPlaceholders = allowedFolders.joinToString(" OR ") { "${MediaStore.Audio.Media.DATA} LIKE ?" }
            selection += " AND ($folderPlaceholders)"
            allowedFolders.forEach { baseArgs.add("$it%") }
        }

        try {
            val queryUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val queryArgs = Bundle().apply {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, baseArgs.toTypedArray())
            }

            context.contentResolver.query(queryUri, projection, queryArgs, null)?.use { c ->
                val dataIndex = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumIndex = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)

                while (c.moveToNext()) {
                    val path = c.getString(dataIndex) ?: continue
                    var albumName = c.getString(albumIndex)
                    if (albumName.isNullOrBlank()) {
                        albumName = File(path).parentFile?.name ?: "Unknown Album"
                    }
                    if (albumName !in albums) {
                        val parentDir = File(path).parentFile
                        if (parentDir != null) {
                            albums[albumName] = parentDir
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return albums.entries.sortedBy { it.key.lowercase() }.map { (name, dir) ->
            UniversalFile(
                name = name,
                isDirectory = true,
                lastModified = dir.lastModified(),
                length = 0,
                provider = LocalProvider,
                providerId = dir.absolutePath,
                parentId = dir.parentFile?.absolutePath,
            )
        }
    }

    fun getMusicAlbumContents(context: Context, dirPath: String): List<UniversalFile> {
        val mediaFiles = mutableListOf<UniversalFile>()

        val projection = arrayOf(
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.SIZE
        )

        val normalizedDir = dirPath.trimEnd('/')

        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? AND " +
                "${MediaStore.Files.FileColumns.DATA} LIKE ? AND " +
                "${MediaStore.Files.FileColumns.DATA} NOT LIKE ? AND " +
                "${MediaStore.Files.FileColumns.DATA} NOT LIKE ?"

        val selectionArgs = arrayOf("audio/%", "$normalizedDir/%", "$normalizedDir/%/%", "%/.%")

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
                    mediaFiles.add(
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

        return mediaFiles
    }
}
