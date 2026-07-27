package com.troikoss.continuum_explorer.ui

import android.os.Environment
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.troikoss.continuum_explorer.R
import com.troikoss.continuum_explorer.managers.*
import com.troikoss.continuum_explorer.model.LibraryItem
import com.troikoss.continuum_explorer.model.UniversalFile
import com.troikoss.continuum_explorer.providers.LocalProvider
import com.troikoss.continuum_explorer.ui.theme.LocalExtendedColors
import com.troikoss.continuum_explorer.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun HomeView(appState: FileExplorerState, onAddStorage: (() -> Unit)? = null) {
    val configs = appState.appConfigs
    val extendedColors = LocalExtendedColors.current
    val gridState = rememberLazyGridState()

    // Track dragging state for reordering
    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var draggingOffset by remember { mutableStateOf(Offset.Zero) }

    // Resolve persistent size for Home items
    LaunchedEffect(Unit) {
        appState.folderConfigs.resolveGridSize("virtual://home")
    }

    val itemSize = appState.folderConfigs.gridItemSize.dp

    // Map the dynamic libraryOrder to HomeItemData, excluding "home" itself
    val items = configs.libraryOrder.filter { it != "home" }.mapNotNull { id ->
        getHomeItemData(id, configs, appState, extendedColors)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .containerGestures(
                selectionManager = appState.selectionManager,
                focusRequester = remember { androidx.compose.ui.focus.FocusRequester() },
                viewMode = com.troikoss.continuum_explorer.model.ViewMode.GRID,
                columns = 1,
                onZoom = { factor ->
                    val newSize = (appState.folderConfigs.gridItemSize * factor).toInt()
                    appState.folderConfigs.updateGridSize(
                        newSize.coerceIn(80, 400),
                        "virtual://home"
                    )
                },
                onDragStart = {},
                onDrag = {},
                onDragEnd = {},
                mousePosition = { null },
                appState = appState,
                gridState = gridState
            )
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = itemSize),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .fadingEdge(gridState, showBottom = false),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(items, key = { it.id }) { item ->
                val isDragging = draggedItemId == item.id
                val elevation by animateDpAsState(if (isDragging) 12.dp else 0.dp, label = "HomeItemElevation")

                var showMenu by remember { mutableStateOf(false) }
                var menuOffset by remember { mutableStateOf(DpOffset.Zero) }


                Box(
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            if (isDragging) {
                                translationX = draggingOffset.x
                                translationY = draggingOffset.y
                            }
                        }
                        .shadow(elevation, RoundedCornerShape(20.dp))
                        .then(if (isDragging) Modifier else Modifier.animateItem())
                        .pointerInput(item.id) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                val startTime = System.currentTimeMillis()
                                var isLongPress = false
                                var dragTriggered = false

                                // Right-click handled immediately
                                if (currentEvent.isContextMenuTrigger()) {
                                    menuOffset = DpOffset(down.position.x.toDp(), down.position.y.toDp())
                                    showMenu = true
                                    down.consume()
                                    return@awaitEachGesture
                                }

                                while (true) {
                                    val elapsed = System.currentTimeMillis() - startTime

                                    // Use withTimeoutOrNull to ensure we wake up at exactly 800ms
                                    // If dragging, we don't need the timeout any more
                                    val event = if (dragTriggered) {
                                        awaitPointerEvent()
                                    } else {
                                        withTimeoutOrNull((800 - elapsed).coerceAtLeast(0)) {
                                            awaitPointerEvent()
                                        }
                                    }

                                    if (event == null) {
                                        // Timeout reached (800ms)
                                        if (!dragTriggered) {
                                            menuOffset = DpOffset(down.position.x.toDp(), down.position.y.toDp())
                                            showMenu = true

                                        }
                                        break
                                    }

                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    val currentElapsed = System.currentTimeMillis() - startTime

                                    if (!change.pressed) {
                                        // Click action
                                        if (currentElapsed < 300 && (change.position - down.position).getDistance() < viewConfiguration.touchSlop) {
                                            item.onClick()
                                        }
                                        break
                                    }

                                    val dist = (change.position - down.position).getDistance()

                                    // Check for Drag start (300ms)
                                    if (currentElapsed >= 300 && !dragTriggered && dist > viewConfiguration.touchSlop) {
                                        dragTriggered = true
                                        draggedItemId = item.id
                                        draggingOffset = Offset.Zero
                                    }

                                    // Check for Context Menu (800ms)
                                    if (currentElapsed >= 800 && !dragTriggered && dist < viewConfiguration.touchSlop) {
                                        menuOffset = DpOffset(down.position.x.toDp(), down.position.y.toDp())
                                        showMenu = true
                                        isLongPress = true

                                        // Consume the rest of the gesture until release
                                        // to prevent any click actions or secondary reactions
                                        var e = currentEvent
                                        while (true) {
                                            e.changes.forEach { if (it.id == down.id) it.consume() }
                                            if (!e.changes.any { it.pressed }) break
                                            e = awaitPointerEvent()
                                        }
                                        break
                                    }

                                    if (dragTriggered) {
                                        draggingOffset += change.positionChange()

                                        // Reordering logic
                                        val currentIndex = configs.libraryOrder.indexOf(item.id)
                                        val layoutInfo = gridState.layoutInfo
                                        val visibleItems = layoutInfo.visibleItemsInfo
                                        val draggedItemInfo = visibleItems.find { it.key == item.id }

                                        if (draggedItemInfo != null) {
                                            val center = draggedItemInfo.offset.let {
                                                Offset(it.x + draggedItemInfo.size.width / 2f + draggingOffset.x,
                                                    it.y + draggedItemInfo.size.height / 2f + draggingOffset.y)
                                            }

                                            val targetItem = visibleItems.find { info ->
                                                info.key != item.id && info.key != "home" &&
                                                        center.x in info.offset.x.toFloat()..(info.offset.x + info.size.width).toFloat() &&
                                                        center.y in info.offset.y.toFloat()..(info.offset.y + info.size.height).toFloat()
                                            }

                                            if (targetItem != null) {
                                                val targetIndex = configs.libraryOrder.indexOf(targetItem.key as String)
                                                if (currentIndex != -1 && targetIndex != -1) {
                                                    configs.moveLibraryItem(currentIndex, targetIndex)
                                                    draggingOffset -= Offset(
                                                        (targetItem.offset.x - draggedItemInfo.offset.x).toFloat(),
                                                        (targetItem.offset.y - draggedItemInfo.offset.y).toFloat()
                                                    )
                                                }
                                            }
                                        }
                                        change.consume()
                                    }
                                }
                                draggedItemId = null
                                draggingOffset = Offset.Zero
                            }
                        }
                ) {
                    HomeShortcutItem(item, appState, itemSize, showMenu, menuOffset, { showMenu = false }, onAddStorage)
                }
            }
        }
    }
}

