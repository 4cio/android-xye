package dev.fourco.xye.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val XyeColorScheme = lightColorScheme(
    primary = Color(0xFF33CC33),
    onPrimary = Color.White,
    secondary = Color(0xFF44BBDD),
    onSecondary = Color.White,
    tertiary = Color(0xFFFFCC00),
    background = Color(0xFFF5F0E8),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFE8E0D0),
    onSurfaceVariant = Color(0xFF4A3728),
)

@Composable
fun XyeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = XyeColorScheme,
        content = content,
    )
}
