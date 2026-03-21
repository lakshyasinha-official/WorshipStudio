package com.example.worshipstudio.utils

import android.content.Context

object ThemeStore {
    private const val PREFS     = "worship_prefs"
    private const val KEY_THEME = "app_theme"

    fun save(context: Context, theme: AppTheme) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, theme.name).apply()
    }

    fun load(context: Context): AppTheme {
        val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_THEME, AppTheme.NIGHTFALL.name)
        return AppTheme.entries.firstOrNull { it.name == name } ?: AppTheme.NIGHTFALL
    }
}
