package app.atemkraft.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * DataStore „settings“ im Hintergrund bereinigt. Das lief dem GLaDOS-Test sporadisch dazwischen.
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
    fun `Meine Muster startet zu, und die Wahl steht nach einem neuen Repository wieder da`() = runBlocking {
        assertEquals(false, SettingsRepository(store).savedPatternsExpanded.first())

        SettingsRepository(store).setSavedPatternsExpanded(true)
        assertEquals(true, SettingsRepository(store).savedPatternsExpanded.first())

        SettingsRepository(store).setSavedPatternsExpanded(false)
        assertEquals(false, SettingsRepository(store).savedPatternsExpanded.first())
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
}
