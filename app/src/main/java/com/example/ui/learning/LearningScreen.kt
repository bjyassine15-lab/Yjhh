package com.example.ui.learning

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RafiqahRose
import com.example.ui.theme.SageOlive
import com.example.ui.theme.TerracottaAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningScreen(
    onNavigateBack: () -> Unit,
    onStartFocusSession: () -> Unit,
    onSpeak: (String) -> Unit,
    onSaveProgress: (concept: String, isCorrect: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentLevel by remember { mutableIntStateOf(1) }
    var selectedQuizOption by remember { mutableIntStateOf(-1) }
    var quizAnswered by remember { mutableStateOf(false) }

    val levelTitles = listOf(
        "1. تشبيه بسيط",
        "2. من الواقع",
        "3. الفكرة العلمية",
        "4. التفاصيل",
        "5. التطبيق الطبي"
    )

    val explanations = listOf(
        "تخيلي يا أمي أن بدنك كيما الدار الكبيرة. والدار تتبنى بالياجورة بالياجورة. أصغر ياجورة حية في جسمنا هي 'الخلية'. ما تنجميش تشوفيها بعينك المجردة، أما هي اللي صانعة كل شيء.",
        "في الكوجينة، كي تقصي بصلة ولا تغسلي نعناع، هذوكم كلهم خلايا نباتية. وبدنك إنتِ مبني من خلايا حيوانية تتنفس وتطلب ماكلة وتشرب ماء كل ثانية.",
        "الخلية (Cellule) هي الحبة الأساسية للحياة. كل خلية فيها غلاف يحميها اسمو الغشاء، وقلب يديرها اسمو النواة، ومحطة طاقة تصنع القوة اسمها الميتوكوندريا.",
        "داخل النواة فما كتاب صغير مكتوب فيه كل أسرارك، لون عينيك، وفصيلة دمك واسمو DNA. والميتوكوندريا تاخذ السكر من الماكلة وتحولو لطاقة وحرارة.",
        "وقت اللي نمرضو، راهو الخلية هي اللي تكون محتاجة مساعدة. الدواء اللي ناكلوه يمشي عبر الدم ويدخل للخلية باش يصلح الخلل ويعاونها تخدم مرتاحة."
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "🧠 نتعلموا الطب ببساطة",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
                    IconButton(
                        onClick = {
                            onSpeak(explanations[currentLevel - 1])
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "استماع للشرح",
                            tint = RafiqahRose
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Focus session callout
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStartFocusSession() },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF4E8))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(GoldAccent, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "جلسة تركيز (7 دقايق)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5E3906)
                            )
                            Text(
                                text = "وقت قصير ومريح، نركزوا فيه مع قصة سارة بلا تشويش 🌷",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF7E551A)
                            )
                        }
                    }
                }
            }

            // Progressive Depth Selector
            item {
                Text(
                    text = "درجات الفهم المتدرج (Progressive Depth):",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    levelTitles.forEachIndexed { index, title ->
                        val levelNum = index + 1
                        val isSelected = currentLevel == levelNum
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) RafiqahRose else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    currentLevel = levelNum
                                    onSpeak(explanations[index])
                                }
                                .padding(vertical = 10.dp, horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "مستوى $levelNum",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Current Level Content Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = levelTitles[currentLevel - 1],
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = RafiqahRose
                            )
                            IconButton(onClick = { onSpeak(explanations[currentLevel - 1]) }) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "استماع",
                                    tint = RafiqahRose
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = explanations[currentLevel - 1],
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 28.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Educational Visual Diagram
            item {
                CellVisualDiagram(onSpeakPart = onSpeak)
            }

            // Understanding Check / Quiz
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quiz_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFBF4EE))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = TerracottaAccent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "سؤال الفهم السريع 🌸",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TerracottaAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "في رأيك يا أمي، الخلية هي حاجة واحدة كبيرة ومصمتة، ولا فيها أجزاء منظمة تخدم مع بعضها؟",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        val options = listOf(
                            "حاجة واحدة مصمتة وما فيها حتى جزء داخلي",
                            "مدينة صغيرة فيها أجزاء منظمة تخدم مع بعضها"
                        )

                        options.forEachIndexed { index, optionText ->
                            val isSelected = selectedQuizOption == index
                            val isCorrect = index == 1

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        when {
                                            quizAnswered && isSelected && isCorrect -> Color(0xFFE4F3E8)
                                            quizAnswered && isSelected && !isCorrect -> Color(0xFFFDECEE)
                                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                                            else -> MaterialTheme.colorScheme.surface
                                        }
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = when {
                                            quizAnswered && isCorrect -> SageOlive
                                            quizAnswered && isSelected && !isCorrect -> RafiqahRose
                                            isSelected -> RafiqahRose
                                            else -> MaterialTheme.colorScheme.outline
                                        },
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        if (!quizAnswered) {
                                            selectedQuizOption = index
                                            quizAnswered = true
                                            val answeredCorrectly = index == 1
                                            onSaveProgress("مفهوم الخلية", answeredCorrectly)
                                            if (answeredCorrectly) {
                                                onSpeak("يعطيك الصحة يا أمي! جوابك صحيح مائة بالمائة. الخلية فعلاً فيها أجزاء منظمة كيما المعمل الصغير.")
                                            } else {
                                                onSpeak("قريبة برشا يا أمي، أما تذكري إن الخلية مدينة صغيرة فيها أجزاء تتعاون مع بعضها.")
                                            }
                                        }
                                    }
                                    .padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = optionText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (quizAnswered && isCorrect) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "صحيح",
                                            tint = SageOlive
                                        )
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(visible = quizAnswered) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                if (selectedQuizOption == 1) {
                                    Text(
                                        text = "🌷 ممتاز يا أمي! ما شاء الله عليك، فهمتي سر الخلية الأول.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = SageOlive
                                    )
                                } else {
                                    Text(
                                        text = "🌸 خطوة باهية يا أمي! ديما نتفكرو إن الخلية كيما الدار، فيها كوجينة وصالة وباب محروس.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = TerracottaAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
