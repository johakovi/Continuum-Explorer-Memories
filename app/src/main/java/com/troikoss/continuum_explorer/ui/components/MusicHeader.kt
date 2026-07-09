package com.troikoss.continuum_explorer.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.troikoss.continuum_explorer.model.FileColumnType
import com.troikoss.continuum_explorer.utils.FileExplorerState


/**
 * Header row for the "Music" view mode displaying track metadata columns.
 */
@Composable
fun MusicHeader(appState: FileExplorerState, scrollState: ScrollState) {
    val iconSize = appState.folderConfigs.detailsItemSize.dp
    val nameColumnWidth = appState.folderConfigs.columnWidths.getOrElse(FileColumnType.NAME) { 200.dp }

    Box {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(iconSize + 12.dp))

            Text(
                text = "Track",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(nameColumnWidth)
            )

            Text(
                text = "Artist",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .width(150.dp)
                    .padding(start = 8.dp)
            )

            Text(
                text = "Duration",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .width(100.dp)
                    .padding(start = 8.dp)
            )
        }
    }
}