package com.troikoss.continuum_explorer.managers

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.*
import androidx.core.os.LocaleListCompat
import com.troikoss.continuum_explorer.model.LibraryItem
import com.troikoss.continuum_explorer.model.ViewMode
import com.troikoss.continuum_explorer.model.SubtitleStyle
import com.troikoss.continuum_explorer.model.SubtitleFontSize
import com.troikoss.continuum_explorer.utils.GlobalEvents

enum class DetailsMode {
    OFF,
    PANE,
    BAR
}

enum class DeleteBehavior {
    ASK,
    RECYCLE,
    PERMANENT
}

enum class TouchDragBehavior {
    ASK,
    COPY,
    MOVE
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    VERY_DARK,
    VERY_LIGHT,
    ENHANCED_SYSTEM
}

enum class ThemeShape {
    ROUNDED,
    SQUARE
}

enum class ThemeTopMode {
    ATTACHED,
    FLOAT
}

enum class IconTheme {
    COLOURFULDUO,
    COLOURFUL,
    MATERIAL
}

enum class IconStyle {
    MATERIAL,
    COLOURFUL,
    COLOURFULDUO,
    CUSTOM
}

object SettingsManager {
    private const val PREFS_NAME = "explorer_settings"
    private const val KEY_DELETE_BEHAVIOR = "delete_behavior"

    private const val KEY_TOUCH_DRAG_BEHAVIOR = "touch_drag_behavior"
    private const val KEY_DEFAULT_ARCHIVE_VIEWER = "default_archive_viewer"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_THEME_BAR = "theme_bar"
    private const val KEY_THEME_CONTENT = "theme_content"
    private const val KEY_THEME_TOP = "theme_top"
    private const val KEY_ICON_THEME = "icon_theme"
    private const val KEY_ICON_STYLE = "icon_style"
    private const val KEY_MUSIC_ICON_THEME = "music_icon_theme"
    private const val KEY_SIDEBAR_ICON_THEME = "sidebar_icon_theme"
    private const val KEY_FOLDER_ICON_THEME = "folder_icon_theme"
    private const val KEY_HOME_ICON_THEME = "home_icon_theme"
    private const val KEY_CUSTOM_THEME_MODE = "custom_theme_mode"

    private const val KEY_LANGUAGE = "language"
    private const val KEY_DETAILS_MODE = "details_mode"
    private const val KEY_TAB_BAR_BACKGROUND_URI = "tab_bar_background_uri"
    private const val KEY_STARTING_PAGE = "starting_page"

    private const val KEY_COMMAND_BAR_VISIBLE = "command_bar_visible"
    private const val KEY_SHOW_HIDDEN_FILES = "show_hidden_files"
    private const val KEY_ICON_TOUCH_SELECTION = "icon_touch_selection"
    private const val KEY_DEFAULT_VIEW_MODE = "default_view_mode"
    private const val KEY_COLORFUL_BARS = "colorful_bars"
    private const val KEY_TERMUX_SUPPORT = "termux_support"
    private const val KEY_FTP_SERVER_ENABLED = "ftp_server_enabled"
    private const val KEY_FTP_USER = "ftp_user"
    private const val KEY_FTP_PASSWORD = "ftp_password"
    private const val KEY_GAME_SAVES_PATH = "game_saves_path"
    private const val KEY_FTP_SHARE_GAME_SAVES = "ftp_share_game_saves"
    private const val KEY_FTP_SERVER_MODE = "ftp_server_mode"
    private const val KEY_FTP_INACTIVITY_TIMEOUT = "ftp_inactivity_timeout"
    private const val KEY_APP_INACTIVITY_TIMEOUT = "app_inactivity_timeout"
    private const val KEY_GALLERY_FOLDERS = "gallery_folders"
    private const val KEY_GALLERY_FILTER_ENABLED = "gallery_filter_enabled"
    private const val KEY_VIDEO_FOLDERS = "video_folders"
    private const val KEY_VIDEO_FILTER_ENABLED = "video_filter_enabled"
    private const val KEY_MUSIC_FOLDERS = "music_folders"
    private const val KEY_MUSIC_FILTER_ENABLED = "music_filter_enabled"
    private const val KEY_SUBTITLE_STYLE = "subtitle_style"
    private const val KEY_SUBTITLE_FONT_SIZE = "subtitle_font_size"
    private const val PREFS_FOLDER_COLORS = "folder_colors"
    private const val KEY_DEFAULT_FOLDER_COLOR = "default_folder_color"

