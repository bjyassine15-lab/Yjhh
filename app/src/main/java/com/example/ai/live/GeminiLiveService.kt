package com.example.ai.live

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.ai.AIConfig
import com.example.ai.VoiceService
import com.example.ai.auth.DevelopmentGeminiAuthProvider
import com.example.ai.auth.GeminiAuthProvider
import com.example.ai.live.audio.AudioInput
import com.example.ai.live.audio.AudioOutput
import com.example.ai.live.audio.HardwarePcmAudioInput
import com.example.ai.live.audio.HardwarePcmAudioOutput
import com.example.ai.tools.AIToolRegistry
import com.example.ai.tools.ToolAccessLevel
import com.example.ai.tools.ToolExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Connection states for Gemini Live Voice in Rafiqah V2.5.
 */
enum class LiveSessionState(val arabicLabel: String) {
    IDLE("جاهزة للمحادثة 🌷"),
    CONNECTING("جاري الاتصال بالسحابة... 🔄"),
    LISTENING("نسمع فيك... 🎙️"),
    THINKING("نفكر... 🧠"),
    SPEAKING("نحكي معاك... 🔊"),
    INTERRUPTED("نسمع فيك يا أمي... 👂"),
    RECONNECTING("جاري إعادة الاتصال... 🔄"),
    DISCONNECTED("انقطع الاتصال... ⚠️"),
    ERROR("تعذر الاتصال بالصوت الحي ❌")
}

data class LiveToolCallEvent(
    val callId: String,
    val functionName: String,
    val arguments: Map<String, Any?>
)

data class LiveToolConfirmation(
    val callId: String,
    val functionName: String,
    val arguments: Map<String, Any?>,
    val title: String,
    val description: String,
    val onConfirm: suspend () -> Unit = {},
    val onReject: suspend () -> Unit = {}
)

data class ToolResponseData(
    val callId: String,
    val functionName: String,
    val result: String
)

/**
 * Production Gemini Live Service for Rafiqah V2.5.
 * Connects to Gemini Live Bidi Streaming API (gemini-3.1-flash-live-preview)
 * using WebSockets over OkHttp.
 *
 * Real Bidirectional Audio Architecture:
 * 1. Microphone -> AudioInput (AudioRecord 16kHz PCM Mono) -> realtimeInput mediaChunks.
 * 2. Server Audio (24kHz PCM) -> AudioOutput (AudioTrack) -> Speaker.
 * 3. Instant Interruption / Barge-in: instant audio track flush + state switch to LISTENING.
 * 4. Two-way Live Tool Calling: functionCall -> ToolExecutor -> toolResponse WebSocket event.
 * 5. Bounded reconnection with exponential backoff (max 3 retries).
 */
