package com.troikoss.continuum_explorer.ui.components

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.text.format.Formatter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.documentfile.provider.DocumentFile
import com.troikoss.continuum_explorer.R
import com.troikoss.continuum_explorer.model.NavSection
import com.troikoss.continuum_explorer.model.NetworkConnection
import com.troikoss.continuum_explorer.model.NetworkProtocol
import com.troikoss.continuum_explorer.providers.StorageProviders
import com.troikoss.continuum_explorer.utils.FileExplorerState
import com.troikoss.continuum_explorer.managers.SettingsManager
import com.troikoss.continuum_explorer.managers.IconTheme
import com.troikoss.continuum_explorer.managers.ThemeShape
import com.troikoss.continuum_explorer.ui.theme.LocalExtendedColors
import com.troikoss.continuum_explorer.utils.contextMenuDetector
import com.troikoss.continuum_explorer.utils.emptyRecycleBin
import com.troikoss.continuum_explorer.utils.fileDropTarget
import com.troikoss.continuum_explorer.utils.navigateTo
import com.troikoss.continuum_explorer.utils.openInNewWindow
import com.troikoss.continuum_explorer.utils.showProperties
import com.troikoss.continuum_explorer.utils.toUniversal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt
/**
 * Data class to hold storage information
 */
/*private */data class StorageVolumeInfo(
    val label: String,
    val path: File?,
    val uri: Uri?,
    val totalSpace: Long,
    val freeSpace: Long,
    val icon: ImageVector,
    val section: NavSection,
    val customIcon: Int? = null
)

private val MINIMIZED_SIDEBAR_WIDTH = 160.dp

/**
 * Extension to apply a horizontal fading edge to text to prevent jitter from ellipsis
 * and provide a smooth "slide to invisible" effect.
 */
private fun Modifier.fadingEdge(alpha: Float = 1f): Modifier = this
    .graphicsLayer {
        this.alpha = alpha
        this.compositingStrategy = CompositingStrategy.Offscreen
    }
    .drawWithContent {
        drawContent()
        val fadeWidth = 32.dp.toPx()
        if (size.width > 0) {
            val stop = ((size.width - fadeWidth) / size.width).coerceIn(0f, 1f)
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Black,
                    stop to Color.Black,
                    1f to Color.Transparent
                ),
                blendMode = BlendMode.DstIn
            )
        }
    }

/**
 * A revamped navigation sidebar with section headers and organized locations.
 */
