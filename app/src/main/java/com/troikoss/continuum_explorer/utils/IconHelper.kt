package com.troikoss.continuum_explorer.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.ParcelFileDescriptor
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import com.troikoss.continuum_explorer.ui.theme.LocalExtendedColors
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import coil.Coil
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.troikoss.continuum_explorer.R
import com.troikoss.continuum_explorer.managers.SettingsManager
import com.troikoss.continuum_explorer.managers.IconTheme
import com.troikoss.continuum_explorer.model.UniversalFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Helper to determine the appropriate icon and handle thumbnails for files and folders.
 */
object IconHelper {

    /**
     * List of game folder names (or substrings) that should use the "external" game save color
     * (like PSP) even if they are technically located in restricted Android/data paths.
     */
    private val EMULATOR_FOLDERS = listOf("PSP", "Dolphin", "PS2", "NetherSX2", "NDS", "PSVita")

    // --- Public UI Components ---

    @Composable
    fun FolderIcon(
        modifier: Modifier = Modifier,
        name: String,
        path: String,
        providerId: String = "",
        iconSize: Dp = 24.dp,
        tint: Color = LocalExtendedColors.current.folderIcon
    ) {
        val customColor = SettingsManager.getFolderColor(providerId.ifEmpty { path })
        val finalTint = if (customColor != null) Color(customColor) else tint

        val iconTheme = SettingsManager.iconTheme.value
        if (iconTheme == IconTheme.MATERIAL) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = modifier.size(iconSize),
                tint = finalTint
            )
            return
        }

        val overlayRes = getOverlayIconRes(name, path, providerId)
        Box(contentAlignment = Alignment.Center, modifier = modifier.size(iconSize)) {
            val packageName = getPackageNameFromPath(providerId.ifEmpty { path })
            if (packageName != null && iconTheme != IconTheme.MATERIAL) {
                AppIcon(packageName, fallbackPainter = painterResource(id = R.drawable.ic_folder), modifier = modifier, iconSize = iconSize, tint = finalTint)
            } else {
                val baseFolderRes = if (iconTheme == IconTheme.COLOURFULDUO) R.drawable.ic_folder_duo else R.drawable.ic_folder
                Icon(
                    painter = painterResource(id = baseFolderRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = finalTint
                )
            }

            if (overlayRes != null) {
                val finalOverlayRes = if (iconTheme == IconTheme.COLOURFULDUO) {
                    when (overlayRes) {
                        R.drawable.ic_nav_gallery -> R.drawable.ic_nav_gallery_duo
                        R.drawable.ic_nav_recent -> R.drawable.ic_nav_recent_duo
                        R.drawable.ic_nav_downloads -> R.drawable.ic_nav_downloads_duo
                        R.drawable.ic_nav_documents -> R.drawable.ic_nav_documents_duo
                        R.drawable.ic_nav_game -> R.drawable.ic_nav_game_duo
                        R.drawable.ic_nav_trash -> R.drawable.ic_nav_trash_duo
                        else -> overlayRes
                    }
                } else overlayRes

                Icon(
                    painter = painterResource(id = finalOverlayRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(0.40f).offset(y = iconSize * 0.1f),
                    tint = Color.White
                )
            }
        }
    }

    @Composable
    fun FileThumbnail(
        file: UniversalFile,
        modifier: Modifier = Modifier,
        iconSize: Dp = 24.dp,
        tint: Color = MaterialTheme.colorScheme.secondary,
        isDetailView: Boolean = false,
        contentScale: ContentScale = ContentScale.Fit
    ) {
        val extendedColors = LocalExtendedColors.current
        val isSelected = tint == MaterialTheme.colorScheme.primary
        val iconTheme = SettingsManager.iconTheme.value
        val finalTint = if (!isSelected) {
            if (file.isDirectory) {
                when {
                    file.providerId == "virtual://gallery" || file.providerId.startsWith("virtual://gallery_album:") -> extendedColors.galleryIcon
                    file.providerId == "virtual://recent" -> extendedColors.recentIcon
                    file.providerId == "virtual://downloads" -> extendedColors.downloadsIcon
                    file.providerId == "virtual://documents" -> extendedColors.documentsIcon
                    file.providerId == "virtual://games_manager" -> extendedColors.gameIcon
                    file.providerId == "virtual://recycle_bin" || file.absolutePath.contains("/.Trash") -> extendedColors.recycleBinIcon
                    file.parentId == "virtual://games_manager" -> {
                        val isEmulator = EMULATOR_FOLDERS.any { file.name.contains(it, ignoreCase = true) }
                        when {
                            file.provider == com.troikoss.continuum_explorer.providers.SafProvider -> extendedColors.gameShortcutIcon
                            isEmulator -> extendedColors.recentIcon
                            else -> {
                                val customColor = SettingsManager.getFolderColor(file.providerId)
                                if (customColor != null) Color(customColor) else extendedColors.folderIcon
                            }
                        }
                    }
                    else -> {
                        val customColor = SettingsManager.getFolderColor(file.providerId)
                        if (customColor != null) Color(customColor) else extendedColors.folderIcon
                    }
                }
            } else {
                val name = file.name.lowercase()
                when {
                    name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z") ||
                    name.endsWith(".tar") || name.endsWith(".gz") -> extendedColors.zipIcon
                    name.endsWith(".pdf") -> if (iconTheme == IconTheme.COLOURFULDUO) Color.Unspecified else extendedColors.pdfIcon
                    name.endsWith(".xls") || name.endsWith(".xlsx") || name.endsWith(".ods") ||
                    name.endsWith(".csv") -> extendedColors.xlsIcon
                    name.endsWith(".doc") || name.endsWith(".docx") || name.endsWith(".odt") -> extendedColors.docxIcon
                    name.endsWith(".txt") -> extendedColors.txtIcon
                    name.endsWith(".sh") -> Color.Unspecified
                    else -> extendedColors.filesIcon
                }
            }
        } else {
            tint
        }

        if (file.isDirectory) {
            val overlayRes = getOverlayIconRes(file)

            if (iconTheme == IconTheme.MATERIAL) {
                Icon(
                    imageVector = getIconForItem(file),
                    contentDescription = null,
                    modifier = modifier.size(iconSize),
                    tint = finalTint
                )
                return
            }

            Box(contentAlignment = Alignment.Center, modifier = modifier.size(iconSize)) {
                val isGameSave = file.parentId == "virtual://games_manager"
                val packageName = getPackageNameFromPath(file.providerId, strict = !isGameSave)
                val isShowingAppIcon = packageName != null && iconTheme != IconTheme.MATERIAL
                
                if (isShowingAppIcon) {
                    AppIcon(packageName!!, fallbackPainter = painterResource(id = R.drawable.ic_nav_game_material), modifier = modifier, iconSize = iconSize, tint = finalTint)
                } else {
                    val baseFolderRes = if (iconTheme == IconTheme.COLOURFULDUO) R.drawable.ic_folder_duo else R.drawable.ic_folder
                    Icon(
                        painter = painterResource(id = baseFolderRes),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        tint = finalTint
                    )
                }

                if (overlayRes != null && !isShowingAppIcon) {
                    val finalOverlayRes = if (iconTheme == IconTheme.COLOURFULDUO) {
                        when (overlayRes) {
                            R.drawable.ic_nav_gallery -> R.drawable.ic_nav_gallery_duo
                            R.drawable.ic_nav_recent -> R.drawable.ic_nav_recent_duo
                            R.drawable.ic_nav_downloads -> R.drawable.ic_nav_downloads_duo
                            R.drawable.ic_nav_documents -> R.drawable.ic_nav_documents_duo
                            R.drawable.ic_nav_game -> R.drawable.ic_nav_game_duo
                            R.drawable.ic_nav_trash -> R.drawable.ic_nav_trash_duo
                            else -> overlayRes
                        }
                    } else overlayRes

                    Icon(
                        painter = painterResource(id = finalOverlayRes),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(0.40f).offset(y = iconSize * 0.1f),
                        tint = Color.White.copy(alpha = 1f)
                    )
                }
            }
            return
        }

        val fallbackPainter = if (iconTheme == IconTheme.COLOURFUL || iconTheme == IconTheme.COLOURFULDUO) {
            painterResource(id = getDrawableForItem(file, iconTheme))
        } else {
            rememberVectorPainter(getIconForItem(file))
        }

        if (!file.isDirectory && isMimeTypePreviewable(file)) {
            val name = file.name.lowercase()
            when {
                name.endsWith(".pdf") -> PdfThumbnail(file, fallbackPainter, modifier, iconSize, finalTint)
                name.endsWith(".apk") -> ApkThumbnail(file, fallbackPainter, modifier, iconSize, finalTint)
                name.endsWith(".txt") -> TextFilePreview(file, fallbackPainter, modifier, iconSize, finalTint, isDetailView)
                else -> {
                    SubcomposeAsyncImage(
                        model = if (file.provider.capabilities.isRemote) file else (file.documentFileRef?.uri ?: file.fileRef?.absolutePath),
                        contentDescription = null,
                        modifier = modifier,
                        contentScale = contentScale,
                    ) {
                        if (painter.state is AsyncImagePainter.State.Success) {
                            SubcomposeAsyncImageContent()
                        } else {
                            Icon(painter = fallbackPainter, contentDescription = null, modifier = Modifier.size(iconSize), tint = finalTint)
                        }
                    }
                }
            }
        } else {
            Icon(
                painter = fallbackPainter,
                contentDescription = null,
                modifier = modifier.size(iconSize),
                tint = finalTint
            )
        }
    }

    // --- Icon Logic ---

    /**
     * Returns the appropriate icon for a given UniversalFile.
     */
    fun getIconForItem(file: UniversalFile): ImageVector {
        if (file.isDirectory) {
            return when {
                file.providerId == "virtual://recent" -> Icons.Default.History
                file.providerId == "virtual://documents" -> Icons.Default.Description
                file.providerId == "virtual://gallery" || file.providerId.startsWith("virtual://gallery_album:") -> Icons.Default.Collections
                file.providerId == "virtual://recycle_bin" || file.absolutePath.contains("/.Trash") -> Icons.Default.Delete
                file.parentId == "virtual://games_manager" -> Icons.AutoMirrored.Filled.ListAlt
                else -> Icons.Default.Folder
            }
        }
        return getIconByFileName(file.name)
    }

    /**
     * Internal helper to determine icon based on file extension.
     */
    private fun getIconByFileName(fileName: String): ImageVector {
        val name = fileName.lowercase()
        return when {
            // Archives
            name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z") ||
            name.endsWith(".tar") || name.endsWith(".gz") -> Icons.Default.FolderZip

            // Images
            name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
            name.endsWith(".gif") || name.endsWith(".webp") || name.endsWith(".bmp") -> Icons.Default.Image

            // Videos
            name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi") ||
            name.endsWith(".mov") || name.endsWith(".webm") -> Icons.Default.VideoFile

            // Audio
            name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".ogg") ||
            name.endsWith(".m4a") || name.endsWith(".flac") -> Icons.Default.AudioFile

            // Documents
            name.endsWith(".pdf") -> Icons.Default.PictureAsPdf
            name.endsWith(".csv") || name.endsWith(".xls") || name.endsWith(".xlsx") ||
            name.endsWith(".ods") -> Icons.AutoMirrored.Filled.ListAlt
            name.endsWith(".txt") || name.endsWith(".doc") || name.endsWith(".docx") ||
            name.endsWith(".odt") -> Icons.Default.Description
            name.endsWith(".ppt") || name.endsWith(".pptx") || name.endsWith(".odp") -> Icons.Default.Slideshow

            name.endsWith(".apk") -> Icons.Default.Android
            name.endsWith(".sh") -> Icons.Default.Terminal

            // Default file icon
            else -> Icons.AutoMirrored.Filled.InsertDriveFile
        }
    }

    /**
     * Internal helper to determine drawable based on file extension for COLOURFUL theme.
     */
    fun getDrawableForItem(file: UniversalFile, iconTheme: IconTheme = IconTheme.COLOURFUL): Int {
        if (file.isDirectory) {
            return when {
                file.providerId == "virtual://gallery" || file.providerId.startsWith("virtual://gallery_album:") -> R.drawable.ic_nav_gallery
                file.providerId == "virtual://recent" -> R.drawable.ic_nav_recent
                file.providerId == "virtual://downloads" -> R.drawable.ic_nav_downloads
                file.providerId == "virtual://documents" -> R.drawable.ic_nav_documents
                file.providerId == "virtual://games_manager" -> R.drawable.ic_nav_game
                file.parentId == "virtual://games_manager" -> R.drawable.ic_nav_game
                file.providerId == "virtual://recycle_bin" || (file.fileRef?.absolutePath ?: "").contains("/.Trash") -> R.drawable.ic_nav_trash
                else -> if (iconTheme == IconTheme.COLOURFULDUO) R.drawable.ic_folder_duo else R.drawable.ic_folder
            }
        }
        return getDrawableByFileName(file.name, iconTheme)
    }

    /**
     * Internal helper to determine drawable based on file extension for COLOURFUL theme.
     */
    fun getDrawableByFileName(fileName: String, iconTheme: IconTheme = IconTheme.COLOURFUL): Int {
        val name = fileName.lowercase()
        return when {
            // Archives
            name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z") ||
                    name.endsWith(".tar") || name.endsWith(".gz") -> if (iconTheme == IconTheme.COLOURFULDUO) R.drawable.ic_zip_duo else R.drawable.ic_zip

            // Documents
            name.endsWith(".pdf") -> if (iconTheme == IconTheme.COLOURFULDUO) R.drawable.ic_pdf_duo else R.drawable.ic_pdf
            name.endsWith(".xls") || name.endsWith(".xlsx") || name.endsWith(".ods") || name.endsWith(
                ".csv"
            ) -> if (iconTheme == IconTheme.COLOURFULDUO) R.drawable.ic_xls_duo else R.drawable.ic_xls

            name.endsWith(".doc") || name.endsWith(".docx") || name.endsWith(".odt") -> if (iconTheme == IconTheme.COLOURFULDUO) R.drawable.ic_docx_duo else R.drawable.ic_docx
            name.endsWith(".txt") -> if (iconTheme == IconTheme.COLOURFULDUO) R.drawable.ic_txt_duo else R.drawable.ic_txt
            name.endsWith(".sh") -> if (iconTheme == IconTheme.COLOURFULDUO) R.drawable.ic_terminal_duo else R.drawable.ic_terminal

            // Default file icon
            else -> if (iconTheme == IconTheme.COLOURFULDUO) R.drawable.ic_file_duo else R.drawable.ic_file
        }
    }

    fun getOverlayIconRes(file: UniversalFile): Int? {
        val internalRoot = android.os.Environment.getExternalStorageDirectory().absolutePath
        val rootId = file.provider.rootId()
        val isRoot = file.parentId == rootId || file.parentId == internalRoot ||
                (file.providerId.startsWith("webdav://") && file.parentId != null && file.parentId!!.endsWith("//"))
        val isVirtual = file.providerId.startsWith("virtual://")
        val isGameSaveItem = file.parentId == "virtual://games_manager"

        // Only show overlays if the folder is in a storage root or is a virtual library item
        if (!isRoot && !isVirtual && !file.absolutePath.contains("/.trash") && !isGameSaveItem) {
            return null
        }
        if (file.providerId == "virtual://games_manager" || isGameSaveItem) return R.drawable.ic_nav_game
        return getOverlayIconRes(file.name, file.providerId, file.providerId)
    }

    fun getOverlayIconRes(name: String, path: String, providerId: String = ""): Int? {
        val lName = name.lowercase()
        val lPath = path.replace("\\", "/").lowercase()
        val lProviderId = providerId.lowercase()

        // 1. Virtual providers always get their icons
        if (lProviderId.startsWith("virtual://") || lPath.startsWith("virtual://")) {
            return when {
                lPath.contains("recent") -> R.drawable.ic_nav_recent
                lPath.contains("gallery") -> R.drawable.ic_nav_gallery
                lPath.contains("downloads") -> R.drawable.ic_nav_downloads
                lPath.contains("documents") -> R.drawable.ic_nav_documents
                lPath.contains("games_manager") -> R.drawable.ic_nav_game
                lPath.contains("trash") || lPath.contains("recycle_bin") -> R.drawable.ic_nav_trash
                else -> null
            }
        }

        // 2. Exception for .trash which we always want to show if it matches
        if (lPath.endsWith("/.trash") || lPath.contains("/.trash/")) return R.drawable.ic_nav_trash

        // 3. Determine if we are at a "root" level for various providers
        val internalRoot = android.os.Environment.getExternalStorageDirectory().absolutePath.replace("\\", "/").lowercase()

        val isDirectChildOfInternalRoot = lPath.startsWith("$internalRoot/") &&
                !lPath.substring(internalRoot.length + 1).contains('/')

        val isDirectChildOfStorageRoot = lPath.startsWith("/storage/") && !lPath.startsWith(internalRoot) &&
                lPath.substring(9).let { it.contains('/') && !it.substringAfter('/').contains('/') }

        // SAF, Network (WebDAV, FTP, SMB, etc.), Archives
        val isRemoteOrVirtualRoot = when {
            lProviderId.startsWith("webdav://") -> {
                val pathPart = lProviderId.substringAfter("://").substringAfter("/", "")
                pathPart.isNotEmpty() && !pathPart.trimEnd('/').contains('/')
            }
            lProviderId.startsWith("ftp://") || lProviderId.startsWith("sftp://") || lProviderId.startsWith("smb://") -> {
                val pathPart = lProviderId.substringAfter("://").substringAfter("/", "")
                pathPart.isNotEmpty() && !pathPart.trimEnd('/').contains('/')
            }
            lProviderId.startsWith("content://") -> {
                // For SAF, if there's no parent-like structure in the ID, it's likely a root child
                !lProviderId.substringAfterLast("%2f", "").contains("%2f") &&
                !lProviderId.substringAfterLast("/", "").contains("/")
            }
            else -> false
        }

        // 4. Restrict to root directory as requested
        if (!isDirectChildOfInternalRoot && !isDirectChildOfStorageRoot && !isRemoteOrVirtualRoot) return null

        // 5. Root folders get their overlays
        return when {
            lName == "documents" || lPath.endsWith("/documents") -> R.drawable.ic_documents_logo
            lName == "download" || lName == "downloads" || lPath.endsWith("/download") || lPath.endsWith("/downloads") -> R.drawable.ic_download_logo
            lName == "dcim" || lPath.endsWith("/dcim") || lName == "camera" || lPath.endsWith("/camera") -> R.drawable.ic_camera_logo
            lName == "pictures" || lPath.endsWith("/pictures") || lName == "photos" || lPath.endsWith("/photos") || lName == "screenshots" || lPath.endsWith("/screenshots") -> R.drawable.ic_gallery_logo
            else -> null
        }
    }


        fun isMimeTypePreviewable(file: UniversalFile): Boolean {
        val name = file.name.lowercase()
        return PREVIEWABLE_EXTENSIONS.any { name.endsWith(it) }
    }

    private val PREVIEWABLE_EXTENSIONS = setOf(
        ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp",
        ".mp4", ".mkv", ".avi", ".pdf", ".apk", ".txt"
    )

    // --- Internal Thumbnail Helpers ---

    private val folderPreviewCache = android.util.LruCache<String, java.util.Optional<UniversalFile>>(256)

    @Composable
    fun FolderPreview(
        folder: UniversalFile,
        thumbSize: Dp,
        modifier: Modifier = Modifier,
    ) {
        if (!folder.isDirectory) return

        // Skip previews for folders in the root of internal storage or those with custom overlays
        val internalRoot = android.os.Environment.getExternalStorageDirectory().absolutePath
        if (folder.parentId == internalRoot || getOverlayIconRes(folder) != null) return

        val cacheKey = "${folder.provider.kind}:${folder.absolutePath}"
        var previewFile by remember(folder) {
            mutableStateOf(folderPreviewCache[cacheKey]?.orElse(null))
        }

        LaunchedEffect(folder) {
            if (folderPreviewCache[cacheKey] != null) return@LaunchedEffect
            withContext(Dispatchers.IO) {
                val found = when {
                    folder.fileRef != null ->
                        folder.fileRef!!.listFiles()?.asSequence()?.map { it.toUniversal() }
                            ?.find { !it.isDirectory && isMimeTypePreviewable(it) && !it.name.lowercase().endsWith(".txt") }
                    folder.documentFileRef != null ->
                        folder.documentFileRef!!.listFiles().asSequence().map { it.toUniversal() }
                            .find { !it.isDirectory && isMimeTypePreviewable(it) && !it.name.lowercase().endsWith(".txt") }
                    else -> try {
                        folder.provider.listChildren(folder.providerId)
                            .find { !it.isDirectory && isMimeTypePreviewable(it) && !it.name.lowercase().endsWith(".txt") }
                    } catch (_: Exception) { null }
                }
                folderPreviewCache.put(cacheKey, java.util.Optional.ofNullable(found))
                previewFile = found
            }
        }

        Box(modifier = modifier) {
            previewFile?.let { file ->
                FileThumbnail(
                    file = file,
                    modifier = Modifier.size(thumbSize),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }

    @Composable
    private fun PdfThumbnail(
        file: UniversalFile,
        fallbackPainter: Painter,
        modifier: Modifier = Modifier,
        iconSize: Dp,
        tint: Color = MaterialTheme.colorScheme.primary
    ) {
        val context = LocalContext.current
        val thumbFile = remember(file) { DiskCache.getCacheFile(context, file) }
        var isReady by remember(file) { mutableStateOf(thumbFile.exists()) }

        LaunchedEffect(file) {
            if (!thumbFile.exists()) {
                withContext(Dispatchers.IO) { renderPdfThumbnail(context, file, thumbFile) }
                isReady = true
            }
        }

        ThumbnailImage(isReady, thumbFile, fallbackPainter, modifier, iconSize, tint)
    }

    @Composable
    private fun ApkThumbnail(
        file: UniversalFile,
        fallbackPainter: Painter,
        modifier: Modifier,
        iconSize: Dp,
        tint: Color
    ) {
        val context = LocalContext.current
        val thumbFile = remember(file) { DiskCache.getCacheFile(context, file) }
        var isReady by remember(file) { mutableStateOf(thumbFile.exists()) }

        LaunchedEffect(file) {
            if (!thumbFile.exists()) {
                withContext(Dispatchers.IO) { renderApkThumbnail(context, file, thumbFile) }
                isReady = true
            }
        }

        ThumbnailImage(isReady, thumbFile, fallbackPainter, modifier, iconSize, tint)
    }

    @Composable
    private fun ThumbnailImage(isReady: Boolean, thumbFile: File, fallbackPainter: Painter, modifier: Modifier, iconSize: Dp, tint: Color) {
        if (isReady) {
            SubcomposeAsyncImage(
                model = thumbFile,
                contentDescription = null,
                modifier = modifier,
                contentScale = ContentScale.Fit,
            ) {
                if (painter.state is AsyncImagePainter.State.Success) {
                    SubcomposeAsyncImageContent()
                } else {
                    Icon(painter = fallbackPainter, contentDescription = null, modifier = Modifier.size(iconSize), tint = tint)
                }
            }
        } else {
            Icon(painter = fallbackPainter, contentDescription = null, modifier = Modifier.size(iconSize), tint = tint)
        }
    }

    @Composable
    private fun TextFilePreview(
        file: UniversalFile,
        fallbackPainter: Painter,
        modifier: Modifier,
        iconSize: Dp,
        tint: Color,
        isDetailView: Boolean
    ) {
        var textSnippet by remember(file) { mutableStateOf("") }

        LaunchedEffect(file) {
            textSnippet = withContext(Dispatchers.IO) {
                try {
                    val stream = try { file.provider.openInput(file.providerId) } catch (_: Exception) { null }
                    stream?.use { it.bufferedReader().use { reader ->
                        val buffer = CharArray(300)
                        val length = reader.read(buffer)
                        if (length > 0) String(buffer, 0, length) else ""
                    } } ?: ""
                } catch (e: Exception) { "" }
            }
        }

        if (isDetailView && textSnippet.isNotEmpty()) {
            Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.extraSmall) {
                Text(
                    text = textSnippet,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(4.dp),
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Icon(painter = fallbackPainter, contentDescription = null, modifier = modifier.size(iconSize), tint = tint)
        }
    }

    @Composable
    fun AppIcon(
        packageName: String,
        fallbackPainter: Painter,
        modifier: Modifier = Modifier,
        iconSize: Dp,
        tint: Color
    ) {
        val context = LocalContext.current
        var appIcon by remember(packageName) { mutableStateOf<Drawable?>(null) }

        LaunchedEffect(packageName) {
            withContext(Dispatchers.IO) {
                try {
                    val pm = context.packageManager
                    appIcon = pm.getApplicationIcon(packageName)
                } catch (e: Exception) { }
            }
        }

        if (appIcon != null) {
            androidx.compose.foundation.Image(
                painter = coil.compose.rememberAsyncImagePainter(appIcon),
                contentDescription = null,
                modifier = modifier.size(iconSize)
            )
        } else {
            Icon(painter = fallbackPainter, contentDescription = null, modifier = modifier.size(iconSize), tint = tint)
        }
    }

    // --- Rendering Logic ---
    private suspend fun renderPdfThumbnail(context: Context, file: UniversalFile, thumbFile: File) {
        try {
            val pfd = when {
                file.documentFileRef != null ->
                    context.contentResolver.openFileDescriptor(file.documentFileRef!!.uri, "r")
                file.fileRef != null ->
                    ParcelFileDescriptor.open(file.fileRef, ParcelFileDescriptor.MODE_READ_ONLY)
                else -> {
                    val cached = RemoteCache.cache(context, file)
                    ParcelFileDescriptor.open(cached, ParcelFileDescriptor.MODE_READ_ONLY)
                }
            }
            pfd?.use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    renderer.openPage(0).use { page ->
                        val bmp = createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                        try {
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            saveBitmapToCache(bmp, thumbFile)
                        } finally {
                            bmp.recycle() // was missing
                        }
                    }
                } // renderer.close() now handled by .use
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private suspend fun renderApkThumbnail(context: Context, file: UniversalFile, thumbFile: File) {
        try {
            val apkPath = when {
                file.fileRef != null -> file.fileRef!!.absolutePath
                file.provider.capabilities.isRemote -> RemoteCache.cache(context, file).absolutePath
                else -> return
            }
            val pm = context.packageManager
            val info = pm.getPackageArchiveInfo(apkPath, 0)
            info?.applicationInfo?.let { appInfo ->
                appInfo.sourceDir = apkPath
                appInfo.publicSourceDir = apkPath
                saveBitmapToCache(drawableToBitmap(appInfo.loadIcon(pm)), thumbFile)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun saveBitmapToCache(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
    }

    // --- Utilities ---

    fun getPackageNameFromPath(path: String, strict: Boolean = true): String? {
        val normalizedPath = path.replace("\\", "/").lowercase()
        if (normalizedPath.contains("android/data/") || normalizedPath.contains("android/obb/")) {
            val segments = normalizedPath.split("/").filter { it.isNotEmpty() }
            val dataIndex = segments.indexOfFirst { it == "data" || it == "obb" }
            if (dataIndex != -1 && dataIndex + 1 < segments.size) {
                if (strict && dataIndex + 1 != segments.size - 1) {
                    return null
                }
                val potentialPackage = segments[dataIndex + 1]
                // Package names usually have at least one dot and don't start with it
                if (potentialPackage.contains(".") && !potentialPackage.startsWith(".")) {
                    return potentialPackage
                }
            }
        }
        return null
    }

    fun getAppName(context: Context, packageName: String): String? {
        return try {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            null
        }
    }

    fun getFolderBitmap(context: Context, path: String, name: String): Bitmap {
        val iconTheme = SettingsManager.iconTheme.value
        val folderRes = if (iconTheme == IconTheme.COLOURFULDUO) R.drawable.ic_folder_duo else R.drawable.ic_folder
        val overlayRes = getOverlayIconRes(name, path, "")

        val folderColor = when (iconTheme) {
            IconTheme.COLOURFUL -> 0xFF2196F3.toInt() // ThemeFolders
            IconTheme.COLOURFULDUO -> 0xFFFFC107.toInt() // ThemeFoldersDuo
            else -> android.graphics.Color.DKGRAY
        }

        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw folder
        val folderDrawable = androidx.core.content.ContextCompat.getDrawable(context, folderRes)
        folderDrawable?.let {
            it.setTint(folderColor)
            it.setBounds(0, 0, size, size)
            it.draw(canvas)
        }

        // Draw overlay
        if (overlayRes != null && iconTheme != IconTheme.MATERIAL) {
            val finalOverlayRes = if (iconTheme == IconTheme.COLOURFULDUO) {
                when (overlayRes) {
                    R.drawable.ic_nav_gallery -> R.drawable.ic_nav_gallery_duo
                    R.drawable.ic_nav_recent -> R.drawable.ic_nav_recent_duo
                    R.drawable.ic_nav_downloads -> R.drawable.ic_nav_downloads_duo
                    R.drawable.ic_nav_documents -> R.drawable.ic_nav_documents_duo
                    R.drawable.ic_nav_trash -> R.drawable.ic_nav_trash_duo
                    else -> overlayRes
                }
            } else overlayRes

            val overlayDrawable = androidx.core.content.ContextCompat.getDrawable(context, finalOverlayRes)
            overlayDrawable?.let {
                it.setTint(android.graphics.Color.WHITE)
                val overlaySize = (size * 0.40f).toInt()
                val x = (size - overlaySize) / 2
                val y = (size - overlaySize) / 2 + (size * 0.1f).toInt()
                it.setBounds(x, y, x + overlaySize, y + overlaySize)
                it.draw(canvas)
            }
        }

        return bitmap
    }

    suspend fun getFileBitmap(context: Context, file: UniversalFile): Bitmap = withContext(Dispatchers.IO) {
        getThumbnailBitmap(context, file) ?: getIconForItem(file).toBitmap(sizePx = 96)
    }

    private suspend fun getThumbnailBitmap(context: Context, file: UniversalFile): Bitmap? = withContext(Dispatchers.IO) {
        if (file.isDirectory) return@withContext null

        val thumbFile = DiskCache.getCacheFile(context, file)

        // Early return if already cached on disk
        if (thumbFile.exists()) {
            return@withContext BitmapFactory.decodeFile(thumbFile.absolutePath)
        }

        val name = file.name.lowercase()
        when {
            name.endsWith(".pdf") -> {
                renderPdfThumbnail(context, file, thumbFile)
                if (thumbFile.exists()) return@withContext BitmapFactory.decodeFile(thumbFile.absolutePath)
            }
            name.endsWith(".apk") -> {
                renderApkThumbnail(context, file, thumbFile)
                if (thumbFile.exists()) return@withContext BitmapFactory.decodeFile(thumbFile.absolutePath)
            }
            // Images and videos — load via Coil, no disk cache needed
            isMimeTypePreviewable(file) && !name.endsWith(".txt") -> {
                val loader = Coil.imageLoader(context)
                val data: Any = file.documentFileRef?.uri ?: file.fileRef?.absolutePath ?: file
                val request = ImageRequest.Builder(context)
                    .data(data)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = drawableToBitmap(result.drawable)
                    saveBitmapToCache(bitmap, thumbFile) // cache it so next call hits disk
                    return@withContext bitmap
                }
            }
        }
        return@withContext null
    }

    fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) return drawable.bitmap
        val bitmap = createBitmap(drawable.intrinsicWidth.coerceAtLeast(1), drawable.intrinsicHeight.coerceAtLeast(1))
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun ImageVector.toBitmap(sizePx: Int, tintArgb: Int = android.graphics.Color.DKGRAY): Bitmap {
        val bitmap = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bitmap)
        canvas.scale(sizePx / viewportWidth, sizePx / viewportHeight)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tintArgb
            style = Paint.Style.FILL
        }
        drawVectorGroup(canvas, root, paint)
        return bitmap
    }

    private fun drawVectorGroup(canvas: Canvas, group: VectorGroup, paint: Paint) {
        canvas.save()
        if (group.rotation != 0f) canvas.rotate(group.rotation, group.pivotX, group.pivotY)
        canvas.translate(group.translationX, group.translationY)
        canvas.scale(group.scaleX, group.scaleY, group.pivotX, group.pivotY)

        for (node in group) {
            when (node) {
                is VectorGroup -> drawVectorGroup(canvas, node, paint)
                is VectorPath -> canvas.drawPath(PathParser().addPathNodes(node.pathData).toPath().asAndroidPath(), paint)
            }
        }
        canvas.restore()
    }
}

object DiskCache {
    fun getThumbnailFolder(context: Context): File {
        val folder = File(context.cacheDir, "thumbnails")
        if (!folder.exists()) folder.mkdirs()
        return folder
    }

    fun getCacheFile(context: Context, file: UniversalFile): File {
        val size = file.length
        val lastModified = file.lastModified
        val key = "${file.providerId}_${size}_${lastModified}"
        val hash = java.security.MessageDigest.getInstance("MD5")
            .digest(key.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(getThumbnailFolder(context), "$hash.png")
    }
}
