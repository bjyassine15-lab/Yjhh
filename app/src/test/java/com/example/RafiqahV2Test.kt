package com.example

import com.example.ai.AIConfig
import com.example.ai.MockAIService
import com.example.ai.context.ContextBuilder
import com.example.ai.learning.ConceptMastery
import com.example.ai.learning.LearningEngine
import com.example.ai.memory.MemoryManager
import com.example.ai.safety.MedicalSafetyGuard
import com.example.ai.story.StoryEngine
import com.example.ai.tools.AIToolRegistry
import com.example.ai.tools.ToolAccessLevel
import com.example.domain.model.HealthProfile
import com.example.domain.model.MemoryCategory
import com.example.domain.model.MotherProfile
import com.example.domain.model.StoryChapter
import kotlinx.coroutines.runBlocking
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
class RafiqahV2Test {

    private val aiService = MockAIService()
    private val contextBuilder = ContextBuilder()
    private val memoryManager = MemoryManager()
    private val safetyGuard = MedicalSafetyGuard()
    private val learningEngine = LearningEngine()
    private val storyEngine = StoryEngine()

    @Test
    fun `scenario 1 - basic cell explanation from scratch`() = runBlocking {
        val response = aiService.sendVoiceMessage(
            userSpeech = "شنوة الخلية؟",
            userProfileSummary = "أمي الحبيبة 53 سنة",
            recentMemories = emptyList()
        )
        assertTrue(response.replyText.contains("الياجورة") || response.replyText.contains("الحيط"))
        assertFalse(response.isUrgentMedicalNotice)
    }

    @Test
    fun `scenario 2 - re-explanation when not understood`() = runBlocking {
        val response = aiService.sendVoiceMessage(
            userSpeech = "ما فهمتش يا رفيقة عاودلي",
            userProfileSummary = "أمي الحبيبة",
            recentMemories = emptyList()
        )
        assertTrue(response.replyText.contains("ما توضّحش مليح"))
        assertTrue(response.replyText.contains("الدار") || response.replyText.contains("مثال"))
    }

    @Test
    fun `scenario 3 - validation check when mother understands`() = runBlocking {
        val response = aiService.sendVoiceMessage(
            userSpeech = "أنا فهمت الخلية مليح عايشك",
            userProfileSummary = "أمي الحبيبة",
            recentMemories = emptyList()
        )
        assertTrue(response.replyText.contains("ما شاء الله") || response.replyText.contains("النواة"))
    }

    @Test
    fun `scenario 4 - appointment asks natural confirmation`() = runBlocking {
        val response = aiService.sendVoiceMessage(
            userSpeech = "اليوم عندي موعد مع الطبيب",
            userProfileSummary = "أمي الحبيبة",
            recentMemories = emptyList()
        )
        assertTrue(response.replyText.contains("تحبي نسجل الموعد هذا في نهارك"))
    }

    @Test
    fun `scenario 5 - continuing medical story references Sarah and current events`() = runBlocking {
        val response = aiService.sendVoiceMessage(
            userSpeech = "وين وصلنا في قصة سارة؟",
            userProfileSummary = "أمي الحبيبة",
            recentMemories = emptyList()
        )
        assertTrue(response.replyText.contains("سارة") && (response.replyText.contains("المجهر") || response.replyText.contains("الكلية")))
    }

    @Test
    fun `scenario 6 - health inquiry gives safe companion advice without prescribing`() = runBlocking {
        val response = aiService.sendVoiceMessage(
            userSpeech = "التونسيو طالعة شوية وراسي يوجع",
            userProfileSummary = "أمي الحبيبة",
            recentMemories = emptyList()
        )
        assertTrue(response.replyText.contains("طبيب") || response.replyText.contains("استشارة"))
        assertTrue(response.replyText.contains("الملح") || response.replyText.contains("ماء"))
    }

    @Test
    fun `scenario 7 - recalls French vocabulary from yesterday`() = runBlocking {
        val response = aiService.sendVoiceMessage(
            userSpeech = "تتذكر الكلمة الفرنسية اللي قريتها البارح؟",
            userProfileSummary = "أمي الحبيبة",
            recentMemories = emptyList()
        )
        assertTrue(response.replyText.contains("Rendez-vous") || response.replyText.contains("رانديفو"))
    }

