package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CompanionRole
import com.example.data.DateKeys
import com.example.data.DuaTag
import com.example.data.SharedDua
import com.example.data.SpiritualContent
import com.example.ui.components.DualPartnerProgressBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.SpiritualCard
import com.example.ui.components.formatArabicNumber
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.SoftChampagneGold
import com.example.ui.theme.SubtleDustyRose
import com.example.viewmodel.SanctuaryDashboardData

@Composable
fun DashboardScreen(
    activeRole: CompanionRole,
    data: SanctuaryDashboardData,
    darkMode: Boolean,
    needsNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onNavigateToPrayers: () -> Unit,
    onNavigateToDhikr: () -> Unit,
    onNavigateToDua: () -> Unit,
    onNavigateToGoal: () -> Unit,
    onToggleAmeen: (SharedDua) -> Unit,
    onSwitchRole: () -> Unit,
    onUpdatePin: (String) -> Unit,
    onSignOut: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val todayKey = remember { DateKeys.todayKey() }
    val todayPrayer = data.todayPrayer(todayKey)
    val mahmoudPrayers = todayPrayer?.completedCount(CompanionRole.MAHMOUD) ?: 0
    val nourPrayers = todayPrayer?.completedCount(CompanionRole.NOUR) ?: 0
    val mahmoudDhikrToday = data.totalTodayDhikr(CompanionRole.MAHMOUD, todayKey)
    val nourDhikrToday = data.totalTodayDhikr(CompanionRole.NOUR, todayKey)
    val (dailyVerse, verseSource) = remember { SpiritualContent.dailyVerseForToday() }
    val latestDua = data.duas.firstOrNull()
    val sharedGoal = data.sharedGoal

    var showSettingsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Spiritual Greeting & Daily Rotating Verse Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_dashboard_header),
                    contentDescription = "خلفية روحانية إسلامية",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xBB0D2621),
                                    Color(0xE0133B34)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = SpiritualContent.timeGreeting(activeRole),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFAF6F0)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "معاً على طاعة الله وفي طريق الحلال",
                                style = MaterialTheme.typography.bodySmall,
                                color = SoftChampagneGold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = onToggleDarkMode,
                                modifier = Modifier.testTag("dashboard_dark_mode_button")
                            ) {
                                Icon(
                                    imageVector = if (darkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                    contentDescription = stringResource(R.string.dark_mode_label),
                                    tint = SoftChampagneGold
                                )
                            }
                            IconButton(
                                onClick = { showSettingsDialog = true },
                                modifier = Modifier.testTag("dashboard_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = stringResource(R.string.settings_title),
                                    tint = Color(0xFFFAF6F0)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        color = Color(0x28FFFFFF),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, SoftChampagneGold.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dailyVerse,
                                fontFamily = AmiriFontFamily,
                                fontSize = 19.sp,
                                lineHeight = 32.sp,
                                color = Color(0xFFFFFDF9),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "— $verseSource —",
                                style = MaterialTheme.typography.labelMedium,
                                color = SoftChampagneGold
                            )
                        }
                    }
                }
            }
        }

        // Optional notification permission banner on Android 13+
        if (needsNotificationPermission) {
            item {
                SpiritualCard(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    borderColor = SoftChampagneGold.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "فعّل الإشعارات لتصلك تنبيهات صلاة وأذكار ${activeRole.partnerArabicName} لحظياً",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        TextButton(
                            onClick = onRequestNotificationPermission,
                            modifier = Modifier.testTag("enable_notifications_button")
                        ) {
                            Text("تفعيل", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Live Partner Status Bar (Mahmoud & Nour side by side)
        item {
            SectionHeader(
                title = "حالتنا اليوم معاً",
                subtitle = "متابعة لحظية للصلوات الخمس ومجموع الأذكار اليومية"
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PartnerLiveStatusCard(
                    name = CompanionRole.MAHMOUD.arabicName,
                    isCurrentUser = activeRole == CompanionRole.MAHMOUD,
                    prayersCompleted = mahmoudPrayers,
                    dhikrToday = mahmoudDhikrToday,
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClickPrayers = onNavigateToPrayers,
                    onClickDhikr = onNavigateToDhikr,
                    modifier = Modifier.weight(1f)
                )
                PartnerLiveStatusCard(
                    name = CompanionRole.NOUR.arabicName,
                    isCurrentUser = activeRole == CompanionRole.NOUR,
                    prayersCompleted = nourPrayers,
                    dhikrToday = nourDhikrToday,
                    accentColor = SubtleDustyRose,
                    onClickPrayers = onNavigateToPrayers,
                    onClickDhikr = onNavigateToDhikr,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Shared Goal Progress Card ("طريق الحلال")
        item {
            SpiritualCard {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Flag,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "هدفنا المشترك — طريق الحلال",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = sharedGoal?.title ?: "10,000 استغفار للرزق والزواج في الحلال",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        TextButton(
                            onClick = onNavigateToGoal,
                            modifier = Modifier.testTag("dashboard_open_goal_button")
                        ) {
                            Text("التفاصيل")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    DualPartnerProgressBar(
                        mahmoudCount = sharedGoal?.mahmoudProgress ?: 0L,
                        nourCount = sharedGoal?.nourProgress ?: 0L,
                        targetCount = sharedGoal?.targetCount ?: 10000L
                    )
                }
            }
        }

        // 4. Latest Shared Dua Card with Quick "آمين 🤍" button
        item {
            SpiritualCard(
                borderColor = SubtleDustyRose.copy(alpha = 0.35f)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.VolunteerActivism,
                                contentDescription = null,
                                tint = SubtleDustyRose,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "آخر دعوة بينكما",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(
                            onClick = onNavigateToDua,
                            modifier = Modifier.testTag("dashboard_open_dua_button")
                        ) {
                            Text("كل الأدعية")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (latestDua == null) {
                        Text(
                            text = "لم تُرسَل دعوة بعد.. ابدأ الآن واكتب دعاءً صادقاً بالرزق والتيسير لشريكك 🤲",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FilledTonalButton(
                            onClick = onNavigateToDua,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("dashboard_first_dua_button")
                        ) {
                            Text("اكتب أول دعوة 🤍")
                        }
                    } else {
                        val sender = CompanionRole.fromKey(latestDua.senderRole) ?: CompanionRole.MAHMOUD
                        val tag = DuaTag.fromKey(latestDua.tag)
                        val myAmeen = latestDua.hasAmeenBy(activeRole)
                        val partnerAmeen = latestDua.hasAmeenBy(activeRole.partner())

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = "من ${sender.arabicName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = tag.arabicLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = latestDua.content,
                            fontFamily = AmiriFontFamily,
                            fontSize = 19.sp,
                            lineHeight = 32.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (partnerAmeen) {
                                    "${activeRole.partnerArabicName} أمّن على هذا الدعاء 🤍"
                                } else {
                                    "في انتظار تأمين ${activeRole.partnerArabicName}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = { onToggleAmeen(latestDua) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (myAmeen) SubtleDustyRose else MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("dashboard_ameen_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (myAmeen) "آمين ✓ 🤍" else stringResource(R.string.ameen_button),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        SanctuarySettingsDialog(
            activeRole = activeRole,
            inviteCode = data.sanctuary?.inviteCode.orEmpty(),
            partnerConnected = (data.sanctuary?.memberUids?.size ?: 1) >= 2,
            darkMode = darkMode,
            onCopyInviteCode = { code ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("رمز الربط", code))
                onShowMessage("تم نسخ رمز الربط: $code")
            },
            onToggleDarkMode = onToggleDarkMode,
            onSwitchRole = {
                showSettingsDialog = false
                onSwitchRole()
            },
            onUpdatePin = { pin ->
                onUpdatePin(pin)
                showSettingsDialog = false
            },
            onSignOut = {
                showSettingsDialog = false
                onSignOut()
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun PartnerLiveStatusCard(
    name: String,
    isCurrentUser: Boolean,
    prayersCompleted: Int,
    dhikrToday: Int,
    accentColor: Color,
    onClickPrayers: () -> Unit,
    onClickDhikr: () -> Unit,
    modifier: Modifier = Modifier
) {
    SpiritualCard(
        modifier = modifier,
        borderColor = if (isCurrentUser) accentColor.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                if (isCurrentUser) {
                    Surface(
                        color = accentColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "أنت",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                onClick = onClickPrayers,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Mosque,
                            contentDescription = "الصلوات",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "الصلوات",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "$prayersCompleted / 5",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                onClick = onClickDhikr,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Spa,
                            contentDescription = "الأذكار",
                            tint = SoftChampagneGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "الذكر",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formatArabicNumber(dhikrToday),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun SanctuarySettingsDialog(
    activeRole: CompanionRole,
    inviteCode: String,
    partnerConnected: Boolean,
    darkMode: Boolean,
    onCopyInviteCode: (String) -> Unit,
    onToggleDarkMode: () -> Unit,
    onSwitchRole: () -> Unit,
    onUpdatePin: (String) -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    var showPinEditor by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "الملف النشط حالياً: ${activeRole.arabicName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (inviteCode.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "رمز ربط الشريك (${if (partnerConnected) "مرتبط ✓" else "بانتظار الربط"})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = inviteCode,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            IconButton(
                                onClick = { onCopyInviteCode(inviteCode) },
                                modifier = Modifier.testTag("copy_invite_code_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "نسخ رمز الربط"
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                OutlinedButton(
                    onClick = onToggleDarkMode,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (darkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (darkMode) "تفعيل الوضع النهاري الدافئ" else "تفعيل الوضع الليلي الهادئ")
                }

                OutlinedButton(
                    onClick = onSwitchRole,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("switch_role_button")
                ) {
                    Icon(imageVector = Icons.Filled.SwapHoriz, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.switch_profile_label))
                }

                OutlinedButton(
                    onClick = { showPinEditor = !showPinEditor },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Filled.Pin, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تغيير الرمز السري (4 أرقام)")
                }

                if (showPinEditor) {
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { input -> newPin = input.filter { it.isDigit() }.take(4) },
                        label = { Text("الرمز الجديد (4 أرقام)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onUpdatePin(newPin) },
                        enabled = newPin.length == 4,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حفظ الرمز الجديد")
                    }
                }

                TextButton(
                    onClick = onSignOut,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sign_out_button")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.sign_out_label))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
