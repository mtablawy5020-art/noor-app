package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.CompanionRole
import com.example.data.DhikrType
import com.example.data.LocalPreferences
import com.example.data.PartnerNotificationHelper
import com.example.data.SanctuaryRepository
import com.example.ui.components.IslamicPatternBackground
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DhikrScreen
import com.example.ui.screens.DuaScreen
import com.example.ui.screens.GoogleAuthScreen
import com.example.ui.screens.PrayersScreen
import com.example.ui.screens.RolePinSetupScreen
import com.example.ui.screens.SharedGoalScreen
import com.example.ui.theme.TariqAlHalalTheme
import com.example.viewmodel.SanctuarySessionState
import com.example.viewmodel.SanctuaryViewModel
import com.example.viewmodel.UiEvent
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

enum class MainTab(
    val routeId: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("dashboard", R.string.nav_dashboard, Icons.Filled.Home, Icons.Outlined.Home),
    PRAYERS("prayers", R.string.nav_prayers, Icons.Filled.Mosque, Icons.Outlined.Mosque),
    DHIKR("dhikr", R.string.nav_dhikr, Icons.Filled.Spa, Icons.Outlined.Spa),
    DUA("dua", R.string.nav_dua, Icons.Filled.VolunteerActivism, Icons.Outlined.VolunteerActivism),
    GOAL("goal", R.string.nav_goal, Icons.Filled.Flag, Icons.Outlined.Flag)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PartnerNotificationHelper.ensureChannel(this)
        setContent {
            AppRoot()
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val localPrefs = remember(context) { LocalPreferences(context.applicationContext) }
    val savedPrefs by localPrefs.sessionFlow.collectAsStateWithLifecycle(
        initialValue = com.example.data.LocalSessionPrefs()
    )
    val scope = rememberCoroutineScope()
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        TariqAlHalalTheme(darkTheme = savedPrefs.darkModeEnabled) {
            GoogleAuthScreen(
                darkMode = savedPrefs.darkModeEnabled,
                onToggleDarkMode = {
                    scope.launch { localPrefs.setDarkMode(!savedPrefs.darkModeEnabled) }
                },
                onAuthSuccess = {}
            )
        }
    } else {
        key(user.uid) {
            AuthenticatedAppHost(
                currentUserId = user.uid,
                localPrefs = localPrefs
            )
        }
    }
}

