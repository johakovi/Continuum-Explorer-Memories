package com.troikoss.continuum_explorer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.troikoss.continuum_explorer.utils.FileExplorerState
import com.troikoss.continuum_explorer.utils.IconHelper
import com.troikoss.continuum_explorer.managers.SettingsManager
import com.troikoss.continuum_explorer.ui.theme.LocalExtendedColors
import com.troikoss.continuum_explorer.managers.ThemeTopMode
import com.troikoss.continuum_explorer.R
import kotlinx.coroutines.launch

private val TAB_SLOT_MIN = 80.dp   // minimum total slot width per tab (inc. 4dp side padding)
private val TAB_SLOT_MAX = 204.dp  // maximum total slot width per tab (inc. 4dp side padding)
private val ADD_BUTTON_WIDTH = 40.dp

@Composable
fun TabBar(
    tabStates: List<FileExplorerState>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onAddTab: () -> Unit,
    onCloseTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val themeTop = SettingsManager.themeTop.value

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(if (themeTop == ThemeTopMode.FLOAT) 48.dp else 40.dp)
            .background(if (themeTop == ThemeTopMode.FLOAT) Color.Transparent else LocalExtendedColors.current.topBarBackground)
            .let {
                if (themeTop == ThemeTopMode.ATTACHED) {
                    it.drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        drawLine(
                            color = borderColor,
                            start = Offset(0f, size.height - strokeWidth / 2),
                            end = Offset(size.width, size.height - strokeWidth / 2),
                            strokeWidth = strokeWidth
                        )
                    }
                } else it
            },
        contentAlignment = if (themeTop == ThemeTopMode.FLOAT) Alignment.CenterStart else Alignment.BottomStart
    ) {
        // Shrink tabs proportionally as more are added, scroll when they hit minimum.
        val horizontalPadding = if (themeTop == ThemeTopMode.FLOAT) 16.dp else 0.dp
        val slotWidth: Dp = if (tabStates.isEmpty()) TAB_SLOT_MAX else {
            val available = maxWidth - ADD_BUTTON_WIDTH - (horizontalPadding * 2)
            (available / tabStates.size).coerceIn(TAB_SLOT_MIN, TAB_SLOT_MAX)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .horizontalScroll(scrollState)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Scroll) {
                                val delta = event.changes.first().scrollDelta
                                coroutineScope.launch {
                                    scrollState.scrollBy(delta.y * 60f)
                                }
                            }
                        }
                    }
                },
            verticalAlignment = if (themeTop == ThemeTopMode.FLOAT) Alignment.CenterVertically else Alignment.Bottom
        ) {
            tabStates.forEachIndexed { index, state ->
                val universalFile = state.currentUniversalPath
                val tabIcon = if (universalFile != null) {
                    IconHelper.getIconForItem(universalFile)
                } else {
                    Icons.Default.Folder
                }

                TabItem(
                    text = state.currentName,
                    icon = tabIcon,
                    slotWidth = slotWidth,
                    selected = (selectedTabIndex == index),
                    onClick = { onTabSelected(index) },
                    onClose = { onCloseTab(index) }
                )
            }

            IconButton(
                onClick = onAddTab,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(36.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.new_tab),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private val TabShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
private val TabShapeFloat = RoundedCornerShape(22.dp)

@Composable
private fun TabItem(
    text: String,
    icon: ImageVector,
    slotWidth: Dp,
    selected: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val isColorful = SettingsManager.isColorfulBarsEnabled.value
    val themeTop = SettingsManager.themeTop.value

    val backgroundColor = when {
        selected && isColorful -> MaterialTheme.colorScheme.primaryContainer
        selected -> LocalExtendedColors.current.tabActiveBackground
        else -> if (themeTop == ThemeTopMode.FLOAT) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color.Transparent
    }
    val textColor = when {
        selected && isColorful -> MaterialTheme.colorScheme.onPrimaryContainer
        selected -> if (themeTop == ThemeTopMode.FLOAT) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val shape = if (themeTop == ThemeTopMode.FLOAT) TabShapeFloat else TabShape

    // slotWidth = visual tab width + 4dp side padding (2dp each side)
    Box(
        modifier = Modifier
            .width(slotWidth)
            .padding(start = 2.dp, end = 2.dp, top = if (themeTop == ThemeTopMode.FLOAT) 0.dp else 8.dp)
            .clip(shape)
            .height(if (themeTop == ThemeTopMode.FLOAT) 36.dp else 32.dp)
            .background(backgroundColor)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val event = awaitPointerEvent()
                    val isMouse = event.changes.any { it.type == PointerType.Mouse }
                    val isTertiary = event.buttons.isTertiaryPressed
                    if (isMouse && event.type == PointerEventType.Press && isTertiary) {
                        onClose()
                    }
                }
            }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = textColor
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                color = textColor,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.close),
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onClose() },
                tint = textColor
            )
        }
    }
}