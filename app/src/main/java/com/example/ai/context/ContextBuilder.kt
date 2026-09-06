package com.example.ai.context

import com.example.domain.model.DailyTask
import com.example.domain.model.MemoryCategory
import com.example.domain.model.MemoryItem
import com.example.domain.model.MotherProfile
import com.example.domain.model.StoryChapter

/**
 * Intelligent Selective Context Builder for Rafiqah V2.1.
 * Constructs domain-specific prompt contexts dynamically.
 * Strictly avoids any hardcoded or fabricated health assumptions.
 */
class ContextBuilder {

    enum class QueryDomain {
        HEALTH,
        LEARNING,
        STORY,
        DAILY_PLANNER,
        FRENCH,
        GENERAL_GREETING
    }

    fun detectDomain(query: String): QueryDomain {
        val q = query.lowercase()
        return when {
            q.contains("وجيعة") || q.contains("دواء") || q.contains("تونسيو") || q.contains("طبيب") ||
            q.contains("ضغط") || q.contains("نوم") || q.contains("صحة") || q.contains("قلب") ||
            q.contains("سخانة") || q.contains("ماء") || q.contains("تعب") || q.contains("راسي") ||
            q.contains("مرض") || q.contains("تحليل") -> QueryDomain.HEALTH

            q.contains("قصة") || q.contains("رواية") || q.contains("سارة") || q.contains("فصل") ||
            q.contains("حكاية") || q.contains("وين وصلنا") -> QueryDomain.STORY

            q.contains("فرنسي") || q.contains("كلمة") || q.contains("فرنسية") || q.contains("نطق") ||
            q.contains("معنى") || q.contains("rendez-vous") || q.contains("ordonnance") -> QueryDomain.FRENCH

            q.contains("خلية") || q.contains("درس") || q.contains("تعلم") || q.contains("نواة") ||
            q.contains("غشاء") || q.contains("dna") || q.contains("علم") || q.contains("ميتوكوندريا") -> QueryDomain.LEARNING

            q.contains("نهاري") || q.contains("برنامج") || q.contains("موعد") || q.contains("مهمة") ||
            q.contains("اليوم") || q.contains("قضية") || q.contains("سجل") -> QueryDomain.DAILY_PLANNER

            else -> QueryDomain.GENERAL_GREETING
        }
    }

