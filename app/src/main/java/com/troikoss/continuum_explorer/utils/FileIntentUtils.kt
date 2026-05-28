package com.troikoss.continuum_explorer.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.troikoss.continuum_explorer.R
import com.troikoss.continuum_explorer.managers.FileOperationsManager
import com.troikoss.continuum_explorer.managers.OperationType
import com.troikoss.continuum_explorer.managers.SettingsManager
import com.troikoss.continuum_explorer.model.UniversalFile
import com.troikoss.continuum_explorer.providers.LocalProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shares one or more files via the system share sheet.
 */
fun shareFiles(context: Context, scope: CoroutineScope, files: List<UniversalFile>) {
    if (files.isEmpty()) return
    if (files.any { it.provider.capabilities.isRemote }) {
        scope.launch {
            try {
                FileOperationsManager.start()
                NotificationHelper.start(context)
                withContext(Dispatchers.Main) {
                    FileOperationsManager.update(0, files.size, operationType = OperationType.COPY)
                }
                val uris = ArrayList<Uri>()
                withContext(Dispatchers.IO) {
                    files.forEach { file ->
                        val uri = if (file.provider.capabilities.isRemote) {
                            val cached = RemoteCache.cache(context, file)
                            FileProvider.getUriForFile(context, context.packageName + ".provider", cached)
                        } else {
                            getUriForUniversalFile(context, file)
                        }
                        if (uri != null) uris.add(uri)
                    }
                }
                withContext(Dispatchers.Main) {
                    FileOperationsManager.finish()
                    if (uris.isEmpty()) {
                        Toast.makeText(context, context.getString(R.string.msg_share_failed_prepare), Toast.LENGTH_SHORT).show()
                    } else {
                        launchShareIntent(context, uris)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    FileOperationsManager.finish()
                    Toast.makeText(context, context.getString(R.string.msg_share_failed_prepare), Toast.LENGTH_SHORT).show()
                }
            }
        }
        return
    }
    val uris = ArrayList<Uri>()
    files.forEach { file ->
        val uri = getUriForUniversalFile(context, file)
        if (uri != null) uris.add(uri)
    }
    if (uris.isEmpty()) {
        Toast.makeText(context, context.getString(R.string.msg_share_failed_prepare), Toast.LENGTH_SHORT).show()
        return
    }
    launchShareIntent(context, uris)
}

private fun launchShareIntent(context: Context, uris: ArrayList<Uri>) {
    val shareIntent = Intent().apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (uris.size == 1) {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uris[0])
            type = context.contentResolver.getType(uris[0]) ?: "*/*"
        } else {
            action = Intent.ACTION_SEND_MULTIPLE
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            type = "*/*"
        }
    }
    try {
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.menu_share)))
    } catch (_: Exception) {
        Toast.makeText(context, context.getString(R.string.msg_no_app_share), Toast.LENGTH_SHORT).show()
    }
}

/**
 * Opens a file using the system "Open with" chooser.
 */
fun openWith(context: Context, scope: CoroutineScope, file: UniversalFile) {
    if (file.provider.capabilities.isRemote) {
        scope.launch {
            try {
                FileOperationsManager.start()
                NotificationHelper.start(context)
                withContext(Dispatchers.Main) {
                    FileOperationsManager.update(0, 1, operationType = OperationType.COPY)
                    FileOperationsManager.currentFileName.value = file.name
                }
                val cached = RemoteCache.cache(context, file)
                withContext(Dispatchers.Main) {
                    FileOperationsManager.finish()
                    val cachedUri = FileProvider.getUriForFile(context, context.packageName + ".provider", cached)
                    launchOpenWithIntent(context, cachedUri, file.name)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    FileOperationsManager.finish()
                    Toast.makeText(context, context.getString(R.string.msg_no_app_open), Toast.LENGTH_SHORT).show()
                }
            }
        }
        return
    }

    val uri = getUriForUniversalFile(context, file) ?: return
    launchOpenWithIntent(context, uri, file.name)
}

private fun launchOpenWithIntent(context: Context, uri: Uri, fileName: String? = null) {
    val extension = fileName?.substringAfterLast('.', "")?.lowercase() ?: ""
    val mimeType = if (fileName != null) {
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: context.contentResolver.getType(uri)
    } else {
        val ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        if (ext != null) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
        } else {
            context.contentResolver.getType(uri)
        }
    } ?: "*/*"

    val isOfficeDoc = when (extension) {
        "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "txt" -> true
        else -> false
    }

    val intent = Intent(if (isOfficeDoc) Intent.ACTION_EDIT else Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        val chooser = Intent.createChooser(intent, context.getString(R.string.menu_open_with_no_dots))
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (_: Exception) {
        if (isOfficeDoc) {
            intent.action = Intent.ACTION_VIEW
            try {
                val chooser = Intent.createChooser(intent, context.getString(R.string.menu_open_with_no_dots))
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
                return
            } catch (_: Exception) {}
        }
        Toast.makeText(context, context.getString(R.string.msg_no_app_open), Toast.LENGTH_SHORT).show()
    }
}

/**
 * Opens a remote file by caching it locally first, then launching the appropriate viewer.
 */
