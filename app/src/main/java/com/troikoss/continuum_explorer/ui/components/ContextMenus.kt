package com.troikoss.continuum_explorer.ui.components

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.troikoss.continuum_explorer.R
import com.troikoss.continuum_explorer.managers.MusicMetadataManager
import com.troikoss.continuum_explorer.managers.SettingsManager
import com.troikoss.continuum_explorer.model.FileColumnType
import com.troikoss.continuum_explorer.model.LibraryItem
import com.troikoss.continuum_explorer.model.ViewMode
import com.troikoss.continuum_explorer.ui.theme.LocalExtendedColors
import com.troikoss.continuum_explorer.ui.theme.ThemeFolderColors
import com.troikoss.continuum_explorer.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Composable
private fun CtrlShortcut(key: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = R.drawable.ic_control),
            contentDescription = null,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = "+ $key",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Context menu shown when right-clicking or long-pressing a specific file or folder.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    appState: FileExplorerState
) {
    val context = LocalContext.current
    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    var hasClipboardItems by remember { mutableStateOf(false) }

    var currentScreen by remember { mutableStateOf("MAIN") }

    LaunchedEffect(expanded) {
        if (expanded) {
            hasClipboardItems = clipboard.hasPrimaryClip()
        } else {
            currentScreen = "MAIN"
        }
    }
    val selectionManager = appState.selectionManager
    val virtualStorage = listOf(LibraryItem.RecycleBin, LibraryItem.Gallery, LibraryItem.Videos, LibraryItem.Recent, LibraryItem.Documents, LibraryItem.Games)
    val isInVirtualStorage = appState.libraryItem in virtualStorage
    val isInGalleryOrVideos = appState.libraryItem == LibraryItem.Gallery || appState.libraryItem == LibraryItem.Videos
    val isInRecycleBin = appState.libraryItem == LibraryItem.RecycleBin
    val selectedItems = selectionManager.selectedItems.toList()
    val onlyOneSelected = selectedItems.size == 1
    val hasDirectories = selectedItems.any { it.isDirectory }
    val hasArchive = selectedItems.any { ZipUtils.isArchive(it) }


    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = LocalExtendedColors.current.menuBackground
    ) {
        when (currentScreen) {
            "MAIN" -> {
                if (onlyOneSelected && appState.libraryItem == LibraryItem.Games) {
                    val item = selectedItems.first()
                    if (item.providerId.startsWith("content://")) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_remove)) },
                            onClick = {
                                onDismiss()
                                appState.appConfigs.removeGameSafUri(Uri.parse(item.providerId))
                                appState.refresh()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, null) }
                        )
                        HorizontalDivider()
                    }
                }

                if (onlyOneSelected) {
                    val item = selectedItems.first()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_open)) },
                        onClick = {
                            onDismiss()
                            appState.open(item)
                        },
                        leadingIcon = { Icon(IconHelper.getIconForItem(item), null) },
                        trailingIcon = {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardReturn,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    )

                    if (!hasDirectories) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_open_with)) },
                            onClick = {
                                onDismiss()
                                openWith(context, appState.scope, selectedItems.first())
                            },
                            leadingIcon = { Icon(Icons.Default.FolderOpen, null) }
                        )
                    }

                    HorizontalDivider()
                }

                if (hasDirectories || hasArchive) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_open_new_tab)) },
                        onClick = {
                            onDismiss()
                            appState.openInNewTab(selectedItems)
                        },
                        leadingIcon = { Icon(Icons.Default.Tab, null) },
                        trailingIcon = {
                            Text(
                                text = "MMB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    if (onlyOneSelected && selectedItems.first().isDirectory && selectedItems.first().fileRef != null && isTermuxInstalled(context)) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_open_terminal)) },
                            onClick = {
                                onDismiss()
                                openInTermux(context, selectedItems.first().fileRef!!.absolutePath)
                            },
                            leadingIcon = { Icon(Icons.Default.Terminal, null) }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_open_new_window)) },
                        onClick = {
                            onDismiss()
                            appState.openInNewWindow(selectedItems)
                        },
                        leadingIcon = { Icon(Icons.Default.Splitscreen, null) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_shift),
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "+ MMB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                    HorizontalDivider()
                }

                if (onlyOneSelected && selectedItems.first().isDirectory) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_folder_color)) },
                        onClick = { currentScreen = "COLORS" },
                        leadingIcon = { Icon(Icons.Default.Folder, null, tint = SettingsManager.getFolderColor(selectedItems.first().providerId)?.let { androidx.compose.ui.graphics.Color(it) } ?: LocalExtendedColors.current.folderIcon) },
                        trailingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                    )
                    HorizontalDivider()
                }

                if (onlyOneSelected && selectedItems.first().isDirectory && selectedItems.first().fileRef != null) {
                    val path = selectedItems.first().fileRef!!.absolutePath
                    val isFav = appState.appConfigs.isFavorite(path)

                    DropdownMenuItem(
                        text = { Text(if (isFav) stringResource(R.string.menu_remove_favorites) else stringResource(R.string.menu_add_favorites)) },
                        onClick = {
                            if (isFav) appState.appConfigs.removeFavorite(path)
                            else appState.appConfigs.addFavorite(path)
                            onDismiss()
                        },
                        leadingIcon = { Icon(if (isFav) Icons.Default.StarOutline else Icons.Default.Star, null) }
                    )
                }

                if (onlyOneSelected) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_add_home)) },
                        onClick = {
                            onDismiss()
                            appState.pinSelectionToHome()
                        },
                        leadingIcon = { Icon(Icons.Default.PushPin, null) }
                    )
                    HorizontalDivider()
                }

                if (!isInVirtualStorage || (isInGalleryOrVideos && appState.currentPath != null)) {
                    if (hasArchive) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_extract)) },
                            onClick = {
                                onDismiss()
                                appState.extractSelection()
                            },
                            leadingIcon = { Icon(Icons.Default.Unarchive, null) }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_compress)) },
                        onClick = {
                            appState.compressSelection()
                            onDismiss()
                        },
                        leadingIcon = { Icon(Icons.Default.Archive, null) }
                    )
                HorizontalDivider()
                }

                if (!isInRecycleBin) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_cut)) },
                        onClick = {
                            appState.cutSelection()
                            onDismiss()
                        },
                        leadingIcon = { Icon(Icons.Default.ContentCut, null) },
                        trailingIcon = { CtrlShortcut("X") }
                    )
                }

                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_copy)) },
                    onClick = {
                        appState.copySelection()
                        onDismiss()
                    },
                    leadingIcon = { Icon(Icons.Default.CopyAll, null) },
                    trailingIcon = { CtrlShortcut("C") }
                )

                if (hasClipboardItems && (!isInVirtualStorage || isInGalleryOrVideos)) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_paste)) },
                        onClick = {
                            appState.paste()
                            onDismiss()
                        },
                        leadingIcon = { Icon(Icons.Default.ContentPaste, null) },
                        trailingIcon = { CtrlShortcut("V") }
                    )
                }

                HorizontalDivider()

                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_select_all)) },
                    onClick = {
                        appState.selectionManager.selectAll()
                        onDismiss()
                    },
                    leadingIcon = { Icon(Icons.Default.SelectAll, null) },
                    trailingIcon = { CtrlShortcut("A") }
                )

                if (!isInRecycleBin) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_rename)) },
                        onClick = {
                            appState.renameSelection()
                            onDismiss()
                        },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null) },
                        trailingIcon = {
                            Text(
                                text = "F2",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
                if (!hasDirectories) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_share)) },
                        onClick = {
                            shareFiles(context, appState.scope, selectedItems)
                            onDismiss()
                        },
                        leadingIcon = { Icon(Icons.Default.Share, null) }
                    )
                }

                if (!isInRecycleBin) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_delete)) },
                        onClick = {
                            appState.deleteSelection()
                            onDismiss()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        trailingIcon = {
                            Text(
                                text = "DEL",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_restore)) },
                        onClick = {
                            appState.restoreSelection()
                            onDismiss()
                        },
                        leadingIcon = { Icon(Icons.Default.Restore, null) }
                    )

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_delete_permanently)) },
                        onClick = {
                            appState.deleteSelection(forcePermanent = true)
                            onDismiss()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, null) }
                    )
                }

                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_properties)) },
                    onClick = {
                        onDismiss()
                        appState.showProperties()
                    },
                    leadingIcon = { Icon(Icons.Default.Info, null) }
                )
            }

            "COLORS" -> {
                val item = selectedItems.first()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.back), color = MaterialTheme.colorScheme.primary) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { currentScreen = "MAIN" }
                )
                HorizontalDivider()

                DropdownMenuItem(
                    text = {
                        Column {
                            Text(stringResource(R.string.menu_select_folder_color), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 8.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThemeFolderColors.defaultOptions.forEach { colorLong ->
                                    val color = androidx.compose.ui.graphics.Color(colorLong)
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(color, CircleShape)
                                            .clickable {
                                                SettingsManager.setFolderColor(context, item.providerId, colorLong)
                                                onDismiss()
                                            }
                                    )
                                }
                                // Reset option
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                        .clickable {
                                            SettingsManager.setFolderColor(context, item.providerId, null)
                                            onDismiss()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    },
                    onClick = {}
                )
            }
        }
    }
}

