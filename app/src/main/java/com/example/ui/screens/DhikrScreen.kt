package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CompanionRole
import com.example.data.DateKeys
import com.example.data.DhikrType
import com.example.ui.components.SpiritualCard
import com.example.ui.components.formatArabicNumber
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.SoftChampagneGold
import com.example.ui.theme.SubtleDustyRose
import com.example.viewmodel.SanctuaryDashboardData
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun DhikrScreen(
    activeRole: CompanionRole,
    data: SanctuaryDashboardData,
    onIncrementDhikr: (DhikrType, Int) -> Unit,
    onResetTodayDhikr: (DhikrType) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val todayKey = remember { DateKeys.todayKey() }
    val weekKey = remember { DateKeys.currentWeekKey() }
    val partnerRole = activeRole.partner()
    val adhkarList = DhikrType.entries

    // Start focused on Istighfar (index 1)
    val wheelListState = rememberLazyListState(initialFirstVisibleItemIndex = 1)
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = wheelListState)

    val selectedIndex by remember {
        derivedStateOf {
            val layoutInfo = wheelListState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo.minByOrNull { item ->
                abs((item.offset + item.size / 2) - viewportCenter)
            }?.index?.coerceIn(0, adhkarList.lastIndex) ?: 1
        }
    }

    var lastTickIndex by remember { mutableIntStateOf(1) }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex != lastTickIndex) {
            lastTickIndex = selectedIndex
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val selectedDhikr = adhkarList[selectedIndex]
    val myRecord = data.dhikrCountFor(activeRole, selectedDhikr)
    val partnerRecord = data.dhikrCountFor(partnerRole, selectedDhikr)

    val myDaily = myRecord?.effectiveDailyCount(todayKey) ?: 0
    val myWeekly = myRecord?.effectiveWeeklyCount(weekKey) ?: 0
    val myLifetime = myRecord?.lifetimeCount ?: 0L

    val partnerDaily = partnerRecord?.effectiveDailyCount(todayKey) ?: 0
    val partnerLifetime = partnerRecord?.lifetimeCount ?: 0L

    val partnerBannerText = if (partnerRole == CompanionRole.NOUR) {
        "${selectedDhikr.partnerSaidFemale} اليوم: ${formatArabicNumber(partnerDaily)}  |  الإجمالي: ${formatArabicNumber(partnerLifetime)}"
    } else {
        "${selectedDhikr.partnerSaidMale} اليوم: ${formatArabicNumber(partnerDaily)}  |  الإجمالي: ${formatArabicNumber(partnerLifetime)}"
    }

    val milestoneProgress = ((myDaily % 500) / 500f).let { if (myDaily > 0 && myDaily % 500 == 0) 1f else it }
    val animatedRing by animateFloatAsState(
        targetValue = milestoneProgress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "milestoneRing"
    )
    val nextMilestone = ((myDaily / 500) + 1) * 500

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Live Partner Counter Banner (Top of Screen)
        SpiritualCard(
            containerColor = SubtleDustyRose.copy(alpha = 0.13f),
            borderColor = SubtleDustyRose.copy(alpha = 0.4f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = SubtleDustyRose,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = partnerBannerText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("partner_dhikr_banner")
                )
            }
        }

        // 2. Interactive Rotary Wheel Picker (بكرة اختيار الأذكار الخمسة)
        SpiritualCard(
            borderColor = SoftChampagneGold.copy(alpha = 0.45f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "بكرة اختيار الذكر — اسحب للتبديل",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Gold highlight lens behind center wheel item
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                            .padding(horizontal = 4.dp)
                    )

                    LazyColumn(
                        state = wheelListState,
                        flingBehavior = snapBehavior,
                        contentPadding = PaddingValues(vertical = 49.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("dhikr_wheel_picker"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        itemsIndexed(adhkarList, key = { _, item -> item.key }) { index, dhikr ->
                            val isSelected = index == selectedIndex
                            val distance = abs(index - selectedIndex)
                            val itemScale = when (distance) {
                                0 -> 1.06f
                                1 -> 0.88f
                                else -> 0.76f
                            }
                            val itemAlpha = when (distance) {
                                0 -> 1f
                                1 -> 0.55f
                                else -> 0.25f
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .graphicsLayer {
                                        scaleX = itemScale
                                        scaleY = itemScale
                                        alpha = itemAlpha
                                    }
                                    .clickable {
                                        scope.launch {
                                            wheelListState.animateScrollToItem(index)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${dhikr.shortTitle}  •  ${dhikr.fullText}",
                                    fontFamily = if (isSelected) AmiriFontFamily else MaterialTheme.typography.titleMedium.fontFamily,
                                    fontSize = if (isSelected) 19.sp else 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = selectedDhikr.virtue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        // 3. Multi-Period Counter Tracking (Daily / Weekly / Lifetime)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DhikrPeriodMetricCard(
                label = "يومي",
                value = formatArabicNumber(myDaily),
                highlight = true,
                modifier = Modifier.weight(1f)
            )
            DhikrPeriodMetricCard(
                label = "أسبوعي",
                value = formatArabicNumber(myWeekly),
                highlight = false,
                modifier = Modifier.weight(1f)
            )
            DhikrPeriodMetricCard(
                label = "الإجمالي الكلي",
                value = formatArabicNumber(myLifetime),
                highlight = false,
                modifier = Modifier.weight(1f)
            )
        }

        // 4. Large Tactile Circular Tasbeeh Button with Progress Ring & Spring Ripple
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val buttonScale by animateFloatAsState(
            targetValue = if (isPressed) 0.93f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "tasbeehTapScale"
        )

        val ringTrackColor = MaterialTheme.colorScheme.surfaceVariant
        val ringGoldColor = SoftChampagneGold
        val primaryColor = MaterialTheme.colorScheme.primary

        Box(
            modifier = Modifier
                .size(238.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 10.dp.toPx()
                drawArc(
                    color = ringTrackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    color = ringGoldColor,
                    startAngle = -90f,
                    sweepAngle = animatedRing * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Box(
                modifier = Modifier
                    .size(192.dp)
                    .scale(buttonScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.92f),
                                primaryColor
                            )
                        )
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true, color = SoftChampagneGold),
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onIncrementDhikr(selectedDhikr, 1)
                        }
                    )
                    .testTag("tasbeeh_tap_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = formatArabicNumber(myDaily),
                        style = MaterialTheme.typography.displayLarge,
                        color = Color(0xFFFAF6F0),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedDhikr.fullText,
                        fontFamily = AmiriFontFamily,
                        fontSize = 15.sp,
                        color = SoftChampagneGold,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }

        // Milestone helper + quick batch / reset actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "المتبقي لإشعار الـ $nextMilestone: ${nextMilestone - myDaily} ذكر ✨",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onIncrementDhikr(selectedDhikr, 33)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("tasbeeh_plus_33_button")
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+33",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = { onResetTodayDhikr(selectedDhikr) },
                    modifier = Modifier.testTag("tasbeeh_reset_today_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "تصفير عدّاد اليوم",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DhikrPeriodMetricCard(
    label: String,
    value: String,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    SpiritualCard(
        modifier = modifier,
        containerColor = if (highlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        else MaterialTheme.colorScheme.surface,
        borderColor = if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.outlineVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
