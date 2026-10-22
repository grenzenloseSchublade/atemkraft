package app.atemkraft.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.atemkraft.AtemkraftApplication
import app.atemkraft.cue.ToneCuePlayer
import app.atemkraft.cue.tts.VoiceDownloadState
import app.atemkraft.cue.tts.VoiceModelManager
import app.atemkraft.cue.tts.VoiceSamplePlayer
import app.atemkraft.cue.tts.VoiceSpec
import app.atemkraft.data.CueSettings
import app.atemkraft.data.LogbookRepository
import app.atemkraft.data.MeditationDisplay
import app.atemkraft.data.SessionDisplay
import app.atemkraft.data.SettingsRepository
import app.atemkraft.domain.GONG_INTERVAL_DEFAULT_MINUTES
import app.atemkraft.domain.SoundMode
import app.atemkraft.domain.ToneVolume
import app.atemkraft.domain.TransitionEmphasis
import app.atemkraft.playback.MeditationController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Alles, was die Einstellungen zeigen. Die Standardwerte sind dieselben wie im
 * [SettingsRepository] (Gong-Abstand 5 min, Gong lang, Session-Anzeige komplett an), damit nichts springt,
 * falls die gespeicherten Werte einen Augenblick später kommen.
 */
data class SettingsUiState(
    val soundMode: SoundMode = CueSettings().soundMode,
    val transition: TransitionEmphasis = CueSettings().transition,
    val volume: ToneVolume = CueSettings().volume,
    val haptics: Boolean = CueSettings().haptics,
    val showSafetyWarning: Boolean = true,
    val sessionDisplay: SessionDisplay = SessionDisplay(),
    val meditationDisplay: MeditationDisplay = MeditationDisplay(),
    val gongIntervalMin: Int = GONG_INTERVAL_DEFAULT_MINUTES,
    val gongLong: Boolean = CueSettings().gongLong,
    val voiceStates: Map<String, VoiceDownloadState> = emptyMap(),
    val activeVoiceId: String? = null,
    val piperEngineReady: Boolean = false,
    /** Die Einstellungen wurden wegen einer beschädigten Datei zurückgesetzt und das ist noch nicht gemeldet. */
    val settingsResetPending: Boolean = false,
    /** Einträge im Logbuch; bei 0 bietet die Daten-Karte kein Leeren an. */
    val logCount: Int = 0,
)

/** Was die Einstellungen auslösen (ohne Navigation und Datei-Auswahl, die bleiben im UI). */
interface SettingsActions {
    fun setSoundMode(mode: SoundMode)
    fun setTransition(emphasis: TransitionEmphasis)
    fun setVolume(volume: ToneVolume)
    fun setHaptics(enabled: Boolean)
    fun setShowSafetyWarning(enabled: Boolean)
    fun setShowNextPhase(enabled: Boolean)
    fun setShowProgress(enabled: Boolean)
    fun setShowSeconds(enabled: Boolean)
    fun setAutoCalm(enabled: Boolean)
    fun setMeditationAutoCalm(enabled: Boolean)
    fun setMeditationShowProgress(enabled: Boolean)
    fun setMeditationShowTime(enabled: Boolean)
    fun setGongInterval(minutes: Int)
    fun setGongLong(long: Boolean)
    fun previewGong()
    fun sampleVoice(spec: VoiceSpec)
    fun downloadVoice(voiceId: String)
    fun selectVoice(voiceId: String?)
    fun deleteVoice(voiceId: String)
    fun clearLog()

    /** Der Hinweis „Einstellungen wurden zurückgesetzt“ wurde gezeigt; er kommt nicht wieder. */
    fun acknowledgeSettingsReset()
}

/**
 * Zustand und Aktionen der Einstellungen – vorher 33 Parameter und 12 Schreib-Lambdas in der
 * App-Wurzel (ARC-3). Hängt an der Activity und sammelt ab dem App-Start (wie vorher die Wurzel),
 * damit beim Öffnen der Seite kein Schalter vom Standard auf den gespeicherten Wert springt. An den
 * Screen gehen nur [state] und [actions], nie der ViewModel selbst (CODE-02).
 */
