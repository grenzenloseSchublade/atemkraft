package app.atemkraft.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.atemkraft.domain.MeditationConfig
import app.atemkraft.domain.MeditationMode
import app.atemkraft.domain.SoundMode
import app.atemkraft.domain.ToneVolume
import app.atemkraft.domain.TransitionEmphasis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SOUND = booleanPreferencesKey("cue_sound") // alt, nur für Migration
        val SOUND_MODE = intPreferencesKey("sound_mode")
        val TRANSITION = intPreferencesKey("transition_emphasis")
        val VOLUME = intPreferencesKey("tone_volume")
        val HAPTICS = booleanPreferencesKey("cue_haptics")
        val SHOW_SAFETY = booleanPreferencesKey("show_safety_warning")
        val SAFETY_ACK = booleanPreferencesKey("safety_acknowledged")
        val SHOW_NEXT = booleanPreferencesKey("show_next_phase")
        val MED_MODE = intPreferencesKey("med_mode")
        val MED_MINUTES = intPreferencesKey("med_minutes")
        val MED_START_END_GONG = booleanPreferencesKey("med_start_end_gong")
        val MED_INTERVAL_ON = booleanPreferencesKey("med_interval_on") // Tab-Wahl: Intervall an
        val MED_GONG_INTERVAL = intPreferencesKey("med_gong_interval") // Einstellung: X Minuten
        val MED_SPEECH = booleanPreferencesKey("med_speech")
        val MED_NEURAL_VOICE = stringPreferencesKey("med_neural_voice") // VoiceCatalog.id; leer = keine
        val MED_GONG_LONG = booleanPreferencesKey("med_gong_long") // Gong-Ausklang: lang (true)/kurz
    }

    /** Gong-Ausklang: true = voller/langer Ausklang (~7 s), false = kürzer (~5 s). Standard: lang. */
    val gongLong: Flow<Boolean> = context.dataStore.data.map { it[Keys.MED_GONG_LONG] ?: true }

    suspend fun setGongLong(long: Boolean) {
        context.dataStore.edit { it[Keys.MED_GONG_LONG] = long }
    }

    /** Aktive neuronale Stimme ([app.atemkraft.cue.tts.VoiceCatalog]-Id); null = keine gewählt. */
    val neuralVoiceId: Flow<String?> = context.dataStore.data.map { it[Keys.MED_NEURAL_VOICE] }

    suspend fun setNeuralVoiceId(voiceId: String?) {
        context.dataStore.edit { prefs ->
            if (voiceId == null) prefs.remove(Keys.MED_NEURAL_VOICE) else prefs[Keys.MED_NEURAL_VOICE] = voiceId
        }
    }

    /** Länge des Intervall-Gongs (Minuten): in Einstellungen wählbar; Standard 5.
     *  Alt-Werte < 3 (frühere „kein Intervall = 0"-Semantik) fallen auf den Standard zurück. */
    private fun gongIntervalOf(prefs: Preferences): Int = prefs[Keys.MED_GONG_INTERVAL]?.takeIf { it >= 3 }?.coerceAtMost(60) ?: 5

    val gongIntervalMin: Flow<Int> = context.dataStore.data.map { gongIntervalOf(it) }

    suspend fun setGongIntervalMin(minutes: Int) {
        context.dataStore.edit { it[Keys.MED_GONG_INTERVAL] = minutes.coerceIn(3, 60) }
    }

    val showNextPhase: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHOW_NEXT] ?: true
    }

    val cueSettings: Flow<CueSettings> = context.dataStore.data.map { prefs ->
        val mode = prefs[Keys.SOUND_MODE]?.let { SoundMode.entries.getOrNull(it) }
            ?: if (prefs[Keys.SOUND] == false) SoundMode.OFF else SoundMode.CUES // Migration
        CueSettings(
            soundMode = mode,
            haptics = prefs[Keys.HAPTICS] ?: true,
            transition = prefs[Keys.TRANSITION]?.let { TransitionEmphasis.entries.getOrNull(it) }
                ?: TransitionEmphasis.MEDIUM,
            volume = prefs[Keys.VOLUME]?.let { ToneVolume.entries.getOrNull(it) }
                ?: ToneVolume.LOUD,
            gongLong = prefs[Keys.MED_GONG_LONG] ?: true,
        )
    }

    val safetySettings: Flow<SafetySettings> = context.dataStore.data.map { prefs ->
        SafetySettings(
            showWarning = prefs[Keys.SHOW_SAFETY] ?: true,
            acknowledged = prefs[Keys.SAFETY_ACK] ?: false,
        )
    }

    suspend fun setSoundMode(mode: SoundMode) {
        context.dataStore.edit { it[Keys.SOUND_MODE] = mode.ordinal }
    }

    suspend fun setTransitionEmphasis(emphasis: TransitionEmphasis) {
        context.dataStore.edit { it[Keys.TRANSITION] = emphasis.ordinal }
    }

    suspend fun setToneVolume(volume: ToneVolume) {
        context.dataStore.edit { it[Keys.VOLUME] = volume.ordinal }
    }

    suspend fun setHaptics(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS] = enabled }
    }

    suspend fun setShowSafetyWarning(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_SAFETY] = enabled }
    }

    suspend fun setSafetyAcknowledged(value: Boolean) {
        context.dataStore.edit { it[Keys.SAFETY_ACK] = value }
    }

    suspend fun setShowNextPhase(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_NEXT] = enabled }
    }

    // Angepasste Phasenlängen je Übung (dynamische Keys pro Übungs-Id). Das Muster des Tages
    // wird bewusst nicht persistiert (konstante Id, Inhalt wechselt täglich) – das entscheidet
    // aber der Aufrufer, hier landen nur stabile Übungs-Ids.
    private fun ivKey(exerciseId: String, part: String) = intPreferencesKey("iv_${exerciseId}_$part")

    /** Gespeicherte Session-Anpassung der Übung; null = nie angepasst. */
    fun exerciseIntervals(exerciseId: String): Flow<IntervalOverrides?> = context.dataStore.data.map { prefs ->
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
        context.dataStore.edit { prefs ->
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
        context.dataStore.edit { prefs ->
            prefs.remove(ivKey(exerciseId, "dur"))
            prefs.remove(ivKey(exerciseId, "in"))
            prefs.remove(ivKey(exerciseId, "hold"))
            prefs.remove(ivKey(exerciseId, "ex"))
        }
    }

    /** Zuletzt gewählte Meditations-Einstellung (Modus, Dauer, Intervall-Gong, Sprache). */
    val meditationSettings: Flow<MeditationConfig> = context.dataStore.data.map { prefs ->
        // Intervall an/aus ist die Tab-Wahl; die Länge X kommt aus der Einstellung.
        val intervalOn = prefs[Keys.MED_INTERVAL_ON] ?: false
        val intervalMin = gongIntervalOf(prefs)
        MeditationConfig(
            mode = prefs[Keys.MED_MODE]?.let { MeditationMode.entries.getOrNull(it) }
                ?: MeditationMode.TIMED,
            minutes = (prefs[Keys.MED_MINUTES] ?: 10).coerceIn(1, 180),
            startEndGong = prefs[Keys.MED_START_END_GONG] ?: true,
            gongEveryMin = intervalMin.takeIf { intervalOn },
            speech = prefs[Keys.MED_SPEECH] ?: false,
        )
    }

    /** Speichert die Tab-Wahl (nicht die globale Intervall-Länge – die bleibt eine Einstellung). */
    suspend fun setMeditationConfig(config: MeditationConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MED_MODE] = config.mode.ordinal
            prefs[Keys.MED_MINUTES] = config.minutes
            prefs[Keys.MED_START_END_GONG] = config.startEndGong
            prefs[Keys.MED_INTERVAL_ON] = config.gongEveryMin != null
            prefs[Keys.MED_SPEECH] = config.speech
        }
    }
}
