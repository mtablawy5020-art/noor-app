package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class CompanionRole(
    val key: String,
    val arabicName: String,
    val partnerArabicName: String,
    val isMale: Boolean,
    val expectedPasswordLength: Int
) {
    MAHMOUD(
        key = "mahmoud",
        arabicName = "محمود",
        partnerArabicName = "نور",
        isMale = true,
        expectedPasswordLength = 6
    ),
    NOUR(
        key = "nour",
        arabicName = "نور",
        partnerArabicName = "محمود",
        isMale = false,
        expectedPasswordLength = 8
    );

    fun partner(): CompanionRole = if (this == MAHMOUD) NOUR else MAHMOUD

    companion object {
        fun fromKey(raw: String?): CompanionRole? =
            entries.firstOrNull { it.key == raw }
    }
}

enum class PrayerType(
    val key: String,
    val arabicName: String,
    val subtitle: String
) {
    FAJR("fajr", "الفجر", "بداية يوم مبارك برضى الله"),
    DHUHR("dhuhr", "الظهر", "سكينة في منتصف اليوم"),
    ASR("asr", "العصر", "الصلاة الوسطى وبركة الوقت"),
    MAGHRIB("maghrib", "المغرب", "دعاء مستجاب عند الغروب"),
    ISHA("isha", "العشاء", "ختام اليوم بطمأنينة القلب")
}

enum class DhikrType(
    val key: String,
    val shortTitle: String,
    val fullText: String,
    val virtue: String,
    val malePastVerb: String,
    val femalePastVerb: String,
    val maleImperative: String,
    val femaleImperative: String,
    val partnerSaidMale: String,
    val partnerSaidFemale: String
) {
    TASBEEH(
        key = "tasbeeh",
        shortTitle = "تسبيح",
        fullText = "سبحان الله",
        virtue = "تُغرَس بها نخلة في الجنة وتُثقِّل الميزان",
        malePastVerb = "سبّح",
        femalePastVerb = "سبّحت",
        maleImperative = "قوم وسبّح",
        femaleImperative = "قومي وسبّحي",
        partnerSaidMale = "محمود قال التسبيح",
        partnerSaidFemale = "نور قالت التسبيح"
    ),
    ISTIGHFAR(
        key = "istighfar",
        shortTitle = "استغفار",
        fullText = "أستغفر الله العظيم وأتوب إليه",
        virtue = "مفتاح الرزق الواسع والفرج والتيسير في الحلال",
        malePastVerb = "استغفر",
        femalePastVerb = "استغفرت",
        maleImperative = "قوم واستغفر",
        femaleImperative = "قومي واستغفري",
        partnerSaidMale = "محمود قال الاستغفار",
        partnerSaidFemale = "نور قالت الاستغفار"
    ),
    TAHMID(
        key = "tahmid",
        shortTitle = "تحميد",
        fullText = "الحمد لله",
        virtue = "تملأ الميزان وتزيد النعم والبركة",
        malePastVerb = "حمِد الله",
        femalePastVerb = "حمِدت الله",
        maleImperative = "قوم واحمد الله",
        femaleImperative = "قومي واحمدي الله",
        partnerSaidMale = "محمود قال التحميد",
        partnerSaidFemale = "نور قالت التحميد"
    ),
    HAWQALA(
        key = "hawqala",
        shortTitle = "حوقلة",
        fullText = "لا حول ولا قوة إلا بالله",
        virtue = "كنز من كنوز الجنة وفرج لكل هم وضيق",
        malePastVerb = "حوقل",
        femalePastVerb = "حوقلت",
        maleImperative = "قوم وحوقل",
        femaleImperative = "قومي وحوقلي",
        partnerSaidMale = "محمود قال الحوقلة",
        partnerSaidFemale = "نور قالت الحوقلة"
    ),
    SALAWAT(
        key = "salawat",
        shortTitle = "صلاة على النبي",
        fullText = "اللهم صلِّ وسلم على نبينا محمد",
        virtue = "تُكفى بها الهموم وتُغفر بها الذنوب وتُقضى الحوائج",
        malePastVerb = "صلّى على النبي ﷺ",
        femalePastVerb = "صلّت على النبي ﷺ",
        maleImperative = "قوم وصلِّ على النبي ﷺ",
        femaleImperative = "قومي وصلّي على النبي ﷺ",
        partnerSaidMale = "محمود صلّى على النبي ﷺ",
        partnerSaidFemale = "نور صلّت على النبي ﷺ"
    );

    companion object {
        fun fromKey(raw: String?): DhikrType =
            entries.firstOrNull { it.key == raw } ?: ISTIGHFAR
    }
}

