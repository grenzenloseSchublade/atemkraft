package app.atemkraft.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.atemkraft.domain.FeelingPrompt
import app.atemkraft.domain.GONG_INTERVAL_DEFAULT_MINUTES
import app.atemkraft.domain.GONG_INTERVAL_MAX_MINUTES
import app.atemkraft.domain.GONG_INTERVAL_MIN_MINUTES
import app.atemkraft.domain.MEDITATION_MAX_MINUTES
import app.atemkraft.domain.MEDITATION_MIN_MINUTES
import app.atemkraft.domain.MeditationConfig
import app.atemkraft.domain.MeditationMode
import app.atemkraft.domain.SoundMode
import app.atemkraft.domain.ToneVolume
import app.atemkraft.domain.TransitionEmphasis
import app.atemkraft.domain.VoiceCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.DayOfWeek

/** Uhrzeit der Erinnerung im Raster [ReminderSettings.STEP_MINUTES] und innerhalb eines Tages. */
internal fun reminderMinute(minuteOfDay: Int): Int {
    val step = ReminderSettings.STEP_MINUTES
    return (minuteOfDay / step * step).coerceIn(0, ReminderSettings.MINUTES_PER_DAY - step)
}

/**
 * Zeitpunkt (Epoch-ms), zu dem eine beschädigte Einstellungsdatei durch eine leere ersetzt wurde.
 * Ohne Cloud-Backup gibt es keine Rettung der alten Werte; die Marke hält den Vorfall wenigstens
 * fest – ohne Logcat (SEC-PRIV-03), abrufbar über [SettingsRepository.resetAfterCorruptionAt].
 */
private val CORRUPTION_RESET_AT = longPreferencesKey("settings_reset_at")

/** [CORRUPTION_RESET_AT] des Zurücksetzens, das die Einstellungen schon einmal gemeldet haben. */
private val CORRUPTION_RESET_SEEN = longPreferencesKey("settings_reset_seen_at")

/**
 * Ein Zurücksetzen ist zu melden, solange es noch nicht gemeldet wurde: Die Marke bleibt als
 * Vermerk stehen, gemeldet wird sie nur einmal (je beschädigter Datei).
 */
internal fun resetNoticePending(resetAt: Long?, seenAt: Long?): Boolean = resetAt != null && resetAt != seenAt

/**
 * Beschädigte settings.preferences_pb (CorruptionException): Statt bei jedem Start abzustürzen,
 * beginnt die App mit Standardwerten und vermerkt das ([CORRUPTION_RESET_AT]). Logbuch und
 * gespeicherte Muster liegen in Room und bleiben unberührt.
 */
internal val settingsCorruptionHandler = ReplaceFileCorruptionHandler<Preferences> {
    mutablePreferencesOf(CORRUPTION_RESET_AT to System.currentTimeMillis())
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    corruptionHandler = settingsCorruptionHandler,
)

/**
 * Enum über seinen Namen lesen (CODE-05). Bis 1.6.5 stand der ordinal-Index unter [legacy];
 * dieser Wert gilt weiter, bis die Einstellung das nächste Mal gesetzt wird. Unbekannt = null.
 */
private inline fun <reified E : Enum<E>> Preferences.enumOf(key: Preferences.Key<String>, legacy: Preferences.Key<Int>): E? {
    val byName = this[key]?.let { name -> enumValues<E>().firstOrNull { it.name == name } }
    return byName ?: this[legacy]?.let { enumValues<E>().getOrNull(it) }
}

/** Enum unter seinem Namen speichern und den alten ordinal-Key entfernen (siehe [enumOf]). */
private fun MutablePreferences.putEnum(key: Preferences.Key<String>, legacy: Preferences.Key<Int>, value: Enum<*>) {
    this[key] = value.name
    remove(legacy)
}

