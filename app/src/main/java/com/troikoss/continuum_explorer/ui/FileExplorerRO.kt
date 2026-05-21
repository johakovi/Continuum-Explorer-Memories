package com.troikoss.continuum_explorer.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.windowInsetsPadding
import com.troikoss.continuum_explorer.managers.DetailsMode
import com.troikoss.continuum_explorer.managers.FileOperationsManager
import com.troikoss.continuum_explorer.managers.SettingsManager
import com.troikoss.continuum_explorer.model.NavSection
import com.troikoss.continuum_explorer.model.NetworkConnection
import com.troikoss.continuum_explorer.managers.ThemeTopMode
import com.troikoss.continuum_explorer.ui.activities.PopUpActivity
import com.troikoss.continuum_explorer.model.ScreenSize
import com.troikoss.continuum_explorer.model.LibraryItem
import com.troikoss.continuum_explorer.providers.StorageProviders
import com.troikoss.continuum_explorer.ui.theme.LocalExtendedColors
import com.troikoss.continuum_explorer.ui.components.*
import com.troikoss.continuum_explorer.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

/**
 * The main layout container for the File Explorer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerRO(
    initialPath: String? = null,
    initialUri: String? = null,
    initialArchive: File? = null,
    initialArchiveUri: Uri? = null,
    initialArchiveName: String? = null,
    initialLibraryItem: LibraryItem = LibraryItem.None,
    initialNetworkConnectionId: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- Tab Management ---
    val tabs = remember { mutableStateListOf<FileExplorerState>() }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    fun createNewTabState(ctx: Context, scp: CoroutineScope): FileExplorerState {
        return FileExplorerState(ctx, scp).apply {
            onOpenInNewTab = { item ->
                val newState = createNewTabState(ctx, scp)
                val tabFileRef = item.fileRef
                val tabDocRef = item.documentFileRef
                when {
                    item.provider.capabilities.isRemote -> newState.navigateTo(
                        null, null, addToHistory = false,
                        networkProvider = item.provider, networkId = item.providerId,
                        networkConnectionId = item.provider.connectionId.ifEmpty { null }
                    )
                    item.absolutePath == "virtual://recent" -> newState.navigateTo(null, null, addToHistory = false, libraryItem = LibraryItem.Recent)
                    item.absolutePath == "virtual://gallery" -> newState.navigateTo(null, null, addToHistory = false, libraryItem = LibraryItem.Gallery)
                    item.absolutePath == "virtual://downloads" -> newState.navigateTo(null, null, addToHistory = false, libraryItem = LibraryItem.Downloads)
                    item.absolutePath == "virtual://documents" -> newState.navigateTo(null, null, addToHistory = false, libraryItem = LibraryItem.Documents)
                    item.absolutePath == "virtual://recycle_bin" -> {
                        val trashDir = File(Environment.getExternalStorageDirectory(), ".Trash")
                        if (!trashDir.exists()) trashDir.mkdirs()
                        newState.navigateTo(trashDir, null, addToHistory = false, libraryItem = LibraryItem.RecycleBin)
                    }
                    ZipUtils.isArchive(item) && SettingsManager.isDefaultArchiveViewerEnabled.value -> {
                        newState.navigateTo(
                            newPath = null,
                            newUri = null,
                            addToHistory = false,
                            archiveFile = tabFileRef,
                            archiveUri = tabDocRef?.uri,
                            archiveName = item.name,
                            archivePath = ""
                        )
                    }
                    tabFileRef != null -> newState.navigateTo(tabFileRef, null, addToHistory = false)
                    tabDocRef != null -> newState.navigateTo(null, tabDocRef.uri, addToHistory = false)
                }
                tabs.add(newState)
                selectedTabIndex = tabs.size - 1
            }
        }
    }

    // Initialize first tab if empty
    LaunchedEffect(Unit) {
        if (tabs.isEmpty()) {
            val firstState = createNewTabState(context, scope)
            when {
                initialArchive != null -> firstState.navigateTo(null, null, addToHistory = false, archiveFile = initialArchive, archivePath = "")
                initialArchiveUri != null -> firstState.navigateTo(null, null, addToHistory = false, archiveUri = initialArchiveUri, archiveName = initialArchiveName, archivePath = "")
                initialLibraryItem == LibraryItem.RecycleBin -> {
                    val trashDir = File(Environment.getExternalStorageDirectory(), ".Trash")
                    if (!trashDir.exists()) trashDir.mkdirs()
                    firstState.navigateTo(trashDir, null, addToHistory = false, libraryItem = LibraryItem.RecycleBin)
                }
                initialNetworkConnectionId != null -> {
                    val conn = firstState.appConfigs.networkConnections.find { it.id == initialNetworkConnectionId }
                    if (conn != null) {
                        val provider = StorageProviders.network(conn)
                        firstState.navigateTo(null, null, addToHistory = false, networkProvider = provider, networkId = provider.rootId(), networkConnectionId = conn.id)
                    }
                }
                initialLibraryItem != LibraryItem.None -> firstState.navigateTo(null, null, addToHistory = false, libraryItem = initialLibraryItem)
                initialPath != null -> firstState.navigateTo(File(initialPath), null, addToHistory = false)
                initialUri != null -> firstState.navigateTo(null, Uri.parse(initialUri), addToHistory = false)
            }
            tabs.add(firstState)
        }
    }

    if (tabs.isEmpty()) return // Wait for initialization

    val safeIndex = selectedTabIndex.coerceIn(0, tabs.lastIndex)
    val appState = tabs[safeIndex]
    val focusManager = LocalFocusManager.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // --- Storage Access Framework Launcher ---
    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        appState.handleSafResult(uri)
    }

    val sdCardLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            appState.handleSafResult(result.data?.data)
        }
    }

    // --- Network Storage Launcher ---
    val networkScope = rememberCoroutineScope()
    val onAddNetwork: () -> Unit = {
        networkScope.launch {
            val result = FileOperationsManager.requestNetworkConnection()
            if (result != null) {
                appState.appConfigs.addNetworkConnection(result)
            }
        }
        context.startActivity(Intent(context, PopUpActivity::class.java))
    }
    val onEditNetwork: (NetworkConnection) -> Unit = { existing ->
        networkScope.launch {
            val result = FileOperationsManager.requestNetworkConnection(existing)
            if (result != null) {
                appState.appConfigs.updateNetworkConnection(result)
            }
        }
        context.startActivity(Intent(context, PopUpActivity::class.java))
    }

    // --- Side Effects ---
    LaunchedEffect(appState, appState.currentPath, appState.currentSafUri, appState.folderConfigs.sortParams,
                   appState.currentArchiveFile, appState.currentArchiveUri, appState.currentArchivePath, appState.libraryItem,
                   appState.currentNetworkProvider, appState.currentNetworkId) {
        appState.triggerLoad()
    }

    BackHandler(enabled = appState.canGoUp || appState.selectionManager.selectedItems.isNotEmpty()) {
        if (appState.selectionManager.selectedItems.isNotEmpty()) {
            appState.selectionManager.clear()
        } else appState.goUp()
    }

    // --- Main Layout ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        appState.isShiftPressed = event.keyboardModifiers.isShiftPressed
                        val activeChange = event.changes.firstOrNull { it.pressed } ?: event.changes.firstOrNull()
                        if (activeChange != null) {
                            appState.isMouseInteraction = (activeChange.type == PointerType.Mouse)
                        }
                    }
                }
            }
            .fileDropTarget(appState)
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = appState.getScreenSize() == ScreenSize.SMALL,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = LocalExtendedColors.current.sidebarBackground
                ) {
                    NavigationContent(
                        appState = appState,
                        onCloseDrawer = { scope.launch { drawerState.close() } },
                        onAddStorage = { safLauncher.launch(null) },
                        onAddSdCard = { volume ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                sdCardLauncher.launch(volume.createOpenDocumentTreeIntent())
                            }
                        },
                        onAddNetwork = onAddNetwork,
                        onEditNetwork = onEditNetwork
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    ExplorerTopBar(
                        tabs = tabs,
                        selectedTabIndex = safeIndex,
                        onTabSelected = { selectedTabIndex = it },
                        onAddTab = {
                            tabs.add(createNewTabState(context, scope))
                            selectedTabIndex = tabs.size - 1
                        },
                        onCloseTab = { index ->
                            if (tabs.size > 1) {
                                tabs.removeAt(index)
                                if (selectedTabIndex >= tabs.size) {
                                    selectedTabIndex = (tabs.size - 1).coerceAtLeast(0)
                                }
                            }
                        },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        appState = appState
                    )
                }
            ) { innerPadding ->
                ExplorerBody(
                    modifier = Modifier.padding(innerPadding),
                    appState = appState,
                    onAddStorage = { safLauncher.launch(null) },
                    onAddSdCard = { volume ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            sdCardLauncher.launch(volume.createOpenDocumentTreeIntent())
                        }
                    },
                    onAddNetwork = onAddNetwork,
                    onEditNetwork = onEditNetwork
                )
            }
        }
    }
}

@Composable
private fun NavigationContent(
    appState: FileExplorerState,
    onCloseDrawer: () -> Unit,
    onAddStorage: () -> Unit,
    onAddSdCard: (android.os.storage.StorageVolume) -> Unit = {},
    onAddNetwork: () -> Unit = {},
    onEditNetwork: (NetworkConnection) -> Unit = {}
) {
    val context = LocalContext.current
    NavigationPane(
        appState = appState,
        onItemSelected = { section ->
            navigateToSection(appState, context, section, onAddSdCard)
            onCloseDrawer()
        },
        onSafItemSelected = { uri ->
            appState.navigateTo(null, uri)
            onCloseDrawer()
        },
        onAddStorageClick = onAddStorage,
        onAddNetworkClick = onAddNetwork,
        onEditNetworkClick = onEditNetwork,
        onNavigate = onCloseDrawer,
        currentWidth = appState.appConfigs.navPaneWidth // Modal drawer usually full width or fixed, but passing for consistency
    )
}

@Composable
private fun ExplorerTopBar(
    tabs: List<FileExplorerState>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onAddTab: () -> Unit,
    onCloseTab: (Int) -> Unit,
    onMenuClick: () -> Unit,
    appState: FileExplorerState
) {
    val themeTop = SettingsManager.themeTop.value
    val density = LocalDensity.current
    Column(
        modifier = Modifier
            .background(if (themeTop == ThemeTopMode.FLOAT) MaterialTheme.colorScheme.surfaceContainerLow else LocalExtendedColors.current.topBarBackground)
    ) {
        TabBar(
            tabStates = tabs,
            selectedTabIndex = selectedTabIndex,
            onTabSelected = onTabSelected,
            onAddTab = onAddTab,
            onCloseTab = onCloseTab
        )

        TopBar(
            onMenuClick = onMenuClick,
            appState = appState
        )

        if (SettingsManager.isCommandBarVisible.value) {
            CommandBar(appState = appState)
        }
    }
}

@Composable
private fun ExplorerBody(
    modifier: Modifier = Modifier,
    appState: FileExplorerState,
    onAddStorage: () -> Unit,
    onAddSdCard: (android.os.storage.StorageVolume) -> Unit = {},
    onAddNetwork: () -> Unit = {},
    onEditNetwork: (NetworkConnection) -> Unit = {}
) {
    val context = LocalContext.current
    val screenSize = appState.getScreenSize()
    val rawNavPaneWidth = appState.appConfigs.navPaneWidth
    val navPaneWidth by androidx.compose.animation.core.animateDpAsState(
        targetValue = rawNavPaneWidth,
        label = "NavPaneWidthAnimation",
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        )
    )
    val detailsPaneWidth = appState.appConfigs.detailsPaneWidth

    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f).padding(8.dp)) {
            // Navigation Pane (Side)
            if (screenSize != ScreenSize.SMALL) {
                Box(modifier = Modifier.fillMaxHeight().padding(end = 8.dp)) {
                    PermanentDrawerSheet(
                        modifier = Modifier.width(navPaneWidth).fillMaxHeight(),
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        drawerShape = RoundedCornerShape(16.dp),
                        drawerContainerColor = LocalExtendedColors.current.sidebarBackground
                    ) {
                        NavigationPane(
                            appState = appState,
                            onItemSelected = { section ->
                                navigateToSection(
                                    appState,
                                    context,
                                    section,
                                    onAddSdCard
                                )
                            },
                            onSafItemSelected = { appState.navigateTo(null, it) },
                            onAddStorageClick = onAddStorage,
                            onAddNetworkClick = onAddNetwork,
                            onEditNetworkClick = onEditNetwork,
                            currentWidth = navPaneWidth
                        )
                    }
                }
                VerticalResizeHandle(
                    showDivider = false,
                    onResize = { delta ->
                        appState.appConfigs.navPaneWidth = (appState.appConfigs.navPaneWidth + delta).coerceIn(80.dp, 320.dp)
                    },
                    onResizeFinished = {
                        val currentWidth = appState.appConfigs.navPaneWidth
                        if (currentWidth > 80.dp && currentWidth < 160.dp) {
                            appState.appConfigs.navPaneWidth = if (currentWidth < 125.dp) 80.dp else 220.dp
                        }
                        appState.appConfigs.savePaneWidths()
                    }
                )
            }

            // Main Content Area
            Box(modifier = Modifier.weight(1f)) {
                FileContent(appState = appState)
            }

            // Details Pane
            if (screenSize == ScreenSize.LARGE && SettingsManager.detailsMode.value == DetailsMode.PANE) {
                VerticalResizeHandle(
                    showDivider = false,
                    onResize = { delta ->
                        appState.appConfigs.detailsPaneWidth =
                            (appState.appConfigs.detailsPaneWidth - delta).coerceIn(200.dp, 300.dp)
                        appState.appConfigs.savePaneWidths()
                    }
                )
                DetailsPane(
                    appState = appState,
                    modifier = Modifier.width(detailsPaneWidth).padding(start = 8.dp).fillMaxHeight()
                )
            }
        }

        // Details Bar (Bottom)
        if (screenSize == ScreenSize.LARGE && SettingsManager.detailsMode.value == DetailsMode.BAR) {
            HorizontalDivider()
            DetailsBar(appState = appState)
        }
    }
}

private fun navigateToSection(
    appState: FileExplorerState,
    context: Context,
    section: NavSection,
    onAddSdCard: (android.os.storage.StorageVolume) -> Unit = {}
) {
    val internalRoot = Environment.getExternalStorageDirectory()
    when (section) {
        is NavSection.InternalStorage -> appState.navigateTo(internalRoot, null, newRoot = internalRoot)
        is NavSection.RecycleBin -> {
            val trashDir = File(internalRoot, ".Trash")
            if (!trashDir.exists()) trashDir.mkdirs()
            appState.navigateTo(trashDir, null, newRoot = internalRoot, libraryItem = LibraryItem.RecycleBin)
        }
        is NavSection.Recent -> appState.navigateTo(null, null, libraryItem = LibraryItem.Recent)
        is NavSection.Gallery -> appState.navigateTo(null, null, libraryItem = LibraryItem.Gallery)
        is NavSection.Downloads -> appState.navigateTo(null, null, libraryItem = LibraryItem.Downloads)
        is NavSection.Documents -> {
            if (appState.appConfigs.isDocumentsFolderEnabled) {
                val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                if (!docsDir.exists()) docsDir.mkdirs()
                appState.navigateTo(docsDir, null, newRoot = internalRoot)
            } else {
                appState.navigateTo(null, null, libraryItem = LibraryItem.Documents)
            }
        }
        is NavSection.NetworkStorage -> {
            val conn = appState.appConfigs.networkConnections.find { it.id == section.connectionId } ?: return
            val provider = StorageProviders.network(conn)
            appState.navigateTo(
                newPath = null, newUri = null,
                networkProvider = provider, networkId = provider.rootId(),
                networkConnectionId = conn.id,
                addToHistory = true,
            )
        }
        is NavSection.RemovableVolume -> {
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val volumes = storageManager.storageVolumes
            val volumeIndex = section.volumeIndex

            if (volumeIndex < volumes.size) {
                val volume = volumes[volumeIndex]
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    volume.directory?.let { appState.navigateTo(it, null, newRoot = it) }
                } else {
                    val externalDirs = context.getExternalFilesDirs(null)
                    val guessedRoot = if (volumeIndex < externalDirs.size) {
                        externalDirs[volumeIndex]?.let { dir ->
                            File(dir.absolutePath.split("/Android")[0])
                        }
                    } else null

                    if (guessedRoot != null && guessedRoot.exists() && guessedRoot.canRead()) {
                        appState.navigateTo(guessedRoot, null, newRoot = guessedRoot)
                    } else {
                        // Fallback: Trigger SAF for this specific volume on Android 10
                        onAddSdCard(volume)
                    }
                }
            }
        }
    }
}
