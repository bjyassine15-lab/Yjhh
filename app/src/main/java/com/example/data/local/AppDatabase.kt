package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.DailyTaskDao
import com.example.data.local.dao.FrenchWordDao
import com.example.data.local.dao.MemoryDao
import com.example.data.local.dao.ProfileDao
import com.example.data.local.dao.StoryDao
import com.example.data.local.entity.DailyTaskEntity
import com.example.data.local.entity.FrenchWordEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.ProfileEntity
import com.example.data.local.entity.StoryChapterEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProfileEntity::class,
        MemoryEntity::class,
        StoryChapterEntity::class,
        DailyTaskEntity::class,
        FrenchWordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun memoryDao(): MemoryDao
    abstract fun storyDao(): StoryDao
    abstract fun dailyTaskDao(): DailyTaskDao
    abstract fun frenchWordDao(): FrenchWordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rafiqah_database"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    INSTANCE?.let { database ->
                        seedInitialData(database)
                    }
                }
            }
        }

        suspend fun seedInitialData(database: AppDatabase) {
            // Seed profile
            database.profileDao().insertOrUpdateProfile(
                ProfileEntity(
                    id = 1,
                    name = "أمي الحبيبة",
                    age = 53,
                    preferredLanguage = "العربية - التونسية المبسطة",
                    speakingStyle = "حنون، هادئ ومبسط",
                    medicineKnowledgeLevel = "مبتدئة محبة للتعلم",
                    scienceLevel = "معرفة عامة طيبة",
                    frenchLevel = "تونسية يومية",
                    lastLessonTitle = "الخلية: مدينة الحياة العجيبة",
                    lastChapterNumber = 1,
                    progressPercent = 20,
                    heightCm = 162f,
                    weightKg = 68f,
                    dailyActivity = "مشي خفيف وأشغال البيت",
                    sleepQuality = "نوم متقطع أحيانًا",
                    studyTime = "المساء بعد المغرب",
                    restTimes = "القيلولة بعد الظهر",
                    sessionDurationMinutes = 7
                )
            )

            // Seed initial long term memories
            database.memoryDao().insertMemory(
                MemoryEntity(
                    category = "USER_FACT",
                    content = "أمي عمرها 53 سنة، تحب الهدوء، وتتعلم أحسن كي نعطيوها أمثلة من الكوجينة والدار والواقع التونسي.",
                    importance = 5,
                    source = "ملف شخصي مؤسس"
                )
            )
            database.memoryDao().insertMemory(
                MemoryEntity(
                    category = "HEALTH_DATA",
                    content = "تاخذ حبة دواء الضغط صباحًا بعد الفطور، ونومها متقطع في بعض الليالي.",
                    importance = 5,
                    source = "ملاحظة صحية دورية"
                )
            )
            database.memoryDao().insertMemory(
                MemoryEntity(
                    category = "PREFERENCE",
                    content = "تفضل الحديث الصوتي التونسي الهادئ، وجلسة لا تتجاوز 7 إلى 10 دقائق.",
                    importance = 4,
                    source = "تفضيلات المحادثة"
                )
            )

            // Seed story chapters for "سارة... والطريق إلى الطب"
            val chapters = listOf(
                StoryChapterEntity(
                    chapterNumber = 1,
                    title = "لماذا أريد أن أفهم جسم الإنسان؟",
                    hook = "في ذلك الصباح، سارة ما كانتش تعرف أن كلمة صغيرة سمعتها من الطبيب باش تبدل طريقتها في فهم الحياة...",
                    storyBody = "كانت سارة قاعدة في قاعة الانتظار، شادة ورقة التحاليل في يدها. شافت الطبيب يشرح لمريضة أخرى بكل لطف كيفاش حبة دواء صغيرة تنجم تعاون القلب.\n\nفي اللحظة هاذيك، سارة سألت روحها: كيفاش جسمنا اللي نعيشو بيه كل يوم فيه كل ها المعجزات وحنا ما نعرفوش أسرارو؟\n\nقررت سارة تبدأ رحلة جديدة، خطوة بخطوة، تفهم كيفاش خلق ربي هالبدن العجيب، بلا تعقيد ولا كلمات صعبة.",
                    scientificConceptKey = "CELL_INTRO",
                    tunisianAudioScript = "صباح الخير يا أمي العزيزة. اليوم سارة تبدأ أول خطوة في رحلتها. حبت تفهم كيفاش جسمنا يخدم، وباش نرافقوها خطوة بخطوة.",
                    isUnlocked = true,
                    isCompleted = false,
                    characterNote = "سارة: امرأة في مقتبل رحلتها المعرفية، تسأل أسئلة ذكية من واقع الحياة."
                ),
                StoryChapterEntity(
                    chapterNumber = 2,
                    title = "الشيء الصغير الذي لم أفهمه",
                    hook = "شنوّة أصغر حاجة حية في جسمنا؟ سارة كانت تظن أن أصغر حاجة هي قطرة الدم...",
                    storyBody = "روّحت سارة للدار، وبدات تغزر لكف يدها. فكرت: الجلد هذا، والعينين، والقلب اللي يدق، من شنوّة مبنيين؟\n\nاكتشفت أن كيما الدار تتبنى بآلاف الياجورات، جسم الإنسان مبني من مليارات الحبات الحية الصغيرة اللي اسمها 'الخلايا'.\n\nكل خلية عايشة، تتنفس، وتخدم ليل مع نهار باش نحنا نعيشو في صحة وعافية.",
                    scientificConceptKey = "CELL_AS_BRICK",
                    tunisianAudioScript = "تخيلي يا أمي، كيما داركم مبنية بالياجورة بالياجورة، بدنك مبني بحبيبات صغيرة اسمهم الخلايا.",
                    isUnlocked = true,
                    isCompleted = false,
                    characterNote = "سارة تتعرف على فكرة أن الجسم مجموعة عوالم متناهية الصغر."
                ),
                StoryChapterEntity(
                    chapterNumber = 3,
                    title = "مدينة لا تراها العين",
                    hook = "تخيلي أن عندك مدينة كاملة، فيها معامل ومحطة طاقة وباب محروس، لكنها أصغر من شعرة الرأس!",
                    storyBody = "دخلت سارة في عالم الخلية. ما لقاتش مجرد كورة صغيرة، لقات مدينة حقيقية!\n\nفي الوسط فما قصر الحكم اللي فيه أسرار العائلة والتعليمات، واسمو 'النواة'.\nوفما محطة التوليد اللي تعطي الضو والقوة للمدينة واسمها 'الميتوكوندريا'.\nوداير بالمدينة سور ذكي ما يدخل كان اللي ينفع، اسمو 'الغشاء الخلوي'.",
                    scientificConceptKey = "CELL_ORGANELLES",
                    tunisianAudioScript = "الخلية ماهيش حبة فارغة، هي مدينة فيها إدارة وفيها محطة طاقة وفيها حراس على الباب.",
                    isUnlocked = true,
                    isCompleted = false,
                    characterNote = "تشبيه الخلية بمدينة نشيطة ومنظمة."
                ),
                StoryChapterEntity(
                    chapterNumber = 4,
                    title = "سر الخلية ومحطة الطاقة",
                    hook = "منين تجينا الطاقة كي ناكلو كعبة تمر ولا نشربو قهيوة؟",
                    storyBody = "سارة كانت تتساءل: كيفاش الماكلة اللي ناكلوها ترجع قوة في عضلاتنا؟\n\nالجواب في 'الميتوكوندريا'. هي الكوزينة ومحطة الطاقة متاع الخلية. تاخذ السكر والأكسجين من النفس، وتحوّلهم لطاقة نظيفة تخلينا نمشيو ونفكرو ونبتسمو.",
                    scientificConceptKey = "MITOCHONDRIA_ENERGY",
                    tunisianAudioScript = "الميتوكوندريا هي الكوزينة ومحطة الكهرباء في الخلية، تعطينا القوة من الماكلة والنفس.",
                    isUnlocked = false,
                    isCompleted = false,
                    characterNote = "سارة تفهم مبدأ الحيوية والنشاط."
                ),
                StoryChapterEntity(
                    chapterNumber = 5,
                    title = "الحارسة على باب الخلية",
                    hook = "لو كان يدخل أي شيء للخلية، شنوا يصير؟ شكون اللي يحميها؟",
                    storyBody = "الغشاء الخلوي هو الحارس الوفي. عندو بوابات ذكية: يعرف البوتاسيوم والصوديوم، يفتح للماء والغذاء، ويسكّر الباب قدام السموم.\n\nكي نشربو الماء وناكلو ماكلة باهية، الحراس يخدمو مرتاحين والدنيا تمشي منظمة.",
                    scientificConceptKey = "CELL_MEMBRANE",
                    tunisianAudioScript = "الغشاء الخلوي كيما عساس الدار، ذكي يعرف شكون يدخل وشكون يقعد لبرة.",
                    isUnlocked = false,
                    isCompleted = false,
                    characterNote = "سارة تدرك أهمية الحماية والتغذية السليمة."
                )
            )
            database.storyDao().insertChapters(chapters)

            // Seed daily tasks
            val tasks = listOf(
                DailyTaskEntity(
                    title = "دواء الضغط مع فطور الصباح",
                    category = "MEDICINE",
                    timeHint = "08:30 صباحًا",
                    isCompleted = false,
                    note = "حبة واحدة بعد شرب كأس ماء وفطور هادئ",
                    isPriority = true
                ),
                DailyTaskEntity(
                    title = "جلسة رفيقة: حكاية الخلية والطب",
                    category = "STUDY",
                    timeHint = "11:00 صباحًا أو المساء",
                    isCompleted = false,
                    note = "7 دقايق تركيز ممتعة مع سارة",
                    isPriority = true
                ),
                DailyTaskEntity(
                    title = "قيلولة واستراحة طيبة",
                    category = "REST",
                    timeHint = "14:00 بعد الظهر",
                    isCompleted = false,
                    note = "إراحة العينين والبدن في غرفة مهواة وهادئة",
                    isPriority = false
                ),
                DailyTaskEntity(
                    title = "شرب كأسين ماء ومشي خفيف",
                    category = "HEALTH_HABIT",
                    timeHint = "17:30 العشية",
                    isCompleted = false,
                    note = "حركة خفيفة وتنشيط الدورة الدموية",
                    isPriority = true
                )
            )
            database.dailyTaskDao().insertTasks(tasks)

            // Seed French words
            val words = listOf(
                FrenchWordEntity(
                    id = 1,
                    frenchWord = "Rendez-vous",
                    arabicPhonetics = "راندي-فو",
                    arabicMeaning = "موعد محدد",
                    tunisianEverydayContext = "نستعملوها ديما: 'عندي رانديفو عند الطبيب' ولا 'رانديفو في البوسطة'.",
                    medicalContext = "الموعد اللي يحددوهولك في الكلينيك أو عند طبيب الاختصاص.",
                    exampleDailySentence = "عندي رانديفو غدوة الصباح مع جارتي فاطمة.",
                    exampleMedicalSentence = "عندي رانديفو مراقبة عند طبيب القلب نهار الثلاثاء.",
                    interactivePrompt = "قولي معايا يا أمي: راندي-فو!",
                    isMastered = false
                ),
                FrenchWordEntity(
                    id = 2,
                    frenchWord = "Ordonnance",
                    arabicPhonetics = "أوردونوس",
                    arabicMeaning = "الوصفة الطبية المكتوبة",
                    tunisianEverydayContext = "الورقة اللي يعطيهالك الطبيب باش تشري بيها الدوا من الصيدلية.",
                    medicalContext = "الوثيقة الرسمية اللي فيها أسماء الأدوية ومقاديرها وأوقاتها.",
                    exampleDailySentence = "خبي الأوردونوس في الصاك باش ما تضيعش كي تمشي للفارماسي.",
                    exampleMedicalSentence = "الطبيب كتبلي أوردونوس فيها فيتامين ودواء الضغط.",
                    interactivePrompt = "أوردونوس، كلمة ساهلة ومهمة برشا في تونس.",
                    isMastered = false
                ),
                FrenchWordEntity(
                    id = 3,
                    frenchWord = "Tension",
                    arabicPhonetics = "تونسيو",
                    arabicMeaning = "ضغط الدم الشرياني",
                    tunisianEverydayContext = "كي نقولو: 'قست التونسيو اليوم؟' أو 'تونسيو طالعة شوية'.",
                    medicalContext = "قوة جريان الدم في العروق، يلزمها تكون متوازنة ومستقرة.",
                    exampleDailySentence = "اليوم قست التونسيو في الصيدلية ولقيتها 12 على 8، ممتازة.",
                    exampleMedicalSentence = "شرب الماء ونقصان الملح يعاون التونسيو تقعد مريقلة.",
                    interactivePrompt = "تونسيو، نحبوها ديما رايضة ومستقرة يا أمي.",
                    isMastered = false
                ),
                FrenchWordEntity(
                    id = 4,
                    frenchWord = "Contrôle",
                    arabicPhonetics = "كونترول",
                    arabicMeaning = "مراقبة وفحص دوري",
                    tunisianEverydayContext = "نقولو: 'ماشية نعمل كونترول عند الطبيب نطمن على صحتي'.",
                    medicalContext = "فحص وقائي مش بالضرورة تكون مريض، فقط للتثبت والمتابعة.",
                    exampleDailySentence = "طبيب العيون قالي إيجاني بعد ستة شهور نعملو كونترول.",
                    exampleMedicalSentence = "الكونترول المنتظم يخلينا ديما متطمنين على صحتنا.",
                    interactivePrompt = "كونترول يطمنا ويريح بالنا.",
                    isMastered = false
                )
            )
            database.frenchWordDao().insertWords(words)
        }
    }
}
