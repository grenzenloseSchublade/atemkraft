package app.atemkraft.ui.settings

import app.atemkraft.cue.tts.VoiceSpec
import app.atemkraft.domain.SoundMode
import app.atemkraft.domain.ToneVolume
import app.atemkraft.domain.TransitionEmphasis

/** Einstellungs-Aktionen ohne Wirkung – für Screenshot- und Navigationstests. */
object NoOpSettingsActions : SettingsActions {
    override fun setSoundMode(mode: SoundMode) = Unit
    override fun setTransition(emphasis: TransitionEmphasis) = Unit
    override fun setVolume(volume: ToneVolume) = Unit
    override fun setHaptics(enabled: Boolean) = Unit
    override fun setShowSafetyWarning(enabled: Boolean) = Unit
    override fun setShowNextPhase(enabled: Boolean) = Unit
    override fun setGongInterval(minutes: Int) = Unit
    override fun setGongLong(long: Boolean) = Unit
    override fun previewGong() = Unit
    override fun sampleVoice(spec: VoiceSpec) = Unit
    override fun downloadVoice(voiceId: String) = Unit
    override fun selectVoice(voiceId: String?) = Unit
    override fun deleteVoice(voiceId: String) = Unit
    override fun clearLog() = Unit
}
