package com.example.data.repository

import com.example.data.local.dao.DailyTaskDao
import com.example.data.local.dao.FrenchWordDao
import com.example.data.local.dao.LearningProgressDao
import com.example.data.local.dao.MemoryDao
import com.example.data.local.dao.ProfileDao
import com.example.data.local.dao.StoryDao
import com.example.data.local.entity.ConceptProgressEntity
import com.example.data.local.entity.DailyTaskEntity
import com.example.data.local.entity.FrenchWordEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.ProfileEntity
import com.example.data.local.entity.StoryChapterEntity
import com.example.domain.model.DailyProfile
import com.example.domain.model.DailyTask
import com.example.domain.model.FrenchWordItem
import com.example.domain.model.HealthProfile
import com.example.domain.model.Identity
import com.example.domain.model.LearningProfile
import com.example.domain.model.MemoryCategory
import com.example.domain.model.MemoryItem
import com.example.domain.model.MotherProfile
import com.example.domain.model.StoryChapter
import com.example.domain.model.TaskCategory
import com.example.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepository(private val profileDao: ProfileDao) {
    val profileFlow: Flow<MotherProfile> = profileDao.getProfileFlow().map { entity ->
        entity?.toDomain() ?: MotherProfile()
    }

    suspend fun getProfile(): MotherProfile {
        return profileDao.getProfile()?.toDomain() ?: MotherProfile()
    }

    suspend fun updateProfile(profile: MotherProfile) {
        profileDao.insertOrUpdateProfile(profile.toEntity())
    }

    suspend fun updateLearningProgress(lastChapterNumber: Int, progressPercent: Int, lastLessonTitle: String) {
        val current = getProfile()
        val updated = current.copy(
            learning = current.learning.copy(
                lastChapterNumber = lastChapterNumber,
                progressPercent = progressPercent,
                lastLessonTitle = lastLessonTitle
            )
        )
        updateProfile(updated)
    }

    private fun ProfileEntity.toDomain(): MotherProfile {
        return MotherProfile(
            identity = Identity(
                name = name,
                age = age,
                preferredLanguage = preferredLanguage,
                speakingStyle = speakingStyle
            ),
            learning = LearningProfile(
                medicineKnowledgeLevel = medicineKnowledgeLevel,
                scienceLevel = scienceLevel,
                frenchLevel = frenchLevel,
                lastLessonTitle = lastLessonTitle,
                lastChapterNumber = lastChapterNumber,
                progressPercent = progressPercent
            ),
            health = HealthProfile(
                heightCm = heightCm,
                weightKg = weightKg,
                dailyActivity = dailyActivity,
                sleepQuality = sleepQuality
            ),
            daily = DailyProfile(
                studyTime = studyTime,
                restTimes = restTimes
            ),
            preferences = UserPreferences(
                sessionDurationMinutes = sessionDurationMinutes
            )
        )
    }

    private fun MotherProfile.toEntity(): ProfileEntity {
        return ProfileEntity(
            id = 1,
            name = identity.name,
            age = identity.age,
            preferredLanguage = identity.preferredLanguage,
            speakingStyle = identity.speakingStyle,
            medicineKnowledgeLevel = learning.medicineKnowledgeLevel,
            scienceLevel = learning.scienceLevel,
            frenchLevel = learning.frenchLevel,
            lastLessonTitle = learning.lastLessonTitle,
            lastChapterNumber = learning.lastChapterNumber,
            progressPercent = learning.progressPercent,
            heightCm = health.heightCm,
            weightKg = health.weightKg,
            dailyActivity = health.dailyActivity,
            sleepQuality = health.sleepQuality,
            studyTime = daily.studyTime,
            restTimes = daily.restTimes,
            sessionDurationMinutes = preferences.sessionDurationMinutes
        )
    }
}

class MemoryRepository(private val memoryDao: MemoryDao) {
    val allMemories: Flow<List<MemoryItem>> = memoryDao.getAllMemories().map { list ->
        list.map { it.toDomain() }
    }

    fun getMemoriesByCategory(category: MemoryCategory): Flow<List<MemoryItem>> {
        return memoryDao.getMemoriesByCategory(category.name).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun saveMemory(
        content: String,
        category: MemoryCategory,
        importance: Int = 3,
        source: String = "محادثة صوتية"
    ): Long {
        return saveMemoryWithDeduplication(content, category, importance, source)
    }

    suspend fun saveMemoryWithDeduplication(
        content: String,
        category: MemoryCategory,
        importance: Int = 3,
        source: String = "محادثة صوتية"
    ): Long {
        val existingList = memoryDao.getAllMemoriesList()
        val normalizedNew = content.trim().lowercase()

        // Deduplication check: if existing memory shares core keywords or exact meaning
        val existingMatch = existingList.find { entity ->
            val existingNormalized = entity.content.trim().lowercase()
            existingNormalized == normalizedNew ||
            (entity.category == category.name && isSemanticallySimilar(existingNormalized, normalizedNew))
        }

        return if (existingMatch != null) {
            val updated = existingMatch.copy(
                content = content,
                importance = maxOf(existingMatch.importance, importance),
                timestamp = System.currentTimeMillis(),
                source = source
            )
            memoryDao.updateMemory(updated)
            existingMatch.id
        } else {
            memoryDao.insertMemory(
                MemoryEntity(
                    category = category.name,
                    content = content,
                    importance = importance,
                    timestamp = System.currentTimeMillis(),
                    source = source
                )
            )
        }
    }

    private fun isSemanticallySimilar(text1: String, text2: String): Boolean {
        val words1 = text1.split(" ", "،", ".", "-").filter { it.length > 3 }.toSet()
        val words2 = text2.split(" ", "،", ".", "-").filter { it.length > 3 }.toSet()
        if (words1.isEmpty() || words2.isEmpty()) return false
        val intersection = words1.intersect(words2).size
        val minSize = minOf(words1.size, words2.size)
        return (intersection.toDouble() / minSize.toDouble()) >= 0.65
    }

    suspend fun getRelevantMemories(limit: Int = 5): List<MemoryItem> {
        return memoryDao.getTopMemories(limit).map { it.toDomain() }
    }

    suspend fun getAllMemoriesList(): List<MemoryItem> {
        return memoryDao.getAllMemoriesList().map { it.toDomain() }
    }

    suspend fun deleteMemory(id: Long) {
        memoryDao.deleteMemoryById(id)
    }

    private fun MemoryEntity.toDomain(): MemoryItem {
        val cat = try {
            MemoryCategory.valueOf(category)
        } catch (_: Exception) {
            MemoryCategory.PERSONAL
        }
        return MemoryItem(
            id = id,
            category = cat,
            content = content,
            importance = importance,
            createdAt = timestamp,
            updatedAt = timestamp,
            source = source,
            confidence = 0.95f
        )
    }
}

class StoryRepository(private val storyDao: StoryDao) {
    val allChapters: Flow<List<StoryChapter>> = storyDao.getAllChapters().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getChapter(number: Int): StoryChapter? {
        return storyDao.getChapter(number)?.toDomain()
    }

