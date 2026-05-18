package com.troikoss.continuum_explorer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.troikoss.continuum_explorer.ui.components.HorizontalScrollbar
import com.troikoss.continuum_explorer.ui.components.VerticalScrollbar
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.troikoss.continuum_explorer.model.FileColumnType
import com.troikoss.continuum_explorer.model.ScreenSize
import com.troikoss.continuum_explorer.model.UniversalFile
import com.troikoss.continuum_explorer.model.ViewMode
import com.troikoss.continuum_explorer.ui.components.BackgroundContextMenu
import com.troikoss.continuum_explorer.ui.components.DetailsHeader
import com.troikoss.continuum_explorer.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The primary view for displaying files.
 * Handles layout (Grid vs List), selection marquee, and auto-scrolling.
 */
@Composable
fun FileContentRO(appState: FileExplorerState) {
    val selectionManager = appState.selectionManager
    val focusRequester = remember { FocusRequester() }
    val density = LocalDensity.current
    val viewMode = appState.activeViewMode

    // --- State Management ---
    val itemPositions = remember { androidx.compose.runtime.snapshots.SnapshotStateMap<UniversalFile, Rect>() }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }
    val marquee = rememberMarquee()
    var containerCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var mousePosition by remember { mutableStateOf<Offset?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }
    var showSpinner by remember { mutableStateOf(false) }
    var gridContainerTopPx by remember { mutableFloatStateOf(0f) }
    var gridContainerHeightPx by remember { mutableFloatStateOf(0f) }
    val headerOffsetPx = gridContainerTopPx - (containerCoordinates?.positionInRoot()?.y ?: 0f)
    val horizontalPaddingPx = with(density) { if (viewMode == ViewMode.DETAILS) 16.dp.toPx() else 32.dp.toPx() }

    // Compute the intrinsic width of a details row (icon + spacer + name + columns + padding)
    // so hover is only triggered when the mouse is over the actual content, not blank space to the right.
    // Uses remember with no key so the State object is stable — all reads inside are state-tracked via
    // mutableStateMapOf, so any column resize triggers an automatic recompute.
    val detailsContentWidthPx = remember {
        derivedStateOf {
            if (appState.folderConfigs.viewMode != ViewMode.DETAILS) Float.MAX_VALUE
            else with(density) {
                val nameWidth = appState.folderConfigs.columnWidths.getOrElse(FileColumnType.NAME) { 200.dp }
                val columnsWidth = appState.folderConfigs.visibleColumns.fold(0.dp) { acc, col ->
                    val colWidth = appState.folderConfigs.columnWidths[col.type] ?: col.minWidth
                    acc + 1.dp + colWidth
                }
                (52.dp + nameWidth + columnsWidth).toPx()  // 52 = 8(pad) + 24(icon) + 12(spacer) + 8(pad)
            }
        }
    }

    // Recreate grid state when the path changes to keep scroll fresh
    val gridState = key(appState.loadedPathKey) {
        rememberLazyGridState(initialFirstVisibleItemIndex = appState.scrollToItemIndex ?: 0)
    }

    // Dynamic column count based on view mode and actual grid layout
    val columnCount by remember(gridState, viewMode, containerCoordinates?.size?.width, appState.folderConfigs.gridItemSize, density) {
        derivedStateOf {
            if (viewMode == ViewMode.DETAILS || viewMode == ViewMode.CONTENT) {
                1
            } else {
                val visibleItems = gridState.layoutInfo.visibleItemsInfo
                if (visibleItems.isNotEmpty()) {
                    // Extract exact columns generated by Compose Adaptive Grid
                    visibleItems.count { it.offset.y == visibleItems.first().offset.y }.coerceAtLeast(1)
                } else {
                    // Fallback to mathematical estimation before first layout pass
                    val width = containerCoordinates?.size?.width ?: 0
                    if (width > 0) {
                        val totalPaddingPx = with(density) { 64.dp.toPx() } // 32.dp * 2 padding
                        val gridItemSizePx = with(density) { appState.folderConfigs.gridItemSize.dp.toPx() }
                        val availableGridWidthPx = (width - totalPaddingPx).coerceAtLeast(0f)
                        (availableGridWidthPx / gridItemSizePx).toInt().coerceAtLeast(1)
                    } else 1
                }
            }
        }
    }

    // --- Side Effects ---

    // Focus management
    LaunchedEffect(appState.isSearchUIActive) {
        if (!appState.isSearchUIActive) {
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    // Position tracking cleanup
    LaunchedEffect(selectionManager) {
        itemPositions.clear()
    }

    // Scroll initialization cleanup
    LaunchedEffect(gridState) {
        if (appState.scrollToItemIndex != null) {
            appState.onScrollToItemCompleted()
        }
    }

    // Focused item auto-scroll (keyboard navigation)
    LaunchedEffect(selectionManager.leadItem) {
        val leadFile = selectionManager.leadItem ?: return@LaunchedEffect
        val index = selectionManager.allFiles.indexOf(leadFile)
        if (index != -1) {
            val layoutInfo = gridState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@LaunchedEffect

            val itemInfo = visibleItems.find { it.index == index }
            val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset

            if (itemInfo != null) {
                val itemTop = itemInfo.offset.y
                val itemHeight = itemInfo.size.height
                val itemBottom = itemTop + itemHeight

                if (itemTop < layoutInfo.viewportStartOffset) {
                    gridState.animateScrollToItem(index, 0)
                } else if (itemBottom > layoutInfo.viewportEndOffset) {
                    gridState.animateScrollToItem(index, -(viewportHeight - itemHeight))
                }
            } else {
                val firstVisibleIndex = visibleItems.first().index
                if (index < firstVisibleIndex) {
                    gridState.animateScrollToItem(index, 0)
                } else {
                    val estimatedHeight = visibleItems.lastOrNull()?.size?.height ?: 100
                    gridState.animateScrollToItem(index, -(viewportHeight - estimatedHeight))
                }
            }
        }
    }

    // Debounced loading spinner
    LaunchedEffect(appState.isLoading) {
        if (appState.isLoading) {
            delay(400)
            showSpinner = true
        } else {
            showSpinner = false
        }
    }

    // System drag auto-scroll
    LaunchedEffect(appState.activeDragY.value != null) {
        if (appState.activeDragY.value == null) return@LaunchedEffect
        val threshold = 120f
        val maxSpeed = 60f
        while (true) {
            val dragY = appState.activeDragY.value ?: break
            val relY = dragY - gridContainerTopPx
            val h = gridContainerHeightPx
            val delta = when {
                relY < threshold && relY > 0f ->
                    -(maxSpeed * ((threshold - relY) / threshold).coerceIn(0f, 1f))
                h > 0f && relY > h - threshold ->
                    maxSpeed * ((relY - (h - threshold)) / threshold).coerceIn(0f, 1f)
                else -> 0f
            }
            if (delta != 0f) gridState.scrollBy(delta)
            delay(16L)
        }
    }

    // Marquee auto-scroller
    MarqueeAutoScroller(
        dragStart = dragStart,
        dragEnd = dragEnd,
        containerCoordinates = containerCoordinates,
        gridState = gridState,
        onDragStartChange = { dragStart = it },
        onSelectionChange = { start, end ->
            marquee.updateSelection(
                start = start,
                end = end,
                gridState = gridState,
                allFiles = appState.files,
                columnCount = columnCount,
                xOffset = horizontalPaddingPx,
                yOffset = headerOffsetPx,
                overrideWidth = if (viewMode == ViewMode.DETAILS) detailsContentWidthPx.value else null,
                onSelectionChange = { selectionManager.updateSelectionFromDrag(it) }
            )
        }
    )

    // Re-evaluate selection strictly when the physical layout fully settles after asynchronous autoscroll
    LaunchedEffect(dragStart, dragEnd, gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset) {
        if (dragStart != null && dragEnd != null) {
            marquee.updateSelection(
                start = dragStart,
                end = dragEnd,
                gridState = gridState,
                allFiles = appState.files,
                columnCount = columnCount,
                xOffset = horizontalPaddingPx,
                yOffset = headerOffsetPx,
                overrideWidth = if (viewMode == ViewMode.DETAILS) detailsContentWidthPx.value else null,
                onSelectionChange = { selectionManager.updateSelectionFromDrag(it) }
            )
        }
    }

    val fileListShape = RoundedCornerShape(16.dp)

    // --- UI Layout ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then( if (appState.getScreenSize() != ScreenSize.SMALL) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, fileListShape).clip(fileListShape).background(MaterialTheme.colorScheme.surfaceContainerLowest) else Modifier)
            .onGloballyPositioned { containerCoordinates = it }
            .containerGestures(
                selectionManager = selectionManager,
                focusRequester = focusRequester,
                viewMode = viewMode,
                columns = columnCount,
                onZoom = { factor ->
                    when (viewMode) {
                        ViewMode.GRID, ViewMode.GALLERY -> {
                            val newSize = (appState.folderConfigs.gridItemSize * factor).toInt()
                            appState.folderConfigs.updateGridSize(
                                newSize.coerceIn(60, 300),
                                appState.getCurrentStorageKey()
                            )
                        }
                        ViewMode.DETAILS -> {
                            val newSize = (appState.folderConfigs.detailsItemSize * factor).toInt()
                            appState.folderConfigs.updateDetailsSize(
                                newSize.coerceIn(16, 64),
                                appState.getCurrentStorageKey()
                            )
                        }
                        ViewMode.CONTENT -> {
                            val newSize = (appState.folderConfigs.contentItemSize * factor).toInt()
                            appState.folderConfigs.updateContentSize(
                                newSize.coerceIn(32, 120),
                                appState.getCurrentStorageKey()
                            )
                        }
                    }
                },
                onDragStart = { offset ->
                    dragStart = offset
                    selectionManager.clear(true)
                },
                onDrag = { offset ->
                    dragEnd = offset
                    marquee.updateSelection(
                        start = dragStart,
                        end = dragEnd,
                        gridState = gridState,
                        allFiles = appState.files,
                        columnCount = columnCount,
                        xOffset = horizontalPaddingPx,
                        yOffset = headerOffsetPx,
                        overrideWidth = if (viewMode == ViewMode.DETAILS) detailsContentWidthPx.value else null,
                        onSelectionChange = { selectionManager.updateSelectionFromDrag(it) }
                    )
                },
                onDragEnd = { dragStart = null; dragEnd = null },
                mousePosition = { mousePosition = it },
                appState = appState,
                gridState = gridState
            )
            .contextMenuDetector(enableLongPress = true, aggressive = false) { offset ->
                val isOverItem = itemPositions.values.any { rect -> rect.contains(offset) }
                if (!isOverItem) {
                    menuOffset = with(density) { DpOffset(offset.x.toDp(), offset.y.toDp()) }
                    showMenu = true
                }
            }
    ) {
        if (showSpinner) {
            LoadingOverlay()
        }

        val networkErr = appState.networkError
        if (networkErr != null) {
            NetworkErrorBanner(message = networkErr, onRetry = { appState.triggerLoad(forceRefresh = true) })
        }

        FileLayout(
            appState = appState,
            gridState = gridState,
            itemPositions = itemPositions,
            containerCoordinates = containerCoordinates,
            mousePosition = { mousePosition },
            focusRequester = focusRequester,
            dragStart = dragStart,
            dragEnd = dragEnd,
            marquee = marquee,
            detailsContentWidthPx = detailsContentWidthPx.value,
            onGridPositioned = { top, height ->
                gridContainerTopPx = top
                gridContainerHeightPx = height
            }
        )

        // Floating context menu
        Box(modifier = Modifier.offset(menuOffset.x, menuOffset.y)) {
            BackgroundContextMenu(
                expanded = showMenu,
                onDismiss = { showMenu = false },
                appState = appState
            )
        }
    }
}