/**
 * Die App nutzt den einen DataStore „settings“ ([Context.dataStore]). Tests geben einen eigenen
 * Speicher mit, damit Hintergrund-Aufräumarbeiten der App (z. B. [dropUnknownNeuralVoice] beim
 * Start) nicht in ihre Daten schreiben.
 */
class SettingsRepository(private val store: DataStore<Preferences>) {

    constructor(context: Context) : this(context.dataStore)

    // Enums stehen unter ihrem Namen (…_name); die Int-Keys ohne Endung sind die ordinal-Werte
    // bis 1.6.5 und werden nur noch gelesen. Enum-Konstanten deshalb nie umbenennen.
    private object Keys {
        val SOUND = booleanPreferencesKey("cue_sound") // alt, nur für Migration
        val SOUND_MODE = stringPreferencesKey("sound_mode_name")
        val SOUND_MODE_ORDINAL = intPreferencesKey("sound_mode")
        val TRANSITION = stringPreferencesKey("transition_emphasis_name")
        val TRANSITION_ORDINAL = intPreferencesKey("transition_emphasis")
        val VOLUME = stringPreferencesKey("tone_volume_name")
        val VOLUME_ORDINAL = intPreferencesKey("tone_volume")
        val HAPTICS = booleanPreferencesKey("cue_haptics")
        val SHOW_SAFETY = booleanPreferencesKey("show_safety_warning")
        val SAFETY_ACK = booleanPreferencesKey("safety_acknowledged") // alt (bis 2.5): einmal für alle, wird entfernt
        val SAFETY_ACK_IDS = stringSetPreferencesKey("safety_acknowledged_ids") // je Übung bestätigt
        val SHOW_NEXT = booleanPreferencesKey("show_next_phase")
        val SHOW_PROGRESS = booleanPreferencesKey("show_session_progress")
        val SHOW_SECONDS = booleanPreferencesKey("show_session_seconds")
        val AUTO_CALM = booleanPreferencesKey("session_auto_calm")
        val SILENCE_AFTER = booleanPreferencesKey("session_silence_after")
        val MED_AUTO_CALM = booleanPreferencesKey("med_auto_calm")
        val MED_SHOW_PROGRESS = booleanPreferencesKey("med_show_progress")
        val MED_SHOW_TIME = booleanPreferencesKey("med_show_time")
        val MED_MODE = stringPreferencesKey("med_mode_name")
        val MED_MODE_ORDINAL = intPreferencesKey("med_mode")
        val MED_MINUTES = intPreferencesKey("med_minutes")
        val MED_START_END_GONG = booleanPreferencesKey("med_start_end_gong")
        val MED_INTERVAL_ON = booleanPreferencesKey("med_interval_on") // Tab-Wahl: Intervall an
        val MED_GONG_INTERVAL = intPreferencesKey("med_gong_interval") // Einstellung: X Minuten
        val MED_SPEECH = booleanPreferencesKey("med_speech")
        val MED_NEURAL_VOICE = stringPreferencesKey("med_neural_voice") // VoiceCatalog.id; leer = keine
        val MED_GONG_LONG = booleanPreferencesKey("med_gong_long") // Gong-Ausklang: lang (true)/kurz
        val MARKED_EXERCISES = stringSetPreferencesKey("marked_exercises") // Atemkreis „tut mir gut“
        val NOTIFICATION_ASKED = booleanPreferencesKey("notification_asked") // einmal gefragt (ab Android 13)
        val WELCOME_DISMISSED = booleanPreferencesKey("welcome_dismissed") // Willkommenskarte weggeklickt
        val SHOW_STREAK = booleanPreferencesKey("show_streak") // „Serie zeigen“ (Tage in Folge), Standard an
        val FEELING_PROMPT = booleanPreferencesKey("feeling_prompt") // „Befinden am Ende fragen“; fehlt = noch nie angeboten
        val REMINDER_ON = booleanPreferencesKey("reminder_enabled") // Erinnerung an/aus, Standard aus
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute") // Uhrzeit, Minuten nach Mitternacht
        val REMINDER_DAYS = stringSetPreferencesKey("reminder_days") // Wochentage als DayOfWeek-Namen
    }

