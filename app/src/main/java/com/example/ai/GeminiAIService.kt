package com.example.ai

import android.util.Log
import com.example.ai.auth.AIConnectionMode
import com.example.ai.auth.DevelopmentGeminiAuthProvider
import com.example.ai.auth.GeminiAuthProvider
import com.example.ai.tools.AIToolRegistry
import com.example.ai.tools.ToolExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Result of a Gemini AI turn, which can either be a direct response or a function call request.
 */
data class GeminiTurnResult(
    val replyText: String,
    val functionCallName: String? = null,
    val functionCallArgs: Map<String, Any?> = emptyMap(),
    val isUrgentMedical: Boolean = false,
    val requiresConfirmation: Boolean = false
)

/**
 * Production Gemini AI Service for Rafiqah V2.5.
 *
 * Supports:
 * 1. Text chat and system persona using [AIConfig.GEMINI_DEFAULT_TEXT_MODEL].
 * 2. Official Gemini Function Calling via JSON schemas.
 * 3. Complete Two-Turn Function Calling cycle:
 *    Model functionCall -> Local Tool Execution -> functionResponse with original args -> Model Final Reply.
 * 4. Tool confirmation enforcement for sensitive operations.
 * 5. Clean authentication abstraction via [GeminiAuthProvider].
 * 6. Resilient error handling (network timeout, HTTP errors, malformed response) with fallback to [MockAIService].
 */
