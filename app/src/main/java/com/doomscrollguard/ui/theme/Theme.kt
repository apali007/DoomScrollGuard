package com.doomscrollguard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Calm = Color(0xFF5B8DEF)
val CalmDeep = Color(0xFF0E1726)
val Leaf = Color(0xFF3DBE8B)

private val LightColors = lightColorScheme(
    primary = Calm,
    secondary = Leaf,
)

private val DarkColors = darkColorScheme(
    primary = Calm,
    secondary = Leaf,
    background = CalmDeep,
    surface = Color(0xFF162236),
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
