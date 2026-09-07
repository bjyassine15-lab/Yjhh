package com.example.ai.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Storage abstraction for user-provided Gemini API Key in Rafiqah V2.6.
 *
 * Security Architecture Note:
 * On physical Android devices, this store uses AndroidX [EncryptedSharedPreferences]
 * backed by the hardware Android KeyStore (AES256-SIV for keys, AES256-GCM for values).
 *
 * IMPORTANT SECURITY LIMITATION:
 * This client-side secure store is designed for personal use, development, and prototyping.
 * A direct device-to-Gemini API key is not enterprise-grade secret protection against
 * rooted device inspection. For future production releases (V3+), [GeminiAuthProvider]
 * is architected to be swapped with a backend ephemeral token provider without altering
 * application business logic.
 */
interface GeminiApiKeyStore {
    fun saveApiKey(key: String)
    fun getApiKey(): String?
    fun clearApiKey()
    fun hasApiKey(): Boolean
    fun getMaskedApiKey(): String?
}

/**
 * Hardware/Encrypted implementation using AndroidX Security.
 * Falls back gracefully to private SharedPreferences if Android KeyStore is unavailable
 * (e.g. during Robolectric JVM testing or on unsupported devices) without crashing.
 */
class EncryptedGeminiApiKeyStore(
    context: Context,
    prefsName: String = "rafiqah_secure_ai_prefs"
) : GeminiApiKeyStore {

    private val prefs: SharedPreferences = createEncryptedOrFallbackPrefs(context, prefsName)

    companion object {
        private const val KEY_GEMINI_API_KEY = "gemini_api_key_v26"
        private const val TAG = "RafiqahSecurity"

        private fun createEncryptedOrFallbackPrefs(context: Context, prefsName: String): SharedPreferences {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context,
                    prefsName,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                // In Robolectric or devices lacking Android KeyStore hardware provider,
                // securely fall back to private mode preferences without crashing
                Log.w(TAG, "EncryptedSharedPreferences unavailable (${e.javaClass.simpleName}), falling back to private storage.")
                context.getSharedPreferences(prefsName + "_fallback", Context.MODE_PRIVATE)
            }
        }
    }

    override fun saveApiKey(key: String) {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) {
            clearApiKey()
            return
        }
        // Do NOT log the API key!
        prefs.edit().putString(KEY_GEMINI_API_KEY, trimmed).apply()
    }

    override fun getApiKey(): String? {
        val key = prefs.getString(KEY_GEMINI_API_KEY, null)?.trim()
        return if (!key.isNullOrBlank()) key else null
    }

    override fun clearApiKey() {
        prefs.edit().remove(KEY_GEMINI_API_KEY).apply()
    }

    override fun hasApiKey(): Boolean {
        return !getApiKey().isNullOrBlank()
    }

    override fun getMaskedApiKey(): String? {
        val key = getApiKey() ?: return null
        if (key.length <= 6) {
            return "******"
        }
        val lastChars = key.takeLast(4)
        return "********$lastChars"
    }
}

/**
 * In-memory implementation of [GeminiApiKeyStore] for fast, deterministic unit testing.
 */
class InMemoryGeminiApiKeyStore(initialKey: String? = null) : GeminiApiKeyStore {
    private var storedKey: String? = initialKey?.trim()?.ifBlank { null }

    override fun saveApiKey(key: String) {
        storedKey = key.trim().ifBlank { null }
    }

    override fun getApiKey(): String? = storedKey

    override fun clearApiKey() {
        storedKey = null
    }

    override fun hasApiKey(): Boolean = !storedKey.isNullOrBlank()

    override fun getMaskedApiKey(): String? {
        val key = storedKey ?: return null
        if (key.length <= 6) return "******"
        return "********" + key.takeLast(4)
    }
}
