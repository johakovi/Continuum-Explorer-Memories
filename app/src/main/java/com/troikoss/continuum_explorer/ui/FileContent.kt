package com.troikoss.continuum_explorer.ui

import androidx.compose.runtime.Composable
import com.troikoss.continuum_explorer.managers.SettingsManager
import com.troikoss.continuum_explorer.managers.ThemeShape
import com.troikoss.continuum_explorer.utils.FileExplorerState

@Composable
fun FileContent(appState: FileExplorerState, isInWindowMode: Boolean = false, onAddStorage: (() -> Unit)? = null) {
    if (SettingsManager.themeContent.value == ThemeShape.SQUARE) {
        FileContentSQ(appState = appState, isInWindowMode = isInWindowMode, onAddStorage = onAddStorage)
    } else {
        FileContentRO(appState = appState, isInWindowMode = isInWindowMode, onAddStorage = onAddStorage)
    }
}