class GeminiAIService(
    private val authProvider: GeminiAuthProvider = DevelopmentGeminiAuthProvider(),
    private val fallbackService: AIService = MockAIService(),
    var toolExecutor: ToolExecutor? = null
) : AIService {

    private val tag = "RafiqahAI"

    val connectionMode: AIConnectionMode
        get() = authProvider.connectionMode

    override suspend fun sendVoiceMessage(
        userSpeech: String,
        userProfileSummary: String,
        recentMemories: List<String>
    ): AIResponse {
        val apiKey = authProvider.getApiKeyOrToken()
        if (apiKey.isNullOrBlank()) {
            Log.i(tag, "[$connectionMode] Gemini auth credentials not configured. Notifying user to configure in Settings.")
            return AIResponse(
                replyText = "أهلاً يا أمي الحبيبة 🌷 لتشغيل رفيقة بذكاء Gemini، يرجى إضافة مفتاح Gemini API من شاشة الإعدادات ⚙️",
                spokenDialectText = "على سلامتك يا أمي، لتشغيل رفيقة مع Gemini يرجى إضافة المفتاح من الإعدادات.",
                isUrgentMedicalNotice = false
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "[$connectionMode] Calling Gemini text API with model ${AIConfig.GEMINI_DEFAULT_TEXT_MODEL}")
                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/${AIConfig.GEMINI_DEFAULT_TEXT_MODEL}:generateContent?key=$apiKey"
                val conn = openPostConnection(endpoint)

                val prompt = """
                    ${AIConfig.SYSTEM_PERSONA_PROMPT}

                    [سياق أمي الموثق لهذا الطلب]:
                    $userProfileSummary

                    [أهم الذكريات المسجلة]:
                    ${recentMemories.joinToString("\n") { "- $it" }}

                    كلام أمي:
                    "$userSpeech"

                    التعليمات:
                    - أجيبي بالتونسي الأبيض الدافئ والمحترم.
                    - إذا كان الطلب يتطلب أداة (مثل جلب برنامج اليوم، حفظ موعد، إحضار الكلمات الفرنسية، إلخ)، استدعي الأداة المناسبة.
                    - إذا كان عارضاً صحياً مقلقاً، انصحي بالطبيب بهدوء دون تشخيص أو دواء.
                """.trimIndent()

                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", prompt) })
                            })
                        })
                    })
                    put("tools", AIToolRegistry.getGeminiToolsDeclarationJson())
                }

                writeJsonToConnection(conn, requestJson)

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = readResponse(conn)
                    val turnResult = parseTurnResult(responseStr)

                    // Check for function call
                    if (!turnResult.functionCallName.isNullOrBlank()) {
                        val fnName = turnResult.functionCallName
                        val fnArgs = turnResult.functionCallArgs

                        Log.i(tag, "[$connectionMode] Function call received from Gemini: $fnName with args keys: ${fnArgs.keys}")

                        val executor = toolExecutor
                        if (executor != null) {
                            if (executor.isConfirmationRequired(fnName, fnArgs)) {
                                val confirmMsg = executor.getConfirmationMessage(fnName, fnArgs)
                                return@withContext AIResponse(
                                    replyText = confirmMsg,
                                    spokenDialectText = confirmMsg,
                                    isUrgentMedicalNotice = false
                                )
                            } else {
                                // Execute tool and send second turn follow-up to Gemini
                                val toolResult = executor.executeTool(fnName, fnArgs)
                                val finalReply = sendToolResultFollowUp(
                                    userSpeech = userSpeech,
                                    functionName = fnName,
                                    originalArgs = fnArgs,
                                    toolResultString = toolResult
                                )
                                return@withContext AIResponse(
                                    replyText = finalReply,
                                    spokenDialectText = finalReply,
                                    isUrgentMedicalNotice = false
                                )
                            }
                        }
                    }

                    AIResponse(
                        replyText = turnResult.replyText,
                        spokenDialectText = turnResult.replyText,
                        isUrgentMedicalNotice = turnResult.isUrgentMedical
                    )
                } else {
                    Log.w(tag, "[$connectionMode] Gemini API returned HTTP $responseCode")
                    val errorMsg = when (responseCode) {
                        400, 401, 403 -> "تعذر الاتصال: مفتاح Gemini API غير صالح أو غير مصرح له. يرجى مراجعته في الإعدادات ⚙️"
                        429 -> "تم تجاوز حد استخدام Gemini (Quota Exceeded). يرجى الانتظار دقيقة والمحاولة مجدداً ⏳"
                        else -> "تعذر الاتصال بـ Gemini (رمز الخطأ: $responseCode)."
                    }
                    AIResponse(
                        replyText = errorMsg,
                        spokenDialectText = "يا أمي تعذر الاتصال بـ Gemini، ثبت في الإعدادات يعيشك.",
                        isUrgentMedicalNotice = false
                    )
                }
            } catch (e: Exception) {
                Log.w(tag, "[$connectionMode] Gemini API call error: ${e.javaClass.simpleName}")
                val errorMsg = when (e) {
                    is java.net.UnknownHostException -> "تعذر الاتصال بـ Gemini: يرجى التحقق من اتصال الإنترنت 📶"
                    is java.net.SocketTimeoutException -> "انتهت مهلة الاتصال بـ Gemini، يرجى المحاولة مجدداً ⏱️"
                    else -> "تعذر الاتصال بـ Gemini: يرجى التحقق من الشبكة والإعدادات."
                }
                AIResponse(
                    replyText = errorMsg,
                    spokenDialectText = "يا أمي فما مشكلة في الإنترنت، ثبت في الاتصال يعيشك.",
                    isUrgentMedicalNotice = false
                )
            }
        }
    }

    /**
     * Executes the second turn of Function Calling: sends functionResponse back to Gemini for the final natural language answer.
     * Preserves original tool name and arguments.
     */
    suspend fun sendToolResultFollowUp(
        userSpeech: String,
        functionName: String,
        originalArgs: Map<String, Any?>,
        toolResultString: String
    ): String {
        val apiKey = authProvider.getApiKeyOrToken()
        if (apiKey.isNullOrBlank()) return toolResultString

        return withContext(Dispatchers.IO) {
            try {
                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/${AIConfig.GEMINI_DEFAULT_TEXT_MODEL}:generateContent?key=$apiKey"
                val conn = openPostConnection(endpoint)

                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        // User message turn
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", userSpeech) })
                            })
                        })
                        // Model function call turn (preserving original arguments)
                        put(JSONObject().apply {
                            put("role", "model")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("functionCall", JSONObject().apply {
                                        put("name", functionName)
                                        put("args", JSONObject(originalArgs))
                                    })
                                })
                            })
                        })
                        // Function response turn
                        put(JSONObject().apply {
                            put("role", "function")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("functionResponse", JSONObject().apply {
                                        put("name", functionName)
                                        put("response", JSONObject().apply {
                                            put("output", toolResultString)
                                        })
                                    })
                                })
                            })
                        })
                    })
                }

                writeJsonToConnection(conn, requestJson)

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val resp = readResponse(conn)
                    val turn = parseTurnResult(resp)
                    if (turn.replyText.isNotBlank()) turn.replyText else toolResultString
                } else {
                    Log.w(tag, "Follow up HTTP ${conn.responseCode}")
                    toolResultString
                }
            } catch (e: Exception) {
                Log.w(tag, "Follow up failed: ${e.javaClass.simpleName}")
                toolResultString
            }
        }
    }

    private fun openPostConnection(endpoint: String): HttpURLConnection {
        return (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            doOutput = true
            connectTimeout = 10000
            readTimeout = 15000
        }
    }

    private fun writeJsonToConnection(conn: HttpURLConnection, json: JSONObject) {
        OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
    }

    private fun readResponse(conn: HttpURLConnection): String {
        return BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
    }

    fun parseTurnResult(responseStr: String): GeminiTurnResult {
        return try {
            val root = JSONObject(responseStr)
            val candidate = root.optJSONArray("candidates")?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            var replyText = ""
            var fnName: String? = null
            val fnArgs = mutableMapOf<String, Any?>()

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i) ?: continue
                    if (part.has("text")) {
                        replyText += part.optString("text")
                    }
                    if (part.has("functionCall")) {
                        val fn = part.getJSONObject("functionCall")
                        fnName = fn.optString("name")
                        val argsObj = fn.optJSONObject("args")
                        if (argsObj != null) {
                            val it = argsObj.keys()
                            while (it.hasNext()) {
                                val k = it.next()
                                fnArgs[k] = argsObj.get(k)
                            }
                        }
                    }
                }
            }

            GeminiTurnResult(
                replyText = replyText.trim(),
                functionCallName = fnName,
                functionCallArgs = fnArgs,
                isUrgentMedical = replyText.contains("190") || replyText.contains("استعجالي")
            )
        } catch (e: Exception) {
            Log.w(tag, "Failed to parse Gemini response: ${e.message}")
            GeminiTurnResult(replyText = "")
        }
    }

    override suspend fun getProgressiveConceptExplanation(conceptKey: String, level: Int): String {
        return fallbackService.getProgressiveConceptExplanation(conceptKey, level)
    }

    override suspend fun generateDailyGreeting(motherName: String, timeOfDay: String, lastLessonTitle: String): String {
        return fallbackService.generateDailyGreeting(motherName, timeOfDay, lastLessonTitle)
    }

    override suspend fun evaluateAnswer(questionKey: String, selectedOptionIndex: Int): ComprehensionResult {
        return fallbackService.evaluateAnswer(questionKey, selectedOptionIndex)
    }
}
