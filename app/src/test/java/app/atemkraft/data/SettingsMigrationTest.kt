package app.atemkraft.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.atemkraft.domain.SoundMode
import app.atemkraft.domain.ToneVolume
import app.atemkraft.domain.TransitionEmphasis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * CODE-07 „Daten überleben Migration“ für die Einstellungen: Stände bis 1.6.5 speicherten
 * Enums als ordinal-Index, davor den Ton als Schalter `cue_sound`. Eine stille Fehl-Migration
 * setzte Ton-Modus, Zäsur oder Lautstärke zurück.
 */
class SettingsMigrationTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @After
    fun tearDown() {
        storeScope.cancel()
    }

    @Test
    fun `Einstellungen aus alten Versionen gelten weiter, Schreiben stellt auf Namen um`() = runBlocking {
        val store = PreferenceDataStoreFactory.create(scope = storeScope) { File(folder.root, "settings.preferences_pb") }
        // Stand vor 1.6.6: Ton per Schalter aus, Zäsur und Lautstärke als ordinal-Index.
        store.edit {
            it[booleanPreferencesKey("cue_sound")] = false
            it[intPreferencesKey("transition_emphasis")] = TransitionEmphasis.STRONG.ordinal
            it[intPreferencesKey("tone_volume")] = ToneVolume.QUIET.ordinal
        }
        val settings = SettingsRepository(store)
        val old = settings.cueSettings.first()
        assertEquals(SoundMode.OFF, old.soundMode)
        assertEquals(TransitionEmphasis.STRONG, old.transition)
        assertEquals(ToneVolume.QUIET, old.volume)

        // Neu gesetzt steht der Wert unter seinem Namen, der ordinal-Key ist weg; der Rest bleibt.
        store.edit { it[intPreferencesKey("sound_mode")] = SoundMode.CUES.ordinal }
        assertEquals(SoundMode.CUES, settings.cueSettings.first().soundMode)
        settings.setSoundMode(SoundMode.CONTINUOUS)
        settings.setToneVolume(ToneVolume.LOUD)
        val prefs = store.data.first()
        assertEquals("CONTINUOUS", prefs[stringPreferencesKey("sound_mode_name")])
        assertNull(prefs[intPreferencesKey("sound_mode")])
        assertNull(prefs[intPreferencesKey("tone_volume")])
        val now = settings.cueSettings.first()
        assertEquals(Triple(SoundMode.CONTINUOUS, TransitionEmphasis.STRONG, ToneVolume.LOUD), Triple(now.soundMode, now.transition, now.volume))
    }
}
