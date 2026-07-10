package com.troikoss.continuum_explorer.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.documentfile.provider.DocumentFile
import com.troikoss.continuum_explorer.R
import com.troikoss.continuum_explorer.managers.DocumentsManager
import com.troikoss.continuum_explorer.managers.DownloadsManager
import com.troikoss.continuum_explorer.managers.GalleryManager
import com.troikoss.continuum_explorer.managers.GamesManager
import com.troikoss.continuum_explorer.managers.MusicManager
import com.troikoss.continuum_explorer.managers.MusicMetadataManager
import com.troikoss.continuum_explorer.managers.RecentFilesManager
import com.troikoss.continuum_explorer.managers.SearchManager
import com.troikoss.continuum_explorer.managers.SelectionManager
import com.troikoss.continuum_explorer.managers.SettingsManager
import com.troikoss.continuum_explorer.model.*
import com.troikoss.continuum_explorer.model.StorageProvider
import com.troikoss.continuum_explorer.providers.LocalProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages the core state and logic for the File Explorer.
 * It handles file navigation, sorting, and selection state.
 */
class FileExplorerState(
    val context: Context,
    val scope: CoroutineScope
) {
    // Configuration managers
    val folderConfigs = FolderConfigurations(context)
    val appConfigs = AppConfigurations(context)

    // Reference to the main storage root to prevent navigating into system files
    var storageRoot by mutableStateOf<File>(Environment.getExternalStorageDirectory())

    // Current directory or SAF URI being viewed
    var currentPath by mutableStateOf<File?>(Environment.getExternalStorageDirectory())
    var currentSafUri by mutableStateOf<Uri?>(null)

    // Flag for special virtual locations
    var libraryItem by mutableStateOf(LibraryItem.None)

    // Archive Navigation State
    var currentArchiveFile by mutableStateOf<File?>(null)
    var currentArchiveUri by mutableStateOf<Uri?>(null)
    var currentArchiveName by mutableStateOf<String?>(null)
    var currentArchivePath by mutableStateOf("")

    // Cache for current archive structure: Path -> List of Files
    var archiveCache: Map<String, List<UniversalFile>>? = null

    // Network provider state
    var currentNetworkProvider by mutableStateOf<StorageProvider?>(null)
    var currentNetworkId by mutableStateOf<String?>(null)
    var currentNetworkConnectionId by mutableStateOf<String?>(null)
    var networkError by mutableStateOf<String?>(null)

    // The processed and sorted list of files to display
    var files by mutableStateOf(emptyList<UniversalFile>())

    // Recycle bin metadata keyed by file name, populated only when in the recycle bin
    var recycleBinMetadata by mutableStateOf(emptyMap<String, RecycleBinMetadata>())

    // Loading state
    var isLoading by mutableStateOf(false)
    private var loadingJob: Job? = null

    // A stable key that only updates once the files for a directory have actually loaded.
    var loadedPathKey by mutableStateOf("initial")
        private set

    // Tracks if shift key is currently pressed (used for drag and drop logic)
    var isShiftPressed by mutableStateOf(false)

    // Tracks the last pointer type for drag logic
    var isMouseInteraction by mutableStateOf(false)

    // Y position (in ComposeView coordinates) of the active system drag, null when idle.
    // Every fileDropTarget writes here; FileContent reads it for edge auto-scroll.
    val activeDragY = mutableStateOf<Float?>(null)

    // Flag indicating if a system drag is currently active.
    val isSystemDragActive = mutableStateOf(false)

    // Flag for adding game shortcuts
    var isAddingGameShortcut by mutableStateOf(false)
    var isConfiguringGalleryFolders by mutableStateOf(false)
    var isConfiguringMusicFolders by mutableStateOf(false)

    // Centralized selection manager
    val selectionManager = SelectionManager()

    // Date formatter for consistent date display
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault())

    // Stack to track navigation hierarchy for SAF (Storage Access Framework)
    val safStack = mutableStateListOf<Uri>()

    var scrollToItemIndex by mutableStateOf<Int?>(null)
        internal set

    val backStack = mutableStateListOf<NavLocation>()
    val forwardStack = mutableStateListOf<NavLocation>()

    // Callback to open a new tab
    var onOpenInNewTab: ((UniversalFile) -> Unit)? = null

    var isSearchMode by mutableStateOf(false)
    var isSearchUIActive by mutableStateOf(false)
    var isAddressBarActive by mutableStateOf(false)

    val activeViewMode: ViewMode
        @Composable
        get() {
            return folderConfigs.viewMode
        }

    init {
        scope.launch(Dispatchers.IO) {
            val key = getCurrentStorageKey()
            withContext(Dispatchers.Main) {
                folderConfigs.resolveViewMode(key)
                folderConfigs.resolveSortParams(key)
                folderConfigs.resolveGridSize(key)
                folderConfigs.resolveColumnVisibility(key, libraryItem == LibraryItem.RecycleBin)
                folderConfigs.resolveColumnWidths(key)
            }
        }
        // Listen for global refresh events from other windows
        scope.launch {
            GlobalEvents.refreshEvent.collect {
                refresh()
            }
        }

        // Listen for config sync events (like favorite updates)
        scope.launch {
            GlobalEvents.configChangeEvent.collect {
                appConfigs.reload()
                // If a configuration changed (like hidden files toggle), refresh the list
                refresh()
            }
        }
    }


    fun getSafDisplayName(uri: Uri): String {
        if (uri.authority == "com.android.externalstorage.documents") {
            val docId = DocumentsContract.getTreeDocumentId(uri)
            val split = docId.split(":")
            val rootId = split[0]

            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                storageManager.storageVolumes.forEach { volume ->
                    val volumeUuid = volume.uuid ?: "primary"
                    if (volumeUuid == rootId) {
                        return volume.getDescription(context)
                    }
                }
            }
            return if (rootId == "primary") context.getString(R.string.nav_internal_storage) else rootId
        }

        val pm = context.packageManager
        val providerInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.resolveContentProvider(uri.authority!!, PackageManager.ComponentInfoFlags.of(0))
            } else {
                pm.resolveContentProvider(uri.authority!!, 0)
            }
        } catch (_: Exception) {
            null
        }

        val appName = providerInfo?.loadLabel(pm)?.toString() ?: ""
        if (appName.isNotEmpty()) return appName

        val doc = DocumentFile.fromTreeUri(context, currentSafUri!!)
        return doc?.name ?: context.getString(R.string.nav_external_location)
    }

    val currentName: String
        get() {
            if (currentNetworkProvider != null && currentNetworkId != null) {
                return currentNetworkProvider!!.displayName(currentNetworkId!!)
            }
            return if (libraryItem == LibraryItem.RecycleBin) {
                context.getString(R.string.nav_recycle_bin)
            } else if (libraryItem == LibraryItem.Recent) {
                context.getString(R.string.nav_recent)
            } else if (libraryItem == LibraryItem.Downloads) {
                context.getString(R.string.nav_downloads)
            } else if (libraryItem == LibraryItem.Games) {
                context.getString(R.string.nav_game_saves)
            } else if (libraryItem == LibraryItem.Documents) {
                context.getString(R.string.nav_documents)
            } else if (libraryItem == LibraryItem.Music) {
                val pathStr = currentPath?.path ?: ""
                val normalized = pathStr.replace("//", "/").removeSuffix("/")
                when {
                    normalized.endsWith("/music/songs") -> context.getString(R.string.audio)
                    normalized.endsWith("/music/albums") -> context.getString(R.string.menu_music_albums)
                    normalized.endsWith("/music/favourites") -> context.getString(R.string.menu_music_favourites)
                    normalized.endsWith("/music/playlists") -> context.getString(R.string.menu_music_playlists)
                    currentPath != null -> currentPath!!.name
                    else -> context.getString(R.string.nav_music)
                }
            } else if (libraryItem == LibraryItem.Gallery) {
                if (currentPath != null) currentPath!!.name
                else context.getString(R.string.nav_gallery)
            } else if (currentArchiveFile != null) {
                currentArchiveFile?.name ?: context.getString(R.string.archive)
            } else if (currentArchiveUri != null) {
                currentArchiveName ?: context.getString(R.string.archive)
            } else if (currentPath != null) {
                if (currentPath?.absolutePath == storageRoot.absolutePath) {
                    if (storageRoot.absolutePath == Environment.getExternalStorageDirectory().absolutePath) context.getString(R.string.nav_internal_storage)
                    else context.getString(R.string.nav_sd_card)
                }
                else currentPath?.name ?: context.getString(R.string.unknown)
            } else if (currentSafUri != null) {
                if (safStack.isNotEmpty()) {
                    val doc = DocumentFile.fromTreeUri(context, currentSafUri!!)
                    doc?.name ?: context.getString(R.string.nav_unknown_folder)
                } else {
                    getSafDisplayName(currentSafUri!!)
                }
            } else {
                context.getString(R.string.new_tab)
            }
        }

    val currentUniversalPath: UniversalFile?
        get() = when {
            currentArchiveFile != null -> currentArchiveFile?.toUniversal()

            currentArchiveUri != null -> {
                val doc = try {
                    DocumentFile.fromSingleUri(context, currentArchiveUri!!)
                } catch (_: Exception) {
                    null
                }
                doc?.toUniversal() ?: UniversalFile(
                    name = currentArchiveName ?: context.getString(R.string.archive),
                    isDirectory = false,
                    lastModified = 0L,
                    length = 0L,
                    provider = com.troikoss.continuum_explorer.providers.SafProvider,
                    providerId = currentArchiveUri.toString(),
                )
            }

            currentNetworkProvider != null && currentNetworkId != null -> UniversalFile(
                name = currentNetworkProvider!!.displayName(currentNetworkId!!),
                isDirectory = true,
                lastModified = 0L,
                length = 0L,
                provider = currentNetworkProvider!!,
                providerId = currentNetworkId!!,
            )

            currentSafUri != null -> {
                val doc = DocumentFile.fromTreeUri(context, currentSafUri!!)
                doc?.toUniversal()
            }

            currentPath != null -> currentPath?.toUniversal()

            libraryItem == LibraryItem.Gallery -> UniversalFile(
                name = if (currentPath != null) currentPath!!.name else context.getString(R.string.nav_gallery),
                isDirectory = true,
                lastModified = 0L,
                length = 0L,
                provider = LocalProvider,
                providerId = currentPath?.absolutePath ?: "virtual://gallery",
            )

            libraryItem == LibraryItem.Music -> {
                val pathStr = currentPath?.path ?: ""
                val normalized = pathStr.replace("//", "/").removeSuffix("/")
                
                val isSongs = normalized.endsWith("/music/songs")
                val isAlbumsRoot = normalized.endsWith("/music/albums")
                val isFavourites = normalized.endsWith("/music/favourites")
                val isPlaylists = normalized.endsWith("/music/playlists")
                val isSpecificAlbum = normalized.contains("/music/albums/") || pathStr.contains("#album:")
                
                val albumName = when {
                    pathStr.contains("#album:") -> pathStr.substringAfterLast("#album:")
                    isSpecificAlbum -> pathStr.substringAfterLast("/")
                    else -> currentPath?.name ?: ""
                }
                
                UniversalFile(
                    name = when {
                        isSongs -> context.getString(R.string.audio)
                        isAlbumsRoot -> context.getString(R.string.menu_music_albums)
                        isFavourites -> "Favourites"
                        isPlaylists -> "Playlists"
                        isSpecificAlbum -> albumName
                        currentPath != null -> currentPath!!.name
                        else -> context.getString(R.string.nav_music)
                    },
                    isDirectory = true,
                    lastModified = 0L,
                    length = 0L,
                    provider = LocalProvider,
                    providerId = pathStr.ifEmpty { "virtual://music" },
                    mimeType = if (isSpecificAlbum) "album" else null
                )
            }

            libraryItem == LibraryItem.Recent -> UniversalFile(
                name = context.getString(R.string.nav_recent),
                isDirectory = true,
                lastModified = 0L,
                length = 0L,
                provider = LocalProvider,
                providerId = "virtual://recent"
            )

            libraryItem == LibraryItem.Downloads -> UniversalFile(
                name = context.getString(R.string.nav_downloads),
                isDirectory = true,
                lastModified = 0L,
                length = 0L,
                provider = LocalProvider,
                providerId = "virtual://downloads"
            )

            libraryItem == LibraryItem.Documents -> UniversalFile(
                name = context.getString(R.string.nav_documents),
                isDirectory = true,
                lastModified = 0L,
                length = 0L,
                provider = LocalProvider,
                providerId = "virtual://documents"
            )

            libraryItem == LibraryItem.Games -> UniversalFile(
                name = context.getString(R.string.nav_game_saves),
                isDirectory = true,
                lastModified = 0L,
                length = 0L,
                provider = LocalProvider,
                providerId = "virtual://games_manager"
            )

            else -> null
        }

    val canGoUp: Boolean
        get() = if (currentNetworkProvider != null && currentNetworkId != null) {
            currentNetworkProvider!!.parentId(currentNetworkId!!) != null
        } else if (currentArchiveFile != null || currentArchiveUri != null) {
            true
        } else if (libraryItem != LibraryItem.None) {
            (libraryItem == LibraryItem.Gallery || libraryItem == LibraryItem.Music) && currentPath != null
        } else if (currentPath != null) {
            currentPath?.absolutePath != storageRoot.absolutePath
        } else if (currentSafUri != null) {
            safStack.isNotEmpty()
        } else {
            false
        }

    val canGoBack: Boolean
        get() = backStack.isNotEmpty()

    val canGoForward: Boolean
        get() = forwardStack.isNotEmpty()

    fun onScrollToItemCompleted() {
        scrollToItemIndex = null
    }

    fun refresh(): Job? {
        if (isSearchMode) {
            isSearchMode = false
        }
        return triggerLoad(forceRefresh = true)
    }

    fun performSearch(query: String, searchSubfolders: Boolean) {
        if (query.isBlank()) {
            if (isSearchMode) {
                isSearchMode = false
                triggerLoad()
            }
            return
        }

        loadingJob?.cancel()
        loadingJob = scope.launch {
            val sortParams = folderConfigs.sortParams
            val showHidden = SettingsManager.showHiddenFiles.value

            withContext(Dispatchers.Main) {
                isSearchMode = true
                isLoading = true
                files = emptyList() // Clear previous results immediately
                val key = getCurrentStorageKey()
                folderConfigs.resolveViewMode(key)
                folderConfigs.resolveSortParams(key)
                folderConfigs.resolveGridSize(key)
                loadedPathKey = key ?: context.getString(R.string.msg_search_results)
            }
            try {
                // Parse if we need to load archives
                if ((currentArchiveFile != null || currentArchiveUri != null) && archiveCache == null) {
                    val source: Any = currentArchiveFile ?: currentArchiveUri!!
                    archiveCache = withContext(Dispatchers.IO) { ZipUtils.parseArchive(context, source, currentArchiveName) }
                }

                val results = SearchManager.search(
                    context = context,
                    query = query,
                    currentPath = currentPath,
                    currentSafUri = currentSafUri,
                    currentNetworkProvider = currentNetworkProvider,
                    currentNetworkId = currentNetworkId,
                    searchSubfolders = searchSubfolders,
                    archiveCache = archiveCache,
                    currentArchivePath = currentArchivePath
                )

                val filtered = results.filter { if (showHidden) true else !it.name.startsWith(".") }

                val sorted = withContext(Dispatchers.IO) {
                    sortFiles(filtered, sortParams)
                }

                withContext(Dispatchers.Main) {
                    files = sorted
                    selectionManager.allFiles = files
                    isLoading = false
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    Toast.makeText(context, context.getString(R.string.msg_search_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun handleSafResult(uri: Uri?) {
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)

            if (isAddingGameShortcut) {
                val decodedUri = Uri.decode(uri.toString())
                val packageName = IconHelper.getPackageNameFromPath(decodedUri)
                val appName = packageName?.let { IconHelper.getAppName(context, it) }
                appConfigs.addGameSafUri(uri, appName)
                isAddingGameShortcut = false
                refresh()
            } else if (isConfiguringGalleryFolders) {
                val path = SafUtils.getRawPathFromUri(context, uri)
                if (path != null) {
                    val currentFolders = SettingsManager.galleryFolders.value.toMutableSet()
                    currentFolders.add(path)
                    SettingsManager.setGalleryFolders(context, currentFolders)
                }
                isConfiguringGalleryFolders = false
                refresh()
            } else if (isConfiguringMusicFolders) {
                val path = SafUtils.getRawPathFromUri(context, uri)
                if (path != null) {
                    val currentFolders = SettingsManager.musicFolders.value.toMutableSet()
                    currentFolders.add(path)
                    SettingsManager.setMusicFolders(context, currentFolders)
                }
                isConfiguringMusicFolders = false
                refresh()
            } else {
                if (!appConfigs.addedSafUris.contains(uri)) {
                    appConfigs.addedSafUris.add(uri)
                    appConfigs.saveAddedSafUris()
                }

                safStack.clear()
                navigateTo(null, uri)
            }
        } else {
            isAddingGameShortcut = false
            isConfiguringGalleryFolders = false
            isConfiguringMusicFolders = false
        }
    }

    fun removeSafUri(uri: Uri) {
        if (appConfigs.addedSafUris.contains(uri)) {
            appConfigs.addedSafUris.remove(uri)
            appConfigs.saveAddedSafUris()

            if (currentSafUri == uri || (currentSafUri != null && currentSafUri.toString().startsWith(uri.toString()))) {
                navigateTo(Environment.getExternalStorageDirectory(), null)
            }

            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.releasePersistableUriPermission(uri, flags)
            } catch (_: Exception) {}
        }
    }

    fun triggerLoad(forceRefresh: Boolean = false): Job? {
        if (isSearchMode) return null
        loadingJob?.cancel()
        val job = scope.launch {
            loadFiles(forceRefresh)
        }
        loadingJob = job
        return job
    }

    fun cancelLoading() {
        loadingJob?.cancel()
        isLoading = false
    }

    internal var pendingFocusPath: File? = null
    internal var pendingFocusUri: Uri? = null

    private suspend fun loadFiles(forceRefresh: Boolean = false) {
        isLoading = true
        if (currentArchiveFile == null && currentArchiveUri == null) {
            archiveCache = null
        }
        if (forceRefresh) {
            archiveCache = null
        }

        // Resolve sort params early so files are sorted correctly during IO work.
        // ViewMode/gridSize/columnVisibility are resolved together with the file list
        // assignment so they never change before the new list is visible.
        val key = getCurrentStorageKey()
        folderConfigs.resolveSortParams(key)

        val sortParams = folderConfigs.sortParams
        val showHidden = SettingsManager.showHiddenFiles.value

        try {
            val (sortedList, newMeta) = withContext(Dispatchers.IO) {
                val universalList = when (libraryItem) {
                    LibraryItem.Recent -> RecentFilesManager.getRecentFiles(context)
                    LibraryItem.Downloads -> DownloadsManager.getDownloadsFiles(context)
                    LibraryItem.Documents -> DocumentsManager.getDocumentsFiles(context)
                    LibraryItem.Games -> GamesManager.getGames(context, appConfigs)
                    LibraryItem.Gallery -> when {
                        currentPath != null -> GalleryManager.getAlbumContents(context, currentPath!!.absolutePath)
                        appConfigs.isGalleryAlbumsEnabled -> GalleryManager.getGalleryAlbums(context, if (SettingsManager.isGalleryFilterEnabled.value) SettingsManager.galleryFolders.value else emptySet())
                        else -> GalleryManager.getGalleryFiles(context, if (SettingsManager.isGalleryFilterEnabled.value) SettingsManager.galleryFolders.value else emptySet())
                    }
                    LibraryItem.Music -> {
                        val pathStr = currentPath?.path ?: ""
                        val normalized = pathStr.replace("//", "/").removeSuffix("/")
                        when {
                            currentPath == null -> listOf(
                                UniversalFile("Songs", true, 0, 0, LocalProvider, "virtual://music/songs", "virtual://music"),
                                UniversalFile("Albums", true, 0, 0, LocalProvider, "virtual://music/albums", "virtual://music"),
                                UniversalFile("Favourites", true, 0, 0, LocalProvider, "virtual://music/favourites", "virtual://music"),
                                UniversalFile("Playlists", true, 0, 0, LocalProvider, "virtual://music/playlists", "virtual://music")
                            )
                            normalized.endsWith("/music/songs") -> MusicMetadataManager.getSongs(context)
                            normalized.endsWith("/music/albums") -> MusicMetadataManager.getAlbums(context)
                            normalized.endsWith("/music/favourites") -> MusicMetadataManager.getFavourites(context)
                            normalized.endsWith("/music/playlists") -> emptyList()
                            normalized.contains("/music/albums/") -> {
                                val albumName = pathStr.substringAfterLast("/")
                                MusicMetadataManager.getSongsForAlbum(context, albumName)
                            }
                            pathStr.contains("#album:") -> {
                                val albumName = pathStr.substringAfterLast("#album:")
                                MusicMetadataManager.getSongsForAlbum(context, albumName)
                            }
                            else -> MusicManager.getMusicAlbumContents(context, currentPath!!.absolutePath)
                        }
                    }
                    LibraryItem.RecycleBin -> if (currentPath != null) {
                        val trashRoot = File(Environment.getExternalStorageDirectory(), ".Trash")
                        if (currentPath!!.absolutePath == trashRoot.absolutePath) {
                            migrateLegacyTrash(trashRoot)
                            val uuidDirs = trashRoot.listFiles()?.filter { it.name != ".metadata" && it.isDirectory } ?: emptyList()
                            uuidDirs.mapNotNull { it.listFiles()?.firstOrNull()?.toUniversal() }
                        } else {
                            val rawFiles = currentPath!!.listFiles()?.toList() ?: emptyList()
                            rawFiles.filter { it.name != ".metadata" }.map { it.toUniversal() }
                        }
                    } else emptyList()
                    LibraryItem.None -> if (currentArchiveFile != null || currentArchiveUri != null) {
                        if (archiveCache == null) {
                            val source: Any = currentArchiveFile ?: currentArchiveUri!!
                            archiveCache = ZipUtils.parseArchive(context, source, currentArchiveName)
                        }
                        archiveCache?.get(currentArchivePath) ?: emptyList()
                    } else if (currentNetworkProvider != null && currentNetworkId != null) {
                        try {
                            val result = currentNetworkProvider!!.listChildren(currentNetworkId!!)
                            withContext(Dispatchers.Main) { networkError = null }
                            result
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) { networkError = e.message ?: "Network error" }
                            emptyList()
                        }
                    } else if (currentPath != null) {
                        LocalProvider.listChildren(currentPath!!.absolutePath)
                    } else if (currentSafUri != null) {
                        val docFile = DocumentFile.fromTreeUri(context, currentSafUri!!)
                        val rawDocs = docFile?.listFiles()?.toList() ?: emptyList()
                        rawDocs.map { it.toUniversal() }
                    } else {
                        emptyList()
                    }
                }

                val meta: Map<String, RecycleBinMetadata> = if (libraryItem == LibraryItem.RecycleBin) {
                    universalList.associate { file ->
                        val uuidKey = file.fileRef?.parentFile?.name ?: file.name
                        uuidKey to RecycleBinMetadata(
                            deletedAt = getDeletedAt(uuidKey),
                            deletedFrom = getDeletedFrom(uuidKey)
                        )
                    }
                } else emptyMap()

                val filteredList = if (showHidden) universalList else universalList.filter { !it.name.startsWith(".") }

                val skipSort = libraryItem == LibraryItem.Recent || (libraryItem == LibraryItem.Gallery && !appConfigs.isGalleryAlbumsEnabled) || (libraryItem == LibraryItem.Music && !appConfigs.isMusicAlbumsEnabled)
                Pair(if (skipSort) filteredList else sortFiles(filteredList, sortParams, meta), meta)
            }

            withContext(Dispatchers.Main) {
                folderConfigs.resolveViewMode(key)
                folderConfigs.resolveGridSize(key)
                folderConfigs.resolveColumnVisibility(key, libraryItem == LibraryItem.RecycleBin)
                folderConfigs.resolveColumnWidths(key)
                recycleBinMetadata = newMeta
                files = sortedList

                if (pendingFocusPath != null || pendingFocusUri != null) {
                    val itemToFind = files.find { item ->
                        if (pendingFocusPath != null) {
                            item.fileRef?.absolutePath == pendingFocusPath?.absolutePath
                        } else {
                            item.documentFileRef?.uri == pendingFocusUri
                        }
                    }

                    if (itemToFind != null) {
                        selectionManager.setFocus(itemToFind)
                        val index = files.indexOf(itemToFind)
                        if (index != -1) {
                            scrollToItemIndex = index
                        }
                    }
                    pendingFocusPath = null
                    pendingFocusUri = null
                }

                loadedPathKey = key ?: when (libraryItem) {
                    LibraryItem.Recent -> "recent"
                    LibraryItem.Gallery -> "gallery"
                    LibraryItem.Music -> "music"
                    LibraryItem.Downloads -> "downloads"
                    LibraryItem.Documents -> "documents"
                    LibraryItem.Games -> "games_manager"
                    LibraryItem.None -> "root"
                    LibraryItem.RecycleBin -> "trash"
                }
                selectionManager.allFiles = files
                isLoading = false
            }
        } catch (e: RestrictedAccessException) {
            withContext(Dispatchers.Main) {
                isLoading = false
                NotificationHelper.showErrorNotification(
                    context,
                    context.getString(R.string.error_restricted_access),
                    context.getString(R.string.msg_restricted_access_shizuku)
                )
            }
        } catch (_: Exception) {
            withContext(Dispatchers.Main) {
                isLoading = false
            }
        }
    }

    private fun sortFiles(
        rawList: List<UniversalFile>,
        params: SortParams,
        meta: Map<String, RecycleBinMetadata> = emptyMap()
    ): List<UniversalFile> {
        return rawList.sortedWith { f1, f2 ->
            if (f1.isDirectory && !f2.isDirectory) return@sortedWith -1
            if (!f1.isDirectory && f2.isDirectory) return@sortedWith 1

            val result = when (params.columnType) {
                FileColumnType.NAME -> f1.name.lowercase().compareTo(f2.name.lowercase())
                FileColumnType.DATE -> f1.lastModified.compareTo(f2.lastModified)
                FileColumnType.SIZE -> f1.length.compareTo(f2.length)
                FileColumnType.TYPE -> {
                    val type1 = getFileType(f1, context)
                    val type2 = getFileType(f2, context)
                    type1.compareTo(type2)
                }
                FileColumnType.DATE_DELETED -> {
                    val key1 = f1.fileRef?.parentFile?.name ?: f1.name
                    val key2 = f2.fileRef?.parentFile?.name ?: f2.name
                    (meta[key1]?.deletedAt ?: 0L).compareTo(meta[key2]?.deletedAt ?: 0L)
                }
                FileColumnType.DELETED_FROM -> {
                    val key1 = f1.fileRef?.parentFile?.name ?: f1.name
                    val key2 = f2.fileRef?.parentFile?.name ?: f2.name
                    (meta[key1]?.deletedFrom ?: "").compareTo(meta[key2]?.deletedFrom ?: "")
                }
            }
            if (params.order == SortOrder.Ascending) result else -result
        }
    }


    fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        calendar.time = date
        val dateYear = calendar.get(Calendar.YEAR)

        val pattern = if (currentYear == dateYear) "dd.MM HH:mm" else "dd.MM.yyyy HH:mm"
        val formatter = SimpleDateFormat(pattern, Locale.getDefault())
        return formatter.format(date)
    }
    //fun formatDateUS(timestamp: Long): String = dateFormatter.format(timestamp)
    fun formatSize(size: Long): String = Formatter.formatFileSize(context, size)

    fun getCurrentStorageKey(): String? {
        if (isSearchMode) return context.getString(R.string.msg_search_results)
        if (currentNetworkProvider != null && currentNetworkId != null) return currentNetworkId
        return if (currentArchiveFile != null || currentArchiveUri != null) {
            val base = currentArchiveFile?.absolutePath ?: currentArchiveUri.toString()
            "archive:$base:${currentArchivePath.removeSuffix("/")}"
        } else if (libraryItem == LibraryItem.Gallery) {
            if (currentPath != null) "virtual://gallery_album:${currentPath!!.absolutePath}"
            else "virtual://gallery"
        } else if (libraryItem == LibraryItem.Music) {
            val pathStr = currentPath?.path ?: ""
            val normalized = pathStr.replace("//", "/").removeSuffix("/")
            when {
                normalized.endsWith("/music/songs") -> "virtual://music/songs"
                normalized.endsWith("/music/albums") -> "virtual://music/albums"
                normalized.endsWith("/music/favourites") -> "virtual://music/favourites"
                normalized.endsWith("/music/playlists") -> "virtual://music/playlists"
                normalized.contains("/music/albums/") || pathStr.contains("#album:") -> "virtual://music_album:${currentPath!!.absolutePath}"
                else -> "virtual://music"
            }
        } else if (libraryItem == LibraryItem.Recent) {
            "virtual://recent"
        } else if (libraryItem == LibraryItem.Downloads) {
            "virtual://downloads"
        } else if (libraryItem == LibraryItem.Documents) {
            "virtual://documents"
        } else if (libraryItem == LibraryItem.Games) {
            "virtual://games_manager"
        } else if (libraryItem == LibraryItem.RecycleBin) {
            "virtual://recycle_bin"
        } else if (currentPath != null) {
            currentPath?.absolutePath
        } else if (currentSafUri != null) {
            currentSafUri.toString()
        } else {
            null
        }
    }

    @Composable
    fun getScreenSize(): ScreenSize? {
        val configuration = LocalConfiguration.current

        val screenWidth = configuration.screenWidthDp

        return when {
            screenWidth > 1000 ->ScreenSize.LARGE
            screenWidth > 600 -> ScreenSize.MEDIUM
            screenWidth <= 600 -> ScreenSize.SMALL
            else -> null
        }
    }

    @Composable
    fun getUIAppearance(): UIAppearance {
        val configuration = LocalConfiguration.current
        val isMulti = try { (context as? android.app.Activity)?.isInMultiWindowMode == true } catch (_: Exception) { false }
        val isDeX = configuration.toString().contains("dexMode", ignoreCase = true)

        return when {
            isDeX || isMulti -> UIAppearance.WINDOWED
            configuration.smallestScreenWidthDp >= 600 -> UIAppearance.TABLET
            else -> UIAppearance.PHONE
        }
    }
}
