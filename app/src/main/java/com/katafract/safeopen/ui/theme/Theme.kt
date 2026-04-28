package com.katafract.safeopen.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.katafract.safeopen.models.RiskLevel

// ── Material 3 color schemes ────────────────────────────────────────────
// Note: we DO NOT use dynamic color (Material You) — SafeOpen's brand
// hinges on the verdict-color contract; we never want OEM tinting to
// recolor Safe/Suspicious/Dangerous surfaces.

private val LightColorScheme = lightColorScheme(
    primary = SafeGreen500,
    onPrimary = Color.White,
    primaryContainer = SafeGreenSoft,
    onPrimaryContainer = SafeGreenDark,
    secondary = CautionAmber500,
    onSecondary = Color.White,
    secondaryContainer = CautionAmberSoft,
    onSecondaryContainer = CautionAmberDark,
    tertiary = DangerRed500,
    onTertiary = Color.White,
    tertiaryContainer = DangerRedSoft,
    onTertiaryContainer = DangerRedDark,
    error = DangerRed500,
    onError = Color.White,
    errorContainer = DangerRedSoft,
    onErrorContainer = DangerRedDark,
    background = Color.White,
    onBackground = Neutral900,
    surface = Neutral50,
    onSurface = Neutral900,
    surfaceVariant = Neutral100,
    onSurfaceVariant = Neutral500,
    outline = Neutral300,
    outlineVariant = Neutral200,
)

private val DarkColorScheme = darkColorScheme(
    primary = SafeGreen300,
    onPrimary = SafeGreenDark,
    primaryContainer = SafeGreen600,
    onPrimaryContainer = Color.White,
    secondary = CautionAmber500,
    onSecondary = CautionAmberDark,
    secondaryContainer = CautionAmber600,
    onSecondaryContainer = Color.White,
    tertiary = DangerRed300,
    onTertiary = Color.White,
    tertiaryContainer = DangerRed600,
    onTertiaryContainer = Color.White,
    error = DangerRed300,
    onError = Color.White,
    errorContainer = DangerRed600,
    onErrorContainer = Color.White,
    background = KataMidnight,
    onBackground = Color.White,
    surface = Neutral900,
    onSurface = Color.White,
    surfaceVariant = Neutral700,
    onSurfaceVariant = Neutral300,
    outline = Neutral500,
    outlineVariant = Neutral700,
)

// ── Risk palette (verdict tokens) ────────────────────────────────────────
// Risk colors are deliberately *fixed* across light/dark — Safe means
// emerald, Dangerous means red, full stop. Soft variants are surfaces.

data class RiskPalette(
    val color: Color,
    val onColor: Color,
    val soft: Color,
    val onSoft: Color,
)

@Composable
fun riskPalette(level: RiskLevel): RiskPalette = when (level) {
    RiskLevel.LOW -> RiskPalette(
        color = SafeGreen500,
        onColor = Color.White,
        soft = if (isSystemInDarkTheme()) SafeGreen600.copy(alpha = 0.18f) else SafeGreenSoft,
        onSoft = if (isSystemInDarkTheme()) SafeGreen300 else SafeGreenDark,
    )
    RiskLevel.CAUTION -> RiskPalette(
        color = CautionAmber500,
        onColor = Color.White,
        soft = if (isSystemInDarkTheme()) CautionAmber600.copy(alpha = 0.18f) else CautionAmberSoft,
        onSoft = if (isSystemInDarkTheme()) CautionAmber500 else CautionAmberDark,
    )
    RiskLevel.HIGH -> RiskPalette(
        color = DangerRed500,
        onColor = Color.White,
        soft = if (isSystemInDarkTheme()) DangerRed600.copy(alpha = 0.20f) else DangerRedSoft,
        onSoft = if (isSystemInDarkTheme()) DangerRed300 else DangerRedDark,
    )
    RiskLevel.UNKNOWN -> RiskPalette(
        color = Neutral500,
        onColor = Color.White,
        soft = if (isSystemInDarkTheme()) Neutral700 else Neutral100,
        onSoft = if (isSystemInDarkTheme()) Neutral300 else Neutral700,
    )
}

@Composable
fun SafeOpenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Status / nav bar tinting per theme. We make the system bars
    // transparent so edge-to-edge layouts can paint underneath, then
    // ask the system to use the appropriate icon contrast.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                window.navigationBarColor = Color.Transparent.toArgb()
            }
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SafeOpenTypography,
        content = content,
    )
}
