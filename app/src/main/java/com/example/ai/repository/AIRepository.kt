package com.example.ai.repository

import com.example.ai.AIResponse
import com.example.ai.AIService
import com.example.ai.ComprehensionResult
import com.example.ai.GeminiAIService
import com.example.ai.context.ContextBuilder
import com.example.ai.learning.LearningEngine
import com.example.ai.memory.MemoryManager
import com.example.ai.safety.MedicalSafetyGuard
import com.example.ai.story.StoryEngine
import com.example.ai.tools.ToolExecutor
import com.example.data.repository.MemoryRepository
import com.example.domain.model.DailyTask
import com.example.domain.model.MemoryCategory
import com.example.domain.model.MemoryItem
import com.example.domain.model.MotherProfile
import com.example.domain.model.StoryChapter

data class PendingConfirmationData(
    val toolName: String,
    val arguments: Map<String, Any?>,
    val confirmationTitle: String,
    val confirmationPrompt: String
)

data class ProcessSpeechResult(
    val replyText: String,
    val spokenDialectText: String,
    val pendingActionConfirmation: PendingConfirmationData? = null,
    val pendingMemoryCandidate: MemoryManager.MemoryCandidate? = null,
    val isUrgentMedicalNotice: Boolean = false
)

/**
 * AIRepository for Rafiqah V2.5.
 * Central coordinator for:
 * User Utterance -> Medical Safety Check -> Context Building -> Gemini AI ->
 * Tool Calling Lifecycle -> Tool Execution (Room) -> Second Turn ->
 * Medical Output Sanitization -> Deduplicated Memory Extraction -> Approval Verification -> Room Persistence.
 */
