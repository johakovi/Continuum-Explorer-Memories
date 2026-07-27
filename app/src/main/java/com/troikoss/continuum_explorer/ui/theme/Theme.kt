package com.troikoss.continuum_explorer.ui.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.troikoss.continuum_explorer.managers.SettingsManager
import com.troikoss.continuum_explorer.managers.ThemeMode
import com.troikoss.continuum_explorer.managers.IconTheme
import com.troikoss.continuum_explorer.managers.ThemePackManager
import com.troikoss.continuum_explorer.managers.CustomThemeColors

data class ExtendedColors(
    val sidebarBackground: Color,
    val topBarBackground: Color,
    val navButtonBackground: Color,
    val searchBoxBackground: Color,
    val tabBarBackground: Color,
    val selectionBackground: Color,
    val sidebarIcons: Color,
    val homeIcon: Color,
    val folderIcon: Color,
    val galleryIcon: Color,
    val recentIcon: Color,
    val filesIcon: Color,
    val documentsIcon: Color,
    val gameIcon: Color,
    val gameShortcutIcon: Color,
    val recycleBinIcon: Color,
    val downloadsIcon: Color,
    val androidIcon: Color,
    val zipIcon: Color,
    val pdfIcon: Color,
    val xlsIcon: Color,
    val docxIcon: Color,
    val txtIcon: Color,
    val terminalIcon: Color,
    val imageIcon: Color,
    val videoIcon: Color,
    val audioIcon: Color,
    val musicIcon: Color,
    val dcimIcon: Color,
    val picturesIcon: Color,
    val folderIconDuo: Color,
    val homeIconDuo: Color,
    val filesIconDuo: Color,
    val recentIconDuo: Color,
    val documentsIconDuo: Color,
    val gameIconDuo: Color,
    val gameShortcutIconDuo: Color,
    val galleryIconDuo: Color,
    val recycleBinIconDuo: Color,
    val downloadsIconDuo: Color,
    val androidIconDuo: Color,
    val zipIconDuo: Color,
    val pdfIconDuo: Color,
    val xlsIconDuo: Color,
    val docxIconDuo: Color,
    val txtIconDuo: Color,
    val terminalIconDuo: Color,
    val imageIconDuo: Color,
    val videoIconDuo: Color,
    val audioIconDuo: Color,
    val musicIconDuo: Color,
    val dcimIconDuo: Color,
    val picturesIconDuo: Color,
    val tabActiveBackground: Color,
    val textColor: Color,
    val menuBackground: Color,
    val fileViewBackground: Color,
    val background: Color,
    val commandPanelBackground: Color,
    val statusBarColor: Color,
    val navigationBarColor: Color,
    val outline: Color,
    val outlineVariant: Color
)

