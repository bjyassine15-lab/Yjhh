package com.example.ai.learning

import com.example.data.local.entity.FrenchWordEntity
import com.example.data.repository.FrenchWordRepository

data class GeneratedFrenchLesson(
    val word: String,
    val arabicPhonetics: String,
    val arabicMeaning: String,
    val tunisianEverydayContext: String,
    val medicalContext: String,
    val exampleDailySentence: String,
    val exampleMedicalSentence: String,
    val interactivePrompt: String
)

/**
 * Generative French lesson provider for Rafiqah V3.
 * Generates tailored French vocabulary with Tunisian dialect context,
 * saving them to Room so they enter the Spaced Repetition pipeline.
 */
class FrenchLessonGenerator(
    private val frenchRepo: FrenchWordRepository
) {
    // High-yield vocabulary bank for medical, pharmacy, daily health, and administrative contexts in Tunisia
    private val curatedLessonPool = listOf(
        GeneratedFrenchLesson(
            word = "Consultation",
            arabicPhonetics = "كونسولطاسيون",
            arabicMeaning = "كشف طبي أو استشارة عند الطبيب",
            tunisianEverydayContext = "في تونس نقولو: 'اليوم عندي كونسولطاسيون عند الطبيب باش يشوفلي التحاليل'.",
            medicalContext = "الفحص الطبي السريري اللي يقعد فيه الطبيب يسمعك ويفحصك.",
            exampleDailySentence = "مشيت الصباح بكري باش نخلط على دوري في الكونسولطاسيون.",
            exampleMedicalSentence = "طبيب العائلة حددلي كونسولطاسيون كل 3 شهور للمتابعة.",
            interactivePrompt = "قولي معايا يا أمي: كونسولطاسيون!"
        ),
        GeneratedFrenchLesson(
            word = "Pharmacie",
            arabicPhonetics = "فارماسي",
            arabicMeaning = "الصيدلية",
            tunisianEverydayContext = "ديما نقولو: 'هابطة للفارماسي باش ناخذ الدوا'.",
            medicalContext = "المكان المخصص لصرف الأدوية وتقديم الإرشادات الصيدلانية السليمة.",
            exampleDailySentence = "الفارماسي اللي بحذانا تحل حتى في الليل.",
            exampleMedicalSentence = "الصيدلي في الفارماسي شرحلي كيفاش ناخذ حبة الدوا بعد الفطور.",
            interactivePrompt = "كلمة ساهلة ومعروفة: فارماسي 🌸"
        ),
        GeneratedFrenchLesson(
            word = "Analyse",
            arabicPhonetics = "أناليز",
            arabicMeaning = "تحليل طبي أو مخبري",
            tunisianEverydayContext = "نقولو: 'مشيت للمخبر عملت أناليز دم'.",
            medicalContext = "فحص عينات الدم أو البول في المخبر للتأكد من نسبة السكر أو وظائف الكلى.",
            exampleDailySentence = "غدوة الصباح نمشي صايمة نعمل أناليز السكر.",
            exampleMedicalSentence = "نتيجة الأناليز طالعة ممتازة ومريقلة برشا.",
            interactivePrompt = "أناليز، تحاليل دورية تطمنا على صحتنا."
        ),
        GeneratedFrenchLesson(
            word = "Vitamines",
            arabicPhonetics = "فيتامين",
            arabicMeaning = "الفيتامينات المغذية للبدن",
            tunisianEverydayContext = "نقولو: 'البرتقال وزيت الزيتون معبيين بالفيتامين'.",
            medicalContext = "عناصر غذائية حيوية يحتاجها الجسم بكميات قليلة للحفاظ على المناعة ونشاط الخلايا.",
            exampleDailySentence = "كليت كعبة غلّة باش ناخذ فيتامينات طبيعية.",
            exampleMedicalSentence = "فيتامين د والشمس الصباحية مهمين برشا لتقوية العظام والمفاصل.",
            interactivePrompt = "فيتامين، يعطينا القوة والنشاط يا أمي."
        ),
        GeneratedFrenchLesson(
            word = "Régime",
            arabicPhonetics = "ريجيم",
            arabicMeaning = "نظام غذائي متوازن وصحي",
            tunisianEverydayContext = "نقولو: 'الطبيب قالي اعمل ريجيم خفيف ونقص الملح والدهنيات'.",
            medicalContext = "تعديل نمط الأكل لحماية الشرايين والتحكم في الوزن والضغط.",
            exampleDailySentence = "نتبع في ريجيم صحي فيه برشا خضرة وماء وشوربة دافية.",
            exampleMedicalSentence = "الريجيم قليل الملح يدعم صحة القلب ويساعد في المحافظة على استقرار الضغط.",
            interactivePrompt = "ريجيم، مش حرمان بل صحة وعافية."
        ),
        GeneratedFrenchLesson(
            word = "Docteur",
            arabicPhonetics = "دوكتور",
            arabicMeaning = "الطبيب",
            tunisianEverydayContext = "نقولو: 'صباح الخير دوكتور، ربي يعينك'.",
            medicalContext = "الطبيب المعالج المؤهل لتشخيص الحالات الصحية ورعايتها.",
            exampleDailySentence = "الدوكتور طمني وقالي صحتك لاباس ما شاء الله.",
            exampleMedicalSentence = "الدوكتور نصحني بالمشي يومياً لمدة عشرين دقيقة.",
            interactivePrompt = "دوكتور، كلمة احترام للأطباء اللي يعاونونا."
        )
    )

    suspend fun generateOrPickNextLesson(): GeneratedFrenchLesson {
        val existingWords = frenchRepo.getAllWordsList().map { it.frenchWord.lowercase().trim() }
        val candidate =
            curatedLessonPool.firstOrNull {
                it.word.lowercase().trim() !in existingWords
            }

        if (candidate == null) {
            return frenchRepo.getAllWordsList()
                .firstOrNull { !it.isMastered }
                ?.let { existing ->
                    GeneratedFrenchLesson(
                        word = existing.frenchWord,
                        arabicPhonetics = existing.arabicPhonetics,
                        arabicMeaning = existing.arabicMeaning,
                        tunisianEverydayContext =
                            existing.tunisianEverydayContext,
                        medicalContext =
                            existing.medicalContext,
                        exampleDailySentence =
                            existing.exampleDailySentence,
                        exampleMedicalSentence =
                            existing.exampleMedicalSentence,
                        interactivePrompt =
                            existing.interactivePrompt
                    )
                }
                ?: throw IllegalStateException(
                    "لا توجد كلمة فرنسية جديدة أو كلمة قيد المراجعة."
                )
        }

        // If it's a new word not yet in DB, persist it
        if (candidate.word.lowercase().trim() !in existingWords) {
            val all = frenchRepo.getAllWordsList()
            val newId = (all.maxOfOrNull { it.id } ?: 4) + 1
            // Save to database
            val entity = FrenchWordEntity(
                id = newId,
                frenchWord = candidate.word,
                arabicPhonetics = candidate.arabicPhonetics,
                arabicMeaning = candidate.arabicMeaning,
                tunisianEverydayContext = candidate.tunisianEverydayContext,
                medicalContext = candidate.medicalContext,
                exampleDailySentence = candidate.exampleDailySentence,
                exampleMedicalSentence = candidate.exampleMedicalSentence,
                interactivePrompt = candidate.interactivePrompt,
                isMastered = false
            )
            frenchRepo.insertWord(entity)
        }

        return candidate
    }
}