@Composable
private fun getHomeItemData(id: String, configs: AppConfigurations, appState: FileExplorerState, extendedColors: com.troikoss.continuum_explorer.ui.theme.ExtendedColors): HomeItemData? {
    return when (id) {
        "recent" -> HomeItemData("recent", stringResource(R.string.nav_recent), Icons.Default.History, R.drawable.ic_nav_recent, R.drawable.ic_nav_recent_duo, configs.isRecentVisible, { configs.toggleRecentVisibility() }, { appState.navigateTo(null, null, libraryItem = LibraryItem.Recent) }, extendedColors.recentIcon)
        "gallery" -> HomeItemData("gallery", stringResource(R.string.nav_gallery), Icons.Default.Image, R.drawable.ic_nav_gallery, R.drawable.ic_nav_gallery_duo, configs.isGalleryVisible, { configs.toggleGalleryVisibility() }, { appState.navigateTo(null, null, libraryItem = LibraryItem.Gallery) }, extendedColors.galleryIcon)
        "music" -> HomeItemData("music", stringResource(R.string.nav_music), Icons.Default.MusicNote, R.drawable.ic_nav_music, R.drawable.ic_nav_music_duo, configs.isMusicVisible, { configs.toggleMusicVisibility() }, { appState.navigateTo(null, null, libraryItem = LibraryItem.Music) }, extendedColors.musicIcon)
        "downloads" -> HomeItemData("downloads", stringResource(R.string.nav_downloads), Icons.Default.FileDownload, R.drawable.ic_nav_downloads, R.drawable.ic_nav_downloads_duo, configs.isDownloadsVisible, { configs.toggleDownloadsVisibility() }, { appState.navigateTo(null, null, libraryItem = LibraryItem.Downloads) }, extendedColors.downloadsIcon)
        "documents" -> HomeItemData("documents", stringResource(R.string.nav_documents), Icons.Default.Description, R.drawable.ic_nav_documents, R.drawable.ic_nav_documents_duo, configs.isDocumentsVisible, { configs.toggleDocumentsVisibility() }, {
            if (configs.isDocumentsFolderEnabled) {
                val internalRoot = Environment.getExternalStorageDirectory()
                val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                if (!docsDir.exists()) docsDir.mkdirs()
                appState.navigateTo(docsDir, null, newRoot = internalRoot)
            } else {
                appState.navigateTo(null, null, libraryItem = LibraryItem.Documents)
            }
        }, extendedColors.documentsIcon)
        "archives" -> HomeItemData("archives", stringResource(R.string.nav_archives), Icons.Default.FolderZip, R.drawable.ic_zip, R.drawable.ic_zip_duo, configs.isArchivesVisible, { configs.toggleArchivesVisibility() }, { appState.navigateTo(null, null, libraryItem = LibraryItem.Archives) }, extendedColors.zipIcon)
        "apks" -> HomeItemData("apks", stringResource(R.string.nav_apks), Icons.Default.Android, R.drawable.ic_android_logo, R.drawable.ic_android_logo, configs.isApksVisible, { configs.toggleApksVisibility() }, { appState.navigateTo(null, null, libraryItem = LibraryItem.Apks) }, extendedColors.androidIcon)
        "games_manager" -> HomeItemData("games_manager", stringResource(R.string.nav_game_saves), Icons.Default.Gamepad, R.drawable.ic_nav_game, R.drawable.ic_nav_game_duo, configs.isGamesVisible, { configs.toggleGamesVisibility() }, { appState.navigateTo(null, null, libraryItem = LibraryItem.Games) }, extendedColors.gameIcon)
        "internal_storage" -> HomeItemData("internal_storage", stringResource(R.string.nav_internal_storage), Icons.Default.Storage, R.drawable.ic_storage, R.drawable.ic_storage_duo, configs.isInternalStorageVisible, { configs.toggleInternalStorageVisibility() }, { appState.navigateTo(Environment.getExternalStorageDirectory(), null) }, extendedColors.sidebarIcons)
        "trash" -> HomeItemData("trash", stringResource(R.string.nav_trash), Icons.Default.Delete, R.drawable.ic_nav_trash, R.drawable.ic_nav_trash_duo, configs.isTrashVisible, { configs.toggleTrashVisibility() }, { appState.navigateTo(null, null, libraryItem = LibraryItem.RecycleBin) }, extendedColors.recycleBinIcon)
        else -> null
    }
}