    private const val KEY_NAV_FAVORITES_EXPANDED = "nav_favorites_expanded"
    private const val KEY_NAV_LIBRARY_EXPANDED = "nav_library_expanded"
    private const val KEY_NAV_STORAGE_EXPANDED = "nav_storage_expanded"
    private const val KEY_NAV_ADDED_LOCATIONS_EXPANDED = "nav_added_locations_expanded"
    private const val KEY_NAV_NETWORK_EXPANDED = "nav_network_expanded"

    enum class FtpMode {
        FULL_STORAGE,
        GAMES
    }

    private val _deleteBehavior = mutableStateOf(DeleteBehavior.ASK)
    val deleteBehavior: State<DeleteBehavior> = _deleteBehavior

    private val _touchDragBehavior = mutableStateOf(TouchDragBehavior.ASK)
    val touchDragBehavior: State<TouchDragBehavior> = _touchDragBehavior

    private val _themeMode = mutableStateOf(ThemeMode.SYSTEM)
    val themeMode: State<ThemeMode> = _themeMode

    private val _themeBar = mutableStateOf(ThemeShape.ROUNDED)
    val themeBar: State<ThemeShape> = _themeBar

    private val _themeContent = mutableStateOf(ThemeShape.ROUNDED)
    val themeContent: State<ThemeShape> = _themeContent

    private val _themeTop = mutableStateOf(ThemeTopMode.ATTACHED)
    val themeTop: State<ThemeTopMode> = _themeTop

    private val _iconTheme = mutableStateOf(IconTheme.COLOURFUL)
    val iconTheme: State<IconTheme> = _iconTheme

    private val _iconStyle = mutableStateOf(IconStyle.COLOURFUL)
    val iconStyle: State<IconStyle> = _iconStyle

    private val _musicIconTheme = mutableStateOf(IconTheme.COLOURFUL)
    val musicIconTheme: State<IconTheme> = _musicIconTheme

    private val _sidebarIconTheme = mutableStateOf(IconTheme.COLOURFUL)
    val sidebarIconTheme: State<IconTheme> = _sidebarIconTheme

    private val _folderIconTheme = mutableStateOf(IconTheme.COLOURFUL)
    val folderIconTheme: State<IconTheme> = _folderIconTheme

    private val _homeIconTheme = mutableStateOf(IconTheme.COLOURFUL)
    val homeIconTheme: State<IconTheme> = _homeIconTheme

    private val _customThemeMode = mutableStateOf(ThemeMode.SYSTEM)
    val customThemeMode: State<ThemeMode> = _customThemeMode

    private val _language = mutableStateOf("system")
    val language: State<String> = _language

    private val _tabBarBackgroundUri = mutableStateOf<String?>(null)
    val tabBarBackgroundUri: State<String?> = _tabBarBackgroundUri

    private val _detailsMode = mutableStateOf(DetailsMode.OFF)
    val detailsMode: State<DetailsMode> = _detailsMode

    private val _startingPage = mutableStateOf(LibraryItem.Home)
    val startingPage: State<LibraryItem> = _startingPage

    private val _isCommandBarVisible = mutableStateOf(true)
    val isCommandBarVisible: State<Boolean> = _isCommandBarVisible

    private val _showHiddenFiles = mutableStateOf(false)
    val showHiddenFiles: State<Boolean> = _showHiddenFiles

    private val _iconTouchSelection = mutableStateOf(true)
    val iconTouchSelection: State<Boolean> = _iconTouchSelection

    private val _defaultViewMode = mutableStateOf(ViewMode.DETAILS)
    val defaultViewMode: State<ViewMode> = _defaultViewMode

    private val _isColorfulBarsEnabled = mutableStateOf(false)
    val isColorfulBarsEnabled: State<Boolean> = _isColorfulBarsEnabled

    private val _termuxSupport = mutableStateOf(true)
    val termuxSupport: State<Boolean> = _termuxSupport

    private val _isFtpServerEnabled = mutableStateOf(false)
    val isFtpServerEnabled: State<Boolean> = _isFtpServerEnabled

    private val _ftpUser = mutableStateOf("admin")
    val ftpUser: State<String> = _ftpUser

    private val _ftpPassword = mutableStateOf("admin")
    val ftpPassword: State<String> = _ftpPassword

    private val _gamesPath = mutableStateOf("")
    val gamesPath: State<String> = _gamesPath

    private val _isFtpShareGamesEnabled = mutableStateOf(false)
    val isFtpShareGamesEnabled: State<Boolean> = _isFtpShareGamesEnabled

    private val _ftpMode = mutableStateOf(FtpMode.FULL_STORAGE)
    val ftpMode: State<FtpMode> = _ftpMode

