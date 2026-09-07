package com.example.ai.memory

import com.example.domain.model.MemoryCategory
import com.example.domain.model.MemoryItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * Enhanced Memory Manager for Rafiqah V2.1.
 * Extracts high-value, enduring memories from conversations.
 * Supports structured AI JSON parsing with reliable heuristic fallback.
 * Strictly ignores transient greetings and duplicates.
 */
class MemoryManager {

    data class MemoryCandidate(
        val category: MemoryCategory,
        val content: String,
        val importance: Int, // 1 to 5
        val confidence: Float = 0.9f,
        val source: String = "محادثة ذكية",
        val requiresApproval: Boolean = false,
        var userApproved: Boolean = false
    )

    private val transientChitchatPatterns = listOf(
        "صباح الخير", "مساء النور", "على سلامتك", "سلام", "شكرا", "عيشك", "يعطيك الصحة",
        "ربي يفضلك", "باهي", "واضح", "نعم", "لا", "ألو", "تسمع فيا"
    )

    /**
     * Extracts memory candidates attempting AI structured output parsing first,
     * falling back to the heuristic extractor.
     */
    fun extractMemories(
        userUtterance: String,
        aiReply: String,
        structuredAiJson: String? = null,
        existingMemories: List<MemoryItem> = emptyList()
    ): List<MemoryCandidate> {
        val candidates = mutableListOf<MemoryCandidate>()

        // 1. Try structured AI JSON extraction if provided
        if (!structuredAiJson.isNullOrBlank()) {
            try {
                val jsonArr = JSONArray(structuredAiJson)
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.optJSONObject(i) ?: continue
                    val content = obj.optString("content").trim()
                    val catStr = obj.optString("category")
                    val importance = obj.optInt("importance", 3)
                    val confidence = obj.optDouble("confidence", 0.9).toFloat()

                    if (isValidMemoryContent(content)) {
                        val cat = try { MemoryCategory.valueOf(catStr) } catch (_: Exception) { MemoryCategory.PERSONAL }
                        val isSensitive = cat == MemoryCategory.HEALTH || importance >= 4 || confidence < 0.92f
                        candidates.add(
                            MemoryCandidate(
                                category = cat,
                                content = content,
                                importance = importance,
                                confidence = confidence,
                                source = "استخراج الذكاء الاصطناعي",
                                requiresApproval = isSensitive
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                // Ignore JSON parse errors, proceed to heuristic extractor
            }
        }

        // 2. Fallback to heuristic extraction if AI extraction produced nothing
        if (candidates.isEmpty()) {
            candidates.addAll(extractHeuristicCandidates(userUtterance, aiReply))
        }

        // 3. Filter out duplicates against existing stored memories
        return deduplicateCandidates(candidates, existingMemories)
    }

    /**
     * Backward-compatible overload
     */
    fun extractMemoriesFromInteraction(userUtterance: String, aiReply: String): List<MemoryCandidate> {
        return extractMemories(userUtterance, aiReply, null, emptyList())
    }

    private fun isValidMemoryContent(text: String): Boolean {
        if (text.length < 6) return false
        val trimmed = text.trim().lowercase()
        // Reject purely transient chitchat
        if (transientChitchatPatterns.any { trimmed == it || trimmed.startsWith("$it ") }) {
            return false
        }
        return true
    }

    private fun extractHeuristicCandidates(userUtterance: String, aiReply: String): List<MemoryCandidate> {
        val candidates = mutableListOf<MemoryCandidate>()
        val text = userUtterance.trim().lowercase()

        // 1. Preferences & Habits
        if (text.contains("نحب نقرا") || text.contains("نحب نطالع")) {
            val timing = if (text.contains("ليل")) "مساءً قبل النوم" else "في أوقات الراحة والهدوء"
            candidates.add(
                MemoryCandidate(
                    category = MemoryCategory.PREFERENCE,
                    content = "تفضل أمي قراءة الروايات والدروس $timing.",
                    importance = 4,
                    confidence = 0.95f
                )
            )
        } else if (text.contains("نحب") || text.contains("نفضل") || text.contains("يعجبني")) {
            candidates.add(
                MemoryCandidate(
                    category = MemoryCategory.PREFERENCE,
                    content = "تفضيل شخصي ذكرته أمي: \"$userUtterance\"",
                    importance = 3,
                    confidence = 0.85f
                )
            )
        }

        // 2. Learning comprehension
        if (text.contains("ما فهمتش") || text.contains("صعيبة") || text.contains("عاودلي")) {
            val concept = when {
                text.contains("خلية") -> "الخلية"
                text.contains("غشاء") -> "غشاء الخلية"
                text.contains("نواة") -> "نواة الخلية"
                text.contains("dna") -> "الحمض النووي DNA"
                text.contains("ميتوكوندريا") -> "الميتوكوندريا"
                else -> "المفهوم الحالي"
            }
            candidates.add(
                MemoryCandidate(
                    category = MemoryCategory.LEARNING,
                    content = "مفهوم $concept يحتاج مراجعة وتبسيط بأمثلة حياتية إضافية.",
                    importance = 4,
                    confidence = 0.92f
                )
            )
        } else if (text.contains("فهمت") && (text.contains("مليح") || text.contains("عايشك") || text.contains("واضح"))) {
            candidates.add(
                MemoryCandidate(
                    category = MemoryCategory.LEARNING,
                    content = "أتقنت أمي المفهوم المعروض وأبدت ارتياحاً وفهماً طيباً.",
                    importance = 3,
                    confidence = 0.90f
                )
            )
        }

        // 3. Health observations (Only personal health statements, not casual questions)
        val hasFirstPersonHealthContext = text.contains("عندي") || text.contains("قست") ||
                text.contains("شربت") || text.contains("وجعني") || text.contains("راسي") ||
                text.contains("نحس") || text.contains("طبيبي قال") || text.contains("طالعة")

        if (text.contains("نومي") || text.contains("ما رقدتش") || text.contains("نوم مقلق")) {
            candidates.add(
                MemoryCandidate(
                    category = MemoryCategory.HEALTH,
                    content = "أفادت أمي بوجود صعوبة أو تقطع في النوم: \"$userUtterance\"",
                    importance = 4,
                    confidence = 0.95f,
                    requiresApproval = true
                )
            )
        } else if ((text.contains("تونسيو") || text.contains("ضغط") || text.contains("دواء") || text.contains("طبيب")) && hasFirstPersonHealthContext) {
            candidates.add(
                MemoryCandidate(
                    category = MemoryCategory.HEALTH,
                    content = "ملاحظة صحية أبلغت عنها أمي: \"$userUtterance\"",
                    importance = 4,
                    confidence = 0.90f,
                    requiresApproval = true
                )
            )
        }

        // 4. Daily Routine
        if ((text.contains("موعد") || text.contains("طبيب") || text.contains("غدوة على") || text.contains("اليوم على")) && hasFirstPersonHealthContext) {
            candidates.add(
                MemoryCandidate(
                    category = MemoryCategory.DAILY_ROUTINE,
                    content = "موعد أو التزام يومي لأمي: \"$userUtterance\"",
                    importance = 4,
                    confidence = 0.90f,
                    requiresApproval = true
                )
            )
        }

        return candidates
    }

    private fun deduplicateCandidates(
        candidates: List<MemoryCandidate>,
        existingMemories: List<MemoryItem>
    ): List<MemoryCandidate> {
        val unique = mutableListOf<MemoryCandidate>()

        for (candidate in candidates) {
            val candidateText = candidate.content.trim().lowercase()
            // Check against existing in database
            val alreadyExists = existingMemories.any { existing ->
                existing.category == candidate.category && isSimilar(existing.content.lowercase(), candidateText)
            }
            // Check against candidates already added in this batch
            val alreadyInBatch = unique.any { inBatch ->
                inBatch.category == candidate.category && isSimilar(inBatch.content.lowercase(), candidateText)
            }

            if (!alreadyExists && !alreadyInBatch) {
                unique.add(candidate)
            }
        }
        return unique
    }

    private fun isSimilar(text1: String, text2: String): Boolean {
        if (text1 == text2) return true
        val words1 = text1.split(" ", "،", ".", "-").filter { it.length > 3 }.toSet()
        val words2 = text2.split(" ", "،", ".", "-").filter { it.length > 3 }.toSet()
        if (words1.isEmpty() || words2.isEmpty()) return false
        val intersection = words1.intersect(words2).size
        val minSize = minOf(words1.size, words2.size)
        return (intersection.toDouble() / minSize.toDouble()) >= 0.70
    }

    fun toMemoryItem(candidate: MemoryCandidate): MemoryItem {
        return MemoryItem(
            id = 0,
            category = candidate.category,
            content = candidate.content,
            importance = candidate.importance,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            source = candidate.source,
            confidence = candidate.confidence
        )
    }
}