@Composable
fun NavigationPane(
    appState: FileExplorerState,
    onItemSelected: (NavSection) -> Unit,
    onSafItemSelected: (Uri) -> Unit,
    onAddStorageClick: () -> Unit,
    onAddNetworkClick: () -> Unit = {},
    onEditNetworkClick: (NetworkConnection) -> Unit = {},
    onNavigate: () -> Unit = {},
    currentWidth: Dp = appState.appConfigs.navPaneWidth
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val isMinimized = currentWidth < MINIMIZED_SIDEBAR_WIDTH

    val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
    val isRecycleBinEnabled by SettingsManager.isRecycleBinEnabled

    // Gather all storage volumes
    val storageVolumes = remember(context) {
        val volumes = mutableListOf<StorageVolumeInfo>()

        // Add Internal Storage
        val internalRoot = Environment.getExternalStorageDirectory()
        volumes.add(
            StorageVolumeInfo(
                label = resources.getString(R.string.nav_internal_storage),
                path = internalRoot,
                uri = null,
                totalSpace = internalRoot.totalSpace,
                freeSpace = internalRoot.usableSpace,
                icon = Icons.Default.Storage,
                section = NavSection.InternalStorage,
                customIcon = R.drawable.ic_storage
            )
        )

        // Add Removable volumes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val externalDirs = context.getExternalFilesDirs(null)
            storageManager.storageVolumes.forEachIndexed { index, volume ->
                if (volume.isRemovable) {
                    val description = volume.getDescription(context) ?: ""
                    val isSdCard = description.contains("SD", ignoreCase = true)
                    
                    val directory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        volume.directory
                    } else {
                        // Guess directory on Android 10/11 using getExternalFilesDirs hack
                        externalDirs.find { it != null && !Environment.isExternalStorageEmulated(it) && it.absolutePath.contains(volume.uuid ?: "") }
                    }
                    
                    if (directory != null) {
                        volumes.add(
                            StorageVolumeInfo(
                                label = description,
                                path = directory,
                                uri = null,
                                totalSpace = directory.totalSpace,
                                freeSpace = directory.usableSpace,
                                icon = if (isSdCard) Icons.Default.SdCard else Icons.Default.Usb,
                                section = NavSection.RemovableVolume(index),
                                customIcon = R.drawable.ic_storage
                            )
                        )
                    }
                }
            }
        }
        volumes
    }

    // Context menu for the background
    var showBgMenu by remember { mutableStateOf(false) }
    var bgMenuOffset by remember { mutableStateOf(DpOffset.Zero) }
    val density = LocalDensity.current

    val lazyListState = rememberLazyListState()

    // Reordering state
    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var draggingOffset by remember { mutableFloatStateOf(0f) }
    val handleWidthPx = with(density) { 48.dp.toPx() }

    // Calculate visible library items directly without 'remember' based on list reference
    val visibleLibraryItems = appState.appConfigs.libraryOrder.filter { id ->
        when (id) {
            "trash" -> isRecycleBinEnabled
            "recent" -> appState.appConfigs.isRecentVisible
            "gallery" -> appState.appConfigs.isGalleryVisible
            "downloads" -> appState.appConfigs.isDownloadsVisible
            "documents" -> appState.appConfigs.isDocumentsVisible
            else -> true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .contextMenuDetector(enableLongPress = true, aggressive = false) { offset ->
                    bgMenuOffset = with(density) { DpOffset(offset.x.toDp(), offset.y.toDp()) }
                    showBgMenu = true
                }
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Section: Favorites
            item { NavSectionHeader(currentWidth, stringResource(R.string.nav_favorites), isMinimized = isMinimized) }

            if (appState.appConfigs.favoritePaths.isEmpty() && !isMinimized) {
                item {
                    Text(
                        text = stringResource(R.string.nav_no_favorites),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                itemsIndexed(
                    items = appState.appConfigs.favoritePaths,
                    key = { _, path -> path }
                ) { _, path ->
                    val file = File(path)
                    val isDragging = draggedItemId == path
                    val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "FavoriteElevation")

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isDragging) Modifier else Modifier.animateItem())
                            .zIndex(if (isDragging) 1f else 0f)
                            .offset {
                                IntOffset(
                                    0,
                                    if (isDragging) draggingOffset.roundToInt() else 0
                                )
                            }
                            .shadow(elevation)
                            .background(if (isDragging) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                            .pointerInput(path) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)

                                    // REORDER LOGIC:
                                    // - Mouse: Drag from anywhere on the item
                                    // - Touch: Drag only from the icon area (handleWidthPx)
                                    if (down.type == PointerType.Mouse || down.position.x <= handleWidthPx) {
                                        val pointerId = down.id
                                        var triggerDrag = false
                                        var distance = 0f

                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val move =
                                                event.changes.firstOrNull { it.id == pointerId }
                                                    ?: break
                                            if (!move.pressed) break
                                            distance += (move.position - down.position).getDistance()
                                            if (distance > 5f) {
                                                triggerDrag = true
                                                break
                                            }
                                        }

                                        if (triggerDrag) {
                                            draggedItemId = path
                                            draggingOffset = 0f

                                            drag(down.id) { change ->
                                                val changeOffset = change.positionChange()
                                                draggingOffset += changeOffset.y

                                                val currentIndex =
                                                    appState.appConfigs.favoritePaths.indexOf(path)
                                                if (currentIndex != -1) {
                                                    val itemHeight = 40.dp.toPx()
                                                    val threshold = itemHeight * 0.6f

                                                    if (draggingOffset > threshold && currentIndex < appState.appConfigs.favoritePaths.size - 1) {
                                                        appState.appConfigs.moveFavorite(
                                                            currentIndex,
                                                            currentIndex + 1
                                                        )
                                                        draggingOffset -= itemHeight
                                                    } else if (draggingOffset < -threshold && currentIndex > 0) {
                                                        appState.appConfigs.moveFavorite(
                                                            currentIndex,
                                                            currentIndex - 1
                                                        )
                                                        draggingOffset += itemHeight
                                                    }
                                                }
                                                change.consume()
                                            }
                                            draggedItemId = null
                                            draggingOffset = 0f
                                        }
                                    }
                                }
                            }
                            .fileDropTarget(appState, destPath = file),
                        contentAlignment = if (isMinimized) Alignment.Center else Alignment.TopStart
                    ) {
                        NavFavoriteItem(
                            label = file.name,
                            path = path,
                            onClick = { appState.navigateTo(file, null); onNavigate() },
                            onRemove = { appState.appConfigs.removeFavorite(path) },
                            appState = appState,
                            currentWidth = currentWidth,
                            isMinimized = isMinimized
                        )
                    }
                }
            }

            if (visibleLibraryItems.isNotEmpty()) {
                if (!isMinimized) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        NavSectionHeader(currentWidth, stringResource(R.string.nav_library), isMinimized = false)
                    }
                }

                itemsIndexed(
                    items = visibleLibraryItems,
                    key = { _, id -> id }
                ) { _, id ->
                    val isDragging = draggedItemId == id
                    val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "LibraryElevation")

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isDragging) Modifier else Modifier.animateItem())
                            .zIndex(if (isDragging) 1f else 0f)
                            .offset {
                                IntOffset(
                                    0,
                                    if (isDragging) draggingOffset.roundToInt() else 0
                                )
                            }
                            .shadow(elevation)
                            .background(if (isDragging) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                            .pointerInput(id) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)

                                    // REORDER LOGIC:
                                    // - Mouse: Drag from anywhere on the item
                                    // - Touch: Drag only from the icon area (handleWidthPx)
                                    if (down.type == PointerType.Mouse || down.position.x <= handleWidthPx) {
                                        val pointerId = down.id
                                        var triggerDrag = false
                                        var distance = 0f

                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val move =
                                                event.changes.firstOrNull { it.id == pointerId }
                                                    ?: break
                                            if (!move.pressed) break
                                            distance += (move.position - down.position).getDistance()
                                            if (distance > 5f) {
                                                triggerDrag = true
                                                break
                                            }
                                        }

                                        if (triggerDrag) {
                                            draggedItemId = id
                                            draggingOffset = 0f

                                            drag(down.id) { change ->
                                                val changeOffset = change.positionChange()
                                                draggingOffset += changeOffset.y

                                                val currentIndex =
                                                    appState.appConfigs.libraryOrder.indexOf(id)
                                                if (currentIndex != -1) {
                                                    val itemHeight = 40.dp.toPx()
                                                    val threshold = itemHeight * 0.6f

                                                    if (draggingOffset > threshold && currentIndex < appState.appConfigs.libraryOrder.size - 1) {
                                                        appState.appConfigs.moveLibraryItem(
                                                            currentIndex,
                                                            currentIndex + 1
                                                        )
                                                        draggingOffset -= itemHeight
                                                    } else if (draggingOffset < -threshold && currentIndex > 0) {
                                                        appState.appConfigs.moveLibraryItem(
                                                            currentIndex,
                                                            currentIndex - 1
                                                        )
                                                        draggingOffset += itemHeight
                                                    }
                                                }
                                                change.consume()
                                            }
                                            draggedItemId = null
                                            draggingOffset = 0f
                                        }
                                    }
                                }
                            },
                        contentAlignment = if (isMinimized) Alignment.Center else Alignment.TopStart
                    ) {
                        when (id) {
                            "gallery" -> NavItem(
                                label = stringResource(R.string.nav_gallery),
                                icon = Icons.Default.Image,
                                customIcon = R.drawable.ic_nav_gallery,
                                onClick = { onItemSelected(NavSection.Gallery) },
                                appState = appState,
                                currentWidth = currentWidth,
                                section = NavSection.Gallery,
                                isMinimized = isMinimized
                            )
                            "recent" -> NavItem(
                                label = stringResource(R.string.nav_recent),
                                icon = Icons.Default.History,
                                customIcon = R.drawable.ic_nav_recent,
                                onClick = { onItemSelected(NavSection.Recent) },
                                appState = appState,
                                currentWidth = currentWidth,
                                section = NavSection.Recent,
                                isMinimized = isMinimized
                            )
                            "trash" -> {
                                val trashDir = File(Environment.getExternalStorageDirectory(), ".Trash")
                                NavItem(
                                    label = stringResource(R.string.nav_trash),
                                    icon = Icons.Default.Delete,
                                    customIcon = R.drawable.ic_nav_trash,
                                    onClick = { onItemSelected(NavSection.RecycleBin) },
                                    modifier = Modifier.fileDropTarget(appState, destPath = trashDir),
                                    appState = appState,
                                    currentWidth = currentWidth,
                                    section = NavSection.RecycleBin,
                                    isMinimized = isMinimized
                                )
                            }
                            "downloads" -> NavItem(
                                label = stringResource(R.string.nav_downloads),
                                icon = Icons.Default.FileDownload,
                                customIcon = R.drawable.ic_nav_downloads,
                                onClick = { onItemSelected(NavSection.Downloads) },
                                appState = appState,
                                currentWidth = currentWidth,
                                section = NavSection.Downloads,
                                isMinimized = isMinimized
                            )
                            "documents" -> NavItem(
                                label = stringResource(R.string.nav_documents),
                                icon = Icons.AutoMirrored.Filled.List,
                                customIcon = R.drawable.ic_nav_documents,
                                onClick = { onItemSelected(NavSection.Documents) },
                                appState = appState,
                                currentWidth = currentWidth,
                                section = NavSection.Documents,
                                isMinimized = isMinimized
                            )
                        }
                    }
                }
            }

            if (!isMinimized) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Section: Storage
            if (!isMinimized) {
                item { NavSectionHeader(currentWidth, stringResource(R.string.nav_storage), isMinimized = false) }
            }
            
            itemsIndexed(storageVolumes) { _, volume ->
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (isMinimized) Alignment.Center else Alignment.TopStart) {
                    NavStorageItem(
                        label = volume.label,
                        icon = volume.icon,
                        totalSpace = volume.totalSpace,
                        freeSpace = volume.freeSpace,
                        onClick = { onItemSelected(volume.section) },
                        modifier = Modifier.fileDropTarget(appState, destPath = volume.path),
                        appState = appState,
                        currentWidth = currentWidth,
                        path = volume.path,
                        onNavigate = onNavigate,
                        customIcon = volume.customIcon,
                        isMinimized = isMinimized
                    )
                }
            }

            // Section: Added Locations (SAF)
            if (appState.appConfigs.addedSafUris.isNotEmpty()) {
                if (!isMinimized) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        NavSectionHeader(currentWidth, stringResource(R.string.nav_added_locations), isMinimized = false)
                    }
                }

                itemsIndexed(
                    items = appState.appConfigs.addedSafUris,
                    key = { _, uri -> uri.toString() }
                ) { _, uri ->
                    val label = appState.getSafDisplayName(uri)
                    val uriKey = uri.toString()
                    val isDragging = draggedItemId == uriKey
                    val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "SafElevation")

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isDragging) Modifier else Modifier.animateItem())
                            .zIndex(if (isDragging) 1f else 0f)
                            .offset { IntOffset(0, if (isDragging) draggingOffset.roundToInt() else 0) }
                            .shadow(elevation)
                            .background(if (isDragging) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                            .pointerInput(uriKey) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    if (down.type == PointerType.Mouse || down.position.x <= handleWidthPx) {
                                        val pointerId = down.id
                                        var triggerDrag = false
                                        var distance = 0f
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val move = event.changes.firstOrNull { it.id == pointerId } ?: break
                                            if (!move.pressed) break
                                            distance += (move.position - down.position).getDistance()
                                            if (distance > 5f) { triggerDrag = true; break }
                                        }
                                        if (triggerDrag) {
                                            draggedItemId = uriKey
                                            draggingOffset = 0f
                                            drag(down.id) { change ->
                                                draggingOffset += change.positionChange().y
                                                val currentIndex = appState.appConfigs.addedSafUris.indexOfFirst { it.toString() == uriKey }
                                                if (currentIndex != -1) {
                                                    val itemHeight = 40.dp.toPx()
                                                    val threshold = itemHeight * 0.6f
                                                    if (draggingOffset > threshold && currentIndex < appState.appConfigs.addedSafUris.size - 1) {
                                                        appState.appConfigs.moveSafUri(currentIndex, currentIndex + 1)
                                                        draggingOffset -= itemHeight
                                                    } else if (draggingOffset < -threshold && currentIndex > 0) {
                                                        appState.appConfigs.moveSafUri(currentIndex, currentIndex - 1)
                                                        draggingOffset += itemHeight
                                                    }
                                                }
                                                change.consume()
                                            }
                                            draggedItemId = null
                                            draggingOffset = 0f
                                        }
                                    }
                                }
                            },
                        contentAlignment = if (isMinimized) Alignment.Center else Alignment.TopStart
                    ) {
                        NavSafItem(
                            label = label,
                            uri = uri,
                            onClick = { onSafItemSelected(uri) },
                            onRemove = { appState.removeSafUri(uri) },
                            modifier = Modifier.fileDropTarget(appState, destSafUri = uri),
                            appState = appState,
                            currentWidth = currentWidth,
                            isMinimized = isMinimized
                        )
                    }
                }
            }

            // Section: Network
            if (appState.appConfigs.networkConnections.isNotEmpty()) {
                if (!isMinimized) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        NavSectionHeader(currentWidth, stringResource(R.string.nav_network), isMinimized = false)
                    }
                }

                itemsIndexed(
                    items = appState.appConfigs.networkConnections,
                    key = { _, conn -> conn.id }
                ) { _, connection ->
                    val isDragging = draggedItemId == connection.id
                    val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "NetworkElevation")

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isDragging) Modifier else Modifier.animateItem())
                            .zIndex(if (isDragging) 1f else 0f)
                            .offset { IntOffset(0, if (isDragging) draggingOffset.roundToInt() else 0) }
                            .shadow(elevation)
                            .background(if (isDragging) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                            .pointerInput(connection.id) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    if (down.type == PointerType.Mouse || down.position.x <= handleWidthPx) {
                                        val pointerId = down.id
                                        var triggerDrag = false
                                        var distance = 0f
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val move = event.changes.firstOrNull { it.id == pointerId } ?: break
                                            if (!move.pressed) break
                                            distance += (move.position - down.position).getDistance()
                                            if (distance > 5f) { triggerDrag = true; break }
                                        }
                                        if (triggerDrag) {
                                            draggedItemId = connection.id
                                            draggingOffset = 0f
                                            drag(down.id) { change ->
                                                draggingOffset += change.positionChange().y
                                                val currentIndex = appState.appConfigs.networkConnections.indexOfFirst { it.id == connection.id }
                                                if (currentIndex != -1) {
                                                    val itemHeight = 40.dp.toPx()
                                                    val threshold = itemHeight * 0.6f
                                                    if (draggingOffset > threshold && currentIndex < appState.appConfigs.networkConnections.size - 1) {
                                                        appState.appConfigs.moveNetworkConnection(currentIndex, currentIndex + 1)
                                                        draggingOffset -= itemHeight
                                                    } else if (draggingOffset < -threshold && currentIndex > 0) {
                                                        appState.appConfigs.moveNetworkConnection(currentIndex, currentIndex - 1)
                                                        draggingOffset += itemHeight
                                                    }
                                                }
                                                change.consume()
                                            }
                                            draggedItemId = null
                                            draggingOffset = 0f
                                        }
                                    }
                                }
                            },
                        contentAlignment = if (isMinimized) Alignment.Center else Alignment.TopStart
                    ) {
                        NavNetworkItem(
                            connection = connection,
                            onClick = { onItemSelected(NavSection.NetworkStorage(connection.id)) },
                            onRemove = { appState.appConfigs.removeNetworkConnection(connection.id) },
                            onEdit = { onEditNetworkClick(connection) },
                            appState = appState,
                            currentWidth = currentWidth,
                            isMinimized = isMinimized
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        // Background context menu
        Box(modifier = Modifier.offset { IntOffset(bgMenuOffset.x.roundToPx(), bgMenuOffset.y.roundToPx()) }) {
            NavBackgroundContextMenu(
                expanded = showBgMenu,
                onDismissRequest = { showBgMenu = false },
                onAddStorageClick = onAddStorageClick,
                onAddNetworkClick = onAddNetworkClick
            )
        }
    }
}

