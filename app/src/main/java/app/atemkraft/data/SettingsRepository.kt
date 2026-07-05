package app.atemkraft.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
}
