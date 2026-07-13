package com.example.worshipstudio.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Design tokens for the app's visual language, with a dark ("Neon") and a
 * light variant. `isLight` is set by [WorshipStudioTheme] from the selected
 * app theme; because it is Compose state, every screen reading these tokens
 * recomposes automatically when the theme changes.
 */
object Mint {
    var isLight by mutableStateOf(false)

    val Accent        get() = if (isLight) Color(0xFF10B981) else Color(0xFF4EE0A0)
    val AccentDeep    get() = if (isLight) Color(0xFF0B7A55) else Color(0xFF1E7A52)
    val OnAccent      get() = if (isLight) Color(0xFFFFFFFF) else Color(0xFF06281A)
    val BgTop         get() = if (isLight) Color(0xFFF5FAF7) else Color(0xFF0D1311)
    val BgBottom      get() = if (isLight) Color(0xFFEAF2ED) else Color(0xFF070B09)
    val Card          get() = if (isLight) Color(0xFFFFFFFF) else Color(0xFF111814)
    val Field         get() = if (isLight) Color(0xFFEFF5F1) else Color(0xFF1A231E)
    val BorderSubtle  get() = if (isLight) Color(0x14000000) else Color(0x14FFFFFF)
    val BorderField   get() = if (isLight) Color(0x22000000) else Color(0x1FFFFFFF)
    val TextPrimary   get() = if (isLight) Color(0xFF16211B) else Color(0xFFF2F5F3)
    val TextSecondary get() = if (isLight) Color(0xFF5C6963) else Color(0xFF98A39D)
    val Error         get() = if (isLight) Color(0xFFC62828) else Color(0xFFFF6B6B)

    // Indigo badge (key labels, tag chips) — mirrors the mockup's purple pills
    val Indigo        get() = if (isLight) Color(0xFF4F46E5) else Color(0xFFA5B4FC)
    val IndigoBg      get() = if (isLight) Color(0x1F6366F1) else Color(0x2E6366F1)
}
