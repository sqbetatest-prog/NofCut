package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CustomDarkColorScheme = darkColorScheme(
    primary = PrimaryNeon,
    onPrimary = Color(0xFF381E72), // Deep purple for contrast text on primary fields
    secondary = AccentBlue,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = TextActive,
    onSurface = TextActive,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSubtle,
    outline = CustomBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark Theme for Video Editor consistency
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // We enforce an elegant premium dark cinematic look
    MaterialTheme(
        colorScheme = CustomDarkColorScheme,
        typography = Typography,
        content = content
    )
}
