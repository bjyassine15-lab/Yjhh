package com.example.ai.auth

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

/**
 * High-level connection mode for Rafiqah V2.6.
 */
enum class AIConnectionMode(val label: String) {
    REAL("اتصال سحابي حقيقي بـ Gemini 🌐"),
    DEMO("محاكاة محلية / وضع غير متصل 🏠")
}

/**
 * Gemini connection and validation status for UI indicators.
 */
enum class GeminiConnectionStatus(val label: String) {
    NOT_CONFIGURED("غير مهيأ ⚪"),
    CONFIGURED("تم الحفظ (لم يُختبر) 🟡"),
    TESTING("جاري فحص الاتصال... ⏳"),
    CONNECTED("متصل 🟢"),
    FAILED("فشل الاتصال 🔴")
}

/**
 * Result of testing the Gemini connection.
 */
data class ConnectionTestResult(
    val success: Boolean,
    val message: String,
    val errorCode: String? = null
)

/**
 * Environments for Gemini authentication.
 */
enum class AuthEnvironment {
    DEVELOPMENT,
    PRODUCTION_BACKEND
}

/**
 * Clean abstraction for Gemini credentials in Rafiqah V2.6.
 * Decouples client-side services from the credential source.
 */
interface GeminiAuthProvider {
    val environment: AuthEnvironment
    val connectionMode: AIConnectionMode
    val connectionStatus: StateFlow<GeminiConnectionStatus>

    /**
     * Resolves the API key or short-lived token.
     * Guaranteed never to log or expose secrets.
     */
    suspend fun getApiKeyOrToken(): String?

    /**
     * Returns true if valid credentials can be resolved.
     */
    fun isAvailable(): Boolean

    /**
     * Returns a safe masked version of the key (e.g. ********ABCD) or null.
     */
    fun getMaskedKey(): String?

    /**
     * Tests connection against the live Gemini endpoint.
     */
    suspend fun testConnection(): ConnectionTestResult
}

/**
 * Primary dynamic implementation of [GeminiAuthProvider] for Rafiqah V2.6.
 *
 * Credential Resolution Order:
 * 1. User-entered key stored securely in [GeminiApiKeyStore].
 * 2. Development key embedded in BuildConfig (if present and not placeholder).
 * 3. None (missing credentials).
 */
