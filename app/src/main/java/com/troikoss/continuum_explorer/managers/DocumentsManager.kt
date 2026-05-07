package com.troikoss.continuum_explorer.managers

import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import com.troikoss.continuum_explorer.model.UniversalFile
import com.troikoss.continuum_explorer.providers.LocalProvider
import java.io.File

object DocumentsManager {
    fun getDocumentsFiles(context: Context): List<UniversalFile> {
        val files = mutableListOf<UniversalFile>()

        val projection = arrayOf(
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE
        )

        // Common document mime types
        val mimeTypes = arrayOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "application/rtf",
            "text/html",
            "application/epub+zip"
        )

        val selection = StringBuilder("(")
        for (i in mimeTypes.indices) {
            selection.append("${MediaStore.Files.FileColumns.MIME_TYPE} = ?")
            if (i < mimeTypes.size - 1) selection.append(" OR ")
        }
        selection.append(") AND ${MediaStore.Files.FileColumns.DATA} NOT LIKE ? AND ${MediaStore.Files.FileColumns.DATA} NOT LIKE ?")

        val selectionArgs = mimeTypes + arrayOf("%/Android/%", "%/.%")

        try {
            val queryUri = MediaStore.Files.getContentUri("external")

            val queryArgs = Bundle().apply {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection.toString())
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
}