    private val _ftpInactivityTimeout = mutableIntStateOf(5)
    val ftpInactivityTimeout: State<Int> = _ftpInactivityTimeout

    private val _appInactivityTimeout = mutableIntStateOf(5)
    val appInactivityTimeout: State<Int> = _appInactivityTimeout

    private val _galleryFolders = mutableStateOf(setOf<String>())
    val galleryFolders: State<Set<String>> = _galleryFolders

    private val _isGalleryFilterEnabled = mutableStateOf(false)
    val isGalleryFilterEnabled: State<Boolean> = _isGalleryFilterEnabled

    private val _videoFolders = mutableStateOf(setOf<String>())
    val videoFolders: State<Set<String>> = _videoFolders

    private val _isVideoFilterEnabled = mutableStateOf(false)
    val isVideoFilterEnabled: State<Boolean> = _isVideoFilterEnabled

    private val _musicFolders = mutableStateOf(setOf<String>())
    val musicFolders: State<Set<String>> = _musicFolders

    private val _isMusicFilterEnabled = mutableStateOf(false)
    val isMusicFilterEnabled: State<Boolean> = _isMusicFilterEnabled

    private val _subtitleStyle = mutableStateOf(SubtitleStyle.BAR)
    val subtitleStyle: State<SubtitleStyle> = _subtitleStyle

    private val _subtitleFontSize = mutableStateOf(SubtitleFontSize.MEDIUM)
    val subtitleFontSize: State<SubtitleFontSize> = _subtitleFontSize

    private val _folderColors = mutableStateMapOf<String, Long>()
    val folderColors: Map<String, Long> = _folderColors

    private val _defaultFolderColor = mutableLongStateOf(0xFF2196F3)
    val defaultFolderColor: State<Long> = _defaultFolderColor

    private val _isRecycleBinEnabled = mutableStateOf(true)
    val isRecycleBinEnabled: State<Boolean> = _isRecycleBinEnabled

    private val _isDefaultArchiveViewerEnabled = mutableStateOf(true)
    val isDefaultArchiveViewerEnabled: State<Boolean> = _isDefaultArchiveViewerEnabled

    private val _isFavoritesExpanded = mutableStateOf(true)
    val isFavoritesExpanded: State<Boolean> = _isFavoritesExpanded

    private val _isLibraryExpanded = mutableStateOf(true)
    val isLibraryExpanded: State<Boolean> = _isLibraryExpanded

    private val _isStorageExpanded = mutableStateOf(true)
    val isStorageExpanded: State<Boolean> = _isStorageExpanded

    private val _isAddedLocationsExpanded = mutableStateOf(true)
    val isAddedLocationsExpanded: State<Boolean> = _isAddedLocationsExpanded

    private val _isNetworkExpanded = mutableStateOf(true)
    val isNetworkExpanded: State<Boolean> = _isNetworkExpanded

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        val savedBehavior = prefs.getString(KEY_DELETE_BEHAVIOR, DeleteBehavior.ASK.name)
        val behavior = try {
            DeleteBehavior.valueOf(savedBehavior ?: DeleteBehavior.ASK.name)
        } catch (_: Exception) {
            DeleteBehavior.ASK
        }
        
        updateBehaviorInternal(behavior)

        val savedTouchDrag = prefs.getString(KEY_TOUCH_DRAG_BEHAVIOR, TouchDragBehavior.ASK.name)
        _touchDragBehavior.value = try {
            TouchDragBehavior.valueOf(savedTouchDrag ?: TouchDragBehavior.ASK.name)
        } catch (_: Exception) {
            TouchDragBehavior.ASK
        }

        val savedTheme = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        _themeMode.value = try {
            ThemeMode.valueOf(savedTheme ?: ThemeMode.SYSTEM.name)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }

        val savedThemeBar = prefs.getString(KEY_THEME_BAR, ThemeShape.ROUNDED.name)
        _themeBar.value = try {
            ThemeShape.valueOf(savedThemeBar ?: ThemeShape.ROUNDED.name)
        } catch (_: Exception) {
            ThemeShape.ROUNDED
        }

        val savedThemeContent = prefs.getString(KEY_THEME_CONTENT, ThemeShape.ROUNDED.name)
        _themeContent.value = try {
            ThemeShape.valueOf(savedThemeContent ?: ThemeShape.ROUNDED.name)
        } catch (_: Exception) {
            ThemeShape.ROUNDED
        }

