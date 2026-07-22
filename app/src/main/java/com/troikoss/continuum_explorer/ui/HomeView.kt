package com.troikoss.continuum_explorer.ui

import android.os.Environment
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import com.troikoss.continuum_explorer.ui.theme.FileExplorerTheme

@Composable
fun HomeView(appState: FileExplorerState, onAddStorage: (() -> Unit)? = null) {
    val configs = appState.appConfigs
    val extendedColors = LocalExtendedColors.current
    val gridState = rememberLazyGridState()
    val density = LocalDensity.current

    // Resolve persistent size for Home items
    LaunchedEffect(Unit) {
        appState.folderConfigs.resolveGridSize("virtual://home")
    }

    val itemSize = appState.folderConfigs.gridItemSize.dp
    
    val items = listOf(
        HomeItemData("recent", stringResource(R.string.nav_recent), Icons.Default.History, R.drawable.ic_nav_recent, R.drawable.ic_nav_recent_duo, configs.isRecentVisible, { configs.toggleRecentVisibility() }, { appState.navigateTo(null, null, libraryItem = LibraryItem.Recent) }, extendedColors.recentIcon),
        HomeItemData("gallery", stringResource(R.string.nav_gallery), Icons.Default.Image, R.drawable.ic_nav_gallery, R.drawable.ic_nav_gallery_duo, configs.isGalleryVisible, { configs.toggleGalleryVisibility() }, { appState.navigateTo(null, null, libraryItem = LibraryItem.Gallery) }, extendedColors.galleryIcon),
        HomeItemData("music", stringResource(R.string.nav_music), Icons.Default.MusicNote, R.drawable.ic_nav_music, R.drawable.ic_nav_music_duo, configs.isMusicVisible, { configs.toggleMusicVisibility() }, { appState.navigateTo(null, null, libraryItem = LibraryItem.Music) }, extendedColors.musicIcon),
        HomeItemData("downloads", stringResource(R.string.nav_downloads), Icons.Default.FileDownload, R.drawable.ic_nav_downloads, R.drawable.ic_nav_downloads_duo, configs.isDownloadsVisible, { configs.toggleDownloadsVisibility() }, { appState.navigateTo(null, null, libraryItem = LibraryItem.Downloads) }, extendedColors.downloadsIcon),
        HomeItemData("documents", stringResource(R.string.nav_documents), Icons.Default.Description, R.drawable.ic_nav_documents, R.drawable.ic_nav_documents_duo, configs.isDocumentsVisible, { configs.toggleDocumentsVisibility() }, { appState.navigateTo(null, null, libraryItem = LibraryItem.Documents) }, extendedColors.documentsIcon),
        HomeItemData("archives", stringResource(R.string.nav_archives), Icons.Default.FolderZip, R.drawable.ic_zip, R.drawable.ic_zip_duo, configs.isArchivesVisible, { configs.toggleArchivesVisibility() }, { appState.navigateTo(null, null, libraryItem = LibraryItem.Archives) }, extendedColors.zipIcon),
        HomeItemData("apks", stringResource(R.string.nav_apks), Icons.Default.Android, R.drawable.ic_android_logo, R.drawable.ic_android_logo, configs.isApksVisible, { configs.toggleApksVisibility() }, { appState.navigateTo(null, null, libraryItem = LibraryItem.Apks) }, extendedColors.androidIcon),
        HomeItemData("games_manager", stringResource(R.string.nav_game_saves), Icons.Default.Gamepad, R.drawable.ic_nav_game, R.drawable.ic_nav_game_duo, configs.isGamesVisible, { configs.toggleGamesVisibility() }, { appState.navigateTo(null, null, libraryItem = LibraryItem.Games) }, extendedColors.gameIcon),
        HomeItemData("trash", stringResource(R.string.nav_trash), Icons.Default.Delete, R.drawable.ic_nav_trash, R.drawable.ic_nav_trash_duo, configs.isTrashVisible, { configs.toggleTrashVisibility() }, { appState.navigateTo(null, null, libraryItem = LibraryItem.RecycleBin) }, extendedColors.recycleBinIcon)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .containerGestures(
                selectionManager = appState.selectionManager,
                focusRequester = remember { androidx.compose.ui.focus.FocusRequester() },
                viewMode = com.troikoss.continuum_explorer.model.ViewMode.GRID,
                columns = 1, // Not strictly used for Home zoom
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
            contentPadding = PaddingValues(top = 80.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(items, key = { it.id }) { item ->
                HomeShortcutItem(item, appState, itemSize, onAddStorage)
            }
        }

        // Fading Header
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                FileExplorerTheme.extendedColors.background,
                                FileExplorerTheme.extendedColors.background,
                                FileExplorerTheme.extendedColors.background.copy(alpha = 0.9f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = stringResource(R.string.nav_home),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
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
fun HomeShortcutItem(item: HomeItemData, appState: FileExplorerState, itemSize: androidx.compose.ui.unit.Dp, onAddStorage: (() -> Unit)? = null) {
    val iconTheme = SettingsManager.iconTheme.value
    var showMenu by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }
    val density = LocalDensity.current
    
    // Scale card components based on itemSize
    val cardHeight = (itemSize * 0.875f).coerceAtLeast(100.dp)
    val iconSize = (itemSize * 0.3f).coerceIn(24.dp, 64.dp)
    val fontSize = (itemSize.value * 0.09f).coerceIn(12f, 20f).sp

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .clip(RoundedCornerShape(20.dp))
                .clickable { item.onClick() }
                .contextMenuDetector { offset ->
                    menuOffset = with(density) { DpOffset(offset.x.toDp(), offset.y.toDp()) }
                    showMenu = true
                },
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
                        painter = IconHelper.rememberThemePainter(resId = iconRes),
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
            onDismissRequest = { showMenu = false },
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
                            showMenu = false
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
                            showMenu = false
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
                            showMenu = false
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
                            showMenu = false
                            if (!isFilterEnabled) {
                                SettingsManager.setGalleryFilterEnabled(context, true)
                                appState.refresh()
                            }
                            appState.isConfiguringGalleryFolders = true
                        },
                        leadingIcon = { Icon(Icons.Default.Folder, null) }
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
                            showMenu = false
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
                            showMenu = false
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
                            showMenu = false
                            appState.appConfigs.toggleMusicAlbums()
                        },
                        leadingIcon = { Icon(Icons.Default.Album, null) }
                    )

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_sync_list)) },
                        onClick = {
                            showMenu = false
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
                            showMenu = false
                            appState.appConfigs.toggleDocumentsFolder()
                        },
                        leadingIcon = { Icon(Icons.Default.Folder, null) }
                    )
                    HorizontalDivider()
                }

                // General items (Open, New Window)
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_open)) },
                    onClick = {
                        showMenu = false
                        item.onClick()
                    },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                )
                
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_open_new_window)) },
                    onClick = {
                        showMenu = false
                        val virtualFile = when (item.id) {
                            "recent" -> UniversalFile(appState.context.getString(R.string.nav_recent), true, 0, 0, LocalProvider, "virtual://recent")
                            "gallery" -> UniversalFile(appState.context.getString(R.string.nav_gallery), true, 0, 0, LocalProvider, "virtual://gallery")
                            "music" -> UniversalFile(appState.context.getString(R.string.nav_music), true, 0, 0, LocalProvider, "virtual://music")
                            "downloads" -> UniversalFile(appState.context.getString(R.string.nav_downloads), true, 0, 0, LocalProvider, "virtual://downloads")
                            "documents" -> UniversalFile(appState.context.getString(R.string.nav_documents), true, 0, 0, LocalProvider, "virtual://documents")
                            "archives" -> UniversalFile(appState.context.getString(R.string.nav_archives), true, 0, 0, LocalProvider, "virtual://archives")
                            "apks" -> UniversalFile(appState.context.getString(R.string.nav_apks), true, 0, 0, LocalProvider, "virtual://apks")
                            "games_manager" -> UniversalFile(appState.context.getString(R.string.nav_game_saves), true, 0, 0, LocalProvider, "virtual://games_manager")
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
                        showMenu = false
                        item.onToggle()
                    },
                    leadingIcon = { Icon(if (item.isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) }
                )

                if (item.id == "trash") {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_empty_recycle_bin)) },
                        onClick = {
                            showMenu = false
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
                        showMenu = false
                        val virtualFile = when (item.id) {
                            "recent" -> UniversalFile(appState.context.getString(R.string.nav_recent), true, 0, 0, LocalProvider, "virtual://recent")
                            "gallery" -> UniversalFile(appState.context.getString(R.string.nav_gallery), true, 0, 0, LocalProvider, "virtual://gallery")
                            "music" -> UniversalFile(appState.context.getString(R.string.nav_music), true, 0, 0, LocalProvider, "virtual://music")
                            "downloads" -> UniversalFile(appState.context.getString(R.string.nav_downloads), true, 0, 0, LocalProvider, "virtual://downloads")
                            "documents" -> UniversalFile(appState.context.getString(R.string.nav_documents), true, 0, 0, LocalProvider, "virtual://documents")
                            "archives" -> UniversalFile(appState.context.getString(R.string.nav_archives), true, 0, 0, LocalProvider, "virtual://archives")
                            "apks" -> UniversalFile(appState.context.getString(R.string.nav_apks), true, 0, 0, LocalProvider, "virtual://apks")
                            "games_manager" -> UniversalFile(appState.context.getString(R.string.nav_game_saves), true, 0, 0, LocalProvider, "virtual://games_manager")
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