fun ExtendedColors.withCustomColors(custom: CustomThemeColors): ExtendedColors {
    return this.copy(
        sidebarBackground = custom.sidebarBackground ?: sidebarBackground,
        topBarBackground = custom.topBarBackground ?: topBarBackground,
        navButtonBackground = custom.navButtonBackground ?: navButtonBackground,
        searchBoxBackground = custom.searchBoxBackground ?: searchBoxBackground,
        tabBarBackground = custom.tabBarBackground ?: tabBarBackground,
        selectionBackground = custom.selectionBackground ?: selectionBackground,
        sidebarIcons = custom.sidebarIcons ?: sidebarIcons,
        homeIcon = custom.homeIcon ?: homeIcon,
        folderIcon = custom.folderIcon ?: folderIcon,
        galleryIcon = custom.galleryIcon ?: galleryIcon,
        recentIcon = custom.recentIcon ?: recentIcon,
        filesIcon = custom.filesIcon ?: filesIcon,
        documentsIcon = custom.documentsIcon ?: documentsIcon,
        gameIcon = custom.gameIcon ?: gameIcon,
        gameShortcutIcon = custom.gameShortcutIcon ?: gameShortcutIcon,
        recycleBinIcon = custom.recycleBinIcon ?: recycleBinIcon,
        downloadsIcon = custom.downloadsIcon ?: downloadsIcon,
        androidIcon = custom.androidIcon ?: androidIcon,
        zipIcon = custom.zipIcon ?: zipIcon,
        pdfIcon = custom.pdfIcon ?: pdfIcon,
        xlsIcon = custom.xlsIcon ?: xlsIcon,
        docxIcon = custom.docxIcon ?: docxIcon,
        txtIcon = custom.txtIcon ?: txtIcon,
        terminalIcon = custom.terminalIcon ?: terminalIcon,
        imageIcon = custom.imageIcon ?: imageIcon,
        videoIcon = custom.videoIcon ?: videoIcon,
        audioIcon = custom.audioIcon ?: audioIcon,
        musicIcon = custom.musicIcon ?: musicIcon,
        dcimIcon = custom.dcimIcon ?: custom.galleryIcon ?: dcimIcon,
        picturesIcon = custom.picturesIcon ?: custom.galleryIcon ?: picturesIcon,
        folderIconDuo = custom.folderIconDuo ?: custom.folderIcon ?: folderIconDuo,
        homeIconDuo = custom.homeIconDuo ?: homeIconDuo,
        filesIconDuo = custom.filesIconDuo ?: custom.filesIcon ?: filesIconDuo,
        recentIconDuo = custom.recentIconDuo ?: custom.recentIcon ?: recentIconDuo,
        documentsIconDuo = custom.documentsIconDuo ?: custom.documentsIcon ?: documentsIconDuo,
        gameIconDuo = custom.gameIconDuo ?: custom.gameIcon ?: gameIconDuo,
        gameShortcutIconDuo = custom.gameShortcutIconDuo ?: custom.gameShortcutIcon ?: gameShortcutIconDuo,
        galleryIconDuo = custom.galleryIconDuo ?: custom.galleryIcon ?: galleryIconDuo,
        recycleBinIconDuo = custom.recycleBinIconDuo ?: custom.recycleBinIcon ?: recycleBinIconDuo,
        downloadsIconDuo = custom.downloadsIconDuo ?: custom.downloadsIcon ?: downloadsIconDuo,
        androidIconDuo = custom.androidIconDuo ?: custom.androidIcon ?: androidIconDuo,
        zipIconDuo = custom.zipIconDuo ?: custom.zipIcon ?: zipIconDuo,
        pdfIconDuo = custom.pdfIconDuo ?: custom.pdfIcon ?: pdfIconDuo,
        xlsIconDuo = custom.xlsIconDuo ?: custom.xlsIcon ?: xlsIconDuo,
        docxIconDuo = custom.docxIconDuo ?: custom.docxIcon ?: docxIconDuo,
        txtIconDuo = custom.txtIconDuo ?: custom.txtIcon ?: txtIconDuo,
        terminalIconDuo = custom.terminalIconDuo ?: custom.terminalIcon ?: terminalIconDuo,
        imageIconDuo = custom.imageIconDuo ?: custom.imageIcon ?: imageIconDuo,
        videoIconDuo = custom.videoIconDuo ?: custom.videoIcon ?: videoIconDuo,
        audioIconDuo = custom.audioIconDuo ?: custom.audioIcon ?: audioIconDuo,
        musicIconDuo = custom.musicIconDuo ?: custom.musicIcon ?: musicIconDuo,
        dcimIconDuo = custom.dcimIconDuo ?: custom.dcimIcon ?: custom.galleryIcon ?: dcimIconDuo,
        picturesIconDuo = custom.picturesIconDuo ?: custom.picturesIcon ?: custom.galleryIcon ?: picturesIconDuo,
        tabActiveBackground = custom.tabActiveBackground ?: tabActiveBackground,
        textColor = custom.textColor ?: textColor,
        menuBackground = custom.menuBackground ?: menuBackground,
        fileViewBackground = custom.fileViewBackground ?: fileViewBackground,
        background = custom.background ?: background,
        commandPanelBackground = custom.commandPanelBackground ?: commandPanelBackground,
        statusBarColor = custom.statusBarColor ?: statusBarColor,
        navigationBarColor = custom.navigationBarColor ?: navigationBarColor,
        outline = custom.outline ?: outline,
        outlineVariant = custom.outlineVariant ?: outlineVariant
    )
}

