package com.example.healthsync.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _isLoggedIn = MutableStateFlow(!getToken().isNullOrBlank())
    val isLoggedInFlow: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isAdmin = MutableStateFlow(JwtUtils.isAdmin(getToken()))
    val isAdminFlow: StateFlow<Boolean> = _isAdmin.asStateFlow()

    fun saveToken(token: String) {
        prefs.edit().putString("auth_token", token).apply()
        _isLoggedIn.value = !token.isBlank()
        _isAdmin.value = JwtUtils.isAdmin(token)
    }

    fun getToken(): String? {
        return prefs.getString("auth_token", null)
    }

    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrBlank()
    }

    fun isAdmin(): Boolean {
        return JwtUtils.isAdmin(getToken())
    }

    fun clear() {
        prefs.edit().clear().apply()
        _isLoggedIn.value = false
        _isAdmin.value = false
    }
}
