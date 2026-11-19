package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CompanionRole
import com.example.data.DateKeys
import com.example.data.DhikrCount
import com.example.data.DhikrType
import com.example.data.DuaTag
import com.example.data.LocalPreferences
import com.example.data.NotificationQueueItem
import com.example.data.PinSecurity
import com.example.data.PrayerDaily
import com.example.data.PrayerType
import com.example.data.Sanctuary
import com.example.data.SanctuaryRepository
import com.example.data.SharedDua
import com.example.data.SharedGoal
import com.example.data.SpiritualNudge
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SanctuarySessionState {
    data object Loading : SanctuarySessionState
    data class NeedsRoleSetup(
        val existingSanctuary: Sanctuary? = null,
        val errorMessage: String? = null,
        val isSubmitting: Boolean = false
    ) : SanctuarySessionState

    data class Ready(
        val sanctuaryId: String,
        val activeRole: CompanionRole
    ) : SanctuarySessionState
}

data class SanctuaryDashboardData(
    val sanctuary: Sanctuary? = null,
    val prayersHistory: List<PrayerDaily> = emptyList(),
    val dhikrCounts: List<DhikrCount> = emptyList(),
    val duas: List<SharedDua> = emptyList(),
    val sharedGoal: SharedGoal? = null,
    val notifications: List<NotificationQueueItem> = emptyList(),
    val streamError: String? = null
) {
    fun todayPrayer(todayKey: String = DateKeys.todayKey()): PrayerDaily? =
        prayersHistory.firstOrNull { it.dateKey == todayKey }

    fun dhikrCountFor(role: CompanionRole, dhikr: DhikrType): DhikrCount? =
        dhikrCounts.firstOrNull { it.role == role.key && it.dhikrKey == dhikr.key }

    fun totalTodayDhikr(role: CompanionRole, todayKey: String = DateKeys.todayKey()): Int =
        dhikrCounts
            .filter { it.role == role.key }
            .sumOf { it.effectiveDailyCount(todayKey) }

    fun calculatePrayerStreak(role: CompanionRole): Int {
        val byDate = prayersHistory.associateBy { it.dateKey }
        val lastDays = DateKeys.lastNDaysKeys(30).reversed()
        var streak = 0
        for ((index, key) in lastDays.withIndex()) {
            val record = byDate[key]
            val count = record?.completedCount(role) ?: 0
            if (count == 5) {
                streak++
            } else if (index == 0) {
                // Today might still be in progress; continue checking yesterday
                continue
            } else {
                break
            }
        }
        return streak
    }
}