    suspend fun getAllChaptersList(): List<StoryChapter> {
        return storyDao.getAllChaptersList().map { it.toDomain() }
    }

    suspend fun completeChapter(chapterNumber: Int) {
        storyDao.markChapterCompleted(chapterNumber)
        // Auto-unlock next chapter
        storyDao.unlockChapter(chapterNumber + 1)
    }

    private fun StoryChapterEntity.toDomain(): StoryChapter {
        return StoryChapter(
            id = chapterNumber,
            chapterNumber = chapterNumber,
            title = title,
            hook = hook,
            storyBody = storyBody,
            scientificConceptKey = scientificConceptKey,
            tunisianAudioScript = tunisianAudioScript,
            isUnlocked = isUnlocked,
            isCompleted = isCompleted,
            characterNote = characterNote
        )
    }
}

class DailyPlannerRepository(private val dailyTaskDao: DailyTaskDao) {
    val allTasks: Flow<List<DailyTask>> = dailyTaskDao.getAllTasks().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getAllTasksList(): List<DailyTask> {
        return dailyTaskDao.getAllTasksList().map { it.toDomain() }
    }

    suspend fun setTaskCompleted(id: Long, completed: Boolean) {
        dailyTaskDao.setTaskCompleted(id, completed)
    }

    suspend fun addTask(
        title: String,
        category: TaskCategory,
        timeHint: String,
        note: String = "",
        isPriority: Boolean = false
    ): Long {
        return dailyTaskDao.insertTask(
            DailyTaskEntity(
                title = title,
                category = category.name,
                timeHint = timeHint,
                isCompleted = false,
                note = note,
                isPriority = isPriority
            )
        )
    }

    suspend fun deleteTask(id: Long) {
        dailyTaskDao.deleteTask(id)
    }

    private fun DailyTaskEntity.toDomain(): DailyTask {
        val cat = try {
            TaskCategory.valueOf(category)
        } catch (_: Exception) {
            TaskCategory.HEALTH_HABIT
        }
        return DailyTask(
            id = id,
            title = title,
            category = cat,
            timeHint = timeHint,
            isCompleted = isCompleted,
            note = note,
            isPriority = isPriority
        )
    }
}

class FrenchWordRepository(private val frenchWordDao: FrenchWordDao) {
    val allWords: Flow<List<FrenchWordItem>> = frenchWordDao.getAllWords().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getAllWordsList(): List<FrenchWordItem> {
        return frenchWordDao.getAllWordsList().map { it.toDomain() }
    }

    suspend fun setWordMastered(id: Int, mastered: Boolean) {
        frenchWordDao.setMastered(id, mastered)
    }

    suspend fun insertWord(entity: FrenchWordEntity) {
        frenchWordDao.insertWord(entity)
    }

    private fun FrenchWordEntity.toDomain(): FrenchWordItem {
        return FrenchWordItem(
            id = id,
            frenchWord = frenchWord,
            arabicPhonetics = arabicPhonetics,
            arabicMeaning = arabicMeaning,
            tunisianEverydayContext = tunisianEverydayContext,
            medicalContext = medicalContext,
            exampleDailySentence = exampleDailySentence,
            exampleMedicalSentence = exampleMedicalSentence,
            interactivePrompt = interactivePrompt,
            isMastered = isMastered
        )
    }
}

class LearningProgressRepository(
    private val learningProgressDao: LearningProgressDao
) {
    fun getAllProgressFlow(): Flow<List<ConceptProgressEntity>> {
        return learningProgressDao.getAllProgressFlow()
    }

    suspend fun getAllProgressList(): List<ConceptProgressEntity> {
        return learningProgressDao.getAllProgressList()
    }

    suspend fun getProgress(conceptKey: String): ConceptProgressEntity? {
        return learningProgressDao.getProgress(conceptKey)
    }

    suspend fun saveProgress(
        conceptKey: String,
        currentLevel: Int,
        mastery: String,
        needsReview: Boolean,
        attempts: Int,
        successfulAttempts: Int
    ) {
        val entity = ConceptProgressEntity(
            conceptKey = conceptKey,
            currentLevel = currentLevel,
            mastery = mastery,
            needsReview = needsReview,
            attempts = attempts,
            successfulAttempts = successfulAttempts,
            lastReviewed = System.currentTimeMillis()
        )
        learningProgressDao.insertOrUpdate(entity)
    }
}