@Composable
private fun NavBackgroundContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onAddStorageClick: () -> Unit,
    onAddNetworkClick: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(16.dp),
        containerColor = LocalExtendedColors.current.menuBackground,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.nav_add_storage)) },
            onClick = {
                onDismissRequest()
                onAddStorageClick()
            },
            leadingIcon = { Icon(Icons.Default.Add, null) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.nav_add_network_storage)) },
            onClick = {
                onDismissRequest()
                onAddNetworkClick()
            },
            leadingIcon = { Icon(Icons.Default.Cloud, null) }
        )
    }
}

@Composable
private fun NavSectionHeader(currentWidth: Dp, text: String, isMinimized: Boolean = false) {
    val textAlpha = if (isMinimized) 0f else ((currentWidth - 160.dp) / 40.dp).coerceIn(0f, 1f)
    if (textAlpha <= 0f) return
    Text(
        text = text.uppercase(),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .fadingEdge(textAlpha),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun NavContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    appState: FileExplorerState,
    path: String? = null,
    uri: Uri? = null,
    section: NavSection? = null,
    onRemove: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onNavigate: (() -> Unit)? = null
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(16.dp),
        containerColor = LocalExtendedColors.current.menuBackground,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        if (onNavigate != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_open)) },
                onClick = {
                    onDismissRequest()
                    onNavigate()
                },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } // Using back arrow as a placeholder for "Open"
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_open_new_window)) },
                onClick = {
                    onDismissRequest()
                    when {
                        section is NavSection.RecycleBin -> {
                            val trashDir = File(Environment.getExternalStorageDirectory(), ".Trash")
                            appState.openInNewWindow(listOf(trashDir.toUniversal()))
                        }
                        path != null -> appState.openInNewWindow(listOf(File(path).toUniversal()))
                        uri != null -> {
                            DocumentFile.fromTreeUri(appState.context, uri)?.let {
                                appState.openInNewWindow(listOf(it.toUniversal()))
                            }
                        }
                        else -> appState.openInNewWindow(emptyList())
                    }
                },
                leadingIcon = { Icon(Icons.Default.Tab, null) }
            )
        }

        if (onEdit != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.nav_network_edit)) },
                onClick = {
                    onDismissRequest()
                    onEdit()
                },
                leadingIcon = { Icon(Icons.Default.Edit, null) }
            )
        }

        if (onRemove != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_remove)) },
                onClick = {
                    onDismissRequest()
                    onRemove()
                },
                leadingIcon = { Icon(Icons.Default.Delete, null) }
            )
        }

        if (section is NavSection.RecycleBin) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_empty_recycle_bin)) },
                onClick = {
                    onDismissRequest()
                    appState.emptyRecycleBin()
                },
                leadingIcon = { Icon(Icons.Default.DeleteForever, null) }
            )
        }

        if (section is NavSection.Documents) {
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Show Folders")
                        Spacer(Modifier.weight(1f))
                    }
                },
                onClick = {
                    onDismissRequest()
                    appState.appConfigs.isDocumentsFolderEnabled = !appState.appConfigs.isDocumentsFolderEnabled
                },
                leadingIcon = { if (appState.appConfigs.isDocumentsFolderEnabled) Icon(Icons.Default.Check, null) }
            )
        }
        if (section !is NavSection.Recent && section !is NavSection.Gallery && section !is NavSection.NetworkStorage) {
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_properties)) },
                onClick = {
                    onDismissRequest()
                    when (section) {
                        is NavSection.RecycleBin -> {
                            val trashDir = File(Environment.getExternalStorageDirectory(), ".Trash")
                            appState.showProperties(listOf(trashDir.toUniversal()))
                        }
                        else -> if (path != null) {
                            appState.showProperties(listOf(File(path).toUniversal()))
                        } else if (uri != null) {
                            val doc = DocumentFile.fromTreeUri(appState.context, uri)
                            if (doc != null) appState.showProperties(listOf(doc.toUniversal()))
                        }
                    }
                },
                leadingIcon = { Icon(Icons.Default.Info, null) }
            )
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    appState: FileExplorerState,
    currentWidth: Dp,
    modifier: Modifier = Modifier,
    section: NavSection? = null,
    customIcon: Int? = null,
    isMinimized: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero)}
    val density = LocalDensity.current

    val themeBar = SettingsManager.themeBar.value
    val shape = if (isMinimized) {
        if (themeBar == ThemeShape.SQUARE) RectangleShape else CircleShape
    } else {
        if (themeBar == ThemeShape.SQUARE) RectangleShape else RoundedCornerShape(18.dp)
    }
    val itemPadding = if (isMinimized) PaddingValues(horizontal = 0.dp) else if (themeBar == ThemeShape.SQUARE) PaddingValues(horizontal = 8.dp) else NavigationDrawerItemDefaults.ItemPadding
    val textAlpha = if (isMinimized) 0f else ((currentWidth - 160.dp) / 40.dp).coerceIn(0f, 1f)

    val iconContent = @Composable {
        val extendedColors = LocalExtendedColors.current
        val iconTheme = SettingsManager.iconTheme.value
        val tint = when (section) {
            is NavSection.Gallery -> extendedColors.galleryIcon
            is NavSection.Recent -> extendedColors.recentIcon
            is NavSection.Downloads -> extendedColors.downloadsIcon
            is NavSection.RecycleBin -> extendedColors.recycleBinIcon
            is NavSection.Documents -> extendedColors.documentsIcon
            else -> extendedColors.sidebarIcons
        }
        if (customIcon != null && (iconTheme == IconTheme.COLOURFUL || iconTheme == IconTheme.COLOURFULDUO)) {
            val finalIcon = if (iconTheme == IconTheme.COLOURFULDUO) {
                when (customIcon) {
                    R.drawable.ic_nav_gallery -> R.drawable.ic_nav_gallery_duo
                    R.drawable.ic_nav_recent -> R.drawable.ic_nav_recent_duo
                    R.drawable.ic_nav_downloads -> R.drawable.ic_nav_downloads_duo
                    R.drawable.ic_nav_documents -> R.drawable.ic_nav_documents_duo
                    R.drawable.ic_nav_trash -> R.drawable.ic_nav_trash_duo
                    else -> customIcon
                }
            } else customIcon
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = finalIcon),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(icon, contentDescription = null, tint = tint)
        }
    }

    Box(
        modifier = modifier
            .padding(itemPadding)
            .then(if (isMinimized) Modifier.size(40.dp) else Modifier.fillMaxWidth().height(36.dp))
            .clip(shape)
            .then(if (isMinimized) Modifier.clickable { onClick() } else Modifier)
            .contextMenuDetector(enableLongPress = true, aggressive = true) { offset ->
                menuOffset = with(density) { DpOffset(offset.x.toDp(), offset.y.toDp()) }
                expanded = true
            },
        contentAlignment = Alignment.Center
    ) {
        if (isMinimized) {
            iconContent()
        } else {
            NavigationDrawerItem(
                label = { Text(label, fontWeight = FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.fillMaxWidth().fadingEdge(textAlpha)) },
                selected = false,
                onClick = onClick,
                icon = iconContent,
                modifier = Modifier.height(36.dp),
                shape = shape
            )
        }

        Box(modifier = Modifier.offset { IntOffset(menuOffset.x.roundToPx(), menuOffset.y.roundToPx()) }) {
            NavContextMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                appState = appState,
                section = section,
                onNavigate = onClick
            )
        }
    }
}