@Composable
private fun NetworkErrorBanner(message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.errorContainer, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun LoadingOverlay() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun FileLayout(
    appState: FileExplorerState,
    gridState: LazyGridState,
    itemPositions: MutableMap<UniversalFile, Rect>,
    containerCoordinates: LayoutCoordinates?,
    mousePosition: () -> Offset?,
    focusRequester: FocusRequester,
    dragStart: Offset?,
    dragEnd: Offset?,
    marquee: Marquee,
    detailsContentWidthPx: Float,
    onGridPositioned: (Float, Float) -> Unit
) {
    val viewMode = appState.activeViewMode
    val hScrollState = rememberScrollState()


    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val edgeThresholdPx = with(density) { 48.dp.toPx() }
    var contentBoxSize by remember { mutableStateOf(IntSize.Zero) }
    var contentLocalMousePos by remember { mutableStateOf<Offset?>(null) }

    val showVertical by remember { derivedStateOf {
        val pos = contentLocalMousePos ?: return@derivedStateOf false
        pos.x > contentBoxSize.width - edgeThresholdPx
    } }
    val showHorizontal by remember { derivedStateOf {
        val pos = contentLocalMousePos ?: return@derivedStateOf false
        pos.y > contentBoxSize.height - edgeThresholdPx
    } }
    var isHScrollActive by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        var headerHeightPx by remember { mutableFloatStateOf(0f) }

        if (viewMode == ViewMode.DETAILS) {
            Box(modifier = Modifier
                .padding(horizontal = 16.dp)
                .onGloballyPositioned { headerHeightPx = it.size.height.toFloat() }
            ) {
                DetailsHeader(appState = appState, scrollState = hScrollState)
            }
        } else {
            headerHeightPx = 0f
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .clipToBounds()
                .onSizeChanged { contentBoxSize = it }
                .onGloballyPositioned { coords ->
                    onGridPositioned(coords.positionInRoot().y, coords.size.height.toFloat())
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            when (event.type) {
                                PointerEventType.Move, PointerEventType.Enter ->
                                    contentLocalMousePos = event.changes.firstOrNull()?.position
                                PointerEventType.Exit -> contentLocalMousePos = null
                                else -> {}
                            }
                        }
                    }
                }
                .pointerInput(hScrollState, viewMode) {
                    if (viewMode != ViewMode.DETAILS) return@pointerInput
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.type == PointerEventType.Scroll && event.keyboardModifiers.isShiftPressed) {
                                val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                coroutineScope.launch { hScrollState.scrollBy(delta * 120f) }
                                coroutineScope.launch {
                                    isHScrollActive = true
                                    delay(800)
                                    isHScrollActive = false
                                }
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
        ) {
            FileGrid(
                appState = appState,
                gridState = gridState,
                itemPositions = itemPositions,
                containerCoordinates = containerCoordinates,
                mousePosition = mousePosition,
                focusRequester = focusRequester,
                dragActive = dragStart != null,
                hScrollState = hScrollState,
                detailsContentWidthPx = detailsContentWidthPx
            )

            MarqueeRenderer(
                dragStart = dragStart?.let { Offset(it.x, it.y - headerHeightPx) },
                dragEnd = dragEnd?.let { Offset(it.x, it.y - headerHeightPx) },
                marquee = marquee
            )

            VerticalScrollbar(
                gridState = gridState,
                isNearEdge = showVertical,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)
            )

            if (viewMode == ViewMode.DETAILS) {
                HorizontalScrollbar(
                    scrollState = hScrollState,
                    isNearEdge = showHorizontal,
                    isRecentlyScrolled = isHScrollActive,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun FileGrid(
    appState: FileExplorerState,
    gridState: LazyGridState,
    itemPositions: MutableMap<UniversalFile, Rect>,
    containerCoordinates: LayoutCoordinates?,
    mousePosition: () -> Offset?,
    focusRequester: FocusRequester,
    dragActive: Boolean,
    hScrollState: ScrollState? = null,
    detailsContentWidthPx: Float
) {
    val viewMode = appState.activeViewMode

    LazyVerticalGrid(
        state = gridState,
        columns = when (viewMode) {
            ViewMode.GRID, ViewMode.GALLERY -> GridCells.Adaptive(minSize = appState.folderConfigs.gridItemSize.dp)
            else -> GridCells.Fixed(1)
        },
        modifier = if (viewMode == ViewMode.DETAILS) Modifier.fillMaxSize().padding(horizontal = 16.dp)
                   else Modifier.fillMaxSize().padding(horizontal = 32.dp),
        contentPadding = if (viewMode == ViewMode.DETAILS) PaddingValues(0.dp)
                         else PaddingValues(16.dp)
    ) {
        items(
            items = appState.files,
            key = { it.absolutePath },
            contentType = { file ->
                when {
                    file.isDirectory -> "folder"
                    file.isArchiveEntry -> "archive_entry"
                    else -> "file"
                }
            }
        ) { file ->

            val isHovered by remember(file, dragActive) {
                derivedStateOf {
                    // Suppress hover during any drag operation
                    if (appState.activeDragY.value != null || appState.isSystemDragActive.value || dragActive) {
                        false
                    } else {
                        val currentRect = itemPositions[file]
                        val pos = mousePosition()
                        if (pos == null || currentRect == null || !currentRect.contains(pos)) false
                        else if (viewMode == ViewMode.DETAILS) (pos.x - currentRect.left) < detailsContentWidthPx
                        else true
                    }
                }
            }

            FileView(
                file = file,
                itemPositions = itemPositions,
                containerCoordinates = containerCoordinates,
                mousePosition = mousePosition,
                appState = appState,
                focusRequester = focusRequester,
                isHovered = isHovered,
                hScrollState = hScrollState
            )
        }
    }
}
