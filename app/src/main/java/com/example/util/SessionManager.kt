package com.example.util

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveUserSession(identifier: String, passwordRaw: String, autoLogin: Boolean) {
        prefs.edit().apply {
            putString(KEY_IDENTIFIER, identifier.trim())
            putString(KEY_PASSWORD_RAW, passwordRaw)
            putBoolean(KEY_AUTO_LOGIN, autoLogin)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_LAST_LOGIN_TIMESTAMP, System.currentTimeMillis())
            apply()
        }
    }

    fun getSavedIdentifier(): String? = prefs.getString(KEY_IDENTIFIER, null)
    fun getSavedPassword(): String? = prefs.getString(KEY_PASSWORD_RAW, null)
    fun isAutoLoginEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_LOGIN, true)
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun clearSession() {
        prefs.edit().apply {
            remove(KEY_IDENTIFIER)
            remove(KEY_PASSWORD_RAW)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
    }

    companion object {
        private const val PREF_NAME = "employee_manager_session_prefs"
        private const val KEY_IDENTIFIER = "saved_identifier"
        private const val KEY_PASSWORD_RAW = "saved_password_raw"
        private const val KEY_AUTO_LOGIN = "auto_login_enabled"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_LAST_LOGIN_TIMESTAMP = "last_login_timestamp"
    }
}