    @Test
    fun `safety red flag triggers emergency warning and 190`() {
        val evaluation = safetyGuard.evaluateInput("عندي وجيعة قوية في صدري وضيق شديد في التنفس")
        assertEquals(MedicalSafetyGuard.HealthRiskLevel.URGENT, evaluation.riskLevel)
        assertTrue(evaluation.requiresEmergencyAdvice)
        assertTrue(evaluation.emergencyAdviceMessage?.contains("190") == true)
    }

    @Test
    fun `medical safety guard sanitizes forbidden prescriptions`() {
        val eval = safetyGuard.evaluateInput("راسي يوجع")
        val sanitized = safetyGuard.sanitizeOutput("أنصحك بأخذ دواء 500 مغ", eval)
        assertFalse(sanitized.contains("أنصحك بأخذ دواء"))
        assertTrue(sanitized.contains("طبيب"))
    }

    @Test
    fun `context builder contains no hardcoded health data when empty`() {
        val emptyProfile = MotherProfile(
            health = HealthProfile(
                diagnosedConditions = emptyList(),
                medications = emptyList(),
                sleepQuality = "",
                healthGoals = emptyList()
            )
        )
        val context = contextBuilder.buildRelevantContext("عندي وجيعة", emptyProfile, emptyList())
        assertTrue(context.contains("لا توجد معلومات صحية مسجلة"))
        assertFalse(context.contains("دواء الصباح منتظم"))
    }

    @Test
    fun `learning engine decreases level and sets review when mother is confused`() {
        learningEngine.registerConcept("cell", "الخلية", initialLevel = 2)
        learningEngine.onUserConfused("cell")
        val progress = learningEngine.getConceptProgress("cell")
        assertEquals(1, progress.currentLevel)
        assertTrue(progress.needsReview)
        assertEquals(ConceptMastery.REVIEW, progress.mastery)
    }

    @Test
    fun `learning engine increases level when mother understands`() {
        learningEngine.registerConcept("membrane", "غشاء الخلية", initialLevel = 2)
        learningEngine.onUserUnderstood("membrane")
        val progress = learningEngine.getConceptProgress("membrane")
        assertEquals(3, progress.currentLevel)
        assertFalse(progress.needsReview)
    }

    @Test
    fun `story engine calculates decoupled progress accurately`() {
        val chapters = listOf(
            StoryChapter(1, 1, "الفصل 1", "", "", "", "", isCompleted = true),
            StoryChapter(2, 2, "الفصل 2", "", "", "", "", isCompleted = true),
            StoryChapter(3, 3, "الفصل 3", "", "", "", "", isCompleted = false),
            StoryChapter(4, 4, "الفصل 4", "", "", "", "", isCompleted = false)
        )
        val progress = storyEngine.calculateStoryProgress(chapters, 2)
        assertEquals(4, progress.totalChapters)
        assertEquals(2, progress.completedChaptersCount)
        assertEquals(50, progress.percentCompleted)
    }

    @Test
    fun `ai tool registry conforms to official gemini declaration schema`() {
        val decls = AIToolRegistry.getGeminiToolsDeclarationJson()
        assertTrue(decls.length() > 0)
        val firstObj = decls.getJSONObject(0)
        assertTrue(firstObj.has("function_declarations"))
        val fnList = firstObj.getJSONArray("function_declarations")
        assertTrue(fnList.length() >= 18)

        val deleteMem = AIToolRegistry.TOOLS.find { it.name == "delete_memory" }
        assertNotNull(deleteMem)
        assertEquals(ToolAccessLevel.SENSITIVE_WRITE, deleteMem?.accessLevel)
    }

    @Test
    fun `memory manager ignores transient chitchat`() {
        val candidates = memoryManager.extractMemories(
            userUtterance = "صباح الخير على سلامتك",
            aiReply = "صباح الورد يا أمي",
            structuredAiJson = null
        )
        assertTrue(candidates.isEmpty())
    }

    @Test
    fun `central models IDs match specifications`() {
        assertEquals("gemini-3.5-flash", AIConfig.GEMINI_DEFAULT_TEXT_MODEL)
        assertEquals("gemini-3.1-flash-live-preview", AIConfig.GEMINI_LIVE_VOICE_MODEL)
    }
}
