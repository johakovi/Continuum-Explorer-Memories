package com.troikoss.continuum_explorer.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.troikoss.continuum_explorer.managers.ThemePackManager
import com.troikoss.continuum_explorer.utils.GlobalEvents
import java.io.File
import androidx.compose.ui.platform.LocalUriHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.troikoss.continuum_explorer.R
import com.troikoss.continuum_explorer.ui.theme.FileExplorerTheme
import com.troikoss.continuum_explorer.ui.theme.ThemeFolderColors
import com.troikoss.continuum_explorer.managers.DeleteBehavior
import com.troikoss.continuum_explorer.managers.DetailsMode
import com.troikoss.continuum_explorer.managers.FileOperationsManager
import com.troikoss.continuum_explorer.managers.IconTheme
import com.troikoss.continuum_explorer.managers.SettingsManager
import com.troikoss.continuum_explorer.managers.ThemeMode
import com.troikoss.continuum_explorer.managers.ThemeShape
import com.troikoss.continuum_explorer.managers.ThemeTopMode
import com.troikoss.continuum_explorer.managers.TouchDragBehavior
import com.troikoss.continuum_explorer.managers.ShizukuManager
import com.troikoss.continuum_explorer.model.LibraryItem
import com.troikoss.continuum_explorer.model.ViewMode

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FileExplorerTheme {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val deleteBehavior = SettingsManager.deleteBehavior.value
    val touchDragBehavior = SettingsManager.touchDragBehavior.value
    val isDefaultArchiveViewerEnabled = SettingsManager.isDefaultArchiveViewerEnabled.value
    val themeMode = SettingsManager.themeMode.value
    val themeBar = SettingsManager.themeBar.value
    val themeContent = SettingsManager.themeContent.value
    val themeTop = SettingsManager.themeTop.value
    val language = SettingsManager.language.value
    val detailsMode = SettingsManager.detailsMode.value
    val startingPage = SettingsManager.startingPage.value
    val isCommandBarVisible = SettingsManager.isCommandBarVisible.value
    val showHiddenFiles = SettingsManager.showHiddenFiles.value
    val iconTouchSelection = SettingsManager.iconTouchSelection.value
    val defaultViewMode = SettingsManager.defaultViewMode.value
    val isColorfulBarsEnabled = SettingsManager.isColorfulBarsEnabled.value
    val termuxSupport = SettingsManager.termuxSupport.value
    val iconTheme = SettingsManager.iconTheme.value
    val customThemeMode = SettingsManager.customThemeMode.value
    val ftpInactivityTimeout = SettingsManager.ftpInactivityTimeout.value
    val appInactivityTimeout = SettingsManager.appInactivityTimeout.value
    val defaultFolderColor = SettingsManager.defaultFolderColor.value
    val currentThemePack = ThemePackManager.currentPack.value

    val msgThemePackLoadedTemplate = stringResource(R.string.msg_theme_pack_loaded)
    val themePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = File(context.cacheDir, "temp_theme.zip")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (ThemePackManager.loadPack(context, file)) {
                val packName = ThemePackManager.currentPack.value?.name ?: ""
                Toast.makeText(context, msgThemePackLoadedTemplate.format(packName), Toast.LENGTH_SHORT).show()
                GlobalEvents.triggerConfigUpdate()
            } else {
                Toast.makeText(context, R.string.msg_theme_pack_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val backgroundPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            SettingsManager.setTabBarBackgroundUri(context, it.toString())
        }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTouchDragDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showThemeBarDialog by remember { mutableStateOf(false) }
    var showThemeContentDialog by remember { mutableStateOf(false) }
    var showThemeTopDialog by remember { mutableStateOf(false) }
    var showIconThemeDialog by remember { mutableStateOf(false) }
    var showCustomThemeModeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showDefaultFolderColorDialog by remember { mutableStateOf(false) }
    var showShortcutsDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showStartingPageDialog by remember { mutableStateOf(false) }
    var showDefaultViewModeDialog by remember { mutableStateOf(false) }
    var showFtpCredentialsDialog by remember { mutableStateOf(false) }
    var showFtpTimeoutDialog by remember { mutableStateOf(false) }
    var showAppTimeoutDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                stringResource(R.string.settings_appearance),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_themetop)) },
                supportingContent = {
                    val text = when (themeTop) {
                        ThemeTopMode.ATTACHED -> stringResource(R.string.settings_themetop_attached)
                        ThemeTopMode.FLOAT -> stringResource(R.string.settings_themetop_float)
                    }
                    Text(text)
                },
                modifier = Modifier.clickable { showThemeTopDialog = true }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_themebar)) },
                supportingContent = {
                    val text = when (themeBar) {
                        ThemeShape.SQUARE -> stringResource(R.string.settings_themebar_square)
                        ThemeShape.ROUNDED -> stringResource(R.string.settings_themebar_rounded)
                    }
                    Text(text)
                },
                modifier = Modifier.clickable { showThemeBarDialog = true }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_themecontent)) },
                supportingContent = {
                    val text = when (themeContent) {
                        ThemeShape.SQUARE -> stringResource(R.string.settings_themecontent_square)
                        ThemeShape.ROUNDED -> stringResource(R.string.settings_themecontent_rounded)
                    }
                    Text(text)
                },
                modifier = Modifier.clickable { showThemeContentDialog = true }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_colorful_bars)) },
                supportingContent = { Text(stringResource(R.string.settings_colorful_bars_desc)) },
                trailingContent = {
                    Switch(
                        checked = isColorfulBarsEnabled,
                        onCheckedChange = { SettingsManager.setColorfulBarsEnabled(context, it) }
                    )
                }
            )




            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_details_large)) },
                supportingContent = {
                    val text = when (detailsMode) {
                        DetailsMode.OFF -> stringResource(R.string.settings_details_hidden)
                        DetailsMode.PANE -> stringResource(R.string.settings_details_pane)
                        DetailsMode.BAR -> stringResource(R.string.settings_details_bar)
                    }
                    Text(text)
                },
                modifier = Modifier.clickable { showDetailsDialog = true }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_default_view_mode)) },
                supportingContent = {
                    val text = when (defaultViewMode) {
                        ViewMode.DETAILS -> stringResource(R.string.menu_details)
                        ViewMode.CONTENT -> stringResource(R.string.menu_content)
                        ViewMode.GRID -> stringResource(R.string.menu_grid)
                        ViewMode.GALLERY -> stringResource(R.string.menu_gallery)
                        ViewMode.MUSIC -> stringResource(R.string.menu_music)

                    }
                    Text(text)
                },
                modifier = Modifier.clickable { showDefaultViewModeDialog = true }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_show_command_bar)) },
                supportingContent = { Text(stringResource(R.string.settings_show_command_bar_desc)) },
                trailingContent = {
                    Switch(
                        checked = isCommandBarVisible,
                        onCheckedChange = { SettingsManager.setCommandBarVisible(context, it) }
                    )
                }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_show_hidden_files)) },
                supportingContent = { Text(stringResource(R.string.settings_show_hidden_files_desc)) },
                trailingContent = {
                    Switch(
                        checked = showHiddenFiles,
                        onCheckedChange = { SettingsManager.setShowHiddenFiles(context, it) }
                    )
                }
            )

            HorizontalDivider()

            Text(
                stringResource(R.string.settings_custom_theme),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )

            ListItem(
                headlineContent = { 
                    Text(
                        stringResource(R.string.settings_theme),
                        color = if (currentThemePack != null) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface
                    ) 
                },
                supportingContent = {
                    val text = when (themeMode) {
                        ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                        ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                        ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                        ThemeMode.VERY_DARK -> stringResource(R.string.settings_theme_very_dark)
                        ThemeMode.VERY_LIGHT -> stringResource(R.string.settings_theme_very_light)
                        ThemeMode.ENHANCED_SYSTEM -> stringResource(R.string.settings_theme_enhanced_system)
                    }
                    Text(
                        text,
                        color = if (currentThemePack != null) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.clickable(enabled = currentThemePack == null) { showThemeDialog = true }
            )

            ListItem(
                headlineContent = { 
                    Text(
                        stringResource(R.string.settings_icon_theme),
                        color = if (currentThemePack != null) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface
                    )
                },
                supportingContent = {
                    val text = when (iconTheme) {
                        IconTheme.COLOURFULDUO -> stringResource(R.string.settings_icon_theme_colourful_duotone)
                        IconTheme.COLOURFUL -> stringResource(R.string.settings_icon_theme_colourful)
                        IconTheme.MATERIAL -> stringResource(R.string.settings_icon_theme_material)
                    }
                    Text(
                        text,
                        color = if (currentThemePack != null) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.clickable(enabled = currentThemePack == null) { showIconThemeDialog = true }
            )

            if (currentThemePack == null && (iconTheme == IconTheme.COLOURFUL || iconTheme == IconTheme.COLOURFULDUO)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_default_folder_color)) },
                    trailingContent = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(defaultFolderColor))
                        )
                    },
                    modifier = Modifier.clickable { showDefaultFolderColorDialog = true }
                )
            }

            ListItem(
                headlineContent = { 
                    Text(
                        "Custom Theme Mode",
                        color = if (currentThemePack == null) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface
                    )
                },
                supportingContent = {
                    val text = when (customThemeMode) {
                        ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                        ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                        ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                        else -> stringResource(R.string.settings_theme_system)
                    }
                    Text(
                        text,
                        color = if (currentThemePack == null) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.clickable(enabled = currentThemePack != null) { showCustomThemeModeDialog = true }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_load_theme_pack)) },
                supportingContent = { currentThemePack?.let { Text(it.name) } },
                modifier = Modifier.clickable { themePickerLauncher.launch("application/zip") }
            )

            if (currentThemePack != null) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_clear_theme_pack)) },
                    modifier = Modifier.clickable {
                        ThemePackManager.clearPack(context)
                        GlobalEvents.triggerConfigUpdate()
                    }
                )
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_tab_bar_background)) },
                supportingContent = {
                    SettingsManager.tabBarBackgroundUri.value?.let { Text(it) } ?: Text(stringResource(R.string.settings_tab_bar_background_none))
                },
                modifier = Modifier.clickable { backgroundPickerLauncher.launch(arrayOf("image/*")) },
                trailingContent = {
                    if (SettingsManager.tabBarBackgroundUri.value != null) {
                        IconButton(onClick = { SettingsManager.setTabBarBackgroundUri(context, null) }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.settings_clear))
                        }
                    }
                }
            )

            HorizontalDivider()

            HorizontalDivider()

            Text(
                stringResource(R.string.settings_file_ops), 
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_starting_page)) },
                supportingContent = {
                    val text = when (startingPage) {
                        LibraryItem.Home -> stringResource(R.string.nav_home)
                        LibraryItem.Recent -> stringResource(R.string.nav_recent)
                        LibraryItem.Gallery -> stringResource(R.string.nav_gallery)
                        LibraryItem.Music -> stringResource(R.string.nav_music)
                        LibraryItem.Downloads -> stringResource(R.string.nav_downloads)
                        LibraryItem.Documents -> stringResource(R.string.nav_documents)
                        LibraryItem.Games -> stringResource(R.string.nav_game_saves)
                        LibraryItem.InternalStorage -> stringResource(R.string.nav_internal_storage)
                        else -> stringResource(R.string.nav_internal_storage)
                    }
                    Text(text)
                },
                modifier = Modifier.clickable { showStartingPageDialog = true }
            )
            
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_deletion_behavior)) },
                supportingContent = { 
                    val text = when(deleteBehavior) {
                        DeleteBehavior.ASK -> stringResource(R.string.settings_del_ask)
                        DeleteBehavior.RECYCLE -> stringResource(R.string.settings_del_recycle)
                        DeleteBehavior.PERMANENT -> stringResource(R.string.settings_del_permanent)
                    }
                    Text(text)
                },
                modifier = Modifier.clickable { showDeleteDialog = true }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_touch_drag)) },
                supportingContent = {
                    val text = when (touchDragBehavior) {
                        TouchDragBehavior.ASK  -> stringResource(R.string.settings_touch_drag_ask)
                        TouchDragBehavior.COPY -> stringResource(R.string.settings_touch_drag_copy)
                        TouchDragBehavior.MOVE -> stringResource(R.string.settings_touch_drag_move)
                    }
                    Text(text)
                },
                modifier = Modifier.clickable { showTouchDragDialog = true }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_internal_archive)) },
                supportingContent = { Text(stringResource(R.string.settings_internal_archive_desc)) },
                trailingContent = {
                    Switch(
                        checked = isDefaultArchiveViewerEnabled,
                        onCheckedChange = { SettingsManager.setDefaultArchiveViewerEnabled(context, it) }
                    )
                }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_termux_support)) },
                supportingContent = { Text(stringResource(R.string.settings_termux_support_desc)) },
                trailingContent = {
                    Switch(
                        checked = termuxSupport,
                        onCheckedChange = { SettingsManager.setTermuxSupportEnabled(context, it) }
                    )
                }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_icon_selection)) },
                supportingContent = { Text(stringResource(R.string.settings_icon_selection_desc)) },
                trailingContent = {
                    Switch(
                        checked = iconTouchSelection,
                        onCheckedChange = { SettingsManager.setIconTouchSelection(context, it) }
                    )
                }
            )


            HorizontalDivider()

            Text(
                stringResource(R.string.settings_network_services),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_ftp_credentials_title)) },
                supportingContent = { Text(stringResource(R.string.settings_ftp_credentials_desc)) },
                modifier = Modifier.clickable { showFtpCredentialsDialog = true }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_ftp_inactivity_timeout)) },
                supportingContent = {
                    val text = if (ftpInactivityTimeout > 0)
                        stringResource(R.string.settings_timeout_minutes, ftpInactivityTimeout)
                    else stringResource(R.string.settings_timeout_disabled)
                    Text(text)
                },
                modifier = Modifier.clickable { showFtpTimeoutDialog = true }
            )

            HorizontalDivider()

            Text(
                "Shizuku",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )

            val isShizukuAvailable = ShizukuManager.isAvailable()
            val hasShizukuPermission = ShizukuManager.hasPermission()

            ListItem(
                headlineContent = { Text(stringResource(R.string.shizuku_status)) },
                supportingContent = {
                    Text(
                        when {
                            !isShizukuAvailable -> stringResource(R.string.shizuku_status_not_running)
                            hasShizukuPermission -> stringResource(R.string.shizuku_status_authorized)
                            else -> stringResource(R.string.shizuku_status_not_authorized)
                        }
                    )
                },
                trailingContent = {
                    if (isShizukuAvailable && !hasShizukuPermission) {
                        Button(onClick = { ShizukuManager.requestPermission(1002) }) {
                            Text(stringResource(R.string.shizuku_request_permission))
                        }
                    }
                }
            )

            HorizontalDivider()

            Text(
                stringResource(R.string.settings_system),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )



            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_app_inactivity_timeout)) },
                supportingContent = { 
                    val text = if (appInactivityTimeout > 0) 
                        stringResource(R.string.settings_timeout_minutes, appInactivityTimeout)
                    else stringResource(R.string.settings_timeout_disabled)
                    Text(text)
                },
                modifier = Modifier.clickable { showAppTimeoutDialog = true }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_language)) },
                supportingContent = {
                    val text = when (language) {
                        "tr" -> stringResource(R.string.settings_language_turkish)
                        "en" -> stringResource(R.string.settings_language_english)
                        "fi" -> stringResource(R.string.settings_language_finnish)
                        "fr" -> stringResource(R.string.settings_language_french)
                        "pt" -> stringResource(R.string.settings_language_portuguese)
                        "es" -> stringResource(R.string.settings_language_spanish)
                        "de" -> stringResource(R.string.settings_language_german)
                        "ru" -> stringResource(R.string.settings_language_russian)
                        "sv" -> stringResource(R.string.settings_language_swedish)
                        "uk" -> stringResource(R.string.settings_language_ukrainian)
                        "ko" -> stringResource(R.string.settings_language_korean)
                        else -> stringResource(R.string.settings_language_system)
                    }
                    Text(text)
                },
                modifier = Modifier.clickable { showLanguageDialog = true }
            )

            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.shortcuts_title)) },
                supportingContent = { Text(stringResource(R.string.settings_shortcuts_desc)) },
                leadingContent = { Icon(Icons.Default.Keyboard, contentDescription = null) },
                modifier = Modifier.clickable { showShortcutsDialog = true }
            )


            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_about)) },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                modifier = Modifier.clickable { showAboutDialog = true }
            )

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text(stringResource(R.string.settings_choose_del_behavior)) },
                    text = {
                        Column {
                            OptionItem(
                                label = stringResource(R.string.settings_del_ask),
                                selected = deleteBehavior == DeleteBehavior.ASK,
                                onClick = { 
                                    SettingsManager.setDeleteBehavior(context, DeleteBehavior.ASK)
                                    showDeleteDialog = false
                                }
                            )
                            OptionItem(
                                label = stringResource(R.string.settings_del_recycle),
                                selected = deleteBehavior == DeleteBehavior.RECYCLE,
                                onClick = { 
                                    SettingsManager.setDeleteBehavior(context, DeleteBehavior.RECYCLE)
                                    showDeleteDialog = false
                                }
                            )
                            OptionItem(
                                label = stringResource(R.string.settings_del_permanent),
                                selected = deleteBehavior == DeleteBehavior.PERMANENT,
                                onClick = { 
                                    SettingsManager.setDeleteBehavior(context, DeleteBehavior.PERMANENT)
                                    showDeleteDialog = false
                                }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            if (showTouchDragDialog) {
                AlertDialog(
                    onDismissRequest = { showTouchDragDialog = false },
                    title = { Text(stringResource(R.string.settings_touch_drag_title)) },
                    text = {
                        Column {
                            OptionItem(
                                label = stringResource(R.string.settings_touch_drag_ask),
                                selected = touchDragBehavior == TouchDragBehavior.ASK,
                                onClick = {
                                    SettingsManager.setTouchDragBehavior(context, TouchDragBehavior.ASK)
                                    showTouchDragDialog = false
                                }
                            )
                            OptionItem(
                                label = stringResource(R.string.settings_touch_drag_copy),
                                selected = touchDragBehavior == TouchDragBehavior.COPY,
                                onClick = {
                                    SettingsManager.setTouchDragBehavior(context, TouchDragBehavior.COPY)
                                    showTouchDragDialog = false
                                }
                            )
                            OptionItem(
                                label = stringResource(R.string.settings_touch_drag_move),
                                selected = touchDragBehavior == TouchDragBehavior.MOVE,
                                onClick = {
                                    SettingsManager.setTouchDragBehavior(context, TouchDragBehavior.MOVE)
                                    showTouchDragDialog = false
                                }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showTouchDragDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            if (showThemeDialog) {
                AlertDialog(
                    onDismissRequest = { showThemeDialog = false },
                    title = { Text(stringResource(R.string.settings_choose_theme)) },
                    text = {
                        Column {
                            OptionItem(
                                label = stringResource(R.string.settings_theme_system),
                                selected = themeMode == ThemeMode.SYSTEM,
                                onClick = {
                                    SettingsManager.setThemeMode(context, ThemeMode.SYSTEM)
                                    showThemeDialog = false
                                }
                            )
                            OptionItem(
                                label = stringResource(R.string.settings_theme_light),
                                selected = themeMode == ThemeMode.LIGHT,
                                onClick = {
                                    SettingsManager.setThemeMode(context, ThemeMode.LIGHT)
                                    showThemeDialog = false
                                }
                            )
                            OptionItem(
                                label = stringResource(R.string.settings_theme_dark),
                                selected = themeMode == ThemeMode.DARK,
                                onClick = {
                                    SettingsManager.setThemeMode(context, ThemeMode.DARK)
                                    showThemeDialog = false
                                }
                            )
                            OptionItem(
                                label = stringResource(R.string.settings_theme_very_dark),
                                selected = themeMode == ThemeMode.VERY_DARK,
                                onClick = {
                                    SettingsManager.setThemeMode(context, ThemeMode.VERY_DARK)
                                    showThemeDialog = false
                                }
                            )
                            OptionItem(
                                label = stringResource(R.string.settings_theme_very_light),
                                selected = themeMode == ThemeMode.VERY_LIGHT,
                                onClick = {
                                    SettingsManager.setThemeMode(context, ThemeMode.VERY_LIGHT)
                                    showThemeDialog = false
                                }
                            )
                            OptionItem(
                                label = stringResource(R.string.settings_theme_enhanced_system),
                                selected = themeMode == ThemeMode.ENHANCED_SYSTEM,
                                onClick = {
                                    SettingsManager.setThemeMode(context, ThemeMode.ENHANCED_SYSTEM)
                                    showThemeDialog = false
                                }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showThemeDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            if (showThemeTopDialog) {
                AlertDialog(
                    onDismissRequest = { showThemeTopDialog = false },
                    title = { Text(stringResource(R.string.settings_themetop)) },
                    text = {
                        Column {
                            OptionItem(
                                label = stringResource(R.string.settings_themetop_attached),
                                selected = themeTop == ThemeTopMode.ATTACHED,
                                onClick = {
                                    SettingsManager.setThemeTop(context, ThemeTopMode.ATTACHED)
                                    showThemeTopDialog = false
                                }
                            )
                            OptionItem(
                                label = stringResource(R.string.settings_themetop_float),
                                selected = themeTop == ThemeTopMode.FLOAT,
                                onClick = {
                                    SettingsManager.setThemeTop(context, ThemeTopMode.FLOAT)
                                    showThemeTopDialog = false
                                }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showThemeTopDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            if (showIconThemeDialog) {
                AlertDialog(
                    onDismissRequest = { showIconThemeDialog = false },
                    title = { Text(stringResource(R.string.settings_choose_icon_theme)) },
                    text = {
                        Column {
                            OptionItem(
                                label = stringResource(R.string.settings_icon_theme_colourful_duotone),
                                selected = iconTheme == IconTheme.COLOURFULDUO,
                                onClick = {
                                    SettingsManager.setIconTheme(context, IconTheme.COLOURFULDUO)
                                    showIconThemeDialog = false
                                }
                            )
                            OptionItem(
                                label = stringResource(R.string.settings_icon_theme_colourful),
                                selected = iconTheme == IconTheme.COLOURFUL,
                                onClick = {
                                    SettingsManager.setIconTheme(context, IconTheme.COLOURFUL)
                                    showIconThemeDialog = false
                                }
                            )
                            OptionItem(
                                label = stringResource(R.string.settings_icon_theme_material),
                                selected = iconTheme == IconTheme.MATERIAL,
                                onClick = {
                                    SettingsManager.setIconTheme(context, IconTheme.MATERIAL)
                                    showIconThemeDialog = false
                                }
                            )

                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showIconThemeDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            if (showCustomThemeModeDialog) {
                AlertDialog(
                    onDismissRequest = { showCustomThemeModeDialog = false },
                    title = { Text(stringResource(R.string.settings_custom_theme_mode)) },
                    text = {
                        Column {
                            OptionItem(
                                label = stringResource(R.string.settings_theme_system),
                                selected = customThemeMode == ThemeMode.SYSTEM,
                                onClick = {
                                    SettingsManager.setCustomThemeMode(context, ThemeMode.SYSTEM)
                                    showCustomThemeModeDialog = false
                                }
                            )
                            OptionItem(
                                label = stringResource(R.string.settings_theme_light),
                                selected = customThemeMode == ThemeMode.LIGHT,
                                onClick = {
                                    SettingsManager.setCustomThemeMode(context, ThemeMode.LIGHT)
                                    showCustomThemeModeDialog = false
                                }
                            )
                            OptionItem(
                                label = stringResource(R.string.settings_theme_dark),
                                selected = customThemeMode == ThemeMode.DARK,
                                onClick = {
                                    SettingsManager.setCustomThemeMode(context, ThemeMode.DARK)
                                    showCustomThemeModeDialog = false
                                }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showCustomThemeModeDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            if (showThemeBarDialog) {
                AlertDialog(
                    onDismissRequest = { showThemeBarDialog = false },
                    title = { Text(stringResource(R.string.settings_themebar)) },
                    text = {
                        Column {
                            OptionItem(
                                label = stringResource(R.string.settings_themebar_rounded),
                                selected = themeBar == ThemeShape.ROUNDED,
                                onClick = {
                                    SettingsManager.setThemeBar(context, ThemeShape.ROUNDED)
                                    showThemeBarDialog = false
                                }
                            )
                            OptionItem(
                                label = stringResource(R.string.settings_themebar_square),
                                selected = themeBar == ThemeShape.SQUARE,
                                onClick = {
                                    SettingsManager.setThemeBar(context, ThemeShape.SQUARE)
                                    showThemeBarDialog = false
                                }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showThemeBarDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            if (showThemeContentDialog) {
                AlertDialog(
                    onDismissRequest = { showThemeContentDialog = false },
                    title = { Text(stringResource(R.string.settings_themecontent)) },
                    text = {
                        Column {
                            OptionItem(
                                label = stringResource(R.string.settings_themecontent_rounded),
                                selected = themeContent == ThemeShape.ROUNDED,
                                onClick = {
                                    SettingsManager.setThemeContent(context, ThemeShape.ROUNDED)
                                    showThemeContentDialog = false
                                }
                            )
                            OptionItem(
                                label = stringResource(R.string.settings_themecontent_square),
                                selected = themeContent == ThemeShape.SQUARE,
                                onClick = {
                                    SettingsManager.setThemeContent(context, ThemeShape.SQUARE)
                                    showThemeContentDialog = false
                                }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showThemeContentDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }



            if (showDefaultFolderColorDialog) {
                AlertDialog(
                    onDismissRequest = { showDefaultFolderColorDialog = false },
                    title = { Text(stringResource(R.string.settings_default_folder_color)) },
                    text = {
                        Column {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThemeFolderColors.defaultOptions.forEach { colorLong ->
                                    val color = Color(colorLong)
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .clickable {
                                                SettingsManager.setDefaultFolderColor(context, colorLong)
                                                showDefaultFolderColorDialog = false
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (defaultFolderColor == colorLong) {
                                            Icon(
                                                Icons.Default.Done,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showDefaultFolderColorDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            if (showDetailsDialog) {
                AlertDialog(
                    onDismissRequest = { showDetailsDialog = false },
                    title = { Text(stringResource(R.string.settings_choose_details_mode)) },
                    text = {
                        Column {
                            OptionItem(
                                label = stringResource(R.string.settings_details_hidden),
                                selected = detailsMode == DetailsMode.OFF,
                                onClick = {
                                    SettingsManager.setDetailsMode(context, DetailsMode.OFF)
                                    showDetailsDialog = false
                                }
                            )
                            OptionItem(
                                label = stringResource(R.string.settings_details_pane),
                                selected = detailsMode == DetailsMode.PANE,
                                onClick = {
                                    SettingsManager.setDetailsMode(context, DetailsMode.PANE)
                                    showDetailsDialog = false
                                }
                            )
                            OptionItem(
                                label = stringResource(R.string.settings_details_bar),
                                selected = detailsMode == DetailsMode.BAR,
                                onClick = {
                                    SettingsManager.setDetailsMode(context, DetailsMode.BAR)
                                    showDetailsDialog = false
                                }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showDetailsDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            if (showStartingPageDialog) {
                AlertDialog(
                    onDismissRequest = { showStartingPageDialog = false },
                    title = { Text(stringResource(R.string.settings_choose_starting_page)) },
                    text = {
                        Column {
                            val items = listOf(
                                LibraryItem.Home to R.string.nav_home,
                                LibraryItem.InternalStorage to R.string.nav_internal_storage,
                                LibraryItem.Recent to R.string.nav_recent,
                                LibraryItem.Gallery to R.string.nav_gallery,
                                LibraryItem.Music to R.string.nav_music,
                                LibraryItem.Downloads to R.string.nav_downloads,
                                LibraryItem.Documents to R.string.nav_documents,
                                LibraryItem.Games to R.string.nav_game_saves,
                                LibraryItem.RecycleBin to R.string.nav_recycle_bin
                            )
                            items.forEach { (item, resId) ->
                                OptionItem(
                                    label = stringResource(resId),
                                    selected = startingPage == item,
                                    onClick = {
                                        SettingsManager.setStartingPage(context, item)
                                        showStartingPageDialog = false
                                    }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showStartingPageDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            if (showDefaultViewModeDialog) {
                AlertDialog(
                    onDismissRequest = { showDefaultViewModeDialog = false },
                    title = { Text(stringResource(R.string.settings_choose_default_view_mode)) },
                    text = {
                        Column {
                            OptionItem(
                                label = stringResource(R.string.menu_details),
                                selected = defaultViewMode == ViewMode.DETAILS,
                                onClick = {
                                    SettingsManager.setDefaultViewMode(context, ViewMode.DETAILS)
                                    showDefaultViewModeDialog = false
                                }
                            )
                            OptionItem(
                                label = stringResource(R.string.menu_content),
                                selected = defaultViewMode == ViewMode.CONTENT,
                                onClick = {
                                    SettingsManager.setDefaultViewMode(context, ViewMode.CONTENT)
                                    showDefaultViewModeDialog = false
                                }
                            )
                            OptionItem(
                                label = stringResource(R.string.menu_grid),
                                selected = defaultViewMode == ViewMode.GRID,
                                onClick = {
                                    SettingsManager.setDefaultViewMode(context, ViewMode.GRID)
                                    showDefaultViewModeDialog = false
                                }
                            )
                            OptionItem(
                                label = stringResource(R.string.menu_gallery),
                                selected = defaultViewMode == ViewMode.GALLERY,
                                onClick = {
                                    SettingsManager.setDefaultViewMode(context, ViewMode.GALLERY)
                                    showDefaultViewModeDialog = false
                                }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showDefaultViewModeDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
            if (showLanguageDialog) {
                AlertDialog(
                    onDismissRequest = { showLanguageDialog = false },
                    title = { Text(stringResource(R.string.settings_choose_language)) },
                    text = {
                        val languages = remember {
                            listOf(
                                "en" to R.string.settings_language_english,
                                "fi" to R.string.settings_language_finnish,
                                "fr" to R.string.settings_language_french,
                                "de" to R.string.settings_language_german,
                                "ko" to R.string.settings_language_korean,
                                "pt" to R.string.settings_language_portuguese,
                                "ru" to R.string.settings_language_russian,
                                "es" to R.string.settings_language_spanish,
                                "sv" to R.string.settings_language_swedish,
                                "tr" to R.string.settings_language_turkish,
                                "uk" to R.string.settings_language_ukrainian
                            )
                        }
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            OptionItem(
                                label = stringResource(R.string.settings_language_system),
                                selected = language == "system",
                                onClick = {
                                    SettingsManager.setLanguage(context, "system")
                                    showLanguageDialog = false
                                }
                            )
                            languages.forEach { (tag, resId) ->
                                OptionItem(
                                    label = stringResource(resId),
                                    selected = language == tag,
                                    onClick = {
                                        SettingsManager.setLanguage(context, tag)
                                        showLanguageDialog = false
                                    }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showLanguageDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            if (showShortcutsDialog) {
                LaunchedEffect(Unit) {
                    FileOperationsManager.showShortcuts()
                    val intent = Intent(context, PopUpActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    showShortcutsDialog = false
                }
            }


            if (showFtpCredentialsDialog) {
                var tempUser by remember { mutableStateOf(SettingsManager.ftpUser.value) }
                var tempPass by remember { mutableStateOf(SettingsManager.ftpPassword.value) }

                AlertDialog(
                    onDismissRequest = { showFtpCredentialsDialog = false },
                    title = { Text(stringResource(R.string.settings_ftp_credentials_title)) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = tempUser,
                                onValueChange = { tempUser = it },
                                label = { Text(stringResource(R.string.settings_ftp_user)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = tempPass,
                                onValueChange = { tempPass = it },
                                label = { Text(stringResource(R.string.settings_ftp_password)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            SettingsManager.setFtpCredentials(context, tempUser, tempPass)
                            showFtpCredentialsDialog = false
                        }) {
                            Text(stringResource(R.string.nav_network_save))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showFtpCredentialsDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            if (showFtpTimeoutDialog) {
                TimeoutDialog(
                    title = stringResource(R.string.settings_ftp_inactivity_timeout),
                    currentValue = ftpInactivityTimeout,
                    onDismiss = { showFtpTimeoutDialog = false },
                    onConfirm = { 
                        SettingsManager.setFtpInactivityTimeout(context, it)
                        showFtpTimeoutDialog = false
                    }
                )
            }

            if (showAppTimeoutDialog) {
                TimeoutDialog(
                    title = stringResource(R.string.settings_app_inactivity_timeout),
                    currentValue = appInactivityTimeout,
                    onDismiss = { showAppTimeoutDialog = false },
                    onConfirm = { 
                        SettingsManager.setAppInactivityTimeout(context, it)
                        showAppTimeoutDialog = false
                    }
                )
            }

            if (showAboutDialog) {
                val uriHandler = LocalUriHandler.current

                // This part safely loads your app icon so it doesn't crash
                val context = LocalContext.current
                val appIcon = remember {
                    context.packageManager.getApplicationIcon(context.packageName)
                }

                val version = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                } catch (_: Exception) {
                    "1.0"
                }

                AlertDialog(
                    onDismissRequest = { showAboutDialog = false },
                    text = {
                        Row {
                            Image(
                                painter = rememberAsyncImagePainter(appIcon),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(MaterialTheme.shapes.small)
                            )
                            Spacer(modifier = Modifier.width(16.dp))

                            // App Name and Version
                            Column {
                                Text(
                                    "Continuum Explorer",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "$version",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "App is made under GPL-3.0 license" + "and based on troikoss/continuum_explorer",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(20.dp))


                                Text(
                                    text = "Icons used:",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "- Phosphor by Phosphor Icons ",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "- Material Design Icons by Google",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "- Lets Icons by Leonid Tsvetkov",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "- IconPark Outline by ByteDance",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "- Bootstrap Icons by The Bootstrap Authors",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "- Solar by 480 Design",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "Font:",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 16.sp
                                )
                                Text(
                                    text = "- Inter Designed by Rasmus Andersson ",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "Links:",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 16.sp
                                )

                                Text(
                                    text = "GitHub troikoss",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline), // Adds the underline
                                    modifier = Modifier
                                        .clickable {
                                            uriHandler.openUri("https://github.com/troikoss/Continuum-Explorer")
                                        }
                                        .padding(top = 4.dp)
                                )
                                Text(
                                    text = "GitHub johakovi",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline // Adds the underline
                                    ),
                                    modifier = Modifier
                                        .clickable {
                                            uriHandler.openUri("https://github.com/johakovi/Continuum-Explorer-Memories")
                                        }
                                        .padding(top = 4.dp))
                                Text(
                                    text = "wavy-slider by mahozad",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline), // Adds the underline
                                    lineHeight = 16.sp,
                                    modifier = Modifier
                                        .clickable {
                                            uriHandler.openUri("https://github.com/mahozad/wavy-slider")
                                        }
                                        .padding(top = 4.dp)
                                )


                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showAboutDialog = false }) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TimeoutDialog(
    title: String,
    currentValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val options = listOf(0, 1, 2, 5, 10, 30, 60)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { minutes ->
                    val label = if (minutes == 0) stringResource(R.string.settings_timeout_disabled)
                                else stringResource(R.string.settings_timeout_minutes, minutes)
                    OptionItem(
                        label = label,
                        selected = currentValue == minutes,
                        onClick = { onConfirm(minutes) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun ShortcutCategory(name: String) {
    Text(
        text = name,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun ShortcutItem(keys: String, action: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = keys, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Text(text = action, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun OptionItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
