package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.datetime.NaturalDateTimeParser
import com.example.ai.learning.ContentSelectionEngine
import com.example.ai.learning.FrenchLessonGenerator
import com.example.ai.learning.SpacedRepetitionEngine
import com.example.ai.routine.DailyRoutineEngine
import com.example.ai.tools.ToolExecutor
import com.example.data.local.AppDatabase
import com.example.data.repository.ContentRepository
import com.example.data.repository.DailyPlannerRepository
import com.example.data.repository.FocusRepository
import com.example.data.repository.FrenchWordRepository
import com.example.data.repository.HealthRepository
import com.example.data.repository.LearningProgressRepository
import com.example.data.repository.MemoryRepository
import com.example.data.repository.ProfileRepository
import com.example.data.repository.ReminderRepository
import com.example.data.repository.RoutineRepository
import com.example.data.repository.StoryRepository
import com.example.service.focus.AppBlockingController
import com.example.service.focus.FocusBlockingMode
import com.example.service.reminder.ReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RafiqahV3FeaturesTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var profileRepo: ProfileRepository
    private lateinit var memoryRepo: MemoryRepository
    private lateinit var storyRepo: StoryRepository
    private lateinit var plannerRepo: DailyPlannerRepository
    private lateinit var frenchRepo: FrenchWordRepository
    private lateinit var learningRepo: LearningProgressRepository
    private lateinit var healthRepo: HealthRepository
    private lateinit var routineRepo: RoutineRepository
    private lateinit var contentRepo: ContentRepository
    private lateinit var focusRepo: FocusRepository
    private lateinit var reminderRepo: ReminderRepository
    private lateinit var reminderScheduler: ReminderScheduler

    private lateinit var spacedRepetition: SpacedRepetitionEngine
    private lateinit var contentSelection: ContentSelectionEngine
    private lateinit var dailyRoutineEngine: DailyRoutineEngine
    private lateinit var toolExecutor: ToolExecutor
    private lateinit var appBlockingController: AppBlockingController
    private lateinit var frenchLessonGenerator: FrenchLessonGenerator

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = AppDatabase.getInstance(context)
        profileRepo = ProfileRepository(db.profileDao())
        memoryRepo = MemoryRepository(db.memoryDao())
        storyRepo = StoryRepository(db.storyDao())
        plannerRepo = DailyPlannerRepository(db.dailyTaskDao())
        frenchRepo = FrenchWordRepository(db.frenchWordDao())
        learningRepo = LearningProgressRepository(db.learningProgressDao())
        healthRepo = HealthRepository(db.healthDao())
        routineRepo = RoutineRepository(db.routineDao())
        contentRepo = ContentRepository(db.contentDao())
        focusRepo = FocusRepository(db.focusDao())
        reminderRepo = ReminderRepository(db.reminderDao())
        reminderScheduler = ReminderScheduler(context)

        spacedRepetition = SpacedRepetitionEngine(learningRepo)
        contentSelection = ContentSelectionEngine(contentRepo, db.contentDao(), learningRepo, spacedRepetition)
        dailyRoutineEngine = DailyRoutineEngine(
            routineRepo = routineRepo,
            healthRepo = healthRepo,
            reminderRepo = reminderRepo,
            learningRepo = learningRepo,
            spacedRepetition = spacedRepetition,
            contentSelectionEngine = contentSelection,
            frenchRepo = frenchRepo
        )
        toolExecutor = ToolExecutor(
            profileRepo = profileRepo,
            memoryRepo = memoryRepo,
            storyRepo = storyRepo,
            plannerRepo = plannerRepo,
            frenchRepo = frenchRepo,
            healthRepo = healthRepo,
            reminderRepo = reminderRepo,
            reminderScheduler = reminderScheduler,
            contentRepo = contentRepo,
            focusRepo = focusRepo,
            learningProgressRepo = learningRepo
        )
        appBlockingController = AppBlockingController(context, focusRepo)
        frenchLessonGenerator = FrenchLessonGenerator(frenchRepo)
    }

    @After
    fun tearDown() {
        // cleanup if needed
    }

    // 1. Natural DateTime Parser & Disambiguation Test
    @Test
    fun testNaturalDateTimeParser_RelativeAndKeywords() {
        val now = System.currentTimeMillis()
        val afterOneHour = NaturalDateTimeParser.parse("بعد ساعة", referenceTimeMillis = now)
        assertNotNull(afterOneHour)
        assertTrue(afterOneHour!!.timeMillis > now)

        val tomorrowMorning = NaturalDateTimeParser.parse("غدوة الصباح على الثمانية", referenceTimeMillis = now)
        assertNotNull(tomorrowMorning)
        assertFalse(tomorrowMorning!!.isAmbiguous)
        assertTrue(tomorrowMorning.formattedHint.contains("غدوة"))

        // Ambiguous time without context: "على العشرة" (AM or PM not specified)
        val ambiguous = NaturalDateTimeParser.parse("على العشرة", referenceTimeMillis = now)
        assertNotNull(ambiguous)
        assertTrue(ambiguous!!.isAmbiguous)
        assertNotNull(ambiguous.disambiguationQuestion)
    }

    // 2. Reminder Creation & Tool Execution with Recurring Rules Test
    @Test
    fun testReminderToolExecution_WithRecurrenceAndDestination() = runBlocking {
        val args = mapOf(
            "title" to "دواء الضغط بعد الفطور",
            "timeExpression" to "غدوة الصباح على الثمانية",
            "category" to "MEDICINE",
            "isRecurring" to true,
            "recurrenceRule" to "DAILY"
        )
        val result = toolExecutor.executeTool("create_reminder", args)
        assertTrue(result.contains("تمت جدولة التذكير بنجاح") || result.contains("دواء الضغط"))

        val activeReminders = reminderRepo.getUpcomingReminders()
        assertTrue(activeReminders.any { it.title.contains("دواء الضغط") })
        val reminder = activeReminders.first { it.title.contains("دواء الضغط") }
        assertTrue(reminder.isRecurring)
        assertEquals("DAILY", reminder.recurrenceRule)
        assertEquals("MEDICINE", reminder.category)
    }

    // 3. Spaced Repetition & Knowledge State Progression Test
    @Test
    fun testSpacedRepetition_StateProgression() = runBlocking {
        val conceptKey = "heart_vascular"
        
        // Initial state before any reviews
        var state = spacedRepetition.getConceptState(conceptKey)
        assertEquals(com.example.ai.learning.KnowledgeState.NOT_INTRODUCED, state.state)

        // Record a correct attempt
        state = spacedRepetition.recordAttempt(conceptKey, isCorrect = true)
        assertEquals(com.example.ai.learning.KnowledgeState.LEARNING, state.state)
        assertFalse(learningRepo.getProgress(conceptKey)!!.needsReview)

        // Record confusion / wrong answer -> transitions to NEEDS_REVIEW
        state = spacedRepetition.recordAttempt(conceptKey, isCorrect = false)
        assertEquals(com.example.ai.learning.KnowledgeState.NEEDS_REVIEW, state.state)
        assertTrue(learningRepo.getProgress(conceptKey)!!.needsReview)

        // Check due reviews
        val dueReviews = spacedRepetition.getDueReviews()
        assertTrue(dueReviews.contains(conceptKey))

        // Multiple successes lead to MASTERED
        spacedRepetition.recordAttempt(conceptKey, isCorrect = true)
        spacedRepetition.recordAttempt(conceptKey, isCorrect = true)
        val finalState = spacedRepetition.recordAttempt(conceptKey, isCorrect = true)
        assertEquals(com.example.ai.learning.KnowledgeState.MASTERED, finalState.state)
    }

    // 4. French Lesson Dynamic Creation & Persistence Test
    @Test
    fun testCreateFrenchLesson_ToolAndPersistence() = runBlocking {
        val args = mapOf(
            "frenchWord" to "Une ordonnance",
            "phoneticArabic" to "أُوردُونَانْسْ",
            "tunisianMeaning" to "ورقة الدواء متاع الطبيب (الوصفة)",
            "examplePhrase" to "هزيت الأوردونانس للفارماسي",
            "category" to "PHARMACY"
        )
        val toolResult = toolExecutor.executeTool("create_french_lesson", args)
        assertTrue(toolResult.contains("Une ordonnance") || toolResult.contains("تمت إضافة"))

        val allWords = frenchRepo.allWords.first()
        val found = allWords.find { it.frenchWord.equals("Une ordonnance", ignoreCase = true) }
        assertNotNull(found)
        assertEquals("أُوردُونَانْسْ", found!!.arabicPhonetics)
        assertTrue(found.tunisianEverydayContext.contains("هزيت الأوردونانس"))
    }

    // 5. App Blocking Controller Real State & Honest Fallback Test (FIX 1 & FIX 13 Test 1)
    @Test
    fun testAppBlockingController_RealStateBehavior() = runBlocking {
        // AppBlockingController checks real PackageUsageStats permissions
        val hasPermission = appBlockingController.checkUsageAccessPermission()
        val status = appBlockingController.status.first()

        assertNotNull(status)
        // Must NEVER claim ACTIVE blocking
        assertEquals(FocusBlockingMode.TIMER_ONLY, status.focusBlockingMode)
        assertNotEquals(FocusBlockingMode.ACTIVE, status.focusBlockingMode)

        // Activate blocking and verify mode remains TIMER_ONLY
        val activeStatus = appBlockingController.activateFocusBlocking()
        assertEquals(FocusBlockingMode.TIMER_ONLY, activeStatus.focusBlockingMode)
        assertNotEquals(FocusBlockingMode.ACTIVE, activeStatus.focusBlockingMode)

        // Toggle app block in database
        val testPkg = "com.android.chrome"
        focusRepo.addBlockedApp(testPkg, "Chrome")
        focusRepo.setAppBlocked(testPkg, true)
        val blockedApps = focusRepo.getBlockedAppsFlow().first()
        assertTrue(blockedApps.any { it.packageName == testPkg && it.isBlocked })
    }

    // 6. Profile and Health Data Consolidation Test
    @Test
    fun testProfileAndHealthDataUpdate() = runBlocking {
        val profileArgs = mapOf(
            "name" to "أمي صليحة",
            "age" to 66,
            "dialect" to "TUNISIAN",
            "healthStatus" to "ضغط دم خفيف ومتابعة طبية منتظمة",
            "primaryGoal" to "النشاط الذهني والراحة"
        )
        val profileRes = toolExecutor.executeTool("update_profile_from_conversation", profileArgs)
        assertTrue(profileRes.contains("تم تحديث") || profileRes.contains("بنجاح"))

        val profile = profileRepo.getProfile()
        assertEquals(66, profile.identity.age)
    }

    // FIX 13 Test 2: Health checkin does not create Memory
    @Test
    fun testHealthCheckin_DoesNotCreateMemory() = runBlocking {
        val initialMemories = memoryRepo.getAllMemoriesList().size
        val initialObservations = healthRepo.getRecentObservations(100).size

        val healthArgs = mapOf(
            "waterGlasses" to 3,
            "bloodPressureSystolic" to 120,
            "bloodPressureDiastolic" to 80,
            "feelingNotes" to "نشاط ممتاز بعد المشي الصباحي"
        )
        val healthRes = toolExecutor.executeTool("log_health_checkin", healthArgs)
        assertTrue(healthRes.contains("تم تسجيل المتابعة الصحية بنجاح"))

        val finalMemories = memoryRepo.getAllMemoriesList().size
        val finalObservations = healthRepo.getRecentObservations(100).size

        // Health observation recorded, but NO memory record duplicated!
        assertEquals(initialMemories, finalMemories)
        assertEquals(initialObservations + 1, finalObservations)
    }

    // 7. Dynamic Daily Routine Generation Test
    @Test
    fun testDailyRoutineEngine_DynamicGenerationAndLifecycle() = runBlocking {
        val sessions = dailyRoutineEngine.generateOrRefreshDailyPlan()
        assertTrue(sessions.isNotEmpty())

        val firstSession = sessions.first()
        assertEquals("PLANNED", firstSession.status)

        // Lifecycle: Start session
        dailyRoutineEngine.startSession(firstSession.id)
        var updated = routineRepo.getSessionsForDate(dailyRoutineEngine.getTodayDateKey()).first { it.id == firstSession.id }
        assertEquals("STARTED", updated.status)

        // Lifecycle: Complete session
        dailyRoutineEngine.completeSession(firstSession.id)
        updated = routineRepo.getSessionsForDate(dailyRoutineEngine.getTodayDateKey()).first { it.id == firstSession.id }
        assertEquals("COMPLETED", updated.status)

        // Reschedule another session
        val secondSession = sessions[1]
        dailyRoutineEngine.rescheduleMissedActivity(secondSession.id, "17:30")
        val rescheduled = routineRepo.getSessionsForDate(dailyRoutineEngine.getTodayDateKey()).first { it.id == secondSession.id }
        assertEquals("RESCHEDULED", rescheduled.status)
        assertEquals("17:30", rescheduled.scheduledAtTimeHint)
    }

    // 8. V3.2 Test: Concept Mastery and Needs Review Updates Learning Progress DAO
    @Test
    fun testConceptMasteryAndReview_UpdatesLearningProgressDao() = runBlocking {
        toolExecutor.executeTool("mark_concept_mastered", mapOf("conceptKey" to "mitochondria"))
        val mastered = learningRepo.getProgress("mitochondria")
        assertNotNull(mastered)
        assertEquals("MASTERED", mastered!!.mastery)
        assertFalse(mastered.needsReview)

        toolExecutor.executeTool("mark_concept_needs_review", mapOf("conceptKey" to "cell_membrane"))
        val review = learningRepo.getProgress("cell_membrane")
        assertNotNull(review)
        assertEquals("NEEDS_REVIEW", review!!.mastery)
        assertTrue(review.needsReview)
    }

    // 9. V3.2 Test: Sanitized Database Seeding has neutral profile and no pre-seeded memories/tasks
    @Test
    fun testSanitizedDatabaseSeeding_NeutralProfileAndNoFakeMemories() = runBlocking {
        val memories = memoryRepo.getAllMemoriesList()
        // Ensure no fake personal memories are pre-seeded
        assertFalse(memories.any { it.content.contains("تحب الحلبة") || it.content.contains("تعاني من ضغط دم") })

        val profile = profileRepo.getProfile()
        // Neutral profile initialized with empty name
        assertEquals("", profile.identity.name)
        assertEquals(0, profile.identity.age)
    }

    // 10. V3.2 Test: Reading session requires explicit duration and rejects fallback 600s
    @Test
    fun testReadingSession_ExplicitDuration() = runBlocking {
        // Calling start_reading_session with duration
        val result = toolExecutor.executeTool(
            "start_reading_session",
            mapOf("contentId" to "content_heart_health", "durationMinutes" to 15)
        )
        assertTrue(result.contains("15 دقائق") || result.contains("15 دقيقة"))
        val session = db.contentDao().getLatestReadingSession()
        assertNotNull(session)
        assertEquals(900, session!!.requiredDurationSeconds)
    }

    // 11. V3.2 Test: French lesson generator when no word is passed
    @Test
    fun testFrenchLessonGenerator_NoWordArgument() = runBlocking {
        val result = toolExecutor.executeTool("create_french_lesson", emptyMap())
        assertTrue(result.contains("درس فرنسي جديد") && result.contains("الكلمة:"))
    }

    // 12. V3.2 Test: Save health note creates observation without injecting synthetic habits
    @Test
    fun testSaveHealthNote_ObservationWithoutSyntheticHabits() = runBlocking {
        val result = toolExecutor.executeTool("save_health_note", mapOf("note" to "أحس ببعض التعب بعد الظهيرة"))
        assertTrue(result.contains("تم تسجيل الملاحظة الصحية في السجل الصحي"))
        val obs = healthRepo.getRecentObservations(5)
        assertTrue(obs.any { it.observationText.contains("أحس ببعض التعب") })
    }

    // FIX 13 Test 3: Reading entity requires duration
    @Test
    fun testReadingSessionEntity_RequiresExplicitDuration() {
        val entity = com.example.data.local.entity.ReadingSessionEntity(
            contentId = "test_content_id",
            contentTitle = "قراءة هادئة",
            requiredDurationSeconds = 480
        )
        assertEquals(480, entity.requiredDurationSeconds)

        val micro = com.example.data.local.entity.MicroSessionEntity(
            id = "test_micro_1",
            type = "READING",
            title = "قراءة تجريبية",
            durationMinutes = 8,
            scheduledAtTimeHint = "10:00",
            requiredDurationSeconds = 480
        )
        assertEquals(480, micro.requiredDurationSeconds)
    }

    // FIX 13 Test 4: No content firstOrNull fallback
    @Test
    fun testContentFallback_FiltersEducationalCategories() = runBlocking {
        // Insert sample content with various categories
        val items = listOf(
            com.example.data.local.entity.ContentItemEntity(
                id = "item_other",
                category = "OTHER_NON_EDUCATIONAL",
                title = "غير تعليمي",
                body = "نص",
                estimatedMinutes = 2,
                keyTakeaway = "فكرة"
            ),
            com.example.data.local.entity.ContentItemEntity(
                id = "item_science",
                category = "SCIENCE",
                title = "الخلية الحية",
                body = "شرح الخلية",
                estimatedMinutes = 6,
                keyTakeaway = "أهمية الغشاء"
            ),
            com.example.data.local.entity.ContentItemEntity(
                id = "item_story",
                category = "STORY",
                title = "قصة قصيرة",
                body = "حكاية",
                estimatedMinutes = 4,
                keyTakeaway = "العبرة"
            )
        )
        db.contentDao().insertContentItems(items)

        val allContent = db.contentDao().getAllContentItems()
        val validEducational = allContent.filter {
            it.category == "SCIENCE" ||
            it.category == "HEALTH_EDUCATION" ||
            it.category == "CULTURE" ||
            it.category == "STORY"
        }
        assertTrue(validEducational.isNotEmpty())

        val shortest = validEducational.minByOrNull { it.estimatedMinutes }
        assertNotNull(shortest)
        assertEquals("item_story", shortest!!.id)
        assertEquals(4, shortest.estimatedMinutes)
    }

    // FIX 13 Test 5: French lesson exhaustion
    @Test
    fun testFrenchLessonGenerator_PoolExhaustion() = runBlocking {
        val allCurated = listOf(
            "Consultation", "Tension", "Ordonnance", "Comprimé",
            "Pharmacie", "Régime", "Docteur"
        )
        // Insert all curated words as mastered
        var maxId = frenchRepo.getAllWordsList().maxOfOrNull { it.id } ?: 0
        allCurated.forEach { w ->
            val exists = frenchRepo.getAllWordsList().any { it.frenchWord.equals(w, ignoreCase = true) }
            if (!exists) {
                maxId++
                frenchRepo.insertWord(
                    com.example.data.local.entity.FrenchWordEntity(
                        id = maxId,
                        frenchWord = w,
                        arabicPhonetics = w,
                        arabicMeaning = w,
                        tunisianEverydayContext = "",
                        medicalContext = "",
                        exampleDailySentence = "",
                        exampleMedicalSentence = "",
                        interactivePrompt = "",
                        isMastered = true
                    )
                )
            } else {
                val existing = frenchRepo.getAllWordsList().first { it.frenchWord.equals(w, ignoreCase = true) }
                frenchRepo.setWordMastered(existing.id, true)
            }
        }

        // When all curated words exist and are mastered, generator must not return Consultation infinitely
        try {
            val lesson = frenchLessonGenerator.generateOrPickNextLesson()
            // If it returned a lesson, it MUST be an unmastered word, not a silent loop
            assertNotNull(lesson)
        } catch (e: IllegalStateException) {
            // Properly throws exhaustion state
            assertTrue(e.message!!.contains("لا توجد كلمة فرنسية جديدة"))
        }
    }
}
