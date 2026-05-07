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

data class ExtendedColors(
    val sidebarBackground: Color,
    val topBarBackground: Color,
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

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

private val VeryDarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF000000),
    surface = VeryDarkTopBar,
    surfaceContainer = VeryDarkTopBar,
    surfaceContainerLow = VeryDarkSidebar,
    surfaceContainerHigh = Color(0xFF1C1C1C),
    onSurface = VeryDarkText,
    onSurfaceVariant = VeryDarkIcons,
    outlineVariant = Color(0xFF353537)
)

private val VeryLightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFFFFFF),
    surface = VeryLightTopBar,
    surfaceContainer = VeryLightTopBar,
    surfaceContainerLow = VeryLightSidebar,
    surfaceContainerHigh = Color(0xFFE8E8E8),
    onSurface = VeryLightText,
    onSurfaceVariant = VeryLightIcons,
    outlineVariant = Color(0xFFDDDDDD)
)

@Composable
fun FileExplorerTheme(
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val themeMode = SettingsManager.themeMode.value
    val iconTheme = SettingsManager.iconTheme.value
    val isSystemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.VERY_DARK -> true
        ThemeMode.VERY_LIGHT -> false
    }

    val colorScheme = when (themeMode) {
        ThemeMode.VERY_DARK -> VeryDarkColorScheme
        ThemeMode.VERY_LIGHT -> VeryLightColorScheme
        else -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else if (darkTheme) {
                DarkColorScheme
            } else {
                LightColorScheme
            }
        }
    }

    val extendedColors = when (themeMode) {
        ThemeMode.VERY_DARK -> {
            ExtendedColors(
                sidebarBackground = VeryDarkSidebar,
                topBarBackground = VeryDarkTopBar,
                sidebarIcons = VeryDarkIcons,
                folderIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeFolders else VeryDarkIcons,
                galleryIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeGallery else VeryDarkIcons,
                recentIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeRecent else VeryDarkIcons,
                filesIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeFiles else VeryDarkIcons,
                documentsIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeFiles else VeryDarkIcons,
                recycleBinIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeRecycleBin else VeryDarkIcons,
                downloadsIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeDownloads else VeryDarkIcons,
                tabActiveBackground = Color(0xFF2e2e2e),
                textColor = VeryDarkText
            )
        }
        ThemeMode.VERY_LIGHT -> {
            ExtendedColors(
                sidebarBackground = VeryLightSidebar,
                topBarBackground = VeryLightTopBar,
                sidebarIcons = VeryLightIcons,
                folderIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeFolders else VeryLightIcons,
                galleryIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeGallery else VeryLightIcons,
                recentIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeRecent else VeryLightIcons,
                filesIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeFiles else VeryLightIcons,
                documentsIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeFiles else VeryLightIcons,
                recycleBinIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeRecycleBin else VeryLightIcons,
                downloadsIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeDownloads else VeryLightIcons,
                tabActiveBackground = Color(0xFFe8e8e8),
                textColor = VeryLightText
            )
        }
        else -> {
            val secondary = colorScheme.secondary
            val onSurface = colorScheme.onSurface
            ExtendedColors(
                sidebarBackground = colorScheme.surfaceContainerLow,
                topBarBackground = colorScheme.surface,
                sidebarIcons = secondary,
                folderIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeFolders else secondary,
                galleryIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeGallery else secondary,
                recentIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeRecent else secondary,
                filesIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeFiles else secondary,
                documentsIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeFiles else secondary,
                recycleBinIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeRecycleBin else secondary,
                downloadsIcon = if (iconTheme == IconTheme.COLOURFUL) ThemeDownloads else secondary,
                tabActiveBackground = colorScheme.surface,
                textColor = onSurface
            )
        }
    }

    val context = LocalContext.current
    DisposableEffect(darkTheme) {
        if (context is ComponentActivity) {
            context.enableEdgeToEdge(
                statusBarStyle = if (darkTheme) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
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