/**
 * Item for Favorite locations with a removal context menu.
 */
@Composable
private fun NavFavoriteItem(
    label: String,
    path: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    appState: FileExplorerState,
    currentWidth: Dp,
    modifier: Modifier = Modifier,
    isMinimized: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero)}
    val density = LocalDensity.current

    val themeBar = SettingsManager.themeBar.value
    val shape = if (isMinimized) {
        if (themeBar == ThemeShape.SQUARE) RectangleShape else CircleShape
    } else {
        if (themeBar == ThemeShape.SQUARE) RectangleShape else RoundedCornerShape(18.dp)
    }
    val itemPadding = if (isMinimized) PaddingValues(horizontal = 0.dp) else if (themeBar == ThemeShape.SQUARE) PaddingValues(horizontal = 8.dp) else NavigationDrawerItemDefaults.ItemPadding
    val textAlpha = if (isMinimized) 0f else ((currentWidth - 160.dp) / 40.dp).coerceIn(0f, 1f)

    val iconContent = @Composable {
        val iconTheme = SettingsManager.iconTheme.value
        if (iconTheme == IconTheme.COLOURFUL || iconTheme == IconTheme.COLOURFULDUO) {
            val drawableId = if (iconTheme == IconTheme.COLOURFULDUO) R.drawable.ic_folder_duo else R.drawable.ic_folder
            Icon(painter = androidx.compose.ui.res.painterResource(id = drawableId), contentDescription = null, tint = LocalExtendedColors.current.folderIcon, modifier = Modifier.size(24.dp))
        } else {
            Icon(Icons.Default.Folder, contentDescription = null, tint = LocalExtendedColors.current.folderIcon)
        }
    }

    Box(
        modifier = modifier
            .padding(itemPadding)
            .then(if (isMinimized) Modifier.size(40.dp) else Modifier.fillMaxWidth().height(36.dp))
            .clip(shape)
            .then(if (isMinimized) Modifier.clickable { onClick() } else Modifier)
            .contextMenuDetector(enableLongPress = true, aggressive = true) { offset ->
                menuOffset = with(density) { DpOffset(offset.x.toDp(), offset.y.toDp()) }
                expanded = true
            },
        contentAlignment = Alignment.Center
    ) {
        if (isMinimized) {
            iconContent()
        } else {
            NavigationDrawerItem(
                label = { Text(label, fontWeight = FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.fillMaxWidth().fadingEdge(textAlpha)) },
                selected = false,
                onClick = onClick,
                icon = iconContent,
                shape = shape,
                modifier = Modifier.height(36.dp)
            )
        }

        Box(modifier = Modifier.offset { IntOffset(menuOffset.x.roundToPx(), menuOffset.y.roundToPx()) }) {
            NavContextMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                appState = appState,
                path = path,
                onRemove = onRemove,
                onNavigate = onClick
            )
        }
    }
}

