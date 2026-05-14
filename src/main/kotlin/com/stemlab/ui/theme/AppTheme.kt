package com.stemlab.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryGreen = Color(0xFF4CAF50)
val PrimaryGreenVariant = Color(0xFF388E3C)
val SecondaryBlue = Color(0xFF2196F3)
val SurfaceDark = Color(0xFF1E1E2E)
val SurfaceVariant = Color(0xFF2A2A3E)
val CardBackground = Color(0xFF252538)
val OnSurface = Color(0xFFE0E0E0)
val OnSurfaceDim = Color(0xFF9E9E9E)
val SuccessGreen = Color(0xFF66BB6A)
val WarningAmber = Color(0xFFFFB300)
val ErrorRed = Color(0xFFEF5350)
val AccentCyan = Color(0xFF00BCD4)

private val DarkColors = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.Black,
    primaryContainer = PrimaryGreenVariant,
    secondary = SecondaryBlue,
    background = SurfaceDark,
    surface = CardBackground,
    surfaceVariant = SurfaceVariant,
    onBackground = OnSurface,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceDim
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
