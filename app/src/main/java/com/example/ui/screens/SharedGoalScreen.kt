package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdhkarChecklistItem
import com.example.data.CompanionRole
import com.example.data.DateKeys
import com.example.data.DhikrType
import com.example.data.SharedGoal
import com.example.data.SpiritualContent
import com.example.data.SpiritualNudge
import com.example.ui.components.DualPartnerProgressBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.SpiritualCard
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.SoftChampagneGold
import com.example.ui.theme.SubtleDustyRose

@Composable
fun SharedGoalScreen(
    activeRole: CompanionRole,
    sharedGoal: SharedGoal?,
    onSaveGoal: (title: String, dhikrKey: String, period: String, target: Long, reset: Boolean) -> Unit,
    onSendNudge: (SpiritualNudge) -> Unit,
    onToggleAdhkarItem: (isMorning: Boolean, itemId: String) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val todayKey = remember { DateKeys.todayKey() }
    val sameDay = sharedGoal?.adhkarDateKey == todayKey
    val partnerRole = activeRole.partner()

    val myMorningDone = if (!sameDay) emptyList() else {
        if (activeRole == CompanionRole.MAHMOUD) sharedGoal?.mahmoudMorningAdhkar.orEmpty()
        else sharedGoal?.nourMorningAdhkar.orEmpty()
    }
    val partnerMorningDone = if (!sameDay) emptyList() else {
        if (partnerRole == CompanionRole.MAHMOUD) sharedGoal?.mahmoudMorningAdhkar.orEmpty()
        else sharedGoal?.nourMorningAdhkar.orEmpty()
    }
    val myEveningDone = if (!sameDay) emptyList() else {
        if (activeRole == CompanionRole.MAHMOUD) sharedGoal?.mahmoudEveningAdhkar.orEmpty()
        else sharedGoal?.nourEveningAdhkar.orEmpty()
    }
    val partnerEveningDone = if (!sameDay) emptyList() else {
        if (partnerRole == CompanionRole.MAHMOUD) sharedGoal?.mahmoudEveningAdhkar.orEmpty()
        else sharedGoal?.nourEveningAdhkar.orEmpty()
    }

    var showEditGoalDialog by remember { mutableStateOf(false) }
    var morningExpanded by remember { mutableStateOf(false) }
    var eveningExpanded by remember { mutableStateOf(false) }

    val linkedDhikrLabel = remember(sharedGoal?.dhikrKey) {
        when (val key = sharedGoal?.dhikrKey) {
            "all" -> "جميع الأذكار"
            else -> DhikrType.fromKey(key).shortTitle
        }
    }
    val periodLabel = if (sharedGoal?.period == "weekly") "هدف أسبوعي" else "هدف شهري"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Shared Goal — "طريق الحلال"
        item {
            SectionHeader(
                title = "هدفنا المشترك (طريق الحلال)",
                subtitle = "كل تسبيحة تذكرها تُضاف تلقائياً إلى شريط التقدم الموحّد بينكما"
            )
            Spacer(modifier = Modifier.height(10.dp))

            SpiritualCard(
                borderColor = SoftChampagneGold.copy(alpha = 0.6f)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Text(
                                        text = periodLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Text(
                                        text = linkedDhikrLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = sharedGoal?.title ?: "10,000 استغفار للرزق والزواج في الحلال",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { showEditGoalDialog = true },
                            modifier = Modifier.testTag("edit_shared_goal_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "تعديل الهدف المشترك",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    DualPartnerProgressBar(
                        mahmoudCount = sharedGoal?.mahmoudProgress ?: 0L,
                        nourCount = sharedGoal?.nourProgress ?: 0L,
                        targetCount = sharedGoal?.targetCount ?: 10000L
                    )
                }
            }
        }

        // 2. Quick Modest Nudges (تذكير بالخير)
        item {
            SectionHeader(
                title = "تذكير بالخير",
                subtitle = "لمسة واحدة ترسل إشعاراً لطيفاً ومهذباً إلى ${partnerRole.arabicName}"
            )
            Spacer(modifier = Modifier.height(10.dp))

            val nudges = SpiritualNudge.entries
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                nudges.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { nudge ->
                            Surface(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSendNudge(nudge)
                                },
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                tonalElevation = 1.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("nudge_button_${nudge.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.NotificationsActive,
                                        contentDescription = null,
                                        tint = SoftChampagneGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = nudge.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = nudge.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Collapsible Morning & Evening Adhkar Checklists
        item {
            SectionHeader(
                title = "أذكار الصباح والمساء",
                subtitle = "حصن يومي يتجدد كل يوم مع متابعة شريكك"
            )
            Spacer(modifier = Modifier.height(10.dp))

            AdhkarCollapsibleSection(
                title = "أذكار الصباح",
                icon = Icons.Filled.WbSunny,
                iconTint = SoftChampagneGold,
                items = SpiritualContent.morningAdhkar,
                myCompletedIds = myMorningDone,
                partnerCompletedCount = partnerMorningDone.size,
                partnerName = partnerRole.arabicName,
                expanded = morningExpanded,
                onToggleExpanded = { morningExpanded = !morningExpanded },
                onToggleItem = { itemId ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleAdhkarItem(true, itemId)
                },
                testTagPrefix = "morning_adhkar"
            )

            Spacer(modifier = Modifier.height(12.dp))

            AdhkarCollapsibleSection(
                title = "أذكار المساء",
                icon = Icons.Filled.NightsStay,
                iconTint = SubtleDustyRose,
                items = SpiritualContent.eveningAdhkar,
                myCompletedIds = myEveningDone,
                partnerCompletedCount = partnerEveningDone.size,
                partnerName = partnerRole.arabicName,
                expanded = eveningExpanded,
                onToggleExpanded = { eveningExpanded = !eveningExpanded },
                onToggleItem = { itemId ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleAdhkarItem(false, itemId)
                },
                testTagPrefix = "evening_adhkar"
            )
        }
    }

    if (showEditGoalDialog) {
        EditSharedGoalDialog(
            initialGoal = sharedGoal,
            onSave = { title, dhikrKey, period, target, reset ->
                onSaveGoal(title, dhikrKey, period, target, reset)
                showEditGoalDialog = false
            },
            onDismiss = { showEditGoalDialog = false }
        )
    }
}

@Composable
private fun AdhkarCollapsibleSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    items: List<AdhkarChecklistItem>,
    myCompletedIds: List<String>,
    partnerCompletedCount: Int,
    partnerName: String,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onToggleItem: (String) -> Unit,
    testTagPrefix: String
) {
    SpiritualCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(16.dp)
                    .testTag("${testTagPrefix}_header"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "أتممت ${myCompletedIds.size}/${items.size}  •  $partnerName $partnerCompletedCount/${items.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "طي القائمة" else "فتح القائمة",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                    items.forEach { item ->
                        val checked = item.id in myCompletedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleItem(item.id) }
                                .padding(vertical = 10.dp)
                                .testTag("${testTagPrefix}_item_${item.id}"),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = if (checked) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (checked) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = item.countLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SoftChampagneGold
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = item.text,
                                    fontFamily = AmiriFontFamily,
                                    fontSize = 16.sp,
                                    lineHeight = 26.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun EditSharedGoalDialog(
    initialGoal: SharedGoal?,
    onSave: (title: String, dhikrKey: String, period: String, target: Long, reset: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialGoal?.title ?: "10,000 استغفار للرزق والزواج في الحلال") }
    var targetText by remember { mutableStateOf((initialGoal?.targetCount ?: 10000L).toString()) }
    var selectedDhikrKey by remember { mutableStateOf(initialGoal?.dhikrKey ?: DhikrType.ISTIGHFAR.key) }
    var selectedPeriod by remember { mutableStateOf(initialGoal?.period ?: "monthly") }
    var resetProgress by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تخصيص هدفنا المشترك",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 120) title = it },
                    label = { Text("عنوان الهدف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { input -> targetText = input.filter { it.isDigit() }.take(7) },
                    label = { Text("العدد المستهدف (مثال: 10000)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "المدة:",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedPeriod == "weekly",
                        onClick = { selectedPeriod = "weekly" },
                        label = { Text("أسبوعي") }
                    )
                    FilterChip(
                        selected = selectedPeriod == "monthly",
                        onClick = { selectedPeriod = "monthly" },
                        label = { Text("شهري") }
                    )
                }

                Text(
                    text = "نوع الذكر المحتسب:",
                    style = MaterialTheme.typography.labelLarge
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = selectedDhikrKey == "all",
                            onClick = { selectedDhikrKey = "all" },
                            label = { Text("كل الأذكار") }
                        )
                    }
                    items(DhikrType.entries, key = { it.key }) { dhikr ->
                        FilterChip(
                            selected = selectedDhikrKey == dhikr.key,
                            onClick = { selectedDhikrKey = dhikr.key },
                            label = { Text(dhikr.shortTitle) }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { resetProgress = !resetProgress }
                ) {
                    Checkbox(checked = resetProgress, onCheckedChange = { resetProgress = it })
                    Text(
                        text = "بدء التقدم من الصفر لهدف جديد",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = (targetText.toLongOrNull() ?: 10000L).coerceIn(100L, 10_000_000L)
                    if (title.isNotBlank()) {
                        onSave(title, selectedDhikrKey, selectedPeriod, target, resetProgress)
                    }
                }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
