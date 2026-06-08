package com.troikoss.continuum_explorer.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import java.io.File

object SafUtils {
    fun getRawPathFromUri(context: Context, uri: Uri): String? {
        if (DocumentsContract.isTreeUri(uri)) {
            val treeId = DocumentsContract.getTreeDocumentId(uri)
            if (treeId != null && treeId.startsWith("primary:")) {
                val relativePath = treeId.removePrefix("primary:")
                return File(Environment.getExternalStorageDirectory(), relativePath).absolutePath
            }
        } else {
            val docId = try { DocumentsContract.getDocumentId(uri) } catch (_: Exception) { null }
            if (docId != null && docId.startsWith("primary:")) {
                val relativePath = docId.removePrefix("primary:")
                return File(Environment.getExternalStorageDirectory(), relativePath).absolutePath
            }
        }
        return null
    }
}
