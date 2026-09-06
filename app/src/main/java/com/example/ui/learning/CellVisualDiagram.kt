package com.example.ui.learning

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RafiqahRose
import com.example.ui.theme.SageOlive
import com.example.ui.theme.TerracottaAccent

enum class CellPart(
    val titleArabic: String,
    val scientificName: String,
    val tunisianAnalogy: String,
    val detailExplanation: String,
    val color: Color
) {
    MEMBRANE(
        "الغشاء الخلوي",
        "Membrane Cellulaire",
        "عسّاس وسور المدينة الذكي",
        "السور الخارجي اللي يحمي الخلية. ذكي برشا، يعرف شنوّة يدخل (كيما الماء والغذاء) وشنوّة يمنعو (كيما الأوساخ والسموم).",
        TerracottaAccent
    ),
    NUCLEUS(
        "النواة",
        "Noyau",
        "دار البلدية وقصر القيادة",
        "في وسط الخلية، فيها كتاب التعليمات والأسرار الوراثية (DNA). هي اللي توجه كل شيء وتطلب من باقي الأجزاء تخدم.",
        RafiqahRose
    ),
    MITOCHONDRIA(
        "الميتوكوندريا",
        "Mitochondrie",
        "محطة الكهرباء والكوجينة",
        "تاخذ السكر من الماكلة والأكسجين من النفس، وتحولهم لطاقة نظيفة وحيوية باش البدن يتحرك ويفكر ويدفى.",
        GoldAccent
    ),
    CYTOPLASM(
        "السيتوبلازم",
        "Cytoplasme",
        "المسبح الصافي وساحة المدينة",
        "سائل نقي ولطيف تعوم فيه كل أجزاء الخلية، وتتنقل فيه المواد الغذائية بالراحة واليسر.",
        SageOlive
    )
}

@Composable
fun CellVisualDiagram(
    onSpeakPart: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPart by remember { mutableStateOf(CellPart.NUCLEUS) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cell_visual_diagram"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔬 رسمة الخلية التفاعلية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = {
                        onSpeakPart("هذا رسم توضيحي مبسط للخلية الحية. الخلية فيها الغشاء والنواة والميتوكوندريا والسيتوبلازم.")
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "استمع لشرح الرسمة",
                        tint = RafiqahRose
                    )
                }
            }

            Text(
                text = "اضغطي على أي جزء باش تسمعي حكايتو بالتونسي 👆",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas drawing of the cell with interactive parts
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .background(Color(0xFFFCFAF7), shape = CircleShape)
                    .border(3.dp, TerracottaAccent, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(240.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Cytoplasm fill
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFEFF8F1), Color(0xFFE4F0E7)),
                            center = center,
                            radius = size.width / 2f
                        ),
                        radius = size.width / 2f - 4.dp.toPx(),
                        center = center
                    )

                    // Mitochondria left
                    drawOval(
                        color = GoldAccent.copy(alpha = 0.85f),
                        topLeft = Offset(center.x - 70.dp.toPx(), center.y - 30.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(34.dp.toPx(), 20.dp.toPx())
                    )

                    // Mitochondria right
                    drawOval(
                        color = GoldAccent.copy(alpha = 0.85f),
                        topLeft = Offset(center.x + 36.dp.toPx(), center.y + 25.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(36.dp.toPx(), 22.dp.toPx())
                    )

                    // Inner Nucleus
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFDE8EC), RafiqahRose),
                            center = center,
                            radius = 42.dp.toPx()
                        ),
                        radius = 40.dp.toPx(),
                        center = center
                    )

                    // DNA symbol center
                    drawCircle(
                        color = Color.White,
                        radius = 12.dp.toPx(),
                        center = center
                    )
                }

                // Interactive Overlays
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "النواة",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "DNA",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selection Buttons for Parts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CellPart.values().forEach { part ->
                    val isSelected = selectedPart == part
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (isSelected) part.color else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) part.color else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedPart = part
                                onSpeakPart("${part.titleArabic}: ${part.tunisianAnalogy}. ${part.detailExplanation}")
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = part.titleArabic,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Detail Card for Selected Part
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(selectedPart.color, shape = CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedPart.titleArabic,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${selectedPart.scientificName})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                onSpeakPart("${selectedPart.titleArabic}. تشبيهها بالتونسي: ${selectedPart.tunisianAnalogy}. ${selectedPart.detailExplanation}")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "استمع للشرح",
                                tint = selectedPart.color
                            )
                        }
                    }

                    Text(
                        text = "💡 التشبيه التونسي: ${selectedPart.tunisianAnalogy}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = selectedPart.color
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = selectedPart.detailExplanation,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
