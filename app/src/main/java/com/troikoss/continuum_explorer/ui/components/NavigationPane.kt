package com.troikoss.continuum_explorer.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.text.format.Formatter
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.troikoss.continuum_explorer.R
import com.troikoss.continuum_explorer.model.NavSection
import com.troikoss.continuum_explorer.model.NetworkConnection
import com.troikoss.continuum_explorer.model.NetworkProtocol
import com.troikoss.continuum_explorer.providers.StorageProviders
import com.troikoss.continuum_explorer.utils.FileExplorerState
import com.troikoss.continuum_explorer.utils.GlobalEvents
import com.troikoss.continuum_explorer.managers.IconTheme
import com.troikoss.continuum_explorer.managers.MusicMetadataManager
import com.troikoss.continuum_explorer.managers.SettingsManager
import com.troikoss.continuum_explorer.managers.ThemeShape
import com.troikoss.continuum_explorer.ui.theme.LocalExtendedColors
import com.troikoss.continuum_explorer.utils.IconHelper
import com.troikoss.continuum_explorer.utils.contextMenuDetector
import com.troikoss.continuum_explorer.utils.createNewPlaylist
import com.troikoss.continuum_explorer.utils.emptyRecycleBin
import com.troikoss.continuum_explorer.utils.fadingEdge
import com.troikoss.continuum_explorer.utils.fileDropTarget
import com.troikoss.continuum_explorer.utils.horizontalResizeHandle
import com.troikoss.continuum_explorer.utils.navigateTo
import com.troikoss.continuum_explorer.utils.openInNewWindow
import com.troikoss.continuum_explorer.utils.showProperties
import com.troikoss.continuum_explorer.utils.toUniversal
import kotlinx.coroutines.Dispatchers
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
 * Optimized to avoid expensive offscreen compositing where possible.
 */
