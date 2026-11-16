package com.example.healthsync.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF2DCE89),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6F8F0),
    onPrimaryContainer = Color(0xFF0F5A38),
    secondary = Color(0xFF007BFF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEBF3FF),
    onSecondaryContainer = Color(0xFF003E80),
    background = Color(0xFFF7F9FC),
    surface = Color.White,
    error = Color(0xFFF5365C),
    onError = Color.White,
    surfaceVariant = Color(0xFFEEF2F6),
    onSurfaceVariant = Color(0xFF4A5568)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF2DCE89),
    secondary = Color(0xFF007BFF),
    background = Color(0xFF121417),
    surface = Color(0xFF1C1E22),
    primaryContainer = Color(0xFF1B3D2F),
    secondaryContainer = Color(0xFF162E4A)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun HealthSyncTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = Typography(),
        shapes = AppShapes,
        content = content,
    )
}