    /**
     * Alle Flows lesen hierüber: Ein Lesefehler (IOException) liefert Standardwerte statt eines
     * Absturzes in der Composition; andere Fehler bleiben Fehler.
     */
    private val data: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    /** Wann die Einstellungen zuletzt wegen einer beschädigten Datei zurückgesetzt wurden; null = nie. */
    val resetAfterCorruptionAt: Flow<Long?> = data.map { it[CORRUPTION_RESET_AT] }

    /** true, solange das letzte Zurücksetzen noch nicht gemeldet ist ([acknowledgeSettingsReset]). */
    val settingsResetPending: Flow<Boolean> = data.map { resetNoticePending(it[CORRUPTION_RESET_AT], it[CORRUPTION_RESET_SEEN]) }

    /** Das Zurücksetzen ist gemeldet; die Marke selbst bleibt als Vermerk stehen. */
    suspend fun acknowledgeSettingsReset() {
        store.edit { prefs -> prefs[CORRUPTION_RESET_AT]?.let { prefs[CORRUPTION_RESET_SEEN] = it } }
    }

    /**
     * Übungs-Ids mit persönlicher Markierung („tut mir gut“, Atemkreis). Eine eigene Einschätzung
     * neben der Studienlage; sie ändert weder Reihenfolge noch Gruppen. Nur stabile Ids eingebauter
     * Übungen – das entscheidet der Aufrufer.
     */
    val markedExercises: Flow<Set<String>> = data.map { it[Keys.MARKED_EXERCISES].orEmpty() }

    suspend fun setExerciseMarked(exerciseId: String, marked: Boolean) {
        store.edit { prefs ->
            val current = prefs[Keys.MARKED_EXERCISES].orEmpty()
            prefs[Keys.MARKED_EXERCISES] = if (marked) current + exerciseId else current - exerciseId
        }
    }

    /**
     * Wurde die Benachrichtigungs-Erlaubnis (ab Android 13) schon einmal erfragt? Die App fragt
     * genau einmal, beim ersten Meditationsstart – nicht beim App-Start und nie wieder danach.
     */
    val notificationAsked: Flow<Boolean> = data.map { it[Keys.NOTIFICATION_ASKED] ?: false }

    suspend fun setNotificationAsked() {
        store.edit { it[Keys.NOTIFICATION_ASKED] = true }
    }

    /**
     * Wurde die Willkommenskarte im Atmen-Tab weggeklickt (Nutzer 2026-10-24)? Bis dahin steht sie
     * oben im Tab: Einstieg und Gesundheitshinweis vor der ersten Sitzung.
     */
    val welcomeDismissed: Flow<Boolean> = data.map { it[Keys.WELCOME_DISMISSED] ?: false }

    suspend fun dismissWelcome() {
        store.edit { it[Keys.WELCOME_DISMISSED] = true }
    }

    /**
     * „Serie zeigen“ (Auswertung 2026-10-24): aus = Verlauf und Logbuch nennen keine „Tage in
     * Folge“, nur Minuten und Sitzungen. Standard an; fehlt der Schlüssel, gilt der Standard – keine
     * Migration nötig.
     */
    val showStreak: Flow<Boolean> = data.map { it[Keys.SHOW_STREAK] ?: true }

    suspend fun setShowStreak(enabled: Boolean) {
        store.edit { it[Keys.SHOW_STREAK] = enabled }
    }