private fun Modifier.horizontalFadingEdge(alphaProvider: () -> Float): Modifier = this
    .graphicsLayer {
        val a = alphaProvider()
        this.alpha = a
        // Avoid CompositingStrategy.Offscreen as it is a major performance bottleneck during animations/resizing.
        // graphicsLayer already creates a layer when alpha < 1f.
    }
    .drawWithContent {
        drawContent()
        val alpha = alphaProvider()
        // Only apply the fading blend if we are in the "transition" zone (partially transparent)
        if (alpha > 0f && alpha < 1f) {
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
    modifier: Modifier = Modifier,
    onAddNetworkClick: () -> Unit = {},
    onEditNetworkClick: (NetworkConnection) -> Unit = {},
    onNavigate: () -> Unit = {},
    widthProvider: () -> Dp,
    isInWindowMode: Boolean = false,
    onResize: ((Dp) -> Unit)? = null,
    onResizeStarted: (() -> Unit)? = null,
    onResizeFinished: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    
    // Optimized: Only recompose when the minimized state actually changes
    val isMinimized by remember { derivedStateOf { widthProvider() < MINIMIZED_SIDEBAR_WIDTH } }
    val textAlphaProvider = remember {
        derivedStateOf {
            val w = widthProvider()
            if (w < MINIMIZED_SIDEBAR_WIDTH) 0f else ((w - 160.dp) / 40.dp).coerceIn(0f, 1f)
        }
    }

    // Common divider with horizontal resizing capability
    @Composable
    fun SidebarDivider(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(8.dp) // Larger hit area for easier "on divider" resizing
                .horizontalResizeHandle(onResize, onResizeStarted, onResizeFinished),
            contentAlignment = Alignment.Center
        ) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
        }
    }

    val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
    val isRecycleBinEnabled by SettingsManager.isRecycleBinEnabled

    val isFavoritesExpanded by SettingsManager.isFavoritesExpanded
    val isLibraryExpanded by SettingsManager.isLibraryExpanded
    val isStorageExpanded by SettingsManager.isStorageExpanded
    val isAddedLocationsExpanded by SettingsManager.isAddedLocationsExpanded
    val isNetworkExpanded by SettingsManager.isNetworkExpanded

    fun getStorageVolumes(): List<StorageVolumeInfo> {
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
        return volumes
    }

    val scope = rememberCoroutineScope()
    var storageVolumes by remember { mutableStateOf<List<StorageVolumeInfo>>(emptyList()) }

    fun refreshVolumes() {
        scope.launch(Dispatchers.IO) {
            val volumes = getStorageVolumes()
            withContext(Dispatchers.Main) {
                storageVolumes = volumes
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshVolumes()
        GlobalEvents.refreshEvent.collect {
            refreshVolumes()
        }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                refreshVolumes()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MEDIA_MOUNTED)
            addAction(Intent.ACTION_MEDIA_UNMOUNTED)
            addAction(Intent.ACTION_MEDIA_EJECT)
            addAction(Intent.ACTION_MEDIA_REMOVED)
            addDataScheme("file")
        }
        context.registerReceiver(receiver, filter)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val callback = object : StorageManager.StorageVolumeCallback() {
                override fun onStateChanged(volume: android.os.storage.StorageVolume) {
                    refreshVolumes()
                }
            }
            storageManager.registerStorageVolumeCallback(context.mainExecutor, callback)
            onDispose {
                context.unregisterReceiver(receiver)
                storageManager.unregisterStorageVolumeCallback(callback)
            }
        } else {
            onDispose {
                context.unregisterReceiver(receiver)
            }
        }
    }

    // Context menu for the background
    var showBgMenu by remember { mutableStateOf(false) }
    var bgMenuOffset by remember { mutableStateOf(DpOffset.Zero) }
    val density = LocalDensity.current

    val lazyListState = rememberLazyListState()

    // Reordering state
    var renamingFavorite by remember { mutableStateOf<String?>(null) }
    var renamingSaf by remember { mutableStateOf<Uri?>(null) }
    var renamingNetwork by remember { mutableStateOf<NetworkConnection?>(null) }
    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var draggingOffset by remember { mutableFloatStateOf(0f) }
    val handleWidthPx = with(density) { 48.dp.toPx() }

    // Calculate visible library items using remember to avoid filtering on every recomposition (like during resizing)
    val visibleLibraryItems = remember(
        appState.appConfigs.libraryOrder.toList(),
        isRecycleBinEnabled,
        appState.appConfigs.isRecentVisible,
        appState.appConfigs.isGalleryVisible,
        appState.appConfigs.isMusicVisible,
        appState.appConfigs.isDownloadsVisible,
        appState.appConfigs.isDocumentsVisible,
        appState.appConfigs.isArchivesVisible,
        appState.appConfigs.isApksVisible,
        appState.appConfigs.isGamesVisible,
        appState.appConfigs.isTrashVisible
    ) {
        appState.appConfigs.libraryOrder.filter { id ->
            when (id) {
                "home" -> true // Permanent
                "trash" -> isRecycleBinEnabled && appState.appConfigs.isTrashVisible
                "recent" -> appState.appConfigs.isRecentVisible
                "gallery" -> appState.appConfigs.isGalleryVisible
                "music" -> appState.appConfigs.isMusicVisible
                "downloads" -> appState.appConfigs.isDownloadsVisible
                "documents" -> appState.appConfigs.isDocumentsVisible
                "archives" -> appState.appConfigs.isArchivesVisible
                "apks" -> appState.appConfigs.isApksVisible
                "games_manager" -> appState.appConfigs.isGamesVisible
                else -> true
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .fadingEdge(lazyListState, showBottom = isInWindowMode)
                .contextMenuDetector(enableLongPress = true, aggressive = false) { offset ->
                    bgMenuOffset = with(density) { DpOffset(offset.x.toDp(), offset.y.toDp()) }
                    showBgMenu = true
                }
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isMinimized) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = {
                        bgMenuOffset = DpOffset(0.dp, 0.dp) // Open at top
                        showBgMenu = true
                    }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.menu),
                            tint = LocalExtendedColors.current.sidebarIcons
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Section: Favorites
            item {
                NavSectionHeader(
                    stringResource(R.string.nav_favorites),
                    textAlphaProvider,
                    isExpanded = isFavoritesExpanded,
                    onToggle = { SettingsManager.setFavoritesExpanded(context, !isFavoritesExpanded) }
                )
            }

            if (appState.appConfigs.favoritePaths.isEmpty() && !isMinimized) {
                item {
                    AnimatedVisibility(
                        visible = isFavoritesExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Text(
                            text = stringResource(R.string.nav_no_favorites),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = appState.appConfigs.favoritePaths,
                    key = { _, path -> path }
                ) { _, path ->
                    val file = File(path)
                    val isDragging = draggedItemId == path
                    val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "FavoriteElevation")

                    AnimatedVisibility(
                        visible = isFavoritesExpanded || isMinimized,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
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
                            val customName = appState.appConfigs.favoriteNames[path]
                            NavFavoriteItem(
                                label = customName ?: file.name,
                                path = path,
                                onClick = { appState.navigateTo(file, null); onNavigate() },
                                onRemove = { appState.appConfigs.removeFavorite(path) },
                                onRename = { renamingFavorite = path },
                                appState = appState,
                                textAlphaProvider = textAlphaProvider,
                                isMinimized = isMinimized
                            )
                        }
                    }
                }
            }

            if (visibleLibraryItems.isNotEmpty()) {
                if (!isMinimized) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 5.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        NavSectionHeader(
                            stringResource(R.string.nav_library),
                            textAlphaProvider,
                            isExpanded = isLibraryExpanded,
                            onToggle = { SettingsManager.setLibraryExpanded(context, !isLibraryExpanded) }
                        )
                    }
                }

                itemsIndexed(
                    items = visibleLibraryItems,
                    key = { _, id -> id }
                ) { _, id ->
                    val isDragging = draggedItemId == id
                    val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "LibraryElevation")

                    AnimatedVisibility(
                        visible = isLibraryExpanded || isMinimized,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
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
                                "home" -> NavItem(
                                    label = stringResource(R.string.nav_home),
                                    icon = Icons.Default.Home,
                                    customIcon = R.drawable.ic_nav_home,
                                    onClick = { onItemSelected(NavSection.Home) },
                                    appState = appState,
                                    textAlphaProvider = textAlphaProvider,
                                    section = NavSection.Home,
                                    isMinimized = isMinimized
                                )
                                "gallery" -> NavItem(
                                    label = stringResource(R.string.nav_gallery),
                                    icon = Icons.Default.Image,
                                    customIcon = R.drawable.ic_nav_gallery,
                                    onClick = { onItemSelected(NavSection.Gallery) },
                                    appState = appState,
                                    textAlphaProvider = textAlphaProvider,
                                    section = NavSection.Gallery,
                                    isMinimized = isMinimized,
                                    onAddStorageClick = onAddStorageClick
                                )
                                "music" -> NavItem(
                                    label = stringResource(R.string.nav_music),
                                    icon = Icons.Default.MusicNote,
                                    customIcon = R.drawable.ic_nav_music,
                                    onClick = { onItemSelected(NavSection.Music) },
                                    appState = appState,
                                    textAlphaProvider = textAlphaProvider,
                                    section = NavSection.Music,
                                    isMinimized = isMinimized,
                                    onAddStorageClick = onAddStorageClick
                                )
                                "recent" -> NavItem(
                                    label = stringResource(R.string.nav_recent),
                                    icon = Icons.Default.History,
                                    customIcon = R.drawable.ic_nav_recent,
                                    onClick = { onItemSelected(NavSection.Recent) },
                                    appState = appState,
                                    textAlphaProvider = textAlphaProvider,
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
                                        textAlphaProvider = textAlphaProvider,
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
                                    textAlphaProvider = textAlphaProvider,
                                    section = NavSection.Downloads,
                                    isMinimized = isMinimized
                                )
                                "documents" -> NavItem(
                                    label = stringResource(R.string.nav_documents),
                                    icon = Icons.AutoMirrored.Filled.List,
                                    customIcon = R.drawable.ic_nav_documents,
                                    onClick = { onItemSelected(NavSection.Documents) },
                                    appState = appState,
                                    textAlphaProvider = textAlphaProvider,
                                    section = NavSection.Documents,
                                    isMinimized = isMinimized
                                )
                                "archives" -> NavItem(
                                    label = stringResource(R.string.nav_archives),
                                    icon = Icons.Default.FolderZip,
                                    customIcon = R.drawable.ic_zip,
                                    onClick = { onItemSelected(NavSection.Archives) },
                                    appState = appState,
                                    textAlphaProvider = textAlphaProvider,
                                    section = NavSection.Archives,
                                    isMinimized = isMinimized
                                )
                                "apks" -> NavItem(
                                    label = stringResource(R.string.nav_apks),
                                    icon = Icons.Default.Android,
                                    customIcon = R.drawable.ic_android_logo,
                                    onClick = { onItemSelected(NavSection.Apks) },
                                    appState = appState,
                                    textAlphaProvider = textAlphaProvider,
                                    section = NavSection.Apks,
                                    isMinimized = isMinimized
                                )
                                "games_manager" -> NavItem(
                                    label = stringResource(R.string.nav_game_saves),
                                    icon = Icons.AutoMirrored.Filled.List,
                                    customIcon = R.drawable.ic_nav_game,
                                    onClick = { onItemSelected(NavSection.Games) },
                                    appState = appState,
                                    textAlphaProvider = textAlphaProvider,
                                    section = NavSection.Games,
                                    isMinimized = isMinimized,
                                    onAddStorageClick = onAddStorageClick
                                )
                            }
                        }
                    }
                }
            }

            if (!isMinimized) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 5.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Section: Storage
            if (!isMinimized) {
                item {
                    NavSectionHeader(
                        stringResource(R.string.nav_storage),
                        textAlphaProvider,
                        isExpanded = isStorageExpanded,
                        onToggle = { SettingsManager.setStorageExpanded(context, !isStorageExpanded) }
                    )
                }
            }
            
            itemsIndexed(
                items = storageVolumes,
                key = { _, volume -> volume.path?.absolutePath ?: volume.uri?.toString() ?: volume.label }
            ) { _, volume ->
                AnimatedVisibility(
                    visible = isStorageExpanded || isMinimized,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().animateItem(), contentAlignment = if (isMinimized) Alignment.Center else Alignment.TopStart) {
                        NavStorageItem(
                            label = volume.label,
                            icon = volume.icon,
                            totalSpace = volume.totalSpace,
                            freeSpace = volume.freeSpace,
                            onClick = { onItemSelected(volume.section) },
                            modifier = Modifier.fileDropTarget(appState, destPath = volume.path),
                            appState = appState,
                            textAlphaProvider = textAlphaProvider,
                            path = volume.path,
                            onNavigate = onNavigate,
                            customIcon = volume.customIcon,
                            isMinimized = isMinimized
                        )

                    }
                }
            }

            // Section: Added Locations (SAF)
            if (appState.appConfigs.addedSafUris.isNotEmpty()) {
                if (!isMinimized) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 5.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        NavSectionHeader(
                            stringResource(R.string.nav_added_locations),
                            textAlphaProvider,
                            isExpanded = isAddedLocationsExpanded,
                            onToggle = { SettingsManager.setAddedLocationsExpanded(context, !isAddedLocationsExpanded) }
                        )

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

                    AnimatedVisibility(
                        visible = isAddedLocationsExpanded || isMinimized,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
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
                                onRename = { renamingSaf = uri },
                                modifier = Modifier.fileDropTarget(appState, destSafUri = uri),
                                appState = appState,
                                textAlphaProvider = textAlphaProvider,
                                isMinimized = isMinimized
                            )
                        }
                    }
                }
            }


            // Section: Network
            if (appState.appConfigs.networkConnections.isNotEmpty()) {
                if (!isMinimized) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 5.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        NavSectionHeader(
                            stringResource(R.string.nav_network),
                            textAlphaProvider,
                            isExpanded = isNetworkExpanded,
                            onToggle = { SettingsManager.setNetworkExpanded(context, !isNetworkExpanded) }
                        )
                    }
                }

                itemsIndexed(
                    items = appState.appConfigs.networkConnections,
                    key = { _, conn -> conn.id }
                ) { _, connection ->
                    val isDragging = draggedItemId == connection.id
                    val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "NetworkElevation")

                    AnimatedVisibility(
                        visible = isNetworkExpanded || isMinimized,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
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
                            onRename = { renamingNetwork = connection },
                            appState = appState,
                            textAlphaProvider = textAlphaProvider,
                            isMinimized = isMinimized
                        )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(50.dp)) }
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

    if (appState.isConfiguringGalleryFolders) {
        GalleryFoldersDialog(appState, onAddStorageClick)
    }
    if (appState.isConfiguringMusicFolders) {
        MusicFoldersDialog(appState, onAddStorageClick)
    }

    renamingFavorite?.let { path ->
        RenameDialog(
            initialValue = appState.appConfigs.favoriteNames[path] ?: File(path).name,
            onDismiss = { renamingFavorite = null },
            onConfirm = { newName -> appState.appConfigs.renameFavorite(path, newName) }
        )
    }

    renamingSaf?.let { uri ->
        RenameDialog(
            initialValue = appState.getSafDisplayName(uri),
            onDismiss = { renamingSaf = null },
            onConfirm = { newName -> appState.appConfigs.renameSafUri(uri, newName) }
        )
    }

    renamingNetwork?.let { connection ->
        RenameDialog(
            initialValue = connection.displayName,
            onDismiss = { renamingNetwork = null },
            onConfirm = { newName ->
                appState.appConfigs.updateNetworkConnection(connection.copy(displayName = newName))
            }
        )
    }
}

