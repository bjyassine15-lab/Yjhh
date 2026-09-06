package com.example.domain.model

enum class MemoryCategory(val arabicLabel: String) {
    PERSONAL("معلومة عن أمي"),
    HEALTH("صحة وعافية"),
    LEARNING("تقدم في التعلم"),
    PREFERENCE("تفضيل شخصي"),
    DAILY_ROUTINE("روتين يومي"),
    FRENCH("تعلم الفرنسية"),
    STORY("مسار الرواية"),
    // Backward compatibility aliases
    USER_FACT("معلومة عن أمي"),
    HEALTH_DATA("صحة وعافية"),
    LEARNING_PROGRESS("تقدم في التعلم"),
    STORY_PROGRESS("مسار الرواية")
}

data class MemoryItem(
    val id: Long = 0,
    val category: MemoryCategory,
    val content: String,
    val importance: Int = 3, // 1 to 5
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val source: String = "محادثة صوتية",
    val confidence: Float = 0.9f
)
