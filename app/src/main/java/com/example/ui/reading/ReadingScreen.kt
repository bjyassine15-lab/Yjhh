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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(
    contentItem: ContentItemEntity?,
    onCompleteReading: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    onSpeak: (String) -> Unit,
    onSaveElapsedProgress: (elapsedSeconds: Int, isCompleted: Boolean) -> Unit = { _, _ -> },
    onConceptEvaluated: (conceptKey: String, isUnderstood: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    // 10 minutes required = 600 seconds
    var secondsLeft by remember { mutableIntStateOf(600) }
    var isRunning by remember { mutableStateOf(true) }
    var hasCompletedTarget by remember { mutableStateOf(false) }
    var showExtensionDialog by remember { mutableStateOf(false) }
    var showQuiz by remember { mutableStateOf(false) }
    var quizAnswerRevealed by remember { mutableStateOf(false) }
    var conceptEvaluationDone by remember { mutableStateOf(false) }

    val handleBackPress = {
        val elapsed = (600 - secondsLeft).coerceAtLeast(0)
        onSaveElapsedProgress(elapsed, hasCompletedTarget)
        onNavigateBack()
    }

    BackHandler(onBack = handleBackPress)

    DisposableEffect(Unit) {
        onDispose {
            val elapsed = (600 - secondsLeft).coerceAtLeast(0)
            onSaveElapsedProgress(elapsed, hasCompletedTarget)
        }
    }

    LaunchedEffect(isRunning, secondsLeft) {
        if (isRunning && secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
            if (secondsLeft == 0) {
                hasCompletedTarget = true
                showExtensionDialog = true
                onCompleteReading(600L)
            }
        }
    }

    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)
    val progress = (600f - secondsLeft) / 600f

    val title = contentItem?.title ?: "كيف يعمل قلبك؟ مضخة الحياة العجيبة"
    val body = contentItem?.body ?: "القلب هو العضلة الأقوى والأوفى في جسم الإنسان. ينبض أكثر من 100 ألف مرة كل يوم بدون توقف، ليضخ الدم المحمل بالأكسجين والغذاء إلى كل خلية في الجسم. تخيلي يا أمي أن هذه العضلة الصغيرة التي بحجم قبضة اليد ترسل الدم عبر أوعية دموية طولها آلاف الكيلومترات! المشي الخفيف يومياً، وشرب الماء، والابتعاد عن التوتر هو أحسن هدية تقدمينها لقلبك ليظل ينبض بالصحة والنشاط."
    val takeaway = contentItem?.keyTakeaway ?: "المشي وشرب الماء والنوم الهادئ يحافظ على صحة عضلة القلب وضغط دم متوازن."
    val question = contentItem?.quizQuestion ?: "كم مرة ينبض القلب تقريباً في اليوم؟"
    val answer = contentItem?.quizAnswer ?: "أكثر من 100 ألف مرة كل يوم."
    val relatedConcept = contentItem?.relatedConceptKey ?: "heart"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "📖 جلسة القراءة الهادئة (10 دقائق)",
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
                                color = if (hasCompletedTarget) SageOlive else RafiqahRose,
                                strokeWidth = 4.dp,
                                trackColor = Color.LightGray.copy(alpha = 0.3f)
                            )
                            Icon(
                                imageVector = if (hasCompletedTarget) Icons.Default.CheckCircle else Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = if (hasCompletedTarget) SageOlive else RafiqahRose,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (hasCompletedTarget) "أتممتِ الـ10 دقائق بنجاح! 🌸" else "الوقت المتبقي للجلسة (مطلوب 10د)",
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

            // Extension / Completion Actions
            if (hasCompletedTarget) {
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
                            text = "🎉 يعطيك الصحة يا أمي! أتممتِ جلسة القراءة اليومية كاملة.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "تحبي تمددي 5 دقايق إضافية اختيارية، ولا نعملو سؤال خفيف يثبت المعلومة؟",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = {
                                    secondsLeft += 300
                                    hasCompletedTarget = false
                                    isRunning = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SageOlive)
                            ) {
                                Icon(Icons.Default.MoreTime, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ 5 دقايق إضافية")
                            }
                            Button(
                                onClick = { showQuiz = !showQuiz },
                                colors = ButtonDefaults.buttonColors(containerColor = RafiqahRose)
                            ) {
                                Icon(Icons.Default.QuestionAnswer, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
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