/**
 * Item for SAF-added locations with a removal context menu.
 */
@Composable
private fun NavSafItem(
    label: String,
    uri: Uri,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    appState: FileExplorerState,
    currentWidth: Dp,
    modifier: Modifier = Modifier,
    isMinimized: Boolean = false
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero)}
    val density = LocalDensity.current

    var diskInfo by remember(uri) { mutableStateOf<Pair<Long, Long>?>(null) }
    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            diskInfo = com.troikoss.continuum_explorer.providers.SafProvider.getDiskInfo(uri)
        }
    }

    val themeBar = SettingsManager.themeBar.value
    val shape = if (isMinimized) {
        if (themeBar == ThemeShape.SQUARE) RectangleShape else CircleShape
    } else {
        if (themeBar == ThemeShape.SQUARE) RectangleShape else RoundedCornerShape(18.dp)
    }
    val itemPadding = if (isMinimized) PaddingValues(horizontal = 0.dp) else if (themeBar == ThemeShape.SQUARE) PaddingValues(horizontal = 8.dp) else NavigationDrawerItemDefaults.ItemPadding
    val textAlpha = if (isMinimized) 0f else ((currentWidth - 160.dp) / 40.dp).coerceIn(0f, 1f)

    val iconContent = @Composable {
        Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = LocalExtendedColors.current.sidebarIcons)
    }

    Box(
        modifier = modifier
            .padding(itemPadding)
            .then(if (isMinimized) Modifier.size(40.dp) else Modifier.fillMaxWidth())
            .clip(shape)
            .then(if (isMinimized) Modifier.clickable { onClick() } else Modifier)
            .contextMenuDetector(enableLongPress = true, aggressive = true) { offset ->
                menuOffset = with(density) { DpOffset(offset.x.toDp(), offset.y.toDp()) }
                expanded = true
            },
        contentAlignment = Alignment.Center
    ) {
        if (isMinimized) {
            iconContent()
        } else {
            NavigationDrawerItem(
                label = {
                    if (diskInfo != null) {
                        val (totalSpace, freeSpace) = diskInfo!!
                        val usedSpace = totalSpace - freeSpace
                        val progress = if (totalSpace > 0L) usedSpace.toFloat() / totalSpace.toFloat() else 0f
                        val totalFormatted = Formatter.formatFileSize(context, totalSpace)
                        val freeFormatted = Formatter.formatFileSize(context, freeSpace)
                        Column(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth().fadingEdge(textAlpha)) {
                            Text(
                                text = label,
                                fontWeight = FontWeight.Normal,
                                maxLines = 2,
                                overflow = TextOverflow.Clip,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .height(4.dp),
                                color = if (progress > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                strokeCap = StrokeCap.Round,
                                gapSize = 0.dp,
                                drawStopIndicator = {}
                            )
                            Text(
                                text = stringResource(R.string.nav_storage_usage_label, freeFormatted, totalFormatted),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Clip
                            )
                        }
                    } else {
                        Text(label, fontWeight = FontWeight.Normal, maxLines = 2, overflow = TextOverflow.Clip, modifier = Modifier.fillMaxWidth().fadingEdge(textAlpha))
                    }
                },
                selected = false,
                onClick = onClick,
                icon = iconContent,
                shape = shape,
                modifier = Modifier.then(if (diskInfo == null) Modifier.height(36.dp) else Modifier)
            )
        }

        Box(modifier = Modifier.offset { IntOffset(menuOffset.x.roundToPx(), menuOffset.y.roundToPx()) }) {
            NavContextMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                appState = appState,
                uri = uri,
                onRemove = onRemove,
                onNavigate = onClick
            )
        }
    }
}

