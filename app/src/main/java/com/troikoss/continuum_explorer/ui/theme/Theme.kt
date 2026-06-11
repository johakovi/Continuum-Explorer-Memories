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
import com.troikoss.continuum_explorer.managers.ThemeTopMode
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
        0xFF00BCD4  // Cyan
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
    val iconTheme = SettingsManager.iconTheme.value
    val isSystemDark = isSystemInDarkTheme()

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
            ExtendedColors(
                sidebarBackground = VeryDarkSidebar,
                topBarBackground = VeryDarkTopBar,
                navButtonBackground = Color(0xFF000000),
                searchBoxBackground = Color(0xFF1A1A1A),
                tabBarBackground = Color(0xFF2D2D2F),
                selectionBackground = DarkPrimarySelection,
                sidebarIcons = VeryDarkIcons,
                folderIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeFolders else if (iconTheme == IconTheme.COLOURFULDUO) ThemeFoldersDuo else VeryDarkIcons,
                galleryIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeGallery else if (iconTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else VeryDarkIcons,
                recentIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeRecent else if (iconTheme == IconTheme.COLOURFULDUO) ThemeRecentDuo else VeryDarkIcons,
                filesIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeFile else if (iconTheme == IconTheme.COLOURFULDUO) ThemeFileDuo else VeryDarkIcons,
                documentsIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeFiles else if (iconTheme == IconTheme.COLOURFULDUO) ThemeFilesDuo else VeryDarkIcons,
                gameIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeGameSaves else if (iconTheme == IconTheme.COLOURFULDUO) ThemeGameSavesDuo else VeryDarkIcons,
                gameShortcutIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeXls else if (iconTheme == IconTheme.COLOURFULDUO) ThemeXlsDuo else VeryDarkIcons,
                recycleBinIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeRecycleBin else if (iconTheme == IconTheme.COLOURFULDUO) ThemeRecycleBinDuo else VeryDarkIcons,
                downloadsIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeDownloads else if (iconTheme == IconTheme.COLOURFULDUO) ThemeDownloadsDuo else VeryDarkIcons,
                androidIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeAndroid else if (iconTheme == IconTheme.COLOURFULDUO) ThemeAndroidDuo else VeryDarkIcons,
                zipIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeZip else if (iconTheme == IconTheme.COLOURFULDUO) ThemeZipDuo else VeryDarkIcons,
                pdfIcon = if (iconTheme == IconTheme.COLOURFUL) ThemePdf else if (iconTheme == IconTheme.COLOURFULDUO) ThemePdfDuo else VeryDarkIcons,
                xlsIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeXls else if (iconTheme == IconTheme.COLOURFULDUO) ThemeXlsDuo else VeryDarkIcons,
                docxIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeDocx else if (iconTheme == IconTheme.COLOURFULDUO) ThemeDocxDuo else VeryDarkIcons,
                txtIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeTxt else if (iconTheme == IconTheme.COLOURFULDUO) ThemeTxtDuo else VeryDarkIcons,
                terminalIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeTerminal else if (iconTheme == IconTheme.COLOURFULDUO) ThemeTerminalDuo else VeryDarkIcons,
                imageIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeImage else if (iconTheme == IconTheme.COLOURFULDUO) ThemeImageDuo else VeryDarkIcons,
                videoIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeVideo else if (iconTheme == IconTheme.COLOURFULDUO) ThemeVideoDuo else VeryDarkIcons,
                audioIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeAudio else if (iconTheme == IconTheme.COLOURFULDUO) ThemeAudioDuo else VeryDarkIcons,
                musicIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeAudio else if (iconTheme == IconTheme.COLOURFULDUO) ThemeAudioDuo else VeryDarkIcons,
                dcimIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeGallery else if (iconTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else VeryDarkIcons,
                picturesIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeGallery else if (iconTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else VeryDarkIcons,
                folderIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeFoldersDuo else VeryDarkIcons,
                filesIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeFileDuo else VeryDarkIcons,
                galleryIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else VeryDarkIcons,
                recentIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeRecentDuo else VeryDarkIcons,
                documentsIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeFilesDuo else VeryDarkIcons,
                gameIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeGameSavesDuo else VeryDarkIcons,
                gameShortcutIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeXlsDuo else VeryDarkIcons,
                recycleBinIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeRecycleBinDuo else VeryDarkIcons,
                downloadsIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeDownloadsDuo else VeryDarkIcons,
                androidIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeAndroidDuo else VeryDarkIcons,
                zipIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeZipDuo else VeryDarkIcons,
                pdfIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemePdfDuo else VeryDarkIcons,
                xlsIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeXlsDuo else VeryDarkIcons,
                docxIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeDocxDuo else VeryDarkIcons,
                txtIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeTxtDuo else VeryDarkIcons,
                terminalIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeTerminalDuo else VeryDarkIcons,
                imageIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeImageDuo else VeryDarkIcons,
                videoIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeVideoDuo else VeryDarkIcons,
                audioIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeAudioDuo else VeryDarkIcons,
                musicIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeAudioDuo else VeryDarkIcons,
                dcimIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else VeryDarkIcons,
                picturesIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else VeryDarkIcons,
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
            ExtendedColors(
                sidebarBackground = VeryLightSidebar,
                topBarBackground = VeryLightTopBar,
                navButtonBackground = VeryLightTopBar,
                searchBoxBackground = Color(0xFFE3E3E3),
                tabBarBackground = Color(0xFFfcfcfe),
                selectionBackground = LightPrimarySelection,
                sidebarIcons = VeryLightIcons,
                folderIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeFolders else if (iconTheme == IconTheme.COLOURFULDUO) ThemeFoldersDuo else VeryLightIcons,
                galleryIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeGallery else if (iconTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else VeryLightIcons,
                recentIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeRecent else if (iconTheme == IconTheme.COLOURFULDUO) ThemeRecentDuo else VeryLightIcons,
                filesIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeFile else if (iconTheme == IconTheme.COLOURFULDUO) ThemeFileDuo else VeryLightIcons,
                documentsIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeFiles else if (iconTheme == IconTheme.COLOURFULDUO) ThemeFilesDuo else VeryLightIcons,
                gameIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeGameSaves else if (iconTheme == IconTheme.COLOURFULDUO) ThemeGameSavesDuo else VeryLightIcons,
                gameShortcutIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeXls else if (iconTheme == IconTheme.COLOURFULDUO) ThemeXlsDuo else VeryLightIcons,
                recycleBinIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeRecycleBin else if (iconTheme == IconTheme.COLOURFULDUO) ThemeRecycleBinDuo else VeryLightIcons,
                downloadsIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeDownloads else if (iconTheme == IconTheme.COLOURFULDUO) ThemeDownloadsDuo else VeryLightIcons,
                androidIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeAndroid else if (iconTheme == IconTheme.COLOURFULDUO) ThemeAndroidDuo else VeryLightIcons,
                zipIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeZip else if (iconTheme == IconTheme.COLOURFULDUO) ThemeZipDuo else VeryLightIcons,
                pdfIcon = if (iconTheme == IconTheme.COLOURFUL) ThemePdf else if (iconTheme == IconTheme.COLOURFULDUO) ThemePdfDuo else VeryLightIcons,
                xlsIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeXls else if (iconTheme == IconTheme.COLOURFULDUO) ThemeXlsDuo else VeryLightIcons,
                docxIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeDocx else if (iconTheme == IconTheme.COLOURFULDUO) ThemeDocxDuo else VeryLightIcons,
                txtIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeTxt else if (iconTheme == IconTheme.COLOURFULDUO) ThemeTxtDuo else VeryLightIcons,
                terminalIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeTerminal else if (iconTheme == IconTheme.COLOURFULDUO) ThemeTerminalDuo else VeryLightIcons,
                imageIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeImage else if (iconTheme == IconTheme.COLOURFULDUO) ThemeImageDuo else VeryLightIcons,
                videoIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeVideo else if (iconTheme == IconTheme.COLOURFULDUO) ThemeVideoDuo else VeryLightIcons,
                audioIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeAudio else if (iconTheme == IconTheme.COLOURFULDUO) ThemeAudioDuo else VeryLightIcons,
                musicIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeAudio else if (iconTheme == IconTheme.COLOURFULDUO) ThemeAudioDuo else VeryLightIcons,
                dcimIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeGallery else if (iconTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else VeryLightIcons,
                picturesIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeGallery else if (iconTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else VeryLightIcons,
                folderIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeFoldersDuo else VeryLightIcons,
                filesIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeFileDuo else VeryLightIcons,
                galleryIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else VeryLightIcons,
                recentIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeRecentDuo else VeryLightIcons,
                documentsIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeFilesDuo else VeryLightIcons,
                gameIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeGameSavesDuo else VeryLightIcons,
                gameShortcutIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeXlsDuo else VeryLightIcons,
                recycleBinIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeRecycleBinDuo else VeryLightIcons,
                downloadsIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeDownloadsDuo else VeryLightIcons,
                androidIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeAndroidDuo else VeryLightIcons,
                zipIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeZipDuo else VeryLightIcons,
                pdfIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemePdfDuo else VeryLightIcons,
                xlsIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeXlsDuo else VeryLightIcons,
                docxIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeDocxDuo else VeryLightIcons,
                txtIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeTxtDuo else VeryLightIcons,
                terminalIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeTerminalDuo else VeryLightIcons,
                imageIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeImageDuo else VeryLightIcons,
                videoIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeVideoDuo else VeryLightIcons,
                audioIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeAudioDuo else VeryLightIcons,
                musicIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeAudioDuo else VeryLightIcons,
                dcimIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else VeryLightIcons,
                picturesIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else VeryLightIcons,
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
                folderIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeFolders else if (iconTheme == IconTheme.COLOURFULDUO) ThemeFoldersDuo else primary,
                galleryIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeGallery else if (iconTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else primary,
                recentIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeRecent else if (iconTheme == IconTheme.COLOURFULDUO) ThemeRecentDuo else primary,
                filesIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeFile else if (iconTheme == IconTheme.COLOURFULDUO) ThemeFileDuo else primary,
                documentsIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeFiles else if (iconTheme == IconTheme.COLOURFULDUO) ThemeFilesDuo else primary,
                gameIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeGameSaves else if (iconTheme == IconTheme.COLOURFULDUO) ThemeGameSavesDuo else primary,
                gameShortcutIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeXls else if (iconTheme == IconTheme.COLOURFULDUO) ThemeXlsDuo else primary,
                recycleBinIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeRecycleBin else if (iconTheme == IconTheme.COLOURFULDUO) ThemeRecycleBinDuo else primary,
                downloadsIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeDownloads else if (iconTheme == IconTheme.COLOURFULDUO) ThemeDownloadsDuo else primary,
                androidIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeAndroid else if (iconTheme == IconTheme.COLOURFULDUO) ThemeAndroidDuo else primary,
                zipIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeZip else if (iconTheme == IconTheme.COLOURFULDUO) ThemeZipDuo else Color(0xFF6E6E6E),
                pdfIcon = if (iconTheme == IconTheme.COLOURFUL) ThemePdf else if (iconTheme == IconTheme.COLOURFULDUO) ThemePdfDuo else ThemePdf,
                xlsIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeXls else if (iconTheme == IconTheme.COLOURFULDUO) ThemeXlsDuo else ThemeXls,
                docxIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeDocx else if (iconTheme == IconTheme.COLOURFULDUO) ThemeDocxDuo else ThemeDocx,
                txtIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeTxt else if (iconTheme == IconTheme.COLOURFULDUO) ThemeTxtDuo else ThemeTxt,
                terminalIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeTerminal else if (iconTheme == IconTheme.COLOURFULDUO) ThemeTerminalDuo else ThemeTerminal,
                imageIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeImage else if (iconTheme == IconTheme.COLOURFULDUO) ThemeImageDuo else primary,
                videoIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeVideo else if (iconTheme == IconTheme.COLOURFULDUO) ThemeVideoDuo else primary,
                audioIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeAudio else if (iconTheme == IconTheme.COLOURFULDUO) ThemeAudioDuo else primary,
                musicIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeAudio else if (iconTheme == IconTheme.COLOURFULDUO) ThemeAudioDuo else primary,
                dcimIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeGallery else if (iconTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else primary,
                picturesIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeGallery else if (iconTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else primary,
                folderIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeFoldersDuo else primary,
                filesIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeFileDuo else primary,
                galleryIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else primary,
                recentIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeRecentDuo else primary,
                documentsIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeFilesDuo else primary,
                gameIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeGameSavesDuo else primary,
                gameShortcutIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeXlsDuo else primary,
                recycleBinIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeRecycleBinDuo else primary,
                downloadsIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeDownloadsDuo else primary,
                androidIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeAndroidDuo else primary,
                zipIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeZipDuo else primary,
                pdfIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemePdfDuo else primary,
                xlsIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeXlsDuo else primary,
                docxIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeDocxDuo else primary,
                txtIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeTxtDuo else primary,
                terminalIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeTerminalDuo else primary,
                imageIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeImageDuo else primary,
                videoIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeVideoDuo else primary,
                audioIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeAudioDuo else primary,
                musicIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeAudioDuo else primary,
                dcimIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else primary,
                picturesIconDuo = if (iconTheme == IconTheme.COLOURFULDUO) ThemeGalleryDuo else primary,
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