data class HomeItemData(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val customIcon: Int,
    val customIconDuo: Int,
    val isVisible: Boolean,
    val onToggle: () -> Unit,
    val onClick: () -> Unit,
    val iconTint: Color
)

@Composable
fun HomeShortcutItem(
    item: HomeItemData,
    appState: FileExplorerState,
    itemSize: androidx.compose.ui.unit.Dp,
    showMenu: Boolean,
    menuOffset: DpOffset,
    onDismissMenu: () -> Unit,
    onAddStorage: (() -> Unit)? = null
) {
    val iconTheme = SettingsManager.getEffectiveIconTheme(IconCategory.HOME)

    // Scale card components based on itemSize
    val cardHeight = (itemSize * 0.875f).coerceAtLeast(100.dp)
    val iconSize = (itemSize * 0.3f).coerceIn(24.dp, 64.dp)
    val fontSize = (itemSize.value * 0.09f).coerceIn(12f, 20f).sp

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .clip(RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val iconRes = if (iconTheme == IconTheme.COLOURFULDUO) item.customIconDuo else item.customIcon

                if (iconTheme == IconTheme.MATERIAL) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                        tint = item.iconTint
                    )
                } else {
                    Icon(
                        painter = IconHelper.rememberThemePainter(resId = iconRes, category = com.troikoss.continuum_explorer.managers.IconCategory.HOME),
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                        tint = item.iconTint
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = item.label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = fontSize
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = onDismissMenu,
            offset = menuOffset,
            containerColor = LocalExtendedColors.current.menuBackground
        ) {
            Column(
                modifier = Modifier.animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
            ) {
                // Section-specific items (Games, Gallery, Music, etc.)
                if (item.id == "games_manager") {
                    val isFtpEnabled by SettingsManager.isFtpServerEnabled
                    val ftpMode by SettingsManager.ftpMode
                    val context = LocalContext.current
                    val isGamesFtpActive = isFtpEnabled && ftpMode == SettingsManager.FtpMode.GAMES

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.nav_add_storage)) },
                        onClick = {
                            onDismissMenu()
                            if (onAddStorage != null) {
                                appState.isAddingGameShortcut = true
                                onAddStorage()
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.Add, null) }
                    )

                    DropdownMenuItem(
                        text = { Text(if (isGamesFtpActive) stringResource(R.string.nav_stop_ftp_game_manager) else stringResource(R.string.nav_start_ftp_game_manager)) },
                        onClick = {
                            onDismissMenu()
                            if (isGamesFtpActive) {
                                SettingsManager.setFtpServerEnabled(context, false)
                            } else {
                                SettingsManager.setFtpServerEnabled(context, true, SettingsManager.FtpMode.GAMES)
                            }
                        },
                        leadingIcon = { Icon(if (isGamesFtpActive) Icons.Default.WifiOff else Icons.Default.Wifi, null) }
                    )
                    HorizontalDivider()
                }

                if (item.id == "gallery") {
                    val isFilterEnabled by SettingsManager.isGalleryFilterEnabled
                    val context = LocalContext.current

                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.menu_gallery_show_all))
                                if (!isFilterEnabled) {
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        onClick = {
                            onDismissMenu()
                            if (isFilterEnabled) {
                                SettingsManager.setGalleryFilterEnabled(context, false)
                                appState.refresh()
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.Collections, null) }
                    )

                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.menu_gallery_folders))
                                if (isFilterEnabled) {
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        onClick = {
                            onDismissMenu()
                            if (!isFilterEnabled) {
                                SettingsManager.setGalleryFilterEnabled(context, true)
                                appState.refresh()
                            }
                            appState.isConfiguringGalleryFolders = true
                        },
                        leadingIcon = { Icon(Icons.Default.Folder, null) }
                    )

                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.menu_gallery_albums))
                                if (appState.appConfigs.isGalleryAlbumsEnabled) {
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        onClick = {
                            onDismissMenu()
                            appState.appConfigs.toggleGalleryAlbums()
                        },
                        leadingIcon = {
                            val iconTheme = SettingsManager.getEffectiveIconTheme(IconCategory.HOME)
                            if (iconTheme == IconTheme.MATERIAL) {
                                Icon(Icons.Default.Folder, null)
                            } else {
                                val resId = if (iconTheme == IconTheme.COLOURFULDUO) R.drawable.ic_folder_duo else R.drawable.ic_folder
                                Icon(IconHelper.rememberThemePainter(resId, category = com.troikoss.continuum_explorer.managers.IconCategory.HOME), null)
                            }
                        }
                    )
                    HorizontalDivider()
                }

                if (item.id == "music") {
                    val isFilterEnabled by SettingsManager.isMusicFilterEnabled
                    val context = LocalContext.current

                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.menu_gallery_show_all))
                                if (!isFilterEnabled) {
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        onClick = {
                            onDismissMenu()
                            if (isFilterEnabled) {
                                SettingsManager.setMusicFilterEnabled(context, false)
                                appState.refresh()
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.LibraryMusic, null) }
                    )

                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.menu_music_folders))
                                if (isFilterEnabled) {
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        onClick = {
                            onDismissMenu()
                            if (!isFilterEnabled) {
                                SettingsManager.setMusicFilterEnabled(context, true)
                                appState.refresh()
                            }
                            appState.isConfiguringMusicFolders = true
                        },
                        leadingIcon = { Icon(Icons.Default.Folder, null) }
                    )

                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.menu_music_albums))
                                if (appState.appConfigs.isMusicAlbumsEnabled) {
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        onClick = {
                            onDismissMenu()
                            appState.appConfigs.toggleMusicAlbums()
                        },
                        leadingIcon = {
                            val iconTheme = SettingsManager.getEffectiveIconTheme(IconCategory.HOME)
                            if (iconTheme == IconTheme.MATERIAL) {
                                Icon(Icons.Default.Folder, null)
                            } else {
                                val resId = if (iconTheme == IconTheme.COLOURFULDUO) R.drawable.ic_folder_duo else R.drawable.ic_folder
                                Icon(IconHelper.rememberThemePainter(resId, category = com.troikoss.continuum_explorer.managers.IconCategory.HOME), null)
                            }
                        }
                    )

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_sync_list)) },
                        onClick = {
                            onDismissMenu()
                            appState.scope.launch(Dispatchers.IO) {
                                MusicMetadataManager.sync(appState.context, SettingsManager.musicFolders.value)
                                GlobalEvents.triggerRefresh()
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.Sync, null) }
                    )
                    HorizontalDivider()
                }

                if (item.id == "documents") {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.menu_documents_show_folders))
                                if (appState.appConfigs.isDocumentsFolderEnabled) {
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        onClick = {
                            onDismissMenu()
                            appState.appConfigs.toggleDocumentsFolder()
                            item.onClick()
                        },
                        leadingIcon = {
                            val iconTheme = SettingsManager.getEffectiveIconTheme(IconCategory.HOME)
                            if (iconTheme == IconTheme.MATERIAL) {
                                Icon(Icons.Default.Folder, null)
                            } else {
                                val resId = if (iconTheme == IconTheme.COLOURFULDUO) R.drawable.ic_folder_duo else R.drawable.ic_folder
                                Icon(IconHelper.rememberThemePainter(resId, category = com.troikoss.continuum_explorer.managers.IconCategory.HOME), null)
                            }
                        }
                    )
                    HorizontalDivider()
                }

                // General items (Open, New Window)
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_open)) },
                    onClick = {
                        onDismissMenu()
                        item.onClick()
                    },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                )

                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_open_new_window)) },
                    onClick = {
                        onDismissMenu()
                        val virtualFile = when (item.id) {
                            "recent" -> UniversalFile(appState.context.getString(R.string.nav_recent), true, 0, 0, LocalProvider, "virtual://recent")
                            "gallery" -> UniversalFile(appState.context.getString(R.string.nav_gallery), true, 0, 0, LocalProvider, "virtual://gallery")
                            "music" -> UniversalFile(appState.context.getString(R.string.nav_music), true, 0, 0, LocalProvider, "virtual://music")
                            "downloads" -> UniversalFile(appState.context.getString(R.string.nav_downloads), true, 0, 0, LocalProvider, "virtual://downloads")
                            "documents" -> UniversalFile(appState.context.getString(R.string.nav_documents), true, 0, 0, LocalProvider, "virtual://documents")
                            "archives" -> UniversalFile(appState.context.getString(R.string.nav_archives), true, 0, 0, LocalProvider, "virtual://archives")
                            "apks" -> UniversalFile(appState.context.getString(R.string.nav_apks), true, 0, 0, LocalProvider, "virtual://apks")
                            "games_manager" -> UniversalFile(appState.context.getString(R.string.nav_game_saves), true, 0, 0, LocalProvider, "virtual://games_manager")
                            "internal_storage" -> UniversalFile(appState.context.getString(R.string.nav_internal_storage), true, 0, 0, LocalProvider, Environment.getExternalStorageDirectory().absolutePath)
                            "trash" -> UniversalFile(appState.context.getString(R.string.nav_trash), true, 0, 0, LocalProvider, "virtual://recycle_bin")
                            else -> null
                        }
                        virtualFile?.let { appState.openInNewWindow(listOf(it)) }
                    },
                    leadingIcon = { Icon(Icons.Default.Tab, null) }
                )

                HorizontalDivider()

                // Library visibility toggle
                DropdownMenuItem(
                    text = { Text(if (item.isVisible) stringResource(R.string.menu_remove) else stringResource(R.string.menu_add_library)) },
                    onClick = {
                        onDismissMenu()
                        item.onToggle()
                    },
                    leadingIcon = { Icon(if (item.isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) }
                )

                if (item.id == "trash") {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_empty_recycle_bin)) },
                        onClick = {
                            onDismissMenu()
                            appState.emptyRecycleBin()
                        },
                        leadingIcon = { Icon(Icons.Default.DeleteForever, null) }
                    )
                }

                HorizontalDivider()

                // Properties
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_properties)) },
                    onClick = {
                        onDismissMenu()
                        val virtualFile = when (item.id) {
                            "recent" -> UniversalFile(appState.context.getString(R.string.nav_recent), true, 0, 0, LocalProvider, "virtual://recent")
                            "gallery" -> UniversalFile(appState.context.getString(R.string.nav_gallery), true, 0, 0, LocalProvider, "virtual://gallery")
                            "music" -> UniversalFile(appState.context.getString(R.string.nav_music), true, 0, 0, LocalProvider, "virtual://music")
                            "downloads" -> UniversalFile(appState.context.getString(R.string.nav_downloads), true, 0, 0, LocalProvider, "virtual://downloads")
                            "documents" -> UniversalFile(appState.context.getString(R.string.nav_documents), true, 0, 0, LocalProvider, "virtual://documents")
                            "archives" -> UniversalFile(appState.context.getString(R.string.nav_archives), true, 0, 0, LocalProvider, "virtual://archives")
                            "apks" -> UniversalFile(appState.context.getString(R.string.nav_apks), true, 0, 0, LocalProvider, "virtual://apks")
                            "games_manager" -> UniversalFile(appState.context.getString(R.string.nav_game_saves), true, 0, 0, LocalProvider, "virtual://games_manager")
                            "internal_storage" -> UniversalFile(appState.context.getString(R.string.nav_internal_storage), true, 0, 0, LocalProvider, Environment.getExternalStorageDirectory().absolutePath)
                            "trash" -> {
                                val trashDir = File(Environment.getExternalStorageDirectory(), ".Trash")
                                UniversalFile(appState.context.getString(R.string.nav_trash), true, 0, 0, LocalProvider, trashDir.absolutePath)
                            }
                            else -> null
                        }
                        virtualFile?.let { appState.showProperties(listOf(it)) }
                    },
                    leadingIcon = { Icon(Icons.Default.Info, null) }
                )
            }
        }
    }
}
