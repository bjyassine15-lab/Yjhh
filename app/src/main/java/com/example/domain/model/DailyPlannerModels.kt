package com.example.domain.model

enum class TaskCategory(val labelArabic: String, val iconName: String) {
    MEDICINE("دواء", "pill"),
    HEALTH_HABIT("صحة وراحة", "heart"),
    STUDY("تعلم واستماع", "book"),
    REST("قيلولة واسترخاء", "spa"),
    APPOINTMENT("موعد طبي", "event")
}

data class DailyTask(
    val id: Long = 0,
    val title: String,
    val category: TaskCategory,
    val timeHint: String,
    val isCompleted: Boolean = false,
    val note: String = "",
    val isPriority: Boolean = false
)
