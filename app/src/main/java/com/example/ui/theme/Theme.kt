package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisColorScheme = darkColorScheme(
    primary = JarvisPrimaryNeon,
    onPrimary = Color(0xFF001D21),
    secondary = JarvisSecondaryBlue,
    onSecondary = Color.White,
    tertiary = JarvisTertiaryOrange,
    onTertiary = Color.White,
    background = JarvisBackground,
    onBackground = JarvisOnBackground,
    surface = JarvisSurface,
    onSurface = JarvisOnSurface,
    primaryContainer = Color(0xFF00373E),
    onPrimaryContainer = JarvisPrimaryNeon,
    surfaceVariant = Color(0xFF092136),
    onSurfaceVariant = JarvisOnBackground
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force sci-fi dark theme always
    dynamicColor: Boolean = false, // Disable to retain custom themed cinematic design
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}
