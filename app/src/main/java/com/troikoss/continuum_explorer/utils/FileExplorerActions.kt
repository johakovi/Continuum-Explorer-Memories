package com.troikoss.continuum_explorer.utils

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.troikoss.continuum_explorer.R
import com.troikoss.continuum_explorer.managers.FileOperationsManager
import com.troikoss.continuum_explorer.managers.OperationType
import com.troikoss.continuum_explorer.managers.SettingsManager
import com.troikoss.continuum_explorer.managers.UndoManager
import com.troikoss.continuum_explorer.ui.activities.NewWindowActivity
import com.troikoss.continuum_explorer.ui.activities.PopUpActivity
import com.troikoss.continuum_explorer.model.LibraryItem
import com.troikoss.continuum_explorer.model.UniversalFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

fun FileExplorerState.open(item: UniversalFile) {
    if (item.isDirectory) {
        if (item.provider.capabilities.isRemote) {
            navigateTo(null, null, networkProvider = item.provider, networkId = item.providerId, networkConnectionId = currentNetworkConnectionId)
            return
        }
        if (item.isArchiveEntry) {
            navigateTo(
                newPath = null,
                newUri = null,
                archiveFile = currentArchiveFile,
                archiveUri = currentArchiveUri,
                archivePath = item.archivePath
            )
        } else {
            val itemFileRef = item.fileRef
            val itemDocRef = item.documentFileRef
            if (itemFileRef != null) {
                when {
                    libraryItem == LibraryItem.Gallery -> navigateTo(itemFileRef, null, libraryItem = LibraryItem.Gallery)
                    item.providerId.startsWith("virtual://music") || item.mimeType == "album" -> navigateTo(itemFileRef, null, libraryItem = LibraryItem.Music)
                    else -> {
                        safStack.clear()
                        navigateTo(itemFileRef, null)
                    }
                }
            } else if (itemDocRef != null) {
                val oldUri = currentSafUri
                navigateTo(null, itemDocRef.uri)
                oldUri?.let { safStack.add(it) }
            }
        }
    } else {
        if (ZipUtils.isArchive(item) && SettingsManager.isDefaultArchiveViewerEnabled.value) {
            openInNewTab(listOf(item))
            return
        }

        if (item.isArchiveEntry) {
            Toast.makeText(context, context.getString(R.string.msg_not_supported_archive), Toast.LENGTH_SHORT).show()
            return
        }

        val extension = item.name.substringAfterLast('.', "").lowercase()
        val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
        val audioExtensions = setOf("mp3", "wav", "ogg", "m4a", "aac", "flac")

        val isImage = imageExtensions.contains(extension) || item.mimeType?.startsWith("image/") == true
        val isAudio = audioExtensions.contains(extension) || item.mimeType?.startsWith("audio/") == true

        val siblings = if (isImage || isAudio) {
            files.filter { sibling ->
                !sibling.isDirectory && if (isImage) {
                    imageExtensions.contains(sibling.name.substringAfterLast('.', "").lowercase()) || sibling.mimeType?.startsWith("image/") == true
                } else {
                    audioExtensions.contains(sibling.name.substringAfterLast('.', "").lowercase()) || sibling.mimeType?.startsWith("audio/") == true
                }
            }
        } else emptyList()

        when {
            item.provider.capabilities.isRemote -> openRemoteFile(context, scope, item, siblings)
            RestrictedCache.isRestricted(item) -> openRestrictedFile(context, scope, item, siblings)
            else -> openFile(context, item, siblings = siblings)
        }
    }
}

fun FileExplorerState.openInNewTab(items: List<UniversalFile>) {
    items.filter { it.isDirectory || (ZipUtils.isArchive(it) && SettingsManager.isDefaultArchiveViewerEnabled.value) }.forEach { item ->
        onOpenInNewTab?.invoke(item)
    }
}