private fun androidx.compose.material3.ColorScheme.withCustomColors(custom: CustomThemeColors): androidx.compose.material3.ColorScheme {
    return this.copy(
        primary = custom.primary ?: custom.sidebarIcons ?: primary,
        onPrimary = custom.onPrimary ?: custom.textColor ?: onPrimary,
        primaryContainer = custom.primaryContainer ?: custom.selectionBackground ?: primaryContainer,
        onPrimaryContainer = custom.onPrimaryContainer ?: custom.onPrimary ?: custom.textColor ?: onPrimaryContainer,
        secondary = custom.secondary ?: custom.sidebarIcons ?: secondary,
        onSecondary = custom.onSecondary ?: custom.textColor ?: onSecondary,
        secondaryContainer = custom.secondaryContainer ?: custom.selectionBackground ?: secondaryContainer,
        onSecondaryContainer = custom.onSecondaryContainer ?: custom.onSecondary ?: custom.textColor ?: onSecondaryContainer,
        tertiary = custom.tertiary ?: custom.sidebarIcons ?: tertiary,
        onTertiary = custom.onTertiary ?: custom.textColor ?: onTertiary,
        tertiaryContainer = custom.tertiaryContainer ?: tertiaryContainer,
        onTertiaryContainer = custom.onTertiaryContainer ?: onTertiaryContainer,
        background = custom.backgroundM3 ?: custom.background ?: background,
        onBackground = custom.onBackground ?: custom.textColor ?: onBackground,
        surface = custom.surface ?: custom.topBarBackground ?: custom.background ?: surface,
        onSurface = custom.onSurface ?: custom.textColor ?: onSurface,
        surfaceVariant = custom.surfaceVariant ?: custom.searchBoxBackground ?: custom.background ?: surfaceVariant,
        onSurfaceVariant = custom.onSurfaceVariant ?: custom.onSurface ?: custom.textColor ?: onSurfaceVariant,
        surfaceContainer = custom.surfaceContainer ?: custom.topBarBackground ?: custom.background ?: surface,
        surfaceContainerLow = custom.surfaceContainerLow ?: custom.sidebarBackground ?: custom.background ?: surface,
        surfaceContainerHigh = custom.surfaceContainerHigh ?: custom.sidebarBackground ?: custom.background ?: surface,
        surfaceContainerHighest = custom.surfaceContainerHighest ?: custom.tabBarBackground ?: custom.background ?: surface,
        surfaceContainerLowest = custom.surfaceContainerLowest ?: custom.fileViewBackground ?: custom.background ?: surface,
        outline = custom.outline ?: custom.sidebarIcons ?: outline,
        outlineVariant = custom.outlineVariant ?: outlineVariant
    )
}

val LocalExtendedColors = staticCompositionLocalOf<ExtendedColors> {
    error("No ExtendedColors provided")
}

object ThemeFolderColors {
    val defaultOptions = listOf(
        0xFF2196F3, // Blue
        0xFFF44336, // Red
        0xFF4CAF50, // Green
        0xFFFFEB3B, // Yellow
        0xFFFF9800, // Orange
        0xFF9C27B0, // Purple
        0xFFE91E63, // Pink
        0xFF795548, // Brown
        0xFF9E9E9E, // Grey
        0xFF00BCD4, // Cyan
        0xFFFF5722, // Deep Orange
        0xFF3F51B5, // Indigo
        0xFF009688, // Teal
        0xFFCDDC39, // Lime
        0xFFFFC107, // Amber
        0xFF673AB7, // Deep Purple
        0xFF03A9F4, // Light Blue
        0xFF8BC34A, // Light Green
        0xFF607D8B, // Blue Grey
        0xFF212121, // Charcoal
        0xFFFF00FF, // Fuchsia
        0xFFFF69B4  // Hot Pink
    )
}

