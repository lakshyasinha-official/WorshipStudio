package com.example.worshipstudio.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.worshipstudio.utils.AppTheme

// ─────────────────────────────────────────────────────────────────────────────
// NEON — dark mint (Material scheme used by not-yet-migrated screens)
// ─────────────────────────────────────────────────────────────────────────────
private val NeonScheme = darkColorScheme(
    primary              = Color(0xFF4EE0A0),
    onPrimary            = Color(0xFF06281A),
    primaryContainer     = Color(0xFF1E4A38),
    onPrimaryContainer   = Color(0xFFA8F0D2),
    secondary            = Color(0xFFA5B4FC),
    onSecondary          = Color(0xFF1B1660),
    secondaryContainer   = Color(0xFF32306B),
    onSecondaryContainer = Color(0xFFDDE1FF),
    tertiary             = Color(0xFF6BCEFF),
    onTertiary           = Color(0xFF00334A),
    background           = Color(0x550A0F0D),
    onBackground         = Color(0xFFF2F5F3),
    surface              = Color(0xF0111814),
    onSurface            = Color(0xFFF2F5F3),
    surfaceVariant       = Color(0xFF1A231E),
    onSurfaceVariant     = Color(0xFFB8C4BE),
    outline              = Color(0x55AACCBB),
    error                = Color(0xFFFF6B6B),
    onError              = Color.White,
    errorContainer       = Color(0x554A0000),
    onErrorContainer     = Color(0xFFFFB3B3)
)

// ─────────────────────────────────────────────────────────────────────────────
// LIGHT — mint on ivory (Material scheme used by not-yet-migrated screens)
// ─────────────────────────────────────────────────────────────────────────────
private val MintLightScheme = lightColorScheme(
    primary              = Color(0xFF10B981),
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFCDEFE2),
    onPrimaryContainer   = Color(0xFF06301F),
    secondary            = Color(0xFF4F46E5),
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFE2E0FB),
    onSecondaryContainer = Color(0xFF1B1660),
    tertiary             = Color(0xFF3A8F74),
    onTertiary           = Color.White,
    background           = Color(0x11EAF2ED),   // near-transparent — gradient shows
    onBackground         = Color(0xFF16211B),
    surface              = Color(0xCCFFFFFF),
    onSurface            = Color(0xFF16211B),
    surfaceVariant       = Color(0xAAEFF5F1),
    onSurfaceVariant     = Color(0xFF3C4A42),
    outline              = Color(0xFFB7C6BE),
    error                = Color(0xFFC62828),
    onError              = Color.White,
    errorContainer       = Color(0xFFF9DEDC),
    onErrorContainer     = Color(0xFF410E0B)
)

// ─────────────────────────────────────────────────────────────────────────────
// Public theme composable
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun WorshipStudioTheme(
    appTheme: AppTheme = AppTheme.NIGHTFALL,
    content: @Composable () -> Unit
) {
    val isDark = appTheme == AppTheme.NIGHTFALL

    val colorScheme = if (isDark) NeonScheme else MintLightScheme

    // Drive the Mint design tokens (new-style screens) from the app theme.
    Mint.isLight = !isDark

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