fun FileExplorerState.openInNewWindow(items: List<UniversalFile>) {
    if (items.isEmpty()) {
        val intent = Intent(context, NewWindowActivity::class.java).apply {
            when {
                libraryItem == LibraryItem.Recent -> putExtra("isRecent", true)
                libraryItem == LibraryItem.Gallery -> putExtra("isGallery", true)
                libraryItem == LibraryItem.RecycleBin -> putExtra("isRecycleBin", true)
                currentPath != null -> putExtra("path", currentPath?.absolutePath)
                currentSafUri != null -> putExtra("uri", currentSafUri.toString())
            }
        }
        context.startActivity(intent)
    } else {
        items.filter { it.isDirectory || (ZipUtils.isArchive(it) && it.fileRef != null && SettingsManager.isDefaultArchiveViewerEnabled.value) }.forEach { item ->
            val itemFileRef = item.fileRef
            val itemDocRef = item.documentFileRef
            val intent = Intent(context, NewWindowActivity::class.java).apply {
                if (ZipUtils.isArchive(item) && SettingsManager.isDefaultArchiveViewerEnabled.value) {
                    if (itemFileRef != null) {
                        putExtra("archivePath", itemFileRef.absolutePath)
                    } else if (itemDocRef != null) {
                        putExtra("archiveUri", itemDocRef.uri)
                        putExtra("archiveName", item.name)
                    }
                } else if (itemFileRef != null) {
                    putExtra("path", itemFileRef.absolutePath)
                } else if (itemDocRef != null) {
                    putExtra("uri", itemDocRef.uri.toString())
                }
            }
            context.startActivity(intent)
        }
    }
}

fun FileExplorerState.copySelection() {
    copyToClipboard(context, selectionManager.selectedItems.toList())
}

fun FileExplorerState.cutSelection() {
    cutToClipboard(context, selectionManager.selectedItems.toList())
}

fun FileExplorerState.paste() {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = clipboard.primaryClip
    val isMove = PendingCut.isActive
    val targetPath = currentPath
    val targetSafUri = currentSafUri
    val targetProvider = currentNetworkProvider
    val targetNetworkId = currentNetworkId

    val intent = Intent(context, PopUpActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)

    FileOperationsManager.enqueue(OperationType.COPY, context.getString(R.string.op_copying)) {
        val pastedNames = pasteFromClipboard(context, targetPath, targetSafUri, targetProvider, targetNetworkId, clipData)
        refresh()?.join()
        if (pastedNames.isNotEmpty()) {
            val pastedFiles = files.filter { pastedNames.contains(it.name) }
            if (pastedFiles.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    selectionManager.clear()
                    pastedFiles.forEach { selectionManager.select(it) }
                }
            }
        }
        if (isMove || pastedNames.isNotEmpty()) GlobalEvents.triggerRefresh()
    }
}

fun FileExplorerState.deleteSelection(forcePermanent: Boolean = false) {
    val selectedItems = selectionManager.selectedItems.toList()
    if (selectedItems.isEmpty()) return

    val intent = Intent(context, PopUpActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)

    FileOperationsManager.enqueue(OperationType.DELETE, context.getString(R.string.op_deleting)) {
        deleteFiles(context, selectedItems, forcePermanent)
        refresh()?.join()
        withContext(Dispatchers.Main) { selectionManager.clear() }
        GlobalEvents.triggerRefresh()
    }
}

fun FileExplorerState.restoreSelection() {
    val selectedItems = selectionManager.selectedItems.toList()
    if (selectedItems.isEmpty()) return

    val intent = Intent(context, PopUpActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)

    FileOperationsManager.enqueue(OperationType.RESTORE, context.getString(R.string.op_restoring)) {
        restoreFiles(context, selectedItems)
        refresh()?.join()
        withContext(Dispatchers.Main) { selectionManager.clear() }
        GlobalEvents.triggerRefresh()
    }
}

