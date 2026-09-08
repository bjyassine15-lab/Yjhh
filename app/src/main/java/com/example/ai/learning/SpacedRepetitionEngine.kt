package com.example.ai.learning

import com.example.data.local.entity.ConceptProgressEntity
import com.example.data.repository.LearningProgressRepository

/**
 * Knowledge State levels as requested in V3 Rule 14.
 */
enum class KnowledgeState {
    NOT_INTRODUCED,
    INTRODUCED,
    LEARNING,
    PARTIALLY_UNDERSTOOD,
    MASTERED,
    NEEDS_REVIEW
}

data class ConceptKnowledge(
    val conceptKey: String,
    val title: String,
    val state: KnowledgeState,
    val masteryScore: Int, // 0 to 100
    val lastSeenAt: Long,
    val nextReviewAt: Long,
    val reviewCount: Int,
    val difficulty: Int // 1 to 5
)

/**
 * Spaced repetition and adaptive learning engine for V3.
 */
class SpacedRepetitionEngine(private val repository: LearningProgressRepository) {

    suspend fun getConceptState(conceptKey: String): ConceptKnowledge {
        val entity = repository.getProgress(conceptKey)
        val state = when {
            entity == null || entity.mastery == "NOT_STARTED" -> KnowledgeState.NOT_INTRODUCED
            entity.needsReview -> KnowledgeState.NEEDS_REVIEW
            entity.mastery == "MASTERED" -> KnowledgeState.MASTERED
            entity.mastery == "IN_PROGRESS" && entity.successfulAttempts > 2 -> KnowledgeState.PARTIALLY_UNDERSTOOD
            entity.mastery == "IN_PROGRESS" -> KnowledgeState.LEARNING
            else -> KnowledgeState.INTRODUCED
        }
        val score = if (entity != null && entity.attempts > 0) {
            ((entity.successfulAttempts.toDouble() / entity.attempts) * 100).toInt()
        } else 0

        val intervalHours = calculateNextReviewIntervalHours(state, entity?.successfulAttempts ?: 0)
        val nextReview = (entity?.lastReviewed ?: System.currentTimeMillis()) + (intervalHours * 3600_000L)

        return ConceptKnowledge(
            conceptKey = conceptKey,
            title = getConceptTitle(conceptKey),
            state = state,
            masteryScore = score,
            lastSeenAt = entity?.lastReviewed ?: System.currentTimeMillis(),
            nextReviewAt = nextReview,
            reviewCount = entity?.attempts ?: 0,
            difficulty = 2
        )
    }

    suspend fun recordAttempt(conceptKey: String, isCorrect: Boolean): ConceptKnowledge {
        val existing = repository.getProgress(conceptKey)
        val attempts = (existing?.attempts ?: 0) + 1
        val successes = (existing?.successfulAttempts ?: 0) + (if (isCorrect) 1 else 0)

        val newMastery = when {
            successes >= 3 && isCorrect -> "MASTERED"
            !isCorrect -> "IN_PROGRESS"
            else -> "IN_PROGRESS"
        }
        val needsReview = !isCorrect

        repository.saveProgress(
            conceptKey = conceptKey,
            currentLevel = if (successes >= 3) 3 else if (successes >= 1) 2 else 1,
            mastery = newMastery,
            needsReview = needsReview,
            attempts = attempts,
            successfulAttempts = successes
        )

        return getConceptState(conceptKey)
    }

    fun calculateNextReviewIntervalHours(state: KnowledgeState, successes: Int): Long {
        return when (state) {
            KnowledgeState.NEEDS_REVIEW -> 4L // Review soon (within 4 hours)
            KnowledgeState.NOT_INTRODUCED -> 0L
            KnowledgeState.INTRODUCED -> 12L
            KnowledgeState.LEARNING -> 24L // 1 day
            KnowledgeState.PARTIALLY_UNDERSTOOD -> 48L // 2 days
            KnowledgeState.MASTERED -> 168L // 1 week
        }
    }

    fun getConceptTitle(key: String): String {
        return when (key) {
            "cell" -> "الخلية ووحدات بناء الحياة"
            "membrane" -> "غشاء الخلية الحارس الذكي"
            "nucleus" -> "نواة الخلية ومركز المعلومات"
            "mitochondria" -> "الميتوكوندريا ومصنع الطاقة"
            "heart" -> "القلب والدورة الدموية"
            "nutrition" -> "التغذية المتوازنة ومضادات الأكسدة"
            else -> key
        }
    }
}
