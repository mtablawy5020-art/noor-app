package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SanctuaryRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول بحساب جوجل أولاً.")
    }

    // --- REAL-TIME STREAMS (Using ServerTimestampBehavior.ESTIMATE & memberUids filters) ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.GET, path, userId)
                    throw error
                }
        )
    }

    fun observeSanctuary(userId: String, sanctuaryId: String): Flow<Sanctuary?> = flow {
        val path = "sanctuaries/$sanctuaryId"
        emitAll(
            db.collection("sanctuaries").document(sanctuaryId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(Sanctuary::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.GET, path, userId)
                    throw error
                }
        )
    }

    fun observePrayers(userId: String, sanctuaryId: String): Flow<List<PrayerDaily>> = flow {
        val path = "prayers_daily"
        emitAll(
            db.collection(path)
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PrayerDaily::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.sanctuaryId == sanctuaryId }
                        .sortedByDescending { it.dateKey }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeDhikrCounts(userId: String, sanctuaryId: String): Flow<List<DhikrCount>> = flow {
        val path = "dhikr_counts"
        emitAll(
            db.collection(path)
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(DhikrCount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.sanctuaryId == sanctuaryId }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeDuas(userId: String, sanctuaryId: String): Flow<List<SharedDua>> = flow {
        val path = "duas"
        emitAll(
            db.collection(path)
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SharedDua::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.sanctuaryId == sanctuaryId }
                        .sortedWith(
                            compareByDescending<SharedDua> { it.pinned }
                                .thenByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                        )
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeSharedGoal(userId: String, sanctuaryId: String): Flow<SharedGoal?> = flow {
        val path = "shared_goals"
        emitAll(
            db.collection(path)
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SharedGoal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .firstOrNull { it.sanctuaryId == sanctuaryId }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeNotifications(userId: String, sanctuaryId: String): Flow<List<NotificationQueueItem>> = flow {
        val path = "notifications_queue"
        emitAll(
            db.collection(path)
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(NotificationQueueItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.sanctuaryId == sanctuaryId }
                        .sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    // --- ONBOARDING, SANCTUARY CREATION, PARTNER JOIN & PIN MANAGEMENT ---

    suspend fun getUserProfileOnce(userId: String): Result<UserProfile?> = try {
        val snap = db.collection("users").document(userId).get().await()
        val profile = if (snap.exists()) {
            snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else null
        Result.success(profile)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.GET, "users/$userId")
        Result.failure(e)
    }

    suspend fun getSanctuaryOnce(sanctuaryId: String): Result<Sanctuary?> = try {
        val snap = db.collection("sanctuaries").document(sanctuaryId).get().await()
        val sanctuary = if (snap.exists()) {
            snap.toObject(Sanctuary::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else null
        Result.success(sanctuary)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.GET, "sanctuaries/$sanctuaryId")
        Result.failure(e)
    }

    suspend fun createSanctuaryAndProfile(
        role: CompanionRole,
        pin: String,
        darkModeEnabled: Boolean
    ): Result<Sanctuary> {
        var docPath = "sanctuaries"
        return try {
            val uid = requireUserId()
            require(pin.length in 4..12 && pin.all { it.isDigit() }) {
                "كلمة السر يجب أن تتكون من أرقام صالحة."
            }
            val enteredHash = PinSecurity.sha256(pin)
            if (enteredHash != PinSecurity.defaultHashFor(role)) {
                return Result.failure(
                    IllegalArgumentException("كلمة السر غير صحيحة لملف ${role.arabicName}.")
                )
            }

            val sanctuaryId = "sanctuary_$uid"
            val inviteCode = PinSecurity.generateInviteCode()
            val mahmoudHash = PinSecurity.defaultHashFor(CompanionRole.MAHMOUD)
            val nourHash = PinSecurity.defaultHashFor(CompanionRole.NOUR)
            val members = listOf(uid)

            docPath = "sanctuaries/$sanctuaryId"
            val sanctuaryPayload = mapOf(
                "ownerUid" to uid,
                "memberUids" to members,
                "inviteCode" to inviteCode,
                "mahmoudPinHash" to mahmoudHash,
                "nourPinHash" to nourHash,
                "mahmoudStreak" to 0L,
                "nourStreak" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("sanctuaries").document(sanctuaryId).set(sanctuaryPayload).await()

            docPath = "invite_codes/$inviteCode"
            db.collection("invite_codes").document(inviteCode).set(
                mapOf(
                    "sanctuaryId" to sanctuaryId,
                    "ownerUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            docPath = "users/$uid"
            val userPayload = mapOf(
                "userId" to uid,
                "displayName" to role.arabicName,
                "activeRole" to role.key,
                "sanctuaryId" to sanctuaryId,
                "darkModeEnabled" to darkModeEnabled,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).set(userPayload).await()

            // Initialize default Shared Goal (Tariq Al-Halal)
            val goalId = "${sanctuaryId}_main"
            docPath = "shared_goals/$goalId"
            db.collection("shared_goals").document(goalId).set(
                mapOf(
                    "sanctuaryId" to sanctuaryId,
                    "userId" to uid,
                    "memberUids" to members,
                    "title" to "10,000 استغفار للرزق والزواج في الحلال",
                    "dhikrKey" to DhikrType.ISTIGHFAR.key,
                    "period" to "monthly",
                    "targetCount" to 10000L,
                    "mahmoudProgress" to 0L,
                    "nourProgress" to 0L,
                    "adhkarDateKey" to DateKeys.todayKey(),
                    "mahmoudMorningAdhkar" to emptyList<String>(),
                    "mahmoudEveningAdhkar" to emptyList<String>(),
                    "nourMorningAdhkar" to emptyList<String>(),
                    "nourEveningAdhkar" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val created = Sanctuary(
                id = sanctuaryId,
                ownerUid = uid,
                memberUids = members,
                inviteCode = inviteCode,
                mahmoudPinHash = mahmoudHash,
                nourPinHash = nourHash
            )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun joinSanctuaryByInviteCode(
        rawCode: String,
        role: CompanionRole,
        pin: String,
        darkModeEnabled: Boolean
    ): Result<Sanctuary> {
        var docPath = "invite_codes"
        return try {
            val uid = requireUserId()
            val code = rawCode.trim().uppercase()
            require(code.length in 6..12 && code.all { it.isLetterOrDigit() }) {
                "رمز الربط غير صالح."
            }
            require(pin.length in 4..12 && pin.all { it.isDigit() }) {
                "كلمة السر يجب أن تتكون من أرقام صالحة."
            }

            docPath = "invite_codes/$code"
            val codeSnap = db.collection("invite_codes").document(code).get().await()
            if (!codeSnap.exists()) {
                return Result.failure(IllegalArgumentException("لم يتم العثور على رمز الربط، تأكد من صحته."))
            }
            val sanctuaryId = codeSnap.getString("sanctuaryId")
                ?: return Result.failure(IllegalStateException("بيانات رمز الربط غير مكتملة."))

            docPath = "sanctuaries/$sanctuaryId"
            // Join action: add uid to memberUids
            val ownerUid = codeSnap.getString("ownerUid") ?: ""
            val updatedMembers = listOf(ownerUid, uid).distinct()
            if (ownerUid != uid) {
                db.collection("sanctuaries").document(sanctuaryId).update(
                    mapOf(
                        "memberUids" to updatedMembers,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Now we are a member -> read sanctuary and verify role password
            val sanctuarySnap = db.collection("sanctuaries").document(sanctuaryId).get().await()
            val sanctuary = sanctuarySnap.toObject(
                Sanctuary::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return Result.failure(IllegalStateException("تعذر تحميل بيانات المساحة المشتركة."))

            val pinHash = PinSecurity.sha256(pin)
            val storedHash = if (role == CompanionRole.MAHMOUD) sanctuary.mahmoudPinHash else sanctuary.nourPinHash
            val expectedHash = storedHash.ifEmpty { PinSecurity.defaultHashFor(role) }
            if (expectedHash != pinHash) {
                return Result.failure(IllegalArgumentException("كلمة السر غير صحيحة لملف ${role.arabicName}."))
            }
            if (storedHash.isEmpty()) {
                val pinField = if (role == CompanionRole.MAHMOUD) "mahmoudPinHash" else "nourPinHash"
                db.collection("sanctuaries").document(sanctuaryId).update(
                    mapOf(
                        pinField to pinHash,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Upsert user profile
            docPath = "users/$uid"
            val existingUser = db.collection("users").document(uid).get().await()
            if (existingUser.exists()) {
                db.collection("users").document(uid).update(
                    mapOf(
                        "displayName" to role.arabicName,
                        "activeRole" to role.key,
                        "sanctuaryId" to sanctuaryId,
                        "darkModeEnabled" to darkModeEnabled,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                db.collection("users").document(uid).set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to role.arabicName,
                        "activeRole" to role.key,
                        "sanctuaryId" to sanctuaryId,
                        "darkModeEnabled" to darkModeEnabled,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(sanctuary)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateRolePinAndProfile(
        sanctuaryId: String,
        role: CompanionRole,
        pinHash: String,
        shouldSavePin: Boolean,
        darkModeEnabled: Boolean
    ): Result<Unit> {
        var docPath = "sanctuaries/$sanctuaryId"
        return try {
            val uid = requireUserId()
            if (shouldSavePin) {
                val pinField = if (role == CompanionRole.MAHMOUD) "mahmoudPinHash" else "nourPinHash"
                db.collection("sanctuaries").document(sanctuaryId).update(
                    mapOf(
                        pinField to pinHash,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            docPath = "users/$uid"
            val userDoc = db.collection("users").document(uid).get().await()
            if (userDoc.exists()) {
                db.collection("users").document(uid).update(
                    mapOf(
                        "displayName" to role.arabicName,
                        "activeRole" to role.key,
                        "sanctuaryId" to sanctuaryId,
                        "darkModeEnabled" to darkModeEnabled,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                db.collection("users").document(uid).set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to role.arabicName,
                        "activeRole" to role.key,
                        "sanctuaryId" to sanctuaryId,
                        "darkModeEnabled" to darkModeEnabled,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateDarkModeInProfile(darkModeEnabled: Boolean): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            docPath = "users/$uid"
            val userDoc = db.collection("users").document(uid).get().await()
            if (userDoc.exists()) {
                db.collection("users").document(uid).update(
                    mapOf(
                        "darkModeEnabled" to darkModeEnabled,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    /**
     * When a second partner joins the sanctuary, ensure all existing documents carry both memberUids.
     */
    suspend fun syncMemberUidsForSanctuary(sanctuary: Sanctuary): Result<Unit> {
        if (sanctuary.memberUids.size < 2) return Result.success(Unit)
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        val collections = listOf("prayers_daily", "dhikr_counts", "duas", "shared_goals", "notifications_queue")
        return try {
            for (col in collections) {
                val snap = db.collection(col)
                    .whereArrayContains("memberUids", uid)
                    .get()
                    .await()
                for (doc in snap.documents) {
                    if (doc.getString("sanctuaryId") != sanctuary.id) continue
                    val currentMembers = (doc.get("memberUids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    if (!currentMembers.containsAll(sanctuary.memberUids)) {
                        doc.reference.update(
                            mapOf(
                                "memberUids" to sanctuary.memberUids,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "sanctuaries/${sanctuary.id}")
            Result.failure(e)
        }
    }

    // --- PRAYERS ---

    suspend fun togglePrayer(
        sanctuary: Sanctuary,
        role: CompanionRole,
        prayer: PrayerType,
        newCompleted: Boolean,
        existingToday: PrayerDaily?,
        dateKey: String = DateKeys.todayKey()
    ): Result<Unit> {
        val docId = "${sanctuary.id}_$dateKey"
        val docPath = "prayers_daily/$docId"
        return try {
            val uid = requireUserId()
            val fieldName = prayerFieldName(role, prayer)
            val docRef = db.collection("prayers_daily").document(docId)

            if (existingToday == null) {
                val initial = mutableMapOf<String, Any>(
                    "sanctuaryId" to sanctuary.id,
                    "userId" to uid,
                    "memberUids" to sanctuary.memberUids,
                    "dateKey" to dateKey,
                    "mahmoudFajr" to false,
                    "mahmoudDhuhr" to false,
                    "mahmoudAsr" to false,
                    "mahmoudMaghrib" to false,
                    "mahmoudIsha" to false,
                    "nourFajr" to false,
                    "nourDhuhr" to false,
                    "nourAsr" to false,
                    "nourMaghrib" to false,
                    "nourIsha" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                initial[fieldName] = newCompleted
                docRef.set(initial).await()
            } else {
                docRef.update(
                    mapOf(
                        fieldName to newCompleted,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            if (newCompleted) {
                enqueueNotification(
                    sanctuary = sanctuary,
                    sender = role,
                    type = "prayer",
                    title = "تقبل الله الصلاة 🕌",
                    body = SpiritualContent.prayerNotificationBody(role, prayer)
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    private fun prayerFieldName(role: CompanionRole, prayer: PrayerType): String {
        val prefix = if (role == CompanionRole.MAHMOUD) "mahmoud" else "nour"
        val suffix = when (prayer) {
            PrayerType.FAJR -> "Fajr"
            PrayerType.DHUHR -> "Dhuhr"
            PrayerType.ASR -> "Asr"
            PrayerType.MAGHRIB -> "Maghrib"
            PrayerType.ISHA -> "Isha"
        }
        return "$prefix$suffix"
    }

    // --- DHIKR COUNTERS & 500-MILESTONE NOTIFICATIONS ---

    suspend fun incrementDhikr(
        sanctuary: Sanctuary,
        role: CompanionRole,
        dhikr: DhikrType,
        existingCount: DhikrCount?,
        currentGoal: SharedGoal?,
        delta: Int = 1
    ): Result<Int?> {
        val docId = "${sanctuary.id}_${role.key}_${dhikr.key}"
        val docPath = "dhikr_counts/$docId"
        return try {
            val uid = requireUserId()
            require(delta in 1..1000) { "قيمة الزيادة غير صالحة." }
            val todayKey = DateKeys.todayKey()
            val weekKey = DateKeys.currentWeekKey()
            val docRef = db.collection("dhikr_counts").document(docId)

            val prevDaily = if (existingCount?.dailyDateKey == todayKey) existingCount.dailyCount else 0L
            val prevWeekly = if (existingCount?.weeklyKey == weekKey) existingCount.weeklyCount else 0L
            val prevLifetime = existingCount?.lifetimeCount ?: 0L
            val prevMilestone = if (existingCount?.dailyDateKey == todayKey) {
                existingCount.lastMilestoneNotified
            } else 0L

            val newDaily = prevDaily + delta
            val newWeekly = prevWeekly + delta
            val newLifetime = prevLifetime + delta
            val reachedMilestoneBucket = (newDaily / 500L) * 500L
            val triggeredMilestone: Int? = if (reachedMilestoneBucket > prevMilestone && reachedMilestoneBucket >= 500L) {
                reachedMilestoneBucket.toInt()
            } else null
            val newLastMilestone = triggeredMilestone?.toLong() ?: prevMilestone

            if (existingCount == null) {
                docRef.set(
                    mapOf(
                        "sanctuaryId" to sanctuary.id,
                        "userId" to uid,
                        "memberUids" to sanctuary.memberUids,
                        "role" to role.key,
                        "dhikrKey" to dhikr.key,
                        "dailyCount" to newDaily,
                        "dailyDateKey" to todayKey,
                        "weeklyCount" to newWeekly,
                        "weeklyKey" to weekKey,
                        "lifetimeCount" to newLifetime,
                        "lastMilestoneNotified" to newLastMilestone,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "dailyCount" to newDaily,
                        "dailyDateKey" to todayKey,
                        "weeklyCount" to newWeekly,
                        "weeklyKey" to weekKey,
                        "lifetimeCount" to newLifetime,
                        "lastMilestoneNotified" to newLastMilestone,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Contribute automatically to Shared Goal if matching dhikrKey or "all"
            if (currentGoal != null && (currentGoal.dhikrKey == "all" || currentGoal.dhikrKey == dhikr.key)) {
                val goalField = if (role == CompanionRole.MAHMOUD) "mahmoudProgress" else "nourProgress"
                val newGoalProgress = (if (role == CompanionRole.MAHMOUD) currentGoal.mahmoudProgress else currentGoal.nourProgress) + delta
                db.collection("shared_goals").document(currentGoal.id).update(
                    mapOf(
                        goalField to newGoalProgress.coerceAtMost(10_000_000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Trigger 500-milestone motivational partner notification
            if (triggeredMilestone != null) {
                enqueueNotification(
                    sanctuary = sanctuary,
                    sender = role,
                    type = "dhikr_milestone",
                    title = "إنجاز جديد في الذكر ✨",
                    body = SpiritualContent.milestoneNotificationBody(role, dhikr, triggeredMilestone)
                )
            }

            Result.success(triggeredMilestone)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun resetDailyDhikr(sanctuary: Sanctuary, role: CompanionRole, dhikr: DhikrType): Result<Unit> {
        val docId = "${sanctuary.id}_${role.key}_${dhikr.key}"
        val docPath = "dhikr_counts/$docId"
        return try {
            requireUserId()
            db.collection("dhikr_counts").document(docId).update(
                mapOf(
                    "dailyCount" to 0L,
                    "dailyDateKey" to DateKeys.todayKey(),
                    "lastMilestoneNotified" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- SHARED DUAS ---

    suspend fun sendDua(
        sanctuary: Sanctuary,
        sender: CompanionRole,
        content: String,
        tag: DuaTag
    ): Result<Unit> {
        val trimmed = content.trim()
        val docRef = db.collection("duas").document()
        return try {
            val uid = requireUserId()
            require(trimmed.isNotEmpty() && trimmed.length <= 1000) {
                "يرجى كتابة دعاء بين حرف واحد و1000 حرف."
            }
            docRef.set(
                mapOf(
                    "sanctuaryId" to sanctuary.id,
                    "userId" to uid,
                    "memberUids" to sanctuary.memberUids,
                    "senderRole" to sender.key,
                    "content" to trimmed,
                    "tag" to tag.key,
                    "ameenByMahmoud" to (sender == CompanionRole.MAHMOUD),
                    "ameenByNour" to (sender == CompanionRole.NOUR),
                    "pinned" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            enqueueNotification(
                sanctuary = sanctuary,
                sender = sender,
                type = "dua",
                title = "دعوة جديدة 🤲🤍",
                body = SpiritualContent.newDuaNotificationBody(sender)
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleDuaAmeen(
        sanctuary: Sanctuary,
        dua: SharedDua,
        role: CompanionRole
    ): Result<Unit> {
        val docPath = "duas/${dua.id}"
        return try {
            requireUserId()
            require(dua.id.isNotBlank()) { "معرف الدعاء غير صالح." }
            val field = if (role == CompanionRole.MAHMOUD) "ameenByMahmoud" else "ameenByNour"
            val newValue = !dua.hasAmeenBy(role)
            db.collection("duas").document(dua.id).update(
                mapOf(
                    field to newValue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (newValue && dua.senderRole != role.key) {
                enqueueNotification(
                    sanctuary = sanctuary,
                    sender = role,
                    type = "ameen",
                    title = "آمين 🤍",
                    body = SpiritualContent.ameenNotificationBody(role)
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleDuaPin(dua: SharedDua): Result<Unit> {
        val docPath = "duas/${dua.id}"
        return try {
            requireUserId()
            require(dua.id.isNotBlank()) { "معرف الدعاء غير صالح." }
            db.collection("duas").document(dua.id).update(
                mapOf(
                    "pinned" to !dua.pinned,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteDua(duaId: String): Result<Unit> {
        val docPath = "duas/$duaId"
        return try {
            requireUserId()
            require(duaId.isNotBlank()) { "معرف الدعاء غير صالح." }
            db.collection("duas").document(duaId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- SHARED GOAL & MORNING/EVENING ADHKAR CHECKLIST ---

    suspend fun saveSharedGoalConfig(
        sanctuary: Sanctuary,
        existingGoal: SharedGoal?,
        title: String,
        dhikrKey: String,
        period: String,
        targetCount: Long,
        resetProgress: Boolean
    ): Result<Unit> {
        val goalId = existingGoal?.id?.ifBlank { "${sanctuary.id}_main" } ?: "${sanctuary.id}_main"
        val docPath = "shared_goals/$goalId"
        return try {
            val uid = requireUserId()
            val trimmedTitle = title.trim()
            require(trimmedTitle.isNotEmpty() && trimmedTitle.length <= 120) { "عنوان الهدف غير صالح." }
            require(targetCount in 100L..10_000_000L) { "يجب أن يكون الهدف بين 100 و 10,000,000." }

            if (existingGoal == null) {
                db.collection("shared_goals").document(goalId).set(
                    mapOf(
                        "sanctuaryId" to sanctuary.id,
                        "userId" to uid,
                        "memberUids" to sanctuary.memberUids,
                        "title" to trimmedTitle,
                        "dhikrKey" to dhikrKey,
                        "period" to period,
                        "targetCount" to targetCount,
                        "mahmoudProgress" to 0L,
                        "nourProgress" to 0L,
                        "adhkarDateKey" to DateKeys.todayKey(),
                        "mahmoudMorningAdhkar" to emptyList<String>(),
                        "mahmoudEveningAdhkar" to emptyList<String>(),
                        "nourMorningAdhkar" to emptyList<String>(),
                        "nourEveningAdhkar" to emptyList<String>(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "title" to trimmedTitle,
                    "dhikrKey" to dhikrKey,
                    "period" to period,
                    "targetCount" to targetCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (resetProgress) {
                    updates["mahmoudProgress"] = 0L
                    updates["nourProgress"] = 0L
                }
                db.collection("shared_goals").document(goalId).update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleAdhkarChecklistItem(
        sanctuary: Sanctuary,
        existingGoal: SharedGoal?,
        role: CompanionRole,
        isMorning: Boolean,
        itemId: String
    ): Result<Unit> {
        val goalId = existingGoal?.id?.ifBlank { "${sanctuary.id}_main" } ?: "${sanctuary.id}_main"
        val docPath = "shared_goals/$goalId"
        return try {
            val uid = requireUserId()
            val todayKey = DateKeys.todayKey()
            val sameDay = existingGoal?.adhkarDateKey == todayKey

            val mMorning = if (sameDay) existingGoal?.mahmoudMorningAdhkar.orEmpty() else emptyList()
            val mEvening = if (sameDay) existingGoal?.mahmoudEveningAdhkar.orEmpty() else emptyList()
            val nMorning = if (sameDay) existingGoal?.nourMorningAdhkar.orEmpty() else emptyList()
            val nEvening = if (sameDay) existingGoal?.nourEveningAdhkar.orEmpty() else emptyList()

            fun toggle(list: List<String>): List<String> =
                if (itemId in list) list - itemId else (list + itemId).take(20)

            val newMMorning = if (role == CompanionRole.MAHMOUD && isMorning) toggle(mMorning) else mMorning
            val newMEvening = if (role == CompanionRole.MAHMOUD && !isMorning) toggle(mEvening) else mEvening
            val newNMorning = if (role == CompanionRole.NOUR && isMorning) toggle(nMorning) else nMorning
            val newNEvening = if (role == CompanionRole.NOUR && !isMorning) toggle(nEvening) else nEvening

            if (existingGoal == null) {
                db.collection("shared_goals").document(goalId).set(
                    mapOf(
                        "sanctuaryId" to sanctuary.id,
                        "userId" to uid,
                        "memberUids" to sanctuary.memberUids,
                        "title" to "10,000 استغفار للرزق والزواج في الحلال",
                        "dhikrKey" to DhikrType.ISTIGHFAR.key,
                        "period" to "monthly",
                        "targetCount" to 10000L,
                        "mahmoudProgress" to 0L,
                        "nourProgress" to 0L,
                        "adhkarDateKey" to todayKey,
                        "mahmoudMorningAdhkar" to newMMorning,
                        "mahmoudEveningAdhkar" to newMEvening,
                        "nourMorningAdhkar" to newNMorning,
                        "nourEveningAdhkar" to newNEvening,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                db.collection("shared_goals").document(goalId).update(
                    mapOf(
                        "adhkarDateKey" to todayKey,
                        "mahmoudMorningAdhkar" to newMMorning,
                        "mahmoudEveningAdhkar" to newMEvening,
                        "nourMorningAdhkar" to newNMorning,
                        "nourEveningAdhkar" to newNEvening,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    // --- NOTIFICATIONS QUEUE & SPIRITUAL NUDGES ---

    suspend fun sendSpiritualNudge(
        sanctuary: Sanctuary,
        sender: CompanionRole,
        nudge: SpiritualNudge
    ): Result<Unit> {
        val body = "${sender.arabicName}: ${nudge.label}"
        return enqueueNotification(
            sanctuary = sanctuary,
            sender = sender,
            type = "nudge",
            title = "تذكير بالخير 🌿",
            body = body
        )
    }

    suspend fun enqueueNotification(
        sanctuary: Sanctuary,
        sender: CompanionRole,
        type: String,
        title: String,
        body: String
    ): Result<Unit> {
        val docRef = db.collection("notifications_queue").document()
        return try {
            val uid = requireUserId()
            docRef.set(
                mapOf(
                    "sanctuaryId" to sanctuary.id,
                    "userId" to uid,
                    "memberUids" to sanctuary.memberUids,
                    "senderRole" to sender.key,
                    "recipientRole" to sender.partner().key,
                    "type" to type,
                    "title" to title.take(120),
                    "body" to body.take(400),
                    "delivered" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationDelivered(notifId: String): Result<Unit> {
        val docPath = "notifications_queue/$notifId"
        return try {
            requireUserId()
            if (notifId.isBlank()) return Result.success(Unit)
            db.collection("notifications_queue").document(notifId).update(
                mapOf(
                    "delivered" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    private fun logStreamErrorIfActive(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
