package com.example.ai.routine

import com.example.ai.learning.KnowledgeState
import com.example.ai.learning.SpacedRepetitionEngine
import com.example.data.local.entity.MicroSessionEntity
import com.example.data.repository.HealthRepository
import com.example.data.repository.LearningProgressRepository
import com.example.data.repository.ReminderRepository
import com.example.data.repository.RoutineRepository
import java.util.Calendar

data class DailyCoachSummary(
    val greeting: String,
    val topPriorities: List<String>,
    val scheduledMicroSessions: List<MicroSessionEntity>,
    val wellnessTip: String
)

/**
 * Intelligent dynamic daily routine generator for Rafiqah V3.
 */
class DailyRoutineEngine(
    private val routineRepo: RoutineRepository,
    private val healthRepo: HealthRepository,
    private val reminderRepo: ReminderRepository,
    private val learningRepo: LearningProgressRepository,
    private val spacedRepetition: SpacedRepetitionEngine
) {

    suspend fun generateOrRefreshDailyPlan(): List<MicroSessionEntity> {
        val currentSessions = routineRepo.getActiveMicroSessions()
        if (currentSessions.isNotEmpty()) {
            return currentSessions
        }

        // Build personalized plan based on knowledge needs, wellness habits, and time
        val sessions = mutableListOf<MicroSessionEntity>()
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)

        // 1. Reading Micro-session (10 mins)
        sessions.add(
            MicroSessionEntity(
                id = "reading_session_${System.currentTimeMillis()}",
                type = "READING",
                title = "قراءة هادئة: كيف يعمل قلبك؟",
                durationMinutes = 10,
                scheduledAtTimeHint = "10:00",
                isRequired = true,
                priority = 1,
                contentId = "content_heart_health",
                relatedConceptKey = "heart"
            )
        )

        // 2. French Micro-lesson (4 mins)
        sessions.add(
            MicroSessionEntity(
                id = "french_session_${System.currentTimeMillis() + 1}",
                type = "FRENCH",
                title = "كلمة فرنسية مفيدة: في الصيدلية والمستشفى",
                durationMinutes = 4,
                scheduledAtTimeHint = "15:30",
                isRequired = true,
                priority = 2
            )
        )

        // 3. Learning review or quick concept recall based on real due reviews
        val dueConcepts = spacedRepetition.getDueReviews()
        val targetConcept = dueConcepts.firstOrNull() ?: "cell"
        val conceptState = spacedRepetition.getConceptState(targetConcept)
        val reviewTitle = when (conceptState.state) {
            KnowledgeState.NEEDS_REVIEW -> "مراجعة خفيفة وتثبيت: ${conceptState.title}"
            KnowledgeState.LEARNING, KnowledgeState.PARTIALLY_UNDERSTOOD -> "استرجاع سريع: ${conceptState.title}"
            KnowledgeState.MASTERED -> "تمرين ذهني خفيف: استرجاع معلومات ${conceptState.title}"
            else -> "اكتشاف مفهوم علمي جديد: ${conceptState.title}"
        }
        sessions.add(
            MicroSessionEntity(
                id = "concept_session_${System.currentTimeMillis() + 2}",
                type = "CONCEPT_REVIEW",
                title = reviewTitle,
                durationMinutes = 3,
                scheduledAtTimeHint = "18:00",
                isRequired = false,
                priority = 3,
                relatedConceptKey = targetConcept
            )
        )

        // 4. Health habit micro-session (5 mins)
        val healthProfile = healthRepo.getHealthProfile()
        val healthTitle = if ((healthProfile?.currentWaterGlasses ?: 0) < 4) {
            "معلومة صحية: شرب الماء وسلامة الشرايين"
        } else {
            "عادتنا اليومية: مشي خفيف ونشاط هادئ"
        }
        sessions.add(
            MicroSessionEntity(
                id = "health_session_${System.currentTimeMillis() + 3}",
                type = "HEALTH_EDUCATION",
                title = healthTitle,
                durationMinutes = 5,
                scheduledAtTimeHint = "19:00",
                isRequired = false,
                priority = 4,
                contentId = "content_heart_health"
            )
        )

        routineRepo.saveSessions(sessions)
        return sessions
    }

    suspend fun getDailyCoachSummary(): DailyCoachSummary {
        val sessions = generateOrRefreshDailyPlan()
        val reminders = reminderRepo.getUpcomingReminders()

        val priorities = mutableListOf<String>()
        reminders.take(2).forEach {
            priorities.add("موعد: ${it.title} (${it.timeHint})")
        }
        sessions.filter { it.isRequired && !it.isCompleted }.take(2).forEach {
            priorities.add("${it.title} (${it.durationMinutes} دقائق)")
        }

        val greeting = "صباح النور والسرور يا أمي الغالية 🌸. نهارك طيب ومبارك."
        val wellnessTip = "كأس ماء دافئ الصباح مع مشي خفيف يعطيك طاقة ونشاط ويحمي ضغط الدم."

        return DailyCoachSummary(
            greeting = greeting,
            topPriorities = priorities,
            scheduledMicroSessions = sessions,
            wellnessTip = wellnessTip
        )
    }

    suspend fun rescheduleMissedActivity(sessionId: String, newTimeHint: String): Boolean {
        val session = routineRepo.getActiveMicroSessions().find { it.id == sessionId }
        if (session != null) {
            val rescheduled = session.copy(
                scheduledAtTimeHint = newTimeHint,
                isCompleted = false
            )
            routineRepo.saveSessions(listOf(rescheduled))
            return true
        }
        return false
    }
}
