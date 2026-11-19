package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.data.CompanionRole
import com.example.data.Sanctuary
import com.example.ui.components.IslamicPatternBackground
import com.example.ui.components.SpiritualCard
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.SoftChampagneGold
import com.example.ui.theme.SubtleDustyRose

@Composable
fun GoogleAuthScreen(
    darkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    IslamicPatternBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onToggleDarkMode,
                        modifier = Modifier.testTag("auth_dark_mode_toggle")
                    ) {
                        Icon(
                            imageVector = if (darkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = stringResource(R.string.dark_mode_label),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                // Arch illustration framed softly
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .border(1.5.dp, SoftChampagneGold.copy(alpha = 0.45f), RoundedCornerShape(28.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_welcome_arch),
                        contentDescription = "زخرفة إسلامية هادئة لتطبيق طريق الحلال",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.65f)
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                    fontFamily = AmiriFontFamily,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.auth_welcome_title),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "محمود 🤍 نور",
                    style = MaterialTheme.typography.titleLarge,
                    color = SubtleDustyRose,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.auth_welcome_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        errorMessage = null
                        isLoading = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.auth_google_button),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "مساحة خاصة ومزامنة لحظية آمنة بينكما فقط",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun RolePinSetupScreen(
    existingSanctuary: Sanctuary?,
    isSubmitting: Boolean,
    errorMessage: String?,
    onSubmit: (role: CompanionRole, pin: String, inviteCode: String) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var selectedRole by remember { mutableStateOf<CompanionRole?>(null) }
    var pinDigits by remember { mutableStateOf("") }
    var showInviteField by remember { mutableStateOf(false) }
    var inviteCode by remember { mutableStateOf("") }

    val activeRole = selectedRole
    val expectedLength = activeRole?.expectedPasswordLength ?: 6

    IslamicPatternBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.role_select_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "اضغط على صورتك ثم أدخل كلمة السر الخاصة بك 🤍",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Two illustrated circular Avatar cards: Mahmoud & Nour
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    RoleChoiceCard(
                        role = CompanionRole.MAHMOUD,
                        avatarRes = R.drawable.img_avatar_mahmoud,
                        selected = selectedRole == CompanionRole.MAHMOUD,
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedRole = CompanionRole.MAHMOUD
                            pinDigits = ""
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("select_role_mahmoud")
                    )
                    RoleChoiceCard(
                        role = CompanionRole.NOUR,
                        avatarRes = R.drawable.img_avatar_nour,
                        selected = selectedRole == CompanionRole.NOUR,
                        accentColor = SubtleDustyRose,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedRole = CompanionRole.NOUR
                            pinDigits = ""
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("select_role_nour")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Password keypad appears smoothly once an avatar is tapped
                AnimatedVisibility(visible = activeRole != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val chosenRole = activeRole ?: CompanionRole.MAHMOUD
                        val accent = if (chosenRole == CompanionRole.MAHMOUD) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            SubtleDustyRose
                        }

                        SpiritualCard {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "أدخل كلمة سر ${chosenRole.arabicName} ($expectedLength أرقام)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(if (expectedLength > 6) 10.dp else 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(expectedLength) { index ->
                                        val filled = index < pinDigits.length
                                        Box(
                                            modifier = Modifier
                                                .size(if (expectedLength > 6) 15.dp else 17.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (filled) accent
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .border(
                                                    width = 1.5.dp,
                                                    color = if (filled) accent
                                                    else MaterialTheme.colorScheme.outline,
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                }

                                if (!errorMessage.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = errorMessage,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                val rows = listOf(
                                    listOf("1", "2", "3"),
                                    listOf("4", "5", "6"),
                                    listOf("7", "8", "9"),
                                    listOf("", "0", "DEL")
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    rows.forEach { row ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                                        ) {
                                            row.forEach { key ->
                                                when (key) {
                                                    "" -> Spacer(modifier = Modifier.size(62.dp))
                                                    "DEL" -> {
                                                        Surface(
                                                            onClick = {
                                                                if (pinDigits.isNotEmpty()) {
                                                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                    pinDigits = pinDigits.dropLast(1)
                                                                }
                                                            },
                                                            shape = CircleShape,
                                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                                            modifier = Modifier
                                                                .size(62.dp)
                                                                .testTag("pin_backspace_button")
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                                    contentDescription = "حذف رقم",
                                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                    }
                                                    else -> {
                                                        Surface(
                                                            onClick = {
                                                                if (pinDigits.length < 12 && !isSubmitting) {
                                                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                    pinDigits += key
                                                                }
                                                            },
                                                            shape = CircleShape,
                                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                            modifier = Modifier
                                                                .size(62.dp)
                                                                .testTag("pin_digit_$key")
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Text(
                                                                    text = key,
                                                                    style = MaterialTheme.typography.headlineMedium,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.onSurface
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (existingSanctuary == null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            TextButton(
                                onClick = { showInviteField = !showInviteField },
                                modifier = Modifier.testTag("toggle_invite_code_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Key,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.invite_code_toggle),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            AnimatedVisibility(visible = showInviteField) {
                                OutlinedTextField(
                                    value = inviteCode,
                                    onValueChange = { inviteCode = it.uppercase().take(12) },
                                    label = { Text(stringResource(R.string.invite_code_hint)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                        .testTag("invite_code_input")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { onSubmit(chosenRole, pinDigits, inviteCode) },
                            enabled = pinDigits.length >= 4 && !isSubmitting,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("confirm_role_pin_button")
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "دخول باسم ${chosenRole.arabicName} 🤍",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleChoiceCard(
    role: CompanionRole,
    avatarRes: Int,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (selected) accentColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 2.5.dp else 1.dp,
            color = if (selected) accentColor else MaterialTheme.colorScheme.outlineVariant
        ),
        tonalElevation = if (selected) 4.dp else 1.dp,
        modifier = modifier
            .height(168.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = avatarRes),
                contentDescription = "صورة ${role.arabicName}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (selected) 3.dp else 1.5.dp,
                        color = if (selected) accentColor else SoftChampagneGold.copy(alpha = 0.55f),
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = role.arabicName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (selected) accentColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