class SettingsViewModel(
    private val settings: SettingsRepository,
    private val voices: VoiceModelManager,
    private val meditation: MeditationController,
    private val logbook: LogbookRepository,
    private val gongPreview: ToneCuePlayer,
    private val voiceSamples: VoiceSamplePlayer,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        combine(settings.cueSettings, settings.safetySettings, settings.sessionDisplay, settings.gongIntervalMin) { cue, safety, display, interval ->
            SettingsUiState(
                soundMode = cue.soundMode,
                transition = cue.transition,
                volume = cue.volume,
                haptics = cue.haptics,
                showSafetyWarning = safety.showWarning,
                sessionDisplay = display,
                gongIntervalMin = interval,
                gongLong = cue.gongLong,
            )
        },
        voices.states,
        settings.neuralVoiceId,
        meditation.piperReady,
        combine(settings.settingsResetPending, logbook.count, settings.meditationDisplay, ::Triple),
    ) { base, voiceStates, voiceId, piperReady, (resetPending, logCount, meditationDisplay) ->
        base.copy(
            meditationDisplay = meditationDisplay,
            voiceStates = voiceStates,
            activeVoiceId = voiceId,
            piperEngineReady = piperReady,
            settingsResetPending = resetPending,
            logCount = logCount,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        SettingsUiState(voiceStates = voices.states.value, piperEngineReady = meditation.piperReady.value),
    )

    /** Was die Seite auslöst; Schreibzugriffe laufen im [viewModelScope]. */
    val actions: SettingsActions = object : SettingsActions {
        override fun setSoundMode(mode: SoundMode) = write { settings.setSoundMode(mode) }

        override fun setTransition(emphasis: TransitionEmphasis) = write { settings.setTransitionEmphasis(emphasis) }

        override fun setVolume(volume: ToneVolume) = write { settings.setToneVolume(volume) }

        override fun setHaptics(enabled: Boolean) = write { settings.setHaptics(enabled) }

        override fun setShowSafetyWarning(enabled: Boolean) = write { settings.setShowSafetyWarning(enabled) }

        override fun setShowNextPhase(enabled: Boolean) = write { settings.setShowNextPhase(enabled) }

        override fun setShowProgress(enabled: Boolean) = write { settings.setShowProgress(enabled) }

        override fun setShowSeconds(enabled: Boolean) = write { settings.setShowSeconds(enabled) }

        override fun setAutoCalm(enabled: Boolean) = write { settings.setAutoCalm(enabled) }

        override fun setMeditationAutoCalm(enabled: Boolean) = write { settings.setMeditationAutoCalm(enabled) }

        override fun setMeditationShowProgress(enabled: Boolean) = write { settings.setMeditationShowProgress(enabled) }

        override fun setMeditationShowTime(enabled: Boolean) = write { settings.setMeditationShowTime(enabled) }

        override fun setGongInterval(minutes: Int) = write { settings.setGongIntervalMin(minutes) }

        override fun setGongLong(long: Boolean) = write { settings.setGongLong(long) }

        /** Gong zum Vorhören in der eingestellten Lautstärke und Länge. */
        override fun previewGong() {
            gongPreview.setVolume(state.value.volume)
            gongPreview.previewGong(state.value.gongLong)
        }

        override fun sampleVoice(spec: VoiceSpec) = voiceSamples.play(spec.sampleAsset)

        override fun downloadVoice(voiceId: String) = voices.download(voiceId)

        override fun selectVoice(voiceId: String?) = meditation.selectNeuralVoice(voiceId)

        override fun deleteVoice(voiceId: String) = meditation.deleteNeuralVoice(voiceId)

        override fun clearLog() = write { logbook.clear() }

        override fun acknowledgeSettingsReset() = write { settings.acknowledgeSettingsReset() }
    }

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        /** DI: zieht Repositories, Stimmen und Player aus dem AppContainer. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as AtemkraftApplication).container
                SettingsViewModel(
                    settings = container.settingsRepository,
                    voices = container.voiceModelManager,
                    meditation = container.meditationController,
                    logbook = container.logbookRepository,
                    gongPreview = container.gongPreviewPlayer,
                    voiceSamples = container.voiceSamplePlayer,
                )
            }
        }
    }
}