    /**
     * „Befinden am Ende fragen“ (SEC-LEGAL-04): fehlt der Schlüssel, wurde die Antwortreihe noch nie
     * angeboten ([FeelingPrompt.UNSET]) – dann erscheint sie beim nächsten regulären Ende einmal mit dem
     * Hinweis, dass man sie abschalten kann, und [feelingOffered] setzt den Schalter auf an.
     */
    val feelingPrompt: Flow<FeelingPrompt> = data.map { prefs ->
        when (prefs[Keys.FEELING_PROMPT]) {
            null -> FeelingPrompt.UNSET
            true -> FeelingPrompt.ON
            false -> FeelingPrompt.OFF
        }
    }

    suspend fun setFeelingPrompt(enabled: Boolean) {
        store.edit { it[Keys.FEELING_PROMPT] = enabled }
    }

    /** Die Reihe wurde zum ersten Mal angeboten: Ab jetzt gilt der Schalter, Standard an. */
    suspend fun feelingOffered() {
        store.edit { prefs -> if (prefs[Keys.FEELING_PROMPT] == null) prefs[Keys.FEELING_PROMPT] = true }
    }

    /**
     * Lokale Erinnerung ([ReminderSettings]): Standard aus. Uhrzeit auf das Raster gezogen, Tage unter
     * ihren Namen (CODE-05); unbekannte Namen fallen weg, ohne Tag gilt jeder Tag.
     */
    val reminderSettings: Flow<ReminderSettings> = data.map { prefs ->
        val defaults = ReminderSettings()
        val days = prefs[Keys.REMINDER_DAYS]?.mapNotNull { name -> DayOfWeek.entries.firstOrNull { it.name == name } }?.toSet()
        ReminderSettings(
            enabled = prefs[Keys.REMINDER_ON] ?: defaults.enabled,
            minuteOfDay = prefs[Keys.REMINDER_MINUTE]?.let(::reminderMinute) ?: defaults.minuteOfDay,
            days = days?.takeIf { it.isNotEmpty() } ?: defaults.days,
        )
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        store.edit { it[Keys.REMINDER_ON] = enabled }
    }

    suspend fun setReminderMinute(minuteOfDay: Int) {
        store.edit { it[Keys.REMINDER_MINUTE] = reminderMinute(minuteOfDay) }
    }

    /** Wochentage der Erinnerung; eine leere Menge wird nicht gespeichert (mindestens ein Tag). */
    suspend fun setReminderDays(days: Set<DayOfWeek>) {
        if (days.isEmpty()) return
        store.edit { prefs -> prefs[Keys.REMINDER_DAYS] = days.map { it.name }.toSet() }
    }

    /**
     * Einstellungen für die Sicherung ([SettingsBackup]): nur, was auf jedem Gerät gleich gilt, mit den
     * Werten, die gerade gelten (auch Standardwerte); dazu die Anpassungen der eingebauten Übungen
     * [builtInIds]. Nicht: Stimme, Erinnerung, bestätigte Sicherheitshinweise, Gerätezustände.
     */
    suspend fun exportSettings(builtInIds: Collection<String>): SettingsBackup {
        val prefs = data.first()
        val cue = cueSettings.first()
        val display = sessionDisplay.first()
        val med = meditationDisplay.first()
        val config = meditationSettings.first()
        return SettingsBackup(
            soundMode = cue.soundMode.name,
            transition = cue.transition.name,
            volume = cue.volume.name,
            haptics = cue.haptics,
            gongLong = cue.gongLong,
            showSafetyWarning = safetySettings.first().showWarning,
            showNextPhase = display.showNextPhase,
            showProgress = display.showProgress,
            showSeconds = display.showSeconds,
            autoCalm = display.autoCalm,
            silenceAfter = display.silenceAfter,
            meditationAutoCalm = med.autoCalm,
            meditationShowProgress = med.showProgress,
            meditationShowTime = med.showTime,
            meditationMode = config.mode.name,
            meditationMinutes = config.minutes,
            meditationStartEndGong = config.startEndGong,
            meditationInterval = prefs[Keys.MED_INTERVAL_ON] ?: false,
            meditationSpeech = config.speech,
            gongIntervalMinutes = gongIntervalOf(prefs),
            showStreak = showStreak.first(),
            askFeeling = prefs[Keys.FEELING_PROMPT],
            adjustments = builtInIds.mapNotNull { id ->
                exerciseIntervals(id).first()?.let { id to PatternBackupOverrides(it.duration, it.inhale, it.hold, it.exhale) }
            }.toMap(),
        )
    }

