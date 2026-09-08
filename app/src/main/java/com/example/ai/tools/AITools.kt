package com.example.ai.tools

import com.example.data.repository.DailyPlannerRepository
import com.example.data.repository.FrenchWordRepository
import com.example.data.repository.MemoryRepository
import com.example.data.repository.ProfileRepository
import com.example.data.repository.StoryRepository
import com.example.domain.model.MemoryCategory
import com.example.domain.model.TaskCategory
import org.json.JSONArray
import org.json.JSONObject

/**
 * Access levels for Gemini Function Calling tools in Rafiqah V2.1.
 */
enum class ToolAccessLevel {
    READ_TOOL,        // Executed directly without confirmation
    SAFE_WRITE,       // Internal progress and safe app updates
    SENSITIVE_WRITE   // Requires explicit user confirmation prior to execution
}

data class ToolDefinition(
    val name: String,
    val description: String,
    val accessLevel: ToolAccessLevel,
    val parametersSchema: JSONObject
)

object AIToolRegistry {

    private fun buildParam(type: String, desc: String): JSONObject {
        return JSONObject().apply {
            put("type", type)
            put("description", desc)
        }
    }

    val TOOLS: List<ToolDefinition> = listOf(
        ToolDefinition(
            name = "get_mother_profile",
            description = "إحضار الملف الشخصي والتعليمي والصحي المسجل لأمي",
            accessLevel = ToolAccessLevel.READ_TOOL,
            parametersSchema = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            }
        ),
        ToolDefinition(
            name = "get_relevant_memories",
            description = "إحضار أهم الذكريات والتفضيلات السابقة المسجلة عن أمي",
            accessLevel = ToolAccessLevel.READ_TOOL,
            parametersSchema = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("limit", buildParam("INTEGER", "عدد الذكريات المراد استرجاعها (افتراضي 5)"))
                })
            }
        ),
        ToolDefinition(
            name = "save_memory",
            description = "حفظ معلومة أو تفضيل دائم عن أمي في الذاكرة طويلة المدى",
            accessLevel = ToolAccessLevel.SAFE_WRITE,
            parametersSchema = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("content", buildParam("STRING", "نص المعلومة أو التفضيل"))
                    put("category", buildParam("STRING", "التصنيف: PERSONAL, HEALTH, LEARNING, PREFERENCE, DAILY_ROUTINE, FRENCH, STORY"))
                    put("importance", buildParam("INTEGER", "درجة الأهمية من 1 إلى 5"))
                })
                put("required", JSONArray().apply { put("content") })
            }
        ),
        ToolDefinition(
            name = "delete_memory",
            description = "حذف معلومة محددة من الذاكرة (يتطلب تأكيداً مسبقاً)",
            accessLevel = ToolAccessLevel.SENSITIVE_WRITE,
            parametersSchema = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("id", buildParam("INTEGER", "المعرف الرقمي للذاكرة المراد حذفها"))
                })
                put("required", JSONArray().apply { put("id") })
            }
        ),
        ToolDefinition(
            name = "get_health_profile",
            description = "إحضار البيانات الصحية المسجلة فقط لأمي (دون افتراضات)",
            accessLevel = ToolAccessLevel.READ_TOOL,
            parametersSchema = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            }
        ),
        ToolDefinition(
            name = "get_health_notes",
            description = "إحضار الملاحظات الصحية السابقة المنقولة عن أمي",
            accessLevel = ToolAccessLevel.READ_TOOL,
            parametersSchema = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            }
        ),
        ToolDefinition(
            name = "save_health_note",
            description = "تسجيل ملاحظة صحية جديدة أبلغت عنها أمي (يتطلب تأكيداً إذا كانت حساسة)",
            accessLevel = ToolAccessLevel.SENSITIVE_WRITE,
            parametersSchema = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("note", buildParam("STRING", "نص الملاحظة الصحية التي ذكرتها أمي"))
                })
                put("required", JSONArray().apply { put("note") })
            }
        ),
        ToolDefinition(
            name = "get_learning_progress",
            description = "إحضار نسبة التقدم التعليمي وآخر درس تم تناوله",
            accessLevel = ToolAccessLevel.READ_TOOL,
            parametersSchema = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            }
        ),
        ToolDefinition(
            name = "save_learning_progress",
            description = "تحديث نسبة التقدم التعليمي وعنوان الدرس الحالي",
            accessLevel = ToolAccessLevel.SAFE_WRITE,
            parametersSchema = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("lastLessonTitle", buildParam("STRING", "عنوان الدرس الحالي"))
                    put("progressPercent", buildParam("INTEGER", "نسبة التقدم الكلية من 0 إلى 100"))
                })
                put("required", JSONArray().apply { put("lastLessonTitle"); put("progressPercent") })
            }
        ),
        ToolDefinition(
            name = "mark_concept_mastered",
            description = "تسجيل إتقان وفهم أمي لمفهوم علمي محدد",
            accessLevel = ToolAccessLevel.SAFE_WRITE,
            parametersSchema = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("conceptKey", buildParam("STRING", "مفتاح المفهوم، مثل الخلية، الغشاء، الميتوكوندريا"))
                })
                put("required", JSONArray().apply { put("conceptKey") })
            }
        ),
        ToolDefinition(
            name = "mark_concept_needs_review",
            description = "تسجيل حاجة مفهوم علمي للمراجعة والتبسيط في الجلسات القادمة",
            accessLevel = ToolAccessLevel.SAFE_WRITE,
            parametersSchema = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("conceptKey", buildParam("STRING", "مفتاح المفهوم الذي يحتاج مراجعة"))
                })
                put("required", JSONArray().apply { put("conceptKey") })
            }
        ),
        ToolDefinition(
            name = "get_current_story",
            description = "إحضار تفاصيل الفصل الحالي من رواية سارة... والطريق إلى الطب",
            accessLevel = ToolAccessLevel.READ_TOOL,
            parametersSchema = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            }
        ),
        ToolDefinition(
            name = "get_story_progress",
            description = "إحضار رقم آخر فصل أتمته أمي في الرواية وعدد الفصول الكلية",
            accessLevel = ToolAccessLevel.READ_TOOL,
            parametersSchema = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            }
        ),
        ToolDefinition(
            name = "save_story_progress",
            description = "حفظ إتمام فصل في الرواية وفتح الفصل الذي يليه",
            accessLevel = ToolAccessLevel.SAFE_WRITE,
            parametersSchema = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("chapterNumber", buildParam("INTEGER", "رقم الفصل المكتمل"))
                })
                put("required", JSONArray().apply { put("chapterNumber") })
            }
        ),
        ToolDefinition(
            name = "get_today_plan",
            description = "إحضار قائمة المهام والمواعيد الحقيقية المسجلة لليوم من قاعدة البيانات",
            accessLevel = ToolAccessLevel.READ_TOOL,
            parametersSchema = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            }
        ),
        ToolDefinition(
            name = "add_daily_task",
            description = "إضافة مهمة أو موعد جديد إلى برنامج نهار أمي (المواعيد الطبية تتطلب تأكيداً)",
            accessLevel = ToolAccessLevel.SENSITIVE_WRITE,
            parametersSchema = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("title", buildParam("STRING", "عنوان المهمة أو الموعد"))
                    put("timeHint", buildParam("STRING", "الوقت المقترح (مثال: صباحاً، بعد الظهر، 15:00)"))
                    put("category", buildParam("STRING", "التصنيف: HEALTH_HABIT, LEARNING, APPOINTMENT, REST, PERSONAL"))
                })
                put("required", JSONArray().apply { put("title") })
            }
        ),
        ToolDefinition(
            name = "complete_daily_task",
            description = "تحديد مهمة كمكتملة في برنامج اليوم",
            accessLevel = ToolAccessLevel.SAFE_WRITE,
            parametersSchema = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("taskId", buildParam("INTEGER", "معرف المهمة الرقمي"))
                })
                put("required", JSONArray().apply { put("taskId") })
            }
        ),
        ToolDefinition(
            name = "get_french_progress",
            description = "إحضار الكلمات الفرنسية الحقيقية المسجلة (المتقنة وقيد التعلم) من قاعدة البيانات",
            accessLevel = ToolAccessLevel.READ_TOOL,
            parametersSchema = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            }
        ),
        ToolDefinition(
            name = "save_french_word",
            description = "تسجيل كلمة فرنسية كمتقنة أو قيد المراجعة",
            accessLevel = ToolAccessLevel.SAFE_WRITE,
            parametersSchema = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("wordId", buildParam("INTEGER", "المعرف الرقمي للكلمة"))
                    put("isMastered", buildParam("BOOLEAN", "هل تم إتقان الكلمة بنجاح"))
                })
                put("required", JSONArray().apply { put("wordId") })
            }
        )
    )

    /**
     * Formats tools into the official Gemini REST JSON format:
     * [{"function_declarations": [...]}]
     */
    fun getGeminiToolsDeclarationJson(): JSONArray {
        val functionDeclarations = JSONArray()
        for (tool in TOOLS) {
            val fn = JSONObject().apply {
                put("name", tool.name)
                put("description", tool.description)
                put("parameters", tool.parametersSchema)
            }
            functionDeclarations.put(fn)
        }

        return JSONArray().apply {
            put(JSONObject().apply {
                put("function_declarations", functionDeclarations)
            })
        }
    }
}

