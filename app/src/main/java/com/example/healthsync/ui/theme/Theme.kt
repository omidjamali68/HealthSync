package com.example.healthsync.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF2DCE89), // Green from header
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F5E9),
    secondary = Color(0xFF007BFF), // Blue from buttons
    onSecondary = Color.White,
    background = Color(0xFFF8F9FE), // Light gray background
    surface = Color.White,
    error = Color(0xFFF5365C), // Red for delete/errors
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF2DCE89),
    secondary = Color(0xFF007BFF),
    background = Color(0xFF1A1C1E),
    surface = Color(0xFF1A1C1E),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp)
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
