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
    suspend fun saveSessions(sessions: List<MicroSessionEntity>) = routineDao.insertOrUpdateSessions(sessions)
    suspend fun markCompleted(id: String) = routineDao.markCompleted(id, true)
    suspend fun markSkipped(id: String) = routineDao.markSkipped(id)
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
}

class FocusRepository(private val focusDao: FocusDao) {
    fun getAllSessionsFlow(): Flow<List<FocusSessionEntity>> = focusDao.getAllFocusSessionsFlow()
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
    suspend fun updateFocusSession(session: FocusSessionEntity) = focusDao.updateFocusSession(session)

    fun getBlockedAppsFlow(): Flow<List<BlockedAppEntity>> = focusDao.getAllBlockedAppsFlow()
    suspend fun getActiveBlockedApps(): List<BlockedAppEntity> = focusDao.getActiveBlockedApps()
    suspend fun addBlockedApp(pkg: String, name: String) = focusDao.insertBlockedApp(BlockedAppEntity(packageName = pkg, appName = name))
    suspend fun setAppBlocked(pkg: String, blocked: Boolean) = focusDao.setBlocked(pkg, blocked)
}

class ReminderRepository(private val reminderDao: ReminderDao) {
    fun getAllActiveRemindersFlow(): Flow<List<ReminderEntity>> = reminderDao.getAllActiveRemindersFlow()
    suspend fun getUpcomingReminders(fromTime: Long = System.currentTimeMillis()): List<ReminderEntity> = reminderDao.getUpcomingReminders(fromTime)
    suspend fun addReminder(title: String, triggerMillis: Long, timeHint: String, category: String = "GENERAL"): Long {
        return reminderDao.insertReminder(
            ReminderEntity(
                title = title,
                triggerTimeMillis = triggerMillis,
                timeHint = timeHint,
                category = category
            )
        )
    }
    suspend fun cancelReminder(id: Long) = reminderDao.cancelReminder(id)
    suspend fun markTriggered(id: Long) = reminderDao.markTriggered(id)
}
