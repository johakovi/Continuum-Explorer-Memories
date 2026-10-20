package com.troikoss.continuum_explorer.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.troikoss.continuum_explorer.R
import com.troikoss.continuum_explorer.model.ProviderKind
import com.troikoss.continuum_explorer.model.UniversalFile
import com.troikoss.continuum_explorer.providers.LocalProvider
import com.troikoss.continuum_explorer.providers.SafProvider
import java.io.File

fun File.toUniversal(): UniversalFile {
    val extension = this.extension.lowercase()
    val mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    return UniversalFile(
        name = this.name,
        isDirectory = this.isDirectory,
        lastModified = this.lastModified(),
        length = this.length(),
        provider = LocalProvider,
        providerId = this.absolutePath,
        parentId = this.parentFile?.absolutePath,
        mimeType = mimeType
    )
}

fun DocumentFile.toUniversal(): UniversalFile {
    return UniversalFile(
        name = this.name ?: "Unknown",
        isDirectory = this.isDirectory,
        lastModified = this.lastModified(),
        length = this.length(),
        provider = SafProvider,
        providerId = this.uri.toString(),
        parentId = this.parentFile?.uri?.toString(),
        mimeType = this.type,
    )
}

/**
 * Provider-aware extension shortcuts.
 */
val UniversalFile.kind: ProviderKind get() = provider.kind

fun UniversalFile.openInput() = provider.openInput(providerId)
fun UniversalFile.openReadFd() = provider.openReadFd(providerId)

suspend fun UniversalFile.children() = provider.listChildren(providerId)

fun UniversalFile.delete() = provider.delete(providerId)

fun UniversalFile.rename(newName: String) = provider.rename(providerId, newName)

/**
 * Generates a shareable URI for a file.
 * Uses FileProvider for local files and direct URIs for SAF documents.
 */
fun getUriForUniversalFile(context: Context, file: UniversalFile): Uri? {
    return try {
        when {
            file.isArchiveEntry -> Uri.parse("archive://${file.absolutePath}")
            file.provider is LocalProvider -> {
                val f = File(file.providerId)
                try {
                    FileProvider.getUriForFile(context, context.packageName + ".provider", f)
                } catch (_: Exception) {
                    Uri.fromFile(f)
                }
            }
            file.providerId.startsWith("content://") || file.providerId.startsWith("file://") -> {
                Uri.parse(file.providerId)
            }
            else -> {
                val p = file.providerId.ifEmpty { file.name }
                if (p.startsWith("/")) Uri.fromFile(File(p))
                else Uri.parse("file://$p")
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Uri.fromFile(File(file.providerId.ifEmpty { file.name }))
    }
}

/**
 * Gets a human-readable file type string.
 */
fun getFileType(file: UniversalFile, context: Context): String {
    if (file.isDirectory) return context.getString(R.string.folder)

    val extension = file.name.substringAfterLast(".", "").lowercase()
    val extensionString = extension.uppercase()

    return when (extension) {
        "zip", "rar", "7z", "tar", "gz" -> "$extensionString ${context.getString(R.string.archive)}"
        "jpg", "jpeg", "bmp", "png", "gif", "webp" -> "$extensionString ${context.getString(R.string.image)}"
        "mp4", "mkv", "avi", "mov", "webm" -> "$extensionString ${context.getString(R.string.video)}"
        "mp3", "wav", "ogg", "m4a", "flac" -> "$extensionString ${context.getString(R.string.audio)}"
        "txt", "doc", "docx", "odt", "pdf" -> "$extensionString ${context.getString(R.string.document)}"
        "" -> context.getString(R.string.file)
        else -> "$extensionString ${context.getString(R.string.file)}"
    }
}
