package com.troikoss.continuum_explorer.utils

import android.content.Context
import android.net.Uri
import androidx.compose.material.icons.Icons
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.troikoss.continuum_explorer.R
import com.troikoss.continuum_explorer.managers.SettingsManager
import com.troikoss.continuum_explorer.model.*
import com.troikoss.continuum_explorer.providers.StorageProviders

/**
 * Manages configuration specific to folders (view mode, sort order, etc.)
 */
class FolderConfigurations(private val context: Context) {
    var sortParams by mutableStateOf(SortParams(FileColumnType.NAME, SortOrder.Descending))
        private set
    
    var viewMode by mutableStateOf(ViewMode.DETAILS)
        private set
        
    var gridItemSize by mutableIntStateOf(100)
    var detailsItemSize by mutableIntStateOf(24)
    var contentItemSize by mutableIntStateOf(40)

    val extraColumns = listOf(
        FileColumnDefinition(FileColumnType.DATE, context.getString(R.string.details_header_date), initialWidth = 135.dp),
        FileColumnDefinition(FileColumnType.DATE_DELETED, context.getString(R.string.details_header_date_deleted), initialWidth = 135.dp),
        FileColumnDefinition(FileColumnType.DELETED_FROM, context.getString(R.string.details_header_deleted_from), initialWidth = 160.dp),
        FileColumnDefinition(FileColumnType.TYPE, context.getString(R.string.details_header_type), initialWidth = 110.dp),
        FileColumnDefinition(FileColumnType.SIZE, context.getString(R.string.details_header_size), initialWidth = 90.dp)
    )

    val hiddenColumns = mutableStateSetOf<FileColumnType>()

    val visibleColumns: List<FileColumnDefinition>
        get() = extraColumns.filter { it.type !in hiddenColumns }

    fun toggleColumnVisibility(type: FileColumnType, key: String?) {
        if (type in hiddenColumns) hiddenColumns.remove(type) else hiddenColumns.add(type)
        saveColumnVisibility(key)
    }

    fun resolveColumnVisibility(key: String?, isInRecycleBin: Boolean) {
        val prefs = context.getSharedPreferences("column_visibility", Context.MODE_PRIVATE)
        val savedHidden = prefs.getStringSet(key ?: "default", null)
        hiddenColumns.clear()
        if (savedHidden != null) {
            savedHidden.forEach { typeName ->
                try { hiddenColumns.add(FileColumnType.valueOf(typeName)) } catch (_: Exception) {}
            }
        } else {
            // Apply defaults: recycle bin shows DATE_DELETED/DELETED_FROM and hides DATE; elsewhere the reverse
            if (isInRecycleBin) {
                hiddenColumns.add(FileColumnType.DATE)
            } else {
                hiddenColumns.add(FileColumnType.DATE_DELETED)
                hiddenColumns.add(FileColumnType.DELETED_FROM)
            }
        }
    }

    private fun saveColumnVisibility(key: String?) {
        val prefs = context.getSharedPreferences("column_visibility", Context.MODE_PRIVATE)
        prefs.edit().putStringSet(key ?: "default", hiddenColumns.map { it.name }.toSet()).apply()
    }

    val columnWidths = mutableStateMapOf<FileColumnType, Dp>().apply {
        put(FileColumnType.NAME, 300.dp)
        extraColumns.forEach { put(it.type, it.initialWidth) }
    }

