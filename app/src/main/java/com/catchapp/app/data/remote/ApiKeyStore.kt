package com.catchapp.app.data.remote

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the user's own Gemini API key, encrypted at rest via the Android
 * Keystore (androidx.security.crypto). Bring-your-own-key model: pasted once
 * during onboarding (a later phase), read here on every structuring call,
 * never logged, never sent anywhere but Google's API.
 */
@Singleton
class ApiKeyStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getKey(): String? = prefs.getString(KEY_GEMINI_API_KEY, null)

    fun setKey(key: String) {
        prefs.edit().putString(KEY_GEMINI_API_KEY, key).apply()
    }

    fun clearKey() {
        prefs.edit().remove(KEY_GEMINI_API_KEY).apply()
    }

    fun hasKey(): Boolean = !getKey().isNullOrBlank()

    private companion object {
        const val PREFS_FILE = "catch_secure_prefs"
        const val KEY_GEMINI_API_KEY = "gemini_api_key"
    }
}
