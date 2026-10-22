package app.atemkraft.ui

import app.atemkraft.cue.CueEvent
import app.atemkraft.cue.PreviewSoundOutput
import app.atemkraft.data.CueSettings
import app.atemkraft.playback.PhaseAudio

/** Vorschau des Muster-Editors ohne Ton: Tests spielen sonst Töne über Robolectric. */
internal object SilentPreviewSound : PreviewSoundOutput {
    override fun apply(settings: CueSettings) = Unit

    override fun cue(event: CueEvent) = Unit

    override fun continuous(phase: PhaseAudio) = Unit

    override fun silence() = Unit

    override fun requestFocus() = Unit

    override fun abandonFocus() = Unit

    override fun release() = Unit
}