fun FileExplorerState.undo() {
    val intent = Intent(context, PopUpActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)

    FileOperationsManager.enqueue(OperationType.NONE, context.getString(R.string.menu_undo)) {
        UndoManager.undo(context)
        refresh()?.join()
        GlobalEvents.triggerRefresh()
    }
}

fun FileExplorerState.redo() {
    val intent = Intent(context, PopUpActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)

    FileOperationsManager.enqueue(OperationType.NONE, context.getString(R.string.menu_redo)) {
        UndoManager.redo(context)
        refresh()?.join()
        GlobalEvents.triggerRefresh()
    }
}

fun FileExplorerState.extractSelection() {
    if (currentNetworkProvider != null || selectionManager.selectedItems.any { it.provider.capabilities.isRemote }) {
        Toast.makeText(context, context.getString(R.string.msg_archive_remote_unsupported), Toast.LENGTH_SHORT).show()
        return
    }
    val selectedArchives = selectionManager.selectedItems.filter { ZipUtils.isArchive(it) && it.fileRef != null }
    if (selectedArchives.isEmpty()) return

    val intent = Intent(context, PopUpActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)

    FileOperationsManager.enqueue(OperationType.EXTRACT, context.getString(R.string.op_extracting)) {
        val displayTitle = if (selectedArchives.size == 1) selectedArchives[0].name else context.getString(R.string.delete_items_count, selectedArchives.size)
        val settings = FileOperationsManager.requestExtractOptions(displayTitle)
        if (settings.isCancelled) return@enqueue

        val parentFile = selectedArchives[0].fileRef?.parentFile ?: return@enqueue
        ZipUtils.extractArchives(context, selectedArchives, parentFile, settings.toSeparateFolder)
        if (settings.deleteSource) {
            deleteFiles(context, selectedArchives, forcePermanent = true, silent = true)
        }
        refresh()?.join()
        GlobalEvents.triggerRefresh()
    }
}

fun FileExplorerState.compressSelection() {
    val selected = selectionManager.selectedItems.toList()
    if (selected.isEmpty()) return
    if (currentNetworkProvider != null || selected.any { it.provider.capabilities.isRemote }) {
        Toast.makeText(context, context.getString(R.string.msg_archive_remote_unsupported), Toast.LENGTH_SHORT).show()
        return
    }
    val targetFolder = currentPath ?: return
    val folderName = targetFolder.name.ifEmpty { context.getString(R.string.archive) }
    val defaultName = if (selected.size == 1) "${selected[0].name}.zip" else "$folderName.zip"

    val intent = Intent(context, PopUpActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)

    FileOperationsManager.enqueue(OperationType.COMPRESS, context.getString(R.string.op_compressing)) {
        val settings = FileOperationsManager.requestArchiveOptions(defaultName)
        if (settings.isCancelled) return@enqueue

        ZipUtils.compressFiles(context, selected, targetFolder, settings)
        if (settings.deleteSource) {
            deleteFiles(context, selected, forcePermanent = true, silent = true)
        }
        refresh()?.join()
        GlobalEvents.triggerRefresh()
    }
}

