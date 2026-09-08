package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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

@Dao
interface HealthDao {
    @Query("SELECT * FROM health_profiles WHERE id = 1")
    fun getHealthProfileFlow(): Flow<HealthProfileEntity?>

    @Query("SELECT * FROM health_profiles WHERE id = 1")
    suspend fun getHealthProfile(): HealthProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateHealthProfile(profile: HealthProfileEntity)

    @Query("SELECT * FROM health_observations ORDER BY timestamp DESC")
    fun getAllObservationsFlow(): Flow<List<HealthObservationEntity>>

    @Query("SELECT * FROM health_observations ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentObservations(limit: Int = 10): List<HealthObservationEntity>

    @Insert
    suspend fun insertObservation(observation: HealthObservationEntity): Long

    @Query("SELECT * FROM wellness_habits ORDER BY id ASC")
    fun getAllHabitsFlow(): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM micro_sessions ORDER BY priority ASC")
    fun getMicroSessionsFlow(): Flow<List<MicroSessionEntity>>

    @Query("SELECT * FROM micro_sessions WHERE isCompleted = 0 AND isSkipped = 0 ORDER BY priority ASC")
    suspend fun getActiveMicroSessions(): List<MicroSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSessions(sessions: List<MicroSessionEntity>)

    @Query("UPDATE micro_sessions SET isCompleted = :completed, completedAt = :time WHERE id = :id")
    suspend fun markCompleted(id: String, completed: Boolean, time: Long = System.currentTimeMillis())

    @Query("UPDATE micro_sessions SET isSkipped = 1 WHERE id = :id")
    suspend fun markSkipped(id: String)
}

@Dao
interface ContentDao {
    @Query("SELECT * FROM content_items")
    fun getAllContentFlow(): Flow<List<ContentItemEntity>>

    @Query("SELECT * FROM content_items WHERE id = :id")
    suspend fun getContentById(id: String): ContentItemEntity?

    @Query("SELECT * FROM content_items WHERE category = :category")
    suspend fun getContentByCategory(category: String): List<ContentItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContentItems(items: List<ContentItemEntity>)

    @Query("SELECT * FROM reading_sessions ORDER BY id DESC LIMIT 1")
    fun getLatestReadingSessionFlow(): Flow<ReadingSessionEntity?>

    @Insert
    suspend fun insertReadingSession(session: ReadingSessionEntity): Long

    @Update
    suspend fun updateReadingSession(session: ReadingSessionEntity)
}

@Dao
interface FocusDao {
    @Query("SELECT * FROM focus_sessions ORDER BY id DESC")
    fun getAllFocusSessionsFlow(): Flow<List<FocusSessionEntity>>

    @Insert
    suspend fun insertFocusSession(session: FocusSessionEntity): Long

    @Update
    suspend fun updateFocusSession(session: FocusSessionEntity)

    @Query("SELECT * FROM blocked_apps")
    fun getAllBlockedAppsFlow(): Flow<List<BlockedAppEntity>>

    @Query("SELECT * FROM blocked_apps WHERE isBlocked = 1")
    suspend fun getActiveBlockedApps(): List<BlockedAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedApp(app: BlockedAppEntity)

    @Query("UPDATE blocked_apps SET isBlocked = :blocked WHERE packageName = :packageName")
    suspend fun setBlocked(packageName: String, blocked: Boolean)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isCancelled = 0 ORDER BY triggerTimeMillis ASC")
    fun getAllActiveRemindersFlow(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isCancelled = 0 AND isTriggered = 0 AND triggerTimeMillis >= :fromTime ORDER BY triggerTimeMillis ASC")
    suspend fun getUpcomingReminders(fromTime: Long): List<ReminderEntity>

    @Insert
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET isCancelled = 1 WHERE id = :id")
    suspend fun cancelReminder(id: Long)

    @Query("UPDATE reminders SET isTriggered = 1 WHERE id = :id")
    suspend fun markTriggered(id: Long)
}
