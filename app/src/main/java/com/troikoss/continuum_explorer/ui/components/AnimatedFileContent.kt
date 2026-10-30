package com.troikoss.continuum_explorer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.troikoss.continuum_explorer.managers.SettingsManager
import com.troikoss.continuum_explorer.model.NavAnimationDirection
import com.troikoss.continuum_explorer.utils.FileExplorerState

/**
 * High-performance transition container for the content window.
 * Executes horizontal slide and fade transitions strictly within the GPU Draw phase
 * (via Modifier.graphicsLayer) without triggering layout re-measurements or
 * recompositions on each animation frame.
 */
@Composable
fun AnimatedFileContent(
    appState: FileExplorerState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isAnimEnabled = SettingsManager.isContentAnimationsEnabled.value

    // Track whether the first initial screen has finished loading.
    // We do NOT want to animate cold start — the starting screen should appear immediately with zero lag.
    var hasCompletedInitialLoad by remember(appState) { mutableStateOf(false) }

    val animProgress = remember(appState, appState.loadedPathKey) {
        Animatable(if (!hasCompletedInitialLoad || !isAnimEnabled) 1f else 0f)
    }

    val direction = appState.navAnimationDirection

    LaunchedEffect(appState, appState.loadedPathKey) {
        // While on uninitialized state, skip
        if (appState.loadedPathKey == "initial") return@LaunchedEffect

        // When the initial screen finishes loading for the first time, skip animation
        // so cold start is instantaneous and does not stutter during app initialization.
        if (!hasCompletedInitialLoad) {
            hasCompletedInitialLoad = true
            return@LaunchedEffect
        }

        if (!isAnimEnabled || animProgress.value == 1f) return@LaunchedEffect

        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = if (direction == NavAnimationDirection.FADE) 180 else 220,
                easing = FastOutSlowInEasing
            )
        )
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .graphicsLayer {
                if (!isAnimEnabled) {
                    alpha = 1f
                    translationX = 0f
                    translationY = 0f
                    return@graphicsLayer
                }

                val progress = animProgress.value
                alpha = progress.coerceIn(0f, 1f)
                val distancePx = 50.dp.toPx()

                // Lock Y translation at 0 to guarantee pure horizontal motion (no diagonal drift)
                translationY = 0f

                when (direction) {
                    NavAnimationDirection.FORWARD -> {
                        // Enters horizontally from right to left (+X to 0) with fade in
                        translationX = (1f - progress) * distancePx
                    }
                    NavAnimationDirection.BACKWARD -> {
                        // Enters horizontally from left to right (-X to 0) with fade in
                        translationX = -(1f - progress) * distancePx
                    }
                    NavAnimationDirection.FADE -> {
                        // Pure fade in with zero translation
                        translationX = 0f
                    }
                }
            }
    ) {
        content()
    }
}