        val savedThemeTop = prefs.getString(KEY_THEME_TOP, ThemeTopMode.ATTACHED.name)
        _themeTop.value = try {
            ThemeTopMode.valueOf(savedThemeTop ?: ThemeTopMode.ATTACHED.name)
        } catch (_: Exception) {
            ThemeTopMode.ATTACHED
        }

        val savedIconTheme = prefs.getString(KEY_ICON_THEME, IconTheme.COLOURFUL.name)
        _iconTheme.value = try {
            IconTheme.valueOf(savedIconTheme ?: IconTheme.COLOURFUL.name)
        } catch (_: Exception) {
            IconTheme.COLOURFUL
        }

        val savedIconStyle = prefs.getString(KEY_ICON_STYLE, IconStyle.COLOURFUL.name)
        _iconStyle.value = try {
            IconStyle.valueOf(savedIconStyle ?: IconStyle.COLOURFUL.name)
        } catch (_: Exception) {
            IconStyle.COLOURFUL
        }

        val savedMusicIconTheme = prefs.getString(KEY_MUSIC_ICON_THEME, IconTheme.COLOURFUL.name)
        _musicIconTheme.value = try {
            IconTheme.valueOf(savedMusicIconTheme ?: IconTheme.COLOURFUL.name)
        } catch (_: Exception) {
            IconTheme.COLOURFUL
        }

        val savedSidebarIconTheme = prefs.getString(KEY_SIDEBAR_ICON_THEME, IconTheme.COLOURFUL.name)
        _sidebarIconTheme.value = try {
            IconTheme.valueOf(savedSidebarIconTheme ?: IconTheme.COLOURFUL.name)
        } catch (_: Exception) {
            IconTheme.COLOURFUL
        }

        val savedFolderIconTheme = prefs.getString(KEY_FOLDER_ICON_THEME, IconTheme.COLOURFUL.name)
        _folderIconTheme.value = try {
            IconTheme.valueOf(savedFolderIconTheme ?: IconTheme.COLOURFUL.name)
        } catch (_: Exception) {
            IconTheme.COLOURFUL
        }

        val savedHomeIconTheme = prefs.getString(KEY_HOME_ICON_THEME, IconTheme.COLOURFUL.name)
        _homeIconTheme.value = try {
            IconTheme.valueOf(savedHomeIconTheme ?: IconTheme.COLOURFUL.name)
        } catch (_: Exception) {
            IconTheme.COLOURFUL
        }

        val savedCustomTheme = prefs.getString(KEY_CUSTOM_THEME_MODE, ThemeMode.SYSTEM.name)
        _customThemeMode.value = try {
            ThemeMode.valueOf(savedCustomTheme ?: ThemeMode.SYSTEM.name)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }

        val savedLanguage = prefs.getString(KEY_LANGUAGE, "system") ?: "system"
        _language.value = savedLanguage
        applyLocale(savedLanguage)

        _tabBarBackgroundUri.value = prefs.getString(KEY_TAB_BAR_BACKGROUND_URI, null)

        val savedDetails = prefs.getString(KEY_DETAILS_MODE, DetailsMode.OFF.name)
        _detailsMode.value = try {
            DetailsMode.valueOf(savedDetails ?: DetailsMode.OFF.name)
        } catch (_: Exception) {
            DetailsMode.OFF
        }

        val savedStartingPage = prefs.getString(KEY_STARTING_PAGE, LibraryItem.Home.name)
        _startingPage.value = try {
            LibraryItem.valueOf(savedStartingPage ?: LibraryItem.Home.name)
        } catch (_: Exception) {
            LibraryItem.Home
        }

        _isCommandBarVisible.value = prefs.getBoolean(KEY_COMMAND_BAR_VISIBLE, true)
        _showHiddenFiles.value = prefs.getBoolean(KEY_SHOW_HIDDEN_FILES, false)
        _iconTouchSelection.value = prefs.getBoolean(KEY_ICON_TOUCH_SELECTION, true)
        _isColorfulBarsEnabled.value = prefs.getBoolean(KEY_COLORFUL_BARS, false)

        _isDefaultArchiveViewerEnabled.value = prefs.getBoolean(KEY_DEFAULT_ARCHIVE_VIEWER, true)
        _termuxSupport.value = prefs.getBoolean(KEY_TERMUX_SUPPORT, true)
        
