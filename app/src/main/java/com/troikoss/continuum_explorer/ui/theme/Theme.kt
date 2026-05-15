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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.troikoss.continuum_explorer.managers.SettingsManager
import com.troikoss.continuum_explorer.managers.ThemeMode
import com.troikoss.continuum_explorer.managers.IconTheme
import com.troikoss.continuum_explorer.managers.ThemeTopMode

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
    val recycleBinIcon: Color,
    val downloadsIcon: Color,
    val tabActiveBackground: Color,
    val textColor: Color
)

val LocalExtendedColors = staticCompositionLocalOf<ExtendedColors> {
    error("No ExtendedColors provided")
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
    val iconTheme = SettingsManager.iconTheme.value
    val isSystemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.ENHANCED_SYSTEM -> isSystemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.VERY_DARK -> true
        ThemeMode.VERY_LIGHT -> false
    }

    val colorScheme = when {
        themeMode == ThemeMode.VERY_DARK || (themeMode == ThemeMode.ENHANCED_SYSTEM && isSystemDark) -> VeryDarkColorScheme
        themeMode == ThemeMode.VERY_LIGHT || (themeMode == ThemeMode.ENHANCED_SYSTEM && !isSystemDark) -> VeryLightColorScheme
        else -> {
            // For LIGHT, DARK, and SYSTEM themes: use dynamic colors on Android 12+
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                // Fallback to Material 3 default schemes when dynamic colors unavailable
                if (darkTheme) {
                    darkColorScheme()
                } else {
                    lightColorScheme()
                }
            }
        }
    }

    val extendedColors = when {
        themeMode == ThemeMode.VERY_DARK || (themeMode == ThemeMode.ENHANCED_SYSTEM && isSystemDark) -> {
            ExtendedColors(
                sidebarBackground = VeryDarkSidebar,
                topBarBackground = VeryDarkTopBar,
                navButtonBackground = Color(0xFF000000),
                searchBoxBackground = Color(0xFF1A1A1A),
                tabBarBackground = Color(0xFF2d2d2f),
                selectionBackground = DarkPrimarySelection,
                sidebarIcons = VeryDarkIcons,
                folderIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeFolders else VeryDarkIcons,
                galleryIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeGallery else VeryDarkIcons,
                recentIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeRecent else VeryDarkIcons,
                filesIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeFiles else VeryDarkIcons,
                documentsIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeFiles else VeryDarkIcons,
                recycleBinIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeRecycleBin else VeryDarkIcons,
                downloadsIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeDownloads else VeryDarkIcons,
                tabActiveBackground = Color(0xFF000000),
                textColor = VeryDarkText
            )
        }
        themeMode == ThemeMode.VERY_LIGHT || (themeMode == ThemeMode.ENHANCED_SYSTEM && !isSystemDark) -> {
            ExtendedColors(
                sidebarBackground = VeryLightSidebar,
                topBarBackground = VeryLightTopBar,
                navButtonBackground = VeryLightTopBar,
                searchBoxBackground = Color(0xFFE3E3E3),
                tabBarBackground = Color(0xFFfcfcfe),
                selectionBackground = LightPrimarySelection,
                sidebarIcons = VeryLightIcons,
                folderIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeFolders else VeryLightIcons,
                galleryIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeGallery else VeryLightIcons,
                recentIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeRecent else VeryLightIcons,
                filesIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeFiles else VeryLightIcons,
                documentsIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeFiles else VeryLightIcons,
                recycleBinIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeRecycleBin else VeryLightIcons,
                downloadsIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeDownloads else VeryLightIcons,
                tabActiveBackground = Color(0xFFF1F1F1),
                textColor = VeryLightText
            )
        }
        else -> {
            val secondary = colorScheme.secondary
            val onSurface = colorScheme.onSurface
            ExtendedColors(
                sidebarBackground = colorScheme.surfaceContainerLow,
                topBarBackground = colorScheme.surface,
                navButtonBackground = colorScheme.surface,
                searchBoxBackground = colorScheme.surfaceContainerHigh,
                tabBarBackground = colorScheme.surfaceContainerLow,
                sidebarIcons = secondary,
                folderIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeFolders else secondary,
                galleryIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeGallery else secondary,
                recentIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeRecent else secondary,
                filesIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeFiles else secondary,
                documentsIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeFiles else secondary,
                recycleBinIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeRecycleBin else secondary,
                downloadsIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeDownloads else secondary,
                tabActiveBackground = colorScheme.surface,
                textColor = onSurface,
                selectionBackground = colorScheme.primaryContainer
            )
        }
    }

    val context = LocalContext.current
    val themeTop = SettingsManager.themeTop.value
    DisposableEffect(darkTheme, themeTop) {
        if (context is ComponentActivity) {
            val statusBarColor = if (themeTop == ThemeTopMode.FLOAT) {
                extendedColors.tabBarBackground.toArgb()
            } else {
                extendedColors.tabBarBackground.toArgb()
            }
            context.enableEdgeToEdge(
                statusBarStyle = if (darkTheme) {
                    SystemBarStyle.dark(statusBarColor)
                } else {
                    SystemBarStyle.light(statusBarColor, statusBarColor)
                },
                navigationBarStyle = if (darkTheme) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                }
            )
        }
        onDispose {}
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}