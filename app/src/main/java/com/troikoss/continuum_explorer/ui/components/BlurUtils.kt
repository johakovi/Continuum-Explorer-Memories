package com.troikoss.continuum_explorer.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.troikoss.continuum_explorer.ui.theme.LocalExtendedColors

object MenuBlurState {
    var isMenuOpen by mutableStateOf(false)
    var menuBounds by mutableStateOf<Rect?>(null)
}

/**
 * Enables in-app menu background blur tracking when placed inside a DropdownMenu or Popup.
 */
@Composable
fun EnableWindowBlur() {
    DisposableEffect(Unit) {
        MenuBlurState.isMenuOpen = true
        onDispose {
            MenuBlurState.isMenuOpen = false
            MenuBlurState.menuBounds = null
        }
    }

    Box(
        modifier = Modifier.onGloballyPositioned { coordinates ->
            val parent = coordinates.parentCoordinates ?: coordinates
            val position = parent.positionInRoot()
            val size = parent.size
            if (size.width > 0 && size.height > 0) {
                MenuBlurState.menuBounds = Rect(
                    left = position.x,
                    top = position.y,
                    right = position.x + size.width,
                    bottom = position.y + size.height
                )
            }
        }
    )
}

/**
 * Modifier placed on background layouts (like Scaffold / main content view)
 * that draws a GPU blur layer ONLY underneath the open context menu bounds,
 * keeping the rest of the screen completely crisp and unblurred.
 */
fun Modifier.menuBackgroundBlur(): Modifier = this.then(
    Modifier.drawWithContent {
        // 1. Always draw the normal screen content (sharp and crisp everywhere)
        drawContent()

        // 2. If a menu is open, draw the blurred layer strictly inside the menu's bounds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && MenuBlurState.isMenuOpen) {
            val bounds = MenuBlurState.menuBounds
            if (bounds != null && !bounds.isEmpty) {
                drawIntoCanvas { canvas ->
                    val cornerRadius = 16.dp.toPx()
                    val path = Path().apply {
                        addRoundRect(
                            RoundRect(
                                rect = bounds,
                                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                            )
                        )
                    }
                    canvas.save()
                    canvas.clipPath(path)

                    val paint = Paint()
                    val frameworkPaint = paint.asFrameworkPaint()
                    try {
                        val blurEffect = RenderEffect.createBlurEffect(
                            25f, 25f, Shader.TileMode.CLAMP
                        )
                        val method = frameworkPaint.javaClass.getMethod("setRenderEffect", RenderEffect::class.java)
                        method.invoke(frameworkPaint, blurEffect)
                    } catch (_: Exception) {
                    }

                    canvas.saveLayer(bounds, paint)
                    drawContent()
                    canvas.restore()

                    canvas.restore()
                }
            }
        }
    }
)

/**
 * Returns a translucent background color on Android 12+ (API 31+) to allow the blurred
 * background underneath the menu to show through, or falls back to alpha = 0.98f on older devices.
 */
@Composable
fun menuContainerColor(blurAlpha: Float = 0.75f, fallbackAlpha: Float = 0.98f): Color {
    val baseColor = LocalExtendedColors.current.menuBackground
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        baseColor.copy(alpha = blurAlpha)
    } else {
        baseColor.copy(alpha = fallbackAlpha)
    }
}