        _isFavoritesExpanded.value = prefs.getBoolean(KEY_NAV_FAVORITES_EXPANDED, true)
        _isLibraryExpanded.value = prefs.getBoolean(KEY_NAV_LIBRARY_EXPANDED, true)
        _isStorageExpanded.value = prefs.getBoolean(KEY_NAV_STORAGE_EXPANDED, true)
        _isAddedLocationsExpanded.value = prefs.getBoolean(KEY_NAV_ADDED_LOCATIONS_EXPANDED, true)
        _isNetworkExpanded.value = prefs.getBoolean(KEY_NAV_NETWORK_EXPANDED, true)

        _isFtpServerEnabled.value = prefs.getBoolean(KEY_FTP_SERVER_ENABLED, false)
        _ftpUser.value = prefs.getString(KEY_FTP_USER, "admin") ?: "admin"
        _ftpPassword.value = prefs.getString(KEY_FTP_PASSWORD, "admin") ?: "admin"
        _gamesPath.value = prefs.getString(KEY_GAME_SAVES_PATH, "") ?: ""
        _isFtpShareGamesEnabled.value = prefs.getBoolean(KEY_FTP_SHARE_GAME_SAVES, false)
        
        _ftpInactivityTimeout.intValue = prefs.getInt(KEY_FTP_INACTIVITY_TIMEOUT, 5)
        _appInactivityTimeout.intValue = prefs.getInt(KEY_APP_INACTIVITY_TIMEOUT, 5)

        _galleryFolders.value = prefs.getStringSet(KEY_GALLERY_FOLDERS, emptySet()) ?: emptySet()
        _isGalleryFilterEnabled.value = prefs.getBoolean(KEY_GALLERY_FILTER_ENABLED, false)

        _videoFolders.value = prefs.getStringSet(KEY_VIDEO_FOLDERS, emptySet()) ?: emptySet()
        _isVideoFilterEnabled.value = prefs.getBoolean(KEY_VIDEO_FILTER_ENABLED, false)

        _musicFolders.value = prefs.getStringSet(KEY_MUSIC_FOLDERS, emptySet()) ?: emptySet()
        _isMusicFilterEnabled.value = prefs.getBoolean(KEY_MUSIC_FILTER_ENABLED, false)

        val savedSubtitleStyle = prefs.getString(KEY_SUBTITLE_STYLE, SubtitleStyle.BAR.name)
        _subtitleStyle.value = try {
            SubtitleStyle.valueOf(savedSubtitleStyle ?: SubtitleStyle.BAR.name)
        } catch (_: Exception) {
            SubtitleStyle.BAR
        }

        val savedSubtitleFontSize = prefs.getString(KEY_SUBTITLE_FONT_SIZE, SubtitleFontSize.MEDIUM.name)
        _subtitleFontSize.value = try {
            SubtitleFontSize.valueOf(savedSubtitleFontSize ?: SubtitleFontSize.MEDIUM.name)
        } catch (_: Exception) {
            SubtitleFontSize.MEDIUM
        }

        val colorPrefs = context.getSharedPreferences(PREFS_FOLDER_COLORS, Context.MODE_PRIVATE)
        _folderColors.clear()
        colorPrefs.all.forEach { (key, value) ->
            if (value is Long) _folderColors[key] = value
            else if (value is Int) _folderColors[key] = value.toLong()
        }

        _defaultFolderColor.longValue = prefs.getLong(KEY_DEFAULT_FOLDER_COLOR, 0xFF2196F3)

        val savedFtpMode = prefs.getString(KEY_FTP_SERVER_MODE, FtpMode.FULL_STORAGE.name) ?: FtpMode.FULL_STORAGE.name
        _ftpMode.value = try {
            FtpMode.valueOf(savedFtpMode)
        } catch (_: Exception) {
            if (savedFtpMode == "GAME_SAVES") FtpMode.GAMES else FtpMode.FULL_STORAGE
        }

