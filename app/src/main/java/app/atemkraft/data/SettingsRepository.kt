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
import app.atemkraft.cue.tts.VoiceCatalog
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Nutzer-Einstellungen für die Begleit-Reize (Cues), persistent via DataStore. */
data class CueSettings(
    val soundMode: SoundMode = SoundMode.CUES,
    val haptics: Boolean = true,
    val transition: TransitionEmphasis = TransitionEmphasis.MEDIUM,
    val volume: ToneVolume = ToneVolume.LOUD,
    /** Gong-Ausklang lang/kurz (app-weit, auch für den Sitzungs-Abschlussgong). */
    val gongLong: Boolean = true,
)

/** Sicherheits-Einstellungen. [acknowledged] = wurde der Hinweis schon einmal bestätigt. */
data class SafetySettings(
    val showWarning: Boolean = true,
    val acknowledged: Boolean = false,
)

/** Vom Nutzer angepasste Session-Werte einer Übung: Dauer (Minuten bzw. Runden) und
 *  Phasenlängen in Sekunden; null = nicht angepasst bzw. Phase nicht vorhanden. */
data class IntervalOverrides(
    val duration: Int? = null,
    val inhale: Int? = null,
    val hold: Int? = null,
    val exhale: Int? = null,
)

/**
 * Zeitpunkt (Epoch-ms), zu dem eine beschädigte Einstellungsdatei durch eine leere ersetzt wurde.
 * Ohne Cloud-Backup gibt es keine Rettung der alten Werte; die Marke hält den Vorfall wenigstens
 * fest – ohne Logcat (SEC-PRIV-03), abrufbar über [SettingsRepository.resetAfterCorruptionAt].
 */
private val CORRUPTION_RESET_AT = longPreferencesKey("settings_reset_at")

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
        val SAFETY_ACK = booleanPreferencesKey("safety_acknowledged")
        val SHOW_NEXT = booleanPreferencesKey("show_next_phase")
        val MED_MODE = stringPreferencesKey("med_mode_name")
        val MED_MODE_ORDINAL = intPreferencesKey("med_mode")
        val MED_MINUTES = intPreferencesKey("med_minutes")
        val MED_START_END_GONG = booleanPreferencesKey("med_start_end_gong")
        val MED_INTERVAL_ON = booleanPreferencesKey("med_interval_on") // Tab-Wahl: Intervall an
        val MED_GONG_INTERVAL = intPreferencesKey("med_gong_interval") // Einstellung: X Minuten
        val MED_SPEECH = booleanPreferencesKey("med_speech")
        val MED_NEURAL_VOICE = stringPreferencesKey("med_neural_voice") // VoiceCatalog.id; leer = keine
        val MED_GONG_LONG = booleanPreferencesKey("med_gong_long") // Gong-Ausklang: lang (true)/kurz
        val SAVED_PATTERNS_EXPANDED = booleanPreferencesKey("saved_patterns_expanded") // „Meine Muster“ offen
        val MARKED_EXERCISES = stringSetPreferencesKey("marked_exercises") // Atemkreis „tut mir gut“
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

    /**
     * Zuletzt gewählter Aufklapp-Zustand von „Meine Muster“ im Situationen-Tab. Gilt erst ab
     * drei gespeicherten Mustern; nicht gesetzt = zu.
     */
    val savedPatternsExpanded: Flow<Boolean> = data.map { it[Keys.SAVED_PATTERNS_EXPANDED] ?: false }

    suspend fun setSavedPatternsExpanded(expanded: Boolean) {
        store.edit { it[Keys.SAVED_PATTERNS_EXPANDED] = expanded }
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
     *  Alt-Werte unter [GONG_INTERVAL_MIN_MINUTES] (frühere „kein Intervall = 0"-Semantik) fallen auf den Standard zurück. */
    private fun gongIntervalOf(prefs: Preferences): Int = prefs[Keys.MED_GONG_INTERVAL]
        ?.takeIf { it >= GONG_INTERVAL_MIN_MINUTES }
        ?.coerceAtMost(GONG_INTERVAL_MAX_MINUTES)
        ?: GONG_INTERVAL_DEFAULT_MINUTES

    val gongIntervalMin: Flow<Int> = data.map { gongIntervalOf(it) }

    suspend fun setGongIntervalMin(minutes: Int) {
        store.edit { it[Keys.MED_GONG_INTERVAL] = minutes.coerceIn(GONG_INTERVAL_MIN_MINUTES, GONG_INTERVAL_MAX_MINUTES) }
    }

    val showNextPhase: Flow<Boolean> = data.map { prefs ->
        prefs[Keys.SHOW_NEXT] ?: true
    }

    val cueSettings: Flow<CueSettings> = data.map { prefs ->
        val mode = prefs.enumOf<SoundMode>(Keys.SOUND_MODE, Keys.SOUND_MODE_ORDINAL)
            ?: if (prefs[Keys.SOUND] == false) SoundMode.OFF else SoundMode.CUES // Migration
        CueSettings(
            soundMode = mode,
            haptics = prefs[Keys.HAPTICS] ?: true,
            transition = prefs.enumOf<TransitionEmphasis>(Keys.TRANSITION, Keys.TRANSITION_ORDINAL)
                ?: TransitionEmphasis.MEDIUM,
            volume = prefs.enumOf<ToneVolume>(Keys.VOLUME, Keys.VOLUME_ORDINAL) ?: ToneVolume.LOUD,
            gongLong = gongLongOf(prefs),
        )
    }

    val safetySettings: Flow<SafetySettings> = data.map { prefs ->
        SafetySettings(
            showWarning = prefs[Keys.SHOW_SAFETY] ?: true,
            acknowledged = prefs[Keys.SAFETY_ACK] ?: false,
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

    suspend fun setSafetyAcknowledged(value: Boolean) {
        store.edit { it[Keys.SAFETY_ACK] = value }
    }

    suspend fun setShowNextPhase(enabled: Boolean) {
        store.edit { it[Keys.SHOW_NEXT] = enabled }
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
