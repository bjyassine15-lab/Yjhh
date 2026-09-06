package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mother_profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val age: Int,
    val preferredLanguage: String,
    val speakingStyle: String,
    val medicineKnowledgeLevel: String,
    val scienceLevel: String,
    val frenchLevel: String,
    val lastLessonTitle: String,
    val lastChapterNumber: Int,
    val progressPercent: Int,
    val heightCm: Float,
    val weightKg: Float,
    val dailyActivity: String,
    val sleepQuality: String,
    val studyTime: String,
    val restTimes: String,
    val sessionDurationMinutes: Int
)

@Entity(tableName = "long_term_memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // USER_FACT, HEALTH_DATA, LEARNING_PROGRESS, PREFERENCE, STORY_PROGRESS
    val content: String,
    val importance: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String
)

@Entity(tableName = "story_chapters")
data class StoryChapterEntity(
    @PrimaryKey val chapterNumber: Int,
    val title: String,
    val hook: String,
    val storyBody: String,
    val scientificConceptKey: String,
    val tunisianAudioScript: String,
    val isUnlocked: Boolean,
    val isCompleted: Boolean,
    val characterNote: String
)

@Entity(tableName = "daily_tasks")
data class DailyTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // MEDICINE, HEALTH_HABIT, STUDY, REST, APPOINTMENT
    val timeHint: String,
    val isCompleted: Boolean,
    val note: String,
    val isPriority: Boolean
)

@Entity(tableName = "french_words")
data class FrenchWordEntity(
    @PrimaryKey val id: Int,
    val frenchWord: String,
    val arabicPhonetics: String,
    val arabicMeaning: String,
    val tunisianEverydayContext: String,
    val medicalContext: String,
    val exampleDailySentence: String,
    val exampleMedicalSentence: String,
    val interactivePrompt: String,
    val isMastered: Boolean
)
