package com.example.worshipstudio.utils

import android.content.Context

/**
 * Saves / loads login credentials in SharedPreferences.
 * Only written when the user explicitly checks "Remember me".
 */
object CredentialsStore {

    private const val PREFS      = "worship_prefs"
    private const val KEY_EMAIL  = "saved_email"
    private const val KEY_PASS   = "saved_password"
    private const val KEY_CHURCH = "saved_church"
    private const val KEY_REMEMBER = "remember_me"

    data class SavedCredentials(
        val email: String,
        val password: String,
        val churchId: String
    )

    fun save(context: Context, email: String, password: String, churchId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_EMAIL, email)
            .putString(KEY_PASS, password)
            .putString(KEY_CHURCH, churchId)
            .putBoolean(KEY_REMEMBER, true)
            .apply()
    }

    fun load(context: Context): SavedCredentials? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_REMEMBER, false)) return null
        return SavedCredentials(
            email    = prefs.getString(KEY_EMAIL,  "") ?: "",
            password = prefs.getString(KEY_PASS,   "") ?: "",
            churchId = prefs.getString(KEY_CHURCH, "") ?: ""
        )
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_EMAIL)
            .remove(KEY_PASS)
            .remove(KEY_CHURCH)
            .putBoolean(KEY_REMEMBER, false)
            .apply()
    }

    fun isRemembered(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_REMEMBER, false)
}
