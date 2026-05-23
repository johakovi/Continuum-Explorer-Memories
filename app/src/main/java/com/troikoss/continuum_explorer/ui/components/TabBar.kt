package com.troikoss.continuum_explorer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.painter.Painter
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
import com.troikoss.continuum_explorer.R
import com.troikoss.continuum_explorer.managers.IconTheme
import com.troikoss.continuum_explorer.managers.SettingsManager
import com.troikoss.continuum_explorer.managers.ThemeTopMode
import com.troikoss.continuum_explorer.managers.WindowManager
import com.troikoss.continuum_explorer.ui.theme.LocalExtendedColors
import com.troikoss.continuum_explorer.utils.FileExplorerState
import com.troikoss.continuum_explorer.utils.IconHelper
import kotlinx.coroutines.delay
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
    onCloseTab: (FileExplorerState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val themeTop = SettingsManager.themeTop.value
    val density = LocalDensity.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val context = androidx.compose.ui.platform.LocalContext.current

    val isInWindowMode = remember(configuration) {
        val isMulti = try {
            (context as? android.app.Activity)?.isInMultiWindowMode == true
        } catch (_: Exception) { false }
        val isLargeScreen = configuration.smallestScreenWidthDp >= 600
        val isDeX = configuration.toString().contains("dexMode", ignoreCase = true)
        // Window mode logic: DeX, or Multi-window on Large Screens
        isDeX || (isMulti && isLargeScreen)
    }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val captionPadding = WindowInsets.captionBar.asPaddingValues().calculateTopPadding()

    // hasCaption: strictly for when the system provides a caption handle area
    val hasCaption = captionPadding > 0.dp
    
    // Always move tabs into title bar area if a handle is detected or in window mode
    val useInCaptionTabs = hasCaption || isInWindowMode
    
    val topInsetHeight = if (useInCaptionTabs) {
        // In window mode, we assume at least 40dp for the handle if not specified
        maxOf(captionPadding, if (isInWindowMode) 40.dp else 0.dp)
    } else {
        // On phone, we use the status bar height
        statusBarPadding
    }

    val tabContentHeight = if (themeTop == ThemeTopMode.FLOAT) 48.dp else 40.dp
    val totalBarHeight = if (useInCaptionTabs) {
        maxOf(topInsetHeight, tabContentHeight)
    } else {
        topInsetHeight + tabContentHeight
    }

    val horizontalPaddingLeft = with(density) { WindowInsets.captionBar.getLeft(density, LayoutDirection.Ltr).toDp() }
    val horizontalPaddingRight = with(density) { WindowInsets.captionBar.getRight(density, LayoutDirection.Ltr).toDp() }

    val restrictedLeft = WindowManager.restrictedLeftPadding.value
    val restrictedRight = WindowManager.restrictedRightPadding.value

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(totalBarHeight)
            .background(LocalExtendedColors.current.tabBarBackground),
        contentAlignment = Alignment.BottomStart
    ) {
        val currentMaxWidth = maxWidth

        // Unified Safety Fallback for all windowed modes (DeX & Pop-ups)
        val safetyPaddingLeft = if (hasCaption && restrictedLeft == 0.dp && horizontalPaddingLeft == 0.dp) {
            minOf(80.dp, currentMaxWidth * 0.2f)
        } else 0.dp

        val safetyPaddingRight = if (hasCaption && restrictedRight == 0.dp && horizontalPaddingRight == 0.dp) {
            minOf(160.dp, currentMaxWidth * 0.4f)
        } else 0.dp

        val finalPaddingLeft = maxOf(horizontalPaddingLeft, restrictedLeft, safetyPaddingLeft)
        val finalPaddingRight = maxOf(horizontalPaddingRight, restrictedRight, safetyPaddingRight)

        // Prevent layout explosion in very narrow windows
        val maxTotalPadding = currentMaxWidth * 0.7f
        val adjustedPaddingRight = if (finalPaddingLeft + finalPaddingRight > maxTotalPadding) {
            (maxTotalPadding - finalPaddingLeft).coerceAtLeast(0.dp)
        } else finalPaddingRight

        val available = currentMaxWidth - ADD_BUTTON_WIDTH - finalPaddingLeft - adjustedPaddingRight - 16.dp
        val targetSlotWidth: Dp = if (tabStates.isEmpty()) TAB_SLOT_MAX else {
            (available / tabStates.size).coerceIn(TAB_SLOT_MIN, TAB_SLOT_MAX)
        }

        val slotWidth by animateDpAsState(
            targetValue = targetSlotWidth,
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            label = "slotWidth"
        )

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
                key(state) {
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

                    // Visibility logic: Immediate for the first tab of a window to avoid DeX startup races
                    var isActuallyClosing by remember(state) { mutableStateOf(false) }
                    val isInitialTab = remember { index == 0 && tabStates.size == 1 }
                    var isVisible by remember(state) { mutableStateOf(isInitialTab) }

                    val isLayoutReady = currentMaxWidth > 0.dp
                    LaunchedEffect(state, isLayoutReady) {
                        if (isLayoutReady && !isVisible) {
                            if (!isInitialTab) delay(100)
                            isVisible = true
                        }
                    }

                    AnimatedVisibility(
                        visible = isVisible && !isActuallyClosing,
                        enter = if (isInitialTab) androidx.compose.animation.EnterTransition.None else (expandHorizontally(animationSpec = tween(200)) + fadeIn(tween(200))),
                        exit = shrinkHorizontally(animationSpec = tween(200)) + fadeOut(tween(200)),
                        modifier = if (useInCaptionTabs) Modifier.systemGestureExclusion() else Modifier
                    ) {
                        TabItem(
                            text = state.currentName,
                            painter = painter,
                            slotWidth = slotWidth,
                            selected = (selectedTabIndex == index),
                            canClose = tabStates.size > 1,
                            onClick = { onTabSelected(index) },
                            onClose = {
                                isActuallyClosing = true
                                coroutineScope.launch {
                                    delay(200) // Match exit animation duration
                                    onCloseTab(state)
                                }
                            }
                        )
                    }
                }
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
    canClose: Boolean,
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
            .pointerInput(canClose) {
                awaitEachGesture {
                    val event = awaitPointerEvent()
                    val isMouse = event.changes.any { it.type == PointerType.Mouse }
                    val isTertiary = event.buttons.isTertiaryPressed
                    if (canClose && isMouse && event.type == PointerEventType.Press && isTertiary) {
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

            if (canClose) {
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
}
