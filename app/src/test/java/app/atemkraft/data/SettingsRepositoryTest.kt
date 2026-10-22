package app.atemkraft.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.atemkraft.domain.GONG_INTERVAL_DEFAULT_MINUTES
import app.atemkraft.domain.GONG_INTERVAL_MAX_MINUTES
import app.atemkraft.domain.MeditationConfig
import app.atemkraft.domain.MeditationMode
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Dauerhafte Einstellungen in DataStore, die der Nutzer nebenbei setzt (SEC-PRIV-04).
 *
 * Jeder Test hat seinen eigenen Speicher in einem eigenen Ordner, ohne Robolectric: Unter
 * Robolectric startet pro Test die echte App, deren Start die Stimmen-Id im gemeinsamen
 * DataStore „settings“ im Hintergrund bereinigt. Das lief dem Stimmen-Test sporadisch dazwischen.
 */
class SettingsRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var scope: CoroutineScope
    private lateinit var store: DataStore<Preferences>

    @Before
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        store = PreferenceDataStoreFactory.create(scope = scope) {
            File(folder.root, "settings.preferences_pb")
        }
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `Markierung tut mir gut gilt je Übung und bleibt nach einem neuen Repository`() = runBlocking {
        SettingsRepository(store).setExerciseMarked("resonanz", true)
        SettingsRepository(store).setExerciseMarked("box-4-4-4-4", true)
        SettingsRepository(store).setExerciseMarked("box-4-4-4-4", false)
        assertEquals(setOf("resonanz"), SettingsRepository(store).markedExercises.first())
    }

    @Test
    fun `eine nicht mehr angebotene Stimme gilt als keine Wahl und wird vergessen`() = runBlocking {
        val repo = SettingsRepository(store)
        repo.setNeuralVoiceId("glados") // gespeichert bis 1.5.1
        assertEquals(null, repo.neuralVoiceId.first())
        assertTrue(repo.dropUnknownNeuralVoice())
        assertFalse(repo.dropUnknownNeuralVoice())

        repo.setNeuralVoiceId("thorsten")
        assertFalse(repo.dropUnknownNeuralVoice())
        assertEquals("thorsten", SettingsRepository(store).neuralVoiceId.first())
    }

    /** Bis 1.6.5 stand jede Einstellung als Enum-ordinal unter dem alten Int-Key. */
    @Test
    fun `alte Einstellungen mit ordinal-Index gelten weiter und wandern beim Setzen unter den Namen`() = runBlocking {
        store.edit {
            it[intPreferencesKey("sound_mode")] = SoundMode.CONTINUOUS.ordinal
            it[intPreferencesKey("transition_emphasis")] = TransitionEmphasis.STRONG.ordinal
            it[intPreferencesKey("tone_volume")] = ToneVolume.QUIET.ordinal
            it[intPreferencesKey("med_mode")] = MeditationMode.FREE.ordinal
        }
        val repo = SettingsRepository(store)
        val cue = repo.cueSettings.first()
        assertEquals(SoundMode.CONTINUOUS, cue.soundMode)
        assertEquals(TransitionEmphasis.STRONG, cue.transition)
        assertEquals(ToneVolume.QUIET, cue.volume)
        assertEquals(MeditationMode.FREE, repo.meditationSettings.first().mode)

        repo.setSoundMode(SoundMode.OFF)
        repo.setTransitionEmphasis(TransitionEmphasis.SOFT)
        repo.setToneVolume(ToneVolume.MEDIUM)
        repo.setMeditationConfig(MeditationConfig(mode = MeditationMode.FREE))
        val raw = store.data.first().asMap().mapKeys { it.key.name }
        assertEquals("OFF", raw["sound_mode_name"])
        assertEquals("SOFT", raw["transition_emphasis_name"])
        assertEquals("MEDIUM", raw["tone_volume_name"])
        assertEquals("FREE", raw["med_mode_name"])
        assertFalse(raw.keys.any { it in setOf("sound_mode", "transition_emphasis", "tone_volume", "med_mode") })

        // Unbekannte Namen (künftige oder kaputte Werte) fallen auf den Standard zurück.
        store.edit { it[stringPreferencesKey("sound_mode_name")] = "GIBT_ES_NICHT" }
        assertEquals(SoundMode.CUES, SettingsRepository(store).cueSettings.first().soundMode)
    }

    @Test
    fun `Gong-Abstand bleibt in seinen Grenzen, Alt-Wert 0 gilt als nicht gesetzt`() = runBlocking {
        val repo = SettingsRepository(store)
        repo.setGongIntervalMin(500)
        assertEquals(GONG_INTERVAL_MAX_MINUTES, repo.gongIntervalMin.first())
        // Alt-Wert 0 (früher „kein Intervall“) gilt als nicht gesetzt.
        store.edit { it[intPreferencesKey("med_gong_interval")] = 0 }
        assertEquals(GONG_INTERVAL_DEFAULT_MINUTES, repo.gongIntervalMin.first())
    }

    @Test
    fun `eine beschädigte Einstellungsdatei startet leer, bleibt beschreibbar und meldet das genau einmal`() = runBlocking {
        val file = File(folder.root, "kaputt.preferences_pb")
        file.writeBytes(byteArrayOf(0x7f, 0x13, 0x00, 0x42, 0x55)) // kein gültiges Protobuf
        val broken = PreferenceDataStoreFactory.create(corruptionHandler = settingsCorruptionHandler, scope = scope) { file }

        val repo = SettingsRepository(broken)
        assertEquals(SoundMode.CUES, repo.cueSettings.first().soundMode)
        assertTrue(repo.settingsResetPending.first())
        repo.acknowledgeSettingsReset()
        assertFalse(repo.settingsResetPending.first())
        // Die Marke bleibt als Vermerk; ein zweites Bestätigen ändert nichts.
        assertNotNull(repo.resetAfterCorruptionAt.first())
        repo.acknowledgeSettingsReset()
        assertFalse(repo.settingsResetPending.first())
        // Danach ist die Datei wieder beschreibbar.
        repo.setSoundMode(SoundMode.OFF)
        assertEquals(SoundMode.OFF, repo.cueSettings.first().soundMode)
    }

    @Test
    fun `ein neues Zurücksetzen nach einem gemeldeten wird wieder gemeldet`() {
        assertFalse(resetNoticePending(resetAt = null, seenAt = null))
        assertTrue(resetNoticePending(resetAt = 1_000L, seenAt = null))
        assertFalse(resetNoticePending(resetAt = 1_000L, seenAt = 1_000L))
        assertTrue(resetNoticePending(resetAt = 2_000L, seenAt = 1_000L))
    }
}
