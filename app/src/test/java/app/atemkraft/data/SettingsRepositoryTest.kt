package app.atemkraft.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Dauerhafte Einstellungen in DataStore, die der Nutzer nebenbei setzt (SEC-PRIV-04). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsRepositoryTest {

    @Test
    fun `Meine Muster startet zu, und die Wahl steht nach einem neuen Repository wieder da`() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        assertEquals(false, SettingsRepository(context).savedPatternsExpanded.first())

        SettingsRepository(context).setSavedPatternsExpanded(true)
        assertEquals(true, SettingsRepository(context).savedPatternsExpanded.first())

        SettingsRepository(context).setSavedPatternsExpanded(false)
        assertEquals(false, SettingsRepository(context).savedPatternsExpanded.first())
    }

    @Test
    fun `eine nicht mehr angebotene Stimme gilt als keine Wahl und wird vergessen`() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val repo = SettingsRepository(context)
        repo.setNeuralVoiceId("glados") // gespeichert bis 1.5.1
        assertEquals(null, repo.neuralVoiceId.first())
        assertTrue(repo.dropUnknownNeuralVoice())
        assertFalse(repo.dropUnknownNeuralVoice())

        repo.setNeuralVoiceId("thorsten")
        assertFalse(repo.dropUnknownNeuralVoice())
        assertEquals("thorsten", SettingsRepository(context).neuralVoiceId.first())
    }
}
