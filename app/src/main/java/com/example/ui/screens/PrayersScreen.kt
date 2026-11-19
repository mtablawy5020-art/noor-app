package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CompanionRole
import com.example.data.DateKeys
import com.example.data.PrayerType
import com.example.ui.components.SectionHeader
import com.example.ui.components.SpiritualCard
import com.example.ui.theme.SoftChampagneGold
import com.example.ui.theme.SubtleDustyRose
import com.example.viewmodel.SanctuaryDashboardData

@Composable
fun PrayersScreen(
    activeRole: CompanionRole,
    data: SanctuaryDashboardData,
    onTogglePrayer: (PrayerType) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val todayKey = remember { DateKeys.todayKey() }
    val todayPrayer = data.todayPrayer(todayKey)
    val partnerRole = activeRole.partner()

    val myCompleted = todayPrayer?.completedCount(activeRole) ?: 0
    val partnerCompleted = todayPrayer?.completedCount(partnerRole) ?: 0
    val mahmoudStreak = data.calculatePrayerStreak(CompanionRole.MAHMOUD)
    val nourStreak = data.calculatePrayerStreak(CompanionRole.NOUR)
    val last7Days = remember { DateKeys.lastNDaysKeys(7) }
    val historyByDate = remember(data.prayersHistory) { data.prayersHistory.associateBy { it.dateKey } }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "الصلوات الخمس اليوم",
                subtitle = "تتجدد تلقائياً كل منتصف ليل مع حفظ سجل الالتزام الأسبوعي"
            )
        }

        // Daily summary & streak header card
        item {
            SpiritualCard(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                borderColor = SoftChampagneGold.copy(alpha = 0.45f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "صلواتك اليوم",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$myCompleted / 5",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(38.dp)
                            .background(MaterialTheme.colorScheme.outline)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "صلوات ${partnerRole.arabicName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$partnerCompleted / 5",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = SubtleDustyRose
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(38.dp)
                            .background(MaterialTheme.colorScheme.outline)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = "سلسلة الالتزام",
                                tint = SoftChampagneGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "سلسلة 5/5",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${if (activeRole == CompanionRole.MAHMOUD) mahmoudStreak else nourStreak} يوم",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        // Five daily prayers list
        items(PrayerType.entries, key = { it.key }) { prayer ->
            val myDone = todayPrayer?.isCompleted(activeRole, prayer) ?: false
            val partnerDone = todayPrayer?.isCompleted(partnerRole, prayer) ?: false

            val cardBg by animateColorAsState(
                targetValue = if (myDone) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                label = "prayerCardBg"
            )

            SpiritualCard(
                containerColor = cardBg,
                borderColor = if (myDone) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.outlineVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTogglePrayer(prayer)
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("prayer_card_${prayer.key}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tactile Check Circle (min 48dp touch area via parent + 44dp visual circle)
                        Surface(
                            shape = CircleShape,
                            color = if (myDone) MaterialTheme.colorScheme.primary else Color.Transparent,
                            border = BorderStroke(
                                width = 2.dp,
                                color = if (myDone) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (myDone) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "تم أداء صلاة ${prayer.arabicName}",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "صلاة ${prayer.arabicName}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = prayer.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Live Partner Status Indicator
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (partnerDone) {
                            SubtleDustyRose.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = BorderStroke(
                            1.dp,
                            if (partnerDone) SubtleDustyRose.copy(alpha = 0.45f) else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (partnerDone) Icons.Filled.CheckCircle else Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = if (partnerDone) SubtleDustyRose else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (partnerDone) {
                                    if (partnerRole == CompanionRole.MAHMOUD) "محمود صلى 🤍" else "نور صلت 🤍"
                                } else {
                                    "${partnerRole.arabicName} لم يسجل"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (partnerDone) SubtleDustyRose else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Weekly Streak History Strip for both Mahmoud and Nour
        item {
            Spacer(modifier = Modifier.height(4.dp))
            SectionHeader(
                title = "سجل الالتزام الأسبوعي",
                subtitle = "متابعة آخر 7 أيام لمحمود ونور"
            )
            Spacer(modifier = Modifier.height(10.dp))

            SpiritualCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        last7Days.forEach { dateKey ->
                            val dayRecord = historyByDate[dateKey]
                            val mCount = dayRecord?.completedCount(CompanionRole.MAHMOUD) ?: 0
                            val nCount = dayRecord?.completedCount(CompanionRole.NOUR) ?: 0
                            val isToday = dateKey == todayKey

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = DateKeys.arabicShortDay(dateKey),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isToday) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                DayPrayerDot(count = mCount, color = MaterialTheme.colorScheme.primary)
                                DayPrayerDot(count = nCount, color = SubtleDustyRose)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "محمود • سلسلة: $mahmoudStreak يوم",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "نور • سلسلة: $nourStreak يوم",
                            style = MaterialTheme.typography.bodySmall,
                            color = SubtleDustyRose,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayPrayerDot(count: Int, color: Color) {
    val alpha = when {
        count >= 5 -> 1f
        count >= 3 -> 0.55f
        count >= 1 -> 0.28f
        else -> 0.08f
    }
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (count >= 3) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
