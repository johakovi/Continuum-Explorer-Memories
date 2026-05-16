package com.troikoss.continuum_explorer.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import com.troikoss.continuum_explorer.managers.SettingsManager
import com.troikoss.continuum_explorer.managers.ThemeShape
import com.troikoss.continuum_explorer.model.LibraryItem
import java.io.File

@Composable
fun FileExplorer(
    initialPath: String? = null,
    initialUri: String? = null,
    initialArchive: File? = null,
    initialArchiveUri: Uri? = null,
    initialArchiveName: String? = null,
    initialLibraryItem: LibraryItem = LibraryItem.None,
    initialNetworkConnectionId: String? = null
) {
    if (SettingsManager.themeBar.value == ThemeShape.SQUARE) {
        FileExplorerSQ(
            initialPath = initialPath,
            initialUri = initialUri,
            initialArchive = initialArchive,
            initialArchiveUri = initialArchiveUri,
            initialArchiveName = initialArchiveName,
            initialLibraryItem = initialLibraryItem,
            initialNetworkConnectionId = initialNetworkConnectionId
        )
    } else {
        FileExplorerRO(
            initialPath = initialPath,
            initialUri = initialUri,
            initialArchive = initialArchive,
            initialArchiveUri = initialArchiveUri,
            initialArchiveName = initialArchiveName,
            initialLibraryItem = initialLibraryItem,
            initialNetworkConnectionId = initialNetworkConnectionId
        )
    }
}
