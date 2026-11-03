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
    surfaceVariant = Color(0xFFE7EBF6),
    onSurfaceVariant = Color(0xFF4B566B),
    outline = Color(0xFF707B90),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA7CCFF),
    primaryContainer = Color(0xFF194475),
    onPrimaryContainer = PaleBlue,
    secondary = Color(0xFF7BD5E8),
    tertiary = Color(0xFFCDBDF6),
    background = Night,
    onBackground = Color(0xFFE1E7F5),
    surface = Color(0xFF1C2639),
    onSurface = Color(0xFFE1E7F5),
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
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp),
        ),
        content = content,
    )
}