class DynamicGeminiAuthProvider(
    val keyStore: GeminiApiKeyStore,
    private val connectionTester: suspend (String) -> ConnectionTestResult = { key -> testGeminiConnectionDirect(key) }
) : GeminiAuthProvider {

    override val environment: AuthEnvironment = AuthEnvironment.DEVELOPMENT

    private val _connectionStatus = MutableStateFlow(
        if (keyStore.hasApiKey()) GeminiConnectionStatus.CONFIGURED else GeminiConnectionStatus.NOT_CONFIGURED
    )
    override val connectionStatus: StateFlow<GeminiConnectionStatus> = _connectionStatus.asStateFlow()

    override val connectionMode: AIConnectionMode
        get() = if (isAvailable()) AIConnectionMode.REAL else AIConnectionMode.DEMO

    override fun isAvailable(): Boolean {
        val userKey = keyStore.getApiKey()
        if (!userKey.isNullOrBlank()) return true
        val devKey = resolveDevKey()
        return devKey.isNotBlank() && devKey != "MY_GEMINI_API_KEY"
    }

    override suspend fun getApiKeyOrToken(): String? {
        val userKey = keyStore.getApiKey()
        if (!userKey.isNullOrBlank()) return userKey
        val devKey = resolveDevKey()
        return if (devKey.isNotBlank() && devKey != "MY_GEMINI_API_KEY") devKey else null
    }

    override fun getMaskedKey(): String? {
        val userMasked = keyStore.getMaskedApiKey()
        if (userMasked != null) return userMasked

        val devKey = resolveDevKey()
        if (devKey.isNotBlank() && devKey != "MY_GEMINI_API_KEY") {
            return if (devKey.length <= 6) "******" else "********" + devKey.takeLast(4)
        }
        return null
    }

    fun updateStatus(status: GeminiConnectionStatus) {
        _connectionStatus.value = status
    }

    override suspend fun testConnection(): ConnectionTestResult {
        val key = getApiKeyOrToken()
        if (key.isNullOrBlank()) {
            _connectionStatus.value = GeminiConnectionStatus.NOT_CONFIGURED
            return ConnectionTestResult(
                success = false,
                message = "أدخل Gemini API Key أولاً من الإعدادات",
                errorCode = "API_KEY_MISSING"
            )
        }

        _connectionStatus.value = GeminiConnectionStatus.TESTING

        val result = try {
            connectionTester(key)
        } catch (e: Exception) {
            ConnectionTestResult(
                success = false,
                message = "تعذر الاتصال بـ Gemini: خطأ غير متوقع",
                errorCode = "EXCEPTION"
            )
        }

        _connectionStatus.value = if (result.success) GeminiConnectionStatus.CONNECTED else GeminiConnectionStatus.FAILED
        return result
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
 * Direct real network test for Gemini API endpoint.
 *
 * Requirements:
 * - Real lightweight network call using HttpURLConnection.
 * - Never prints or leaks the API key in logs or error messages.
 * - Does not show raw responses to the user.
 */
suspend fun testGeminiConnectionDirect(apiKey: String): ConnectionTestResult = withContext(Dispatchers.IO) {
    if (apiKey.isBlank()) {
        return@withContext ConnectionTestResult(
            success = false,
            message = "مفتاح API فارغ",
            errorCode = "API_KEY_MISSING"
        )
    }

    var connection: HttpURLConnection? = null
    try {
        val urlStr = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val url = URL(urlStr)
        connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8000
            readTimeout = 8000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }

        // Minimal lightweight prompt for testing API connectivity
        val minimalPayload = "{\"contents\":[{\"parts\":[{\"text\":\"ping\"}]}]}"
        connection.outputStream.use { os ->
            os.write(minimalPayload.toByteArray(Charsets.UTF_8))
            os.flush()
        }

        val responseCode = connection.responseCode
        when {
            responseCode in 200..299 -> {
                ConnectionTestResult(
                    success = true,
                    message = "تم الاتصال بـ Gemini بنجاح 🟢"
                )
            }
            responseCode == 400 || responseCode == 401 || responseCode == 403 -> {
                ConnectionTestResult(
                    success = false,
                    message = "تعذر الاتصال: مفتاح API غير صالح أو غير مصرح له ❌",
                    errorCode = "INVALID_API_KEY"
                )
            }
            responseCode == 429 -> {
                ConnectionTestResult(
                    success = false,
                    message = "تعذر الاتصال: تم تجاوز حد الاستخدام (Quota Exceeded) ⏳",
                    errorCode = "QUOTA_EXCEEDED"
                )
            }
            else -> {
                ConnectionTestResult(
                    success = false,
                    message = "تعذر الاتصال بـ Gemini (رمز الاستجابة: $responseCode)",
                    errorCode = "HTTP_$responseCode"
                )
            }
        }
    } catch (e: UnknownHostException) {
        ConnectionTestResult(
            success = false,
            message = "تعذر الاتصال: تحقق من اتصالك بالإنترنت 📶",
            errorCode = "NO_INTERNET"
        )
    } catch (e: SocketTimeoutException) {
        ConnectionTestResult(
            success = false,
            message = "انتهت مهلة الاتصال بالخادم، يرجى المحاولة لاحقاً ⏱️",
            errorCode = "TIMEOUT"
        )
    } catch (e: IOException) {
        ConnectionTestResult(
            success = false,
            message = "تعذر الاتصال بـ Gemini: خطأ في الشبكة",
            errorCode = "NETWORK_ERROR"
        )
    } catch (e: Exception) {
        ConnectionTestResult(
            success = false,
            message = "تعذر الاتصال بـ Gemini",
            errorCode = "UNKNOWN_ERROR"
        )
    } finally {
        connection?.disconnect()
    }
}

/**
 * Legacy Development implementation of [GeminiAuthProvider].
 */
class DevelopmentGeminiAuthProvider(
    private val keyStore: GeminiApiKeyStore = InMemoryGeminiApiKeyStore()
) : GeminiAuthProvider {

    private val delegate = DynamicGeminiAuthProvider(keyStore)

    override val environment: AuthEnvironment = AuthEnvironment.DEVELOPMENT
    override val connectionMode: AIConnectionMode get() = delegate.connectionMode
    override val connectionStatus: StateFlow<GeminiConnectionStatus> get() = delegate.connectionStatus

    override fun isAvailable(): Boolean = delegate.isAvailable()
    override suspend fun getApiKeyOrToken(): String? = delegate.getApiKeyOrToken()
    override fun getMaskedKey(): String? = delegate.getMaskedKey()
    override suspend fun testConnection(): ConnectionTestResult = delegate.testConnection()
}

/**
 * Production Backend Auth Provider (Contract for V3+).
 */
class BackendGeminiAuthProvider(
    private val backendTokenUrl: String = ""
) : GeminiAuthProvider {
    override val environment: AuthEnvironment = AuthEnvironment.PRODUCTION_BACKEND

    private val _status = MutableStateFlow(GeminiConnectionStatus.NOT_CONFIGURED)
    override val connectionStatus: StateFlow<GeminiConnectionStatus> = _status.asStateFlow()

    override val connectionMode: AIConnectionMode
        get() = if (isAvailable()) AIConnectionMode.REAL else AIConnectionMode.DEMO

    override fun isAvailable(): Boolean = backendTokenUrl.isNotBlank()

    override suspend fun getApiKeyOrToken(): String? = null

    override fun getMaskedKey(): String? = null

    override suspend fun testConnection(): ConnectionTestResult {
        return ConnectionTestResult(false, "خادم الإنتاج غير مهيأ بعد", "NOT_CONFIGURED")
    }
}

typealias ProductionGeminiAuthProvider = BackendGeminiAuthProvider
