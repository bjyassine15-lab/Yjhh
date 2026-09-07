package com.example.ai.learning

import com.example.data.repository.LearningProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Mastery states for educational concepts in Rafiqah V2.1.
 */
enum class ConceptMastery {
    NOT_STARTED,
    LEARNING,
    PARTIAL,
    MASTERED,
    REVIEW
}

enum class EvaluationResult {
    CORRECT,
    PARTIAL,
    INCORRECT
}

data class ConceptProgress(
    val conceptKey: String,
    var currentLevel: Int = 1, // 1 to 5
    var mastery: ConceptMastery = ConceptMastery.NOT_STARTED,
    var needsReview: Boolean = false,
    var attempts: Int = 0,
    var successfulAttempts: Int = 0,
    var lastReviewed: Long = System.currentTimeMillis()
)

data class AdaptiveQuestion(
    val id: String,
    val conceptKey: String,
    val questionTunisian: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanationTunisian: String
)

/**
 * Adaptive Learning Engine for Rafiqah V2.5.
 * Adjusts explanation depth levels and dynamically updates concept mastery with Room persistence.
 */
class LearningEngine(
    private val repository: LearningProgressRepository? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val conceptsProgress = mutableMapOf<String, ConceptProgress>()

    init {
        // Initialize standard body/cell concepts
        registerConcept("cell", "الخلية", 1)
        registerConcept("membrane", "غشاء الخلية", 1)
        registerConcept("nucleus", "نواة الخلية وDNA", 1)
        registerConcept("mitochondria", "الميتوكوندريا والطاقة", 1)

        repository?.let { repo ->
            scope.launch {
                loadFromRepository(repo)
            }
        }
    }

    suspend fun loadFromRepository(repo: LearningProgressRepository) {
        val list = repo.getAllProgressList()
        list.forEach { entity ->
            val mastery = try {
                ConceptMastery.valueOf(entity.mastery)
            } catch (_: Exception) {
                ConceptMastery.NOT_STARTED
            }
            conceptsProgress[entity.conceptKey] = ConceptProgress(
                conceptKey = entity.conceptKey,
                currentLevel = entity.currentLevel,
                mastery = mastery,
                needsReview = entity.needsReview,
                attempts = entity.attempts,
                successfulAttempts = entity.successfulAttempts,
                lastReviewed = entity.lastReviewed
            )
        }
    }

    private fun persistProgress(progress: ConceptProgress) {
        repository?.let { repo ->
            scope.launch {
                repo.saveProgress(
                    conceptKey = progress.conceptKey,
                    currentLevel = progress.currentLevel,
                    mastery = progress.mastery.name,
                    needsReview = progress.needsReview,
                    attempts = progress.attempts,
                    successfulAttempts = progress.successfulAttempts
                )
            }
        }
    }

    fun registerConcept(key: String, label: String, initialLevel: Int = 1) {
        val existing = conceptsProgress[key]
        if (existing == null) {
            val created = ConceptProgress(conceptKey = key, currentLevel = initialLevel)
            conceptsProgress[key] = created
            persistProgress(created)
        } else {
            existing.currentLevel = initialLevel
            persistProgress(existing)
        }
    }

    fun getConceptProgress(key: String): ConceptProgress {
        return conceptsProgress.getOrPut(key) {
            val created = ConceptProgress(conceptKey = key, currentLevel = 1)
            persistProgress(created)
            created
        }
    }

    fun getAllConcepts(): List<ConceptProgress> = conceptsProgress.values.toList()

    fun onUserConfused(conceptKey: String) {
        val progress = getConceptProgress(conceptKey)
        progress.needsReview = true
        progress.attempts += 1
        progress.currentLevel = (progress.currentLevel - 1).coerceAtLeast(1)
        progress.mastery = ConceptMastery.REVIEW
        progress.lastReviewed = System.currentTimeMillis()
        persistProgress(progress)
    }

    fun onUserUnderstood(conceptKey: String) {
        val progress = getConceptProgress(conceptKey)
        progress.needsReview = false
        progress.attempts += 1
        progress.successfulAttempts += 1
        progress.currentLevel = (progress.currentLevel + 1).coerceAtMost(5)
        progress.mastery = if (progress.currentLevel >= 4) ConceptMastery.MASTERED else ConceptMastery.LEARNING
        progress.lastReviewed = System.currentTimeMillis()
        persistProgress(progress)
    }

    fun getLevelDescription(level: Int): String {
        return when (level) {
            1 -> "Level 1: تشبيه من الحياة اليومية والدار التونسية 🏠"
            2 -> "Level 2: شرح علمي مبسط وميسر 🔬"
            3 -> "Level 3: المصطلح العلمي بالفرنسية والعربية 📖"
            4 -> "Level 4: علاقة المفهوم ببقية أعضاء الجسم 🫀"
            5 -> "Level 5: تطبيق وحكمة بيولوجية عميقة 🌟"
            else -> "مستوى تمهيدي"
        }
    }

    fun generateComprehensionQuestion(conceptKey: String): AdaptiveQuestion {
        return when (conceptKey.lowercase()) {
            "cell", "الخلية" -> AdaptiveQuestion(
                id = "q_cell",
                conceptKey = conceptKey,
                questionTunisian = "يا أمي الغالية، الخلية في جسمنا كيفاش نشبهوها باش نبسطوها؟ 🌷",
                options = listOf(
                    "الياجورة الحية في حيط الدار اللي تبني كل عظم ولحم",
                    "حبة رمل ميتة ما فيها حتى حركة",
                    "دواء نشربوه مع الماء"
                ),
                correctIndex = 0,
                explanationTunisian = "يعطيك الصحة يا أمي! تماماً كالالياجورة الحية المتضامنة مع خياتها."
            )
            "membrane", "غشاء الخلية" -> AdaptiveQuestion(
                id = "q_membrane",
                conceptKey = conceptKey,
                questionTunisian = "غشاء الخلية شنوة خدمته الرئيسية يا أمي؟ 🛡️",
                options = listOf(
                    "حارس وباب ذكي يدخل الغذاء ويخرج الفضلات ويحمي الدار",
                    "مجرد حزام للزينة",
                    "يصنع العظام فقط"
                ),
                correctIndex = 0,
                explanationTunisian = "صحيح يا أمي، هو الحارس الذكي للخلية!"
            )
            else -> AdaptiveQuestion(
                id = "q_general",
                conceptKey = conceptKey,
                questionTunisian = "هل حسيتي المفهوم هذا واضح وسهل عليك يا أمي؟ 🌸",
                options = listOf(
                    "واضح وبسيط ويعطيك الصحة",
                    "نحب نزيدو نراجعوه بمثال آخر",
                    "نحب نسأل على حاجة أخرى"
                ),
                correctIndex = 0,
                explanationTunisian = "ربي يبارك في عقلك وفهمك يا أمي."
            )
        }
    }

    fun evaluateAnswer(question: AdaptiveQuestion, selectedIndex: Int): EvaluationResult {
        return if (selectedIndex == question.correctIndex) {
            onUserUnderstood(question.conceptKey)
            EvaluationResult.CORRECT
        } else {
            onUserConfused(question.conceptKey)
            EvaluationResult.INCORRECT
        }
    }
}
