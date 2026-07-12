package com.troikoss.continuum_explorer.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.ui.window.PopupPositionProvider
import com.troikoss.continuum_explorer.managers.AudioManager
import com.troikoss.continuum_explorer.managers.SettingsManager
import com.troikoss.continuum_explorer.managers.selectionBackground
import com.troikoss.continuum_explorer.model.*
import com.troikoss.continuum_explorer.ui.components.ItemContextMenu
import com.troikoss.continuum_explorer.utils.*
import com.troikoss.continuum_explorer.utils.IconHelper.FileThumbnail
import com.troikoss.continuum_explorer.utils.IconHelper.FolderPreview
import com.troikoss.continuum_explorer.utils.IconHelper.isMimeTypePreviewable
import com.troikoss.continuum_explorer.R


/**
 * Renders a single file or folder item, switching layout based on the current ViewMode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileView(
    file: UniversalFile,
    itemPositions: MutableMap<UniversalFile, Rect>,
    containerCoordinates: LayoutCoordinates?,
    mousePosition: () -> Offset?,
    appState: FileExplorerState,
    focusRequester: FocusRequester,
    isHovered: Boolean,
    hScrollState: ScrollState? = null,
) {
    // Selection
    val selectionManager = appState.selectionManager
    val isSelected = selectionManager.isSelected(file)
    val isLead = selectionManager.leadItem == file

    // Context Menu
    var showMenu by remember { mutableStateOf(value = false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }
    
    val density = LocalDensity.current

    // Lifecycle & Positioning
    DisposableEffect(file) {
        onDispose { itemPositions.remove(file) }
    }


    val mouseTooltipProvider = remember(containerCoordinates) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val mousePos = mousePosition()
                if ((mousePos == null) || (containerCoordinates == null) || !containerCoordinates.isAttached) {
                    return IntOffset(
                        x = anchorBounds.left + ((anchorBounds.width - popupContentSize.width) / 2),
                        y = anchorBounds.bottom,
                    )
                }

                val windowMousePos = containerCoordinates.localToWindow(mousePos)
                return IntOffset(
                    x = windowMousePos.x.toInt() + 15,
                    y = windowMousePos.y.toInt() + 15,
                )
            }
        }
    }

    // Layout Configuration
    val viewMode = appState.activeViewMode
    val isGridView = viewMode == ViewMode.GRID
    val shape = if (isGridView) RoundedCornerShape(8.dp) else RectangleShape
    
    Box(modifier = if (isGridView) Modifier.padding(4.dp) else Modifier) {
            Surface(
                color = Color.Transparent,
                shape = shape,
                modifier = Modifier
                    .then(if (viewMode != ViewMode.DETAILS) Modifier.fillMaxWidth() else Modifier)
                    .fileDragSource(
                        file = file,
                        selectionManager = selectionManager,
                        appState = appState,
                        onShowContextMenu = { offset ->
                            menuOffset = with(density) { DpOffset(offset.x.toDp(), offset.y.toDp()) }
                            showMenu = true
                        }
                    ) {
                        showMenu = false
                    }
                    .then(
                        if (file.isDirectory) {
                            val isRemote = file.provider.capabilities.isRemote
                            Modifier.fileDropTarget(
                                appState = appState,
                                destPath = file.fileRef,
                                destSafUri = file.documentFileRef?.uri,
                                destNetworkProvider = if (isRemote) file.provider else null,
                                destNetworkId = if (isRemote) file.providerId else null,
                            )
                        } else Modifier
                    )
                    .trackPosition(file, itemPositions, containerCoordinates)
                    .itemGestures(
                        file = file,
                        selectionManager = selectionManager,
                        focusRequester = focusRequester,
                        appState = appState
                    )
                    .contextMenuDetector(enableLongPress = false, aggressive = true) { offset ->
                        if (!selectionManager.isSelected(file)) {
                            selectionManager.handleRowClick(
                                file,
                                isShiftPressed = false,
                                isCtrlPressed = false,
                            )
                        }
                        menuOffset = with(density) { DpOffset(offset.x.toDp(), offset.y.toDp()) }
                        showMenu = true
                    }
            ) {
                when (viewMode) {
                    ViewMode.GRID -> FileGridView(file, isSelected, isHovered, isLead, appState, mouseTooltipProvider)
                    ViewMode.GALLERY -> FileGalleryView(file, isSelected, isHovered, isLead, appState, mouseTooltipProvider)
                    ViewMode.CONTENT -> FileContentView(file, isSelected, isHovered, isLead, appState, mouseTooltipProvider)
                    ViewMode.DETAILS -> FileDetailsView(file, isSelected, isHovered, isLead, appState, hScrollState, mouseTooltipProvider)
                    ViewMode.MUSIC -> FileMusicView(file, isSelected, isHovered, isLead, appState, mouseTooltipProvider)
                }
            }

        // Context Menu
        Box(modifier = Modifier.offset { IntOffset(menuOffset.x.roundToPx(), menuOffset.y.roundToPx()) }) {
            ItemContextMenu(
                expanded = showMenu,
                onDismiss = { showMenu = false },
                appState = appState
            )
        }
    }
}

// --- Specific View Components ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileGalleryView(
    file: UniversalFile,
    isSelected: Boolean,
    isHovered: Boolean,
    isLead: Boolean,
    appState: FileExplorerState,
    toolTipProvider: PopupPositionProvider,
) {

    val tooltipState = rememberTooltipState()
    var isOverflowing by remember { mutableStateOf(value = false) }

    val contentSize = (appState.folderConfigs.gridItemSize * 0.6f).dp

    TooltipBox(
        positionProvider = toolTipProvider,
        tooltip = { if (isOverflowing) PlainTooltip { Text(file.name) } },
        state = tooltipState
    ) {
        Box(
            modifier = Modifier.padding(2.dp).fillMaxWidth().aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            FileThumbnail(
                file = file,
                modifier = Modifier.fillMaxSize(),
                selected = isSelected,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                iconSize = contentSize,
                contentScale = ContentScale.Crop
            )

            FolderPreview(
                folder = file,
                thumbSize = contentSize/1.5f,
                modifier = Modifier.align(Alignment.BottomEnd)
            )


            if (!isMimeTypePreviewable(file) || file.isDirectory) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box (modifier = Modifier.background(Color.Black.copy(alpha = 0.5f)).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = file.name,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,

                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.5f)
                    .selectionBackground(isSelected, isHovered, isLead, hoverAlpha = false),
                contentAlignment = Alignment.Center
            ) {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileGridView(
    file: UniversalFile,
    isSelected: Boolean,
    isHovered: Boolean,
    isLead: Boolean,
    appState: FileExplorerState,
    toolTipProvider: PopupPositionProvider,
) {

    val tooltipState = rememberTooltipState()
    var isOverflowing by remember { mutableStateOf(value = false) }

    val shape = RoundedCornerShape(8.dp)

    Column(
        modifier = Modifier
            .padding(8.dp).fillMaxWidth()
            .selectionBackground(isSelected, isHovered, isLead, shape = shape),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val contentSize = (appState.folderConfigs.gridItemSize * 0.6f).dp

        Box {
            FileThumbnail(
                file = file,
                modifier = Modifier.size((appState.folderConfigs.gridItemSize * 0.6f).dp),
                selected = isSelected,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                iconSize = contentSize
            )
            FolderPreview(
                folder = file,
                thumbSize = contentSize/2,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
        TooltipBox(
            positionProvider = toolTipProvider,
            tooltip = { if (isOverflowing) PlainTooltip { Text(file.name) } },
            state = tooltipState
        ) {
            Text(
                text = file.name,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { textLayoutResult ->
                    isOverflowing = textLayoutResult.hasVisualOverflow
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileContentView(
    file: UniversalFile,
    isSelected: Boolean,
    isHovered: Boolean,
    isLead: Boolean,
    appState: FileExplorerState,
    toolTipProvider: PopupPositionProvider,
) {
    val formattedSize = remember(file) { appState.formatSize(file.length) }
    val formattedDate = remember(file) { appState.formatDate(file.lastModified) }
    val iconSelectionEnabled = SettingsManager.iconTouchSelection.value
    val tooltipState = rememberTooltipState()
    var isOverflowing by remember { mutableStateOf(value = false) }
    val shape = RoundedCornerShape(8.dp)
    val itemSize = appState.folderConfigs.contentItemSize.dp

    Column(
        modifier = Modifier.selectionBackground(isSelected, isHovered, isLead, shape = shape)
    ) {
        Row(
            modifier = Modifier
                .padding( start = 1.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box {
                val iconModifier = if (iconSelectionEnabled) {
                    Modifier.size(itemSize).iconTouchToggle(file, appState.selectionManager)
                } else {
                    Modifier.size(itemSize)
                }
                FileThumbnail(
                    file = file,
                    selected = isSelected,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = iconModifier,
                    iconSize = itemSize
                )
                FolderPreview(
                    folder = file,
                    thumbSize = itemSize * 0.6f,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }

            Spacer(Modifier.width(18.dp))

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(itemSize),
                    contentAlignment = Alignment.CenterStart
                ) {
                    TooltipBox(
                        positionProvider = toolTipProvider,
                        tooltip = { if (isOverflowing) PlainTooltip { Text(file.name) } },
                        state = tooltipState
                    ) {
                        Text(
                            text = file.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { textLayoutResult ->
                                isOverflowing = textLayoutResult.hasVisualOverflow
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().offset(y = (-8).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    if (!file.isDirectory) {
                        Text(
                            text = formattedSize,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
        HorizontalDivider()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileMusicView(
    file: UniversalFile,
    isSelected: Boolean,
    isHovered: Boolean,
    isLead: Boolean,
    appState: FileExplorerState,
    toolTipProvider: PopupPositionProvider,
) {
    val formattedSize = remember(file) { appState.formatSize(file.length) }
    val formattedDate = remember(file) { appState.formatDate(file.lastModified) }
    val tooltipState = rememberTooltipState()
    var isOverflowing by remember { mutableStateOf(value = false) }
    val shape = RoundedCornerShape(12.dp)
    val itemSize = 40.dp
    
    val isCurrentTrack = AudioManager.currentTrack?.providerId == file.providerId
    val isPlaying = isCurrentTrack && AudioManager.isAudioPlaying

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .selectionBackground(isSelected, isHovered, isLead, shape = shape),
        shape = shape,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isCurrentTrack) {
                IconButton(
                    onClick = { AudioManager.togglePlayPause() },
                    modifier = Modifier.size(itemSize).padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Box {
                FileThumbnail(
                    file = file,
                    selected = isSelected,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(itemSize),
                    iconSize = itemSize,
                    contentScale = ContentScale.Crop
                )
                FolderPreview(
                    folder = file,
                    thumbSize = itemSize * 0.6f,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                TooltipBox(
                    positionProvider = toolTipProvider,
                    tooltip = { if (isOverflowing) PlainTooltip { Text(file.name) } },
                    state = tooltipState
                ) {
                    Text(
                        text = file.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { textLayoutResult ->
                            isOverflowing = textLayoutResult.hasVisualOverflow
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Row {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    if (!file.isDirectory) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = formattedSize,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(onClick = { appState.addToPlaylist(file) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = "Add to Playlist",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FileDetailsView(
    file: UniversalFile,
    isSelected: Boolean,
    isHovered: Boolean,
    isLead: Boolean,
    appState: FileExplorerState,
    hScrollState: ScrollState?,
    toolTipProvider: PopupPositionProvider,
) {
    val context = LocalContext.current
    val resources = LocalResources.current

    val iconSelectionEnabled = SettingsManager.iconTouchSelection.value

    val nameColumnWidth = appState.folderConfigs.columnWidths.getOrElse(FileColumnType.NAME) {Dp.Unspecified}

    val tooltipState = rememberTooltipState()
    var isOverflowing by remember { mutableStateOf(value = false) }

    val shape = RoundedCornerShape(8.dp)
    val itemSize = appState.folderConfigs.detailsItemSize.dp

    val isCurrentTrack = AudioManager.currentTrack?.providerId == file.providerId
    val isPlaying = isCurrentTrack && AudioManager.isAudioPlaying

    CompositionLocalProvider(LocalOverscrollFactory provides null) {
        Column(
            modifier = Modifier
                .then(if (hScrollState != null) Modifier.horizontalScroll(hScrollState) else Modifier)
        ) {
            Row(
                modifier = Modifier
                    .selectionBackground(isSelected, isHovered, isLead, shape = shape)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCurrentTrack) {
                    IconButton(
                        onClick = { AudioManager.togglePlayPause() },
                        modifier = Modifier.size(itemSize + 8.dp).padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(itemSize)
                        )
                    }
                }

                Box{
                    val iconModifier = if (iconSelectionEnabled) {
                        Modifier.size(itemSize).iconTouchToggle(file, appState.selectionManager)
                    } else {
                        Modifier.size(itemSize)
                    }
                    FileThumbnail(
                        file = file,
                        modifier = iconModifier,
                        selected = isSelected,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        iconSize = itemSize
                    )
                    FolderPreview(
                        folder = file,
                        thumbSize = itemSize * 0.65f,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
                Spacer(Modifier.width(12.dp))
                TooltipBox(
                    positionProvider = toolTipProvider,
                    tooltip = { if (isOverflowing) PlainTooltip { Text(file.name) } },
                    state = tooltipState
                ) {
                    Text(
                        text = file.name,
                        modifier = Modifier.width(nameColumnWidth),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { textLayoutResult ->
                            isOverflowing = textLayoutResult.hasVisualOverflow
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                appState.folderConfigs.visibleColumns.forEach { column ->
                    val width = appState.folderConfigs.columnWidths[column.type] ?: column.minWidth
                    val uuidKey = file.fileRef?.parentFile?.name
                    val meta = uuidKey?.let { appState.recycleBinMetadata[it] }
                    val text = when (column.type) {
                        FileColumnType.DATE -> remember(file) { appState.formatDate(file.lastModified) }
                        FileColumnType.SIZE -> if (file.isDirectory) remember(file) { file.fileRef?.listFiles()?.size?.let { n -> if (n == 1) resources.getString(R.string.details_item_count_singular) else resources.getString(R.string.details_item_count_plural, n) } ?: "--" } else remember(file) { appState.formatSize(file.length) }
                        FileColumnType.TYPE -> getFileType(file, context)
                        FileColumnType.DATE_DELETED -> meta?.deletedAt?.let { appState.formatDate(it) } ?: "--"
                        FileColumnType.DELETED_FROM -> meta?.deletedFrom ?: "--"
                        else -> ""
                    }

                    Spacer(Modifier.width(1.dp))
                    Text(
                        text = text,
                        modifier = Modifier.width(width).padding(start = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            }
            HorizontalDivider()
        }
    }
}
