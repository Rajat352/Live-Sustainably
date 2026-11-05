package com.livesustainably.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = PaleBlue,
    onPrimaryContainer = Color(0xFF123865),
    secondary = Cyan,
    secondaryContainer = Color(0xFFCBF1FA),
    onSecondaryContainer = Color(0xFF003845),
    tertiary = Lavender,
    background = Mist,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE8ECF8),
    onSurfaceVariant = Color(0xFF6B6884),
    outline = Color(0xFF8784A2),
    outlineVariant = PaleBlue,
    surfaceContainer = Color(0xFFF9FBFF),
    surfaceContainerLow = Color(0xFFEBF2FF),
    surfaceContainerHigh = Color(0xFFE4EBFA),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA7CCFF),
    onPrimary = Color(0xFF13356C),
    primaryContainer = Color(0xFF194475),
    onPrimaryContainer = PaleBlue,
    secondary = Color(0xFF7BD5E8),
    tertiary = Color(0xFFCDBDF6),
    background = Night,
    onBackground = Color(0xFFE1E7F5),
    surface = Color(0xFF1C2639),
    onSurface = Color(0xFFE1E7F5),
    outlineVariant = Color(0xFF334565),
    surfaceContainer = Color(0xFF253149),
    surfaceContainerLow = Color(0xFF202A42),
    surfaceContainerHigh = Color(0xFF303D59),
    surfaceVariant = Color(0xFF303C52),
    onSurfaceVariant = Color(0xFFC0C9DA),
)

@Composable
fun LiveSustainablyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = Shapes(
            small = RoundedCornerShape(18.dp),
            medium = RoundedCornerShape(24.dp),
            large = RoundedCornerShape(32.dp),
        ),
        content = content,
    )
}