    /**
     * Übernimmt eine schon geprüfte Sicherung ([PatternBackup.decode]): jedes vorhandene Feld ersetzt
     * die Einstellung, ein fehlendes lässt sie stehen. Enum-Namen, die es nicht gibt, hat die
     * Prüfung schon verworfen.
     */
    suspend fun importSettings(backup: SettingsBackup) {
        store.edit { prefs ->
            backup.soundMode?.let { n -> SoundMode.entries.firstOrNull { it.name == n } }?.let { prefs.putEnum(Keys.SOUND_MODE, Keys.SOUND_MODE_ORDINAL, it) }
            backup.transition?.let { n -> TransitionEmphasis.entries.firstOrNull { it.name == n } }?.let { prefs.putEnum(Keys.TRANSITION, Keys.TRANSITION_ORDINAL, it) }
            backup.volume?.let { n -> ToneVolume.entries.firstOrNull { it.name == n } }?.let { prefs.putEnum(Keys.VOLUME, Keys.VOLUME_ORDINAL, it) }
            backup.meditationMode?.let { n -> MeditationMode.entries.firstOrNull { it.name == n } }?.let { prefs.putEnum(Keys.MED_MODE, Keys.MED_MODE_ORDINAL, it) }
            backup.haptics?.let { prefs[Keys.HAPTICS] = it }
            backup.gongLong?.let { prefs[Keys.MED_GONG_LONG] = it }
            backup.showSafetyWarning?.let { prefs[Keys.SHOW_SAFETY] = it }
            backup.showNextPhase?.let { prefs[Keys.SHOW_NEXT] = it }
            backup.showProgress?.let { prefs[Keys.SHOW_PROGRESS] = it }
            backup.showSeconds?.let { prefs[Keys.SHOW_SECONDS] = it }
            backup.autoCalm?.let { prefs[Keys.AUTO_CALM] = it }
            backup.silenceAfter?.let { prefs[Keys.SILENCE_AFTER] = it }
            backup.meditationAutoCalm?.let { prefs[Keys.MED_AUTO_CALM] = it }
            backup.meditationShowProgress?.let { prefs[Keys.MED_SHOW_PROGRESS] = it }
            backup.meditationShowTime?.let { prefs[Keys.MED_SHOW_TIME] = it }
            backup.meditationMinutes?.let { prefs[Keys.MED_MINUTES] = it.coerceIn(MEDITATION_MIN_MINUTES, MEDITATION_MAX_MINUTES) }
            backup.meditationStartEndGong?.let { prefs[Keys.MED_START_END_GONG] = it }
            backup.meditationInterval?.let { prefs[Keys.MED_INTERVAL_ON] = it }
            backup.meditationSpeech?.let { prefs[Keys.MED_SPEECH] = it }
            backup.gongIntervalMinutes?.let { prefs[Keys.MED_GONG_INTERVAL] = it.coerceIn(GONG_INTERVAL_MIN_MINUTES, GONG_INTERVAL_MAX_MINUTES) }
            backup.showStreak?.let { prefs[Keys.SHOW_STREAK] = it }
            backup.askFeeling?.let { prefs[Keys.FEELING_PROMPT] = it }
        }
        backup.adjustments.forEach { (id, o) -> setExerciseIntervals(id, o.duration, o.inhale, o.hold, o.exhale) }
    }

    /** Fügt Markierungen „Tut mir gut“ hinzu (Sicherung zusammenführen); vorhandene bleiben. */
    suspend fun addMarkedExercises(ids: Set<String>) {
        if (ids.isEmpty()) return
        store.edit { prefs -> prefs[Keys.MARKED_EXERCISES] = prefs[Keys.MARKED_EXERCISES].orEmpty() + ids }
    }

