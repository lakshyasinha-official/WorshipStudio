package com.example.worshipstudio.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.worshipstudio.R

// ─────────────────────────────────────────────────────────────────────────────
// Inter — bundled in res/font, no network or Play Services required
// ─────────────────────────────────────────────────────────────────────────────
val AppFontFamily = FontFamily(
    Font(R.font.inter_regular,  FontWeight.Normal),
    Font(R.font.inter_medium,   FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold,     FontWeight.Bold)
)

// Material3 default scale, re-based on Inter
private val base = Typography()

val Typography = Typography(
    displayLarge   = base.displayLarge.copy(fontFamily = AppFontFamily),
    displayMedium  = base.displayMedium.copy(fontFamily = AppFontFamily),
    displaySmall   = base.displaySmall.copy(fontFamily = AppFontFamily),
    headlineLarge  = base.headlineLarge.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold),
    headlineSmall  = base.headlineSmall.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold),
    titleLarge     = base.titleLarge.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold),
    titleMedium    = base.titleMedium.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold),
    titleSmall     = base.titleSmall.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium),
    bodyLarge      = base.bodyLarge.copy(fontFamily = AppFontFamily),
    bodyMedium     = base.bodyMedium.copy(fontFamily = AppFontFamily),
    bodySmall      = base.bodySmall.copy(fontFamily = AppFontFamily),
    labelLarge     = base.labelLarge.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium),
    labelMedium    = base.labelMedium.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium),
    labelSmall     = base.labelSmall.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium)
)
