package com.example.data.repository

import com.example.data.local.dao.ContentDao
import com.example.data.local.dao.FocusDao
import com.example.data.local.dao.HealthDao
import com.example.data.local.dao.ReminderDao
import com.example.data.local.dao.RoutineDao
import com.example.data.local.entity.BlockedAppEntity
import com.example.data.local.entity.ContentItemEntity
import com.example.data.local.entity.FocusSessionEntity
import com.example.data.local.entity.HabitEntity
import com.example.data.local.entity.HealthObservationEntity
import com.example.data.local.entity.HealthProfileEntity
import com.example.data.local.entity.MicroSessionEntity
import com.example.data.local.entity.ReadingSessionEntity
import com.example.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

class HealthRepository(private val healthDao: HealthDao) {
    fun getHealthProfileFlow(): Flow<HealthProfileEntity?> = healthDao.getHealthProfileFlow()
    suspend fun getHealthProfile(): HealthProfileEntity? = healthDao.getHealthProfile()
    suspend fun saveHealthProfile(profile: HealthProfileEntity) = healthDao.insertOrUpdateHealthProfile(profile)

    fun getObservationsFlow(): Flow<List<HealthObservationEntity>> = healthDao.getAllObservationsFlow()
    suspend fun getRecentObservations(limit: Int = 10): List<HealthObservationEntity> = healthDao.getRecentObservations(limit)
    suspend fun addObservation(text: String, category: String = "WELLNESS"): Long {
        return healthDao.insertObservation(
            HealthObservationEntity(
                observationText = text,
                category = category,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun getHabitsFlow(): Flow<List<HabitEntity>> = healthDao.getAllHabitsFlow()
    suspend fun addHabit(title: String, timeHint: String): Long {
        return healthDao.insertHabit(
            HabitEntity(
                title = title,
                timeHint = timeHint
            )
        )
    }
}

class RoutineRepository(private val routineDao: RoutineDao) {
    fun getMicroSessionsFlow(): Flow<List<MicroSessionEntity>> = routineDao.getMicroSessionsFlow()
    suspend fun getActiveMicroSessions(): List<MicroSessionEntity> = routineDao.getActiveMicroSessions()
    suspend fun getSessionsForDate(dateKey: String): List<MicroSessionEntity> = routineDao.getSessionsForDate(dateKey)
    suspend fun saveSessions(sessions: List<MicroSessionEntity>) = routineDao.insertOrUpdateSessions(sessions)
    suspend fun updateStatus(id: String, status: String) = routineDao.updateStatus(id, status)
    suspend fun markStarted(id: String) = routineDao.markStarted(id)
    suspend fun markCompleted(id: String) = routineDao.markCompleted(id, true)
    suspend fun markSkipped(id: String) = routineDao.markSkipped(id)
    suspend fun markRescheduled(id: String, newTimeHint: String) = routineDao.markRescheduled(id, newTimeHint)
}

class ContentRepository(private val contentDao: ContentDao) {
    fun getAllContentFlow(): Flow<List<ContentItemEntity>> = contentDao.getAllContentFlow()
    suspend fun getContentById(id: String): ContentItemEntity? = contentDao.getContentById(id)
    suspend fun getContentByCategory(cat: String): List<ContentItemEntity> = contentDao.getContentByCategory(cat)
    suspend fun insertContentItems(items: List<ContentItemEntity>) = contentDao.insertContentItems(items)

    fun getLatestReadingSessionFlow(): Flow<ReadingSessionEntity?> = contentDao.getLatestReadingSessionFlow()
    suspend fun startReadingSession(contentId: String, title: String, requiredSecs: Int = 600): Long {
        return contentDao.insertReadingSession(
            ReadingSessionEntity(
                contentId = contentId,
                contentTitle = title,
                requiredDurationSeconds = requiredSecs,
                startedAt = System.currentTimeMillis()
            )
        )
    }
    suspend fun updateReadingSession(session: ReadingSessionEntity) = contentDao.updateReadingSession(session)

    suspend fun recordReadingProgress(
        contentId: String,
        elapsedSeconds: Int,
        isCompleted: Boolean,
        customRequiredSecs: Int? = null
    ) {
        val session = contentDao.getLatestReadingSession()
        val targetReq = customRequiredSecs ?: session?.requiredDurationSeconds ?: 600
        val reqSecs = elapsedSeconds.coerceAtMost(targetReq)
        val optSecs = (elapsedSeconds - targetReq).coerceAtLeast(0)
        val reqCompleted = isCompleted || elapsedSeconds >= targetReq

        if (session != null && session.contentId == contentId) {
            val updated = session.copy(
                requiredDurationSeconds = targetReq,
                elapsedRequiredSeconds = reqSecs,
                elapsedOptionalSeconds = optSecs,
                isRequiredCompleted = reqCompleted,
                isFinished = isCompleted,
                finishedAt = if (isCompleted) System.currentTimeMillis() else 0L
            )
            contentDao.updateReadingSession(updated)
        } else {
            contentDao.insertReadingSession(
                ReadingSessionEntity(
                    contentId = contentId,
                    contentTitle = "قراءة هادئة",
                    requiredDurationSeconds = targetReq,
                    elapsedRequiredSeconds = reqSecs,
                    elapsedOptionalSeconds = optSecs,
                    isRequiredCompleted = reqCompleted,
                    isFinished = isCompleted,
                    finishedAt = if (isCompleted) System.currentTimeMillis() else 0L
                )
            )
        }
    }
}

class FocusRepository(private val focusDao: FocusDao) {
    fun getAllSessionsFlow(): Flow<List<FocusSessionEntity>> = focusDao.getAllFocusSessionsFlow()
    fun getActiveFocusSessionFlow(): Flow<FocusSessionEntity?> = focusDao.getActiveFocusSessionFlow()
    suspend fun getActiveFocusSession(): FocusSessionEntity? = focusDao.getActiveFocusSession()

    suspend fun startFocusSession(targetMinutes: Int, activityTitle: String, level: Int): Long {
        return focusDao.insertFocusSession(
            FocusSessionEntity(
                targetDurationMinutes = targetMinutes,
                focusActivityTitle = activityTitle,
                blockingLevel = level,
                startedAt = System.currentTimeMillis()
            )
        )
    }
    suspend fun completeFocusSession(
        id: Long,
        actualSecs: Int,
        completedSuccessfully: Boolean = true,
        interrupted: Boolean = false
    ) {
        val session = focusDao.getActiveFocusSession()
        if (session != null && (id == 0L || session.id == id)) {
            val updated = session.copy(
                completedSuccessfully = completedSuccessfully,
                interrupted = interrupted,
                actualElapsedSeconds = actualSecs,
                endedAt = System.currentTimeMillis()
            )
            focusDao.updateFocusSession(updated)
        }
    }
    suspend fun updateFocusSession(session: FocusSessionEntity) = focusDao.updateFocusSession(session)

    fun getBlockedAppsFlow(): Flow<List<BlockedAppEntity>> = focusDao.getAllBlockedAppsFlow()
    suspend fun getActiveBlockedApps(): List<BlockedAppEntity> = focusDao.getActiveBlockedApps()
    suspend fun addBlockedApp(pkg: String, name: String) = focusDao.insertBlockedApp(BlockedAppEntity(packageName = pkg, appName = name))
    suspend fun setAppBlocked(pkg: String, blocked: Boolean) = focusDao.setBlocked(pkg, blocked)
}

class ReminderRepository(private val reminderDao: ReminderDao) {
    fun getAllActiveRemindersFlow(): Flow<List<ReminderEntity>> = reminderDao.getAllActiveRemindersFlow()
    suspend fun getUpcomingReminders(fromTime: Long = System.currentTimeMillis()): List<ReminderEntity> = reminderDao.getUpcomingReminders(fromTime)
    suspend fun addReminder(
        title: String,
        triggerMillis: Long,
        timeHint: String,
        category: String = "GENERAL",
        isRecurring: Boolean = false,
        recurrenceRule: String? = null
    ): Long {
        return reminderDao.insertReminder(
            ReminderEntity(
                title = title,
                triggerTimeMillis = triggerMillis,
                timeHint = timeHint,
                category = category,
                isRecurring = isRecurring,
                recurrenceRule = recurrenceRule
            )
        )
    }
    suspend fun cancelReminder(id: Long) = reminderDao.cancelReminder(id)
    suspend fun markTriggered(id: Long) = reminderDao.markTriggered(id)
}