    /** Gong-Ausklang: true = voller/langer Ausklang (~7 s), false = kürzer (~5 s). Standard: lang. */
    private fun gongLongOf(prefs: Preferences): Boolean = prefs[Keys.MED_GONG_LONG] ?: true

    val gongLong: Flow<Boolean> = data.map { gongLongOf(it) }

    suspend fun setGongLong(long: Boolean) {
        store.edit { it[Keys.MED_GONG_LONG] = long }
    }

    /**
     * Aktive neuronale Stimme ([VoiceCatalog]-Id); null = keine gewählt. Eine gespeicherte Id, die
     * nicht mehr im Katalog steht (GLaDOS bis 1.5.1), zählt als keine Wahl: Die Anleitung nutzt dann
     * die Sprachausgabe des Geräts. Bereinigt wird sie über [dropUnknownNeuralVoice].
     */
    val neuralVoiceId: Flow<String?> = data.map { prefs ->
        prefs[Keys.MED_NEURAL_VOICE]?.takeIf { VoiceCatalog.byId(it) != null }
    }

    /** Entfernt eine gespeicherte Stimmen-Id, die nicht mehr im Katalog steht; true, wenn eine entfernt wurde. */
    suspend fun dropUnknownNeuralVoice(): Boolean {
        var dropped = false
        store.edit { prefs ->
            val id = prefs[Keys.MED_NEURAL_VOICE]
            if (id != null && VoiceCatalog.byId(id) == null) {
                prefs.remove(Keys.MED_NEURAL_VOICE)
                dropped = true
            }
        }
        return dropped
    }

    suspend fun setNeuralVoiceId(voiceId: String?) {
        store.edit { prefs ->
            if (voiceId == null) prefs.remove(Keys.MED_NEURAL_VOICE) else prefs[Keys.MED_NEURAL_VOICE] = voiceId
        }
    }

    /** Länge des Intervall-Gongs (Minuten): in Einstellungen wählbar; Standard [GONG_INTERVAL_DEFAULT_MINUTES].
     *  Alt-Werte unter [GONG_INTERVAL_MIN_MINUTES] (frühere „kein Intervall = 0“-Semantik) fallen auf den Standard zurück. */
    private fun gongIntervalOf(prefs: Preferences): Int = prefs[Keys.MED_GONG_INTERVAL]
        ?.takeIf { it >= GONG_INTERVAL_MIN_MINUTES }
        ?.coerceAtMost(GONG_INTERVAL_MAX_MINUTES)
        ?: GONG_INTERVAL_DEFAULT_MINUTES

    val gongIntervalMin: Flow<Int> = data.map { gongIntervalOf(it) }

    suspend fun setGongIntervalMin(minutes: Int) {
        store.edit { it[Keys.MED_GONG_INTERVAL] = minutes.coerceIn(GONG_INTERVAL_MIN_MINUTES, GONG_INTERVAL_MAX_MINUTES) }
    }

    val sessionDisplay: Flow<SessionDisplay> = data.map { prefs ->
        val defaults = SessionDisplay()
        SessionDisplay(
            showNextPhase = prefs[Keys.SHOW_NEXT] ?: defaults.showNextPhase,
            showProgress = prefs[Keys.SHOW_PROGRESS] ?: defaults.showProgress,
            showSeconds = prefs[Keys.SHOW_SECONDS] ?: defaults.showSeconds,
            autoCalm = prefs[Keys.AUTO_CALM] ?: defaults.autoCalm,
            silenceAfter = prefs[Keys.SILENCE_AFTER] ?: defaults.silenceAfter,
        )
    }