@Composable
private fun NavNetworkItem(
    connection: NetworkConnection,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onEdit: () -> Unit,
    appState: FileExplorerState,
    currentWidth: Dp,
    isMinimized: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }
    val density = LocalDensity.current
    val context = LocalContext.current

    val icon = when (connection.protocol) {
        NetworkProtocol.FTP -> Icons.Default.Lan
        NetworkProtocol.SFTP -> Icons.Default.Lan
        NetworkProtocol.WEBDAV -> Icons.Default.Lan
        NetworkProtocol.SMB -> Icons.Default.Lan
    }

    // Fetch disk info for SMB connections only
    var diskInfo by remember(connection.id) { mutableStateOf<Pair<Long, Long>?>(null) }
    val isSmb = connection.protocol == NetworkProtocol.SMB
    LaunchedEffect(connection.id) {
        if (isSmb) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val provider = StorageProviders.network(connection)
                    provider.getDiskInfo()
                }.getOrNull()?.let { diskInfo = it }
            }
        }
    }

    val themeBar = SettingsManager.themeBar.value
    val shape = if (isMinimized) {
        if (themeBar == ThemeShape.SQUARE) RectangleShape else CircleShape
    } else {
        if (themeBar == ThemeShape.SQUARE) RectangleShape else RoundedCornerShape(18.dp)
    }
    val itemPadding = if (isMinimized) PaddingValues(horizontal = 0.dp) else if (themeBar == ThemeShape.SQUARE) PaddingValues(horizontal = 8.dp) else NavigationDrawerItemDefaults.ItemPadding
    val textAlpha = if (isMinimized) 0f else ((currentWidth - 160.dp) / 40.dp).coerceIn(0f, 1f)

    val iconContent = @Composable {
        val iconTheme = SettingsManager.iconTheme.value
        if (iconTheme == IconTheme.COLOURFUL || iconTheme == IconTheme.COLOURFULDUO) {
            val drawableId = if (iconTheme == IconTheme.COLOURFULDUO) R.drawable.ic_network_duo else R.drawable.ic_network
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = drawableId),
                contentDescription = null,
                tint = LocalExtendedColors.current.sidebarIcons,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(icon, contentDescription = null, tint = LocalExtendedColors.current.sidebarIcons)
        }
    }

    Box(
        modifier = Modifier
            .padding(itemPadding)
            .then(if (isMinimized) Modifier.size(40.dp) else Modifier.fillMaxWidth())
            .clip(shape)
            .then(if (isMinimized) Modifier.clickable { onClick() } else Modifier)
            .contextMenuDetector(enableLongPress = true, aggressive = true) { offset ->
                menuOffset = with(density) { DpOffset(offset.x.toDp(), offset.y.toDp()) }
                expanded = true
            },
        contentAlignment = Alignment.Center
    ) {
        if (isMinimized) {
            iconContent()
        } else {
            NavigationDrawerItem(
                label = {
                    if (isSmb && diskInfo != null) {
                        val (totalSpace, freeSpace) = diskInfo!!
                        val usedSpace = totalSpace - freeSpace
                        val progress = if (totalSpace > 0L) usedSpace.toFloat() / totalSpace.toFloat() else 0f
                        val totalFormatted = Formatter.formatFileSize(context, totalSpace)
                        val freeFormatted = Formatter.formatFileSize(context, freeSpace)
                        Column(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth().fadingEdge(textAlpha)) {
                            Text(
                                text = connection.displayName,
                                fontWeight = FontWeight.Normal,
                                maxLines = 2,
                                overflow = TextOverflow.Clip,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .height(4.dp),
                                color = if (progress > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                strokeCap = StrokeCap.Round,
                                gapSize = 0.dp,
                                drawStopIndicator = {}
                            )
                            Text(
                                text = stringResource(R.string.nav_storage_usage_label, freeFormatted, totalFormatted),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Clip
                            )
                        }
                    } else {
                        Text(connection.displayName, fontWeight = FontWeight.Normal, maxLines = 2, overflow = TextOverflow.Clip, modifier = Modifier.fillMaxWidth().fadingEdge(textAlpha))
                    }
                },
                selected = false,
                onClick = onClick,
                icon = iconContent,
                shape = shape,
                modifier = Modifier.then(if (!isSmb || diskInfo == null) Modifier.height(36.dp) else Modifier)
            )
        }

        Box(modifier = Modifier.offset { IntOffset(menuOffset.x.roundToPx(), menuOffset.y.roundToPx()) }) {
            NavContextMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                appState = appState,
                section = NavSection.NetworkStorage(connection.id),
                onRemove = onRemove,
                onEdit = onEdit,
                onNavigate = onClick
            )
        }
    }
}