@Composable
private fun AuthenticatedAppHost(
    currentUserId: String,
    localPrefs: LocalPreferences
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: SanctuaryViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                SanctuaryViewModel(
                    repository = SanctuaryRepository(db),
                    localPrefs = localPrefs,
                    currentUserId = currentUserId
                )
            }
        }
    )

    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val dataState by viewModel.dataState.collectAsStateWithLifecycle()
    val darkMode by viewModel.darkMode.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var milestoneDialog by remember { mutableStateOf<Pair<DhikrType, Int>?>(null) }

    var hasNotifPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.Snackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.PartnerAlert -> {
                    PartnerNotificationHelper.showPartnerNotification(
                        context = context,
                        id = event.id,
                        title = event.title,
                        body = event.body
                    )
                    snackbarHostState.showSnackbar("${event.title}: ${event.body}")
                }
                is UiEvent.MilestoneCelebration -> {
                    milestoneDialog = event.dhikr to event.milestone
                }
            }
        }
    }

    TariqAlHalalTheme(darkTheme = darkMode) {
        when (val state = sessionState) {
            is SanctuarySessionState.Loading -> {
                IslamicPatternBackground {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            is SanctuarySessionState.NeedsRoleSetup -> {
                RolePinSetupScreen(
                    existingSanctuary = state.existingSanctuary,
                    isSubmitting = state.isSubmitting,
                    errorMessage = state.errorMessage,
                    onSubmit = { role, pin, inviteCode ->
                        viewModel.completeRoleSetup(role, pin, inviteCode)
                    }
                )
            }

            is SanctuarySessionState.Ready -> {
                MainCompanionShell(
                    activeRole = state.activeRole,
                    dataState = dataState,
                    darkMode = darkMode,
                    needsNotificationPermission = !hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    snackbarHostState = snackbarHostState,
                    viewModel = viewModel,
                    onSignOut = {
                        signOutUser(
                            credentialManager = credentialManager,
                            scope = scope,
                            onBeforeSignOut = { viewModel.clearLocalSessionForSignOut() }
                        )
                    }
                )
            }
        }

        milestoneDialog?.let { (dhikr, milestone) ->
            val role = (sessionState as? SanctuarySessionState.Ready)?.activeRole ?: CompanionRole.MAHMOUD
            AlertDialog(
                onDismissRequest = { milestoneDialog = null },
                title = {
                    Text(
                        text = "ما شاء الله! $milestone ${dhikr.shortTitle} ✨",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "تقبل الله ذكرك وزادك من فضله 🤍 تم إرسال إشعار تشجيعي إلى ${role.partnerArabicName} ليشاركك الأجر والبركة."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { milestoneDialog = null }) {
                        Text("آمين 🤍")
                    }
                }
            )
        }
    }
}

@Composable
private fun MainCompanionShell(
    activeRole: CompanionRole,
    dataState: com.example.viewmodel.SanctuaryDashboardData,
    darkMode: Boolean,
    needsNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: SanctuaryViewModel,
    onSignOut: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.DASHBOARD) }
    val scope = rememberCoroutineScope()

    // Handle system back button gracefully from secondary tabs back to Dashboard
    BackHandler(enabled = selectedTab != MainTab.DASHBOARD) {
        selectedTab = MainTab.DASHBOARD
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        MainTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        maxLines = 1,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.routeId}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            IslamicPatternBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (useNavigationRail) {
                        NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                            MainTab.entries.forEach { tab ->
                                val selected = selectedTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { selectedTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(tab.labelRes)) },
                                    modifier = Modifier.testTag("rail_tab_${tab.routeId}")
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 680.dp)
                        ) {
                            when (selectedTab) {
                                MainTab.DASHBOARD -> DashboardScreen(
                                    activeRole = activeRole,
                                    data = dataState,
                                    darkMode = darkMode,
                                    needsNotificationPermission = needsNotificationPermission,
                                    onRequestNotificationPermission = onRequestNotificationPermission,
                                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                                    onNavigateToPrayers = { selectedTab = MainTab.PRAYERS },
                                    onNavigateToDhikr = { selectedTab = MainTab.DHIKR },
                                    onNavigateToDua = { selectedTab = MainTab.DUA },
                                    onNavigateToGoal = { selectedTab = MainTab.GOAL },
                                    onToggleAmeen = { viewModel.toggleDuaAmeen(it) },
                                    onSwitchRole = { viewModel.requestRoleSwitch() },
                                    onUpdatePin = { viewModel.updateCurrentPin(it) },
                                    onSignOut = onSignOut,
                                    onShowMessage = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )

                                MainTab.PRAYERS -> PrayersScreen(
                                    activeRole = activeRole,
                                    data = dataState,
                                    onTogglePrayer = { viewModel.togglePrayer(it) }
                                )

                                MainTab.DHIKR -> DhikrScreen(
                                    activeRole = activeRole,
                                    data = dataState,
                                    onIncrementDhikr = { dhikr, delta ->
                                        viewModel.incrementDhikr(dhikr, delta)
                                    },
                                    onResetTodayDhikr = { viewModel.resetTodayDhikr(it) }
                                )

                                MainTab.DUA -> DuaScreen(
                                    activeRole = activeRole,
                                    duas = dataState.duas,
                                    onSendDua = { content, tag -> viewModel.sendDua(content, tag) },
                                    onToggleAmeen = { viewModel.toggleDuaAmeen(it) },
                                    onTogglePin = { viewModel.toggleDuaPin(it) },
                                    onDeleteDua = { viewModel.deleteDua(it) }
                                )

                                MainTab.GOAL -> SharedGoalScreen(
                                    activeRole = activeRole,
                                    sharedGoal = dataState.sharedGoal,
                                    onSaveGoal = { title, dhikrKey, period, target, reset ->
                                        viewModel.saveSharedGoal(title, dhikrKey, period, target, reset)
                                    },
                                    onSendNudge = { viewModel.sendNudge(it) },
                                    onToggleAdhkarItem = { isMorning, id ->
                                        viewModel.toggleAdhkarItem(isMorning, id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
