package com.troikoss.continuum_explorer.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.troikoss.continuum_explorer.ui.theme.LocalExtendedColors
import com.troikoss.continuum_explorer.utils.PendingCut

@Composable
fun ClipboardIndicator(
    modifier: Modifier = Modifier,
    backgroundColor: Color = LocalExtendedColors.current.sidebarBackground,
    contentColor: Color = LocalExtendedColors.current.textColor
) {
    val clipboardFiles = PendingCut.files
    val isCut = PendingCut.isActive
    val count = clipboardFiles.size

    if (count == 0) return

    var isExpanded by remember { mutableStateOf(false) }
    
    val width by animateDpAsState(
        targetValue = if (isExpanded) 280.dp else 72.dp,
        label = "WidthAnimation"
    )

    val height by animateDpAsState(
        targetValue = if (isExpanded) 400.dp else 48.dp,
        label = "HeightAnimation"
    )

    val cornerRadius by animateDpAsState(
        targetValue = if (isExpanded) 16.dp else 24.dp,
        label = "CornerAnimation"
    )

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(topStart = cornerRadius, bottomStart = cornerRadius))
            .clip(RoundedCornerShape(topStart = cornerRadius, bottomStart = cornerRadius))
            .background(backgroundColor.copy(alpha = 0.98f))
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount < -10 && !isExpanded) isExpanded = true
                    if (dragAmount > 10 && isExpanded) isExpanded = false
                }
            }
            .clickable { isExpanded = !isExpanded }
    ) {
        if (!isExpanded) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isCut) Icons.Default.ContentCut else Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = count.toString(),
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    fontSize = 14.sp
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isCut) "To be moved ($count)" else "To be copied ($count)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { PendingCut.clear() }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(clipboardFiles) { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = contentColor.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
