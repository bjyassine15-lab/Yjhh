package com.example.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceService(context: Context) : TextToSpeech.OnInitListener {

    private val tag = "RafiqahAI"
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentSpokenText = MutableStateFlow("")
    val currentSpokenText: StateFlow<String> = _currentSpokenText.asStateFlow()

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.w(tag, "TextToSpeech service initialization failed: ${e.message}")
        }
    }

    private fun resolveBestArabicLocale(): Locale {
        return try {
            val available = Locale.getAvailableLocales()
            val tunisian = available.firstOrNull { it.language == "ar" && it.country.equals("TN", ignoreCase = true) }
            if (tunisian != null) return tunisian
            val anyArabic = available.firstOrNull { it.language == "ar" }
            anyArabic ?: Locale("ar")
        } catch (_: Exception) {
            Locale("ar")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            try {
                val bestAr = resolveBestArabicLocale()
                val res = tts?.setLanguage(bestAr)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    val fallbackRes = tts?.setLanguage(Locale("ar"))
                    if (fallbackRes == TextToSpeech.LANG_MISSING_DATA || fallbackRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.setLanguage(Locale.getDefault())
                    }
                }
                tts?.setSpeechRate(0.85f) // Warm, relaxed, slightly slower speech for mother
                tts?.setPitch(1.05f)

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        _currentSpokenText.value = ""
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
            } catch (e: Exception) {
                Log.w(tag, "Error setting TTS language/parameters: ${e.message}")
            }
        }
    }

    fun speak(text: String, languageTag: String = "ar") {
        if (!isInitialized || tts == null) return
        try {
            stop()
            _currentSpokenText.value = text
            val targetLocale = if (languageTag.startsWith("fr")) Locale.FRENCH else resolveBestArabicLocale()
            tts?.language = targetLocale
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "RafiqahUtterance_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.w(tag, "TTS speak error: ${e.message}")
            _isSpeaking.value = false
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        _isSpeaking.value = false
        _currentSpokenText.value = ""
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
    }
}