sealed interface UiEvent {
    data class Snackbar(val message: String) : UiEvent
    data class PartnerAlert(val id: Int, val title: String, val body: String) : UiEvent
    data class MilestoneCelebration(val dhikr: DhikrType, val milestone: Int) : UiEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class SanctuaryViewModel(
    private val repository: SanctuaryRepository,
    private val localPrefs: LocalPreferences,
    private val currentUserId: String
) : ViewModel() {

    private val _sessionState = MutableStateFlow<SanctuarySessionState>(SanctuarySessionState.Loading)
    val sessionState: StateFlow<SanctuarySessionState> = _sessionState.asStateFlow()

    private val _darkMode = MutableStateFlow(false)
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    private val deliveredNotifIds = mutableSetOf<String>()
    private var syncedMemberCount = 0

    val dataState: StateFlow<SanctuaryDashboardData> = _sessionState
        .flatMapLatest { state ->
            when (state) {
                is SanctuarySessionState.Ready -> observeSanctuaryStreams(state.sanctuaryId, state.activeRole)
                else -> flowOf(SanctuaryDashboardData())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = SanctuaryDashboardData()
        )

    init {
        bootstrapSession()
    }

    private fun bootstrapSession() {
        viewModelScope.launch {
            localPrefs.sessionFlow.collect { prefs ->
                _darkMode.value = prefs.darkModeEnabled
                if (_sessionState.value is SanctuarySessionState.Loading) {
                    resolveInitialSession(prefs.selectedRoleKey, prefs.sanctuaryId, prefs.lastAuthUid)
                }
            }
        }
    }

    private suspend fun resolveInitialSession(
        savedRoleKey: String,
        savedSanctuaryId: String,
        savedAuthUid: String
    ) {
        val profileResult = repository.getUserProfileOnce(currentUserId)
        val profile = profileResult.getOrNull()

        if (profile != null && profile.sanctuaryId.isNotBlank()) {
            _darkMode.value = profile.darkModeEnabled
            localPrefs.setDarkMode(profile.darkModeEnabled)
            val savedRole = if (savedAuthUid == currentUserId) CompanionRole.fromKey(savedRoleKey) else null
            val effectiveRole = savedRole ?: CompanionRole.fromKey(profile.activeRole) ?: CompanionRole.MAHMOUD
            localPrefs.saveSession(currentUserId, effectiveRole.key, profile.sanctuaryId)
            _sessionState.value = SanctuarySessionState.Ready(
                sanctuaryId = profile.sanctuaryId,
                activeRole = effectiveRole
            )
            return
        }

        // Check if saved local session belongs to this UID and still exists
        if (savedAuthUid == currentUserId && savedSanctuaryId.isNotBlank()) {
            val savedRole = CompanionRole.fromKey(savedRoleKey)
            val existingSanctuary = repository.getSanctuaryOnce(savedSanctuaryId).getOrNull()
            if (savedRole != null && existingSanctuary != null && currentUserId in existingSanctuary.memberUids) {
                _sessionState.value = SanctuarySessionState.Ready(savedSanctuaryId, savedRole)
                return
            }
        }

        _sessionState.value = SanctuarySessionState.NeedsRoleSetup()
    }

    private fun observeSanctuaryStreams(
        sanctuaryId: String,
        activeRole: CompanionRole
    ): Flow<SanctuaryDashboardData> {
        val sanctuaryFlow = repository.observeSanctuary(currentUserId, sanctuaryId)
            .safeResultFlow("sanctuary")
        val prayersFlow = repository.observePrayers(currentUserId, sanctuaryId)
            .safeResultFlow("prayers")
        val dhikrFlow = repository.observeDhikrCounts(currentUserId, sanctuaryId)
            .safeResultFlow("dhikr")
        val duasFlow = repository.observeDuas(currentUserId, sanctuaryId)
            .safeResultFlow("duas")
        val goalFlow = repository.observeSharedGoal(currentUserId, sanctuaryId)
            .safeResultFlow("shared_goal")
        val notifsFlow = repository.observeNotifications(currentUserId, sanctuaryId)
            .safeResultFlow("notifications")

        return combine(
            combine(sanctuaryFlow, prayersFlow, dhikrFlow) { s, p, d -> Triple(s, p, d) },
            combine(duasFlow, goalFlow, notifsFlow) { u, g, n -> Triple(u, g, n) }
        ) { left, right ->
            val sanctuary = left.first.getOrNull()
            val prayers = left.second.getOrNull().orEmpty()
            val dhikrs = left.third.getOrNull().orEmpty()
            val duas = right.first.getOrNull().orEmpty()
            val goal = right.second.getOrNull()
            val notifs = right.third.getOrNull().orEmpty()

            val firstError = listOfNotNull(
                left.first.exceptionOrNull(),
                left.second.exceptionOrNull(),
                left.third.exceptionOrNull(),
                right.first.exceptionOrNull(),
                right.second.exceptionOrNull(),
                right.third.exceptionOrNull()
            ).firstOrNull()?.localizedMessage

            // If partner joined, backfill memberUids so both partners see all history
            if (sanctuary != null && sanctuary.memberUids.size >= 2 && syncedMemberCount < sanctuary.memberUids.size) {
                syncedMemberCount = sanctuary.memberUids.size
                viewModelScope.launch {
                    repository.syncMemberUidsForSanctuary(sanctuary)
                }
            }

            // Dispatch incoming undelivered notifications intended for the active role
            val incoming = notifs.filter {
                !it.delivered && it.recipientRole == activeRole.key && it.id !in deliveredNotifIds
            }
            for (item in incoming) {
                deliveredNotifIds.add(item.id)
                _events.tryEmit(
                    UiEvent.PartnerAlert(
                        id = item.id.hashCode(),
                        title = item.title,
                        body = item.body
                    )
                )
                viewModelScope.launch {
                    repository.markNotificationDelivered(item.id)
                }
            }

            SanctuaryDashboardData(
                sanctuary = sanctuary,
                prayersHistory = prayers,
                dhikrCounts = dhikrs,
                duas = duas,
                sharedGoal = goal,
                notifications = notifs,
                streamError = firstError
            )
        }
    }

    private fun <T> Flow<T>.safeResultFlow(label: String): Flow<Result<T>> =
        this.map { Result.success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Stream error in $label", rootError)
                emit(Result.failure(rootError))
            }

    // --- ROLE & PIN SETUP / SWITCHING ---

    fun completeRoleSetup(
        role: CompanionRole,
        pin: String,
        inviteCode: String
    ) {
        val currentState = _sessionState.value as? SanctuarySessionState.NeedsRoleSetup ?: return
        if (pin.length != 4 || !pin.all { it.isDigit() }) {
            _sessionState.value = currentState.copy(errorMessage = "يرجى إدخال رمز سري من 4 أرقام.")
            return
        }

        _sessionState.value = currentState.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            val trimmedCode = inviteCode.trim()
            val existingSanctuary = currentState.existingSanctuary

            val result: Result<Sanctuary> = when {
                trimmedCode.isNotEmpty() -> {
                    repository.joinSanctuaryByInviteCode(trimmedCode, role, pin, _darkMode.value)
                }
                existingSanctuary != null -> {
                    val pinHash = PinSecurity.sha256(pin)
                    val storedHash = if (role == CompanionRole.MAHMOUD) {
                        existingSanctuary.mahmoudPinHash
                    } else {
                        existingSanctuary.nourPinHash
                    }
                    if (storedHash.isNotEmpty() && storedHash != pinHash) {
                        Result.failure(IllegalArgumentException("الرمز السري غير صحيح لملف ${role.arabicName}."))
                    } else {
                        repository.updateRolePinAndProfile(
                            sanctuaryId = existingSanctuary.id,
                            role = role,
                            pinHash = pinHash,
                            shouldSavePin = storedHash.isEmpty(),
                            darkModeEnabled = _darkMode.value
                        ).map { existingSanctuary }
                    }
                }
                else -> {
                    repository.createSanctuaryAndProfile(role, pin, _darkMode.value)
                }
            }

            result.onSuccess { sanctuary ->
                localPrefs.saveSession(currentUserId, role.key, sanctuary.id)
                _sessionState.value = SanctuarySessionState.Ready(
                    sanctuaryId = sanctuary.id,
                    activeRole = role
                )
            }.onFailure { err ->
                _sessionState.value = currentState.copy(
                    isSubmitting = false,
                    errorMessage = err.localizedMessage ?: "حدث خطأ أثناء إعداد الحساب."
                )
            }
        }
    }

    fun requestRoleSwitch() {
        val sanctuary = dataState.value.sanctuary
        _sessionState.value = SanctuarySessionState.NeedsRoleSetup(existingSanctuary = sanctuary)
    }

    fun updateCurrentPin(newPin: String) {
        val ready = _sessionState.value as? SanctuarySessionState.Ready ?: return
        if (newPin.length != 4 || !newPin.all { it.isDigit() }) {
            _events.tryEmit(UiEvent.Snackbar("يجب أن يتكون الرمز السري من 4 أرقام."))
            return
        }
        viewModelScope.launch {
            repository.updateRolePinAndProfile(
                sanctuaryId = ready.sanctuaryId,
                role = ready.activeRole,
                pinHash = PinSecurity.sha256(newPin),
                shouldSavePin = true,
                darkModeEnabled = _darkMode.value
            ).onSuccess {
                _events.tryEmit(UiEvent.Snackbar("تم تحديث الرمز السري بنجاح 🤍"))
            }.onFailure {
                _events.tryEmit(UiEvent.Snackbar("تعذر تحديث الرمز السري، حاول مرة أخرى."))
            }
        }
    }

    fun toggleDarkMode() {
        val next = !_darkMode.value
        _darkMode.value = next
        viewModelScope.launch {
            localPrefs.setDarkMode(next)
            repository.updateDarkModeInProfile(next)
        }
    }

    // --- FEATURE ACTIONS ---

    fun togglePrayer(prayer: PrayerType) {
        val ready = _sessionState.value as? SanctuarySessionState.Ready ?: return
        val sanctuary = dataState.value.sanctuary ?: return
        val today = dataState.value.todayPrayer()
        val currentlyDone = today?.isCompleted(ready.activeRole, prayer) ?: false

        viewModelScope.launch {
            repository.togglePrayer(
                sanctuary = sanctuary,
                role = ready.activeRole,
                prayer = prayer,
                newCompleted = !currentlyDone,
                existingToday = today
            ).onSuccess {
                if (!currentlyDone) {
                    _events.tryEmit(
                        UiEvent.Snackbar("تقبل الله صلاة ${prayer.arabicName} 🕌 تم إشعار ${ready.activeRole.partnerArabicName}")
                    )
                }
            }.onFailure {
                _events.tryEmit(UiEvent.Snackbar("تعذر حفظ حالة الصلاة، تحقق من الاتصال."))
            }
        }
    }

    fun incrementDhikr(dhikr: DhikrType, delta: Int = 1) {
        val ready = _sessionState.value as? SanctuarySessionState.Ready ?: return
        val sanctuary = dataState.value.sanctuary ?: return
        val existing = dataState.value.dhikrCountFor(ready.activeRole, dhikr)
        val goal = dataState.value.sharedGoal

        viewModelScope.launch {
            repository.incrementDhikr(
                sanctuary = sanctuary,
                role = ready.activeRole,
                dhikr = dhikr,
                existingCount = existing,
                currentGoal = goal,
                delta = delta
            ).onSuccess { milestone ->
                if (milestone != null) {
                    _events.tryEmit(UiEvent.MilestoneCelebration(dhikr, milestone))
                }
            }.onFailure {
                _events.tryEmit(UiEvent.Snackbar("تعذر حفظ الذكر حالياً."))
            }
        }
    }

    fun resetTodayDhikr(dhikr: DhikrType) {
        val ready = _sessionState.value as? SanctuarySessionState.Ready ?: return
        val sanctuary = dataState.value.sanctuary ?: return
        viewModelScope.launch {
            repository.resetDailyDhikr(sanctuary, ready.activeRole, dhikr)
                .onSuccess {
                    _events.tryEmit(UiEvent.Snackbar("تم تصفير عدّاد اليوم لـ ${dhikr.shortTitle}"))
                }
        }
    }

    fun sendDua(content: String, tag: DuaTag) {
        val ready = _sessionState.value as? SanctuarySessionState.Ready ?: return
        val sanctuary = dataState.value.sanctuary ?: return
        if (content.trim().isEmpty()) {
            _events.tryEmit(UiEvent.Snackbar("اكتب دعاءً صادقاً قبل الإرسال 🤲"))
            return
        }
        viewModelScope.launch {
            repository.sendDua(sanctuary, ready.activeRole, content, tag)
                .onSuccess {
                    _events.tryEmit(UiEvent.Snackbar("تم إرسال الدعوة وإشعار ${ready.activeRole.partnerArabicName} 🤲🤍"))
                }
                .onFailure {
                    _events.tryEmit(UiEvent.Snackbar("تعذر إرسال الدعاء، حاول مجدداً."))
                }
        }
    }

    fun toggleDuaAmeen(dua: SharedDua) {
        val ready = _sessionState.value as? SanctuarySessionState.Ready ?: return
        val sanctuary = dataState.value.sanctuary ?: return
        viewModelScope.launch {
            repository.toggleDuaAmeen(sanctuary, dua, ready.activeRole)
                .onFailure {
                    _events.tryEmit(UiEvent.Snackbar("تعذر تحديث حالة آمين."))
                }
        }
    }

    fun toggleDuaPin(dua: SharedDua) {
        viewModelScope.launch {
            repository.toggleDuaPin(dua)
                .onFailure {
                    _events.tryEmit(UiEvent.Snackbar("تعذر تثبيت الدعاء."))
                }
        }
    }

    fun deleteDua(duaId: String) {
        viewModelScope.launch {
            repository.deleteDua(duaId)
                .onSuccess {
                    _events.tryEmit(UiEvent.Snackbar("تم حذف الدعاء."))
                }
        }
    }

    fun saveSharedGoal(
        title: String,
        dhikrKey: String,
        period: String,
        targetCount: Long,
        resetProgress: Boolean
    ) {
        val sanctuary = dataState.value.sanctuary ?: return
        viewModelScope.launch {
            repository.saveSharedGoalConfig(
                sanctuary = sanctuary,
                existingGoal = dataState.value.sharedGoal,
                title = title,
                dhikrKey = dhikrKey,
                period = period,
                targetCount = targetCount,
                resetProgress = resetProgress
            ).onSuccess {
                _events.tryEmit(UiEvent.Snackbar("تم حفظ هدفنا المشترك بنجاح ✨"))
            }.onFailure {
                _events.tryEmit(UiEvent.Snackbar("تعذر حفظ الهدف المشترك."))
            }
        }
    }

    fun toggleAdhkarItem(isMorning: Boolean, itemId: String) {
        val ready = _sessionState.value as? SanctuarySessionState.Ready ?: return
        val sanctuary = dataState.value.sanctuary ?: return
        viewModelScope.launch {
            repository.toggleAdhkarChecklistItem(
                sanctuary = sanctuary,
                existingGoal = dataState.value.sharedGoal,
                role = ready.activeRole,
                isMorning = isMorning,
                itemId = itemId
            ).onFailure {
                _events.tryEmit(UiEvent.Snackbar("تعذر تحديث قائمة الأذكار."))
            }
        }
    }

    fun sendNudge(nudge: SpiritualNudge) {
        val ready = _sessionState.value as? SanctuarySessionState.Ready ?: return
        val sanctuary = dataState.value.sanctuary ?: return
        viewModelScope.launch {
            repository.sendSpiritualNudge(sanctuary, ready.activeRole, nudge)
                .onSuccess {
                    _events.tryEmit(UiEvent.Snackbar("تم إرسال التذكير إلى ${ready.activeRole.partnerArabicName} 🤍"))
                }
                .onFailure {
                    _events.tryEmit(UiEvent.Snackbar("تعذر إرسال التذكير."))
                }
        }
    }

    suspend fun clearLocalSessionForSignOut() {
        localPrefs.clearRoleSession()
    }

    private companion object {
        const val TAG = "SanctuaryVM"
    }
}
