package com.example.worshipstudio.utils

// Enum names kept from the old 4-theme system so stored preferences
// keep working; ThemeStore falls back to NIGHTFALL for removed values.
enum class AppTheme(
    val displayName: String,
    val subtitle: String
) {
    NIGHTFALL("Neon",  "Mint · Charcoal"),
    DAWN_MIST("Light", "Mint · Ivory")
}
