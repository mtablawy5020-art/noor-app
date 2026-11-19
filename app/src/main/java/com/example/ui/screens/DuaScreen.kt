package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CompanionRole
import com.example.data.DuaTag
import com.example.data.SharedDua
import com.example.ui.components.SectionHeader
import com.example.ui.components.SpiritualCard
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.SoftChampagneGold
import com.example.ui.theme.SubtleDustyRose
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DuaScreen(
    activeRole: CompanionRole,
    duas: List<SharedDua>,
    onSendDua: (String, DuaTag) -> Unit,
    onToggleAmeen: (SharedDua) -> Unit,
    onTogglePin: (SharedDua) -> Unit,
    onDeleteDua: (String) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var draftText by remember { mutableStateOf("") }
    var selectedDraftTag by remember { mutableStateOf(DuaTag.MARRIAGE) }
    var activeFilter by remember { mutableStateOf("all") }

    val filteredDuas = remember(duas, activeFilter) {
        when (activeFilter) {
            "all" -> duas
            "pinned" -> duas.filter { it.pinned }
            else -> duas.filter { it.tag == activeFilter }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "مساحة الدعاء المشتركة",
                subtitle = "ادعُ لشريكك بظهر الغيب وسيصله إشعار فوري ليقول آمين 🤍"
            )
        }

        // Compose new Dua card
        item {
            SpiritualCard(
                borderColor = SoftChampagneGold.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = draftText,
                        onValueChange = { if (it.length <= 1000) draftText = it },
                        placeholder = {
                            Text("اكتب دعاءً صادقاً للرزق أو التيسير أو راحة البال...")
                        },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dua_input_field")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(DuaTag.entries, key = { it.key }) { tag ->
                            FilterChip(
                                selected = selectedDraftTag == tag,
                                onClick = { selectedDraftTag = tag },
                                label = { Text(tag.arabicLabel) },
                                modifier = Modifier.testTag("dua_draft_tag_${tag.key}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (draftText.isNotBlank()) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSendDua(draftText, selectedDraftTag)
                                draftText = ""
                            }
                        },
                        enabled = draftText.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("send_dua_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.send_dua_button),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Filter row
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = activeFilter == "all",
                        onClick = { activeFilter = "all" },
                        label = { Text("الكل (${duas.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = activeFilter == "pinned",
                        onClick = { activeFilter = "pinned" },
                        label = { Text("المثبّتة") }
                    )
                }
                items(DuaTag.entries.filter { it != DuaTag.GENERAL }, key = { "filter_${it.key}" }) { tag ->
                    FilterChip(
                        selected = activeFilter == tag.key,
                        onClick = { activeFilter = tag.key },
                        label = { Text(tag.arabicLabel) }
                    )
                }
            }
        }

        // Empty state
        if (filteredDuas.isEmpty()) {
            item {
                SpiritualCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VolunteerActivism,
                            contentDescription = null,
                            tint = SoftChampagneGold,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "لا توجد أدعية في هذا التصنيف بعد",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ابدأ بإرسال أول دعوة صادقة من القلب 🤲🤍",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Shared Duas feed
        items(filteredDuas, key = { it.id }) { dua ->
            SharedDuaCard(
                dua = dua,
                activeRole = activeRole,
                onToggleAmeen = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleAmeen(dua)
                },
                onTogglePin = { onTogglePin(dua) },
                onDelete = { onDeleteDua(dua.id) }
            )
        }
    }
}

@Composable
private fun SharedDuaCard(
    dua: SharedDua,
    activeRole: CompanionRole,
    onToggleAmeen: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val sender = CompanionRole.fromKey(dua.senderRole) ?: CompanionRole.MAHMOUD
    val partner = activeRole.partner()
    val tag = DuaTag.fromKey(dua.tag)
    val myAmeen = dua.hasAmeenBy(activeRole)
    val partnerAmeen = dua.hasAmeenBy(partner)

    val formattedTime = remember(dua.createdAt) {
        val date = dua.createdAt?.toDate()
        if (date != null) {
            SimpleDateFormat("d MMM • hh:mm a", Locale("ar")).format(date)
        } else {
            "الآن"
        }
    }

    SpiritualCard(
        borderColor = if (dua.pinned) SoftChampagneGold.copy(alpha = 0.7f)
        else MaterialTheme.colorScheme.outlineVariant
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (sender == CompanionRole.MAHMOUD) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            SubtleDustyRose.copy(alpha = 0.16f)
                        },
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = sender.arabicName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (sender == CompanionRole.MAHMOUD) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                SubtleDustyRose
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = tag.arabicLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.testTag("pin_dua_${dua.id}")
                    ) {
                        Icon(
                            imageVector = if (dua.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (dua.pinned) "إلغاء التثبيت" else "تثبيت الدعاء",
                            tint = if (dua.pinned) SoftChampagneGold else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_dua_${dua.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "حذف الدعاء",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = dua.content,
                fontFamily = AmiriFontFamily,
                fontSize = 20.sp,
                lineHeight = 34.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            dua.ameenByMahmoud && dua.ameenByNour -> "محمود ونور قالا: آمين 🤍"
                            partnerAmeen -> "${partner.arabicName} قال${if (!partner.isMale) "ت" else ""}: آمين 🤍"
                            myAmeen -> "أنت أمّنت على الدعاء"
                            else -> "بانتظار التأمين"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (partnerAmeen) SubtleDustyRose else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (partnerAmeen) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                Surface(
                    onClick = onToggleAmeen,
                    shape = RoundedCornerShape(14.dp),
                    color = if (myAmeen) SubtleDustyRose else MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(
                        1.dp,
                        if (myAmeen) SubtleDustyRose else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("ameen_dua_${dua.id}")
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (myAmeen) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = null,
                                tint = if (myAmeen) MaterialTheme.colorScheme.onTertiary
                                else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (myAmeen) "آمين ✓ 🤍" else "آمين 🤍",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (myAmeen) MaterialTheme.colorScheme.onTertiary
                                else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
