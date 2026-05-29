package com.troikoss.continuum_explorer.managers

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.os.LocaleListCompat
import com.troikoss.continuum_explorer.model.ViewMode
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

    private const val KEY_LANGUAGE = "language"
    private const val KEY_DETAILS_MODE = "details_mode"

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

    enum class FtpMode {
        FULL_STORAGE,
        GAME_SAVES
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

    private val _language = mutableStateOf("system")
    val language: State<String> = _language

    private val _detailsMode = mutableStateOf(DetailsMode.OFF)
    val detailsMode: State<DetailsMode> = _detailsMode

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

    private val _gameSavesPath = mutableStateOf("")
    val gameSavesPath: State<String> = _gameSavesPath

    private val _isFtpShareGameSavesEnabled = mutableStateOf(false)
    val isFtpShareGameSavesEnabled: State<Boolean> = _isFtpShareGameSavesEnabled

    private val _ftpMode = mutableStateOf(FtpMode.FULL_STORAGE)
    val ftpMode: State<FtpMode> = _ftpMode

    private val _isRecycleBinEnabled = mutableStateOf(true)
    val isRecycleBinEnabled: State<Boolean> = _isRecycleBinEnabled

    private val _isDefaultArchiveViewerEnabled = mutableStateOf(true)
    val isDefaultArchiveViewerEnabled: State<Boolean> = _isDefaultArchiveViewerEnabled

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

        val savedLanguage = prefs.getString(KEY_LANGUAGE, "system") ?: "system"
        _language.value = savedLanguage
        applyLocale(savedLanguage)

        val savedDetails = prefs.getString(KEY_DETAILS_MODE, DetailsMode.OFF.name)
        _detailsMode.value = try {
            DetailsMode.valueOf(savedDetails ?: DetailsMode.OFF.name)
        } catch (_: Exception) {
            DetailsMode.OFF
        }

        _isCommandBarVisible.value = prefs.getBoolean(KEY_COMMAND_BAR_VISIBLE, true)
        _showHiddenFiles.value = prefs.getBoolean(KEY_SHOW_HIDDEN_FILES, false)
        _iconTouchSelection.value = prefs.getBoolean(KEY_ICON_TOUCH_SELECTION, true)
        _isColorfulBarsEnabled.value = prefs.getBoolean(KEY_COLORFUL_BARS, false)

        _isDefaultArchiveViewerEnabled.value = prefs.getBoolean(KEY_DEFAULT_ARCHIVE_VIEWER, true)
        _termuxSupport.value = prefs.getBoolean(KEY_TERMUX_SUPPORT, true)
        _isFtpServerEnabled.value = prefs.getBoolean(KEY_FTP_SERVER_ENABLED, false)
        _ftpUser.value = prefs.getString(KEY_FTP_USER, "admin") ?: "admin"
        _ftpPassword.value = prefs.getString(KEY_FTP_PASSWORD, "admin") ?: "admin"
        _gameSavesPath.value = prefs.getString(KEY_GAME_SAVES_PATH, "") ?: ""
        _isFtpShareGameSavesEnabled.value = prefs.getBoolean(KEY_FTP_SHARE_GAME_SAVES, false)
        _ftpMode.value = FtpMode.valueOf(prefs.getString(KEY_FTP_SERVER_MODE, FtpMode.FULL_STORAGE.name) ?: FtpMode.FULL_STORAGE.name)

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

    fun setGameSavesPath(context: Context, path: String) {
        _gameSavesPath.value = path
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_GAME_SAVES_PATH, path).apply()
        
        if (_isFtpServerEnabled.value && _isFtpShareGameSavesEnabled.value) {
            setFtpServerEnabled(context, false)
            setFtpServerEnabled(context, true)
        }
    }

    fun getEffectiveGameSavesPath(context: Context): String {
        if (_gameSavesPath.value.isNotEmpty()) return _gameSavesPath.value

        val storageRoot = android.os.Environment.getExternalStorageDirectory()
        
        // 1. Check for common folder names in internal storage
        val commonNames = listOf("GameSaves", "Game Saves", "Saves")
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

    fun setFtpShareGameSavesEnabled(context: Context, enabled: Boolean) {
        _isFtpShareGameSavesEnabled.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_FTP_SHARE_GAME_SAVES, enabled).apply()

        // If enabling and path is empty, try to discover it
        if (enabled && _gameSavesPath.value.isEmpty()) {
            val discovered = getEffectiveGameSavesPath(context)
            _gameSavesPath.value = discovered
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
}
