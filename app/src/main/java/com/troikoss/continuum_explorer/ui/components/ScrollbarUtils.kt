package com.troikoss.continuum_explorer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class GridScrollMetrics(
    val viewportHeight: Float,
    val estimatedContentHeight: Float,
    val columnsCount: Int,
    val avgItemHeight: Float
)

fun LazyGridLayoutInfo.computeScrollMetrics(): GridScrollMetrics? {
    val visibleItems = visibleItemsInfo
    if (visibleItems.isEmpty() || totalItemsCount == 0) return null
    val viewportHeight = (viewportEndOffset - viewportStartOffset).toFloat()
    if (viewportHeight == 0f) return null
    val avgItemHeight = visibleItems.sumOf { it.size.height }.toFloat() / visibleItems.size
    val columnsCount = visibleItems.count { it.offset.y == visibleItems.first().offset.y }.coerceAtLeast(1)
    val totalRows = (totalItemsCount + columnsCount - 1) / columnsCount
    return GridScrollMetrics(
        viewportHeight = viewportHeight,
        estimatedContentHeight = totalRows * avgItemHeight,
        columnsCount = columnsCount,
        avgItemHeight = avgItemHeight
    )
}

@Composable
fun VerticalScrollbar(
    gridState: LazyGridState,
    isNearEdge: Boolean,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    val isScrollable by remember(gridState) {
        derivedStateOf {
            val m = gridState.layoutInfo.computeScrollMetrics() ?: return@derivedStateOf false
            m.estimatedContentHeight > m.viewportHeight + 1f
        }
    }

    val isScrolling = gridState.isScrollInProgress
    val alpha by animateFloatAsState(
        targetValue = if ((isNearEdge || isScrolling) && isScrollable) 1f else 0f,
        animationSpec = if ((isNearEdge || isScrolling) && isScrollable) tween(150) else tween(durationMillis = 300, delayMillis = 500),
        label = "v_scrollbar_alpha"
    )

    val thumbColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(16.dp)
            .pointerInput(gridState) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    var prevY = down.position.y
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val dragDeltaY = change.position.y - prevY
                        if (dragDeltaY != 0f) {
                            change.consume()
                            val m = gridState.layoutInfo.computeScrollMetrics()
                            if (m != null && size.height > 0f) {
                                coroutineScope.launch { gridState.scrollBy(dragDeltaY * (m.estimatedContentHeight / size.height)) }
                            }
                        }
                        prevY = change.position.y
                    }
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .width(6.dp)
                .padding(vertical = 4.dp)
                .align(Alignment.CenterEnd)
        ) {
            if (alpha == 0f) return@Canvas
            val m = gridState.layoutInfo.computeScrollMetrics() ?: return@Canvas

            val thumbFraction = (m.viewportHeight / m.estimatedContentHeight).coerceIn(0.08f, 1f)
            val thumbHeight = size.height * thumbFraction
            val scrolledAmount = (gridState.firstVisibleItemIndex.toFloat() / m.columnsCount) * m.avgItemHeight +
                    gridState.firstVisibleItemScrollOffset
            val scrollFraction = (scrolledAmount / (m.estimatedContentHeight - m.viewportHeight)).coerceIn(0f, 1f)
            val thumbTop = (size.height - thumbHeight) * scrollFraction

            // Track
            drawRoundRect(
                color = thumbColor.copy(alpha = alpha * 0.15f),
                topLeft = Offset(0f, 0f),
                size = size,
                cornerRadius = CornerRadius(size.width / 2f)
            )
            // Thumb
            drawRoundRect(
                color = thumbColor.copy(alpha = alpha * 0.55f),
                topLeft = Offset(0f, thumbTop),
                size = Size(size.width, thumbHeight),
                cornerRadius = CornerRadius(size.width / 2f)
            )
        }
    }
}

@Composable
fun HorizontalScrollbar(
    scrollState: ScrollState,
    isNearEdge: Boolean,
    isRecentlyScrolled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val isScrollable by remember { derivedStateOf { scrollState.maxValue > 0 } }

    val isScrolling = scrollState.isScrollInProgress
    val alpha by animateFloatAsState(
        targetValue = if ((isNearEdge || isScrolling || isRecentlyScrolled) && isScrollable) 1f else 0f,
        animationSpec = if ((isNearEdge || isScrolling || isRecentlyScrolled) && isScrollable) tween(150) else tween(durationMillis = 300, delayMillis = 500),
        label = "h_scrollbar_alpha"
    )

    val thumbColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp)
            .pointerInput(scrollState) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    var prevX = down.position.x
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val dragDeltaX = change.position.x - prevX
                        if (dragDeltaX != 0f) {
                            change.consume()
                            val maxValue = scrollState.maxValue
                            if (maxValue > 0 && size.width > 0) {
                                val contentWidth = size.width + maxValue.toFloat()
                                val scrollRatio = contentWidth / size.width
                                coroutineScope.launch { scrollState.scrollBy(dragDeltaX * scrollRatio) }
                            }
                        }
                        prevX = change.position.x
                    }
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .padding(horizontal = 4.dp)
                .align(Alignment.BottomCenter)
        ) {
            if (alpha == 0f) return@Canvas
            val maxValue = scrollState.maxValue
            if (maxValue <= 0) return@Canvas

            val contentWidth = size.width + maxValue
            val thumbFraction = (size.width / contentWidth).coerceIn(0.08f, 1f)
            val thumbWidth = size.width * thumbFraction
            val scrollFraction = (scrollState.value.toFloat() / maxValue).coerceIn(0f, 1f)
            val thumbLeft = (size.width - thumbWidth) * scrollFraction

            // Track
            drawRoundRect(
                color = thumbColor.copy(alpha = alpha * 0.15f),
                topLeft = Offset(0f, 0f),
                size = size,
                cornerRadius = CornerRadius(size.height / 2f)
            )
            // Thumb
            drawRoundRect(
                color = thumbColor.copy(alpha = alpha * 0.55f),
                topLeft = Offset(thumbLeft, 0f),
                size = Size(thumbWidth, size.height),
                cornerRadius = CornerRadius(size.height / 2f)
            )
        }
    }
}