@Composable
private fun RenameDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.menu_rename)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text); onDismiss() }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun GalleryFoldersDialog(appState: FileExplorerState, onAddFolder: () -> Unit) {
    val context = LocalContext.current
    val folders by SettingsManager.galleryFolders
    val isFilterEnabled by SettingsManager.isGalleryFilterEnabled
    var showCustomPathDialog by remember { mutableStateOf(false) }

    if (showCustomPathDialog) {
        var customPath by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCustomPathDialog = false },
            title = { Text(stringResource(R.string.dialog_add_custom_path)) },
            text = {
                OutlinedTextField(
                    value = customPath,
                    onValueChange = { customPath = it },
                    placeholder = { Text(stringResource(R.string.custom_path_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customPath.isNotBlank()) {
                            val newSet = folders.toMutableSet()
                            newSet.add(customPath.trim())
                            SettingsManager.setGalleryFolders(context, newSet)
                            appState.refresh()
                        }
                        showCustomPathDialog = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomPathDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Dialog(
        onDismissRequest = { appState.isConfiguringGalleryFolders = false },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var isVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { isVisible = true }

        AnimatedVisibility(
            visible = isVisible,
            enter = scaleIn(initialScale = 0.9f) + fadeIn(),
            exit = scaleOut(targetScale = 0.9f) + fadeOut(),
            label = "GalleryFoldersDialogAnimation"
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(24.dp)
                    .shadow(16.dp, RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        stringResource(R.string.menu_gallery_folders),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.height(16.dp))

                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        if (!isFilterEnabled) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Text(
                                    stringResource(R.string.gallery_filter_off_msg),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(8.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        if (folders.isEmpty()) {
                            Text(
                                stringResource(R.string.gallery_no_folders_msg),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        } else {
                            Text(
                                stringResource(R.string.gallery_managed_folders),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                                itemsIndexed(folders.toList()) { _, path ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Folder, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = path,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        IconButton(onClick = {
                                            val newSet = folders.toMutableSet()
                                            newSet.remove(path)
                                            SettingsManager.setGalleryFolders(context, newSet)
                                            appState.refresh()
                                        }) {
                                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                        
                        val currentPath = appState.getCurrentStorageKey()
                        val canAddCurrent = currentPath != null && !currentPath.startsWith("virtual://")

                        Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onAddFolder() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("SAF")
                                }

                                if (canAddCurrent) {
                                    Button(
                                        onClick = {
                                            val newSet = folders.toMutableSet()
                                            newSet.add(currentPath!!)
                                            SettingsManager.setGalleryFolders(context, newSet)
                                            appState.refresh()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Icon(Icons.Default.AddLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.menu_gallery_add_current))
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = { showCustomPathDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.menu_gallery_custom_path))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { appState.isConfiguringGalleryFolders = false }) {
                            Text(stringResource(R.string.done))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MusicFoldersDialog(appState: FileExplorerState, onAddFolder: () -> Unit) {
    val context = LocalContext.current
    val folders by SettingsManager.musicFolders
    val isFilterEnabled by SettingsManager.isMusicFilterEnabled
    var showCustomPathDialog by remember { mutableStateOf(false) }

    if (showCustomPathDialog) {
        var customPath by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCustomPathDialog = false },
            title = { Text(stringResource(R.string.dialog_add_custom_path)) },
            text = {
                OutlinedTextField(
                    value = customPath,
                    onValueChange = { customPath = it },
                    placeholder = { Text(stringResource(R.string.custom_path_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customPath.isNotBlank()) {
                            val newSet = folders.toMutableSet()
                            newSet.add(customPath.trim())
                            SettingsManager.setMusicFolders(context, newSet)
                            appState.refresh()
                        }
                        showCustomPathDialog = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomPathDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Dialog(
        onDismissRequest = { appState.isConfiguringMusicFolders = false },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var isVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { isVisible = true }

        AnimatedVisibility(
            visible = isVisible,
            enter = scaleIn(initialScale = 0.9f) + fadeIn(),
            exit = scaleOut(targetScale = 0.9f) + fadeOut(),
            label = "MusicFoldersDialogAnimation"
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(24.dp)
                    .shadow(16.dp, RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        stringResource(R.string.menu_music_folders),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.height(16.dp))

                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        if (!isFilterEnabled) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Text(
                                    stringResource(R.string.gallery_filter_off_msg),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(8.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        if (folders.isEmpty()) {
                            Text(
                                "No music folders added. Standard music locations will be used if filtering is enabled.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        } else {
                            Text(
                                "Managed Music Folders:",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                                itemsIndexed(folders.toList()) { _, path ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Folder, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = path,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        IconButton(onClick = {
                                            val newSet = folders.toMutableSet()
                                            newSet.remove(path)
                                            SettingsManager.setMusicFolders(context, newSet)
                                            appState.refresh()
                                        }) {
                                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                        
                        val currentPath = appState.getCurrentStorageKey()
                        val canAddCurrent = currentPath != null && !currentPath.startsWith("virtual://")

                        Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onAddFolder() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("SAF")
                                }

                                if (canAddCurrent) {
                                    Button(
                                        onClick = {
                                            val newSet = folders.toMutableSet()
                                            newSet.add(currentPath!!)
                                            SettingsManager.setMusicFolders(context, newSet)
                                            appState.refresh()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Icon(Icons.Default.AddLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.menu_music_add_current))
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = { showCustomPathDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.menu_music_custom_path))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                appState.scope.launch(Dispatchers.IO) {
                                    MusicMetadataManager.sync(context, folders)
                                    GlobalEvents.triggerRefresh()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Sync, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.menu_sync_list))
                        }

                        TextButton(onClick = { appState.isConfiguringMusicFolders = false }) {
                            Text(stringResource(R.string.done))
                        }
                    }
                }
            }
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
    val context = LocalContext.current
    val isFtpEnabled by SettingsManager.isFtpServerEnabled
    val ftpMode by SettingsManager.ftpMode

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(16.dp),
        containerColor = LocalExtendedColors.current.menuBackground
    ) {
        Column(
            modifier = Modifier.animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
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
            HorizontalDivider()
            DropdownMenuItem(
                text = { 
                    val text = if (isFtpEnabled && ftpMode == SettingsManager.FtpMode.FULL_STORAGE) 
                        stringResource(R.string.nav_stop_ftp) else stringResource(R.string.nav_start_ftp)
                    Text(text)
                },
                onClick = {
                    onDismissRequest()
                    if (isFtpEnabled && ftpMode == SettingsManager.FtpMode.FULL_STORAGE) {
                        SettingsManager.setFtpServerEnabled(context, false)
                    } else {
                        SettingsManager.setFtpServerEnabled(context, true, SettingsManager.FtpMode.FULL_STORAGE)
                    }
                },
                leadingIcon = { Icon(if (isFtpEnabled && ftpMode == SettingsManager.FtpMode.FULL_STORAGE) Icons.Default.WifiOff else Icons.Default.Wifi, null) }
            )
        }
    }
}

@Composable
private fun NavSectionHeader(
    text: String,
    textAlphaProvider: State<Float>,
    isExpanded: Boolean = true,
    onToggle: (() -> Unit)? = null
) {
    val textAlpha = textAlphaProvider.value
    if (textAlpha <= 0f) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onToggle != null) Modifier.clickable { onToggle() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text.uppercase(),
            modifier = Modifier
                .weight(1f)
                .horizontalFadingEdge { textAlpha },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.5.sp
        )
        if (onToggle != null) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp).graphicsLayer { alpha = textAlpha },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
    onRename: (() -> Unit)? = null,
    onNavigate: (() -> Unit)? = null,
    onAddStorageClick: (() -> Unit)? = null
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(16.dp),
        containerColor = LocalExtendedColors.current.menuBackground
    ) {
        val isLibrarySection = section is NavSection.Recent || section is NavSection.Gallery ||
                section is NavSection.Music || section is NavSection.Downloads ||
                section is NavSection.Documents || section is NavSection.Games ||
                section is NavSection.RecycleBin

        Column(
            modifier = Modifier.animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
        ) {
            if (isLibrarySection && section !is NavSection.Home) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_remove)) },
                    onClick = {
                        onDismissRequest()
                        when (section) {
                            is NavSection.Recent -> appState.appConfigs.toggleRecentVisibility()
                            is NavSection.Gallery -> appState.appConfigs.toggleGalleryVisibility()
                            is NavSection.Music -> appState.appConfigs.toggleMusicVisibility()
                            is NavSection.Downloads -> appState.appConfigs.toggleDownloadsVisibility()
                            is NavSection.Documents -> appState.appConfigs.toggleDocumentsVisibility()
                            is NavSection.Archives -> appState.appConfigs.toggleArchivesVisibility()
                            is NavSection.Apks -> appState.appConfigs.toggleApksVisibility()
                            is NavSection.Games -> appState.appConfigs.toggleGamesVisibility()
                            is NavSection.RecycleBin -> appState.appConfigs.toggleTrashVisibility()
                            else -> {}
                        }
                    },
                    leadingIcon = { Icon(Icons.Default.VisibilityOff, null) }
                )
                HorizontalDivider()
            }

            if (section == NavSection.Games) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.nav_add_storage)) },
                    onClick = {
                        onDismissRequest()
                        if (onAddStorageClick != null) {
                            appState.isAddingGameShortcut = true
                            onAddStorageClick()
                        }
                    },
                    leadingIcon = { Icon(Icons.Default.Add, null) }
                )
                HorizontalDivider()
            }

            if (section is NavSection.Gallery) {
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
                        onDismissRequest()
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
                        onDismissRequest()
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
                        onDismissRequest()
                        appState.appConfigs.toggleGalleryAlbums()
                    },
                    leadingIcon = {
                        val iconTheme = SettingsManager.getEffectiveIconTheme(com.troikoss.continuum_explorer.managers.IconCategory.FILES_FOLDERS)
                        if (iconTheme == IconTheme.MATERIAL) {
                            Icon(Icons.Default.Folder, null)
                        } else {
                            val resId = if (iconTheme == IconTheme.COLOURFULDUO) R.drawable.ic_folder_duo else R.drawable.ic_folder
                            Icon(IconHelper.rememberThemePainter(resId, category = com.troikoss.continuum_explorer.managers.IconCategory.FILES_FOLDERS), null)
                        }
                    }
                )
                HorizontalDivider()
            }

            if (section is NavSection.Music) {
                val isFilterEnabled by SettingsManager.isMusicFilterEnabled
                val context = LocalContext.current



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
                        onDismissRequest()
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
                        onDismissRequest()
                        appState.createNewPlaylist()
                    },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) }
                )

                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_sync_list)) },
                    onClick = {
                        onDismissRequest()
                        appState.scope.launch(Dispatchers.IO) {
                            MusicMetadataManager.sync(appState.context, SettingsManager.musicFolders.value)
                            GlobalEvents.triggerRefresh()
                        }
                    },
                    leadingIcon = { Icon(Icons.Default.Sync, null) }
                )
                HorizontalDivider()
            }

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

            if (onRename != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_rename)) },
                    onClick = {
                        onDismissRequest()
                        onRename()
                    },
                    leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null) }
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

            if (section is NavSection.Games) {
                val isFtpEnabled by SettingsManager.isFtpServerEnabled
                val ftpMode by SettingsManager.ftpMode
                val context = LocalContext.current
                
                val isGamesFtpActive = isFtpEnabled && ftpMode == SettingsManager.FtpMode.GAMES

                DropdownMenuItem(
                    text = { Text(if (isGamesFtpActive) stringResource(R.string.nav_stop_ftp_game_manager) else stringResource(R.string.nav_start_ftp_game_manager)) },
                    onClick = {
                        onDismissRequest()
                        if (isGamesFtpActive) {
                            SettingsManager.setFtpServerEnabled(context, false)
                        } else {
                            SettingsManager.setFtpServerEnabled(context, true, SettingsManager.FtpMode.GAMES)
                        }
                    },
                    leadingIcon = { Icon(if (isGamesFtpActive) Icons.Default.WifiOff else Icons.Default.Wifi, null) }
                )
            }

            if (section is NavSection.Documents) {
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
                        onDismissRequest()
                        appState.appConfigs.toggleDocumentsFolder()
                        onNavigate?.invoke()
                    },
                    leadingIcon = {
                        val iconTheme = SettingsManager.getEffectiveIconTheme(com.troikoss.continuum_explorer.managers.IconCategory.FILES_FOLDERS)
                        if (iconTheme == IconTheme.MATERIAL) {
                            Icon(Icons.Default.Folder, null)
                        } else {
                            val resId = if (iconTheme == IconTheme.COLOURFULDUO) R.drawable.ic_folder_duo else R.drawable.ic_folder
                            Icon(IconHelper.rememberThemePainter(resId, category = com.troikoss.continuum_explorer.managers.IconCategory.FILES_FOLDERS), null)
                        }
                    }
                )
            }
            if (section !is NavSection.Recent && section !is NavSection.Gallery && section !is NavSection.Music && section !is NavSection.NetworkStorage) {
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
}