enum class DuaTag(val key: String, val arabicLabel: String) {
    RIZQ("rizq", "للرزق"),
    MARRIAGE("marriage", "للزواج والتيسير"),
    PEACE("peace", "لراحة البال"),
    FOR_YOU("for_you", "دعاء لك / لكِ"),
    GENERAL("general", "دعاء عام");

    companion object {
        fun fromKey(raw: String?): DuaTag =
            entries.firstOrNull { it.key == raw } ?: GENERAL
    }
}

enum class SpiritualNudge(val id: String, val label: String, val subtitle: String) {
    PRAY_FOR_ME("pray_for_me", "ادعيلي دلوقتي 🤲", "تذكير لطيف بطلب الدعاء"),
    SALAWAT("salawat_nudge", "صلِّ على النبي ﷺ 🤍", "تذكير بالصلاة على الحبيب"),
    ADHKAR_TIME("adhkar_time", "وقت أذكار الصباح / المساء 🌙", "حصن المسلم وراحة القلب"),
    RIZQ_HALAL("rizq_halal", "ربنا يرزقنا ويجمعنا على خير 🌿", "دعوة صادقة بالرزق والحلال")
}

data class AdhkarChecklistItem(
    val id: String,
    val title: String,
    val text: String,
    val countLabel: String
)

object SpiritualContent {
    val dailyVersesAndDuas: List<Pair<String, String>> = listOf(
        "﴿ وَمَن يَتَّقِ اللَّهَ يَجْعَل لَّهُ مَخْرَجًا ۝ وَيَرْزُقْهُ مِنْ حَيْثُ لَا يَحْتَسِبُ ﴾" to "سورة الطلاق",
        "﴿ رَبِّ إِنِّي لِمَا أَنزَلْتَ إِلَيَّ مِنْ خَيْرٍ فَقِيرٌ ﴾" to "دعاء نبي الله موسى للرزق والزواج",
        "﴿ رَبَّنَا هَبْ لَنَا مِنْ أَزْوَاجِنَا وَذُرِّيَّاتِنَا قُرَّةَ أَعْيُنٍ ﴾" to "سورة الفرقان",
        "﴿ فَقُلْتُ اسْتَغْفِرُوا رَبَّكُمْ إِنَّهُ كَانَ غَفَّارًا ۝ يُرْسِلِ السَّمَاءَ عَلَيْكُم مِّدْرَارًا ﴾" to "سورة نوح",
        "اللهم اكفنا بحلالك عن حرامك، وأغننا بفضلك عمّن سواك، ويسّر لنا كل عسير" to "دعاء الرزق والتيسير",
        "﴿ وَمَا تَوْفِيقِي إِلَّا بِاللَّهِ ۚ عَلَيْهِ تَوَكَّلْتُ وَإِلَيْهِ أُنِيبُ ﴾" to "سورة هود",
        "اللهم ألّف بين قلوبنا، وبارك لنا في أرزاقنا، واجمعنا في الحلال على طاعتك" to "دعاء طريق الحلال"
    )

    val morningAdhkar: List<AdhkarChecklistItem> = listOf(
        AdhkarChecklistItem("m_kursi", "آية الكرسي", "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ...", "مرة واحدة"),
        AdhkarChecklistItem("m_muawwidhat", "الإخلاص والمعوذتان", "قراءة سورة الإخلاص والفلق والناس", "3 مرات"),
        AdhkarChecklistItem("m_asbahna", "دعاء الصباح", "أصبحنا وأصبح الملك لله، والحمد لله، لا إله إلا الله وحده لا شريك له", "مرة واحدة"),
        AdhkarChecklistItem("m_sayyid", "سيد الاستغفار", "اللهم أنت ربي لا إله إلا أنت خلقتني وأنا عبدك...", "مرة واحدة"),
        AdhkarChecklistItem("m_rizq", "دعاء الرزق والعلم", "اللهم إني أسألك علماً نافعاً، ورزقاً طيباً، وعملاً متقبلاً", "مرة واحدة"),
        AdhkarChecklistItem("m_bismillah", "التحصين", "بسم الله الذي لا يضر مع اسمه شيء في الأرض ولا في السماء", "3 مرات")
    )

