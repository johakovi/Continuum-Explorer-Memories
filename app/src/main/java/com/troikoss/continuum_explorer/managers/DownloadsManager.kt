package com.troikoss.continuum_explorer.managers

import android.content.Context
import android.os.Environment
import com.troikoss.continuum_explorer.model.UniversalFile
import com.troikoss.continuum_explorer.utils.toUniversal
import java.io.File

object DownloadsManager {
    fun getDownloadsFiles(context: Context): List<UniversalFile> {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val files = downloadsDir.listFiles()?.toList() ?: emptyList()
        return files.map { it.toUniversal() }
    }
}