class GeminiLiveService(
    private val context: Context,
    private val fallbackVoiceService: VoiceService,
    private val authProvider: GeminiAuthProvider = DevelopmentGeminiAuthProvider(),
    var audioInput: AudioInput = HardwarePcmAudioInput(context),
    var audioOutput: AudioOutput = HardwarePcmAudioOutput(24000),
    var toolExecutor: ToolExecutor? = null
) {
    private val tag = "RafiqahLive"
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _sessionState = MutableStateFlow(LiveSessionState.IDLE)
    val sessionState: StateFlow<LiveSessionState> = _sessionState.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _toolCallEvents = MutableSharedFlow<LiveToolCallEvent>()
    val toolCallEvents: SharedFlow<LiveToolCallEvent> = _toolCallEvents.asSharedFlow()

    val pendingConfirmationsMap = ConcurrentHashMap<String, LiveToolConfirmation>()

    private val _pendingToolConfirmation = MutableStateFlow<LiveToolConfirmation?>(null)
    val pendingToolConfirmation: StateFlow<LiveToolConfirmation?> = _pendingToolConfirmation.asStateFlow()

    private val _toolConfirmationEvents = MutableSharedFlow<LiveToolConfirmation>(replay = 1)
    val toolConfirmationEvents: SharedFlow<LiveToolConfirmation> = _toolConfirmationEvents.asSharedFlow()

    var lastSentToolResult: ToolResponseData? = null
        private set

    private var webSocket: WebSocket? = null
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Keep-alive for streaming
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private var isUsingFallback = false
    private var mockJob: Job? = null
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 3

    /**
     * Initiates the Live Voice session.
     */
    fun startLiveSession(onGreetingSpoken: () -> Unit = {}) {
        scope.launch {
            val apiKey = authProvider.getApiKeyOrToken()
            if (apiKey.isNullOrBlank()) {
                Log.i(tag, "No Gemini API key resolved. Informing user to set API key in settings.")
                _sessionState.value = LiveSessionState.ERROR
                _liveTranscript.value = "يرجى إدخال وحفظ مفتاح Gemini API من شاشة الإعدادات ⚙️ لتشغيل المحادثة الصوتية الحية."
                fallbackVoiceService.speak("يا أمي، يرجى إضافة مفتاح Gemini من الإعدادات لتشغيل الصوت الحي.", "ar")
                return@launch
            }

            _sessionState.value = LiveSessionState.CONNECTING
            isUsingFallback = false

            val wsUrl = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"
            val request = Request.Builder().url(wsUrl).build()

            webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    Log.i(tag, "Gemini Live WebSocket opened successfully.")
                    reconnectAttempts = 0
                    scope.launch {
                        _sessionState.value = LiveSessionState.LISTENING
                        sendSetupMessage(ws)
                        startMicrophoneCapture()
                        onGreetingSpoken()
                    }
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    handleServerMessage(text)
                }

                override fun onMessage(ws: WebSocket, bytes: ByteString) {
                    // Server binary PCM audio frame
                    scope.launch {
                        _sessionState.value = LiveSessionState.SPEAKING
                        audioOutput.playPcmChunk(bytes.toByteArray())
                    }
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    Log.w(tag, "Live WebSocket error: ${t.javaClass.simpleName} - ${t.message}")
                    scope.launch {
                        handleConnectionFailure(onGreetingSpoken)
                    }
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    Log.i(tag, "Live WebSocket closed ($code: $reason)")
                    scope.launch {
                        stopMicrophoneCapture()
                        audioOutput.stopAndFlush()
                        if (_sessionState.value != LiveSessionState.ERROR) {
                            _sessionState.value = LiveSessionState.IDLE
                        }
                    }
                }
            })
        }
    }

    private fun handleConnectionFailure(onGreetingSpoken: () -> Unit) {
        stopMicrophoneCapture()
        audioOutput.stopAndFlush()

        if (reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++
            _sessionState.value = LiveSessionState.RECONNECTING
            val backoffMs = (1000L * (1 shl (reconnectAttempts - 1)))
            Log.i(tag, "Attempting reconnect $reconnectAttempts/$maxReconnectAttempts after ${backoffMs}ms")
            scope.launch {
                delay(backoffMs)
                startLiveSession(onGreetingSpoken)
            }
        } else {
            Log.w(tag, "Max reconnect attempts reached. Live session failed.")
            _sessionState.value = LiveSessionState.ERROR
            _liveTranscript.value = "تعذر الاتصال بـ Gemini Live. يرجى التحقق من المفتاح والإنترنت من الإعدادات ⚙️"
            fallbackVoiceService.speak("يا أمي تعذر الاتصال بـ Gemini، ثبت في المفتاح والإنترنت في الإعدادات.", "ar")
        }
    }

    private fun sendSetupMessage(ws: WebSocket) {
        val setupJson = JSONObject().apply {
            val setupObj = JSONObject().apply {
                put("model", "models/${AIConfig.GEMINI_LIVE_VOICE_MODEL}")
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().apply { put("AUDIO"); put("TEXT") })
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", "Aoede")
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", AIConfig.SYSTEM_PERSONA_PROMPT)
                        })
                    })
                })
                put("tools", AIToolRegistry.getGeminiToolsDeclarationJson())
            }
            put("setup", setupObj)
        }
        ws.send(setupJson.toString())
    }

    /**
     * Begins capturing microphone audio via [AudioInput] and streaming it to Gemini Live.
     */
    fun startMicrophoneCapture() {
        val success = audioInput.startRecording { pcm16Chunk ->
            // If the model was speaking and user speaks, trigger instant barge-in
            if (_sessionState.value == LiveSessionState.SPEAKING) {
                scope.launch {
                    interruptPlayback()
                }
            }
            sendAudioChunk(pcm16Chunk)
        }
        if (success) {
            Log.i(tag, "Microphone capture active and streaming to Gemini Live.")
        }
    }

    /**
     * Stops capturing microphone audio.
     */
    fun stopMicrophoneCapture() {
        audioInput.stopRecording()
    }

    /**
     * Sends microphone PCM audio chunk to the Live API over WebSocket.
     */
    fun sendAudioChunk(pcm16Data: ByteArray) {
        if (isUsingFallback || webSocket == null) return

        val base64Data = Base64.encodeToString(pcm16Data, Base64.NO_WRAP)
        val realtimeMessage = JSONObject().apply {
            put("realtimeInput", JSONObject().apply {
                put("mediaChunks", JSONArray().apply {
                    put(JSONObject().apply {
                        put("mimeType", "audio/pcm;rate=16000")
                        put("data", base64Data)
                    })
                })
            })
        }
        webSocket?.send(realtimeMessage.toString())
    }

    /**
     * Sends user text in Live session.
     */
    fun sendTextMessage(userText: String) {
        if (isUsingFallback) {
            handleMockUserSpeech(userText)
            return
        }

        interruptPlayback()
        _sessionState.value = LiveSessionState.THINKING
        _liveTranscript.value = "أمي: $userText"

        val clientContent = JSONObject().apply {
            put("clientContent", JSONObject().apply {
                put("turns", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", userText)
                            })
                        })
                    })
                })
                put("turnComplete", true)
            })
        }
        webSocket?.send(clientContent.toString())
    }

    private fun handleServerMessage(text: String) {
        try {
            val json = JSONObject(text)
            val serverContent = json.optJSONObject("serverContent")

            if (serverContent != null) {
                // Interruption notification from server
                if (serverContent.optBoolean("interrupted", false)) {
                    scope.launch {
                        audioOutput.stopAndFlush()
                        _sessionState.value = LiveSessionState.INTERRUPTED
                        delay(150)
                        _sessionState.value = LiveSessionState.LISTENING
                    }
                    return
                }

                val modelTurn = serverContent.optJSONObject("modelTurn")
                val parts = modelTurn?.optJSONArray("parts")
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val part = parts.optJSONObject(i) ?: continue

                        // 1. Text transcript part
                        val textPart = part.optString("text")
                        if (textPart.isNotBlank()) {
                            scope.launch {
                                _sessionState.value = LiveSessionState.SPEAKING
                                _liveTranscript.value = "رفيقة: $textPart"
                            }
                        }

                        // 2. Audio PCM inlineData part
                        val inlineData = part.optJSONObject("inlineData")
                        if (inlineData != null) {
                            val dataBase64 = inlineData.optString("data")
                            if (dataBase64.isNotBlank()) {
                                try {
                                    val pcmBytes = Base64.decode(dataBase64, Base64.DEFAULT)
                                    scope.launch {
                                        _sessionState.value = LiveSessionState.SPEAKING
                                        audioOutput.playPcmChunk(pcmBytes)
                                    }
                                } catch (e: Exception) {
                                    Log.w(tag, "Error decoding PCM audio chunk: ${e.message}")
                                }
                            }
                        }

                        // 3. Tool call part
                        val functionCall = part.optJSONObject("functionCall")
                        if (functionCall != null) {
                            val name = functionCall.optString("name")
                            val id = functionCall.optString("id", System.currentTimeMillis().toString())
                            val argsObj = functionCall.optJSONObject("args") ?: JSONObject()
                            val argsMap = mutableMapOf<String, Any?>()
                            val keys = argsObj.keys()
                            while (keys.hasNext()) {
                                val k = keys.next()
                                argsMap[k] = argsObj.get(k)
                            }

                            scope.launch {
                                handleFunctionCall(id, name, argsMap)
                            }
                        }
                    }
                }

                if (serverContent.optBoolean("turnComplete", false)) {
                    scope.launch {
                        delay(400)
                        if (_sessionState.value == LiveSessionState.SPEAKING) {
                            _sessionState.value = LiveSessionState.LISTENING
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error parsing server message: ${e.message}")
        }
    }

    /**
     * Dispatches function call received from Gemini Live API.
     * Evaluates tool access level and security policy:
     * - READ_TOOL: executed directly without confirmation.
     * - SAFE_WRITE: executed directly per existing safety policy.
     * - SENSITIVE_WRITE: pauses execution without running the tool, issuing LiveToolConfirmation for user approval.
     */
    suspend fun handleFunctionCall(callId: String, functionName: String, arguments: Map<String, Any?>) {
        _toolCallEvents.emit(LiveToolCallEvent(callId, functionName, arguments))

        val toolDef = AIToolRegistry.TOOLS.find { it.name == functionName }
        val accessLevel = toolDef?.accessLevel ?: ToolAccessLevel.READ_TOOL
        val executor = toolExecutor

        val requiresConfirmation = if (executor != null) {
            executor.isConfirmationRequired(functionName, arguments)
        } else {
            accessLevel == ToolAccessLevel.SENSITIVE_WRITE
        }

        when {
            // 1. READ_TOOL executes automatically
            accessLevel == ToolAccessLevel.READ_TOOL && !requiresConfirmation -> {
                Log.d(tag, "Executing READ_TOOL directly: $functionName")
                executor?.let { exec ->
                    val result = exec.executeTool(functionName, arguments)
                    sendToolResult(callId, functionName, result)
                }
            }

            // 2. SAFE_WRITE follows existing policy
            accessLevel == ToolAccessLevel.SAFE_WRITE && !requiresConfirmation -> {
                Log.d(tag, "Executing SAFE_WRITE directly: $functionName")
                executor?.let { exec ->
                    val result = exec.executeTool(functionName, arguments)
                    sendToolResult(callId, functionName, result)
                }
            }

            // 3. SENSITIVE_WRITE pauses for confirmation
            else -> {
                Log.i(tag, "SENSITIVE_WRITE intercepted for user confirmation: $functionName ($callId)")
                val title = when (functionName) {
                    "add_daily_task" -> "تأكيد موعد أو مهمة"
                    "delete_memory" -> "تأكيد حذف من الذاكرة"
                    "save_health_note" -> "تأكيد تسجيل ملاحظة صحية"
                    else -> "تأكيد إجراء حساس"
                }
                val description = executor?.getConfirmationMessage(functionName, arguments)
                    ?: "تحبي نأكد هذا الإجراء يا أمي؟"

                val confirmation = LiveToolConfirmation(
                    callId = callId,
                    functionName = functionName,
                    arguments = arguments,
                    title = title,
                    description = description,
                    onConfirm = { confirmLiveTool(callId) },
                    onReject = { rejectLiveTool(callId) }
                )

                pendingConfirmationsMap[callId] = confirmation
                _pendingToolConfirmation.value = confirmation
                _toolConfirmationEvents.emit(confirmation)
            }
        }
    }

    /**
     * Confirms and executes a pending sensitive live tool exactly once.
     * Preserves callId, functionName and original arguments, then sends tool response to Gemini Live.
     */
    suspend fun confirmLiveTool(callId: String): String? {
        val pending = pendingConfirmationsMap.remove(callId) ?: run {
            Log.w(tag, "confirmLiveTool: No pending confirmation found for callId: $callId (already executed or rejected)")
            return null
        }

        if (_pendingToolConfirmation.value?.callId == callId) {
            _pendingToolConfirmation.value = null
        }

        Log.i(tag, "Executing confirmed sensitive tool: ${pending.functionName}")
        val result = toolExecutor?.executeTool(pending.functionName, pending.arguments)
            ?: "تم تنفيذ الإجراء بنجاح."
        sendToolResult(pending.callId, pending.functionName, result)
        return result
    }

    /**
     * Rejects a pending sensitive live tool without executing it.
     * Sends a rejection notice back to Gemini Live so it responds naturally to the user.
     */
    suspend fun rejectLiveTool(callId: String): String {
        val pending = pendingConfirmationsMap.remove(callId)
        if (_pendingToolConfirmation.value?.callId == callId) {
            _pendingToolConfirmation.value = null
        }

        val fnName = pending?.functionName ?: "الإجراء"
        Log.i(tag, "User rejected sensitive tool: $fnName ($callId)")
        val rejectionResult = "تم رفض الإجراء من قبل المستخدم (أمي). لم يتم تنفيذ أي تغيير."
        sendToolResult(callId, fnName, rejectionResult)
        return rejectionResult
    }

    /**
     * Sends tool execution result back to the Live Session over WebSocket.
     */
    fun sendToolResult(callId: String, functionName: String, resultString: String) {
        lastSentToolResult = ToolResponseData(callId, functionName, resultString)
        if (isUsingFallback || webSocket == null) return

        val toolResponseMsg = JSONObject().apply {
            put("toolResponse", JSONObject().apply {
                put("functionResponses", JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", callId)
                        put("name", functionName)
                        put("response", JSONObject().apply {
                            put("output", resultString)
                        })
                    })
                })
            })
        }
        webSocket?.send(toolResponseMsg.toString())
        Log.d(tag, "Tool result sent back to Gemini Live for function: $functionName")
    }

    /**
     * Interruption / Barge-in: stops audio playback instantly, flushes buffers, and transitions to LISTENING.
     */
    fun interruptPlayback() {
        audioOutput.stopAndFlush()
        fallbackVoiceService.stop()
        mockJob?.cancel()

        if (!isUsingFallback && webSocket != null) {
            _sessionState.value = LiveSessionState.INTERRUPTED
            scope.launch {
                delay(150)
                _sessionState.value = LiveSessionState.LISTENING
            }
        } else {
            _sessionState.value = LiveSessionState.LISTENING
        }
    }

    /**
     * Reconnects after transient error or network disconnection.
     */
    fun reconnect() {
        closeSession()
        _sessionState.value = LiveSessionState.RECONNECTING
        scope.launch {
            delay(500)
            startLiveSession()
        }
    }

    /**
     * Local Voice Simulation for offline/mock fallback.
     */
    private fun startLocalSimulation(onGreetingSpoken: () -> Unit) {
        isUsingFallback = true
        _sessionState.value = LiveSessionState.LISTENING
        _liveTranscript.value = "في الاستماع إليك يا أمي... 🌷"

        val welcome = "على سلامتك يا أمي 🌷 رفيقة تسمع فيك، تفضلي احكيلي."
        fallbackVoiceService.speak(welcome, "ar")
        onGreetingSpoken()
    }

    private fun handleMockUserSpeech(userText: String) {
        interruptPlayback()
        _sessionState.value = LiveSessionState.THINKING
        _liveTranscript.value = "أمي: $userText"

        mockJob?.cancel()
        mockJob = scope.launch {
            delay(400)
            val reply = "نسمع فيك بكل حب يا أمي الغالية. أنا معاك ديما خطوة بخطوة 🌷"
            _sessionState.value = LiveSessionState.SPEAKING
            _liveTranscript.value = "رفيقة: $reply"
            fallbackVoiceService.speak(reply, "ar")

            delay(1500)
            _sessionState.value = LiveSessionState.LISTENING
        }
    }

    /**
     * Closes and cleans up the active session.
     */
    fun closeSession() {
        stopMicrophoneCapture()
        audioOutput.stopAndFlush()
        audioOutput.release()
        fallbackVoiceService.stop()
        mockJob?.cancel()

        webSocket?.close(1000, "Session ended by user")
        webSocket = null
        pendingConfirmationsMap.clear()
        _pendingToolConfirmation.value = null
        _sessionState.value = LiveSessionState.IDLE
        _liveTranscript.value = ""
    }
}