    fun saveColumnWidths(key: String?) {
        val prefs = context.getSharedPreferences("column_widths", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val prefKey = key ?: "default"
        columnWidths.forEach { (type, width) ->
            editor.putFloat("${prefKey}:${type.name}", width.value)
        }
        editor.apply()
    }

    fun resolveColumnWidths(key: String?) {
        val prefs = context.getSharedPreferences("column_widths", Context.MODE_PRIVATE)
        val prefKey = key ?: "default"
        // Always reset to defaults first so widths don't bleed from a previous folder
        columnWidths[FileColumnType.NAME] = 300.dp
        extraColumns.forEach { col -> columnWidths[col.type] = col.initialWidth }
        // Then overlay any saved values for this folder
        val nameKey = "${prefKey}:${FileColumnType.NAME.name}"
        if (prefs.contains(nameKey)) {
            columnWidths[FileColumnType.NAME] = prefs.getFloat(nameKey, 300f).dp
            extraColumns.forEach { col ->
                val k = "${prefKey}:${col.type.name}"
                if (prefs.contains(k)) {
                    columnWidths[col.type] = prefs.getFloat(k, col.initialWidth.value).dp
                }
            }
        }
    }

    fun updateViewMode(mode: ViewMode, key: String?) {
        if (viewMode != mode) {
            viewMode = mode
            if (key != null) {
                saveViewModeForCurrentPath(mode, key)
            }
        }
    }

    private fun saveViewModeForCurrentPath(mode: ViewMode, key: String) {
        val prefs = context.getSharedPreferences("folder_view_modes", Context.MODE_PRIVATE)
        prefs.edit().putString(key, mode.name).apply()
    }

    fun resolveViewMode(key: String?) {
        val prefs = context.getSharedPreferences("folder_view_modes", Context.MODE_PRIVATE)
        if (key != null) {
            val saved = prefs.getString(key, null)
            if (saved != null) {
                try {
                    updateViewMode(ViewMode.valueOf(saved), null)
                    return
                } catch (_: Exception) {}
            }
        }
        val fallback = when {
            key == "virtual://gallery" || key?.startsWith("virtual://gallery_album:") == true -> ViewMode.GALLERY
            key == "virtual://music" || key?.startsWith("virtual://music/") == true || key?.startsWith("virtual://music_album:") == true || key?.startsWith("virtual://playlist:") == true -> ViewMode.MUSIC
            else -> SettingsManager.defaultViewMode.value
        }
        updateViewMode(fallback, null)
    }

    fun toggleSort(columnType: FileColumnType, key: String?, onSortChanged: () -> Unit) {
        val isSameColumn = sortParams.columnType == columnType
        val newOrder = if (isSameColumn) {
            if (sortParams.order == SortOrder.Ascending) SortOrder.Descending else SortOrder.Ascending
        } else {
            SortOrder.Ascending
        }
        updateSortParams(SortParams(columnType, newOrder), key)
        onSortChanged()
    }
    
    fun updateSortParams(params: SortParams, key: String?) {
        if (sortParams != params) {
            sortParams = params
            if (key != null) {
                saveSortParamsForCurrentPath(params, key)
            }
        }
    }

    private fun saveSortParamsForCurrentPath(params: SortParams, key: String) {
        val prefs = context.getSharedPreferences("folder_sort_params", Context.MODE_PRIVATE)
        val value = "${params.columnType.name}:${params.order.name}"
        prefs.edit().putString(key, value).apply()
    }

    fun resolveSortParams(key: String?) {
        val prefs = context.getSharedPreferences("folder_sort_params", Context.MODE_PRIVATE)
        fun parseParams(value: String): SortParams? {
            return try {
                val split = value.split(":")
                SortParams(FileColumnType.valueOf(split[0]), SortOrder.valueOf(split[1]))
            } catch (_: Exception) { null }
        }

        if (key != null) {
            val saved = prefs.getString(key, null)
            if (saved != null) {
                val params = parseParams(saved)
                if (params != null) {
                    updateSortParams(params, null)
                    return
                }
            }
        }
        val default = when (key) {
            "virtual://recent" -> SortParams(FileColumnType.DATE, SortOrder.Descending)
            "virtual://recycle_bin" -> SortParams(FileColumnType.DATE_DELETED, SortOrder.Descending)
            else -> SortParams(FileColumnType.NAME, SortOrder.Ascending)
        }
        updateSortParams(default, null)
    }

    @Composable
    fun SortArrow(type: FileColumnType) {
        if (sortParams.columnType == type) {
            Icon(
                imageVector = if (sortParams.order == SortOrder.Ascending) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    fun updateGridSize(newSize: Int, key: String?) {
        gridItemSize = newSize
        if (key != null) {
            saveGridSizeForCurrentPath(newSize, key)
        }
    }

    fun updateDetailsSize(newSize: Int, key: String?) {
        detailsItemSize = newSize
        if (key != null) {
            saveDetailsSizeForCurrentPath(newSize, key)
        }
    }

    fun updateContentSize(newSize: Int, key: String?) {
        contentItemSize = newSize
        if (key != null) {
            saveContentSizeForCurrentPath(newSize, key)
        }
    }

    private fun saveGridSizeForCurrentPath(size: Int, key: String) {
        val prefs = context.getSharedPreferences("folder_grid_sizes", Context.MODE_PRIVATE)
        prefs.edit().putInt(key, size).apply()
    }

    private fun saveDetailsSizeForCurrentPath(size: Int, key: String) {
        val prefs = context.getSharedPreferences("folder_details_sizes", Context.MODE_PRIVATE)
        prefs.edit().putInt(key, size).apply()
    }

    private fun saveContentSizeForCurrentPath(size: Int, key: String) {
        val prefs = context.getSharedPreferences("folder_content_sizes", Context.MODE_PRIVATE)
        prefs.edit().putInt(key, size).apply()
    }

    fun resolveGridSize(key: String?) {
        val gridPrefs = context.getSharedPreferences("folder_grid_sizes", Context.MODE_PRIVATE)
        val detailsPrefs = context.getSharedPreferences("folder_details_sizes", Context.MODE_PRIVATE)
        val contentPrefs = context.getSharedPreferences("folder_content_sizes", Context.MODE_PRIVATE)
        
        if (key != null) {
            gridItemSize = gridPrefs.getInt(key, 100)
            detailsItemSize = detailsPrefs.getInt(key, 24)
            contentItemSize = contentPrefs.getInt(key, 40)
            return
        }
        gridItemSize = 100
        detailsItemSize = 24
        contentItemSize = 40
    }


}






/**
 * Manages global application configurations (favorites, SAF roots, library layout)
 */
class AppConfigurations(private val context: Context) {
    val addedSafUris = mutableStateListOf<Uri>()
    val gameSafShortcuts = mutableStateListOf<GameShortcut>()
    val favoritePaths = mutableStateListOf<String>()
    val favoriteNames = mutableStateMapOf<String, String>()
    val safNames = mutableStateMapOf<String, String>()
    val libraryOrder = mutableStateListOf("home", "gallery", "music", "recent", "downloads", "documents", "archives", "apks", "games_manager", "trash")
    val networkConnections = mutableStateListOf<NetworkConnection>()
    var isRecentVisible by mutableStateOf(true)
    var isGalleryVisible by mutableStateOf(true)
    var isMusicVisible by mutableStateOf(false)
    var isDownloadsVisible by mutableStateOf(false)
    var isDocumentsVisible by mutableStateOf(true)
    var isArchivesVisible by mutableStateOf(false)
    var isApksVisible by mutableStateOf(false)
    var isGamesVisible by mutableStateOf(false)
    var isTrashVisible by mutableStateOf(true)
    var isGalleryAlbumsEnabled by mutableStateOf(false)
    var isMusicAlbumsEnabled by mutableStateOf(false)
    var isDocumentsFolderEnabled by mutableStateOf(false)

    var navPaneWidth by mutableStateOf(240.dp)
    var detailsPaneWidth by mutableStateOf(240.dp)

    init {
        reload()
    }

    fun reload() {
        loadAddedSafUris()
        loadGameSafUris()
        loadFavorites()
        loadLibrarySettings()
        loadPaneWidths()
        loadNetworkConnections()

        // Ensure all library items are present in the order list
        val required = listOf("home", "gallery", "music", "recent", "downloads", "documents", "archives", "apks", "games_manager", "trash")
        required.forEach { id ->
            if (!libraryOrder.contains(id)) {
                if (id == "home") {
                    libraryOrder.add(0, id)
                } else if (id == "music") {
                    val idx = libraryOrder.indexOf("gallery")
                    libraryOrder.add(if (idx != -1) idx + 1 else 0, id)
                } else if (id == "downloads") {
                    val idx = libraryOrder.indexOf("recent")
                    libraryOrder.add(if (idx != -1) idx + 1 else libraryOrder.size, id)
                } else if (id == "documents") {
                    val idx = libraryOrder.indexOf("downloads")
                    libraryOrder.add(if (idx != -1) idx + 1 else libraryOrder.size, id)
                } else if (id == "archives") {
                    val idx = libraryOrder.indexOf("documents")
                    libraryOrder.add(if (idx != -1) idx + 1 else libraryOrder.size, id)
                } else if (id == "apks") {
                    val idx = libraryOrder.indexOf("archives")
                    libraryOrder.add(if (idx != -1) idx + 1 else libraryOrder.size, id)
                } else if (id == "games_manager") {
                    val idx = libraryOrder.indexOf("apks")
                    libraryOrder.add(if (idx != -1) idx + 1 else libraryOrder.size, id)
                } else {
                    libraryOrder.add(id)
                }
            }
        }
    }

    fun savePaneWidths() {
        val prefs = context.getSharedPreferences("pane_widths", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putFloat("nav_width", navPaneWidth.value)
            putFloat("details_width", detailsPaneWidth.value)
        }.apply()
    }

    private fun loadPaneWidths() {
        val prefs = context.getSharedPreferences("pane_widths", Context.MODE_PRIVATE)
        navPaneWidth = prefs.getFloat("nav_width", 240f).dp
        detailsPaneWidth = prefs.getFloat("details_width", 240f).dp
    }

    private fun loadAddedSafUris() {
        val prefs = context.getSharedPreferences("saf_storage", Context.MODE_PRIVATE)
        addedSafUris.clear()
        safNames.clear()
        
        // Filter out URIs for which we no longer have permission (e.g. app uninstalled)
        val persistedPermissions = context.contentResolver.persistedUriPermissions
        val validUris = persistedPermissions.map { it.uri.toString() }.toSet()

        val ordered = prefs.getString("ordered_uris", null)
        if (ordered != null) {
            if (ordered.isNotEmpty()) {
                ordered.split("|").forEach { uriString ->
                    if (validUris.contains(uriString)) {
                        addedSafUris.add(Uri.parse(uriString))
                    }
                }
            }
        } else {
            // Legacy: migrate from unordered Set
            val uris = prefs.getStringSet("uris", emptySet()) ?: emptySet()
            uris.forEach { uriString ->
                if (validUris.contains(uriString)) {
                    addedSafUris.add(Uri.parse(uriString))
                }
            }
            saveAddedSafUris()
        }

        val namesJson = prefs.getString("custom_names", null)
        if (namesJson != null) {
            try {
                val obj = JSONObject(namesJson)
                obj.keys().forEach { key ->
                    safNames[key] = obj.getString(key)
                }
            } catch (_: Exception) {}
        }
    }

    fun saveAddedSafUris() {
        val prefs = context.getSharedPreferences("saf_storage", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("ordered_uris", addedSafUris.joinToString("|") { it.toString() })
        
        val namesObj = JSONObject()
        safNames.forEach { (uri, name) -> namesObj.put(uri, name) }
        editor.putString("custom_names", namesObj.toString())
        editor.apply()
    }

    private fun loadGameSafUris() {
        val prefs = context.getSharedPreferences("game_saf_storage", Context.MODE_PRIVATE)
        gameSafShortcuts.clear()

        val persistedPermissions = context.contentResolver.persistedUriPermissions
        val validUris = persistedPermissions.map { it.uri.toString() }.toSet()

        val jsonStr = prefs.getString("shortcuts_json", null)
        if (jsonStr != null && jsonStr.isNotEmpty()) {
            try {
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val uriString = obj.getString("uri")
                    if (validUris.contains(uriString)) {
                        gameSafShortcuts.add(GameShortcut(
                            uri = Uri.parse(uriString),
                            name = if (obj.has("name")) obj.getString("name") else null
                        ))
                    }
                }
            } catch (_: Exception) {}
        } else {
            // Migration from simple URI list
            val ordered = prefs.getString("ordered_uris", null)
            if (ordered != null && ordered.isNotEmpty()) {
                ordered.split("|").forEach { uriString ->
                    if (validUris.contains(uriString)) {
                        gameSafShortcuts.add(GameShortcut(Uri.parse(uriString)))
                    }
                }
                saveGameSafUris()
            }
        }
    }

    fun saveGameSafUris() {
        val prefs = context.getSharedPreferences("game_saf_storage", Context.MODE_PRIVATE)
        val arr = JSONArray()
        gameSafShortcuts.forEach { shortcut ->
            val obj = JSONObject()
            obj.put("uri", shortcut.uri.toString())
            shortcut.name?.let { obj.put("name", it) }
            arr.put(obj)
        }
        prefs.edit().putString("shortcuts_json", arr.toString()).apply()
    }

    fun addGameSafUri(uri: Uri, name: String? = null) {
        if (gameSafShortcuts.none { it.uri == uri }) {
            gameSafShortcuts.add(GameShortcut(uri, name))
            saveGameSafUris()
            GlobalEvents.triggerConfigUpdate()
        }
    }

    fun removeGameSafUri(uri: Uri) {
        if (gameSafShortcuts.removeAll { it.uri == uri }) {
            saveGameSafUris()
            GlobalEvents.triggerConfigUpdate()
        }
    }

    fun renameGameShortcut(uri: Uri, newName: String) {
        val index = gameSafShortcuts.indexOfFirst { it.uri == uri }
        if (index != -1) {
            gameSafShortcuts[index] = gameSafShortcuts[index].copy(name = newName)
            saveGameSafUris()
            GlobalEvents.triggerConfigUpdate()
        }
    }

    fun moveSafUri(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val item = addedSafUris.removeAt(fromIndex)
        addedSafUris.add(toIndex, item)
        saveAddedSafUris()
        GlobalEvents.triggerConfigUpdate()
    }

    fun renameSafUri(uri: Uri, newName: String) {
        safNames[uri.toString()] = newName
        saveAddedSafUris()
        GlobalEvents.triggerConfigUpdate()
    }

    private fun loadFavorites() {
        val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
        val favoritesList = prefs.getString("ordered_paths", "") ?: ""
        favoritePaths.clear()
        favoriteNames.clear()
        if (favoritesList.isNotEmpty()) {
            favoritePaths.addAll(favoritesList.split("|"))
        } else {
            val favoritesSet = prefs.getStringSet("paths", emptySet()) ?: emptySet()
            favoritePaths.addAll(favoritesSet.sorted())
            saveFavorites()
        }

        val namesJson = prefs.getString("custom_names", null)
        if (namesJson != null) {
            try {
                val obj = JSONObject(namesJson)
                obj.keys().forEach { key ->
                    favoriteNames[key] = obj.getString(key)
                }
            } catch (_: Exception) {}
        }

        ShortcutHelper.updateFavoritesShortcuts(context, favoritePaths)
    }
    
    fun saveFavorites() {
        val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val orderedString = favoritePaths.joinToString("|")
        editor.putString("ordered_paths", orderedString)
        editor.putStringSet("paths", favoritePaths.toSet())

        val namesObj = JSONObject()
        favoriteNames.forEach { (path, name) -> namesObj.put(path, name) }
        editor.putString("custom_names", namesObj.toString())
        
        editor.apply()
        ShortcutHelper.updateFavoritesShortcuts(context, favoritePaths)
    }

    fun renameFavorite(path: String, newName: String) {
        favoriteNames[path] = newName
        saveFavorites()
        GlobalEvents.triggerConfigUpdate()
    }

    private fun loadLibrarySettings() {
        val prefs = context.getSharedPreferences("library", Context.MODE_PRIVATE)
        val order = prefs.getString("order", "home|gallery|recent|documents|trash") ?: "home|gallery|recent|documents|trash"
        libraryOrder.clear()
        // Map legacy ID "game_saves" to new "games_manager"
        val loaded = order.split("|").map { if (it == "game_saves") "games_manager" else it }
        libraryOrder.addAll(loaded)
        // Migrate: add home if not present
        if (!libraryOrder.contains("home")) {
            libraryOrder.add(0, "home")
        }
        // Migrate: add gallery if not present in saved order
        if (!libraryOrder.contains("gallery")) {
            val insertIndex = libraryOrder.indexOf("home").let { if (it != -1) it + 1 else 0 }
            libraryOrder.add(insertIndex.coerceIn(0, libraryOrder.size), "gallery")
        }
        // Migrate: add documents if not present in saved order
        if (!libraryOrder.contains("documents")) {
            val insertIndex = libraryOrder.indexOf("downloads").let { if (it != -1) it + 1 else 0 }
            libraryOrder.add(insertIndex.coerceIn(0, libraryOrder.size), "documents")
        }
        // Migrate: add downloads if not present in saved order
        if (!libraryOrder.contains("downloads")) {
            val insertIndex = libraryOrder.indexOf("recent").let { if (it != -1) it + 1 else 0 }
            libraryOrder.add(insertIndex.coerceIn(0, libraryOrder.size), "downloads")
        }
        // Migrate: add games_manager if not present in saved order
        if (!libraryOrder.contains("games_manager")) {
            val insertIndex = libraryOrder.indexOf("documents").let { if (it != -1) it + 1 else 0 }
            libraryOrder.add(insertIndex.coerceIn(0, libraryOrder.size), "games_manager")
        }
        // Migrate: add music if not present in saved order
        if (!libraryOrder.contains("music")) {
            val insertIndex = libraryOrder.indexOf("gallery").let { if (it != -1) it + 1 else 0 }
            libraryOrder.add(insertIndex.coerceIn(0, libraryOrder.size), "music")
        }
        isRecentVisible = prefs.getBoolean("is_recent_visible", true)
        isGalleryVisible = prefs.getBoolean("is_gallery_visible", true)
        isMusicVisible = prefs.getBoolean("is_music_visible", false)
        isDownloadsVisible = prefs.getBoolean("is_downloads_visible", false)
        isDocumentsVisible = prefs.getBoolean("is_documents_visible", true)
        isArchivesVisible = prefs.getBoolean("is_archives_visible", false)
        isApksVisible = prefs.getBoolean("is_apks_visible", false)
        isGamesVisible = if (prefs.contains("is_games_visible")) {
            prefs.getBoolean("is_games_visible", false)
        } else {
            prefs.getBoolean("is_game_saves_visible", false)
        }
        isTrashVisible = prefs.getBoolean("is_trash_visible", true)
        isGalleryAlbumsEnabled = prefs.getBoolean("is_gallery_albums_enabled", false)
        isMusicAlbumsEnabled = prefs.getBoolean("is_music_albums_enabled", false)
        isDocumentsFolderEnabled = prefs.getBoolean("is_documents_folder_enabled", false)
    }

    fun saveLibrarySettings() {
        val prefs = context.getSharedPreferences("library", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("order", libraryOrder.joinToString("|"))
            putBoolean("is_recent_visible", isRecentVisible)
            putBoolean("is_gallery_visible", isGalleryVisible)
            putBoolean("is_music_visible", isMusicVisible)
            putBoolean("is_downloads_visible", isDownloadsVisible)
            putBoolean("is_documents_visible", isDocumentsVisible)
            putBoolean("is_archives_visible", isArchivesVisible)
            putBoolean("is_apks_visible", isApksVisible)
            putBoolean("is_games_visible", isGamesVisible)
            putBoolean("is_trash_visible", isTrashVisible)
            putBoolean("is_gallery_albums_enabled", isGalleryAlbumsEnabled)
            putBoolean("is_music_albums_enabled", isMusicAlbumsEnabled)
            putBoolean("is_documents_folder_enabled", isDocumentsFolderEnabled)
        }.apply()
    }

    fun toggleRecentVisibility() {
        isRecentVisible = !isRecentVisible
        saveLibrarySettings()
        GlobalEvents.triggerConfigUpdate()
    }

    fun toggleGalleryVisibility() {
        isGalleryVisible = !isGalleryVisible
        saveLibrarySettings()
        GlobalEvents.triggerConfigUpdate()
    }

    fun toggleMusicVisibility() {
        isMusicVisible = !isMusicVisible
        saveLibrarySettings()
        GlobalEvents.triggerConfigUpdate()
    }

    fun toggleDownloadsVisibility() {
        isDownloadsVisible = !isDownloadsVisible
        saveLibrarySettings()
        GlobalEvents.triggerConfigUpdate()
    }

    fun toggleDocumentsVisibility() {
        isDocumentsVisible = !isDocumentsVisible
        saveLibrarySettings()
        GlobalEvents.triggerConfigUpdate()
    }

    fun toggleArchivesVisibility() {
        isArchivesVisible = !isArchivesVisible
        saveLibrarySettings()
        GlobalEvents.triggerConfigUpdate()
    }

    fun toggleApksVisibility() {
        isApksVisible = !isApksVisible
        saveLibrarySettings()
        GlobalEvents.triggerConfigUpdate()
    }

    fun toggleGamesVisibility() {
        isGamesVisible = !isGamesVisible
        saveLibrarySettings()
        GlobalEvents.triggerConfigUpdate()
    }

    fun toggleTrashVisibility() {
        isTrashVisible = !isTrashVisible
        saveLibrarySettings()
        GlobalEvents.triggerConfigUpdate()
    }

    fun toggleGalleryAlbums() {
        isGalleryAlbumsEnabled = !isGalleryAlbumsEnabled
        saveLibrarySettings()
        GlobalEvents.triggerConfigUpdate()
    }

    fun toggleMusicAlbums() {
        isMusicAlbumsEnabled = !isMusicAlbumsEnabled
        saveLibrarySettings()
        GlobalEvents.triggerConfigUpdate()
    }

    fun toggleDocumentsFolder() {
        isDocumentsFolderEnabled = !isDocumentsFolderEnabled
        saveLibrarySettings()
        GlobalEvents.triggerConfigUpdate()
    }
    
    fun addFavorite(path: String) {
        if (!favoritePaths.contains(path)) {
            favoritePaths.add(path)
            saveFavorites()
            GlobalEvents.triggerConfigUpdate()
        }
    }
    
    fun removeFavorite(path: String) {
        if (favoritePaths.remove(path)) {
            saveFavorites()
            GlobalEvents.triggerConfigUpdate()
        }
    }
    
    fun moveFavorite(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val item = favoritePaths.removeAt(fromIndex)
        favoritePaths.add(toIndex, item)
        saveFavorites()
        GlobalEvents.triggerConfigUpdate()
    }
    
    fun moveLibraryItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val item = libraryOrder.removeAt(fromIndex)
        libraryOrder.add(toIndex, item)
        saveLibrarySettings()
        GlobalEvents.triggerConfigUpdate()
    }
    
    fun isFavorite(path: String): Boolean {
        return favoritePaths.contains(path)
    }

    private fun loadNetworkConnections() {
        // Try encrypted prefs first; fall back to legacy plaintext (one-shot migration)
        var securePrefs: android.content.SharedPreferences? = null
        try {
            securePrefs = SecurePrefs.get(context)
        } catch (_: Exception) {}

        var jsonStr = securePrefs?.getString("connections", null)

        if (jsonStr == null) {
            // Legacy plaintext fallback – migrate and wipe
            val legacyPrefs = context.getSharedPreferences("network_storage", Context.MODE_PRIVATE)
            val legacyJson = legacyPrefs.getString("connections", null)
            if (legacyJson != null) {
                jsonStr = legacyJson
                try {
                    securePrefs?.edit()?.putString("connections", legacyJson)?.apply()
                } catch (_: Exception) {}
                legacyPrefs.edit().clear().apply()
            }
        }

        jsonStr ?: return
        networkConnections.clear()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                networkConnections.add(
                    NetworkConnection(
                        id = obj.getString("id"),
                        protocol = NetworkProtocol.valueOf(obj.getString("protocol")),
                        displayName = obj.getString("displayName"),
                        host = obj.optString("host", ""),
                        port = obj.optInt("port", 0),
                        username = obj.optString("username", ""),
                        password = obj.optString("password", ""),
                        remotePath = obj.optString("remotePath", "/"),
                        useTls = obj.optBoolean("useTls", false),
                        ftpPassiveMode = obj.optBoolean("ftpPassiveMode", true),
                        rootUrl = obj.optString("rootUrl", ""),
                        acceptUntrustedCerts = obj.optBoolean("acceptUntrustedCerts", false),
                        smbDomain = obj.optString("smbDomain", ""),
                        sftpPrivateKeyUri = obj.optString("sftpPrivateKeyUri", ""),
                        sftpPrivateKeyPassphrase = obj.optString("sftpPrivateKeyPassphrase", ""),
                        sftpPrivateKey = obj.optString("sftpPrivateKey", ""),
                    )
                )
            }
        } catch (_: Exception) {}
    }

    fun saveNetworkConnections() {
        val arr = JSONArray()
        networkConnections.forEach { conn ->
            arr.put(JSONObject().apply {
                put("id", conn.id)
                put("protocol", conn.protocol.name)
                put("displayName", conn.displayName)
                put("host", conn.host)
                put("port", conn.port)
                put("username", conn.username)
                put("password", conn.password)
                put("remotePath", conn.remotePath)
                put("useTls", conn.useTls)
                put("ftpPassiveMode", conn.ftpPassiveMode)
                put("rootUrl", conn.rootUrl)
                put("acceptUntrustedCerts", conn.acceptUntrustedCerts)
                put("smbDomain", conn.smbDomain)
                put("sftpPrivateKeyUri", conn.sftpPrivateKeyUri)
                put("sftpPrivateKeyPassphrase", conn.sftpPrivateKeyPassphrase)
                put("sftpPrivateKey", conn.sftpPrivateKey)
            })
        }
        try {
            SecurePrefs.get(context).edit().putString("connections", arr.toString()).apply()
        } catch (_: Exception) {
            // Fallback to plaintext if crypto init fails (shouldn't happen on supported devices)
            context.getSharedPreferences("network_storage", Context.MODE_PRIVATE)
                .edit().putString("connections", arr.toString()).apply()
        }
    }

    fun addNetworkConnection(connection: NetworkConnection) {
        networkConnections.add(connection)
        saveNetworkConnections()
        GlobalEvents.triggerConfigUpdate()
    }

    fun removeNetworkConnection(id: String) {
        if (networkConnections.removeAll { it.id == id }) {
            StorageProviders.evictNetwork(id)
            saveNetworkConnections()
            GlobalEvents.triggerConfigUpdate()
        }
    }

    fun moveNetworkConnection(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val item = networkConnections.removeAt(fromIndex)
        networkConnections.add(toIndex, item)
        saveNetworkConnections()
        GlobalEvents.triggerConfigUpdate()
    }

    fun updateNetworkConnection(connection: NetworkConnection) {
        val index = networkConnections.indexOfFirst { it.id == connection.id }
        if (index >= 0) {
            StorageProviders.evictNetwork(connection.id)
            networkConnections[index] = connection
            saveNetworkConnections()
            GlobalEvents.triggerConfigUpdate()
        }
    }
}