fun openRemoteFile(context: Context, scope: CoroutineScope, file: UniversalFile) {
    val mime = MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(file.name.substringAfterLast('.', "").lowercase())

    scope.launch {
        try {
            FileOperationsManager.start()
            NotificationHelper.start(context)
            withContext(Dispatchers.Main) {
                FileOperationsManager.update(0, 1, operationType = OperationType.COPY)
                FileOperationsManager.currentFileName.value = file.name
            }
            val cached = RemoteCache.cache(context, file) { copied, total ->
                if (total > 0) {
                    FileOperationsManager.updateDetailed(
                        processedBytes = copied, totalBytes = total,
                        speed = 0L, remainingMillis = 0L, fileName = file.name
                    )
                }
            }
            withContext(Dispatchers.Main) {
                FileOperationsManager.finish()
                val cachedUri = FileProvider.getUriForFile(
                    context, context.packageName + ".provider", cached
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(cachedUri, mime ?: "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(context, context.getString(R.string.msg_no_app_open), Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                FileOperationsManager.finish()
                Toast.makeText(context, e.message ?: context.getString(R.string.msg_share_failed_prepare), Toast.LENGTH_SHORT).show()
            }
        }
    }
}

/**
 * Opens a restricted file (e.g. Android/data) by caching it to .temp first.
 */
fun openRestrictedFile(context: Context, scope: CoroutineScope, file: UniversalFile) {
    scope.launch {
        try {
            FileOperationsManager.start()
            withContext(Dispatchers.Main) {
                FileOperationsManager.update(0, 1, operationType = OperationType.COPY)
                FileOperationsManager.currentFileName.value = file.name
            }
            val cached = RestrictedCache.cache(context, file) { copied, total ->
                if (total > 0) {
                    FileOperationsManager.updateDetailed(
                        processedBytes = copied, totalBytes = total,
                        speed = 0L, remainingMillis = 0L, fileName = file.name
                    )
                }
            }
            withContext(Dispatchers.Main) {
                FileOperationsManager.finish()
                // Use a modified UniversalFile so openFile knows where the actual data is,
                // but we pass the original providerId in the intent for saving back.
                val tempFile = file.copy(providerId = cached.absolutePath, provider = LocalProvider)
                openFile(context, tempFile, originalFile = file)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                FileOperationsManager.finish()
                Toast.makeText(context, e.message ?: "Failed to open restricted file", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

/**
 * Opens a file with the default system app.
 * Falls back to the "Open with" chooser if no default handles the type.
 */
fun openFile(context: Context, file: UniversalFile, originalFile: UniversalFile? = null) {
    if (file.provider.capabilities.isRemote) return // Must use openRemoteFile with a scope instead

    val uri = getUriForUniversalFile(context, file) ?: return

    val extension = file.name.substringAfterLast('.', "").lowercase()

    if (extension == "sh" && SettingsManager.termuxSupport.value) {
        if (isTermuxInstalled(context)) {
            val fileRef = file.fileRef
            if (fileRef != null) {
                openInTermux(context, fileRef.absolutePath, isScript = true)
                return
            }
        }
    }

    if (file.name.endsWith(".apk", ignoreCase = true)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                Toast.makeText(context, context.getString(R.string.msg_apk_install_permission), Toast.LENGTH_LONG).show()
                return
            }
        }
    }

    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        ?: context.contentResolver.getType(uri)
        ?: "*/*"

    // Use internal Text Editor for text files
    val textExtensions = setOf("txt", "log", "cfg", "ini", "md", "xml", "json", "sh", "py", "js", "html", "css")
    if (textExtensions.contains(extension) || mimeType.startsWith("text/")) {
        val intent = Intent(context, com.troikoss.continuum_explorer.ui.activities.TextEditorActivity::class.java).apply {
            setData(uri)
            if (originalFile != null) {
                // If it's a restricted file, we have a local temp path and an original target
                putExtra("originalPath", originalFile.providerId)
                putExtra("tempPath", file.providerId)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return
    }

    // For Office documents, ACTION_EDIT can sometimes bypass the read-only mode in apps like Word/Excel
    val isOfficeDoc = when (extension) {
        "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "txt" -> true
        else -> false
    }

    val intent = Intent(if (isOfficeDoc) Intent.ACTION_EDIT else Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback to ACTION_VIEW if ACTION_EDIT is not supported
        if (isOfficeDoc) {
            intent.action = Intent.ACTION_VIEW
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }
        launchOpenWithIntent(context, uri, file.name)
    }
}

fun isTermuxInstalled(context: Context): Boolean {
    return try {
        context.packageManager.getPackageInfo("com.termux", 0)
        true
    } catch (_: Exception) {
        false
    }
}

fun openInTermux(context: Context, path: String, isScript: Boolean = false) {
    val intent = Intent().apply {
        setClassName("com.termux", "com.termux.app.RunCommandService")
        action = "com.termux.RUN_COMMAND"
        
        val escapedPath = path.replace("'", "'\\''")
        
        if (isScript) {
            // Run script through sh to handle non-executable files on /sdcard
            putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/sh")
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf(path))
        } else {
            // Open terminal in the directory by running a command that cd's and then starts a login shell
            putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", "cd '$escapedPath' && exec bash -l"))
        }
        
        // 1 = opens a new terminal session (tab)
        putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "1")
    }
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to open Termux: ${e.message}. Ensure 'Allow external apps' is enabled in Termux settings.", Toast.LENGTH_LONG).show()
    }
}

