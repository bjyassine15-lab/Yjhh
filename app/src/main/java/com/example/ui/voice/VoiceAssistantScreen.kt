package com.example.ui.voice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.LiveVoiceState
import com.example.domain.model.ConversationMessage
import com.example.domain.model.MessageSender
import com.example.ui.PendingActionConfirmation
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RafiqahRose
import com.example.ui.theme.SageOlive
import com.example.ui.theme.TerracottaAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantScreen(
    messages: List<ConversationMessage>,
    liveVoiceState: LiveVoiceState = LiveVoiceState.IDLE,
    liveTranscript: String = "",
    isLiveSessionActive: Boolean = false,
    pendingConfirmation: PendingActionConfirmation? = null,
    onConfirmAction: () -> Unit = {},
    onDismissAction: () -> Unit = {},
    onStartLiveSession: () -> Unit = {},
    onStopLiveSession: () -> Unit = {},
    onInterruptVoice: () -> Unit = {},
    onReconnectVoice: () -> Unit = {},
    onSendMessage: (String) -> Unit,
    onSpeak: (String) -> Unit,
    onStopSpeech: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isChatModeActive by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Gentle pulsating animation for voice interaction
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = when (liveVoiceState) {
            LiveVoiceState.LISTENING -> 1.18f
            LiveVoiceState.SPEAKING -> 1.28f
            LiveVoiceState.THINKING -> 1.08f
            else -> 1.05f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val micButtonColor by animateColorAsState(
        targetValue = when (liveVoiceState) {
            LiveVoiceState.LISTENING -> RafiqahRose
            LiveVoiceState.SPEAKING -> SageOlive
            LiveVoiceState.THINKING -> GoldAccent
            LiveVoiceState.DISCONNECTED -> TerracottaAccent
            LiveVoiceState.IDLE -> RafiqahRose
        },
        label = "micColor"
    )

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickQuestions = listOf(
        "شنوة الخلية؟ 🔬",
        "ما فهمتش 🌸",
        "أنا فهمت 🌷",
        "اليوم عندي موعد مع الطبيب 🩺",
        "نكملوا قصة سارة 📖",
        "تتذكر الكلمة الفرنسية اللي قريتها البارح؟ 🇫🇷"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "🎙️ رفيقة الحبيبة",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = liveVoiceState.arabicStatus,
                            style = MaterialTheme.typography.bodyMedium,
                            color = micButtonColor,
                            fontSize = 13.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                actions = {
                    // Toggle between Voice and Chat mode
                    IconButton(
                        onClick = { isChatModeActive = !isChatModeActive },
                        modifier = Modifier.testTag("toggle_chat_mode_button")
                    ) {
                        Icon(
                            imageVector = if (isChatModeActive) Icons.Default.Mic else Icons.Default.Keyboard,
                            contentDescription = if (isChatModeActive) "التبديل للصوت" else "التبديل للكتابة",
                            tint = RafiqahRose
                        )
                    }

                    if (liveVoiceState == LiveVoiceState.SPEAKING) {
                        IconButton(onClick = onInterruptVoice) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "إيقاف الصوت",
                                tint = RafiqahRose
                            )
                        }
                    } else if (liveVoiceState == LiveVoiceState.DISCONNECTED) {
                        IconButton(onClick = onReconnectVoice) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "إعادة الاتصال",
                                tint = TerracottaAccent
                            )
                        }
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
        ) {
            // Natural Confirmation Banner if sensitive action pending
            AnimatedVisibility(visible = pendingConfirmation != null) {
                pendingConfirmation?.let { conf ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("action_confirmation_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF5E7))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = conf.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = conf.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onConfirmAction,
                                    colors = ButtonDefaults.buttonColors(containerColor = SageOlive),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("نعم سجلي 🌸")
                                }
                                Button(
                                    onClick = onDismissAction,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("لا شكراً", color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }

            // Dialogue history
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                }

                items(messages) { msg ->
                    val isMother = msg.sender == MessageSender.USER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMother) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .testTag(if (isMother) "user_message_card" else "rafiqah_message_card"),
                            shape = RoundedCornerShape(
                                topStart = 20.dp,
                                topEnd = 20.dp,
                                bottomStart = if (isMother) 20.dp else 4.dp,
                                bottomEnd = if (isMother) 4.dp else 20.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMother) RafiqahRose else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isMother) "أمي الحبيبة" else "رفيقة 🌷",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMother) Color.White else RafiqahRose
                                    )
                                    if (!isMother) {
                                        IconButton(onClick = { onSpeak(msg.spokenDialectText) }) {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = "استماع",
                                                tint = RafiqahRose
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isMother) Color.White else MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 26.sp
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Quick Question Chips for easy tapping
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 10.dp, horizontal = 16.dp)
            ) {
                Text(
                    text = "مواضيع سريعة مع رفيقة:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickQuestions.take(3).forEach { q ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable {
                                    onSendMessage(q)
                                }
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = q,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Main Interactive Area: Either Voice Pulsing Orb or Text Chat
            if (isChatModeActive) {
                // Fallback Chat Mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("اكتبي رسالتك لرفيقة هنا يا أمي...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("voice_text_input"),
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                onSendMessage(textInput)
                                textInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .background(RafiqahRose, shape = CircleShape)
                            .testTag("send_chat_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "إرسال",
                            tint = Color.White
                        )
                    }
                }
            } else {
                // Voice-First Animated State
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pulsating Mic Action
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .scale(pulseScale)
                            .background(micButtonColor, shape = CircleShape)
                            .clickable {
                                if (liveVoiceState == LiveVoiceState.SPEAKING) {
                                    onInterruptVoice()
                                } else if (liveVoiceState == LiveVoiceState.DISCONNECTED) {
                                    onReconnectVoice()
                                } else {
                                    onSendMessage("سلام يا رفيقة، كيفاش أصبحت اليوم؟")
                                }
                            }
                            .testTag("voice_talk_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (liveVoiceState) {
                                LiveVoiceState.SPEAKING -> Icons.Default.GraphicEq
                                LiveVoiceState.DISCONNECTED -> Icons.Default.Refresh
                                else -> Icons.Default.Mic
                            },
                            contentDescription = "محادثة صوتية مع رفيقة",
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = when (liveVoiceState) {
                            LiveVoiceState.LISTENING -> "🎙️ رفيقة تسمع فيك... تفضلي احكي بالتونسي"
                            LiveVoiceState.THINKING -> "🧠 رفيقة تفكر باش تجاوبك بكل حب..."
                            LiveVoiceState.SPEAKING -> "🔊 رفيقة تجاوب فيك... اضغطي للإيقاف"
                            LiveVoiceState.DISCONNECTED -> "⚠️ انقطع الاتصال... اضغطي لإعادة المحاولة"
                            LiveVoiceState.IDLE -> "🎙️ اضغطي وتكلمي مع رفيقة طبيعياً"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = micButtonColor
                    )
                }
            }
        }
    }
}
