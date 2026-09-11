package com.example.ai.routine

import com.example.ai.learning.ContentSelectionEngine
import com.example.ai.learning.KnowledgeState
import com.example.ai.learning.SpacedRepetitionEngine
import com.example.data.local.entity.MicroSessionEntity
import com.example.data.repository.HealthRepository
import com.example.data.repository.LearningProgressRepository
import com.example.data.repository.ReminderRepository
import com.example.data.repository.RoutineRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailyCoachSummary(
    val greeting: String,
    val topPriorities: List<String>,
    val scheduledMicroSessions: List<MicroSessionEntity>,
    val wellnessTip: String
)

/**
 * Intelligent, persistent dynamic daily routine generator for Rafiqah V3.
 * Eliminates static fallback sessions. Routine is derived from real reminders,
 * due spaced repetition concepts, health habit records, and user preferences.
 * Persists statuses (PLANNED, STARTED, COMPLETED, SKIPPED, RESCHEDULED) across app restarts.
 */
class DailyRoutineEngine(
    private val routineRepo: RoutineRepository,
    private val healthRepo: HealthRepository,
    private val reminderRepo: ReminderRepository,
    private val learningRepo: LearningProgressRepository,
    private val spacedRepetition: SpacedRepetitionEngine,
    private val contentSelectionEngine: ContentSelectionEngine? = null,
    private val frenchRepo: com.example.data.repository.FrenchWordRepository? = null
) {

    fun getTodayDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
    }

    suspend fun generateOrRefreshDailyPlan(): List<MicroSessionEntity> {
        val todayKey = getTodayDateKey()
        val existingTodaySessions = routineRepo.getSessionsForDate(todayKey)

        // If today's plan already exists, persist state across restarts!
        if (existingTodaySessions.isNotEmpty()) {
            return existingTodaySessions
        }

        val sessions = mutableListOf<MicroSessionEntity>()
        var priorityCounter = 1

        // 1. Reminders & scheduled appointments
        val upcomingReminders = reminderRepo.getUpcomingReminders()
        upcomingReminders.take(2).forEach { reminder ->
            sessions.add(
                MicroSessionEntity(
                    id = "reminder_${reminder.id}_$todayKey",
                    type = "REMINDER",
                    title = "موعد هام: ${reminder.title}",
                    durationMinutes = 5,
                    scheduledAtTimeHint = reminder.timeHint,
                    isRequired = true,
                    priority = priorityCounter++,
                    status = "PLANNED",
                    dateKey = todayKey,
                    requiredDurationSeconds = 300
                )
            )
        }

        // 2. Health & Wellness habits (only from real habits or active profile hydration goals)
        val realHabits = healthRepo.getAllHabits()
        if (realHabits.isNotEmpty()) {
            val habit = realHabits.first()
            sessions.add(
                MicroSessionEntity(
                    id = "health_habit_${habit.id}_$todayKey",
                    type = "HEALTH_HABIT",
                    title = habit.title,
                    durationMinutes = 5,
                    scheduledAtTimeHint = habit.timeHint.ifBlank { "09:30" },
                    isRequired = false,
                    priority = priorityCounter++,
                    status = "PLANNED",
                    dateKey = todayKey,
                    requiredDurationSeconds = 300
                )
            )
        } else {
            // Do not create a synthetic health session from default profile values.
            // Health sessions must originate from an actual persisted user habit/goal.
        }

        // 3. Dynamic Educational Reading & Spaced Repetition Reviews (FIX 9 & FIX 20)
        val dueConcepts = spacedRepetition.getDueReviews()
        if (dueConcepts.isNotEmpty()) {
            dueConcepts.take(2).forEachIndexed { idx, dueConcept ->
                val selectedContent = contentSelectionEngine?.selectContentForSession(
                    sessionType = "READING",
                    preferredConceptKey = dueConcept
                )
                val conceptState = spacedRepetition.getConceptState(dueConcept)
                val readingTitle = if (selectedContent != null) {
                    "مراجعة: ${selectedContent.title}"
                } else {
                    "مراجعة وتثبيت: ${conceptState.title}"
                }
                val readingMinutes = selectedContent?.estimatedMinutes ?: 8
                sessions.add(
                    MicroSessionEntity(
                        id = "review_${dueConcept}_${System.currentTimeMillis()}_${idx}_$todayKey",
                        type = "READING",
                        title = readingTitle,
                        durationMinutes = readingMinutes,
                        scheduledAtTimeHint = if (idx == 0) "11:00" else "15:30",
                        isRequired = true,
                        priority = priorityCounter++,
                        contentId = selectedContent?.id ?: "content_heart_health",
                        relatedConceptKey = dueConcept,
                        status = "PLANNED",
                        dateKey = todayKey,
                        requiredDurationSeconds = readingMinutes * 60
                    )
                )
            }
        } else {
            val selectedContent =
                contentSelectionEngine?.selectContentForSession(
                    sessionType = "READING"
                )

            if (selectedContent != null) {
                val readingMinutes =
                    selectedContent.estimatedMinutes
                        .coerceAtLeast(1)

                sessions.add(
                    MicroSessionEntity(
                        id = "reading_${selectedContent.id}_$todayKey",
                        type = "READING",
                        title = "قراءة اختيارية: ${selectedContent.title}",
                        durationMinutes = readingMinutes,
                        scheduledAtTimeHint = "11:00",
                        isRequired = false,
                        priority = priorityCounter++,
                        contentId = selectedContent.id,
                        relatedConceptKey =
                            selectedContent.relatedConceptKey,
                        status = "PLANNED",
                        dateKey = todayKey,
                        requiredDurationSeconds =
                            readingMinutes * 60
                    )
                )
            }
        }

        // 4. French review
        val unmasteredWords =
            frenchRepo
                ?.getAllWordsList()
                ?.filter { !it.isMastered }
                ?: emptyList()

        if (unmasteredWords.isNotEmpty()) {
            val word = unmasteredWords.first()

            sessions.add(
                MicroSessionEntity(
                    id = "french_review_${word.id}_$todayKey",
                    type = "FRENCH",
                    title = "مراجعة كلمة فرنسية: ${word.frenchWord}",
                    durationMinutes = 4,
                    scheduledAtTimeHint = "16:00",
                    isRequired = false,
                    priority = priorityCounter++,
                    status = "PLANNED",
                    dateKey = todayKey,
                    requiredDurationSeconds = 240
                )
            )
        }

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
        sessions.filter { it.isRequired && it.status != "COMPLETED" }.take(2).forEach {
            priorities.add("${it.title} (${it.durationMinutes} دقائق)")
        }

        val greeting = "صباح النور والسرور يا أمي الغالية 🌸. نهارك طيب ومبارك."
        val wellnessTip =
            "اليوم نجموا نركزوا على عادة بسيطة تناسب روتينك، مثل شوية حركة أو الاهتمام بالترطيب إذا كان هذا مناسب ليك."

        return DailyCoachSummary(
            greeting = greeting,
            topPriorities = priorities,
            scheduledMicroSessions = sessions,
            wellnessTip = wellnessTip
        )
    }

    suspend fun startSession(sessionId: String) {
        routineRepo.markStarted(sessionId)
    }

    suspend fun completeSession(sessionId: String) {
        routineRepo.markCompleted(sessionId)
    }

    suspend fun skipSession(sessionId: String) {
        routineRepo.markSkipped(sessionId)
    }

    suspend fun rescheduleMissedActivity(sessionId: String, newTimeHint: String): Boolean {
        routineRepo.markRescheduled(sessionId, newTimeHint)
        return true
    }
}
