package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.RafiqahViewModel
import com.example.ui.french.FrenchScreen
import com.example.ui.health.HealthScreen
import com.example.ui.home.HomeScreen
import com.example.ui.learning.FocusSessionScreen
import com.example.ui.learning.LearningScreen
import com.example.ui.planner.PlannerScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.stories.StoryScreen
import com.example.ui.voice.VoiceAssistantScreen

object RafiqahDestinations {
    const val HOME = "home"
    const val VOICE = "voice"
    const val LEARNING = "learning"
    const val FOCUS_SESSION = "focus_session"
    const val READING = "reading"
    const val STORY = "story"
    const val HEALTH = "health"
    const val PLANNER = "planner"
    const val FRENCH = "french"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
}

@Composable
fun RafiqahNavGraph(
    viewModel: RafiqahViewModel,
    modifier: Modifier = Modifier,
    startDestination: String = RafiqahDestinations.HOME,
    navController: NavHostController = rememberNavController()
) {
    val profile by viewModel.profile.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val activeReminders by viewModel.activeReminders.collectAsState()
    val frenchWords by viewModel.frenchWords.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val liveVoiceState by viewModel.liveVoiceState.collectAsState()
    val liveTranscript by viewModel.liveTranscript.collectAsState()
    val isLiveSessionActive by viewModel.isLiveSessionActive.collectAsState()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsState()
    val geminiConnectionStatus by viewModel.geminiConnectionStatus.collectAsState()
    val maskedApiKey by viewModel.maskedApiKey.collectAsState()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(RafiqahDestinations.HOME) {
            HomeScreen(
                profile = profile,
                isSpeaking = isSpeaking,
                onNavigateToVoice = { navController.navigate(RafiqahDestinations.VOICE) },
                onNavigateToLearning = { navController.navigate(RafiqahDestinations.LEARNING) },
                onNavigateToStory = { navController.navigate(RafiqahDestinations.STORY) },
                onNavigateToHealth = { navController.navigate(RafiqahDestinations.HEALTH) },
                onNavigateToPlanner = { navController.navigate(RafiqahDestinations.PLANNER) },
                onNavigateToFrench = { navController.navigate(RafiqahDestinations.FRENCH) },
                onNavigateToProfile = { navController.navigate(RafiqahDestinations.PROFILE) },
                onNavigateToSettings = { navController.navigate(RafiqahDestinations.SETTINGS) },
                onNavigateToReading = { navController.navigate(RafiqahDestinations.READING) },
                onSpeakGreeting = { viewModel.speakText(it) }
            )
        }

        composable(RafiqahDestinations.VOICE) {
            VoiceAssistantScreen(
                messages = messages,
                liveVoiceState = liveVoiceState,
                geminiConnectionStatus = geminiConnectionStatus,
                liveTranscript = liveTranscript,
                isLiveSessionActive = isLiveSessionActive,
                pendingConfirmation = pendingConfirmation,
                onConfirmAction = { viewModel.confirmPendingAction() },
                onDismissAction = { viewModel.dismissPendingAction() },
                onStartLiveSession = { viewModel.startLiveVoiceSession() },
                onStopLiveSession = { viewModel.stopLiveVoiceSession() },
                onInterruptVoice = { viewModel.interruptLiveVoice() },
                onReconnectVoice = { viewModel.reconnectLiveVoice() },
                onSendMessage = { viewModel.sendVoiceMessage(it) },
                onSpeak = { viewModel.speakText(it) },
                onStopSpeech = { viewModel.stopSpeaking() },
                onNavigateToSettings = { navController.navigate(RafiqahDestinations.SETTINGS) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(RafiqahDestinations.LEARNING) {
            LearningScreen(
                onNavigateBack = { navController.popBackStack() },
                onStartFocusSession = { navController.navigate(RafiqahDestinations.FOCUS_SESSION) },
                onSpeak = { viewModel.speakText(it) },
                onSaveProgress = { concept, isCorrect ->
                    viewModel.saveLearningProgress(concept, isCorrect)
                }
            )
        }

        composable(RafiqahDestinations.FOCUS_SESSION) {
            val activeFocusSession by viewModel.focusRepo.getActiveFocusSessionFlow().collectAsState(initial = null)
            val blockingStatus by viewModel.appBlockingStatus.collectAsState()
            val targetMinutes = activeFocusSession?.targetDurationMinutes ?: 15
            val actTitle = activeFocusSession?.focusActivityTitle ?: "جلسة تركيز وقراءة هادئة"

            FocusSessionScreen(
                targetDurationMinutes = targetMinutes,
                activityTitle = actTitle,
                blockingStatusMessage = blockingStatus.statusMessage,
                onNavigateBack = {
                    viewModel.endFocusSession()
                    navController.popBackStack()
                },
                onSpeak = { viewModel.speakText(it) },
                onSessionFinished = {
                    viewModel.endFocusSession()
                    viewModel.saveLearningProgress("جلسة تركيز: $actTitle", true)
                }
            )
        }

        composable(RafiqahDestinations.READING) {
            val contentList by viewModel.allContentItems.collectAsState()
            val firstItem = contentList.firstOrNull()
            com.example.ui.reading.ReadingScreen(
                contentItem = firstItem,
                onCompleteReading = { secs ->
                    firstItem?.let { item ->
                        viewModel.saveReadingProgress(item.id, secs.toInt(), true)
                    }
                },
                onSaveElapsedProgress = { elapsed, completed ->
                    firstItem?.let { item ->
                        viewModel.saveReadingProgress(item.id, elapsed, completed)
                    }
                },
                onConceptEvaluated = { conceptKey, isUnderstood ->
                    viewModel.evaluateConceptFromReading(conceptKey, isUnderstood)
                },
                onNavigateBack = { navController.popBackStack() },
                onSpeak = { viewModel.speakText(it) }
            )
        }

        composable(RafiqahDestinations.STORY) {
            StoryScreen(
                chapters = chapters,
                onNavigateBack = { navController.popBackStack() },
                onSpeak = { viewModel.speakText(it) },
                onCompleteChapter = { chapterNumber ->
                    viewModel.completeChapter(chapterNumber)
                }
            )
        }

        composable(RafiqahDestinations.HEALTH) {
            HealthScreen(
                healthProfile = profile.health,
                onNavigateBack = { navController.popBackStack() },
                onSpeak = { viewModel.speakText(it) }
            )
        }

        composable(RafiqahDestinations.PLANNER) {
            PlannerScreen(
                tasks = tasks,
                reminders = activeReminders,
                onToggleTaskCompleted = { id, completed -> viewModel.toggleTask(id, completed) },
                onAddTask = { title, timeHint -> viewModel.addTask(title, timeHint) },
                onAddReminder = { title, timeExpr -> viewModel.addNaturalReminder(title, timeExpr) },
                onCancelReminder = { id -> viewModel.cancelReminder(id) },
                onNavigateBack = { navController.popBackStack() },
                onSpeak = { viewModel.speakText(it) }
            )
        }

        composable(RafiqahDestinations.FRENCH) {
            FrenchScreen(
                words = frenchWords,
                onToggleMastered = { id, mastered -> viewModel.toggleFrenchMastered(id, mastered) },
                onSpeakFrench = { viewModel.speakText(it, "fr") },
                onSpeakExplanation = { viewModel.speakText(it, "ar") },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(RafiqahDestinations.PROFILE) {
            ProfileScreen(
                profile = profile,
                memories = memories,
                onDeleteMemory = { viewModel.deleteMemory(it) },
                onNavigateBack = { navController.popBackStack() },
                onSpeak = { viewModel.speakText(it) }
            )
        }

        composable(RafiqahDestinations.SETTINGS) {
            SettingsScreen(
                connectionStatus = geminiConnectionStatus,
                maskedApiKey = maskedApiKey,
                onSaveApiKey = { viewModel.saveApiKey(it) },
                onClearApiKey = { viewModel.clearApiKey() },
                onTestConnection = { viewModel.testGeminiConnection() },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
