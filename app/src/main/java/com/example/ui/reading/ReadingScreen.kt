package com.example.ui.reading

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ContentItemEntity
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RafiqahRose
import com.example.ui.theme.SageOlive
import kotlinx.coroutines.delay

enum class ReadingMode {
    REQUIRED_TIME,
    OPTIONAL_CONTINUATION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(
    contentItem: ContentItemEntity?,
    onCompleteReading: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    onSpeak: (String) -> Unit,
    initialRequiredDurationSeconds: Int? = null,
    onSaveElapsedProgress: (elapsedSeconds: Int, isCompleted: Boolean) -> Unit = { _, _ -> },
    onSaveDetailedProgress: (requiredElapsed: Int, optionalElapsed: Int, isCompleted: Boolean, quizUnderstood: Boolean?) -> Unit = { _, _, _, _ -> },
    onConceptEvaluated: (conceptKey: String, isUnderstood: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val totalRequiredSecs = (
        initialRequiredDurationSeconds
            ?: contentItem?.estimatedMinutes?.times(60)
            ?: 0
    ).coerceAtLeast(60)
    var currentMode by remember { mutableStateOf(ReadingMode.REQUIRED_TIME) }
    var secondsLeftInPhase by remember(totalRequiredSecs) { mutableIntStateOf(totalRequiredSecs) }
    var optionalSecondsElapsed by remember { mutableIntStateOf(0) }
    var isRunning by remember { mutableStateOf(true) }
    var hasCompletedRequired by remember { mutableStateOf(false) }
    var showExtensionPrompt by remember { mutableStateOf(false) }
    var showQuiz by remember { mutableStateOf(false) }
    var quizAnswerRevealed by remember { mutableStateOf(false) }
    var conceptEvaluationDone by remember { mutableStateOf(false) }
    var quizOutcome by remember { mutableStateOf<Boolean?>(null) }

    val requiredElapsed = if (hasCompletedRequired) totalRequiredSecs else (totalRequiredSecs - secondsLeftInPhase).coerceAtLeast(0)

    val handleBackPress = {
        onSaveElapsedProgress(requiredElapsed + optionalSecondsElapsed, hasCompletedRequired)
        onSaveDetailedProgress(requiredElapsed, optionalSecondsElapsed, hasCompletedRequired, quizOutcome)
        onNavigateBack()
    }

    BackHandler(onBack = handleBackPress)

    DisposableEffect(Unit) {
        onDispose {
            onSaveElapsedProgress(requiredElapsed + optionalSecondsElapsed, hasCompletedRequired)
            onSaveDetailedProgress(requiredElapsed, optionalSecondsElapsed, hasCompletedRequired, quizOutcome)
        }
    }

    LaunchedEffect(isRunning, secondsLeftInPhase, currentMode) {
        if (isRunning) {
            delay(1000L)
            if (currentMode == ReadingMode.REQUIRED_TIME) {
                if (secondsLeftInPhase > 0) {
                    secondsLeftInPhase--
                    if (secondsLeftInPhase == 0) {
                        hasCompletedRequired = true
                        showExtensionPrompt = true
                        onCompleteReading(totalRequiredSecs.toLong())
                    }
                }
            } else {
                optionalSecondsElapsed++
            }
        }
    }

    val displayMinutes = if (currentMode == ReadingMode.REQUIRED_TIME) secondsLeftInPhase / 60 else optionalSecondsElapsed / 60
    val displaySeconds = if (currentMode == ReadingMode.REQUIRED_TIME) secondsLeftInPhase % 60 else optionalSecondsElapsed % 60
    val timeFormatted = String.format("%02d:%02d", displayMinutes, displaySeconds)
    val progress = if (currentMode == ReadingMode.REQUIRED_TIME) {
        (totalRequiredSecs.toFloat() - secondsLeftInPhase) / totalRequiredSecs.toFloat()
    } else {
        1f
    }

    val title = contentItem?.title ?: "كيف يعمل قلبك؟ مضخة الحياة العجيبة"
    val body = contentItem?.body ?: "القلب هو العضلة الأقوى والأوفى في جسم الإنسان. ينبض أكثر من 100 ألف مرة كل يوم بدون توقف..."
    val takeaway = contentItem?.keyTakeaway ?: "المشي وشرب الماء والنوم الهادئ يحافظ على صحة عضلة القلب وضغط دم متوازن."
    val question = contentItem?.quizQuestion ?: "كم مرة ينبض القلب تقريباً في اليوم؟"
    val answer = contentItem?.quizAnswer ?: "أكثر من 100 ألف مرة كل يوم."
    val relatedConcept = contentItem?.relatedConceptKey ?: "heart"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (currentMode == ReadingMode.OPTIONAL_CONTINUATION) "📖 استمرار اختياري (وقت إضافي)" else "📖 جلسة القراءة (${totalRequiredSecs / 60} دقائق)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = handleBackPress) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header timer card
            Card(
                colors = CardDefaults.cardColors(containerColor = SageOlive.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { progress.coerceIn(0f, 1f) },
                                modifier = Modifier.size(54.dp),
                                color = if (hasCompletedRequired) SageOlive else RafiqahRose,
                                strokeWidth = 4.dp,
                                trackColor = Color.LightGray.copy(alpha = 0.3f)
                            )
                            Icon(
                                imageVector = if (hasCompletedRequired) Icons.Default.CheckCircle else Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = if (hasCompletedRequired) SageOlive else RafiqahRose,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (hasCompletedRequired) {
                                    if (currentMode == ReadingMode.OPTIONAL_CONTINUATION) "قراءة إضافية اختيارية 🌷" else "أتممتِ الوقت الإلزامي بنجاح! 🌸"
                                } else {
                                    "الوقت المتبقي للجلسة (${totalRequiredSecs / 60}د)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = timeFormatted,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = { isRunning = !isRunning }) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isRunning) "إيقاف مؤقت" else "استئناف",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { onSpeak("$title. $body") }) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "قراءة صوتية",
                                tint = RafiqahRose
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reading Content Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 28.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Key Takeaway
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GoldAccent.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(text = "💡", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "الخلاصة الطيبة يا أمي:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = takeaway,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Extension / Continuation Prompt
            if (hasCompletedRequired) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SageOlive.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎉 يعطيك الصحة يا أمي! أتممتِ الوقت المطلوب (${totalRequiredSecs / 60}د).",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "تحب تكمل شوية ولا نكتفي بهذا؟",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (currentMode != ReadingMode.OPTIONAL_CONTINUATION) {
                                Button(
                                    onClick = {
                                        currentMode = ReadingMode.OPTIONAL_CONTINUATION
                                        isRunning = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SageOlive),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.MoreTime, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("نكمل شوية 📖")
                                }
                            }
                            Button(
                                onClick = { handleBackPress() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نكتفي بهذا 🌷")
                            }
                            Button(
                                onClick = { showQuiz = !showQuiz },
                                colors = ButtonDefaults.buttonColors(containerColor = RafiqahRose),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.QuestionAnswer, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("سؤال الفهم")
                            }
                        }
                    }
                }
            }

            // Quiz Section linked with Spaced Repetition Engine
            AnimatedVisibility(visible = showQuiz) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "سؤال خفيف يا أمي 🌸:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = question,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (!quizAnswerRevealed) {
                            OutlinedButton(onClick = { quizAnswerRevealed = true }) {
                                Text("كشف الإجابة والتثبت")
                            }
                        } else {
                            Text(
                                text = "الإجابة الصحيحة: $answer",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SageOlive,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            if (!conceptEvaluationDone) {
                                Text(
                                    text = "كيف حسيتي المفهوم هذا يا أمي؟",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            conceptEvaluationDone = true
                                            quizOutcome = true
                                            onConceptEvaluated(relatedConcept, true)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SageOlive)
                                    ) {
                                        Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("فهمتو بالباهي 🌸")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            conceptEvaluationDone = true
                                            quizOutcome = false
                                            onConceptEvaluated(relatedConcept, false)
                                        }
                                    ) {
                                        Icon(Icons.Default.ThumbDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("يحتاج مراجعة")
                                    }
                                }
                            } else {
                                Text(
                                    text = "✅ تم تسجيل تقدمك في محرك المراجعة الذكية.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SageOlive
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