    fun buildRelevantContext(
        query: String,
        profile: MotherProfile,
        recentMemories: List<MemoryItem>,
        currentChapter: StoryChapter? = null,
        todayTasks: List<DailyTask> = emptyList(),
        currentLessonKey: String? = null
    ): String {
        val domain = detectDomain(query)
        val sb = StringBuilder()

        // 1. Identity
        sb.appendLine("=== هوية أمي الأساسية ===")
        sb.appendLine("الاسم: ${profile.identity.name}، العمر: ${profile.identity.age} سنة، اللغة المفضلة: ${profile.identity.preferredLanguage}.")

        // 2. Relevant Memories filtered by domain
        val filteredMemories = filterMemoriesForDomain(domain, recentMemories)
        if (filteredMemories.isNotEmpty()) {
            sb.appendLine("=== ذكريات وملاحظات سابقة ذات صلة ===")
            filteredMemories.forEach { mem ->
                sb.appendLine("- [${mem.category.arabicLabel}] ${mem.content}")
            }
        }

        // 3. Domain-specific context built strictly from documented facts
        when (domain) {
            QueryDomain.HEALTH -> {
                sb.appendLine("=== السياق الصحي المسجل لأمي ===")
                val healthFacts = mutableListOf<String>()

                if (profile.health.diagnosedConditions.isNotEmpty()) {
                    healthFacts.add("الحالات المشخصة المسجلة: ${profile.health.diagnosedConditions.joinToString("، ")}")
                }
                if (profile.health.medications.isNotEmpty()) {
                    healthFacts.add("الأدوية المعتادة المسجلة: ${profile.health.medications.joinToString("، ")}")
                }
                if (profile.health.sleepQuality.isNotBlank()) {
                    healthFacts.add("جودة النوم المبلغ عنها: ${profile.health.sleepQuality}")
                }
                if (profile.health.healthGoals.isNotEmpty()) {
                    healthFacts.add("الأهداف الصحية: ${profile.health.healthGoals.joinToString("، ")}")
                }

                val healthMemories = recentMemories.filter {
                    it.category == MemoryCategory.HEALTH || it.category == MemoryCategory.HEALTH_DATA
                }
                if (healthMemories.isNotEmpty()) {
                    healthFacts.add("ملاحظات صحية من المحادثات: ${healthMemories.take(3).joinToString(" | ") { it.content }}")
                }

                if (healthFacts.isEmpty()) {
                    sb.appendLine("لا توجد معلومات صحية مسجلة.")
                } else {
                    healthFacts.forEach { sb.appendLine("- $it") }
                }

                sb.appendLine("توجيه أمان: كل الملاحظات الصحية منقولة عن أمي (User-reported). لستِ طبيبة ولا تعوضين الفحص السريري.")
            }

            QueryDomain.STORY -> {
                sb.appendLine("=== سياق الرواية (سارة... والطريق إلى الطب) ===")
                if (currentChapter != null) {
                    sb.appendLine("الفصل الحالي: ${currentChapter.chapterNumber} - ${currentChapter.title}")
                    sb.appendLine("ملخص الأحداث: ${currentChapter.hook}")
                    sb.appendLine("المفهوم العلمي المرتبط: ${currentChapter.scientificConceptKey}")
                } else {
                    sb.appendLine("الفصل المسجل: الفصل ${profile.learning.lastChapterNumber}.")
                }
            }

            QueryDomain.LEARNING -> {
                sb.appendLine("=== سياق التعلم والعلوم ===")
                sb.appendLine("آخر درس: ${profile.learning.lastLessonTitle}")
                sb.appendLine("نسبة التقدم: ${profile.learning.progressPercent}%")
                if (profile.learning.conceptsNeedingReview.isNotEmpty()) {
                    sb.appendLine("مفاهيم تحتاج مراجعة: ${profile.learning.conceptsNeedingReview.joinToString("، ")}")
                }
                sb.appendLine("الأسلوب: تشبيهات حياتية، أمثلة من الدار التونسية، تدرج هادئ.")
            }

            QueryDomain.DAILY_PLANNER -> {
                sb.appendLine("=== سياق نهار أمي والمواعيد ===")
                val uncompleted = todayTasks.filter { !it.isCompleted }
                if (uncompleted.isNotEmpty()) {
                    sb.appendLine("المهام غير المكتملة:")
                    uncompleted.take(4).forEach { t ->
                        sb.appendLine("- ${t.title} (${t.timeHint})")
                    }
                } else if (todayTasks.isNotEmpty()) {
                    sb.appendLine("كل مهام اليوم (${todayTasks.size}) مكتملة.")
                } else {
                    sb.appendLine("لا توجد مهام مسجلة في برنامج اليوم حتى الآن.")
                }
            }

            QueryDomain.FRENCH -> {
                sb.appendLine("=== سياق اللغة الفرنسية ===")
                sb.appendLine("المستوى: كلمات الحياة اليومية والطبية المتداولة في تونس مع النطق الصوتي الهادئ والمطمئن.")
            }

            QueryDomain.GENERAL_GREETING -> {
                sb.appendLine("=== سياق عام وودود ===")
                sb.appendLine("آخر إنجاز: ${profile.learning.lastLessonTitle}، رغبة الوالدة في يوم طيب ومريح.")
            }
        }

        return sb.toString().trim()
    }

    private fun filterMemoriesForDomain(domain: QueryDomain, memories: List<MemoryItem>): List<MemoryItem> {
        return when (domain) {
            QueryDomain.HEALTH -> memories.filter { it.category == MemoryCategory.HEALTH || it.category == MemoryCategory.HEALTH_DATA || it.category == MemoryCategory.PREFERENCE }
            QueryDomain.STORY -> memories.filter { it.category == MemoryCategory.STORY || it.category == MemoryCategory.STORY_PROGRESS || it.category == MemoryCategory.PREFERENCE }
            QueryDomain.LEARNING -> memories.filter { it.category == MemoryCategory.LEARNING || it.category == MemoryCategory.LEARNING_PROGRESS }
            QueryDomain.DAILY_PLANNER -> memories.filter { it.category == MemoryCategory.DAILY_ROUTINE || it.category == MemoryCategory.PREFERENCE }
            QueryDomain.FRENCH -> memories.filter { it.category == MemoryCategory.FRENCH || it.category == MemoryCategory.LEARNING }
            QueryDomain.GENERAL_GREETING -> memories.take(3)
        }.take(5)
    }
}
