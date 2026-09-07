package com.example.ai.auth

import android.util.Log

/**
 * Connection modes for Rafiqah V2.5.
 */
enum class AIConnectionMode(val label: String) {
    REAL("اتصال سحابي حقيقي بـ Gemini 🌐"),
    DEMO("محاكاة محلية / وضع غير متصل 🏠")
}

/**
 * Environments for Gemini authentication.
 */
enum class AuthEnvironment {
    DEVELOPMENT,
    PRODUCTION_BACKEND
}

/**
 * Clean abstraction for Gemini authentication in Rafiqah V2.5.
 * Separates client-side logic from the source of API credentials.
 */
interface GeminiAuthProvider {
    val environment: AuthEnvironment
    val connectionMode: AIConnectionMode

    /**
     * Resolves the API key or short-lived token.
     * Guaranteed never to log or expose secrets.
     */
    suspend fun getApiKeyOrToken(): String?

    /**
     * Returns true if valid credentials can be resolved.
     */
    fun isAvailable(): Boolean
}

/**
 * Development implementation of [GeminiAuthProvider].
 * Safely reads the development key from BuildConfig via reflection.
 *
 * NOTE: For production release, this must be replaced with a secure backend
 * authentication provider that issues short-lived ephemeral session tokens.
 */
class DevelopmentGeminiAuthProvider : GeminiAuthProvider {
    private val tag = "RafiqahAI"

    override val environment: AuthEnvironment = AuthEnvironment.DEVELOPMENT

    override val connectionMode: AIConnectionMode
        get() = if (isAvailable()) AIConnectionMode.REAL else AIConnectionMode.DEMO

    override fun isAvailable(): Boolean {
        val key = resolveDevKey()
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    override suspend fun getApiKeyOrToken(): String? {
        val key = resolveDevKey()
        return if (key.isNotBlank() && key != "MY_GEMINI_API_KEY") key else null
    }

    private fun resolveDevKey(): String {
        return try {
            val clazz = Class.forName("com.example.BuildConfig")
            val field = clazz.getField("GEMINI_API_KEY")
            val key = field.get(null) as? String ?: ""
            key.trim()
        } catch (_: Exception) {
            ""
        }
    }
}

/**
 * Production Backend Auth Provider (Contract for V3+).
 *
 * TODO [PRODUCTION_BLOCKER]:
 * In production releases, do NOT embed long-lived API keys in the client APK.
 * The production client should request a short-lived ephemeral session token from
 * a secure cloud backend (e.g. Cloud Run / Firebase App Check authenticated endpoint).
 */
class BackendGeminiAuthProvider(
    private val backendTokenUrl: String = ""
) : GeminiAuthProvider {
    override val environment: AuthEnvironment = AuthEnvironment.PRODUCTION_BACKEND

    override val connectionMode: AIConnectionMode
        get() = if (isAvailable()) AIConnectionMode.REAL else AIConnectionMode.DEMO

    override fun isAvailable(): Boolean = backendTokenUrl.isNotBlank()

    override suspend fun getApiKeyOrToken(): String? {
        // TODO: Call secure backend to fetch ephemeral token
        return null
    }
}

typealias ProductionGeminiAuthProvider = BackendGeminiAuthProvider