object FileExplorerTheme {
    val extendedColors: ExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current
}

private val VeryDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF00372A),
    onPrimaryContainer = Color(0xFF84D9D2),
    secondary = DarkSecondary,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF1B3535),
    onSecondaryContainer = Color(0xFFCCCCCE),
    tertiary = DarkTertiary,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF00372A),
    onTertiaryContainer = Color(0xFF84D9D2),
    background = Color(0xFF000000),
    onBackground = VeryDarkText,
    surface = VeryDarkTopBar,
    onSurface = VeryDarkText,
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = VeryDarkIcons,
    surfaceContainer = VeryDarkTopBar,
    surfaceContainerLow = VeryDarkSidebar,
    surfaceContainerHigh = Color(0xFF1C1C1C),
    surfaceContainerLowest = Color(0xFF000000),
    outline = Color(0xFF444446),
    outlineVariant = Color(0xFF353537)
)

private val VeryLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF00372A),
    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0E0),
    onSecondaryContainer = Color(0xFF444344),
    tertiary = LightTertiary,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFF1F1F1),
    onTertiaryContainer = Color(0xFF090909),
    background = Color(0xFFF1F1F1),
    onBackground = VeryLightText,
    surface = VeryLightTopBar,
    onSurface = VeryLightText,
    surfaceVariant = Color(0xFFE8E8E8),
    onSurfaceVariant = VeryLightIcons,
    surfaceContainer = VeryLightTopBar,
    surfaceContainerLow = VeryLightSidebar,
    surfaceContainerHigh = Color(0xFFE8E8E8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    outline = Color(0xFFA2A2A2),
    outlineVariant = Color(0xFFA2A2A2)
)


