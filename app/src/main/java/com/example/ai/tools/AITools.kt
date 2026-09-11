package com.example.ai.tools

import com.example.ai.learning.FrenchLessonGenerator
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
        ),
        ToolDefinition(
            name = "create_reminder",
            description = "إنشاء تذكير حقيقي بالصوت مع وقت وتنبيه خارج التطبيق (المواعيد الحساسة تؤكد مسبقاً)",
            accessLevel = ToolAccessLevel.SAFE_WRITE,
            parametersSchema = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("title", buildParam("STRING", "نص التذكير، مثلا: شرب الماء، موعد الطبيب، قراءة"))
                    put("timeExpression", buildParam("STRING", "التعبير الزمني، مثلا: غدوة على الثمانية، بعد ساعتين"))
                    put(
                        "category",
                        buildParam(
                            "STRING",
                            "MEDICINE, APPOINTMENT, WATER, READING, LEARNING, FRENCH, FOCUS, HEALTH, GENERAL"
                        )
                    )
                    put(
                        "isRecurring",
                        buildParam(
                            "BOOLEAN",
                            "هل التذكير متكرر؟ إذا لم يذكر المستخدم التكرار استخدم false."
                        )
                    )
                    put(
                        "recurrenceRule",
                        buildParam(
                            "STRING",
                            "NONE, DAILY, WEEKLY"
                        )
                    )
                    put(
                        "destination",
                        buildParam(
                            "STRING",
                            "planner, reading, learning, french, focus, health"
                        )
                    )
                })
                put("required", JSONArray().apply { put("title"); put("timeExpression") })
            }
        ),
        ToolDefinition(
            name = "get_active_reminders",
            description = "قراءة التذكيرات والمواعيد النشطة المسجلة في النظام",
            accessLevel = ToolAccessLevel.READ_TOOL,
            parametersSchema = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            }
        ),
        ToolDefinition(
            name = "start_reading_session",
            description = "بدء جلسة قراءة بمحتوى تعليمي ومدة محددة من المستخدم أو من الجلسة الحالية",
            accessLevel = ToolAccessLevel.SAFE_WRITE,
            parametersSchema = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("contentId", buildParam("STRING", "معرف المحتوى، مثلا: content_heart_health, content_cell_membrane"))
                    put("durationMinutes", buildParam("INTEGER", "المدة المطلوبة بالدقائق. يجب ذكرها أو توفيرها من جلسة القراءة الحالية."))
                })
            }
        ),
        ToolDefinition(
            name = "start_focus_session",
            description = "بدء جلسة تركيز هادئة مع عد تنازلي وتتبع التطبيقات المشتتة",
            accessLevel = ToolAccessLevel.SAFE_WRITE,
            parametersSchema = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("durationMinutes", buildParam("INTEGER", "مدة التركيز بالدقائق (مثلا 15 دقيقة)"))
                    put("activityTitle", buildParam("STRING", "اسم نشاط التركيز"))
                })
                put("required", JSONArray().apply { put("durationMinutes") })
            }
        ),
        ToolDefinition(
            name = "update_profile_from_conversation",
            description = "تحديث بيانات الملف الشخصي التي صرحت بها أمي طبيعياً (العمر، المدينة، نمط الحياة)",
            accessLevel = ToolAccessLevel.SAFE_WRITE,
            parametersSchema = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put(
                        "name",
                        buildParam("STRING", "الاسم الذي تفضّل الأم أن نناديها به")
                    )

                    put(
                        "age",
                        buildParam("INTEGER", "العمر إذا ذكرته الأم")
                    )

                    put(
                        "generalLocation",
                        buildParam(
                            "STRING",
                            "المدينة أو المنطقة العامة فقط، وليس عنوان المنزل"
                        )
                    )

                    put(
                        "dialect",
                        buildParam("STRING", "اللهجة أو أسلوب اللغة المفضل")
                    )

                    put(
                        "primaryGoal",
                        buildParam("STRING", "الهدف الرئيسي الذي صرحت به الأم")
                    )

                    put(
                        "dailyActivity",
                        buildParam("STRING", "النشاط اليومي أو الحركة")
                    )

                    put(
                        "sleepQuality",
                        buildParam("STRING", "جودة النوم أو الروتين الليلي")
                    )

                    put(
                        "learningGoals",
                        buildParam(
                            "STRING",
                            "أهداف التعلم مفصولة بفواصل، مثال: طب, فرنسية, فيزياء"
                        )
                    )

                    put(
                        "learningInterests",
                        buildParam(
                            "STRING",
                            "مواضيع التعلم المفضلة مفصولة بفواصل"
                        )
                    )

                    put(
                        "readingPreferences",
                        buildParam(
                            "STRING",
                            "أنواع القراءة المفضلة مفصولة بفواصل"
                        )
                    )
                })
            }
        ),
        ToolDefinition(
            name = "create_french_lesson",
            description = "إنشاء درس ومفردة فرنسية جديدة مع السياق التونسي والنطق الصوتي",
            accessLevel = ToolAccessLevel.SAFE_WRITE,
            parametersSchema = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("frenchWord", buildParam("STRING", "الكلمة بالفرنسية"))
                    put("phoneticArabic", buildParam("STRING", "النطق التقريبي بالحروف العربية"))
                    put("tunisianMeaning", buildParam("STRING", "المعنى والشرح باللهجة التونسية"))
                    put("examplePhrase", buildParam("STRING", "مثال عملي في جملة يومية"))
                    put("category", buildParam("STRING", "التصنيف: PHARMACY, MEDICAL, DAILY_LIFE"))
                })
            }
        ),
        ToolDefinition(
            name = "log_health_checkin",
            description = "تسجيل متابعة صحية سريعة مثل شرب الماء أو قياس الضغط أو المشي",
            accessLevel = ToolAccessLevel.SAFE_WRITE,
            parametersSchema = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("waterGlasses", buildParam("INTEGER", "عدد كؤوس الماء"))
                    put("bloodPressureSystolic", buildParam("INTEGER", "الضغط الانقباضي"))
                    put("bloodPressureDiastolic", buildParam("INTEGER", "الضغط الانبساطي"))
                    put("feelingNotes", buildParam("STRING", "ملاحظات إحساس الأم ونشاطها"))
                })
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
    private val frenchRepo: FrenchWordRepository,
    private val healthRepo: com.example.data.repository.HealthRepository? = null,
    private val reminderRepo: com.example.data.repository.ReminderRepository? = null,
    private val reminderScheduler: com.example.service.reminder.ReminderScheduler? = null,
    private val contentRepo: com.example.data.repository.ContentRepository? = null,
    private val focusRepo: com.example.data.repository.FocusRepository? = null,
    private val learningProgressRepo: com.example.data.repository.LearningProgressRepository? = null
) {

    open fun isConfirmationRequired(toolName: String, args: Map<String, Any?>): Boolean {
        val def = AIToolRegistry.TOOLS.find { it.name == toolName }
        if (def?.accessLevel == ToolAccessLevel.SENSITIVE_WRITE) return true

        // Extra check for appointment in add_daily_task or create_reminder
        if (toolName == "add_daily_task" || toolName == "create_reminder") {
            val title = args["title"]?.toString()?.lowercase() ?: ""
            val cat = args["category"]?.toString() ?: ""
            if (cat == "APPOINTMENT" || cat == "MEDICINE" || title.contains("طبيب") || title.contains("موعد") || title.contains("سبيطار") || title.contains("دواء")) {
                return true
            }
        }
        return false
    }

    open fun getConfirmationMessage(toolName: String, args: Map<String, Any?>): String {
        return when (toolName) {
            "create_reminder" -> {
                val title = args["title"]?.toString() ?: "الموعد"
                val time = args["timeExpression"]?.toString() ?: ""
                "تحبي نبرمجلك منبه وتذكير رسمي لـ \"$title\" ($time) يا أمي؟"
            }
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

    private fun parseCommaSeparated(value: Any?): List<String> {
        return value
            ?.toString()
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
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
                    val hp = healthRepo?.getHealthProfile()
                    if (hp != null) {
                        val facts = mutableListOf<String>()
                        facts.add("العمر: ${hp.age} سنة")
                        facts.add("جودة النوم: ${hp.sleepQuality} (النوم: ${hp.sleepTimeHint}، الاستيقاظ: ${hp.wakeTimeHint})")
                        facts.add("مستوى النشاط: ${hp.activityLevel}")
                        facts.add("الماء اليوم: ${hp.currentWaterGlasses}/${hp.waterIntakeGoalGlasses} كؤوس")
                        facts.add("العادات الغذائية: ${hp.dietaryHabits}")
                        facts.add("الأهداف العامة: ${hp.generalGoals}")
                        facts.joinToString(" | ")
                    } else {
                        val p = profileRepo.getProfile()
                        "العمر: ${p.identity.age} سنة | النوم: ${p.health.sleepQuality} | النشاط: ${p.health.dailyActivity}"
                    }
                }

                "get_health_notes" -> {
                    val obs = healthRepo?.getRecentObservations(10) ?: emptyList()
                    if (obs.isNotEmpty()) {
                        obs.joinToString("\n") { "- [${it.category}] ${it.observationText}" }
                    } else {
                        val mems = memoryRepo.getAllMemoriesList().filter {
                            it.category == MemoryCategory.HEALTH || it.category == MemoryCategory.HEALTH_DATA
                        }
                        if (mems.isEmpty()) "لا توجد ملاحظات صحية سابقة مسجلة."
                        else mems.joinToString("\n") { "- ${it.content}" }
                    }
                }

                "save_health_note" -> {
                    val note =
                        arguments["note"]
                            ?.toString()
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                            ?: return "خطأ: نص الملاحظة فارغ"

                    val cat =
                        arguments["category"]
                            ?.toString()
                            ?.takeIf { it.isNotBlank() }
                            ?: "WELLNESS"

                    val obsId =
                        healthRepo?.addObservation(note, cat)
                            ?: return "تعذر حفظ الملاحظة الصحية."

                    "تم تسجيل الملاحظة الصحية في السجل الصحي (معرف: $obsId)."
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
                    val concept =
                        arguments["conceptKey"]
                            ?.toString()
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                            ?: return "مفتاح المفهوم مفقود."

                    val repo =
                        learningProgressRepo
                            ?: return "محرك التعلم غير متاح حالياً."

                    val existing =
                        repo.getProgress(concept)

                    repo.saveProgress(
                        conceptKey = concept,
                        currentLevel = maxOf(
                            existing?.currentLevel ?: 1,
                            2
                        ),
                        mastery = "MASTERED",
                        needsReview = false,
                        attempts = (existing?.attempts ?: 0) + 1,
                        successfulAttempts =
                            (existing?.successfulAttempts ?: 0) + 1
                    )

                    "تم تسجيل إتقان المفهوم ($concept) في سجل التعلم."
                }

                "mark_concept_needs_review" -> {
                    val concept =
                        arguments["conceptKey"]
                            ?.toString()
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                            ?: return "مفتاح المفهوم مفقود."

                    val repo =
                        learningProgressRepo
                            ?: return "محرك التعلم غير متاح حالياً."

                    val existing =
                        repo.getProgress(concept)

                    repo.saveProgress(
                        conceptKey = concept,
                        currentLevel =
                            existing?.currentLevel ?: 1,
                        mastery = "NEEDS_REVIEW",
                        needsReview = true,
                        attempts = (existing?.attempts ?: 0) + 1,
                        successfulAttempts =
                            existing?.successfulAttempts ?: 0
                    )

                    "تم وضع المفهوم ($concept) في قائمة المراجعة."
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

                "create_reminder" -> {
                    val title = arguments["title"]?.toString() ?: return "خطأ: عنوان التذكير فارغ"
                    val expr = arguments["timeExpression"]?.toString() ?: ""
                    val cat = arguments["category"]?.toString() ?: "GENERAL"
                    val isRecur =
                        (arguments["isRecurring"] as? Boolean) ?: false

                    val recurRule =
                        arguments["recurrenceRule"]
                            ?.toString()
                            ?.uppercase()
                            ?: "NONE"

                    val destination =
                        arguments["destination"]
                            ?.toString()
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }

                    if (isRecur && recurRule !in setOf("DAILY", "WEEKLY")) {
                        return "نوع التكرار غير واضح. اختاري يومياً أو أسبوعياً."
                    }

                    if (expr.isBlank()) {
                        return "وقتاش تحبي نذكرك يا أمي؟ الصباح ولا في الليل؟"
                    }

                    val parsed = com.example.ai.datetime.NaturalDateTimeParser.parse(expr)
                    if (parsed == null) {
                        return "سامحني يا أمي، ما فهمتش بالباهي الوقت اللي تحبي نذكرك فيه. تحبي نذكّرك الصباح وإلا في الليل؟"
                    }
                    if (parsed.isAmbiguous) {
                        return parsed.disambiguationQuestion ?: "يا أمي، تقصدي الوقت هذا الصباح ولا في الليل؟"
                    }

                    val triggerMillis = parsed.timeMillis
                    val timeHint = parsed.formattedHint

                    val id = reminderRepo?.addReminder(
                        title = title,
                        triggerMillis = triggerMillis,
                        timeHint = timeHint,
                        category = cat,
                        isRecurring = isRecur,
                        recurrenceRule = if (isRecur) recurRule else null
                    ) ?: 0L

                    val feedbackMsg = if (id > 0 && reminderScheduler != null) {
                        val res = reminderScheduler.scheduleReminder(
                            id = id,
                            title = title,
                            triggerMillis = triggerMillis,
                            category = cat,
                            destination = destination,
                            isRecurring = isRecur,
                            recurrenceRule = if (isRecur) recurRule else null,
                            originalTriggerMillis = triggerMillis
                        )
                        res.feedbackMessage
                    } else {
                        "تمت إضافة التذكير."
                    }
                    "تمت جدولة التذكير بنجاح: \"$title\" في الوقت المحدد ($timeHint). $feedbackMsg"
                }

                "get_active_reminders" -> {
                    val list = reminderRepo?.getUpcomingReminders() ?: emptyList()
                    if (list.isEmpty()) {
                        "لا توجد مواعيد أو تذكيرات قادمة مسجلة حالياً."
                    } else {
                        val formatted = list.mapIndexed { i, r -> "${i + 1}. ${r.title} - ${r.timeHint}" }
                        "التذكيرات والمواعيد القادمة:\n" + formatted.joinToString("\n")
                    }
                }

                "start_reading_session" -> {
                    val cid = arguments["contentId"]?.toString() ?: "content_heart_health"
                    val content = contentRepo?.getContentById(cid)
                    val requestedDuration =
                        (arguments["durationMinutes"] as? Number)?.toInt()

                    val contentDuration = content?.estimatedMinutes

                    val dur = requestedDuration
                        ?: contentDuration
                        ?: return "ما عنديش مدة واضحة لجلسة القراءة. قوليلي مثلاً 10 دقايق ولا 20 دقيقة."

                    val title = content?.title ?: "قراءة هادئة"
                    contentRepo?.startReadingSession(
                        cid,
                        title,
                        dur * 60
                    )
                    "بدأت جلسة القراءة لمدة $dur دقائق: $title."
                }

                "start_focus_session" -> {
                    val dur = (arguments["durationMinutes"] as? Number)?.toInt() ?: 15
                    val title = arguments["activityTitle"]?.toString() ?: "قراءة هادئة وتركيز"
                    focusRepo?.startFocusSession(dur, title, 1)
                    "بدأت جلسة التركيز لمدة $dur دقيقة بنجاح: $title."
                }

                "update_profile_from_conversation" -> {
                    val current = profileRepo.getProfile()

                    val name =
                        arguments["name"]?.toString()
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                            ?: current.identity.name

                    val age =
                        (arguments["age"] as? Number)
                            ?.toInt()
                            ?.takeIf { it > 0 }
                            ?: current.identity.age

                    val location =
                        arguments["generalLocation"]?.toString()
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                            ?: current.identity.generalLocation

                    val dialect =
                        arguments["dialect"]?.toString()
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                            ?: current.identity.preferredLanguage

                    val activity =
                        arguments["dailyActivity"]?.toString()
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                            ?: current.health.dailyActivity

                    val sleep =
                        arguments["sleepQuality"]?.toString()
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                            ?: current.health.sleepQuality

                    val goals = parseCommaSeparated(arguments["learningGoals"])
                    val interests = parseCommaSeparated(arguments["learningInterests"])
                    val readingPreferences = parseCommaSeparated(
                        arguments["readingPreferences"]
                    )

                    val updated = current.copy(
                        identity = current.identity.copy(
                            name = name,
                            age = age,
                            generalLocation = location,
                            preferredLanguage = dialect
                        ),
                        learning = current.learning.copy(
                            learningGoals =
                                if (goals.isNotEmpty()) goals
                                else current.learning.learningGoals,

                            learningInterests =
                                if (interests.isNotEmpty()) interests
                                else current.learning.learningInterests
                        ),
                        health = current.health.copy(
                            dailyActivity = activity,
                            sleepQuality = sleep,

                            healthGoals =
                                if (
                                    arguments["primaryGoal"]
                                        ?.toString()
                                        ?.isNotBlank() == true
                                ) {
                                    listOf(
                                        arguments["primaryGoal"]!!.toString().trim()
                                    )
                                } else {
                                    current.health.healthGoals
                                }
                        ),
                        preferences = current.preferences.copy(
                            readingPreferences =
                                if (readingPreferences.isNotEmpty()) {
                                    readingPreferences
                                } else {
                                    current.preferences.readingPreferences
                                }
                        )
                    )

                    profileRepo.updateProfile(updated)

                    val hp = healthRepo?.getHealthProfile()

                    if (hp != null) {
                        healthRepo.saveHealthProfile(
                            hp.copy(
                                age = age,
                                activityLevel = activity,
                                sleepQuality = sleep,
                                generalGoals =
                                    if (arguments["primaryGoal"]
                                            ?.toString()
                                            ?.isNotBlank() == true
                                    ) {
                                        arguments["primaryGoal"]!!.toString().trim()
                                    } else {
                                        hp.generalGoals
                                    }
                            )
                        )
                    }

                    "تم تحديث المعلومات التي صرحت بها أمي في الملف المناسب."
                }

                "create_french_lesson" -> {
                    val requestedWord =
                        arguments["frenchWord"]
                            ?.toString()
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }

                    // If the user simply asks:
                    // "علمني كلمة فرنسية"
                    // use the real lesson generator.
                    if (requestedWord == null) {
                        val generator = FrenchLessonGenerator(frenchRepo)
                        val lesson = generator.generateOrPickNextLesson()

                        """
                            درس فرنسي جديد:
                            الكلمة: ${lesson.word}
                            النطق: ${lesson.arabicPhonetics}
                            المعنى: ${lesson.arabicMeaning}
                            السياق التونسي: ${lesson.tunisianEverydayContext}
                            المثال: ${lesson.exampleDailySentence}
                            ${lesson.interactivePrompt}
                        """.trimIndent()
                    } else {
                        // If Gemini explicitly requested a particular word,
                        // preserve that behavior.
                        val phonetic =
                            arguments["phoneticArabic"]?.toString() ?: ""

                        val meaning =
                            arguments["tunisianMeaning"]?.toString() ?: ""

                        val example =
                            arguments["examplePhrase"]?.toString() ?: ""

                        val category =
                            arguments["category"]?.toString() ?: "DAILY_LIFE"

                        if (phonetic.isBlank() || meaning.isBlank()) {
                            "باش نعلمك الكلمة هذي بطريقة صحيحة، نحتاج معناها والنطق متاعها."
                        } else {
                            val existing = frenchRepo.getAllWordsList()
                                .firstOrNull {
                                    it.frenchWord.equals(
                                        requestedWord,
                                        ignoreCase = true
                                    )
                                }

                            if (existing != null) {
                                "الكلمة $requestedWord موجودة من قبل في دروسك."
                            } else {
                                val all = frenchRepo.getAllWordsList()

                                val newId =
                                    (all.maxOfOrNull { it.id } ?: 0) + 1

                                val entity =
                                    com.example.data.local.entity.FrenchWordEntity(
                                        id = newId,
                                        frenchWord = requestedWord,
                                        arabicPhonetics = phonetic,
                                        arabicMeaning = meaning,
                                        tunisianEverydayContext =
                                            "سياق الاستعمال اليومي: $example",
                                        medicalContext =
                                            if (
                                                category == "MEDICAL" ||
                                                category == "PHARMACY"
                                            ) {
                                                "سياق طبي وصيدلي"
                                            } else {
                                                ""
                                            },
                                        exampleDailySentence = example,
                                        exampleMedicalSentence = "",
                                        interactivePrompt =
                                            "قولي معايا يا أمي: $phonetic ($requestedWord)",
                                        isMastered = false
                                    )

                                frenchRepo.insertWord(entity)

                                "تمت إضافة الكلمة الفرنسية \"$requestedWord\" إلى دروسك."
                            }
                        }
                    }
                }

                "log_health_checkin" -> {
                    val water = (arguments["waterGlasses"] as? Number)?.toInt()
                    val sys = (arguments["bloodPressureSystolic"] as? Number)?.toInt()
                    val dia = (arguments["bloodPressureDiastolic"] as? Number)?.toInt()
                    val notes = arguments["feelingNotes"]?.toString() ?: ""

                    val noteParts = mutableListOf<String>()
                    if (water != null && water > 0) noteParts.add("شربت $water كؤوس ماء")
                    if (sys != null && dia != null) noteParts.add("قياس ضغط الدم: $sys/$dia")
                    if (notes.isNotBlank()) noteParts.add(notes)

                    val summaryNote = if (noteParts.isNotEmpty()) noteParts.joinToString(" - ") else "متابعة صحية روتينية"
                    val observationId =
                        healthRepo?.addObservation(summaryNote, "CHECKIN")

                    if (observationId == null || observationId <= 0L) {
                        return "تعذر حفظ المتابعة الصحية."
                    }

                    if (water != null && water > 0) {
                        val hp = healthRepo?.getHealthProfile()
                        if (hp != null) {
                            healthRepo.saveHealthProfile(
                                hp.copy(
                                    currentWaterGlasses =
                                        hp.currentWaterGlasses + water
                                )
                            )
                        }
                    }

                    "تم تسجيل المتابعة الصحية بنجاح: $summaryNote"
                }

                else -> "أداة غير معروفة: $toolName"
            }
        } catch (e: Exception) {
            "حدث خطأ أثناء تنفيذ الأداة: ${e.message}"
        }
    }
}
