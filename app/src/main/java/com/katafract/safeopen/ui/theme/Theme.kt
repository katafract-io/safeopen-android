package com.katafract.safeopen.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// SafeOpen color palette
private val EmergentGreen = Color(0xFF10B981)
private val EmergentGreenDark = Color(0xFF059669)
private val SafeGreen = Color(0xFF34D399)
private val CautionAmber = Color(0xFFF59E0B)
private val DangerRed = Color(0xFFEF4444)
private val ErrorRed = Color(0xFFF87171)
private val NeutralGray = Color(0xFF6B7280)
private val SurfaceLight = Color(0xFFFAFAFA)
private val SurfaceDark = Color(0xFF1F2937)

private val LightColorScheme = lightColorScheme(
    primary = EmergentGreen,
    onPrimary = Color.White,
    primaryContainer = SafeGreen,
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = CautionAmber,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = DangerRed,
    onTertiary = Color.White,
    tertiaryContainer = ErrorRed,
    onTertiaryContainer = Color(0xFF7F1D1D),
    error = DangerRed,
    onError = Color.White,
    errorContainer = ErrorRed,
    onErrorContainer = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = SurfaceLight,
    onSurface = Color(0xFF1F2937),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFD1D5DB)
)

private val DarkColorScheme = darkColorScheme(
    primary = SafeGreen,
    onPrimary = Color(0xFF064E3B),
    primaryContainer = EmergentGreenDark,
    onPrimaryContainer = Color.White,
    secondary = CautionAmber,
    onSecondary = Color(0xFF78350F),
    secondaryContainer = Color(0xFF92400E),
    onSecondaryContainer = Color.White,
    tertiary = ErrorRed,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xB3B3B3),
    onTertiaryContainer = Color.White,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = DangerRed,
    onErrorContainer = Color.White,
    background = SurfaceDark,
    onBackground = Color.White,
    surface = Color(0xFF111827),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF374151),
    onSurfaceVariant = Color(0xFFD1D5DB),
    outline = Color(0xFF9CA3AF)
)

@Composable
fun SafeOpenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
