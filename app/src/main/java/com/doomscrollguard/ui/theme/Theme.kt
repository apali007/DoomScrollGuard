package com.doomscrollguard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// "Touch grass" palette: meadow greens on a deep forest background.
val Grass = Color(0xFF6CC47C) // bright highlights on dark backgrounds, breathing circle
val Leaf = Color(0xFF2A8040) // buttons and "within goal"; white text passes contrast
val Forest = Color(0xFF0E1D14) // full-screen reminder backgrounds

private val LightColors = lightColorScheme(
    primary = Leaf,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEBD2),
    onPrimaryContainer = Color(0xFF0B2913),
    secondary = Grass,
    onSecondary = Color(0xFF0B2913),
    background = Color(0xFFF4F9F1),
    surface = Color(0xFFF4F9F1),
    surfaceVariant = Color(0xFFDCE8D7),
    onSurfaceVariant = Color(0xFF43503F),
    outlineVariant = Color(0xFFC2CFBD),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFEEF5EA),
    surfaceContainer = Color(0xFFE8F1E4),
    surfaceContainerHigh = Color(0xFFE2EDDE),
    surfaceContainerHighest = Color(0xFFDCE8D7),
)

private val DarkColors = darkColorScheme(
    primary = Grass,
    onPrimary = Color(0xFF0B2913),
    primaryContainer = Color(0xFF1F5A2E),
    onPrimaryContainer = Color(0xFFCDEBD2),
    secondary = Leaf,
    onSecondary = Color.White,
    background = Forest,
    surface = Forest,
    surfaceVariant = Color(0xFF223929),
    onSurfaceVariant = Color(0xFFBFCDBA),
    outlineVariant = Color(0xFF3B4C3D),
    surfaceContainerLowest = Color(0xFF0A160F),
    surfaceContainerLow = Color(0xFF142519),
    surfaceContainer = Color(0xFF172A1D),
    surfaceContainerHigh = Color(0xFF1C3123),
    surfaceContainerHighest = Color(0xFF223929),
)

@Composable
fun DoomScrollGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