class AIRepository(
    private val aiService: AIService,
    val toolExecutor: ToolExecutor,
    val memoryRepo: MemoryRepository,
    val contextBuilder: ContextBuilder = ContextBuilder(),
    val memoryManager: MemoryManager = MemoryManager(),
    val medicalSafetyGuard: MedicalSafetyGuard = MedicalSafetyGuard(),
    val learningEngine: LearningEngine = LearningEngine(),
    val storyEngine: StoryEngine = StoryEngine()
) {

    init {
        if (aiService is GeminiAIService) {
            aiService.toolExecutor = toolExecutor
        }
    }

    suspend fun processUserSpeech(
        userSpeech: String,
        profile: MotherProfile,
        recentMemories: List<MemoryItem>,
        currentChapter: StoryChapter? = null,
        todayTasks: List<DailyTask> = emptyList()
    ): ProcessSpeechResult {
        // Step 1: Medical Safety Pre-evaluation (Red Flags)
        val safetyEval = medicalSafetyGuard.evaluateInput(userSpeech)
        if (safetyEval.riskLevel == MedicalSafetyGuard.HealthRiskLevel.URGENT) {
            val emergencyMessage = safetyEval.emergencyAdviceMessage
                ?: "يا أمي الغالية، هذه علامة تستوجب فحصاً طبياً عاجلاً وبدون تأخير! اتصلي فوراً بالإسعاف على الرقم 190 (SAMU)."
            return ProcessSpeechResult(
                replyText = emergencyMessage,
                spokenDialectText = emergencyMessage,
                isUrgentMedicalNotice = true
            )
        }

        // Step 2: Check for sensitive tool confirmation triggers
        val lower = userSpeech.lowercase()
        var pendingConfirmation: PendingConfirmationData? = null
        if (lower.contains("موعد") && (lower.contains("طبيب") || lower.contains("سبيطار") || lower.contains("عيادة") || lower.contains("كلينيك"))) {
            val args = mapOf("title" to userSpeech, "timeHint" to "اليوم", "category" to "APPOINTMENT")
            if (toolExecutor.isConfirmationRequired("add_daily_task", args)) {
                pendingConfirmation = PendingConfirmationData(
                    toolName = "add_daily_task",
                    arguments = args,
                    confirmationTitle = "تسجيل موعد مع الطبيب 🩺",
                    confirmationPrompt = toolExecutor.getConfirmationMessage("add_daily_task", args)
                )
            }
        }

        // Step 3: Direct tool data injection for READ queries
        var injectedToolData: String? = null
        if (lower.contains("نهاري") || lower.contains("برنامجي") || lower.contains("شنوة عندي اليوم")) {
            injectedToolData = toolExecutor.executeTool("get_today_plan", emptyMap())
        } else if (lower.contains("تتذكر الكلمة الفرنسية") || lower.contains("فرنسي") && lower.contains("البارح")) {
            injectedToolData = toolExecutor.executeTool("get_french_progress", emptyMap())
        }

        // Step 4: Build Selective Context
        val contextPrompt = contextBuilder.buildRelevantContext(
            query = userSpeech,
            profile = profile,
            recentMemories = recentMemories,
            currentChapter = currentChapter,
            todayTasks = todayTasks
        ) + (if (injectedToolData != null) "\n=== بيانات الأدوات الحقيقية المسترجعة ===\n$injectedToolData" else "")

        // Step 5: Call Gemini AI Service
        val response = aiService.sendVoiceMessage(
            userSpeech = userSpeech,
            userProfileSummary = contextPrompt,
            recentMemories = recentMemories.map { it.content }
        )

        // Step 6: Medical Safety Post-evaluation (Sanitize Output)
        val sanitizedReply = medicalSafetyGuard.sanitizeOutput(response.replyText, safetyEval)
        val sanitizedSpoken = medicalSafetyGuard.sanitizeOutput(response.spokenDialectText, safetyEval)

        // Step 7: Update Learning Engine if user indicates comprehension status
        if (lower.contains("ما فهمتش") || lower.contains("عاودلي") || lower.contains("صعيبة")) {
            val conceptKey = currentChapter?.scientificConceptKey ?: "cell"
            learningEngine.onUserConfused(conceptKey)
        } else if (lower.contains("فهمت") && (lower.contains("عايشك") || lower.contains("مليح") || lower.contains("واضح"))) {
            val conceptKey = currentChapter?.scientificConceptKey ?: "cell"
            learningEngine.onUserUnderstood(conceptKey)
        }

        // Step 8: Memory Extraction with Deduplication and Sensitivity Approval Verification
        val candidates = memoryManager.extractMemories(
            userUtterance = userSpeech,
            aiReply = sanitizedReply,
            structuredAiJson = null,
            existingMemories = recentMemories
        )

        var pendingMemory: MemoryManager.MemoryCandidate? = null
        for (candidate in candidates) {
            if (candidate.requiresApproval) {
                // Sensitive memories (e.g. Health conditions/prescriptions) require user approval
                pendingMemory = candidate
            } else {
                memoryRepo.saveMemoryWithDeduplication(
                    content = candidate.content,
                    category = candidate.category,
                    importance = candidate.importance,
                    source = candidate.source
                )
            }
        }

        return ProcessSpeechResult(
            replyText = sanitizedReply,
            spokenDialectText = sanitizedSpoken,
            pendingActionConfirmation = pendingConfirmation,
            pendingMemoryCandidate = pendingMemory,
            isUrgentMedicalNotice = safetyEval.riskLevel == MedicalSafetyGuard.HealthRiskLevel.URGENT
        )
    }

    suspend fun approveAndSaveMemory(candidate: MemoryManager.MemoryCandidate): Long {
        return memoryRepo.saveMemoryWithDeduplication(
            content = candidate.content,
            category = candidate.category,
            importance = candidate.importance,
            source = candidate.source
        )
    }

    suspend fun deleteMemory(id: Long) {
        memoryRepo.deleteMemory(id)
    }

    suspend fun executeConfirmedTool(toolName: String, args: Map<String, Any?>): String {
        return toolExecutor.executeTool(toolName, args)
    }

    suspend fun handleRejectedConfirmation(toolName: String): String {
        return "باهي يا أمي، كيما تحب، ما سجلت حتى شيء وكل شيء تحت أمرك 🌷"
    }

    suspend fun getProgressiveExplanation(conceptKey: String, level: Int): String {
        return aiService.getProgressiveConceptExplanation(conceptKey, level)
    }

    suspend fun evaluateComprehension(questionKey: String, selectedIndex: Int): ComprehensionResult {
        return aiService.evaluateAnswer(questionKey, selectedIndex)
    }

    suspend fun generateDailyGreeting(motherName: String, timeOfDay: String, lastLesson: String): String {
        return aiService.generateDailyGreeting(motherName, timeOfDay, lastLesson)
    }
}
