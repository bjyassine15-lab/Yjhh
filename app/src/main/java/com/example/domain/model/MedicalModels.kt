package com.example.domain.model

data class StoryChapter(
    val id: Int,
    val chapterNumber: Int,
    val title: String,
    val hook: String,
    val storyBody: String,
    val scientificConceptKey: String,
    val tunisianAudioScript: String,
    val isUnlocked: Boolean = true,
    val isCompleted: Boolean = false,
    val characterNote: String = "سارة، امرأة شغوفة تبحث عن فهم سر الحياة في جسم الإنسان"
)

data class ProgressiveDepthLevel(
    val levelNumber: Int, // 1 to 5
    val levelTitle: String,
    val explanationTunisian: String,
    val scientificTermArabic: String,
    val scientificTermFrench: String = "",
    val realLifeAnalogy: String
)

data class MedicalConcept(
    val id: String,
    val nameArabic: String,
    val nameTunisian: String,
    val scientificName: String,
    val summary: String,
    val levels: List<ProgressiveDepthLevel>,
    val visualDiagramType: String // "CELL", "HEART", "BLOOD_VESSEL"
)

data class QuizQuestion(
    val id: String,
    val questionTunisian: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanationTunisianIfCorrect: String,
    val explanationTunisianIfIncorrect: String
)