    val eveningAdhkar: List<AdhkarChecklistItem> = listOf(
        AdhkarChecklistItem("e_kursi", "آية الكرسي", "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ...", "مرة واحدة"),
        AdhkarChecklistItem("e_muawwidhat", "الإخلاص والمعوذتان", "قراءة سورة الإخلاص والفلق والناس", "3 مرات"),
        AdhkarChecklistItem("e_amsayna", "دعاء المساء", "أمسينا وأمسى الملك لله، والحمد لله، لا إله إلا الله وحده لا شريك له", "مرة واحدة"),
        AdhkarChecklistItem("e_sayyid", "سيد الاستغفار", "اللهم أنت ربي لا إله إلا أنت خلقتني وأنا عبدك...", "مرة واحدة"),
        AdhkarChecklistItem("e_bismillah", "التحصين", "بسم الله الذي لا يضر مع اسمه شيء في الأرض ولا في السماء", "3 مرات"),
        AdhkarChecklistItem("e_kalimat", "أعوذ بكلمات الله التامات", "أعوذ بكلمات الله التامات من شر ما خلق", "3 مرات")
    )

    fun dailyVerseForToday(): Pair<String, String> {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return dailyVersesAndDuas[dayOfYear % dailyVersesAndDuas.size]
    }

    fun timeGreeting(role: CompanionRole): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val prefix = when (hour) {
            in 4..11 -> "صباح الخير يا"
            in 12..16 -> "أهلاً يا"
            in 17..21 -> "مساء الخير يا"
            else -> "طابت ليلتك يا"
        }
        return "$prefix ${role.arabicName} 🤍"
    }

    fun prayerNotificationBody(sender: CompanionRole, prayer: PrayerType): String {
        return if (sender == CompanionRole.MAHMOUD) {
            "محمود صلى صلاة ${prayer.arabicName} 🕌 تقبل الله، الدور عليكِ يا نور 🤍"
        } else {
            "نور صلت صلاة ${prayer.arabicName} 🕌 تقبل الله، الدور عليك يا محمود 🤍"
        }
    }

    fun milestoneNotificationBody(
        sender: CompanionRole,
        dhikr: DhikrType,
        milestone: Int
    ): String {
        return if (sender == CompanionRole.MAHMOUD) {
            "محمود ${dhikr.malePastVerb} $milestone مرة ✨ ${dhikr.femaleImperative} خلي ربنا يوفقكم ويرزقكم ويجمعكم في الحلال 🤍"
        } else {
            "نور ${dhikr.femalePastVerb} $milestone مرة ✨ ${dhikr.maleImperative} خلي ربنا يوفقكم ويرزقكم ويجمعكم في الحلال 🤍"
        }
    }

    fun newDuaNotificationBody(sender: CompanionRole): String {
        return "وصلتك دعوة جديدة من ${sender.arabicName} 🤲🤍"
    }

    fun ameenNotificationBody(sender: CompanionRole): String {
        return if (sender == CompanionRole.MAHMOUD) {
            "محمود أمّن على دعوتك: آمين يا رب العالمين 🤍"
        } else {
            "نور أمّنت على دعوتك: آمين يا رب العالمين 🤍"
        }
    }
}

object DateKeys {
    fun todayKey(date: Date = Date()): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(date)
    }

    fun currentWeekKey(date: Date = Date()): String {
        val cal = Calendar.getInstance().apply { time = date }
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        return String.format(Locale.US, "%04d-W%02d", year, week)
    }

    fun lastNDaysKeys(days: Int = 7): List<String> {
        val cal = Calendar.getInstance()
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return (days - 1 downTo 0).map { offset ->
            val c = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
            fmt.format(c.time)
        }
    }

    fun arabicShortDay(dateKey: String): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val parsed = runCatching { fmt.parse(dateKey) }.getOrNull() ?: return dateKey.takeLast(2)
        val cal = Calendar.getInstance().apply { time = parsed }
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> "سبت"
            Calendar.SUNDAY -> "أحد"
            Calendar.MONDAY -> "إثنين"
            Calendar.TUESDAY -> "ثلاثاء"
            Calendar.WEDNESDAY -> "أربعاء"
            Calendar.THURSDAY -> "خميس"
            Calendar.FRIDAY -> "جمعة"
            else -> dateKey.takeLast(2)
        }
    }
}

