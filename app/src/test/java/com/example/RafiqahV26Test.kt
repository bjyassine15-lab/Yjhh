package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.GeminiAIService
import com.example.ai.VoiceService
import com.example.ai.auth.AuthEnvironment
import com.example.ai.auth.DynamicGeminiAuthProvider
import com.example.ai.auth.EncryptedGeminiApiKeyStore
import com.example.ai.auth.GeminiApiKeyStore
import com.example.ai.auth.GeminiConnectionStatus
import com.example.ai.live.GeminiLiveService
import com.example.ai.live.LiveSessionState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RafiqahV26Test {

    private lateinit var context: Context
    private lateinit var keyStore: GeminiApiKeyStore
    private lateinit var authProvider: DynamicGeminiAuthProvider

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        keyStore = EncryptedGeminiApiKeyStore(context)
        keyStore.clearApiKey()
        authProvider = DynamicGeminiAuthProvider(keyStore)
    }

    @Test
    fun `api key store saves, retrieves, masks and clears key correctly`() {
        assertNull(keyStore.getApiKey())
        assertFalse(keyStore.hasApiKey())
        assertNull(keyStore.getMaskedApiKey())

        val testKey = "AIzaSyDummyTestKey1234ABCD"
        keyStore.saveApiKey(testKey)

        assertTrue(keyStore.hasApiKey())
        assertEquals(testKey, keyStore.getApiKey())

        val masked = keyStore.getMaskedApiKey()
        assertNotNull(masked)
        assertTrue(masked!!.endsWith("ABCD"))
        assertTrue(masked.startsWith("********"))

        keyStore.clearApiKey()
        assertFalse(keyStore.hasApiKey())
        assertNull(keyStore.getApiKey())
        assertNull(keyStore.getMaskedApiKey())
    }

    @Test
    fun `dynamic auth provider updates status when key is stored or cleared`() = runBlocking {
        // Clear any stored key
        keyStore.clearApiKey()
        val emptyProvider = DynamicGeminiAuthProvider(keyStore)
        assertEquals(AuthEnvironment.DEVELOPMENT, emptyProvider.environment)

        // Save key and update
        keyStore.saveApiKey("AIzaSyValidFormatKeyForTesting9999")
        val configuredProvider = DynamicGeminiAuthProvider(keyStore)
        assertEquals(GeminiConnectionStatus.CONFIGURED, configuredProvider.connectionStatus.first())
        assertEquals("AIzaSyValidFormatKeyForTesting9999", configuredProvider.getApiKeyOrToken())

        // Clear key
        keyStore.clearApiKey()
        configuredProvider.updateStatus(GeminiConnectionStatus.NOT_CONFIGURED)
        assertEquals(GeminiConnectionStatus.NOT_CONFIGURED, configuredProvider.connectionStatus.first())
        assertNull(configuredProvider.getApiKeyOrToken())
    }

    @Test
    fun `testConnection with missing key returns informative failure without crashing`() = runBlocking {
        keyStore.clearApiKey()
        val result = authProvider.testConnection()
        assertFalse(result.success)
        assertTrue(result.message.contains("Gemini API Key") || result.message.contains("الإعدادات"))
    }

    @Test
    fun `gemini ai service returns helpful error message when api key is not configured`() = runBlocking {
        keyStore.clearApiKey()
        val emptyAuth = DynamicGeminiAuthProvider(keyStore)
        val aiService = GeminiAIService(authProvider = emptyAuth)

        val response = aiService.sendVoiceMessage(
            userSpeech = "أهلا رفيقة كيف حالك اليوم؟",
            userProfileSummary = "أمي صليحة",
            recentMemories = emptyList()
        )
        assertTrue(response.replyText.contains("الإعدادات") || response.replyText.contains("مفتاح"))
    }

    @Test
    fun `gemini live service reports clear error when starting session without key`() = runBlocking {
        keyStore.clearApiKey()
        val emptyAuth = DynamicGeminiAuthProvider(keyStore)
        val voiceService = VoiceService(context)
        val liveService = GeminiLiveService(
            context = context,
            fallbackVoiceService = voiceService,
            authProvider = emptyAuth
        )

        liveService.startLiveSession()
        ShadowLooper.idleMainLooper()

        assertEquals(LiveSessionState.ERROR, liveService.sessionState.value)
        assertTrue(liveService.liveTranscript.value.contains("الإعدادات"))
    }
}
