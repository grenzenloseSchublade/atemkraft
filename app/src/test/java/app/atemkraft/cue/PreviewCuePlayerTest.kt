package app.atemkraft.cue

import app.atemkraft.data.CueSettings
import app.atemkraft.domain.PhaseType
import app.atemkraft.domain.SoundMode
import app.atemkraft.domain.ToneVolume
import app.atemkraft.playback.PhaseAudio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Logik des Vorschau-Tons (Nachtrag 3) ohne AudioTrack: Wechseltöne nur zum Phasenbeginn im
 * Modus „Töne“, Dauerton im Modus „Dauerton“, nichts bei „Aus“, Schalter aus oder gesperrt;
 * Audio-Fokus einmal angefordert und mit dem Anhalten abgegeben.
 */
class PreviewCuePlayerTest {

    private val output = FakePreviewSoundOutput()
    private val player = PreviewCuePlayer(output)

    private val inhale = PhaseAudio(PhaseType.INHALE, 4_000L, open = false)
    private val hold = PhaseAudio(PhaseType.HOLD_FULL, 2_000L, open = false)

    @Test
    fun `Toene spielen den Wechselton zum Phasenbeginn, nicht beim Fortsetzen`() {
        player.updateSettings(CueSettings(soundMode = SoundMode.CUES))
        player.onPhase(inhale)
        player.onPhase(hold, phaseBegins = false)
        player.onPhase(hold)

        assertEquals(listOf("cue INHALE", "cue HOLD"), output.sounds)
        assertEquals(1, output.focusRequests)
    }

    @Test
    fun `Dauerton folgt jeder Phase, auch beim Fortsetzen`() {
        player.updateSettings(CueSettings(soundMode = SoundMode.CONTINUOUS))
        player.onPhase(inhale)
        player.onPhase(hold.copy(durationMs = 900L), phaseBegins = false)

        assertEquals(listOf("continuous INHALE 4000", "continuous HOLD_FULL 900"), output.sounds)
    }

    @Test
    fun `still ohne Einstellungen, bei Modus Aus und gesperrt, danach wieder hoerbar`() {
        player.onPhase(inhale)
        assertFalse(player.audible)

        player.updateSettings(CueSettings(soundMode = SoundMode.OFF))
        player.onPhase(inhale)
        assertTrue(output.sounds.isEmpty())
        assertEquals(0, output.focusRequests)

        player.updateSettings(CueSettings(soundMode = SoundMode.CUES))
        player.blocked = true
        player.onPhase(inhale)
        assertTrue(output.sounds.isEmpty())

        player.blocked = false
        player.onPhase(hold)
        assertEquals(listOf("cue HOLD"), output.sounds)
    }

    @Test
    fun `Schalter aus bleibt still und gibt den Fokus ab`() {
        player.updateSettings(CueSettings(soundMode = SoundMode.CONTINUOUS))
        player.onPhase(inhale)
        player.enabled = false
        player.onPhase(hold)

        assertEquals(listOf("continuous INHALE 4000"), output.sounds)
        assertEquals(1, output.focusAbandons)
        assertTrue(output.silenced > 0)
    }

    @Test
    fun `stop und release sind idempotent`() {
        player.updateSettings(CueSettings(soundMode = SoundMode.CUES))
        player.onPhase(inhale)
        player.stop()
        player.stop()
        assertEquals(1, output.focusRequests)
        assertEquals(1, output.focusAbandons)

        player.release()
        player.release()
        player.onPhase(inhale)
        assertEquals(listOf("cue INHALE"), output.sounds)
        assertEquals(1, output.releases)
    }

    @Test
    fun `Lautstaerke wird uebernommen, Moduswechsel beendet den Ton`() {
        player.updateSettings(CueSettings(soundMode = SoundMode.CONTINUOUS, volume = ToneVolume.QUIET))
        player.onPhase(inhale)
        val silencedBefore = output.silenced
        player.updateSettings(CueSettings(soundMode = SoundMode.CUES, volume = ToneVolume.QUIET))

        assertEquals(ToneVolume.QUIET, output.applied.last().volume)
        assertTrue(output.silenced > silencedBefore)
        assertEquals(1, output.focusAbandons)
    }
}

/** Zeichnet auf, was die Vorschau spielen würde (geteilt mit `PreviewSoundTest`). */
internal class FakePreviewSoundOutput : PreviewSoundOutput {
    val sounds = mutableListOf<String>()
    val applied = mutableListOf<CueSettings>()
    var silenced = 0
    var focusRequests = 0
    var focusAbandons = 0
    var releases = 0

    override fun apply(settings: CueSettings) {
        applied += settings
    }

    override fun cue(event: CueEvent) {
        sounds += "cue $event"
    }

    override fun continuous(phase: PhaseAudio) {
        sounds += "continuous ${phase.type} ${phase.durationMs}"
    }

    override fun silence() {
        silenced++
    }

    override fun requestFocus() {
        focusRequests++
    }

    override fun abandonFocus() {
        focusAbandons++
    }

    override fun release() {
        releases++
    }
}
