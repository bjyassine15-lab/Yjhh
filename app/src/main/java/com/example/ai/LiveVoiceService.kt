package com.example.ai

import android.content.Context
import com.example.ai.live.GeminiLiveService
import com.example.ai.live.LiveSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Connection states for Live Voice in Rafiqah UI.
 */
enum class LiveVoiceState(val arabicStatus: String) {
    LISTENING("نسمع فيك... 🎙️"),
    THINKING("نفكر... 🧠"),
    SPEAKING("نحكي معاك... 🔊"),
    DISCONNECTED("انقطع الاتصال... ⚠️"),
    IDLE("جاهزة للمحادثة 🌷")
}

/**
 * Live Voice Service Facade for Rafiqah V2.1.
 * Bridges Jetpack Compose UI with [GeminiLiveService] (WebSocket Live API)
 * with graceful fallback to [VoiceService] (Android TTS) when offline.
 */
class LiveVoiceService(
    private val context: Context,
    private val voiceService: VoiceService,
    val geminiLiveService: GeminiLiveService = GeminiLiveService(context, voiceService)
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _connectionState = MutableStateFlow(LiveVoiceState.IDLE)
    val connectionState: StateFlow<LiveVoiceState> = _connectionState.asStateFlow()

    val liveTranscript: StateFlow<String> = geminiLiveService.liveTranscript

    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    init {
        // Synchronize GeminiLiveService states with UI state
        scope.launch {
            geminiLiveService.sessionState.collect { liveState ->
                _connectionState.value = when (liveState) {
                    LiveSessionState.LISTENING -> LiveVoiceState.LISTENING
                    LiveSessionState.THINKING, LiveSessionState.CONNECTING, LiveSessionState.RECONNECTING -> LiveVoiceState.THINKING
                    LiveSessionState.SPEAKING -> LiveVoiceState.SPEAKING
                    LiveSessionState.DISCONNECTED, LiveSessionState.ERROR -> LiveVoiceState.DISCONNECTED
                    LiveSessionState.INTERRUPTED -> LiveVoiceState.LISTENING
                    LiveSessionState.IDLE -> LiveVoiceState.IDLE
                }
            }
        }
    }

    /**
     * Starts the live voice session via GeminiLiveService.
     */
    fun startLiveSession(onAssistantSpeechGenerated: (userUtterance: String, replyText: String) -> Unit) {
        _isSessionActive.value = true
        geminiLiveService.startLiveSession()
    }

    /**
     * Dispatches user speech input to the Gemini Live Service.
     */
    fun onUserSpeechInput(
        speechText: String,
        onReplyReady: (userSpeech: String, reply: String, spokenDialect: String) -> Unit
    ) {
        geminiLiveService.sendTextMessage(speechText)
    }

    /**
     * Interrupts current assistant playback immediately.
     */
    fun interrupt() {
        geminiLiveService.interruptPlayback()
    }

    /**
     * Reconnects to the Live service.
     */
    fun reconnect() {
        geminiLiveService.reconnect()
    }

    /**
     * Ends the voice session.
     */
    fun endSession() {
        _isSessionActive.value = false
        geminiLiveService.closeSession()
    }
}
