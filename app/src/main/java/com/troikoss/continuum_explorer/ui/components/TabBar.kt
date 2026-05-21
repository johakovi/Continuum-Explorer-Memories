package com.troikoss.continuum_explorer.ui.components

import android.content.res.Configuration
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
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.troikoss.continuum_explorer.utils.FileExplorerState
import com.troikoss.continuum_explorer.utils.IconHelper
import com.troikoss.continuum_explorer.managers.SettingsManager
import com.troikoss.continuum_explorer.managers.IconTheme
import com.troikoss.continuum_explorer.ui.theme.LocalExtendedColors
import com.troikoss.continuum_explorer.managers.ThemeTopMode
import com.troikoss.continuum_explorer.managers.WindowManager
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
    val density = LocalDensity.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val isInWindowMode = remember(configuration) {
        val isInMultiWindow = try {
            (context as? android.app.Activity)?.isInMultiWindowMode == true
        } catch (_: Exception) { false }
        val isLargeScreen = configuration.smallestScreenWidthDp >= 600
        isInMultiWindow && isLargeScreen
    }

    val captionBarTop = WindowInsets.captionBar.getTop(density)
    val statusBarTop = WindowInsets.statusBars.getTop(density)
    
    // Immediate detection: If in Window mode, we assume 40dp handle exists pre-emptively
    val hasCaption = captionBarTop > 0 || isInWindowMode
    
    val topInsetHeight = with(density) { 
        if (captionBarTop > 0) captionBarTop.toDp() 
        else if (isInWindowMode) 40.dp 
        else statusBarTop.toDp() 
    }
    
    // Always move tabs into title bar area if a handle is detected
    val useInCaptionTabs = hasCaption
    
    val tabContentHeight = if (themeTop == ThemeTopMode.FLOAT) 48.dp else 40.dp
    val totalBarHeight = if (useInCaptionTabs) topInsetHeight else (topInsetHeight + tabContentHeight)

    val horizontalPaddingLeft = with(density) { WindowInsets.captionBar.getLeft(density, LayoutDirection.Ltr).toDp() }
    val horizontalPaddingRight = with(density) { WindowInsets.captionBar.getRight(density, LayoutDirection.Ltr).toDp() }

    val restrictedLeft = WindowManager.restrictedLeftPadding.value
    val restrictedRight = WindowManager.restrictedRightPadding.value

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(totalBarHeight)
            .background(LocalExtendedColors.current.tabBarBackground)
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
        contentAlignment = Alignment.BottomStart
    ) {
        // Unified Safety Fallback for all windowed modes (DeX & Pop-ups)
        val safetyPaddingLeft = if (hasCaption && restrictedLeft == 0.dp && horizontalPaddingLeft == 0.dp) {
            minOf(80.dp, maxWidth * 0.2f)
        } else 0.dp

        val safetyPaddingRight = if (hasCaption && restrictedRight == 0.dp && horizontalPaddingRight == 0.dp) {
            minOf(160.dp, maxWidth * 0.4f)
        } else 0.dp
        
        val finalPaddingLeft = maxOf(horizontalPaddingLeft, restrictedLeft, safetyPaddingLeft)
        val finalPaddingRight = maxOf(horizontalPaddingRight, restrictedRight, safetyPaddingRight)

        // Prevent layout explosion in very narrow windows
        val maxTotalPadding = maxWidth * 0.7f
        val adjustedPaddingRight = if (finalPaddingLeft + finalPaddingRight > maxTotalPadding) {
            (maxTotalPadding - finalPaddingLeft).coerceAtLeast(0.dp)
        } else finalPaddingRight

        val available = maxWidth - ADD_BUTTON_WIDTH - finalPaddingLeft - adjustedPaddingRight - 16.dp
        val slotWidth: Dp = if (tabStates.isEmpty()) TAB_SLOT_MAX else {
            (available / tabStates.size).coerceIn(TAB_SLOT_MIN, TAB_SLOT_MAX)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = finalPaddingLeft, end = adjustedPaddingRight)
                .height(tabContentHeight)
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
            verticalAlignment = Alignment.Bottom
        ) {
            tabStates.forEachIndexed { index, state ->
                val universalFile = state.currentUniversalPath
                val iconTheme = SettingsManager.iconTheme.value

                val painter = if (universalFile != null) {
                    if (iconTheme == IconTheme.COLOURFUL) {
                        androidx.compose.ui.res.painterResource(id = IconHelper.getDrawableForItem(universalFile))
                    } else {
                        rememberVectorPainter(IconHelper.getIconForItem(universalFile))
                    }
                } else {
                    rememberVectorPainter(Icons.Default.Folder)
                }

                TabItem(
                    text = state.currentName,
                    painter = painter,
                    slotWidth = slotWidth,
                    selected = (selectedTabIndex == index),
                    onClick = { onTabSelected(index) },
                    onClose = { onCloseTab(index) },
                    modifier = if (useInCaptionTabs) Modifier.systemGestureExclusion() else Modifier
                )
            }

            IconButton(
                onClick = onAddTab,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(36.dp)
                    .then(if (useInCaptionTabs) Modifier.systemGestureExclusion() else Modifier)
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

private val TabShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
private val TabShapeFloat = RoundedCornerShape(22.dp)

@Composable
private fun TabItem(
    text: String,
    painter: Painter,
    slotWidth: Dp,
    selected: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
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
        modifier = modifier
            .width(slotWidth)
            .padding(start = 4.dp, end = 4.dp, top = if (themeTop == ThemeTopMode.FLOAT) 0.dp else 8.dp)
            .let { 
                if (selected && themeTop == ThemeTopMode.ATTACHED) {
                    it.drawBehind {
                        val r = 8.dp.toPx()
                        val path = Path().apply {
                            // Left inverted corner
                            moveTo(-r, size.height)
                            quadraticTo(0f, size.height, 0f, size.height - r)
                            // Top part
                            lineTo(0f, r)
                            arcTo(Rect(0f, 0f, r * 2, r * 2), 180f, 90f, false)
                            lineTo(size.width - r, 0f)
                            arcTo(Rect(size.width - r * 2, 0f, size.width, r * 2), 270f, 90f, false)
                            // Right inverted corner
                            lineTo(size.width, size.height - r)
                            quadraticTo(size.width, size.height, size.width + r, size.height)
                            lineTo(size.width + r, size.height)
                            close()
                        }
                        drawPath(path, backgroundColor)
                    }
                } else {
                    it.clip(shape).background(backgroundColor)
                }
            }
            .height(if (themeTop == ThemeTopMode.FLOAT) 36.dp else 32.dp)
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
                painter = painter,
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