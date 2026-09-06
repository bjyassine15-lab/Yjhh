package com.example.ui.health

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
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
import com.example.domain.model.HealthProfile
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RafiqahRose
import com.example.ui.theme.SageOlive
import com.example.ui.theme.SageOliveContainer
import com.example.ui.theme.TerracottaAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(
    healthProfile: HealthProfile,
    onNavigateBack: () -> Unit,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var waterGlassesDrank by remember { mutableStateOf(4) }
    var medicineTakenToday by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "💚 صحتي وراحتي",
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
                    IconButton(onClick = {
                        onSpeak("ربي يعطيك صحة البدن يا أمي الغالية. دواء الضغط مأخوذ اليوم، وأهم حاجة شرب الماء وراحة البال.")
                    }) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "استماع للنصيحة",
                            tint = SageOlive
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
            // Calm Header Greeting
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("health_welcome_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F2EC))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(SageOlive, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "صحتي أمانة ونعمة من ربي 🌿",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF143B22)
                            )
                            Text(
                                text = "هنا نعتنيو ببدنك بهدوء، بلا قلق وبلا خوف من الأمراض.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF2E5E3E)
                            )
                        }
                    }
                }
            }

            // Blood Pressure & Medication Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalPharmacy,
                                    contentDescription = null,
                                    tint = RafiqahRose
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "دواء الصباح وضغط الدم",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = {
                                onSpeak("دواء الضغط حبة واحدة صباحا بعد فطور الصباح مع كأس ماء كبير.")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "استماع",
                                    tint = RafiqahRose
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (medicineTakenToday) Color(0xFFE5F4E9) else Color(0xFFFDECEE))
                                .clickable {
                                    medicineTakenToday = !medicineTakenToday
                                    if (medicineTakenToday) {
                                        onSpeak("صحة وفرحة أمي! سجلت أنك أخذتِ دواء الصباح.")
                                    }
                                }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "حبة الضغط (صباحًا بعد الفطور)",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (medicineTakenToday) "✅ تم أخذ الحبة اليوم" else "⏳ اضغطي هنا لتأكيد أخذ الدواء",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (medicineTakenToday) SageOlive else TerracottaAccent
                                    )
                                }
                                if (medicineTakenToday) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "مأخوذ",
                                        tint = SageOlive,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Water & Sleep Tracker
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Water card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (waterGlassesDrank < 8) waterGlassesDrank++
                                onSpeak("صحة وبالشفاء! شربتي $waterGlassesDrank كيسان ماء اليوم.")
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF5FA))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    tint = Color(0xFF1E6B9E)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "شرب الماء",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF13486B)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$waterGlassesDrank من 6 كيسان",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF13486B)
                            )
                            Text(
                                text = "+ اضغطي عند كل كأس 💧",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF1E6B9E)
                            )
                        }
                    }

                    // Sleep quality card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F1FA))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Bedtime,
                                    contentDescription = null,
                                    tint = Color(0xFF6B3E9E)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "نوم البارح",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF43206B)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "مريح وهادئ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF43206B)
                            )
                            Text(
                                text = "نصيحة: لويزة دافية ليلاً ☕",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6B3E9E)
                            )
                        }
                    }
                }
            }

            // Tunisian Kitchen & Nutrition Tip
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF7EC))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Restaurant,
                                    contentDescription = null,
                                    tint = GoldAccent
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "نصيحة الكوجينة التونسية 🫒",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6B450B)
                                )
                            }
                            IconButton(onClick = {
                                onSpeak("نصيحة اليوم: قطيرة زيت زيتونة بكر مع صحين سلاطة خضرا، وتخفيف الملح في الماكلة، يخلي شرايين القلب مرتاحة والتونسيو ديما مريقلة.")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "استماع للنصيحة",
                                    tint = GoldAccent
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "قطيرة زيت زيتونة بكر مع صحين سلاطة خضرا طازجة، وتخفيف الملح في المرقة، يعاون شرايين القلب تقعد ديما مرتاحة ونظيفة 🌷",
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 26.sp,
                            color = Color(0xFF5E3906)
                        )
                    }
                }
            }

            // Doctor Appointment reminder
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(SageOliveContainer, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = SageOlive
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "الموعد الطبي القادم (Rendez-vous)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "مراقبة دورية عادية مع طبيب العائلة الشهر القادم.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Important Safety Advice (Red Flags)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFB3261E)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تنبيه صحي مهم وأمان:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB3261E)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "في حال حسيتي بألم مفاجئ قوي في الصدر، أو ضيق تنفس حاد، أو دوخة شديدة مع تنميل، يجب الاتصال بالطبيب فوراً أو الإسعاف (190) دون أي تردد.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF7A1B16),
                            lineHeight = 24.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