fun FileExplorerState.renameSelection() {
    val selected = selectionManager.selectedItems.toList()
    if (selected.size == 1) {
        val target = selected[0]
        FileOperationsManager.openRename(target, context) { newName ->
            confirmRename(target, newName)
        }
        val intent = Intent(context, PopUpActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } else if (selected.isEmpty()) {
        Toast.makeText(context, context.getString(R.string.msg_select_rename), Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(context, context.getString(R.string.msg_select_one_rename), Toast.LENGTH_SHORT).show()
    }
}

fun FileExplorerState.confirmRename(target: UniversalFile, newName: String) {
    if (target.parentId == "virtual://games_manager" && target.providerId.startsWith("content://")) {
        appConfigs.renameGameShortcut(Uri.parse(target.providerId), newName)
        refresh()
        selectionManager.clear()
        return
    }

    FileOperationsManager.enqueue(OperationType.RENAME, context.getString(R.string.op_renaming, target.name)) {
        FileOperationsManager.update(0, 1, operationType = OperationType.RENAME)
        FileOperationsManager.currentFileName.value = target.name

        val success = renameFile(target, newName, context)
        withContext(Dispatchers.Main) {
            if (success) {
                refresh()?.join()
                selectionManager.clear()
                val newFile = files.find { it.name == newName }
                if (newFile != null) selectionManager.select(newFile)
                GlobalEvents.triggerRefresh()
            } else {
                Toast.makeText(context, context.getString(R.string.msg_rename_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }
}

fun FileExplorerState.createNewFolder() {
    FileOperationsManager.openCreateFolder(context) { name ->
        FileOperationsManager.enqueue(OperationType.NONE, context.getString(R.string.dialog_new_folder)) {
            val success = createDirectory(context, currentPath, currentSafUri, currentNetworkProvider, currentNetworkId, name)
            withContext(Dispatchers.Main) {
                if (success) {
                    refresh()?.join()
                    val newFile = files.find { it.name == name }
                    if (newFile != null) selectionManager.select(newFile)
                    GlobalEvents.triggerRefresh()
                } else {
                    Toast.makeText(context, context.getString(R.string.msg_failed_create_folder), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val intent = Intent(context, PopUpActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

fun FileExplorerState.createNewFile() {
    FileOperationsManager.openCreateFile(context) { name ->
        FileOperationsManager.enqueue(OperationType.NONE, context.getString(R.string.dialog_new_file)) {
            val success = createFile(context, currentPath, currentSafUri, currentNetworkProvider, currentNetworkId, name)
            withContext(Dispatchers.Main) {
                if (success) {
                    refresh()?.join()
                    val newFile = files.find { it.name == name }
                    if (newFile != null) {
                        selectionManager.select(newFile)
                        // Automatically open in text editor if it's a text file
                        val ext = name.substringAfterLast('.', "").lowercase()
                        val textExtensions = setOf("txt", "log", "cfg", "ini", "md", "xml", "json", "sh", "py", "js", "html", "css")
                        if (textExtensions.contains(ext)) {
                            open(newFile)
                        }
                    }
                    GlobalEvents.triggerRefresh()
                } else {
                    Toast.makeText(context, context.getString(R.string.msg_failed_create_file), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val intent = Intent(context, PopUpActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

fun FileExplorerState.showProperties(items: List<UniversalFile>? = null) {
    val targets = items ?: selectionManager.selectedItems.toList()
    if (targets.isNotEmpty()) {
        FileOperationsManager.showProperties(targets)
        val intent = Intent(context, PopUpActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

fun FileExplorerState.emptyRecycleBin() {
    val trashDir = File(Environment.getExternalStorageDirectory(), ".Trash")
    val filesToDelete = trashDir.listFiles()?.filter { it.name != ".metadata" }?.map { it.toUniversal() } ?: emptyList()
    if (filesToDelete.isNotEmpty()) {
        val intent = Intent(context, PopUpActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)

        FileOperationsManager.enqueue(OperationType.DELETE, context.getString(R.string.menu_empty_recycle_bin)) {
            deleteFiles(context, filesToDelete, forcePermanent = true)
            val metaFile = File(trashDir, ".metadata")
            if (metaFile.exists()) metaFile.delete()
            refresh()?.join()
            GlobalEvents.triggerRefresh()
        }
    } else {
        Toast.makeText(context, context.getString(R.string.msg_recycle_bin_empty), Toast.LENGTH_SHORT).show()
    }
}

fun FileExplorerState.pinSelectionToHome() {
    val selected = selectionManager.selectedItems.toList()
    if (selected.size == 1) {
        ShortcutHelper.addToHome(context, scope, selected[0])
    } else if (selected.isEmpty()) {
        Toast.makeText(context, context.getString(R.string.msg_select_pin), Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(context, context.getString(R.string.msg_select_one_pin), Toast.LENGTH_SHORT).show()
    }
}
