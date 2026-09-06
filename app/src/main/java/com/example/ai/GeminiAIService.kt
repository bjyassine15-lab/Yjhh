package com.example.ai

import android.util.Log
import com.example.ai.tools.AIToolRegistry
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
    val isUrgentMedical: Boolean = false
)

/**
 * Production Gemini AI Service for Rafiqah V2.1.
 * Supports:
 * 1. Text chat and system persona with gemini-3.5-flash.
 * 2. Official Gemini Function Calling via JSON schemas.
 * 3. Multi-turn tool execution (sending functionResponse back for final answer).
 * 4. Graceful offline fallback to [MockAIService] when offline or key is unset.
 */
class GeminiAIService(
    private val fallbackService: AIService = MockAIService()
) : AIService {

    private val tag = "GeminiAIService"

    private fun resolveApiKey(): String {
        return try {
            val clazz = Class.forName("com.example.BuildConfig")
            val field = clazz.getField("GEMINI_API_KEY")
            val key = field.get(null) as? String ?: ""
            if (key == "MY_GEMINI_API_KEY" || key.isBlank()) "" else key
        } catch (_: Exception) {
            ""
        }
    }

    override suspend fun sendVoiceMessage(
        userSpeech: String,
        userProfileSummary: String,
        recentMemories: List<String>
    ): AIResponse {
        val apiKey = resolveApiKey()
        if (apiKey.isBlank()) {
            return fallbackService.sendVoiceMessage(userSpeech, userProfileSummary, recentMemories)
        }

        return withContext(Dispatchers.IO) {
            try {
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
                    - إذا كان عارضاً صحياً مقلقاً، انصحي بالطبيب دون تشخيص أو دواء.
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

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = readResponse(conn)
                    val turnResult = parseTurnResult(responseStr)

                    AIResponse(
                        replyText = turnResult.replyText,
                        spokenDialectText = turnResult.replyText,
                        isUrgentMedicalNotice = turnResult.isUrgentMedical
                    )
                } else {
                    Log.w(tag, "Gemini returned ${conn.responseCode}, using fallback service")
                    fallbackService.sendVoiceMessage(userSpeech, userProfileSummary, recentMemories)
                }
            } catch (e: Exception) {
                Log.w(tag, "Gemini call failed: ${e.message}, safely using fallback")
                fallbackService.sendVoiceMessage(userSpeech, userProfileSummary, recentMemories)
            }
        }
    }

    /**
     * Executes the second turn of Function Calling: sends tool result back to Gemini for the final answer.
     */
    suspend fun sendToolResultFollowUp(
        userSpeech: String,
        functionName: String,
        toolResultString: String
    ): String {
        val apiKey = resolveApiKey()
        if (apiKey.isBlank()) return toolResultString

        return withContext(Dispatchers.IO) {
            try {
                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/${AIConfig.GEMINI_DEFAULT_TEXT_MODEL}:generateContent?key=$apiKey"
                val conn = openPostConnection(endpoint)

                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        // User message
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", userSpeech) })
                            })
                        })
                        // Model function call turn
                        put(JSONObject().apply {
                            put("role", "model")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("functionCall", JSONObject().apply {
                                        put("name", functionName)
                                        put("args", JSONObject())
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
                    toolResultString
                }
            } catch (e: Exception) {
                Log.w(tag, "Follow up failed: ${e.message}")
                toolResultString
            }
        }
    }

    private fun openPostConnection(endpoint: String): HttpURLConnection {
        return (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            doOutput = true
            connectTimeout = 8000
            readTimeout = 8000
        }
    }

    private fun writeJsonToConnection(conn: HttpURLConnection, json: JSONObject) {
        OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
    }

    private fun readResponse(conn: HttpURLConnection): String {
        return BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
    }

    private fun parseTurnResult(responseStr: String): GeminiTurnResult {
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

        return GeminiTurnResult(
            replyText = replyText.trim(),
            functionCallName = fnName,
            functionCallArgs = fnArgs,
            isUrgentMedical = replyText.contains("190") || replyText.contains("استعجالي")
        )
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
