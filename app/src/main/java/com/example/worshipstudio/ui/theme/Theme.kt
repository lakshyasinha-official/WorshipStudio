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
// NIGHTFALL  — dark purple / blue (uses app_bg.jpg image in MainActivity)
// ─────────────────────────────────────────────────────────────────────────────
private val NightfallScheme = darkColorScheme(
    primary              = Color(0xFFBB86FC),
    onPrimary            = Color(0xFF1A0033),
    primaryContainer     = Color(0x664A0080),
    onPrimaryContainer   = Color(0xFFD7AAFF),
    secondary            = Color(0xFFFF79C6),
    onSecondary          = Color(0xFF2D0030),
    secondaryContainer   = Color(0x554A0050),
    onSecondaryContainer = Color(0xFFFFB3E6),
    tertiary             = Color(0xFF6BCEFF),
    onTertiary           = Color(0xFF00334A),
    background           = Color(0x550A0A18),
    onBackground         = Color.White,
    surface              = Color(0x660D0D20),
    onSurface            = Color.White,
    surfaceVariant       = Color(0x4D1A1A35),
    onSurfaceVariant     = Color(0xCCDDDDFF),
    outline              = Color(0x55AAAACC),
    error                = Color(0xFFFF6B6B),
    onError              = Color.White,
    errorContainer       = Color(0x554A0000),
    onErrorContainer     = Color(0xFFFFB3B3)
)

// ─────────────────────────────────────────────────────────────────────────────
// DAWN MIST  — ivory · peach · blush  (light warm)
// ─────────────────────────────────────────────────────────────────────────────
private val DawnMistScheme = lightColorScheme(
    primary              = Color(0xFFC4836C),   // warm terracotta
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFFFD5C4),
    onPrimaryContainer   = Color(0xFF3A1008),
    secondary            = Color(0xFFAA7BA0),   // muted plum
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFF2D9E8),
    onSecondaryContainer = Color(0xFF3A0A30),
    tertiary             = Color(0xFFD4956A),
    onTertiary           = Color.White,
    background           = Color(0x11FAE8D8),   // near-transparent — gradient shows
    onBackground         = Color(0xFF2A1818),
    surface              = Color(0xCCFFF8F4),   // warm white, 80% opaque
    onSurface            = Color(0xFF2A1818),
    surfaceVariant       = Color(0xAAF5EBE4),
    onSurfaceVariant     = Color(0xFF5A3830),
    outline              = Color(0xFFD4B8B0),
    error                = Color(0xFFB3261E),
    onError              = Color.White,
    errorContainer       = Color(0xFFF9DEDC),
    onErrorContainer     = Color(0xFF410E0B)
)

// ─────────────────────────────────────────────────────────────────────────────
// HOLY LIGHT  — sky blue · white · sage  (light cool)
// ─────────────────────────────────────────────────────────────────────────────
private val HolyLightScheme = lightColorScheme(
    primary              = Color(0xFF3A7FC1),   // sky blue
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFB8D8F8),
    onPrimaryContainer   = Color(0xFF0A2A4A),
    secondary            = Color(0xFF4A9975),   // sage
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFB8ECD8),
    onSecondaryContainer = Color(0xFF0A3020),
    tertiary             = Color(0xFF7A9ECC),
    onTertiary           = Color.White,
    background           = Color(0x11D6EAFF),   // near-transparent
    onBackground         = Color(0xFF1A2A3A),
    surface              = Color(0xCCF5FAFF),   // cool white, 80% opaque
    onSurface            = Color(0xFF1A2A3A),
    surfaceVariant       = Color(0xAAE8F3FF),
    onSurfaceVariant     = Color(0xFF2A4060),
    outline              = Color(0xFFB0C8E0),
    error                = Color(0xFFB3261E),
    onError              = Color.White,
    errorContainer       = Color(0xFFF9DEDC),
    onErrorContainer     = Color(0xFF410E0B)
)

// ─────────────────────────────────────────────────────────────────────────────
// SANCTUARY  — cream · sage · lavender  (light neutral)
// ─────────────────────────────────────────────────────────────────────────────
private val SanctuaryScheme = lightColorScheme(
    primary              = Color(0xFF7A8CCC),   // lavender
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFD4D9F0),
    onPrimaryContainer   = Color(0xFF151A40),
    secondary            = Color(0xFF6A9B6A),   // sage green
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFCCE8CC),
    onSecondaryContainer = Color(0xFF0A2A0A),
    tertiary             = Color(0xFF9B8ACA),
    onTertiary           = Color.White,
    background           = Color(0x11EDE8F6),   // near-transparent
    onBackground         = Color(0xFF252535),
    surface              = Color(0xCCFAFBF8),   // cream white, 80% opaque
    onSurface            = Color(0xFF252535),
    surfaceVariant       = Color(0xAAEEF0EC),
    onSurfaceVariant     = Color(0xFF404050),
    outline              = Color(0xFFBBBBCC),
    error                = Color(0xFFB3261E),
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
    val colorScheme = when (appTheme) {
        AppTheme.NIGHTFALL  -> NightfallScheme
        AppTheme.DAWN_MIST  -> DawnMistScheme
        AppTheme.HOLY_LIGHT -> HolyLightScheme
        AppTheme.SANCTUARY  -> SanctuaryScheme
    }

    val isDark = appTheme == AppTheme.NIGHTFALL

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