/**
 * Concrete Executor for Gemini Function Calling in Rafiqah V2.1.
 * Connects function call arguments directly to Room repositories.
 */
open class ToolExecutor(
    private val profileRepo: ProfileRepository,
    private val memoryRepo: MemoryRepository,
    private val storyRepo: StoryRepository,
    private val plannerRepo: DailyPlannerRepository,
    private val frenchRepo: FrenchWordRepository
) {

    open fun isConfirmationRequired(toolName: String, args: Map<String, Any?>): Boolean {
        val def = AIToolRegistry.TOOLS.find { it.name == toolName }
        if (def?.accessLevel == ToolAccessLevel.SENSITIVE_WRITE) return true

        // Extra check for appointment in add_daily_task
        if (toolName == "add_daily_task") {
            val title = args["title"]?.toString()?.lowercase() ?: ""
            val cat = args["category"]?.toString() ?: ""
            if (cat == "APPOINTMENT" || title.contains("طبيب") || title.contains("موعد") || title.contains("سبيطار")) {
                return true
            }
        }
        return false
    }

    open fun getConfirmationMessage(toolName: String, args: Map<String, Any?>): String {
        return when (toolName) {
            "add_daily_task" -> {
                val title = args["title"]?.toString() ?: "الموعد"
                val time = args["timeHint"]?.toString() ?: ""
                "تحبي نسجل الموعد هذا ($title ${if (time.isNotBlank()) "على $time" else ""}) في نهارك يا أمي؟"
            }
            "delete_memory" -> "تحبي نحذف هذه المعلومة من الذاكرة يا أمي؟"
            "save_health_note" -> {
                val note = args["note"]?.toString() ?: ""
                "تحبي نسجل الملاحظة الصحية هذه: \"$note\" في ملفك يا أمي؟"
            }
            else -> "تحبي نأكد هذا الإجراء يا أمي؟"
        }
    }

    open suspend fun executeTool(toolName: String, arguments: Map<String, Any?>): String {
        return try {
            when (toolName) {
                "get_mother_profile" -> {
                    val p = profileRepo.getProfile()
                    "الاسم: ${p.identity.name}، العمر: ${p.identity.age} سنة، اللغة المفضلة: ${p.identity.preferredLanguage}، آخر درس: ${p.learning.lastLessonTitle}، نسبة التقدم: ${p.learning.progressPercent}%"
                }

                "get_relevant_memories" -> {
                    val limit = (arguments["limit"] as? Number)?.toInt() ?: 5
                    val mems = memoryRepo.getRelevantMemories(limit)
                    if (mems.isEmpty()) "لا توجد ذكريات سابقة مسجلة."
                    else mems.joinToString("\n") { "- [${it.category.arabicLabel}] ${it.content}" }
                }

                "save_memory" -> {
                    val content = arguments["content"]?.toString() ?: return "خطأ: المحتوى فارغ"
                    val catStr = arguments["category"]?.toString() ?: "PERSONAL"
                    val importance = (arguments["importance"] as? Number)?.toInt() ?: 3
                    val category = try {
                        MemoryCategory.valueOf(catStr)
                    } catch (_: Exception) {
                        MemoryCategory.PERSONAL
                    }
                    val id = memoryRepo.saveMemoryWithDeduplication(content, category, importance, "استدعاء أداة ذكاء اصطناعي")
                    "تم حفظ المعلومة في الذاكرة بنجاح (معرف: $id)"
                }

                "delete_memory" -> {
                    val id = (arguments["id"] as? Number)?.toLong() ?: return "خطأ: المعرف غير صالح"
                    memoryRepo.deleteMemory(id)
                    "تم حذف المعلومة بنجاح من الذاكرة."
                }

                "get_health_profile" -> {
                    val p = profileRepo.getProfile()
                    val facts = mutableListOf<String>()
                    if (p.health.diagnosedConditions.isNotEmpty()) facts.add("الحالات المشخصة: ${p.health.diagnosedConditions.joinToString("، ")}")
                    if (p.health.medications.isNotEmpty()) facts.add("الأدوية: ${p.health.medications.joinToString("، ")}")
                    if (p.health.sleepQuality.isNotBlank()) facts.add("جودة النوم: ${p.health.sleepQuality}")
                    if (p.health.weightKg > 0) facts.add("الوزن: ${p.health.weightKg} كغ")
                    if (facts.isEmpty()) "لا توجد معلومات صحية مسجلة." else facts.joinToString(" | ")
                }

                "get_health_notes" -> {
                    val mems = memoryRepo.getAllMemoriesList().filter {
                        it.category == MemoryCategory.HEALTH || it.category == MemoryCategory.HEALTH_DATA
                    }
                    if (mems.isEmpty()) "لا توجد ملاحظات صحية سابقة مسجلة."
                    else mems.joinToString("\n") { "- ${it.content}" }
                }

                "save_health_note" -> {
                    val note = arguments["note"]?.toString() ?: return "خطأ: نص الملاحظة فارغ"
                    val id = memoryRepo.saveMemoryWithDeduplication(note, MemoryCategory.HEALTH, 4, "ملاحظة صحية أبلغت عنها أمي")
                    "تم تسجيل الملاحظة الصحية بنجاح (معرف: $id)"
                }

                "get_learning_progress" -> {
                    val p = profileRepo.getProfile()
                    "آخر درس: ${p.learning.lastLessonTitle}، نسبة التقدم الكلية: ${p.learning.progressPercent}%، آخر فصل في القصة: ${p.learning.lastChapterNumber}"
                }

                "save_learning_progress" -> {
                    val title = arguments["lastLessonTitle"]?.toString() ?: "درس طبي"
                    val progress = (arguments["progressPercent"] as? Number)?.toInt() ?: 20
                    profileRepo.updateLearningProgress(1, progress, title)
                    "تم تحديث التقدم التعليمي بنجاح إلى $progress% (الدرس: $title)"
                }

                "mark_concept_mastered" -> {
                    val concept = arguments["conceptKey"]?.toString() ?: "الخلية"
                    memoryRepo.saveMemoryWithDeduplication("أتقنت أمي مفهوم $concept بالكامل.", MemoryCategory.LEARNING, 4, "محرك التعلم")
                    "تم تسجيل إتقان المفهوم ($concept) بنجاح."
                }

                "mark_concept_needs_review" -> {
                    val concept = arguments["conceptKey"]?.toString() ?: "الخلية"
                    memoryRepo.saveMemoryWithDeduplication("مفهوم $concept يحتاج مراجعة وتبسيط إضافي.", MemoryCategory.LEARNING, 4, "محرك التعلم")
                    "تم تسجيل المفهوم ($concept) للمراجعة في الجلسة القادمة."
                }

                "get_current_story" -> {
                    val p = profileRepo.getProfile()
                    val ch = storyRepo.getChapter(p.learning.lastChapterNumber)
                    if (ch != null) {
                        "الفصل ${ch.chapterNumber}: ${ch.title} - ${ch.hook} (المفهوم العلمي: ${ch.scientificConceptKey})"
                    } else {
                        "رواية سارة... والطريق إلى الطب، جاهزة للمتابعة."
                    }
                }

                "get_story_progress" -> {
                    val p = profileRepo.getProfile()
                    val totalChapters = storyRepo.getAllChaptersList().size
                    "الفصل الحالي: ${p.learning.lastChapterNumber} من أصل $totalChapters فصول."
                }

                "save_story_progress" -> {
                    val chNum = (arguments["chapterNumber"] as? Number)?.toInt() ?: 1
                    storyRepo.completeChapter(chNum)
                    profileRepo.updateLearningProgress(chNum, (chNum * 20).coerceAtMost(100), "الفصل $chNum في رواية سارة")
                    "تم تسجيل إتمام الفصل $chNum وفتح الفصل الذي يليه."
                }

                "get_today_plan" -> {
                    // REAL Room fetch from dailyTaskDao
                    val allTasks = plannerRepo.getAllTasksList()
                    if (allTasks.isEmpty()) {
                        "لا توجد مهام أو مواعيد مسجلة في برنامج اليوم حتى الآن."
                    } else {
                        val formatted = allTasks.mapIndexed { idx, task ->
                            val status = if (task.isCompleted) "✅ مكتملة" else "⏳ في الانتظار"
                            "${idx + 1}. [${task.category.labelArabic}] ${task.title} (${task.timeHint}) - $status"
                        }
                        "مهام ومواعيد نهار أمي المسجلة:\n" + formatted.joinToString("\n")
                    }
                }

                "add_daily_task" -> {
                    val title = arguments["title"]?.toString() ?: return "خطأ: عنوان المهمة فارغ"
                    val timeHint = arguments["timeHint"]?.toString() ?: ""
                    val catStr = arguments["category"]?.toString() ?: "HEALTH_HABIT"
                    val cat = try { TaskCategory.valueOf(catStr) } catch (_: Exception) { TaskCategory.HEALTH_HABIT }
                    val id = plannerRepo.addTask(title, cat, timeHint, isPriority = true)
                    "تمت إضافة المهمة بنجاح إلى نهار أمي: $title (معرف: $id)"
                }

                "complete_daily_task" -> {
                    val taskId = (arguments["taskId"] as? Number)?.toLong() ?: return "خطأ: معرف المهمة غير صالح"
                    plannerRepo.setTaskCompleted(taskId, true)
                    "تم تعيين المهمة كمكتملة بنجاح."
                }

                "get_french_progress" -> {
                    // REAL Room fetch from frenchWordDao
                    val allWords = frenchRepo.getAllWordsList()
                    if (allWords.isEmpty()) {
                        "لا توجد كلمات فرنسية مسجلة في قاعدة البيانات."
                    } else {
                        val mastered = allWords.filter { it.isMastered }
                        val inLearning = allWords.filter { !it.isMastered }
                        val sb = StringBuilder()
                        sb.appendLine("تقدم أمي في الفرنسية (المجموع: ${allWords.size} كلمات):")
                        if (mastered.isNotEmpty()) {
                            sb.appendLine("الكلمات المتقنة (${mastered.size}): " + mastered.joinToString("، ") { "${it.frenchWord} (${it.arabicMeaning})" })
                        }
                        if (inLearning.isNotEmpty()) {
                            sb.appendLine("الكلمات قيد التعلم (${inLearning.size}): " + inLearning.take(3).joinToString("، ") { "${it.frenchWord} (${it.arabicMeaning})" })
                        }
                        sb.toString().trim()
                    }
                }

                "save_french_word" -> {
                    val wordId = (arguments["wordId"] as? Number)?.toInt() ?: 1
                    val isMastered = arguments["isMastered"] as? Boolean ?: true
                    frenchRepo.setWordMastered(wordId, isMastered)
                    "تم تحديث حالة الكلمة الفرنسية بنجاح."
                }

                else -> "أداة غير معروفة: $toolName"
            }
        } catch (e: Exception) {
            "حدث خطأ أثناء تنفيذ الأداة: ${e.message}"
        }
    }
}
