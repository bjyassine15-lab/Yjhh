package com.example.domain.model

data class MotherProfile(
    val identity: Identity = Identity(),
    val learning: LearningProfile = LearningProfile(),
    val health: HealthProfile = HealthProfile(),
    val daily: DailyProfile = DailyProfile(),
    val preferences: UserPreferences = UserPreferences()
)

data class Identity(
    val name: String = "أمي الحبيبة",
    val age: Int = 53,
    val preferredLanguage: String = "العربية (اللهجة التونسية المبسطة)",
    val speakingStyle: String = "دافئ، هادئ، محترم ومبسط"
)

data class LearningProfile(
    val medicineKnowledgeLevel: String = "مبتدئة بشغف وفضول",
    val masteredConcepts: List<String> = listOf("مفهوم الخلية كوحدة حياة"),
    val conceptsNeedingReview: List<String> = emptyList(),
    val conceptsInProgress: List<String> = listOf("الغشاء الخلوي والميتوكوندريا"),
    val scienceLevel: String = "معرفة عامة جيدة",
    val frenchLevel: String = "تونسية يومية",
    val preferredExplanationType: String = "قصص، تشبيهات من الواقع وأمثلة ملموسة",
    val lovedStories: List<String> = listOf("رحلة سارة في عالم الطب"),
    val dislikedTopics: List<String> = emptyList(),
    val learningSpeed: String = "هادئة ومريحة",
    val lastLessonTitle: String = "الخلية: مدينة الحياة الصغيرة",
    val lastChapterNumber: Int = 1,
    val progressPercent: Int = 18
)

data class HealthProfile(
    val heightCm: Float = 162f,
    val weightKg: Float = 68f,
    val dailyActivity: String = "مشي خفيف وأعمال البيت اليومية",
    val sleepQuality: String = "تحتاج تحسين، نوم متقطع أحيانًا",
    val habits: List<String> = listOf("شرب فنجان شاي أخضر صباحًا", "المشي في الحي بعد العصر"),
    val diagnosedConditions: List<String> = listOf("ضغط الدم (مستقر تحت المتابعة)"),
    val medications: List<String> = listOf("دواء الضغط (حبة واحدة صباحًا بعد الفطور)"),
    val allergies: List<String> = listOf("لا توجد حساسيات معروفة"),
    val doctorAppointments: List<String> = listOf("موعد متابعة روتيني مع طبيب العائلة الشهر القادم"),
    val healthGoals: List<String> = listOf("شرب 1.5 لتر ماء يوميًا", "نوم مريح وهادئ 7 ساعات"),
    val healthNotes: List<String> = listOf("تفضل تقليل الملح في الماكلة التونسية")
)

data class DailyProfile(
    val activeTimes: String = "بين 9:00 صباحًا و 12:00، ثم 17:00 إلى 20:00",
    val studyTime: String = "المساء، وقت الهدوء بعد المغرب (حوالي الساعة 18:30)",
    val restTimes: String = "القيلولة بعد صلاة الظهر، والراحة بعد العشاء"
)

data class UserPreferences(
    val prefersVoice: Boolean = true,
    val sessionDurationMinutes: Int = 7,
    val favoriteStoryType: String = "قصص إنسانية واقعية ومبسطة",
    val preferredLearningPace: String = "خفيف وبسيط بدون ضغط"
)