/**
 * Context menu shown when right-clicking the empty background area of the file explorer.
 */
@Composable
fun BackgroundContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    appState: FileExplorerState,
    onAddStorage: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    var hasClipboardItems by remember { mutableStateOf(false) }

    LaunchedEffect(expanded) {
        if (expanded) {
            hasClipboardItems = clipboard.hasPrimaryClip()
        }
    }

    var currentScreen by remember { mutableStateOf("MAIN") }
    val virtualStorage = listOf(LibraryItem.RecycleBin, LibraryItem.Gallery, LibraryItem.Videos, LibraryItem.Recent, LibraryItem.Documents, LibraryItem.Games)
    val isInVirtualStorage = appState.libraryItem in virtualStorage
    val isInGalleryOrVideos = appState.libraryItem == LibraryItem.Gallery || appState.libraryItem == LibraryItem.Videos
    val isInRecycleBin = appState.libraryItem == LibraryItem.RecycleBin

    LaunchedEffect(expanded) {
        if (!expanded) {
            currentScreen = "MAIN"
        }
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = LocalExtendedColors.current.menuBackground
    ) {
        when (currentScreen) {
            "MAIN" -> {
                if (appState.libraryItem == LibraryItem.Games) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.nav_add_storage)) },
                        leadingIcon = { Icon(Icons.Default.Add, null) },
                        onClick = {
                            onDismiss()
                            if (onAddStorage != null) {
                                appState.isAddingGameShortcut = true
                                onAddStorage()
                            }
                        }
                    )
                    HorizontalDivider()
                }

                if (!isInVirtualStorage || isInGalleryOrVideos) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_new)) },
                        leadingIcon = { Icon(Icons.Default.Add, null) },
                        trailingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                        onClick = { currentScreen = "NEW" }
                    )
                }

                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_sort)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                    trailingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                    onClick = { currentScreen = "SORT" }
                )

                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_view)) },
                    leadingIcon = { Icon(Icons.Default.ViewModule, null) },
                    trailingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                    onClick = { currentScreen = "VIEW" }
                )

                if (appState.libraryItem == LibraryItem.Music || appState.getCurrentStorageKey()?.startsWith("virtual://music") == true) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_music)) },
                        leadingIcon = { Icon(Icons.Default.MusicNote, null) },
                        trailingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                        onClick = { currentScreen = "MUSIC_MANAGER" }
                    )
                }

                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_select_all)) },
                    leadingIcon = { Icon(Icons.Default.SelectAll, null) },
                    onClick = {
                        appState.selectionManager.selectAll()
                        onDismiss()
                    },
                    trailingIcon = { CtrlShortcut("A") }
                )

                if (appState.currentPath != null && isTermuxInstalled(context)) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_open_terminal)) },
                        leadingIcon = { Icon(Icons.Default.Terminal, null) },
                        onClick = {
                            onDismiss()
                            openInTermux(context, appState.currentPath!!.absolutePath)
                        }
                    )
                }

                HorizontalDivider()

                if (isInRecycleBin) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_empty_recycle_bin)) },
                        leadingIcon = { Icon(Icons.Default.DeleteForever, null) },
                        onClick = { appState.emptyRecycleBin() }
                    )
                }

                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_refresh)) },
                    leadingIcon = { Icon(Icons.Default.Refresh, null) },
                    onClick = {
                        appState.refresh()
                        onDismiss()
                    },
                    trailingIcon = {
                        Text(
                            text = "F5",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                if (hasClipboardItems && (!isInVirtualStorage || isInGalleryOrVideos)) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_paste)) },
                        leadingIcon = { Icon(Icons.Default.ContentPaste, null) },
                        onClick = {
                            appState.paste()
                            onDismiss()
                        },
                        trailingIcon = { CtrlShortcut("V") }
                    )
                }
            }

            "NEW" -> {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.back), color = MaterialTheme.colorScheme.primary) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { currentScreen = "MAIN" }
                )
                HorizontalDivider()

                if (appState.currentPath != null || isInGalleryOrVideos) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.folder)) },
                        leadingIcon = { Icon(Icons.Default.Folder, null) },
                        onClick = {
                            appState.createNewFolder()
                            onDismiss()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_text_document)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.InsertDriveFile, null) },
                        onClick = {
                            appState.createNewFile()
                            onDismiss()
                        }
                    )
                }
            }

            "MUSIC_MANAGER" -> {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.back), color = MaterialTheme.colorScheme.primary) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { currentScreen = "MAIN" }
                )
                HorizontalDivider()

                val isFilterEnabled by SettingsManager.isMusicFilterEnabled
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
                        onDismiss()
                        if (!isFilterEnabled) {
                            SettingsManager.setMusicFilterEnabled(context, true)
                            appState.refresh()
                        }
                        appState.isConfiguringMusicFolders = true
                    },
                    leadingIcon = { Icon(Icons.Default.Folder, null) }
                )

                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_create_playlist)) },
                    onClick = {
                        appState.createNewPlaylist()
                        onDismiss()
                    },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) }
                )

                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_sync_list)) },
                    onClick = {
                        onDismiss()
                        appState.scope.launch(Dispatchers.IO) {
                            MusicMetadataManager.sync(appState.context, SettingsManager.musicFolders.value)
                            GlobalEvents.triggerRefresh()
                        }
                    },
                    leadingIcon = { Icon(Icons.Default.Sync, null) }
                )
            }

            "SORT" -> {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.back), color = MaterialTheme.colorScheme.primary) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { currentScreen = "MAIN" }
                )
                HorizontalDivider()

                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_by_name)) },
                    leadingIcon = { Icon(Icons.Default.TextFormat, null) },
                    trailingIcon = { appState.folderConfigs.SortArrow(FileColumnType.NAME) },
                    onClick = {
                        appState.folderConfigs.toggleSort(FileColumnType.NAME, appState.getCurrentStorageKey()) { appState.refresh() }
                        onDismiss()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_by_date)) },
                    leadingIcon = { Icon(Icons.Default.DateRange, null) },
                    trailingIcon = { appState.folderConfigs.SortArrow(FileColumnType.DATE) },
                    onClick = {
                        appState.folderConfigs.toggleSort(FileColumnType.DATE, appState.getCurrentStorageKey()) { appState.refresh() }
                        onDismiss()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_by_type)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                    trailingIcon = { appState.folderConfigs.SortArrow(FileColumnType.TYPE) },
                    onClick = {
                        appState.folderConfigs.toggleSort(FileColumnType.TYPE, appState.getCurrentStorageKey()) { appState.refresh() }
                        onDismiss()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_by_size)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                    trailingIcon = { appState.folderConfigs.SortArrow(FileColumnType.SIZE) },
                    onClick = {
                        appState.folderConfigs.toggleSort(FileColumnType.SIZE, appState.getCurrentStorageKey()) { appState.refresh() }
                        onDismiss()
                    }
                )
                if (isInRecycleBin) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_by_date_deleted)) },
                        leadingIcon = { Icon(Icons.Default.DateRange, null) },
                        trailingIcon = { appState.folderConfigs.SortArrow(FileColumnType.DATE_DELETED) },
                        onClick = {
                            appState.folderConfigs.toggleSort(FileColumnType.DATE_DELETED, appState.getCurrentStorageKey()) { appState.refresh() }
                            onDismiss()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_by_location)) },
                        leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                        trailingIcon = { appState.folderConfigs.SortArrow(FileColumnType.DELETED_FROM) },
                        onClick = {
                            appState.folderConfigs.toggleSort(FileColumnType.DELETED_FROM, appState.getCurrentStorageKey()) { appState.refresh() }
                            onDismiss()
                        }
                    )
                }
            }

            "VIEW" -> {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.back), color = MaterialTheme.colorScheme.primary) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { currentScreen = "MAIN" }
                )
                HorizontalDivider()

                val storageKey = appState.getCurrentStorageKey() ?: ""
                val isMusicRoot = storageKey == "virtual://music"
                val isMusicSub = storageKey.startsWith("virtual://music/") || 
                                 storageKey.startsWith("virtual://music_album:") || 
                                 storageKey.startsWith("virtual://playlist:")
                
                if (isMusicRoot) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_grid)) },
                        leadingIcon = { Icon(Icons.Default.ViewModule, null) },
                        trailingIcon = { if (appState.activeViewMode == ViewMode.GRID) { Icon(Icons.Default.Done, null) } },
                        onClick = {
                            appState.folderConfigs.updateViewMode(ViewMode.GRID, storageKey)
                            onDismiss()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_music)) },
                        leadingIcon = { Icon(Icons.Default.MusicNote, null) },
                        trailingIcon = { if (appState.activeViewMode == ViewMode.MUSIC) { Icon(Icons.Default.Done, null) } },
                        onClick = {
                            appState.folderConfigs.updateViewMode(ViewMode.MUSIC, storageKey)
                            onDismiss()
                        }
                    )
                } else {
                    if (!isMusicSub) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_details)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ListAlt, null) },
                            trailingIcon = { if (appState.activeViewMode == ViewMode.DETAILS) { Icon(Icons.Default.Done, null) } },
                            onClick = {
                                appState.folderConfigs.updateViewMode(ViewMode.DETAILS, storageKey)
                                onDismiss()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_grid)) },
                        leadingIcon = { Icon(Icons.Default.ViewModule, null) },
                        trailingIcon = { if (appState.activeViewMode == ViewMode.GRID) { Icon(Icons.Default.Done, null) } },
                        onClick = {
                            appState.folderConfigs.updateViewMode(ViewMode.GRID, storageKey)
                            onDismiss()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_gallery)) },
                        leadingIcon = { Icon(Icons.Default.PhotoLibrary, null) },
                        trailingIcon = { if (appState.activeViewMode == ViewMode.GALLERY) { Icon(Icons.Default.Done, null) } },
                        onClick = {
                            appState.folderConfigs.updateViewMode(ViewMode.GALLERY, storageKey)
                            onDismiss()
                        }
                    )
                    if (!isMusicSub) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_content)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                            trailingIcon = { if (appState.activeViewMode == ViewMode.CONTENT) { Icon(Icons.Default.Done, null) } },
                            onClick = {
                                appState.folderConfigs.updateViewMode(ViewMode.CONTENT, storageKey)
                                onDismiss()
                            }
                        )
                    }
                    if (isMusicSub) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_music)) },
                            leadingIcon = { Icon(Icons.Default.MusicNote, null) },
                            trailingIcon = { if (appState.activeViewMode == ViewMode.MUSIC) { Icon(Icons.Default.Done, null) } },
                            onClick = {
                                appState.folderConfigs.updateViewMode(ViewMode.MUSIC, storageKey)
                                onDismiss()
                            }
                        )
                    }
                }

                HorizontalDivider()

                val showHidden = SettingsManager.showHiddenFiles.value
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.settings_show_hidden_files)) },
                    leadingIcon = { Icon(if (showHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) },
                    trailingIcon = {
                        Checkbox(
                            checked = showHidden,
                            onCheckedChange = null
                        )
                    },
                    onClick = {
                        SettingsManager.setShowHiddenFiles(context, !showHidden)
                    }
                )
            }
        }
    }
}
