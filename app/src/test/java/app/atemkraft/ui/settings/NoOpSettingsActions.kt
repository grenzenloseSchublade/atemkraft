package app.atemkraft.ui.settings

import app.atemkraft.domain.SoundMode
import app.atemkraft.domain.ToneVolume
import app.atemkraft.domain.TransitionEmphasis
import app.atemkraft.domain.VoiceSpec
import java.time.DayOfWeek

/** Einstellungs-Aktionen ohne Wirkung – für Screenshot- und Navigationstests. */
object NoOpSettingsActions : SettingsActions {
    override fun setSoundMode(mode: SoundMode) = Unit
    override fun setTransition(emphasis: TransitionEmphasis) = Unit
    override fun setVolume(volume: ToneVolume) = Unit
    override fun setHaptics(enabled: Boolean) = Unit
    override fun setShowSafetyWarning(enabled: Boolean) = Unit
    override fun setShowStreak(enabled: Boolean) = Unit
    override fun setAskFeeling(enabled: Boolean) = Unit
    override fun setReminderEnabled(enabled: Boolean) = Unit
    override fun setReminderMinute(minuteOfDay: Int) = Unit
    override fun setReminderDays(days: Set<DayOfWeek>) = Unit
    override fun setShowNextPhase(enabled: Boolean) = Unit
    override fun setShowProgress(enabled: Boolean) = Unit
    override fun setShowSeconds(enabled: Boolean) = Unit
    override fun setAutoCalm(enabled: Boolean) = Unit
    override fun setSilenceAfter(enabled: Boolean) = Unit
    override fun setMeditationAutoCalm(enabled: Boolean) = Unit
    override fun setMeditationShowProgress(enabled: Boolean) = Unit
    override fun setMeditationShowTime(enabled: Boolean) = Unit
    override fun setGongLong(long: Boolean) = Unit
    override fun previewGong() = Unit
    override fun sampleVoice(spec: VoiceSpec) = Unit
    override fun downloadVoice(voiceId: String) = Unit
    override fun selectVoice(voiceId: String?) = Unit
    override fun deleteVoice(voiceId: String) = Unit
    override fun clearLog() = Unit
    override fun acknowledgeSettingsReset() = Unit
}