@Composable
private fun NavItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    appState: FileExplorerState,
    textAlphaProvider: State<Float>,
    isMinimized: Boolean,
    modifier: Modifier = Modifier,
    section: NavSection? = null,
    customIcon: Int? = null,
    badge: (@Composable () -> Unit)? = null,
    onAddStorageClick: (() -> Unit)? = null
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

    val iconContent = @Composable {
        val extendedColors = LocalExtendedColors.current
        val iconTheme = SettingsManager.getEffectiveIconTheme(com.troikoss.continuum_explorer.managers.IconCategory.SIDEBAR)
        val tint = when (section) {
            is NavSection.Home -> extendedColors.homeIcon
            is NavSection.Gallery -> extendedColors.galleryIcon
            is NavSection.Music -> extendedColors.musicIcon
            is NavSection.Recent -> extendedColors.recentIcon
            is NavSection.Downloads -> extendedColors.downloadsIcon
            is NavSection.RecycleBin -> extendedColors.recycleBinIcon
            is NavSection.Documents -> extendedColors.documentsIcon
            is NavSection.Games -> extendedColors.gameIcon
            else -> extendedColors.sidebarIcons
        }
        if (customIcon != null && (iconTheme == IconTheme.COLOURFUL || iconTheme == IconTheme.COLOURFULDUO)) {
            val finalIcon = if (iconTheme == IconTheme.COLOURFULDUO) {
                when (customIcon) {
                    R.drawable.ic_nav_home -> R.drawable.ic_nav_home_duo
                    R.drawable.ic_nav_gallery -> R.drawable.ic_nav_gallery_duo
                    R.drawable.ic_nav_music -> R.drawable.ic_nav_music_duo
                    R.drawable.ic_nav_recent -> R.drawable.ic_nav_recent_duo
                    R.drawable.ic_nav_downloads -> R.drawable.ic_nav_downloads_duo
                    R.drawable.ic_nav_documents -> R.drawable.ic_nav_documents_duo
                    R.drawable.ic_zip -> R.drawable.ic_zip_duo
                    R.drawable.ic_android_logo -> R.drawable.ic_android_logo // Add duo if available
                    R.drawable.ic_nav_game -> R.drawable.ic_nav_game_duo
                    R.drawable.ic_nav_trash -> R.drawable.ic_nav_trash_duo
                    R.drawable.ic_storage -> R.drawable.ic_storage_duo
                    else -> customIcon
                }
            } else customIcon
            Icon(
                painter = IconHelper.rememberThemePainter(resId = finalIcon, category = com.troikoss.continuum_explorer.managers.IconCategory.SIDEBAR),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        } else if (section == NavSection.Games) {
            Icon(
                painter = IconHelper.rememberThemePainter(resId = R.drawable.ic_nav_game_material, category = com.troikoss.continuum_explorer.managers.IconCategory.SIDEBAR),
                contentDescription = null,
                tint = tint
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
                label = { Text(label, fontWeight = FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.fillMaxWidth().horizontalFadingEdge { textAlphaProvider.value }) },
                selected = false,
                onClick = onClick,
                icon = iconContent,
                modifier = Modifier.height(36.dp),
                shape = shape,
                badge = badge
            )
        }

        Box(modifier = Modifier.offset { IntOffset(menuOffset.x.roundToPx(), menuOffset.y.roundToPx()) }) {
            NavContextMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                appState = appState,
                section = section,
                onNavigate = onClick,
                onAddStorageClick = onAddStorageClick
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
    onRename: () -> Unit,
    appState: FileExplorerState,
    textAlphaProvider: State<Float>,
    isMinimized: Boolean,
    modifier: Modifier = Modifier
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

    val iconContent = @Composable {
        IconHelper.FolderIcon(name = label, path = path, isFavorite = true, category = com.troikoss.continuum_explorer.managers.IconCategory.SIDEBAR)
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
                label = { Text(label, fontWeight = FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.fillMaxWidth().horizontalFadingEdge { textAlphaProvider.value }) },
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
                onRename = onRename,
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
    onRename: () -> Unit,
    appState: FileExplorerState,
    textAlphaProvider: State<Float>,
    isMinimized: Boolean,
    modifier: Modifier = Modifier
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

    val iconContent = @Composable {
        IconHelper.FolderIcon(name = label, path = uri.toString(), tint = LocalExtendedColors.current.sidebarIcons, category = com.troikoss.continuum_explorer.managers.IconCategory.SIDEBAR)
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
                        Column(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth().horizontalFadingEdge { textAlphaProvider.value }) {
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
                        Text(label, fontWeight = FontWeight.Normal, maxLines = 2, overflow = TextOverflow.Clip, modifier = Modifier.fillMaxWidth().horizontalFadingEdge { textAlphaProvider.value })
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
                onRename = onRename,
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
    onRename: () -> Unit,
    appState: FileExplorerState,
    textAlphaProvider: State<Float>,
    isMinimized: Boolean
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

    val iconContent = @Composable {
        val iconTheme = SettingsManager.getEffectiveIconTheme(com.troikoss.continuum_explorer.managers.IconCategory.SIDEBAR)
        if (iconTheme == IconTheme.COLOURFUL || iconTheme == IconTheme.COLOURFULDUO) {
            val drawableId = if (iconTheme == IconTheme.COLOURFULDUO) R.drawable.ic_network_duo else R.drawable.ic_network
            Icon(
                painter = IconHelper.rememberThemePainter(resId = drawableId, category = com.troikoss.continuum_explorer.managers.IconCategory.SIDEBAR),
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
                        Column(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth().horizontalFadingEdge { textAlphaProvider.value }) {
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
                        Text(connection.displayName, fontWeight = FontWeight.Normal, maxLines = 2, overflow = TextOverflow.Clip, modifier = Modifier.fillMaxWidth().horizontalFadingEdge { textAlphaProvider.value })
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
                onRename = onRename,
                onNavigate = onClick
            )
        }
    }
}

/**
 * Item for Storage that shows a progress bar and usage details.
 */
@Composable
private fun NavStorageItem(
    label: String,
    icon: ImageVector,
    totalSpace: Long,
    freeSpace: Long,
    onClick: () -> Unit,
    appState: FileExplorerState,
    textAlphaProvider: State<Float>,
    isMinimized: Boolean,
    path: File?,
    modifier: Modifier = Modifier,
    onNavigate: () -> Unit = {},
    customIcon: Int? = null
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

    val iconContent = @Composable {
        val iconTheme = SettingsManager.getEffectiveIconTheme(com.troikoss.continuum_explorer.managers.IconCategory.SIDEBAR)
        if (customIcon != null && (iconTheme == IconTheme.COLOURFUL || iconTheme == IconTheme.COLOURFULDUO)) {
            val finalIcon = if (iconTheme == IconTheme.COLOURFULDUO) {
                when (customIcon) {
                    R.drawable.ic_folder -> R.drawable.ic_folder_duo
                    R.drawable.ic_storage -> R.drawable.ic_storage_duo
                    else -> customIcon
                }
            } else customIcon
            Icon(
                painter = IconHelper.rememberThemePainter(resId = finalIcon, category = com.troikoss.continuum_explorer.managers.IconCategory.SIDEBAR),
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
                        Column(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth().horizontalFadingEdge { textAlphaProvider.value }) {
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
                    textAlphaProvider = textAlphaProvider,
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
    textAlphaProvider: State<Float>,
    onNavigate: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var subDirs by remember { mutableStateOf<List<File>>(emptyList()) }

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
            label = { Text(folder.name, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.fillMaxWidth().horizontalFadingEdge { textAlphaProvider.value }) },
            selected = false,
            onClick = {
                appState.navigateTo(folder, null)
                onNavigate()
            },
            icon = {
                Spacer(modifier = Modifier.width((level * 16).dp))
                IconHelper.FolderIcon(name = folder.name, path = folder.absolutePath, iconSize = 20.dp, category = com.troikoss.continuum_explorer.managers.IconCategory.SIDEBAR)
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
                    textAlphaProvider = textAlphaProvider,
                    onNavigate = onNavigate
                )
            }
        }
    }
}
