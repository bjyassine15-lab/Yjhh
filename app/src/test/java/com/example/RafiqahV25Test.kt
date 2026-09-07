package com.example

import com.example.ai.GeminiAIService
import com.example.ai.auth.AuthEnvironment
import com.example.ai.auth.DevelopmentGeminiAuthProvider
import com.example.ai.auth.ProductionGeminiAuthProvider
import com.example.ai.learning.LearningEngine
import com.example.ai.live.audio.AudioInput
import com.example.ai.live.audio.AudioOutput
import com.example.ai.memory.MemoryManager
import com.example.ai.tools.AIToolRegistry
import com.example.ai.tools.ToolAccessLevel
import com.example.domain.model.MemoryCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RafiqahV25Test {

    @Test
    fun `auth provider returns development mode and safe key resolution`() {
        val devAuth = DevelopmentGeminiAuthProvider()
        assertEquals(AuthEnvironment.DEVELOPMENT, devAuth.environment)

        val prodAuth = ProductionGeminiAuthProvider()
        assertEquals(AuthEnvironment.PRODUCTION_BACKEND, prodAuth.environment)
    }

    @Test
    fun `sensitive tools require confirmation`() {
        val deleteMem = AIToolRegistry.TOOLS.find { it.name == "delete_memory" }
        assertNotNull(deleteMem)
        assertEquals(ToolAccessLevel.SENSITIVE_WRITE, deleteMem?.accessLevel)

        val saveHealth = AIToolRegistry.TOOLS.find { it.name == "save_health_note" }
        assertNotNull(saveHealth)
        assertEquals(ToolAccessLevel.SENSITIVE_WRITE, saveHealth?.accessLevel)

        val getProfile = AIToolRegistry.TOOLS.find { it.name == "get_mother_profile" }
        assertNotNull(getProfile)
        assertEquals(ToolAccessLevel.READ_TOOL, getProfile?.accessLevel)
    }

    @Test
    fun `gemini turn result parser extracts functionCall and arguments correctly`() {
        val service = GeminiAIService()
        val geminiJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "functionCall": {
                          "name": "add_daily_task",
                          "args": {
                            "title": "موعد مع طبيب القلب",
                            "timeHint": "10:00",
                            "category": "APPOINTMENT"
                          }
                        }
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val result = service.parseTurnResult(geminiJson)
        assertEquals("add_daily_task", result.functionCallName)
        assertEquals("موعد مع طبيب القلب", result.functionCallArgs["title"])
        assertEquals("10:00", result.functionCallArgs["timeHint"])
        assertEquals("APPOINTMENT", result.functionCallArgs["category"])
    }

    @Test
    fun `memory candidate flags sensitive health statements for user approval`() {
        val memoryManager = MemoryManager()
        val candidates = memoryManager.extractMemories(
            userUtterance = "عندي السكر من 10 سنين وناخذ في الدواء",
            aiReply = "ربي يشفيك يا أمي الغالية ويحفظك",
            structuredAiJson = null
        )

        assertFalse(candidates.isEmpty())
        val healthCandidate = candidates.find { it.category == MemoryCategory.HEALTH }
        assertNotNull(healthCandidate)
        assertTrue(healthCandidate?.requiresApproval == true)
    }

    @Test
    fun `general memory candidate does not block with approval requirement`() {
        val memoryManager = MemoryManager()
        val candidates = memoryManager.extractMemories(
            userUtterance = "نحب نشرب التاي بنعناع في العشية",
            aiReply = "صحة وبالشفاء يا أمي الغالية",
            structuredAiJson = null
        )

        assertFalse(candidates.isEmpty())
        val prefCandidate = candidates.find { it.category == MemoryCategory.PREFERENCE }
        assertNotNull(prefCandidate)
        assertFalse(prefCandidate?.requiresApproval == true)
    }

    @Test
    fun `audio interfaces contract verification`() {
        var recordedChunks = 0
        val mockInput = object : AudioInput {
            override val isRecording: Boolean = true
            override fun startRecording(onChunkCaptured: (ByteArray) -> Unit): Boolean {
                onChunkCaptured(ByteArray(320))
                return true
            }
            override fun stopRecording() {}
            override fun release() {}
        }

        mockInput.startRecording {
            recordedChunks++
        }
        assertEquals(1, recordedChunks)

        var chunksPlayed = 0
        var flushed = false
        val mockOutput = object : AudioOutput {
            override val isPlaying: Boolean = true
            override fun playPcmChunk(pcmData: ByteArray) {
                chunksPlayed++
            }
            override fun stopAndFlush() {
                flushed = true
            }
            override fun release() {}
        }

        mockOutput.playPcmChunk(ByteArray(480))
        mockOutput.stopAndFlush()

        assertEquals(1, chunksPlayed)
        assertTrue(flushed)
    }

    @Test
    fun `learning engine tracks progress and review flags`() {
        val engine = LearningEngine()
        engine.registerConcept("mitochondria", "الميتوكوندريا", initialLevel = 2)

        engine.onUserConfused("mitochondria")
        val progressConfused = engine.getConceptProgress("mitochondria")
        assertEquals(1, progressConfused.currentLevel)
        assertTrue(progressConfused.needsReview)

        engine.onUserUnderstood("mitochondria")
        val progressUnderstood = engine.getConceptProgress("mitochondria")
        assertEquals(2, progressUnderstood.currentLevel)
        assertFalse(progressUnderstood.needsReview)
    }
}