    val cueSettings: Flow<CueSettings> = data.map { prefs ->
        val mode = prefs.enumOf<SoundMode>(Keys.SOUND_MODE, Keys.SOUND_MODE_ORDINAL)
            ?: if (prefs[Keys.SOUND] == false) SoundMode.OFF else SoundMode.CUES // Migration
        CueSettings(
            soundMode = mode,
            haptics = prefs[Keys.HAPTICS] ?: true,
            transition = prefs.enumOf<TransitionEmphasis>(Keys.TRANSITION, Keys.TRANSITION_ORDINAL)
                ?: TransitionEmphasis.MEDIUM,
            volume = prefs.enumOf<ToneVolume>(Keys.VOLUME, Keys.VOLUME_ORDINAL) ?: CueSettings().volume,
            gongLong = gongLongOf(prefs),
        )
    }

    val safetySettings: Flow<SafetySettings> = data.map { prefs ->
        SafetySettings(
            showWarning = prefs[Keys.SHOW_SAFETY] ?: true,
            acknowledgedIds = prefs[Keys.SAFETY_ACK_IDS].orEmpty(),
        )
    }

    suspend fun setSoundMode(mode: SoundMode) {
        store.edit { it.putEnum(Keys.SOUND_MODE, Keys.SOUND_MODE_ORDINAL, mode) }
    }

    suspend fun setTransitionEmphasis(emphasis: TransitionEmphasis) {
        store.edit { it.putEnum(Keys.TRANSITION, Keys.TRANSITION_ORDINAL, emphasis) }
    }

    suspend fun setToneVolume(volume: ToneVolume) {
        store.edit { it.putEnum(Keys.VOLUME, Keys.VOLUME_ORDINAL, volume) }
    }

    suspend fun setHaptics(enabled: Boolean) {
        store.edit { it[Keys.HAPTICS] = enabled }
    }

    suspend fun setShowSafetyWarning(enabled: Boolean) {
        store.edit { it[Keys.SHOW_SAFETY] = enabled }
    }

    /**
     * Der Sicherheitshinweis von [exerciseId] wurde bestätigt. Die frühere Bestätigung „einmal für
     * alle“ zählt nicht mehr: Wer den Hinweis abgeschaltet hat, bestätigt jede intensive Übung
     * einmal neu – lieber einmal zu oft als ein fremder Hinweis, der nie erscheint.
     */
    suspend fun acknowledgeSafety(exerciseId: String) {
        store.edit { prefs ->
            prefs[Keys.SAFETY_ACK_IDS] = prefs[Keys.SAFETY_ACK_IDS].orEmpty() + exerciseId
            prefs.remove(Keys.SAFETY_ACK)
        }
    }

    suspend fun setShowNextPhase(enabled: Boolean) {
        store.edit { it[Keys.SHOW_NEXT] = enabled }
    }

    suspend fun setShowProgress(enabled: Boolean) {
        store.edit { it[Keys.SHOW_PROGRESS] = enabled }
    }

    suspend fun setShowSeconds(enabled: Boolean) {
        store.edit { it[Keys.SHOW_SECONDS] = enabled }
    }

    suspend fun setAutoCalm(enabled: Boolean) {
        store.edit { it[Keys.AUTO_CALM] = enabled }
    }

    suspend fun setSilenceAfter(enabled: Boolean) {
        store.edit { it[Keys.SILENCE_AFTER] = enabled }
    }

    val meditationDisplay: Flow<MeditationDisplay> = data.map { prefs ->
        val defaults = MeditationDisplay()
        MeditationDisplay(
            autoCalm = prefs[Keys.MED_AUTO_CALM] ?: defaults.autoCalm,
            showProgress = prefs[Keys.MED_SHOW_PROGRESS] ?: defaults.showProgress,
            showTime = prefs[Keys.MED_SHOW_TIME] ?: defaults.showTime,
        )
    }

    suspend fun setMeditationAutoCalm(enabled: Boolean) {
        store.edit { it[Keys.MED_AUTO_CALM] = enabled }
    }