@Composable
fun FileExplorerTheme(
    // Dynamic colour is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val themeMode = SettingsManager.themeMode.value
    val customThemeMode = SettingsManager.customThemeMode.value
    val currentPack = ThemePackManager.currentPack.value
    val iconStyle = SettingsManager.iconStyle.value
    val isSystemDark = isSystemInDarkTheme()
    val defaultFolderColor = Color(SettingsManager.defaultFolderColor.value)

    fun getEffectiveTheme(style: com.troikoss.continuum_explorer.managers.IconStyle, customTheme: IconTheme): IconTheme {
        return when (style) {
            com.troikoss.continuum_explorer.managers.IconStyle.MATERIAL -> IconTheme.MATERIAL
            com.troikoss.continuum_explorer.managers.IconStyle.COLOURFUL -> IconTheme.COLOURFUL
            com.troikoss.continuum_explorer.managers.IconStyle.COLOURFULDUO -> IconTheme.COLOURFULDUO
            com.troikoss.continuum_explorer.managers.IconStyle.CUSTOM -> customTheme
        }
    }

    val musicTheme = getEffectiveTheme(iconStyle, SettingsManager.musicIconTheme.value)
    val sidebarTheme = getEffectiveTheme(iconStyle, SettingsManager.sidebarIconTheme.value)
    val folderTheme = getEffectiveTheme(iconStyle, SettingsManager.folderIconTheme.value)

    val darkTheme = if (currentPack != null) {
        when (customThemeMode) {
            ThemeMode.SYSTEM -> isSystemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            else -> isSystemDark
        }
    } else {
        when (themeMode) {
            ThemeMode.SYSTEM, ThemeMode.ENHANCED_SYSTEM -> isSystemDark
            ThemeMode.DARK, ThemeMode.VERY_DARK -> true
            ThemeMode.LIGHT, ThemeMode.VERY_LIGHT -> false
        }
    }

    val isVeryDark = currentPack == null && (themeMode == ThemeMode.VERY_DARK || (themeMode == ThemeMode.ENHANCED_SYSTEM && isSystemDark))
    val isVeryLight = currentPack == null && (themeMode == ThemeMode.VERY_LIGHT || (themeMode == ThemeMode.ENHANCED_SYSTEM && !isSystemDark))

    val dynamicScheme = if (dynamicColor && currentPack == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else null

    var colorScheme = when {
        isVeryDark -> VeryDarkColorScheme
        isVeryLight -> VeryLightColorScheme
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    if (dynamicScheme != null) {
        colorScheme = colorScheme.copy(
            primary = dynamicScheme.primary,
            secondary = dynamicScheme.secondary,
            tertiary = dynamicScheme.tertiary,
            primaryContainer = dynamicScheme.primaryContainer,
            secondaryContainer = dynamicScheme.secondaryContainer,
            tertiaryContainer = dynamicScheme.tertiaryContainer
        )
    }

    val finalColorScheme = currentPack?.let { pack ->
        if (darkTheme) colorScheme.withCustomColors(pack.darkColors)
        else colorScheme.withCustomColors(pack.lightColors)
    } ?: colorScheme

    val baseExtendedColors = when {
        isVeryDark -> {
            val sidebarIconColor = if (sidebarTheme == IconTheme.MATERIAL) colorScheme.primary else VeryDarkIcons
            val folderIconColor = if (folderTheme == IconTheme.MATERIAL) colorScheme.primary else VeryDarkIcons
            val musicIconColor = if (musicTheme == IconTheme.MATERIAL) colorScheme.primary else VeryDarkIcons
            ExtendedColors(
                sidebarBackground = VeryDarkSidebar,
                topBarBackground = VeryDarkTopBar,
                navButtonBackground = Color(0xFF000000),
                searchBoxBackground = Color(0xFF1A1A1A),
                tabBarBackground = Color(0xFF2D2D2F),
                selectionBackground = DarkPrimarySelection,
                sidebarIcons = sidebarIconColor,
                homeIcon = if (sidebarTheme == IconTheme.COLOURFUL) ThemeHome else if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeHomeDuo else sidebarIconColor,
                folderIcon = if (folderTheme == IconTheme.COLOURFUL || folderTheme == IconTheme.COLOURFULDUO) defaultFolderColor else folderIconColor,
                galleryIcon = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else ThemeGallery,
                recentIcon = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeRecentDuo else ThemeRecent,
                filesIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeFileDuo else ThemeFile,
                documentsIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeFilesDuo else ThemeFiles,
                gameIcon = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeGameSavesDuo else ThemeGameSaves,
                gameShortcutIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeXlsDuo else ThemeXls,
                recycleBinIcon = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeRecycleBinDuo else ThemeRecycleBin,
                downloadsIcon = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeDownloadsDuo else ThemeDownloads,
                androidIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeAndroidDuo else ThemeAndroid,
                zipIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeZipDuo else ThemeZip,
                pdfIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemePdfDuo else ThemePdf,
                xlsIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeXlsDuo else ThemeXls,
                docxIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeDocxDuo else ThemeDocx,
                txtIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeTxtDuo else ThemeTxt,
                terminalIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeTerminalDuo else ThemeTerminal,
                imageIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeImageDuo else ThemeImage,
                videoIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeVideoDuo else ThemeVideo,
                audioIcon = if (musicTheme == IconTheme.COLOURFULDUO) ThemeAudioDuo else ThemeAudio,
                musicIcon = if (musicTheme == IconTheme.COLOURFULDUO) ThemeAudioDuo else ThemeAudio,
                dcimIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else ThemeGallery,
                picturesIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else ThemeGallery,
                folderIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) defaultFolderColor else folderIconColor,
                homeIconDuo = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeHomeDuo else sidebarIconColor,
                filesIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeFileDuo else folderIconColor,
                galleryIconDuo = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else sidebarIconColor,
                recentIconDuo = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeRecentDuo else sidebarIconColor,
                documentsIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeFilesDuo else folderIconColor,
                gameIconDuo = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeGameSavesDuo else sidebarIconColor,
                gameShortcutIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeXlsDuo else folderIconColor,
                recycleBinIconDuo = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeRecycleBinDuo else sidebarIconColor,
                downloadsIconDuo = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeDownloadsDuo else sidebarIconColor,
                androidIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeAndroidDuo else folderIconColor,
                zipIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeZipDuo else folderIconColor,
                pdfIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemePdfDuo else folderIconColor,
                xlsIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeXlsDuo else folderIconColor,
                docxIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeDocxDuo else folderIconColor,
                txtIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeTxtDuo else folderIconColor,
                terminalIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeTerminalDuo else folderIconColor,
                imageIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeImageDuo else ThemeImage,
                videoIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeVideoDuo else ThemeVideo,
                audioIconDuo = if (musicTheme == IconTheme.COLOURFULDUO) ThemeAudioDuo else ThemeAudio,
                musicIconDuo = if (musicTheme == IconTheme.COLOURFULDUO) ThemeAudioDuo else ThemeAudio,
                dcimIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else ThemeGallery,
                picturesIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else ThemeGallery,
                tabActiveBackground = Color(0xFF000000),
                textColor = VeryDarkText,
                menuBackground = Color(0xFF1C1C1C).copy(alpha = 0.98f),
                fileViewBackground = Color(0xFF0F0F11),
                background = Color(0xFF000000),
                commandPanelBackground = VeryDarkTopBar,
                statusBarColor = Color.Transparent,
                navigationBarColor = Color.Transparent,
                outline = Color(0xFF444446),
                outlineVariant = Color(0xFF353537)
            )
        }
        isVeryLight -> {
            val sidebarIconColor = if (sidebarTheme == IconTheme.MATERIAL) colorScheme.primary else VeryLightIcons
            val folderIconColor = if (folderTheme == IconTheme.MATERIAL) colorScheme.primary else VeryLightIcons
            val musicIconColor = if (musicTheme == IconTheme.MATERIAL) colorScheme.primary else VeryLightIcons
            ExtendedColors(
                sidebarBackground = VeryLightSidebar,
                topBarBackground = VeryLightTopBar,
                navButtonBackground = VeryLightTopBar,
                searchBoxBackground = Color(0xFFE3E3E3),
                tabBarBackground = Color(0xFFfcfcfe),
                selectionBackground = LightPrimarySelection,
                sidebarIcons = sidebarIconColor,
                homeIcon = if (sidebarTheme == IconTheme.COLOURFUL) ThemeHome else if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeHomeDuo else sidebarIconColor,
                folderIcon = if (folderTheme == IconTheme.COLOURFUL || folderTheme == IconTheme.COLOURFULDUO) defaultFolderColor else folderIconColor,
                galleryIcon = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else ThemeGallery,
                recentIcon = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeRecentDuo else ThemeRecent,
                filesIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeFileDuo else ThemeFile,
                documentsIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeFilesDuo else ThemeFiles,
                gameIcon = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeGameSavesDuo else ThemeGameSaves,
                gameShortcutIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeXlsDuo else ThemeXls,
                recycleBinIcon = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeRecycleBinDuo else ThemeRecycleBin,
                downloadsIcon = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeDownloadsDuo else ThemeDownloads,
                androidIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeAndroidDuo else ThemeAndroid,
                zipIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeZipDuo else ThemeZip,
                pdfIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemePdfDuo else ThemePdf,
                xlsIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeXlsDuo else ThemeXls,
                docxIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeDocxDuo else ThemeDocx,
                txtIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeTxtDuo else ThemeTxt,
                terminalIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeTerminalDuo else ThemeTerminal,
                imageIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeImageDuo else ThemeImage,
                videoIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeVideoDuo else ThemeVideo,
                audioIcon = if (musicTheme == IconTheme.COLOURFULDUO) ThemeAudioDuo else ThemeAudio,
                musicIcon = if (musicTheme == IconTheme.COLOURFULDUO) ThemeAudioDuo else ThemeAudio,
                dcimIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else ThemeGallery,
                picturesIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else ThemeGallery,
                folderIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) defaultFolderColor else folderIconColor,
                homeIconDuo = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeHomeDuo else sidebarIconColor,
                filesIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeFileDuo else folderIconColor,
                galleryIconDuo = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else sidebarIconColor,
                recentIconDuo = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeRecentDuo else sidebarIconColor,
                documentsIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeFilesDuo else folderIconColor,
                gameIconDuo = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeGameSavesDuo else sidebarIconColor,
                gameShortcutIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeXlsDuo else folderIconColor,
                recycleBinIconDuo = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeRecycleBinDuo else sidebarIconColor,
                downloadsIconDuo = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeDownloadsDuo else sidebarIconColor,
                androidIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeAndroidDuo else folderIconColor,
                zipIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeZipDuo else folderIconColor,
                pdfIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemePdfDuo else folderIconColor,
                xlsIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeXlsDuo else folderIconColor,
                docxIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeDocxDuo else folderIconColor,
                txtIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeTxtDuo else folderIconColor,
                terminalIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeTerminalDuo else folderIconColor,
                imageIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeImageDuo else ThemeImage,
                videoIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeVideoDuo else ThemeVideo,
                audioIconDuo = if (musicTheme == IconTheme.COLOURFULDUO) ThemeAudioDuo else ThemeAudio,
                musicIconDuo = if (musicTheme == IconTheme.COLOURFULDUO) ThemeAudioDuo else ThemeAudio,
                dcimIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else ThemeGallery,
                picturesIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else ThemeGallery,
                tabActiveBackground = Color(0xFFF1F1F1),
                textColor = VeryLightText,
                menuBackground = Color.White.copy(alpha = 0.98f),
                fileViewBackground = Color(0xFFFFFFFF),
                background = Color(0xFFF1F1F1),
                commandPanelBackground = VeryLightTopBar,
                statusBarColor = Color.Transparent,
                navigationBarColor = Color.Transparent,
                outline = Color(0xFFA2A2A2),
                outlineVariant = Color(0xFFA2A2A2)
            )
        }
        else -> {
            val primary = colorScheme.primary
            val onSurface = colorScheme.onSurface
            val surfaceLow = colorScheme.surfaceContainerLow
            ExtendedColors(
                sidebarBackground = colorScheme.surfaceContainerHigh,
                topBarBackground = surfaceLow,
                navButtonBackground = surfaceLow,
                searchBoxBackground = colorScheme.surfaceContainerHigh,
                tabBarBackground = colorScheme.surfaceContainerHighest,
                selectionBackground = colorScheme.primaryContainer,
                sidebarIcons = primary,
                homeIcon = if (sidebarTheme == IconTheme.COLOURFUL) ThemeHome else if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeHomeDuo else primary,
                folderIcon = if (folderTheme == IconTheme.COLOURFUL || folderTheme == IconTheme.COLOURFULDUO) defaultFolderColor else primary,
                galleryIcon = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else ThemeGallery,
                recentIcon = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeRecentDuo else ThemeRecent,
                filesIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeFileDuo else ThemeFile,
                documentsIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeFilesDuo else ThemeFiles,
                gameIcon = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeGameSavesDuo else ThemeGameSaves,
                gameShortcutIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeXlsDuo else ThemeXls,
                recycleBinIcon = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeRecycleBinDuo else ThemeRecycleBin,
                downloadsIcon = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeDownloadsDuo else ThemeDownloads,
                androidIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeAndroidDuo else ThemeAndroid,
                zipIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeZipDuo else ThemeZip,
                pdfIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemePdfDuo else ThemePdf,
                xlsIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeXlsDuo else ThemeXls,
                docxIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeDocxDuo else ThemeDocx,
                txtIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeTxtDuo else ThemeTxt,
                terminalIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeTerminalDuo else ThemeTerminal,
                imageIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeImageDuo else ThemeImage,
                videoIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeVideoDuo else ThemeVideo,
                audioIcon = if (musicTheme == IconTheme.COLOURFULDUO) ThemeAudioDuo else ThemeAudio,
                musicIcon = if (musicTheme == IconTheme.COLOURFULDUO) ThemeAudioDuo else ThemeAudio,
                dcimIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else ThemeGallery,
                picturesIcon = if (folderTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else ThemeGallery,
                folderIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) defaultFolderColor else primary,
                homeIconDuo = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeHomeDuo else primary,
                filesIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeFileDuo else primary,
                galleryIconDuo = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else primary,
                recentIconDuo = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeRecentDuo else primary,
                documentsIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeFilesDuo else primary,
                gameIconDuo = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeGameSavesDuo else primary,
                gameShortcutIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeXlsDuo else primary,
                recycleBinIconDuo = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeRecycleBinDuo else primary,
                downloadsIconDuo = if (sidebarTheme == IconTheme.COLOURFULDUO) ThemeDownloadsDuo else primary,
                androidIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeAndroidDuo else primary,
                zipIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeZipDuo else primary,
                pdfIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemePdfDuo else primary,
                xlsIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeXlsDuo else primary,
                docxIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeDocxDuo else primary,
                txtIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeTxtDuo else primary,
                terminalIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeTerminalDuo else primary,
                imageIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeImageDuo else ThemeImage,
                videoIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeVideoDuo else ThemeVideo,
                audioIconDuo = if (musicTheme == IconTheme.COLOURFULDUO) ThemeAudioDuo else ThemeAudio,
                musicIconDuo = if (musicTheme == IconTheme.COLOURFULDUO) ThemeAudioDuo else ThemeAudio,
                dcimIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else ThemeGallery,
                picturesIconDuo = if (folderTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else ThemeGallery,
                tabActiveBackground = surfaceLow,
                textColor = onSurface,
                menuBackground = colorScheme.surfaceContainerLow.copy(alpha = 0.98f),
                fileViewBackground = colorScheme.surfaceContainerLowest,
                background = surfaceLow,
                commandPanelBackground = surfaceLow,
                statusBarColor = Color.Transparent,
                navigationBarColor = Color.Transparent,
                outline = colorScheme.outline,
                outlineVariant = colorScheme.outlineVariant
            )
        }
    }

    val finalExtendedColors = currentPack?.let { pack ->
        if (darkTheme) baseExtendedColors.withCustomColors(pack.darkColors)
        else baseExtendedColors.withCustomColors(pack.lightColors)
    } ?: baseExtendedColors

    val context = LocalContext.current
    val themeTop = SettingsManager.themeTop.value
    DisposableEffect(darkTheme, themeTop, finalExtendedColors.statusBarColor, finalExtendedColors.navigationBarColor) {
        if (context is ComponentActivity) {
            val statusColor = finalExtendedColors.statusBarColor.toArgb()
            val navColor = finalExtendedColors.navigationBarColor.toArgb()
            
            context.enableEdgeToEdge(
                statusBarStyle = if (darkTheme) {
                    SystemBarStyle.dark(statusColor)
                } else {
                    SystemBarStyle.light(statusColor, statusColor)
                },
                navigationBarStyle = if (darkTheme) {
                    SystemBarStyle.dark(navColor)
                } else {
                    SystemBarStyle.light(navColor, navColor)
                }
            )
            
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(context.window, false)

            if (Build.VERSION.SDK_INT >= 35) {
                // For Android 15+ Desktop Windowing
                context.window.insetsController?.setSystemBarsAppearance(
                    android.view.WindowInsetsController.APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND,
                    android.view.WindowInsetsController.APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND
                )
            }
        }
        onDispose {}
    }

    CompositionLocalProvider(LocalExtendedColors provides finalExtendedColors) {
        MaterialTheme(
            colorScheme = finalColorScheme,
            typography = Typography,
            content = content
        )
    }
}

private fun Color.toArgb(): Int {
    return (this.alpha * 255.0f + 0.5f).toInt() shl 24 or
            ((this.red * 255.0f + 0.5f).toInt() shl 16) or
            ((this.green * 255.0f + 0.5f).toInt() shl 8) or
            (this.blue * 255.0f + 0.5f).toInt()
}
