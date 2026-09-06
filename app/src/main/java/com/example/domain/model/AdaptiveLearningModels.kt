package com.example.domain.model

enum class MasteryStatus(val arabicLabel: String) {
    UNDERSTOOD("مفهوم ومتقن 🌸"),
    PARTIALLY_UNDERSTOOD("مفهوم جزئياً يحتاج مثال 🌿"),
    NOT_UNDERSTOOD("يحتاج إعادة شرح من الصفر 🌷")
}

data class LearningNode(
    val conceptKey: String,
    val conceptNameArabic: String,
    val conceptNameTunisian: String,
    val prerequisiteConcepts: List<String> = emptyList(),
    val explanationLevel: Int = 1, // 1 to 5
    val mastery: MasteryStatus = MasteryStatus.NOT_UNDERSTOOD,
    val reviewNeeded: Boolean = false,
    val confidenceScore: Float = 0.0f,
    val lastReviewedAt: Long = System.currentTimeMillis()
)

object MedicalCurriculumTree {
    val NODES = listOf(
        LearningNode(
            conceptKey = "BODY",
            conceptNameArabic = "جسم الإنسان",
            conceptNameTunisian = "بدن الإنسان والنعمة الربانية",
            prerequisiteConcepts = emptyList(),
            explanationLevel = 1,
            mastery = MasteryStatus.UNDERSTOOD,
            confidenceScore = 1.0f
        ),
        LearningNode(
            conceptKey = "CELL",
            conceptNameArabic = "الخلية",
            conceptNameTunisian = "الخلية: الوحدة الأساسية للحياة في البدن",
            prerequisiteConcepts = listOf("BODY"),
            explanationLevel = 1,
            mastery = MasteryStatus.PARTIALLY_UNDERSTOOD,
            confidenceScore = 0.75f
        ),
        LearningNode(
            conceptKey = "CELL_MEMBRANE",
            conceptNameArabic = "غشاء الخلية",
            conceptNameTunisian = "غشاء الخلية: حارس الباب اللي يدخل ويخرج بحكمة",
            prerequisiteConcepts = listOf("CELL"),
            explanationLevel = 2,
            mastery = MasteryStatus.UNDERSTOOD,
            confidenceScore = 0.9f
        ),
        LearningNode(
            conceptKey = "NUCLEUS",
            conceptNameArabic = "نواة الخلية",
            conceptNameTunisian = "نواة الخلية: عقل الخلية ومكتبة أسرارها",
            prerequisiteConcepts = listOf("CELL"),
            explanationLevel = 2,
            mastery = MasteryStatus.NOT_UNDERSTOOD,
            confidenceScore = 0.3f
        ),
        LearningNode(
            conceptKey = "DNA",
            conceptNameArabic = "الحمض النووي (DNA)",
            conceptNameTunisian = "الـ DNA: كتيب التعليمات اللي يحفظ صفاتك ووراثتك",
            prerequisiteConcepts = listOf("NUCLEUS"),
            explanationLevel = 3,
            mastery = MasteryStatus.NOT_UNDERSTOOD,
            confidenceScore = 0.0f
        ),
        LearningNode(
            conceptKey = "PROTEIN",
            conceptNameArabic = "البروتينات الحيوية",
            conceptNameTunisian = "البروتينات: عمال البناء والترميم في كل عضو",
            prerequisiteConcepts = listOf("DNA"),
            explanationLevel = 3,
            mastery = MasteryStatus.NOT_UNDERSTOOD,
            confidenceScore = 0.0f
        ),
        LearningNode(
            conceptKey = "TISSUES",
            conceptNameArabic = "الأنسجة الحيوية",
            conceptNameTunisian = "الأنسجة: مجموعة خلايا تتضامن باش تخدم خدمة واحدة",
            prerequisiteConcepts = listOf("CELL", "CELL_MEMBRANE"),
            explanationLevel = 4,
            mastery = MasteryStatus.NOT_UNDERSTOOD,
            confidenceScore = 0.0f
        ),
        LearningNode(
            conceptKey = "ORGANS",
            conceptNameArabic = "الأعضاء (القلب والشرايين)",
            conceptNameTunisian = "الأعضاء: القلب والدورة الدموية اللي توصل الحياة لكل خلية",
            prerequisiteConcepts = listOf("TISSUES"),
            explanationLevel = 5,
            mastery = MasteryStatus.NOT_UNDERSTOOD,
            confidenceScore = 0.0f
        )
    )

    fun getNode(key: String): LearningNode? = NODES.find { it.conceptKey.equals(key, ignoreCase = true) }

    fun canUnlock(key: String, masteredKeys: Set<String>): Boolean {
        val node = getNode(key) ?: return false
        return node.prerequisiteConcepts.all { it in masteredKeys }
    }
}
