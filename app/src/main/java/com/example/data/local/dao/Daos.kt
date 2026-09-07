package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ConceptProgressEntity
import com.example.data.local.entity.DailyTaskEntity
import com.example.data.local.entity.FrenchWordEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.ProfileEntity
import com.example.data.local.entity.StoryChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM mother_profile WHERE id = 1 LIMIT 1")
    fun getProfileFlow(): Flow<ProfileEntity?>

    @Query("SELECT * FROM mother_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: ProfileEntity)
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM long_term_memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM long_term_memories WHERE category = :category ORDER BY timestamp DESC")
    fun getMemoriesByCategory(category: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM long_term_memories ORDER BY importance DESC, timestamp DESC LIMIT :limit")
    suspend fun getTopMemories(limit: Int): List<MemoryEntity>

    @Query("SELECT * FROM long_term_memories ORDER BY timestamp DESC")
    suspend fun getAllMemoriesList(): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Query("DELETE FROM long_term_memories WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    @Query("DELETE FROM long_term_memories")
    suspend fun clearAllMemories()
}

@Dao
interface StoryDao {
    @Query("SELECT * FROM story_chapters ORDER BY chapterNumber ASC")
    fun getAllChapters(): Flow<List<StoryChapterEntity>>

    @Query("SELECT * FROM story_chapters ORDER BY chapterNumber ASC")
    suspend fun getAllChaptersList(): List<StoryChapterEntity>

    @Query("SELECT * FROM story_chapters WHERE chapterNumber = :number LIMIT 1")
    suspend fun getChapter(number: Int): StoryChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<StoryChapterEntity>)

    @Query("UPDATE story_chapters SET isCompleted = 1 WHERE chapterNumber = :chapterNumber")
    suspend fun markChapterCompleted(chapterNumber: Int)

    @Query("UPDATE story_chapters SET isUnlocked = 1 WHERE chapterNumber = :chapterNumber")
    suspend fun unlockChapter(chapterNumber: Int)
}

@Dao
interface DailyTaskDao {
    @Query("SELECT * FROM daily_tasks ORDER BY isCompleted ASC, isPriority DESC, id ASC")
    fun getAllTasks(): Flow<List<DailyTaskEntity>>

    @Query("SELECT * FROM daily_tasks ORDER BY isCompleted ASC, isPriority DESC, id ASC")
    suspend fun getAllTasksList(): List<DailyTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DailyTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<DailyTaskEntity>)

    @Query("UPDATE daily_tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun setTaskCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM daily_tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)
}

@Dao
interface FrenchWordDao {
    @Query("SELECT * FROM french_words ORDER BY id ASC")
    fun getAllWords(): Flow<List<FrenchWordEntity>>

    @Query("SELECT * FROM french_words ORDER BY id ASC")
    suspend fun getAllWordsList(): List<FrenchWordEntity>

    @Query("SELECT * FROM french_words WHERE id = :id LIMIT 1")
    suspend fun getWordById(id: Int): FrenchWordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<FrenchWordEntity>)

    @Query("UPDATE french_words SET isMastered = :isMastered WHERE id = :id")
    suspend fun setMastered(id: Int, isMastered: Boolean)
}

@Dao
interface LearningProgressDao {
    @Query("SELECT * FROM concept_progress")
    fun getAllProgressFlow(): Flow<List<ConceptProgressEntity>>

    @Query("SELECT * FROM concept_progress")
    suspend fun getAllProgressList(): List<ConceptProgressEntity>

    @Query("SELECT * FROM concept_progress WHERE conceptKey = :key LIMIT 1")
    suspend fun getProgress(key: String): ConceptProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: ConceptProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ConceptProgressEntity>)
}
