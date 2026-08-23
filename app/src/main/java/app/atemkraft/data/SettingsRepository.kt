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
)

/** Sicherheits-Einstellungen. [acknowledged] = wurde der Hinweis schon einmal bestätigt. */
data class SafetySettings(
    val showWarning: Boolean = true,
    val acknowledged: Boolean = false,
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
        val MED_TTS_VOICE = stringPreferencesKey("med_tts_voice") // Voice.getName; leer = auto
    }

    /** Bevorzugte TTS-Stimme (Voice-Name); null = automatisch beste. In Einstellungen wählbar. */
    val ttsVoiceId: Flow<String?> = context.dataStore.data.map { it[Keys.MED_TTS_VOICE] }

    suspend fun setTtsVoiceId(voiceId: String?) {
        context.dataStore.edit { prefs ->
            if (voiceId == null) prefs.remove(Keys.MED_TTS_VOICE) else prefs[Keys.MED_TTS_VOICE] = voiceId
        }
    }

    /** Länge des Intervall-Gongs (Minuten) – in Einstellungen wählbar; Standard 5.
     *  Alt-Werte < 3 (frühere „kein Intervall = 0"-Semantik) fallen auf den Standard zurück. */
    val gongIntervalMin: Flow<Int> = context.dataStore.data.map {
        it[Keys.MED_GONG_INTERVAL]?.takeIf { m -> m >= 3 }?.coerceAtMost(60) ?: 5
    }

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

    /** Zuletzt gewählte Meditations-Einstellung (Modus, Dauer, Intervall-Gong, Sprache). */
    val meditationSettings: Flow<MeditationConfig> = context.dataStore.data.map { prefs ->
        // Intervall an/aus ist die Tab-Wahl; die Länge X kommt aus der Einstellung.
        val intervalOn = prefs[Keys.MED_INTERVAL_ON] ?: false
        val intervalMin = prefs[Keys.MED_GONG_INTERVAL]?.takeIf { it >= 3 }?.coerceAtMost(60) ?: 5
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
