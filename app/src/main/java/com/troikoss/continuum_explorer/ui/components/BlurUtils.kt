package com.troikoss.continuum_explorer.ui.components

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import com.troikoss.continuum_explorer.ui.theme.LocalExtendedColors
import java.io.File

object CrashLogger {
    var lastError by mutableStateOf<String?>(null)
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val crashFile = File(context.filesDir, "last_crash.txt")
        if (crashFile.exists()) {
            try {
                val text = crashFile.readText()
                if (text.isNotBlank()) {
                    lastError = text
                }
                crashFile.delete()
            } catch (_: Exception) {}
        }

        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val errorText = "${throwable.javaClass.name}: ${throwable.message}\n" +
                    throwable.stackTrace.take(20).joinToString("\n") { "  at $it" }
            try {
                crashFile.writeText(errorText)
            } catch (_: Exception) {}
            lastError = errorText
            originalHandler?.uncaughtException(thread, throwable)
        }
    }
}

@Composable
fun CrashDialog() {
    val error = CrashLogger.lastError
    if (error != null) {
        AlertDialog(
            onDismissRequest = { CrashLogger.lastError = null },
            title = { Text("Crash Caught", color = MaterialTheme.colorScheme.error) },
            text = {
                SelectionContainer {
                    Text(
                        text = error,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
            },
            confirmButton = {
                Button(onClick = { CrashLogger.lastError = null }) {
                    Text("Dismiss")
                }
            }
        )
    }
}

val LocalHazeState = compositionLocalOf { HazeState() }

/**
 * Modifier applied to scrollable/background screen content (e.g. ExplorerBody) to capture pixels for Haze blur.
 */
fun Modifier.hazeBackground(hazeState: HazeState): Modifier =
    this.hazeSource(state = hazeState)
/**
 * Crash-proof Modifier applied to floating overlays, context menus, top bar, or sidebars to render the blurred backdrop.
 */
fun Modifier.hazeBlur(
    hazeState: HazeState,
    tintColor: Color = Color.Unspecified,
    shape: Shape = RoundedCornerShape(16.dp)
): Modifier {
    val effectiveColor = if (tintColor != Color.Unspecified) tintColor else Color(0xFF1C1C1C).copy(alpha = 0.65f)
    val style = HazeStyle(
        blurRadius = 24.dp,
        tint = HazeTint(effectiveColor),
        backgroundColor = effectiveColor
    )

    return this.clip(shape).hazeEffect(state = hazeState, style = style)
}

/**
 * Returns translucent menu background color for Haze glassmorphism.
 */
@Composable
fun menuContainerColor(): Color {
    return LocalExtendedColors.current.menuBackground.copy(alpha = 0.65f)
}

/**
 * Helper to enable window tracking.
 */
@Composable
fun EnableWindowBlur() { }