object PinSecurity {
    fun sha256(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("tariq_halal_v1:$pin".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun defaultHashFor(role: CompanionRole): String = when (role) {
        CompanionRole.MAHMOUD -> sha256("122007")
        CompanionRole.NOUR -> sha256("29102002")
    }

    fun generateInviteCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { alphabet.random() }.joinToString("")
    }
}

// --- FIRESTORE DATA MODELS (Strict 1:1 parity with firestore.rules & firebase-blueprint.json) ---

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val activeRole: String = "mahmoud",
    val sanctuaryId: String = "",
    val darkModeEnabled: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Sanctuary(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val inviteCode: String = "",
    val mahmoudPinHash: String = "",
    val nourPinHash: String = "",
    val mahmoudStreak: Long = 0L,
    val nourStreak: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PrayerDaily(
    @DocumentId val id: String = "",
    val sanctuaryId: String = "",
    val userId: String = "",
    val memberUids: List<String> = emptyList(),
    val dateKey: String = "",
    val mahmoudFajr: Boolean = false,
    val mahmoudDhuhr: Boolean = false,
    val mahmoudAsr: Boolean = false,
    val mahmoudMaghrib: Boolean = false,
    val mahmoudIsha: Boolean = false,
    val nourFajr: Boolean = false,
    val nourDhuhr: Boolean = false,
    val nourAsr: Boolean = false,
    val nourMaghrib: Boolean = false,
    val nourIsha: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isCompleted(role: CompanionRole, prayer: PrayerType): Boolean = when (role) {
        CompanionRole.MAHMOUD -> when (prayer) {
            PrayerType.FAJR -> mahmoudFajr
            PrayerType.DHUHR -> mahmoudDhuhr
            PrayerType.ASR -> mahmoudAsr
            PrayerType.MAGHRIB -> mahmoudMaghrib
            PrayerType.ISHA -> mahmoudIsha
        }
        CompanionRole.NOUR -> when (prayer) {
            PrayerType.FAJR -> nourFajr
            PrayerType.DHUHR -> nourDhuhr
            PrayerType.ASR -> nourAsr
            PrayerType.MAGHRIB -> nourMaghrib
            PrayerType.ISHA -> nourIsha
        }
    }

    fun completedCount(role: CompanionRole): Int =
        PrayerType.entries.count { isCompleted(role, it) }
}

@Keep
@IgnoreExtraProperties
data class DhikrCount(
    @DocumentId val id: String = "",
    val sanctuaryId: String = "",
    val userId: String = "",
    val memberUids: List<String> = emptyList(),
    val role: String = "mahmoud",
    val dhikrKey: String = "istighfar",
    val dailyCount: Long = 0L,
    val dailyDateKey: String = "",
    val weeklyCount: Long = 0L,
    val weeklyKey: String = "",
    val lifetimeCount: Long = 0L,
    val lastMilestoneNotified: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun effectiveDailyCount(todayKey: String): Int =
        if (dailyDateKey == todayKey) dailyCount.toInt() else 0

    fun effectiveWeeklyCount(weekKey: String): Int =
        if (weeklyKey == weekKey) weeklyCount.toInt() else 0
}

@Keep
@IgnoreExtraProperties
data class SharedDua(
    @DocumentId val id: String = "",
    val sanctuaryId: String = "",
    val userId: String = "",
    val memberUids: List<String> = emptyList(),
    val senderRole: String = "mahmoud",
    val content: String = "",
    val tag: String = "general",
    val ameenByMahmoud: Boolean = false,
    val ameenByNour: Boolean = false,
    val pinned: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun hasAmeenBy(role: CompanionRole): Boolean =
        if (role == CompanionRole.MAHMOUD) ameenByMahmoud else ameenByNour
}

@Keep
@IgnoreExtraProperties
data class SharedGoal(
    @DocumentId val id: String = "",
    val sanctuaryId: String = "",
    val userId: String = "",
    val memberUids: List<String> = emptyList(),
    val title: String = "10,000 استغفار للرزق والزواج في الحلال",
    val dhikrKey: String = "istighfar",
    val period: String = "monthly",
    val targetCount: Long = 10000L,
    val mahmoudProgress: Long = 0L,
    val nourProgress: Long = 0L,
    val adhkarDateKey: String = "",
    val mahmoudMorningAdhkar: List<String> = emptyList(),
    val mahmoudEveningAdhkar: List<String> = emptyList(),
    val nourMorningAdhkar: List<String> = emptyList(),
    val nourEveningAdhkar: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class NotificationQueueItem(
    @DocumentId val id: String = "",
    val sanctuaryId: String = "",
    val userId: String = "",
    val memberUids: List<String> = emptyList(),
    val senderRole: String = "mahmoud",
    val recipientRole: String = "nour",
    val type: String = "nudge",
    val title: String = "",
    val body: String = "",
    val delivered: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
