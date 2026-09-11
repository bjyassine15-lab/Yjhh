package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Health & Wellness profile separate from basic user profile (V3).
 */
@Entity(tableName = "health_profiles")
data class HealthProfileEntity(
    @PrimaryKey val id: Int = 1,
    val age: Int = 0, // 0 = unconfigured
    val sleepQuality: String = "",
    val sleepTimeHint: String = "",
    val wakeTimeHint: String = "",
    val activityLevel: String = "",
    val waterIntakeGoalGlasses: Int = 0,
    val currentWaterGlasses: Int = 0,
    val dietaryHabits: String = "",
    val generalGoals: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Unstructured/structured health observation extracted from natural conversation (V3).
 */
@Entity(tableName = "health_observations")
data class HealthObservationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val observationText: String,
    val category: String, // SLEEP, ACTIVITY, NUTRITION, HYDRATION, SYMPTOM_NOTE, WELLNESS
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "VOICE_CONVERSATION",
    val userConfirmed: Boolean = true
)

/**
 * Habits to track in wellness routine (V3).
 */
@Entity(tableName = "wellness_habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetFrequency: String = "DAILY", // DAILY, WEEKLY
    val timeHint: String = "الصباح",
    val isCompletedToday: Boolean = false,
    val streakDays: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Scheduled micro-sessions and required routine items (V3).
 * Status transitions: PLANNED -> STARTED -> COMPLETED / SKIPPED / RESCHEDULED
 */
@Entity(tableName = "micro_sessions")
data class MicroSessionEntity(
    @PrimaryKey val id: String,
    val type: String, // READING, MENTAL_EXERCISE, FRENCH, HEALTH_EDUCATION, CONCEPT_REVIEW
    val title: String,
    val durationMinutes: Int,
    val scheduledAtTimeHint: String,
    val isRequired: Boolean = true,
    val isCompleted: Boolean = false,
    val isSkipped: Boolean = false,
    val status: String = "PLANNED", // PLANNED, STARTED, COMPLETED, SKIPPED, RESCHEDULED
    val dateKey: String = "", // e.g. "2026-09-11" to preserve and reload today's routine
    val requiredDurationSeconds: Int,
    val priority: Int = 1,
    val relatedConceptKey: String? = null,
    val contentId: String? = null,
    val completedAt: Long = 0L
)

/**
 * Content items for educational reading and stories (V3).
 */
@Entity(tableName = "content_items")
data class ContentItemEntity(
    @PrimaryKey val id: String,
    val category: String, // SCIENCE, HEALTH_EDUCATION, HISTORY, CULTURE, PERSONAL_GROWTH, STORY
    val title: String,
    val body: String,
    val estimatedMinutes: Int = 10,
    val keyTakeaway: String,
    val relatedConceptKey: String? = null,
    val quizQuestion: String? = null,
    val quizAnswer: String? = null
)

/**
 * Active and past reading sessions with required & optional continuation time (V3).
 */
@Entity(tableName = "reading_sessions")
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contentId: String,
    val contentTitle: String,
    val requiredDurationSeconds: Int,
    val elapsedRequiredSeconds: Int = 0,
    val elapsedOptionalSeconds: Int = 0,
    val isRequiredCompleted: Boolean = false,
    val isFinished: Boolean = false,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long = 0L
)

/**
 * Focus mode sessions (V3).
 */
@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetDurationMinutes: Int = 15,
    val actualElapsedSeconds: Int = 0,
    val focusActivityTitle: String = "قراءة هادئة وتركيز",
    val completedSuccessfully: Boolean = false,
    val interrupted: Boolean = false,
    val blockingLevel: Int = 0, // 0 = timer only, 1 = usage monitoring, 2 = system restrictions
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long = 0L
)

/**
 * Blocked/distracting apps chosen by the user for focus mode (V3).
 */
@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isBlocked: Boolean = true,
    val category: String = "SOCIAL_OR_ENTERTAINMENT",
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Real alarms & notifications reminder entity (V3).
 */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val triggerTimeMillis: Long,
    val timeHint: String,
    val category: String = "GENERAL", // MEDICINE, APPOINTMENT, WATER, READING, ROUTINE
    val isRecurring: Boolean = false,
    val recurrenceRule: String? = null, // DAILY, WEEKLY
    val isTriggered: Boolean = false,
    val isCancelled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
