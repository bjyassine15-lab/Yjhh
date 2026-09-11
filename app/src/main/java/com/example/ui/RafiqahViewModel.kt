package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiAIService
import com.example.ai.LiveVoiceService
import com.example.ai.LiveVoiceState
import com.example.ai.MockAIService
import com.example.ai.VoiceService
import com.example.ai.auth.ConnectionTestResult
import com.example.ai.auth.DevelopmentGeminiAuthProvider
import com.example.ai.auth.DynamicGeminiAuthProvider
import com.example.ai.auth.EncryptedGeminiApiKeyStore
import com.example.ai.auth.GeminiApiKeyStore
import com.example.ai.auth.GeminiAuthProvider
import com.example.ai.auth.GeminiConnectionStatus
import com.example.ai.context.ContextBuilder
import com.example.ai.learning.LearningEngine
import com.example.ai.live.GeminiLiveService
import com.example.ai.memory.MemoryManager
import com.example.ai.repository.AIRepository
import com.example.ai.safety.MedicalSafetyGuard
import com.example.ai.story.StoryEngine
import com.example.ai.tools.ToolExecutor
import com.example.data.local.AppDatabase
import com.example.data.repository.DailyPlannerRepository
import com.example.data.repository.FrenchWordRepository
import com.example.data.repository.LearningProgressRepository
import com.example.data.repository.MemoryRepository
import com.example.data.repository.ProfileRepository
import com.example.data.repository.StoryRepository
import com.example.domain.model.ConversationMessage
import com.example.domain.model.DailyTask
import com.example.domain.model.FrenchWordItem
import com.example.domain.model.MemoryCategory
import com.example.domain.model.MemoryItem
import com.example.domain.model.MessageSender
import com.example.domain.model.MotherProfile
import com.example.domain.model.StoryChapter
import com.example.domain.model.TaskCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

data class PendingActionConfirmation(
    val title: String,
    val description: String,
    val onConfirmAction: () -> Unit,
    val onRejectAction: () -> Unit = {}
)

class RafiqahViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val profileRepo = ProfileRepository(db.profileDao())
    val memoryRepo = MemoryRepository(db.memoryDao())
    val storyRepo = StoryRepository(db.storyDao())
    val plannerRepo = DailyPlannerRepository(db.dailyTaskDao())
    val frenchRepo = FrenchWordRepository(db.frenchWordDao())
    val learningProgressRepo = LearningProgressRepository(db.learningProgressDao())
    val healthRepo = com.example.data.repository.HealthRepository(db.healthDao())
    val routineRepo = com.example.data.repository.RoutineRepository(db.routineDao())
    val contentRepo = com.example.data.repository.ContentRepository(db.contentDao())
    val focusRepo = com.example.data.repository.FocusRepository(db.focusDao())
    val reminderRepo = com.example.data.repository.ReminderRepository(db.reminderDao())
    val reminderScheduler = com.example.service.reminder.ReminderScheduler(application)

    val spacedRepetitionEngine = com.example.ai.learning.SpacedRepetitionEngine(learningProgressRepo)
    val contentSelectionEngine = com.example.ai.learning.ContentSelectionEngine(
        contentRepo = contentRepo,
        contentDao = db.contentDao(),
        learningRepo = learningProgressRepo,
        spacedRepetition = spacedRepetitionEngine
    )
    val dailyRoutineEngine = com.example.ai.routine.DailyRoutineEngine(
        routineRepo = routineRepo,
        healthRepo = healthRepo,
        reminderRepo = reminderRepo,
        learningRepo = learningProgressRepo,
        spacedRepetition = spacedRepetitionEngine,
        contentSelectionEngine = contentSelectionEngine
    )

    // Gemini Authentication Abstraction & Secure Store (V2.6)
    val keyStore: GeminiApiKeyStore = EncryptedGeminiApiKeyStore(application)
    val authProvider: DynamicGeminiAuthProvider = DynamicGeminiAuthProvider(keyStore)

    val geminiConnectionStatus: StateFlow<GeminiConnectionStatus> = authProvider.connectionStatus

    private val _maskedApiKey = MutableStateFlow(keyStore.getMaskedApiKey())
    val maskedApiKey: StateFlow<String?> = _maskedApiKey.asStateFlow()

    fun saveApiKey(key: String) {
        val trimmed = key.trim()
        keyStore.saveApiKey(trimmed)
        _maskedApiKey.value = keyStore.getMaskedApiKey()
        authProvider.updateStatus(
            if (trimmed.isNotBlank()) GeminiConnectionStatus.CONFIGURED else GeminiConnectionStatus.NOT_CONFIGURED
        )
    }

    fun clearApiKey() {
        keyStore.clearApiKey()
        _maskedApiKey.value = null
        authProvider.updateStatus(GeminiConnectionStatus.NOT_CONFIGURED)
    }

    suspend fun testGeminiConnection(): ConnectionTestResult {
        return authProvider.testConnection()
    }

    val toolExecutor = ToolExecutor(
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
        learningProgressRepo = learningProgressRepo
    )

    val appBlockingController = com.example.service.focus.AppBlockingController(application, focusRepo)
    val frenchLessonGenerator = com.example.ai.learning.FrenchLessonGenerator(frenchRepo)
    val appBlockingStatus = appBlockingController.status

    val voiceService = VoiceService(application)
    val geminiLiveService = GeminiLiveService(
        context = application,
        fallbackVoiceService = voiceService,
        authProvider = authProvider,
        toolExecutor = toolExecutor
    )
    val liveVoiceService = LiveVoiceService(application, voiceService, geminiLiveService)

    val learningEngine = LearningEngine(repository = learningProgressRepo)
    val storyEngine = StoryEngine()
    val medicalSafetyGuard = MedicalSafetyGuard()
    val memoryManager = MemoryManager()
    val contextBuilder = ContextBuilder()

    val geminiAIService = GeminiAIService(
        authProvider = authProvider,
        fallbackService = MockAIService(),
        toolExecutor = toolExecutor
    )

    val aiRepository = AIRepository(
        aiService = geminiAIService,
        toolExecutor = toolExecutor,
        memoryRepo = memoryRepo,
        contextBuilder = contextBuilder,
        memoryManager = memoryManager,
        medicalSafetyGuard = medicalSafetyGuard,
        learningEngine = learningEngine,
        storyEngine = storyEngine
    )

    // Data Flows from Room
    val profile: StateFlow<MotherProfile> = profileRepo.profileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MotherProfile())

    val memories: StateFlow<List<MemoryItem>> = memoryRepo.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chapters: StateFlow<List<StoryChapter>> = storyRepo.allChapters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<DailyTask>> = plannerRepo.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val frenchWords: StateFlow<List<FrenchWordItem>> = frenchRepo.allWords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // V3 Reactive Flows
    val healthProfile = healthRepo.getHealthProfileFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val microSessions = routineRepo.getMicroSessionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allContentItems = contentRepo.getAllContentFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeReminders = reminderRepo.getAllActiveRemindersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val focusSessions = focusRepo.getAllSessionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedApps = focusRepo.getBlockedAppsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Voice and Live Conversation states
    val isSpeaking: StateFlow<Boolean> = voiceService.isSpeaking
    val liveVoiceState: StateFlow<LiveVoiceState> = liveVoiceService.connectionState
    val liveTranscript: StateFlow<String> = liveVoiceService.liveTranscript
    val isLiveSessionActive: StateFlow<Boolean> = liveVoiceService.isSessionActive

    // Dynamically selected reading content via ContentSelectionEngine (never arbitrary firstOrNull)
    private val _selectedReadingContent = MutableStateFlow<com.example.data.local.entity.ContentItemEntity?>(null)
    val selectedReadingContent: StateFlow<com.example.data.local.entity.ContentItemEntity?> = _selectedReadingContent.asStateFlow()

    fun prepareReadingContent(conceptKey: String? = null) {
        viewModelScope.launch {
            val content = contentSelectionEngine.selectContentForSession(
                sessionType = "READING",
                preferredConceptKey = conceptKey
            )
            _selectedReadingContent.value = content
        }
    }

    // Pending natural confirmation for sensitive actions
    private val _pendingConfirmation = MutableStateFlow<PendingActionConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingActionConfirmation?> = _pendingConfirmation.asStateFlow()

    fun requestActionConfirmation(
        title: String,
        description: String,
        onConfirm: () -> Unit,
        onReject: () -> Unit = {}
    ) {
        _pendingConfirmation.value = PendingActionConfirmation(
            title = title,
            description = description,
            onConfirmAction = {
                onConfirm()
                _pendingConfirmation.value = null
            },
            onRejectAction = {
                onReject()
                _pendingConfirmation.value = null
            }
        )
    }

    // Pending approval for sensitive memory items (e.g. medical conditions / medications)
    private val _pendingMemoryApproval = MutableStateFlow<MemoryManager.MemoryCandidate?>(null)
    val pendingMemoryApproval: StateFlow<MemoryManager.MemoryCandidate?> = _pendingMemoryApproval.asStateFlow()

    private val _messages = MutableStateFlow<List<ConversationMessage>>(
        listOf(
            ConversationMessage(
                id = "welcome_v2",
                sender = MessageSender.RAFIQAH,
                text = "على سلامتك يا أمي الحبيبة 🌷 نهارك مبروك وهادي. أنا رفيقتك ومعاك خطوة بخطوة، تحبي نحكيو على صحتك، ولا نكملو قصة سارة في الطب، ولا نشوفو كلمة جديدة بالفرنسية؟",
                spokenDialectText = "على سلامتك يا أمي الغالية، ربي ينور نهارك. أنا رفيقتك وهنا ديما باش نونسك ونفرحك."
            )
        )
    )
    val messages: StateFlow<List<ConversationMessage>> = _messages.asStateFlow()

    init {
        viewModelScope.launch {
            AppDatabase.seedInitialData(db)
            dailyRoutineEngine.generateOrRefreshDailyPlan()
            prepareReadingContent()
        }
        // Collect live tool events for logging and UI awareness
        viewModelScope.launch {
            geminiLiveService.toolCallEvents.collect { event ->
                val notice = "استدعاء أداة: ${event.functionName}"
                android.util.Log.d("RafiqahViewModel", notice)
            }
        }
        // Synchronize Gemini Live tool confirmations with UI
        viewModelScope.launch {
            geminiLiveService.pendingToolConfirmation.collect { liveConf ->
                if (liveConf != null) {
                    _pendingConfirmation.value = PendingActionConfirmation(
                        title = liveConf.title,
                        description = liveConf.description,
                        onConfirmAction = {
                            viewModelScope.launch {
                                geminiLiveService.confirmLiveTool(liveConf.callId)
                            }
                            _pendingConfirmation.value = null
                        },
                        onRejectAction = {
                            viewModelScope.launch {
                                geminiLiveService.rejectLiveTool(liveConf.callId)
                            }
                            _pendingConfirmation.value = null
                        }
                    )
                }
            }
        }
    }

    fun getDynamicHomeGreeting(): String {
        val currentProfile = profile.value
        val name = currentProfile.identity.name
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val timeGreeting = if (hour < 12) "صباح الخير والياسمين" else if (hour < 18) "نهارك مبروك ومزين" else "مساء النور والراحة"

        val lastChapter = currentProfile.learning.lastChapterNumber
        return "$timeGreeting يا $name 🌷\nأنت توقفت عند الفصل $lastChapter في رحلة سارة، ونطمنوا على صحتك ونومك اليوم."
    }

    fun sendVoiceMessage(userText: String) {
        val userMsg = ConversationMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.USER,
            text = userText
        )
        _messages.value = _messages.value + userMsg

        viewModelScope.launch {
            val currentProfile = profile.value
            val currentMemories = memories.value
            val currentChapter = chapters.value.find { it.chapterNumber == currentProfile.learning.lastChapterNumber }
            val currentTasks = tasks.value

            val response = aiRepository.processUserSpeech(
                userSpeech = userText,
                profile = currentProfile,
                recentMemories = currentMemories,
                currentChapter = currentChapter,
                todayTasks = currentTasks
            )

            val rafiqahMsg = ConversationMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.RAFIQAH,
                text = response.replyText,
                spokenDialectText = response.spokenDialectText
            )
            _messages.value = _messages.value + rafiqahMsg

            // Speak the reply in warm dialect
            voiceService.speak(response.spokenDialectText, "ar")

            // Check if sensitive memory candidate requires explicit approval
            if (response.pendingMemoryCandidate != null) {
                _pendingMemoryApproval.value = response.pendingMemoryCandidate
            }

            // Check if tool execution requires confirmation
            val pending = response.pendingActionConfirmation
            if (pending != null) {
                _pendingConfirmation.value = PendingActionConfirmation(
                    title = pending.confirmationTitle,
                    description = pending.confirmationPrompt,
                    onConfirmAction = {
                        viewModelScope.launch {
                            val result = aiRepository.executeConfirmedTool(pending.toolName, pending.arguments)
                            speakText(result, "ar")
                        }
                        _pendingConfirmation.value = null
                    },
                    onRejectAction = {
                        viewModelScope.launch {
                            val declineNotice = aiRepository.handleRejectedConfirmation(pending.toolName)
                            speakText(declineNotice, "ar")
                        }
                        _pendingConfirmation.value = null
                    }
                )
            }
        }
    }

    fun confirmPendingAction() {
        _pendingConfirmation.value?.onConfirmAction?.invoke()
    }

    fun dismissPendingAction() {
        val pending = _pendingConfirmation.value
        _pendingConfirmation.value = null
        pending?.onRejectAction?.invoke()
    }

    fun approveMemoryCandidate() {
        val candidate = _pendingMemoryApproval.value ?: return
        viewModelScope.launch {
            aiRepository.approveAndSaveMemory(candidate)
            _pendingMemoryApproval.value = null
            speakText("تم حفظ المعلومة في الذاكرة يا أمي 🌷", "ar")
        }
    }

    fun rejectMemoryCandidate() {
        _pendingMemoryApproval.value = null
        speakText("باهي يا أمي، لم يتم حفظ هذه المعلومة 🌷", "ar")
    }

    // Live Voice Engine controls
    fun startLiveVoiceSession() {
        liveVoiceService.startLiveSession { _, _ -> }
    }

    fun onLiveVoiceInput(speech: String) {
        liveVoiceService.onUserSpeechInput(speech) { userSpeech, reply, spoken ->
            val userMsg = ConversationMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.USER,
                text = userSpeech
            )
            val rafiqahMsg = ConversationMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.RAFIQAH,
                text = reply,
                spokenDialectText = spoken
            )
            _messages.value = _messages.value + userMsg + rafiqahMsg
        }
    }

    fun interruptLiveVoice() {
        liveVoiceService.interrupt()
    }

    fun stopLiveVoiceSession() {
        liveVoiceService.endSession()
    }

    fun reconnectLiveVoice() {
        liveVoiceService.reconnect()
    }

    fun speakText(text: String, languageTag: String = "ar") {
        voiceService.speak(text, languageTag)
    }

    fun stopSpeaking() {
        voiceService.stop()
    }

    fun completeChapter(chapterNumber: Int) {
        viewModelScope.launch {
            storyRepo.completeChapter(chapterNumber)

            val currentChapters = storyRepo.getAllChaptersList()
            val storyProgress = storyEngine.calculateStoryProgress(currentChapters, chapterNumber)

            profileRepo.updateLearningProgress(
                lastChapterNumber = chapterNumber,
                progressPercent = storyProgress.percentCompleted,
                lastLessonTitle = "الفصل $chapterNumber في رحلة سارة"
            )
            memoryRepo.saveMemoryWithDeduplication(
                content = "أكملت أمي الفصل $chapterNumber من رواية سارة بنجاح واهتمام.",
                category = MemoryCategory.STORY,
                importance = 4,
                source = "رواية سارة"
            )
        }
    }

    fun toggleTask(id: Long, completed: Boolean) {
        viewModelScope.launch {
            plannerRepo.setTaskCompleted(id, completed)
        }
    }

    fun addTask(title: String, timeHint: String) {
        viewModelScope.launch {
            plannerRepo.addTask(
                title = title,
                category = TaskCategory.HEALTH_HABIT,
                timeHint = timeHint,
                isPriority = false
            )
        }
    }

    fun toggleFrenchMastered(id: Int, mastered: Boolean) {
        viewModelScope.launch {
            frenchRepo.setWordMastered(id, mastered)
            if (mastered) {
                memoryRepo.saveMemoryWithDeduplication(
                    content = "حفظت أمي كلمة بالفرنسية وطبقتها في سياقها التونسي.",
                    category = MemoryCategory.FRENCH,
                    importance = 3,
                    source = "تعلم الفرنسية"
                )
            }
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            memoryRepo.deleteMemory(id)
        }
    }

    fun completeMicroSession(id: String) {
        viewModelScope.launch {
            routineRepo.markCompleted(id)
        }
    }

    fun skipMicroSession(id: String) {
        viewModelScope.launch {
            routineRepo.markSkipped(id)
        }
    }

    fun addNaturalReminder(
        title: String,
        timeExpression: String,
        category: String = "GENERAL",
        isRecurring: Boolean = false,
        onFeedback: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            if (timeExpression.isBlank()) {
                onFeedback?.invoke("وقتاش تحبي نذكرك يا أمي؟ الصباح وإلا في الليل؟")
                return@launch
            }
            val parsed = com.example.ai.datetime.NaturalDateTimeParser.parse(timeExpression)
            if (parsed == null) {
                onFeedback?.invoke("سامحني يا أمي، ما فهمتش بالباهي وقتاش تحبي نذكّرك. الصباح وإلا في الليل؟")
                return@launch
            }
            if (parsed.isAmbiguous) {
                onFeedback?.invoke(parsed.disambiguationQuestion ?: "يا أمي، تقصدي الوقت هذا الصباح ولا في الليل؟")
                return@launch
            }
            val trigger = parsed.timeMillis
            val hint = parsed.formattedHint
            val id = reminderRepo.addReminder(title, trigger, hint, category, isRecurring)
            val res = reminderScheduler.scheduleReminder(id, title, trigger, category, isRecurring = isRecurring)
            onFeedback?.invoke("تمت جدولة التذكير بنجاح: \"$title\" ($hint). ${res.feedbackMessage}")
        }
    }

    fun cancelReminder(id: Long) {
        viewModelScope.launch {
            reminderRepo.cancelReminder(id)
            reminderScheduler.cancelReminder(id)
        }
    }

    fun startFocusSession(minutes: Int, activityTitle: String, requireConfirmation: Boolean = false) {
        val action = {
            viewModelScope.launch {
                focusRepo.startFocusSession(minutes, activityTitle, 1)
                appBlockingController.activateFocusBlocking()
            }
        }
        if (requireConfirmation) {
            requestActionConfirmation(
                title = "بدء جلسة تركيز مع حجب التطبيقات",
                description = "هل تودين بدء جلسة تركيز لمدة $minutes دقيقة؟ سيتم حجب التطبيقات المحددة للمساعدة على الهدوء والتركيز.",
                onConfirm = { action() }
            )
        } else {
            action()
        }
    }

    fun endFocusSession(interrupted: Boolean = false, elapsedSeconds: Int = 0) {
        viewModelScope.launch {
            appBlockingController.deactivateFocusBlocking()
            val active = focusRepo.getActiveFocusSession()
            if (active != null) {
                val duration = if (elapsedSeconds > 0) elapsedSeconds else active.targetDurationMinutes * 60
                focusRepo.completeFocusSession(
                    id = active.id,
                    actualSecs = duration,
                    completedSuccessfully = !interrupted,
                    interrupted = interrupted
                )
            }
        }
    }

    fun toggleAppBlock(packageName: String, blocked: Boolean) {
        viewModelScope.launch {
            focusRepo.setAppBlocked(packageName, blocked)
            appBlockingController.refreshStatus()
        }
    }

    fun startRoutineSession(sessionId: String) {
        viewModelScope.launch {
            dailyRoutineEngine.startSession(sessionId)
        }
    }

    fun completeRoutineSession(sessionId: String) {
        viewModelScope.launch {
            dailyRoutineEngine.completeSession(sessionId)
        }
    }

    fun skipRoutineSession(sessionId: String) {
        viewModelScope.launch {
            dailyRoutineEngine.skipSession(sessionId)
        }
    }

    fun saveDetailedReadingProgress(
        contentId: String,
        requiredElapsed: Int,
        optionalElapsed: Int,
        isCompleted: Boolean,
        quizUnderstood: Boolean?
    ) {
        viewModelScope.launch {
            val totalElapsed = requiredElapsed + optionalElapsed
            contentRepo.recordReadingProgress(
                contentId = contentId,
                elapsedSeconds = totalElapsed,
                isCompleted = isCompleted
            )
            if (isCompleted) {
                routineRepo.getActiveMicroSessions().find { it.contentId == contentId }?.let {
                    dailyRoutineEngine.completeSession(it.id)
                }
            }
            if (quizUnderstood != null) {
                val content = contentRepo.getContentById(contentId)
                val conceptKey = content?.relatedConceptKey ?: "reading_concept"
                spacedRepetitionEngine.recordAttempt(conceptKey, quizUnderstood)
                val statusText = if (quizUnderstood) "أتقنت أمي مفهوم" else "يحتاج مفهوم"
                memoryRepo.saveMemoryWithDeduplication(
                    content = "$statusText $conceptKey من جلسة القراءة.",
                    category = MemoryCategory.LEARNING,
                    importance = 4,
                    source = "اختبار الفهم بجلسة القراءة"
                )
            }
        }
    }

    fun saveReadingProgress(contentId: String, elapsedSeconds: Int, isCompleted: Boolean) {
        saveDetailedReadingProgress(contentId, elapsedSeconds, 0, isCompleted, null)
    }

    fun evaluateConceptFromReading(conceptKey: String, isUnderstood: Boolean) {
        viewModelScope.launch {
            spacedRepetitionEngine.recordAttempt(conceptKey, isUnderstood)
            val statusText = if (isUnderstood) "أتقنت أمي مفهوم" else "يحتاج مفهوم"
            memoryRepo.saveMemoryWithDeduplication(
                content = "$statusText $conceptKey ${if (isUnderstood) "بنجاح" else "مراجعة إضافية"}.",
                category = MemoryCategory.LEARNING,
                importance = 4,
                source = "جلسة القراءة والمراجعة"
            )
        }
    }

    fun rescheduleRoutineSession(sessionId: String, newTimeHint: String) {
        viewModelScope.launch {
            dailyRoutineEngine.rescheduleMissedActivity(sessionId, newTimeHint)
        }
    }

    fun saveLearningProgress(concept: String, isCorrect: Boolean) {
        viewModelScope.launch {
            if (isCorrect) {
                learningEngine.onUserUnderstood(concept)
            } else {
                learningEngine.onUserConfused(concept)
            }
            val statusText = if (isCorrect) "أجابت أمي إجابة صحيحة وفهمت" else "راجعت أمي مفهوم"
            memoryRepo.saveMemoryWithDeduplication(
                content = "$statusText $concept.",
                category = MemoryCategory.LEARNING,
                importance = 4,
                source = "اختبار الفهم السريع"
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        liveVoiceService.endSession()
        voiceService.shutdown()
    }
}
