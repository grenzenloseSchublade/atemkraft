package app.atemkraft.ui.patterns

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.atemkraft.R
import app.atemkraft.cue.FakePreviewSoundOutput
import app.atemkraft.data.CueSettings
import app.atemkraft.domain.PhaseType
import app.atemkraft.domain.SoundMode
import app.atemkraft.playback.PhaseAudio
import app.atemkraft.ui.theme.AtemkraftTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Vorschau-Ton im Editor (Nachtrag 3): Lautsprecher-Schalter als Icon-only-Switch („Ton der
 * Vorschau“, An/Aus, Startwert nach Klangmodus), Ton zu Phasenwechseln und still bei Pause,
 * Schalter aus, Modus „Aus“, App im Hintergrund und Verlassen der Seite.
 */
@RunWith(RobolectricTestRunner::class)
class PreviewSoundTest {

    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private val output = FakePreviewSoundOutput()
    private val lifecycle = TestLifecycle()

    private var settings by mutableStateOf(CueSettings(soundMode = SoundMode.CUES))
    private var playing by mutableStateOf(false)
    private var index by mutableIntStateOf(0)
    private var shown by mutableStateOf(true)
    private var hiddenCalls = 0

    private var phases by mutableStateOf(
        listOf(
            PhaseAudio(PhaseType.INHALE, 4_000L, open = false),
            PhaseAudio(PhaseType.EXHALE, 6_000L, open = false),
        ),
    )

    private val switch get() = compose.onNodeWithContentDescription(app.getString(R.string.cd_preview_sound))

    @Before
    fun setUp() {
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycle) {
                AtemkraftTheme {
                    if (shown) {
                        val sound = rememberPreviewSound(settings, onHide = { hiddenCalls++ }, output = { output })
                        val current = phases[index % phases.size]
                        PreviewSoundEffect(sound, playing, index, phaseKind = current.type, revision = phases) { current }
                        PreviewSoundSwitch(sound)
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun assertSwitch(on: Boolean) {
        val state = app.getString(if (on) R.string.state_switch_on else R.string.state_switch_off)
        switch.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, state))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, if (on) ToggleableState.On else ToggleableState.Off))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
    }

    @Test
    fun `Schalter ist ein Switch mit Namen und Zustand, startet an und schaltet um`() {
        assertSwitch(on = true)
        switch.assertIsEnabled().performClick()
        compose.waitForIdle()
        assertSwitch(on = false)
    }

    @Test
    fun `Klangmodus Aus - Schalter aus und deaktiviert, kein Ton`() {
        settings = CueSettings(soundMode = SoundMode.OFF)
        compose.waitForIdle()
        assertSwitch(on = false)
        switch.assertIsNotEnabled()

        playing = true
        compose.waitForIdle()
        assertTrue(output.sounds.isEmpty())
        assertEquals(0, output.focusRequests)
    }

    @Test
    fun `Phasenwechsel spielen den Ton, Pause macht still und gibt den Fokus ab`() {
        playing = true
        compose.waitForIdle()
        index = 1
        compose.waitForIdle()
        assertEquals(listOf("cue INHALE", "cue EXHALE"), output.sounds)
        assertEquals(1, output.focusRequests)

        playing = false
        compose.waitForIdle()
        assertEquals(1, output.focusAbandons)

        // Neu starten = wieder Phasenbeginn.
        playing = true
        compose.waitForIdle()
        assertEquals("cue EXHALE", output.sounds.last())
    }

    @Test
    fun `Schalter aus - kein Ton, Einschalten mitten in der Phase ohne Wechselton`() {
        switch.performClick()
        playing = true
        compose.waitForIdle()
        index = 1
        compose.waitForIdle()
        assertTrue(output.sounds.isEmpty())

        switch.performClick()
        compose.waitForIdle()
        assertTrue(output.sounds.isEmpty())
        index = 2
        compose.waitForIdle()
        assertEquals(listOf("cue INHALE"), output.sounds)
    }

    @Test
    fun `Folge aendert sich bei gleichem Index - Ton folgt der neuen Phase`() {
        playing = true
        compose.waitForIdle()
        // Andere Art an derselben Stelle (Phase davor entfernt): Wechselton der neuen Phase.
        phases = listOf(PhaseAudio(PhaseType.EXHALE, 6_000L, open = false), PhaseAudio(PhaseType.INHALE, 4_000L, open = false))
        compose.waitForIdle()
        assertEquals(listOf("cue INHALE", "cue EXHALE"), output.sounds)
        // Nur die Dauer geändert: kein neuer Wechselton.
        phases = listOf(PhaseAudio(PhaseType.EXHALE, 7_000L, open = false), PhaseAudio(PhaseType.INHALE, 4_000L, open = false))
        compose.waitForIdle()
        assertEquals(listOf("cue INHALE", "cue EXHALE"), output.sounds)
    }

    @Test
    fun `Dauerton folgt einer geaenderten Folge mit neuer Dauer`() {
        settings = CueSettings(soundMode = SoundMode.CONTINUOUS)
        playing = true
        compose.waitForIdle()
        phases = listOf(PhaseAudio(PhaseType.HOLD_FULL, 2_000L, open = false), PhaseAudio(PhaseType.EXHALE, 6_000L, open = false))
        compose.waitForIdle()
        assertEquals(listOf("continuous INHALE 4000", "continuous HOLD_FULL 2000"), output.sounds)
    }

    @Test
    fun `Dauerton laeuft im eingestellten Modus`() {
        settings = CueSettings(soundMode = SoundMode.CONTINUOUS)
        playing = true
        compose.waitForIdle()
        assertEquals(listOf("continuous INHALE 4000"), output.sounds)
        assertEquals(SoundMode.CONTINUOUS, output.applied.last().soundMode)
    }

    @Test
    fun `App im Hintergrund - still, Seite erfaehrt es, danach wieder hoerbar`() {
        playing = true
        compose.waitForIdle()
        val silencedBefore = output.silenced

        compose.runOnUiThread { lifecycle.registry.currentState = Lifecycle.State.CREATED }
        compose.waitForIdle()
        assertEquals(1, hiddenCalls)
        assertTrue(output.silenced > silencedBefore)
        assertEquals(1, output.focusAbandons)

        index = 1
        compose.waitForIdle()
        assertEquals(listOf("cue INHALE"), output.sounds)

        compose.runOnUiThread { lifecycle.registry.currentState = Lifecycle.State.RESUMED }
        compose.waitForIdle()
        index = 2
        compose.waitForIdle()
        assertEquals(listOf("cue INHALE", "cue INHALE"), output.sounds)
    }

    @Test
    fun `Verlassen der Seite gibt Ton und Fokus frei`() {
        playing = true
        compose.waitForIdle()
        shown = false
        compose.waitForIdle()

        assertEquals(1, output.releases)
        assertEquals(1, output.focusAbandons)
    }

    /** Lebenszyklus, den der Test selbst steuert (startet sichtbar). */
    private class TestLifecycle : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
    }
}