        val savedViewMode = prefs.getString(KEY_DEFAULT_VIEW_MODE, ViewMode.DETAILS.name)
        _defaultViewMode.value = try {
            ViewMode.valueOf(savedViewMode ?: ViewMode.DETAILS.name)
        } catch (e: Exception) {
            ViewMode.DETAILS
        }
    }

    fun setLanguage(context: Context, languageTag: String) {
        _language.value = languageTag
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageTag).apply()
        applyLocale(languageTag)
        GlobalEvents.triggerConfigUpdate()
    }

    private fun applyLocale(languageTag: String) {
        val localeList = if (languageTag == "system") {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageTag)
        }
        AppCompatDelegate.setApplicationLocales(localeList)

    }

    fun setDetailsMode(context: Context, mode: DetailsMode) {
        _detailsMode.value = mode
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DETAILS_MODE, mode.name).apply()
        GlobalEvents.triggerConfigUpdate()
    }

    fun setStartingPage(context: Context, item: LibraryItem) {
        _startingPage.value = item
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_STARTING_PAGE, item.name).apply()
        GlobalEvents.triggerConfigUpdate()
    }

    fun setTabBarBackgroundUri(context: Context, uri: String?) {
        _tabBarBackgroundUri.value = uri
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (uri == null) {
            prefs.edit().remove(KEY_TAB_BAR_BACKGROUND_URI).apply()
        } else {
            prefs.edit().putString(KEY_TAB_BAR_BACKGROUND_URI, uri).apply()
        }
        GlobalEvents.triggerConfigUpdate()
    }

    fun setCommandBarVisible(context: Context, visible: Boolean) {
        _isCommandBarVisible.value = visible
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_COMMAND_BAR_VISIBLE, visible).apply()
        GlobalEvents.triggerConfigUpdate()
    }

    fun setShowHiddenFiles(context: Context, show: Boolean) {
        _showHiddenFiles.value = show
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SHOW_HIDDEN_FILES, show).apply()
        GlobalEvents.triggerConfigUpdate()
    }

    fun setIconTouchSelection(context: Context, enabled: Boolean) {
        _iconTouchSelection.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ICON_TOUCH_SELECTION, enabled).apply()
        GlobalEvents.triggerConfigUpdate()
    }

    fun setColorfulBarsEnabled(context: Context, enabled: Boolean) {
        _isColorfulBarsEnabled.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_COLORFUL_BARS, enabled).apply()
        GlobalEvents.triggerConfigUpdate()
    }

    fun setTermuxSupportEnabled(context: Context, enabled: Boolean) {
        _termuxSupport.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_TERMUX_SUPPORT, enabled).apply()
        GlobalEvents.triggerConfigUpdate()
    }

    fun setDeleteBehavior(context: Context, behavior: DeleteBehavior) {
        updateBehaviorInternal(behavior)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DELETE_BEHAVIOR, behavior.name).apply()
    }

    fun setTouchDragBehavior(context: Context, behavior: TouchDragBehavior) {
        _touchDragBehavior.value = behavior
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TOUCH_DRAG_BEHAVIOR, behavior.name).apply()
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        _themeMode.value = mode
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun setThemeBar(context: Context, mode: ThemeShape) {
        _themeBar.value = mode
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_BAR, mode.name).apply()
        GlobalEvents.triggerConfigUpdate()
    }

    fun setThemeContent(context: Context, mode: ThemeShape) {
        _themeContent.value = mode
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_CONTENT, mode.name).apply()
        GlobalEvents.triggerConfigUpdate()
    }

    fun setThemeTop(context: Context, mode: ThemeTopMode) {
        _themeTop.value = mode
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_TOP, mode.name).apply()
        GlobalEvents.triggerConfigUpdate()
    }

    fun setIconTheme(context: Context, mode: IconTheme) {
        _iconTheme.value = mode
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ICON_THEME, mode.name).apply()
        GlobalEvents.triggerConfigUpdate()
    }

    fun setIconStyle(context: Context, style: IconStyle) {
        _iconStyle.value = style
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ICON_STYLE, style.name).apply()
        GlobalEvents.triggerConfigUpdate()
    }

    fun setMusicIconTheme(context: Context, theme: IconTheme) {
        _musicIconTheme.value = theme
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MUSIC_ICON_THEME, theme.name).apply()
        GlobalEvents.triggerConfigUpdate()
    }

    fun setSidebarIconTheme(context: Context, theme: IconTheme) {
        _sidebarIconTheme.value = theme
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SIDEBAR_ICON_THEME, theme.name).apply()
        GlobalEvents.triggerConfigUpdate()
    }

    fun setFolderIconTheme(context: Context, theme: IconTheme) {
        _folderIconTheme.value = theme
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FOLDER_ICON_THEME, theme.name).apply()
        GlobalEvents.triggerConfigUpdate()
    }

    fun setHomeIconTheme(context: Context, theme: IconTheme) {
        _homeIconTheme.value = theme
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HOME_ICON_THEME, theme.name).apply()
        GlobalEvents.triggerConfigUpdate()
    }

    fun getEffectiveIconTheme(category: IconCategory): IconTheme {
        val style = _iconStyle.value
        if (style == IconStyle.CUSTOM) {
            return when (category) {
                IconCategory.SIDEBAR -> _sidebarIconTheme.value
                IconCategory.MUSIC -> _musicIconTheme.value
                IconCategory.FILES_FOLDERS -> _folderIconTheme.value
                IconCategory.HOME -> _homeIconTheme.value
            }
        }
        return when (style) {
            IconStyle.MATERIAL -> IconTheme.MATERIAL
            IconStyle.COLOURFUL -> IconTheme.COLOURFUL
            IconStyle.COLOURFULDUO -> IconTheme.COLOURFULDUO
            else -> IconTheme.COLOURFUL
        }
    }

    fun setCustomThemeMode(context: Context, mode: ThemeMode) {
        _customThemeMode.value = mode
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CUSTOM_THEME_MODE, mode.name).apply()
        GlobalEvents.triggerConfigUpdate()
    }

    private fun updateBehaviorInternal(behavior: DeleteBehavior) {
        _deleteBehavior.value = behavior
        // Automatically disable recycle bin view if user chooses to always delete permanently
        _isRecycleBinEnabled.value = (behavior != DeleteBehavior.PERMANENT)
    }

    fun setDefaultArchiveViewerEnabled(context: Context, enabled: Boolean) {
        _isDefaultArchiveViewerEnabled.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DEFAULT_ARCHIVE_VIEWER, enabled).apply()
    }

    fun setDefaultViewMode(context: Context, mode: ViewMode) {
        _defaultViewMode.value = mode
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DEFAULT_VIEW_MODE, mode.name).apply()
    }

    fun setFtpServerEnabled(context: Context, enabled: Boolean, mode: FtpMode = _ftpMode.value) {
        _isFtpServerEnabled.value = enabled
        _ftpMode.value = mode
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_FTP_SERVER_ENABLED, enabled)
            .putString(KEY_FTP_SERVER_MODE, mode.name)
            .apply()
        
        val intent = Intent(context, com.troikoss.continuum_explorer.services.FtpServerService::class.java)
        if (enabled) {
            intent.putExtra(com.troikoss.continuum_explorer.services.FtpServerService.EXTRA_MODE, mode.name)
            context.startForegroundService(intent)
        } else {
            intent.action = com.troikoss.continuum_explorer.services.FtpServerService.ACTION_STOP
            context.startService(intent)
        }
    }

    fun setFtpCredentials(context: Context, user: String, pass: String) {
        _ftpUser.value = user
        _ftpPassword.value = pass
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_FTP_USER, user)
            .putString(KEY_FTP_PASSWORD, pass)
            .apply()
        
        // If server is running, restart it to apply changes
        if (_isFtpServerEnabled.value) {
            setFtpServerEnabled(context, false)
            setFtpServerEnabled(context, true)
        }
    }

    fun setGamesPath(context: Context, path: String) {
        _gamesPath.value = path
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_GAME_SAVES_PATH, path).apply()
        
        if (_isFtpServerEnabled.value && _isFtpShareGamesEnabled.value) {
            setFtpServerEnabled(context, false)
            setFtpServerEnabled(context, true)
        }
    }

    fun getEffectiveGamesPath(context: Context): String {
        if (_gamesPath.value.isNotEmpty()) return _gamesPath.value

        val storageRoot = android.os.Environment.getExternalStorageDirectory()
        
        // 1. Check for common folder names in internal storage
        val commonNames = listOf("Games", "Game Manager", "Saves")
        for (name in commonNames) {
            val folder = java.io.File(storageRoot, name)
            if (folder.exists() && folder.isDirectory) return folder.absolutePath
        }

        // 2. Check favorites from SharedPreferences
        val favPrefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
        val favPaths = favPrefs.getString("ordered_paths", "") ?: ""
        if (favPaths.isNotEmpty()) {
            val paths = favPaths.split("|")
            for (path in paths) {
                val file = java.io.File(path)
                if (file.name.contains("game", ignoreCase = true) && file.name.contains("save", ignoreCase = true)) {
                    return path
                }
            }
        }

        // 3. Default to Android/data (where the library gets its data)
        return java.io.File(storageRoot, "Android/data").absolutePath
    }

    fun setFtpShareGamesEnabled(context: Context, enabled: Boolean) {
        _isFtpShareGamesEnabled.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_FTP_SHARE_GAME_SAVES, enabled).apply()

        // If enabling and path is empty, try to discover it
        if (enabled && _gamesPath.value.isEmpty()) {
            val discovered = getEffectiveGamesPath(context)
            _gamesPath.value = discovered
            prefs.edit().putString(KEY_GAME_SAVES_PATH, discovered).apply()
        }

        if (_isFtpServerEnabled.value) {
            setFtpServerEnabled(context, false)
            setFtpServerEnabled(context, true)
        }
    }

    fun setFtpMode(context: Context, mode: FtpMode) {
        setFtpServerEnabled(context, _isFtpServerEnabled.value, mode)
    }

    fun setFtpInactivityTimeout(context: Context, minutes: Int) {
        _ftpInactivityTimeout.intValue = minutes
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_FTP_INACTIVITY_TIMEOUT, minutes).apply()
        GlobalEvents.triggerConfigUpdate()
    }

    fun setAppInactivityTimeout(context: Context, minutes: Int) {
        _appInactivityTimeout.intValue = minutes
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_APP_INACTIVITY_TIMEOUT, minutes).apply()
        GlobalEvents.triggerConfigUpdate()
    }

    fun setGalleryFolders(context: Context, folders: Set<String>) {
        _galleryFolders.value = folders
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY_GALLERY_FOLDERS, folders).apply()
        GlobalEvents.triggerConfigUpdate()
    }

    fun setGalleryFilterEnabled(context: Context, enabled: Boolean) {
        _isGalleryFilterEnabled.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_GALLERY_FILTER_ENABLED, enabled).apply()
        GlobalEvents.triggerRefresh()
        GlobalEvents.triggerConfigUpdate()
    }

    fun setVideoFolders(context: Context, folders: Set<String>) {
        _videoFolders.value = folders
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY_VIDEO_FOLDERS, folders).apply()
        GlobalEvents.triggerRefresh()
        GlobalEvents.triggerConfigUpdate()
    }

    fun setVideoFilterEnabled(context: Context, enabled: Boolean) {
        _isVideoFilterEnabled.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_VIDEO_FILTER_ENABLED, enabled).apply()
        GlobalEvents.triggerRefresh()
        GlobalEvents.triggerConfigUpdate()
    }

    fun setMusicFolders(context: Context, folders: Set<String>) {
        _musicFolders.value = folders
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY_MUSIC_FOLDERS, folders).apply()
        GlobalEvents.triggerConfigUpdate()
    }

    fun setMusicFilterEnabled(context: Context, enabled: Boolean) {
        _isMusicFilterEnabled.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_MUSIC_FILTER_ENABLED, enabled).apply()
        GlobalEvents.triggerConfigUpdate()
    }

    fun setSubtitleStyle(context: Context, style: SubtitleStyle) {
        _subtitleStyle.value = style
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_SUBTITLE_STYLE, style.name).apply()
        GlobalEvents.triggerConfigUpdate()
    }

    fun setSubtitleFontSize(context: Context, size: SubtitleFontSize) {
        _subtitleFontSize.value = size
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_SUBTITLE_FONT_SIZE, size.name).apply()
        GlobalEvents.triggerConfigUpdate()
    }

    fun setFolderColor(context: Context, providerId: String, color: Long?) {
        val prefs = context.getSharedPreferences(PREFS_FOLDER_COLORS, Context.MODE_PRIVATE)
        if (color == null) {
            _folderColors.remove(providerId)
            prefs.edit().remove(providerId).apply()
        } else {
            _folderColors[providerId] = color
            prefs.edit().putLong(providerId, color).apply()
        }
        GlobalEvents.triggerConfigUpdate()
    }

    fun getFolderColor(providerId: String): Long? {
        return _folderColors[providerId]
    }

    fun setDefaultFolderColor(context: Context, color: Long) {
        _defaultFolderColor.longValue = color
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_DEFAULT_FOLDER_COLOR, color).apply()
        GlobalEvents.triggerConfigUpdate()
    }

    fun setFavoritesExpanded(context: Context, expanded: Boolean) {
        _isFavoritesExpanded.value = expanded
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_NAV_FAVORITES_EXPANDED, expanded).apply()
    }

    fun setLibraryExpanded(context: Context, expanded: Boolean) {
        _isLibraryExpanded.value = expanded
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_NAV_LIBRARY_EXPANDED, expanded).apply()
    }

    fun setStorageExpanded(context: Context, expanded: Boolean) {
        _isStorageExpanded.value = expanded
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_NAV_STORAGE_EXPANDED, expanded).apply()
    }

    fun setAddedLocationsExpanded(context: Context, expanded: Boolean) {
        _isAddedLocationsExpanded.value = expanded
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_NAV_ADDED_LOCATIONS_EXPANDED, expanded).apply()
    }

    fun setNetworkExpanded(context: Context, expanded: Boolean) {
        _isNetworkExpanded.value = expanded
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_NAV_NETWORK_EXPANDED, expanded).apply()
    }
}

enum class IconCategory {
    SIDEBAR,
    MUSIC,
    FILES_FOLDERS,
    HOME
}
