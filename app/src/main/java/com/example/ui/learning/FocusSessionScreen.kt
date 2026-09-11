package com.example.ui.learning

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RafiqahRose
import com.example.ui.theme.SageOlive
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusSessionScreen(
    onNavigateBack: () -> Unit,
    onSpeak: (String) -> Unit,
    onSessionFinished: (elapsedSeconds: Int) -> Unit,
    onSessionInterrupted: (elapsedSeconds: Int) -> Unit = {},
    targetDurationMinutes: Int = 15,
    activityTitle: String = "جلسة تركيز وقراءة هادئة",
    blockingStatus: com.example.service.focus.BlockingStatus? = null,
    onRequestUsagePermission: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val totalSeconds = (targetDurationMinutes.coerceAtLeast(1) * 60).toFloat()
    var secondsLeft by remember(targetDurationMinutes) { mutableIntStateOf(totalSeconds.toInt()) }
    var isRunning by remember { mutableStateOf(true) }
    var currentStep by remember { mutableIntStateOf(1) } // 1: Story, 2: Visual, 3: Recap

    val elapsedSeconds = (totalSeconds.toInt() - secondsLeft).coerceAtLeast(0)

    LaunchedEffect(isRunning, secondsLeft) {
        if (isRunning && secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
            if (secondsLeft == 0) {
                onSessionFinished(totalSeconds.toInt())
            }
        }
    }

    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)
    val progress = (totalSeconds - secondsLeft) / totalSeconds

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "🌸 وقت التركيز مع رفيقة",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (secondsLeft > 0) {
                            onSessionInterrupted(elapsedSeconds)
                        }
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Calm Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$targetDurationMinutes دقيقة هادية ومفيدة ليك يا أمي 🌷",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = RafiqahRose
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = activityTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "خالينا نركزوا على فكرة واحدة بكل هدوء وراحة بال.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Real App Blocking Policy Status Card (Honest UI: ACTIVE vs TIMER_ONLY)
            if (blockingStatus != null) {
                val isTimerOnly = blockingStatus.focusBlockingMode == com.example.service.focus.FocusBlockingMode.TIMER_ONLY
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTimerOnly) MaterialTheme.colorScheme.surfaceVariant else SageOlive.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "حماية التركيز",
                                tint = if (isTimerOnly) MaterialTheme.colorScheme.onSurfaceVariant else SageOlive,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isTimerOnly) "وضع المؤقت الهادئ (TIMER_ONLY) ⏱️" else "حماية التركيز نشطة (ACTIVE) 🛡️",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = blockingStatus.explanationNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (blockingStatus.state == com.example.service.focus.BlockingState.NOT_CONFIGURED && onRequestUsagePermission != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onRequestUsagePermission,
                                colors = ButtonDefaults.buttonColors(containerColor = RafiqahRose),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "تفعيل صلاحية الاستخدام من الإعدادات ⚙️",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Circular Timer
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .testTag("focus_timer"),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = RafiqahRose,
                    trackColor = Color(0xFFFCE9ED),
                    strokeWidth = 10.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isRunning) "الجلسة جارية..." else "متوقفة مؤقتاً",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Steps Progress Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (currentStep) {
                                1 -> "الخطوة 1: استماع وتأمل هادئ 📖"
                                2 -> "الخطوة 2: استيعاب الفكرة الطيبة 🔬"
                                else -> "الخطوة 3: تثبيت المعنى في البال 🌷"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = {
                            val textToSay = when (currentStep) {
                                1 -> "استمعي بهدوء لصوت رفيقة، كل دقيقة تركيز تعطي صحة وعافية لذهنك."
                                2 -> "المعلومة الهادية تدخل للبال وتستقر بلا تعب ولا ضغط."
                                else -> "يعطيك الصحة يا أمي، كملنا جلسة اليوم وكل معلومة ربي يبارك فيها."
                            }
                            onSpeak(textToSay)
                        }) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "استماع",
                                tint = RafiqahRose
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = when (currentStep) {
                            1 -> "استمعي بهدوء لصوت رفيقة يرويلك كيفاش المعرفة تبني حكمة وصحة."
                            2 -> "شوفي كيفاش ربي خلق التوازن في كل شيء في جسمنا وحياتنا."
                            else -> "ما شاء الله، هاك كسبتي دقائق قيمة في وقت ممتع وبلا تعب!"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (currentStep < 3) {
                            Button(
                                onClick = { currentStep++ },
                                colors = ButtonDefaults.buttonColors(containerColor = RafiqahRose),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("الخطوة التالية 👈", style = MaterialTheme.typography.bodyLarge)
                            }
                        } else {
                            Button(
                                onClick = {
                                    onSessionFinished(totalSeconds.toInt())
                                    onNavigateBack()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SageOlive),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إتمام الجلسة بنجاح 🌷", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }

            // Pause / Play / Stop controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { isRunning = !isRunning },
                    modifier = Modifier
                        .size(56.dp)
                        .background(RafiqahRose, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "إيقاف مؤقت" else "متابعة",
                        tint = Color.White
                    )
                }

                Button(
                    onClick = {
                        if (secondsLeft > 0) {
                            onSessionInterrupted(elapsedSeconds)
                        } else {
                            onSessionFinished(totalSeconds.toInt())
                        }
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "إنهاء الجلسة والعودة",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
