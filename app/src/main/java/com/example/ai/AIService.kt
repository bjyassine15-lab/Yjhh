package com.example.ai

data class AIResponse(
    val replyText: String,
    val spokenDialectText: String,
    val extractedMemories: List<ExtractedMemoryCandidate> = emptyList(),
    val suggestedFollowUp: String? = null,
    val isUrgentMedicalNotice: Boolean = false
)

data class ExtractedMemoryCandidate(
    val content: String,
    val category: String,
    val importance: Int = 3
)

data class ComprehensionResult(
    val isCorrect: Boolean,
    val feedbackTunisian: String,
    val encouragementMessage: String
)

interface AIService {
    suspend fun sendVoiceMessage(
        userSpeech: String,
        userProfileSummary: String,
        recentMemories: List<String>
    ): AIResponse

    suspend fun getProgressiveConceptExplanation(
        conceptKey: String,
        level: Int
    ): String

    suspend fun generateDailyGreeting(
        motherName: String,
        timeOfDay: String,
        lastLessonTitle: String
    ): String

    suspend fun evaluateAnswer(
        questionKey: String,
        selectedOptionIndex: Int
    ): ComprehensionResult
}
