package com.example.ai.story

import com.example.domain.model.StoryChapter

/**
 * Story Progress decoupled from overall learning progress.
 */
data class StoryProgress(
    val currentChapterNumber: Int,
    val totalChapters: Int,
    val completedChaptersCount: Int,
    val percentCompleted: Int
)

/**
 * Story Engine for "Sarah... and the Path to Medicine" in Rafiqah V2.1.
 * Seamlessly connects story narrative with concept mastery.
 */
class StoryEngine {

    fun calculateStoryProgress(chapters: List<StoryChapter>, currentChapterNumber: Int): StoryProgress {
        val total = chapters.size.coerceAtLeast(1)
        val completedCount = chapters.count { it.isCompleted }
        val percent = ((completedCount.toDouble() / total.toDouble()) * 100).toInt().coerceIn(0, 100)

        return StoryProgress(
            currentChapterNumber = currentChapterNumber,
            totalChapters = total,
            completedChaptersCount = completedCount,
            percentCompleted = percent
        )
    }

    /**
     * Builds contextual prompt for the narrative based on current chapter and concepts needing review.
     */
    fun buildStoryContextPrompt(
        currentChapter: StoryChapter,
        conceptsNeedingReview: List<String>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("رواية: سارة... والطريق إلى الطب")
        sb.appendLine("الفصل ${currentChapter.chapterNumber}: ${currentChapter.title}")
        sb.appendLine("مقدمة الحدث: ${currentChapter.hook}")
        sb.appendLine("المفهوم العلمي المرتبط: ${currentChapter.scientificConceptKey}")

        if (conceptsNeedingReview.isNotEmpty()) {
            sb.appendLine("ملاحظة توجيهية: أمي تحتاج مراجعة لمفهوم (${conceptsNeedingReview.first()}). دعي سارة في أحداث هذا الفصل تتأمل في هذا المفهوم وتشاركه مع أمي برفق.")
        }
        return sb.toString()
    }
}