/**
 * Custom navigation item for Storage that shows a progress bar and usage details.
 */
@Composable
private fun NavStorageItem(
    label: String,
    icon: ImageVector,
    totalSpace: Long,
    freeSpace: Long,
    onClick: () -> Unit,
    appState: FileExplorerState,
    currentWidth: Dp,
    path: File?,
    modifier: Modifier = Modifier,
    onNavigate: () -> Unit = {},
    customIcon: Int? = null,
    isMinimized: Boolean = false
) {
    val context = LocalContext.current
    var expandedMenu by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero)}
    val density = LocalDensity.current

    var expandedTree by remember { mutableStateOf(false) }
    var subDirs by remember { mutableStateOf<List<File>>(emptyList()) }

    LaunchedEffect(expandedTree) {
        if (expandedTree && path != null) {
            withContext(Dispatchers.IO) {
                val dirs = path.listFiles()?.filter { it.isDirectory && !it.isHidden }?.sortedBy { it.name }
                if (dirs != null) {
                    withContext(Dispatchers.Main) {
                        subDirs = dirs
                    }
                }
            }
        }
    }

    val totalFormatted = Formatter.formatFileSize(context, totalSpace)
    val freeFormatted = Formatter.formatFileSize(context, freeSpace)

    val themeBar = SettingsManager.themeBar.value
    val shape = if (isMinimized) {
        if (themeBar == ThemeShape.SQUARE) RectangleShape else CircleShape
    } else {
        if (themeBar == ThemeShape.SQUARE) RectangleShape else RoundedCornerShape(18.dp)
    }
    val itemPadding = if (isMinimized) PaddingValues(horizontal = 0.dp) else if (themeBar == ThemeShape.SQUARE) PaddingValues(horizontal = 8.dp) else NavigationDrawerItemDefaults.ItemPadding
    val textAlpha = if (isMinimized) 0f else ((currentWidth - 160.dp) / 40.dp).coerceIn(0f, 1f)

    val iconContent = @Composable {
        val iconTheme = SettingsManager.iconTheme.value
        if (customIcon != null && (iconTheme == IconTheme.COLOURFUL || iconTheme == IconTheme.COLOURFULDUO)) {
            val finalIcon = if (iconTheme == IconTheme.COLOURFULDUO) {
                when (customIcon) {
                    R.drawable.ic_folder -> R.drawable.ic_folder_duo
                    // Add other mappings if storage icons can vary
                    else -> customIcon
                }
            } else customIcon
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = finalIcon),
                contentDescription = null,
                tint = LocalExtendedColors.current.sidebarIcons,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(icon, contentDescription = null, tint = LocalExtendedColors.current.sidebarIcons)
        }
    }

    Column {
        Box(
            modifier = modifier
                .padding(itemPadding)
                .then(if (isMinimized) Modifier.size(40.dp) else Modifier.fillMaxWidth())
                .clip(shape)
                .then(if (isMinimized) Modifier.clickable { onClick() } else Modifier)
                .contextMenuDetector(enableLongPress = true, aggressive = true) { offset ->
                    menuOffset = with(density) { DpOffset(offset.x.toDp(), offset.y.toDp()) }
                    expandedMenu = true
                },
            contentAlignment = Alignment.Center
        ) {
            if (isMinimized) {
                iconContent()
            } else {
                NavigationDrawerItem(
                    label = {
                        val usedSpace = totalSpace - freeSpace
                        val progress = if (totalSpace > 0L) usedSpace.toFloat() / totalSpace.toFloat() else 0f
                        Column(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth().fadingEdge(textAlpha)) {
                            Text(
                                text = label,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Clip,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .height(4.dp),
                                color = if (progress > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                strokeCap = StrokeCap.Round,
                                gapSize = 0.dp,
                                drawStopIndicator = {}
                            )
                            Text(
                                text = stringResource(R.string.nav_storage_usage_label, freeFormatted, totalFormatted),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Clip
                            )
                        }
                    },
                    selected = false,
                    onClick = onClick,
                    icon = iconContent,
                    badge = {
                        if (path != null) {
                            IconButton(
                                onClick = { expandedTree = !expandedTree },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (expandedTree) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = if (expandedTree) stringResource(R.string.nav_collapse) else stringResource(R.string.nav_expand),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    shape = shape
                )
            }

            Box(modifier = Modifier.offset { IntOffset(menuOffset.x.roundToPx(), menuOffset.y.roundToPx()) }) {
                NavContextMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false },
                    appState = appState,
                    path = path?.absolutePath,
                    onNavigate = onClick
                )
            }
        }

        if (expandedTree && !isMinimized) {
            subDirs.forEach { childFolder ->
                StorageFolderTreeItem(
                    folder = childFolder,
                    level = 1,
                    appState = appState,
                    currentWidth = currentWidth,
                    onNavigate = onNavigate
                )
            }
        }
    }
}

@Composable
private fun StorageFolderTreeItem(
    folder: File,
    level: Int,
    appState: FileExplorerState,
    currentWidth: Dp,
    onNavigate: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var subDirs by remember { mutableStateOf<List<File>>(emptyList()) }
    val textAlpha = ((currentWidth - 160.dp) / 40.dp).coerceIn(0f, 1f)

    LaunchedEffect(expanded) {
        if (expanded) {
            withContext(Dispatchers.IO) {
                val dirs = folder.listFiles()?.filter { it.isDirectory && !it.isHidden }?.sortedBy { it.name }
                if (dirs != null) {
                    withContext(Dispatchers.Main) {
                        subDirs = dirs
                    }
                }
            }
        }
    }

    Column {
        NavigationDrawerItem(
            label = { Text(folder.name, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.fillMaxWidth().fadingEdge(textAlpha)) },
            selected = false,
            onClick = {
                appState.navigateTo(folder, null)
                onNavigate()
            },
            icon = {
                Spacer(modifier = Modifier.width((level * 16).dp))
                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(20.dp), tint = LocalExtendedColors.current.folderIcon)
            },
            badge = {
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            },
            modifier = Modifier.height(32.dp),
            shape = RoundedCornerShape(16.dp)
        )

        if (expanded) {
            subDirs.forEach { child ->
                StorageFolderTreeItem(
                    folder = child,
                    level = level + 1,
                    appState = appState,
                    currentWidth = currentWidth,
                    onNavigate = onNavigate
                )
            }
        }
    }
}
