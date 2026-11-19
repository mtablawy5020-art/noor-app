package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SoftChampagneGold
import com.example.ui.theme.SubtleDustyRose
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun formatArabicNumber(number: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return formatter.format(number)
}

fun formatArabicNumber(number: Int): String = formatArabicNumber(number.toLong())

/**
 * Subtle Islamic 8-pointed star geometric watermark drawn behind screens using Compose Canvas.
 */
@Composable
fun IslamicPatternBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val bgColor = MaterialTheme.colorScheme.background
    val patternColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.07f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .drawBehind {
                val cellSize = 92.dp.toPx()
                val radius = cellSize * 0.28f
                val cols = (size.width / cellSize).toInt() + 2
                val rows = (size.height / cellSize).toInt() + 2
                val stroke = Stroke(width = 1.dp.toPx())

                for (row in 0..rows) {
                    for (col in 0..cols) {
                        val cx = col * cellSize + if (row % 2 == 1) cellSize / 2f else 0f
                        val cy = row * cellSize
                        val path = Path()
                        val points = 16
                        for (i in 0 until points) {
                            val angle = (i * 2.0 * PI / points).toFloat()
                            val r = if (i % 2 == 0) radius else radius * 0.56f
                            val px = cx + r * cos(angle)
                            val py = cy + r * sin(angle)
                            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        path.close()
                        drawPath(path = path, color = patternColor, style = stroke)
                        drawCircle(
                            color = patternColor.copy(alpha = 0.04f),
                            radius = radius * 0.25f,
                            center = Offset(cx, cy),
                            style = stroke
                        )
                    }
                }
            }
    ) {
        content()
    }
}

@Composable
fun SpiritualCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        content()
    }
}

/**
 * Dual-segment animated progress bar displaying both Mahmoud's and Nour's contributions toward one shared goal.
 */
@Composable
fun DualPartnerProgressBar(
    mahmoudCount: Long,
    nourCount: Long,
    targetCount: Long,
    modifier: Modifier = Modifier
) {
    val safeTarget = targetCount.coerceAtLeast(1L).toFloat()
    val rawMahmoud = (mahmoudCount / safeTarget).coerceIn(0f, 1f)
    val rawNour = (nourCount / safeTarget).coerceIn(0f, (1f - rawMahmoud).coerceAtLeast(0f))

    val animMahmoud by animateFloatAsState(
        targetValue = rawMahmoud,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "mahmoudProgress"
    )
    val animNour by animateFloatAsState(
        targetValue = rawNour,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "nourProgress"
    )

    val total = mahmoudCount + nourCount
    val percent = ((total * 100.0) / safeTarget).toInt().coerceIn(0, 100)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${formatArabicNumber(total)} / ${formatArabicNumber(targetCount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (animMahmoud > 0.002f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(animMahmoud)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                if (animNour > 0.002f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(animNour)
                            .background(SubtleDustyRose)
                    )
                }
                val remaining = (1f - animMahmoud - animNour).coerceAtLeast(0f)
                if (remaining > 0.002f) {
                    Spacer(modifier = Modifier.weight(remaining))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PartnerLegendChip(
                color = MaterialTheme.colorScheme.primary,
                label = "محمود: ${formatArabicNumber(mahmoudCount)}"
            )
            PartnerLegendChip(
                color = SubtleDustyRose,
                label = "نور: ${formatArabicNumber(nourCount)}"
            )
        }
    }
}

@Composable
private fun PartnerLegendChip(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SoftChampagneGold)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 14.dp)
            )
        }
    }
}
