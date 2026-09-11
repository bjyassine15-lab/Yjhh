package com.example.domain.model

data class MotherProfile(
    val identity: Identity = Identity(),
    val learning: LearningProfile = LearningProfile(),
    val health: HealthProfile = HealthProfile(),
    val daily: DailyProfile = DailyProfile(),
    val preferences: UserPreferences = UserPreferences()
)

data class Identity(
    val name: String = "",
    val age: Int = 0, // 0 indicates not set yet
    val generalLocation: String = "",
    val preferredLanguage: String = "العربية (اللهجة التونسية المبسطة)",
    val speakingStyle: String = "دافئ، هادئ، محترم ومبسط"
)

data class LearningProfile(
    val medicineKnowledgeLevel: String = "مبتدئة بشغف وفضول",
    val masteredConcepts: List<String> = emptyList(),
    val conceptsNeedingReview: List<String> = emptyList(),
    val conceptsInProgress: List<String> = emptyList(),
    val scienceLevel: String = "معرفة عامة",
    val frenchLevel: String = "تونسية يومية",
    val learningGoals: List<String> = emptyList(),
    val learningInterests: List<String> = emptyList(),
    val preferredExplanationType: String = "قصص، تشبيهات من الواقع وأمثلة ملموسة",
    val lovedStories: List<String> = emptyList(),
    val dislikedTopics: List<String> = emptyList(),
    val learningSpeed: String = "هادئة ومريحة",
    val lastLessonTitle: String = "بداية الرحلة التعليمية",
    val lastChapterNumber: Int = 1,
    val progressPercent: Int = 0
)

data class HealthProfile(
    val heightCm: Float = 0f,
    val weightKg: Float = 0f,
    val dailyActivity: String = "",
    val sleepQuality: String = "",
    val habits: List<String> = emptyList(),
    val diagnosedConditions: List<String> = emptyList(),
    val medications: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
    val doctorAppointments: List<String> = emptyList(),
    val healthGoals: List<String> = emptyList(),
    val healthNotes: List<String> = emptyList()
)

data class DailyProfile(
    val activeTimes: String = "",
    val studyTime: String = "",
    val restTimes: String = ""
)

data class UserPreferences(
    val prefersVoice: Boolean = true,
    val sessionDurationMinutes: Int = 10,
    val readingPreferences: List<String> = emptyList(),
    val favoriteStoryType: String = "قصص إنسانية واقعية ومبسطة",
    val preferredLearningPace: String = "خفيف وبسيط بدون ضغط"
)

/**
 * Explicit DEMO profile used only for isolated UI previews or mock testing.
 * Never used as default user data.
 */
object DemoMotherProfile {
    val sample = MotherProfile(
        identity = Identity(name = "أمي الحبيبة (نموذج تجريبي)", age = 53),
        learning = LearningProfile(
            masteredConcepts = listOf("مفهوم الخلية كوحدة حياة"),
            progressPercent = 20
        ),
        health = HealthProfile(
            heightCm = 162f,
            weightKg = 68f,
            dailyActivity = "مشي خفيف وأعمال البيت",
            sleepQuality = "نوم متقطع أحياناً",
            habits = listOf("شرب شاي أخضر صباحاً"),
            healthGoals = listOf("شرب 1.5 لتر ماء يومياً")
        )
    )
}
