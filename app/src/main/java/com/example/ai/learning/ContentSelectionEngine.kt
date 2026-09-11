package com.example.ai.learning

import com.example.data.local.dao.ContentDao
import com.example.data.local.entity.ContentItemEntity
import com.example.data.repository.ContentRepository
import com.example.data.repository.LearningProgressRepository

/**
 * Intelligent ContentSelectionEngine for Rafiqah V3.
 * Eliminates static fallback or arbitrary firstOrNull() by dynamically ranking educational
 * content according to due spaced repetition concepts, recent completion history,
 * category relevance, and the mother's learning priorities.
 */
class ContentSelectionEngine(
    private val contentRepo: ContentRepository,
    private val contentDao: ContentDao,
    private val learningRepo: LearningProgressRepository,
    private val spacedRepetition: SpacedRepetitionEngine
) {

    suspend fun selectContentForSession(
        sessionType: String,
        preferredConceptKey: String? = null,
        preferredCategory: String? = null
    ): ContentItemEntity? {
        val allItems: List<ContentItemEntity> = contentDao.getAllContentItems()
        if (allItems.isEmpty()) return null

        // If a specific concept is requested (e.g. from a due review or session link)
        if (!preferredConceptKey.isNullOrBlank()) {
            val matching: ContentItemEntity? = allItems.find { 
                it.relatedConceptKey.equals(preferredConceptKey, ignoreCase = true) 
            }
            if (matching != null) return matching
        }

        // Check which concepts are due for review via Spaced Repetition Engine
        val dueConcepts: Set<String> = spacedRepetition.getDueReviews().toSet()

        // Fetch recent reading sessions to avoid repetition
        val recentSessions = contentDao.getRecentReadingSessions(limit = 10)
        val recentlyReadContentIds: Set<String> = recentSessions.map { it.contentId }.toSet()

        // Filter by category relevance if sessionType provides hints
        val categoryFilter: List<String> = when {
            !preferredCategory.isNullOrBlank() -> listOf(preferredCategory)
            sessionType.contains("HEALTH", ignoreCase = true) -> listOf("HEALTH_EDUCATION")
            sessionType.contains("FRENCH", ignoreCase = true) -> listOf("FRENCH")
            sessionType.contains("STORY", ignoreCase = true) -> listOf("STORY", "CULTURE", "HISTORY")
            sessionType.contains("SCIENCE", ignoreCase = true) || sessionType.contains("CONCEPT", ignoreCase = true) -> listOf("SCIENCE")
            else -> listOf("SCIENCE", "HEALTH_EDUCATION", "CULTURE", "HISTORY", "FRENCH", "STORY")
        }

        val eligibleItems: List<ContentItemEntity> = allItems.filter { item ->
            categoryFilter.any { cat -> cat.equals(item.category, ignoreCase = true) }
        }.ifEmpty { allItems }

        // Rank candidates:
        // +100 if relatedConceptKey is in dueConcepts
        // +50 if NOT in recentlyReadContentIds
        // +10 for estimated duration around 8-10 mins
        val scoredItems: List<Pair<ContentItemEntity, Int>> = eligibleItems.map { item: ContentItemEntity ->
            var score = 0
            val conceptKey = item.relatedConceptKey
            if (conceptKey != null && dueConcepts.contains(conceptKey)) {
                score += 100
            }
            if (!recentlyReadContentIds.contains(item.id)) {
                score += 50
            }
            if (item.estimatedMinutes in 7..12) {
                score += 10
            }
            Pair(item, score)
        }.sortedByDescending { it.second }

        return scoredItems.firstOrNull()?.first ?: eligibleItems.firstOrNull()
    }
}
