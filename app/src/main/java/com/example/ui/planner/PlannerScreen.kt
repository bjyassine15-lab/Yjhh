package com.example.ui.planner

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.domain.model.DailyTask
import com.example.domain.model.TaskCategory
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RafiqahRose
import com.example.ui.theme.SageOlive
import com.example.ui.theme.TerracottaAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    tasks: List<DailyTask>,
    onToggleTaskCompleted: (id: Long, completed: Boolean) -> Unit,
    onAddTask: (title: String, timeHint: String) -> Unit,
    onNavigateBack: () -> Unit,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskTime by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "🌷 نهاري المبروك",
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
                        val uncompleted = tasks.filter { !it.isCompleted }
                        val summary = if (uncompleted.isEmpty()) {
                            "ما شاء الله يا أمي، كملتي كل حاجات اليوم، ربي يعطيك الصحة."
                        } else {
                        "عندك اليوم ${uncompleted.size} حاجات هادين، أهمهم: ${uncompleted.first().title}."
                    }
                    onSpeak(summary)
                }) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "استماع لملخص اليوم",
                        tint = RafiqahRose
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )
    },
    floatingActionButton = {
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = RafiqahRose,
            contentColor = Color.White,
            modifier = Modifier.testTag("add_task_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة حاجة في النهج")
        }
    },
    modifier = modifier.fillMaxSize()
) { innerPadding ->
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Daily Calming Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(RafiqahRose, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "يوم هادئ ومريح يا أمي",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ما نكثروش المهام، 3 حاجات مهمة تكفي باش ترتاحي وتفرحي 🌸",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // List of tasks
        items(tasks) { task ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        onToggleTaskCompleted(task.id, !task.isCompleted)
                        if (!task.isCompleted) {
                            onSpeak("يعطيك الصحة يا أمي! كملتي: ${task.title}.")
                        }
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (task.isCompleted) Color(0xFFF1F7F3) else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.Circle,
                        contentDescription = if (task.isCompleted) "مكتمل" else "غير مكتمل",
                        tint = if (task.isCompleted) SageOlive else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.Bold,
                            color = if (task.isCompleted) Color(0xFF6B756E) else MaterialTheme.colorScheme.onSurface
                        )
                        if (task.timeHint.isNotEmpty()) {
                            Text(
                                text = "⏰ ${task.timeHint}" + if (task.note.isNotEmpty()) " • ${task.note}" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = {
                        onSpeak("${task.title}. ${task.timeHint}. ${task.note}")
                    }) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "استماع للمهمة",
                            tint = RafiqahRose
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "إضافة موعد أو حاجة في النهج 🌸",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        label = { Text("شنوّة تحبي تزيدي؟ (مثال: شرب التاي)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTaskTime,
                        onValueChange = { newTaskTime = it },
                        label = { Text("الوقت (مثال: 10:00 صباحًا)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTaskTitle.isNotBlank()) {
                            onAddTask(newTaskTitle, newTaskTime)
                            newTaskTitle = ""
                            newTaskTime = ""
                            showAddDialog = false
                            onSpeak("تمت إضافة الحاجة الجديدة لنهارك يا أمي.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RafiqahRose)
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
}