    suspend fun setMeditationShowProgress(enabled: Boolean) {
        store.edit { it[Keys.MED_SHOW_PROGRESS] = enabled }
    }

    suspend fun setMeditationShowTime(enabled: Boolean) {
        store.edit { it[Keys.MED_SHOW_TIME] = enabled }
    }

    // Angepasste Phasenlängen je Übung (dynamische Keys pro Übungs-Id). Das Muster des Tages
    // wird bewusst nicht persistiert (konstante Id, Inhalt wechselt täglich) – das entscheidet
    // aber der Aufrufer, hier landen nur stabile Übungs-Ids.
    private fun ivKey(exerciseId: String, part: String) = intPreferencesKey("iv_${exerciseId}_$part")

    /** Gespeicherte Session-Anpassung der Übung; null = nie angepasst. */
    fun exerciseIntervals(exerciseId: String): Flow<IntervalOverrides?> = data.map { prefs ->
        val duration = prefs[ivKey(exerciseId, "dur")]
        val inhale = prefs[ivKey(exerciseId, "in")]
        val hold = prefs[ivKey(exerciseId, "hold")]
        val exhale = prefs[ivKey(exerciseId, "ex")]
        if (duration == null && inhale == null && hold == null && exhale == null) {
            null
        } else {
            IntervalOverrides(duration = duration, inhale = inhale, hold = hold, exhale = exhale)
        }
    }

    suspend fun setExerciseIntervals(exerciseId: String, duration: Int?, inhale: Int?, hold: Int?, exhale: Int?) {
        store.edit { prefs ->
            fun put(part: String, value: Int?) {
                if (value != null) prefs[ivKey(exerciseId, part)] = value else prefs.remove(ivKey(exerciseId, part))
            }
            put("dur", duration)
            put("in", inhale)
            put("hold", hold)
            put("ex", exhale)
        }
    }

    suspend fun clearExerciseIntervals(exerciseId: String) {
        store.edit { prefs ->
            prefs.remove(ivKey(exerciseId, "dur"))
            prefs.remove(ivKey(exerciseId, "in"))
            prefs.remove(ivKey(exerciseId, "hold"))
            prefs.remove(ivKey(exerciseId, "ex"))
        }
    }

    /** Zuletzt gewählte Meditations-Einstellung (Modus, Dauer, Intervall-Gong, Sprache). */
    val meditationSettings: Flow<MeditationConfig> = data.map { prefs ->
        // Intervall an/aus ist die Tab-Wahl; die Länge X kommt aus der Einstellung.
        val intervalOn = prefs[Keys.MED_INTERVAL_ON] ?: false
        val intervalMin = gongIntervalOf(prefs)
        MeditationConfig(
            mode = prefs.enumOf<MeditationMode>(Keys.MED_MODE, Keys.MED_MODE_ORDINAL) ?: MeditationMode.TIMED,
            minutes = (prefs[Keys.MED_MINUTES] ?: 10).coerceIn(MEDITATION_MIN_MINUTES, MEDITATION_MAX_MINUTES),
            startEndGong = prefs[Keys.MED_START_END_GONG] ?: true,
            gongEveryMin = intervalMin.takeIf { intervalOn },
            speech = prefs[Keys.MED_SPEECH] ?: false,
        )
    }

    /** Speichert die Tab-Wahl (nicht die globale Intervall-Länge – die bleibt eine Einstellung). */
    suspend fun setMeditationConfig(config: MeditationConfig) {
        store.edit { prefs ->
            prefs.putEnum(Keys.MED_MODE, Keys.MED_MODE_ORDINAL, config.mode)
            prefs[Keys.MED_MINUTES] = config.minutes
            prefs[Keys.MED_START_END_GONG] = config.startEndGong
            prefs[Keys.MED_INTERVAL_ON] = config.gongEveryMin != null
            prefs[Keys.MED_SPEECH] = config.speech
        }
    }
}
