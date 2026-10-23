package app.atemkraft

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.atemkraft.cue.HapticPlayer
import app.atemkraft.cue.ToneCuePlayer
import app.atemkraft.cue.tts.VoiceModelManager
import app.atemkraft.cue.tts.VoiceSamplePlayer
import app.atemkraft.data.ExerciseRepository
import app.atemkraft.data.LogbookRepository
import app.atemkraft.data.SavedPatternsRepository
import app.atemkraft.data.SettingsRepository
import app.atemkraft.data.local.AtemkraftDatabase
import app.atemkraft.domain.supervisedScope
import app.atemkraft.playback.BreathSessionController
import app.atemkraft.playback.MeditationAudioCoordinator
import app.atemkraft.playback.MeditationController
import app.atemkraft.playback.PlaybackServiceLinks
import app.atemkraft.playback.SessionAudioCoordinator
import app.atemkraft.playback.SilenceAfterBreath
import app.atemkraft.ui.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Manuelles DI: hält Repositories, Player und die app-weiten Abläufe einmalig (keine
 * Instanziierung in Composables). Wird von [AtemkraftApplication] erzeugt und an
 * ViewModels/Composables weitergereicht. Liegt im Wurzelpaket, weil es data, cue und playback
 * zusammensteckt.
 */
class AppContainer(context: Context) {

    /**
     * App-weiter Scope für Schreibvorgänge, die die Oberfläche anstößt und nicht abwartet
     * (Lesezeichen, Markierung, Anpassungen, Bestätigungen): Er lebt so lange wie der Prozess und
     * übersteht Drehen und Verlassen der Seite, anders als `rememberCoroutineScope()`.
     */
    val appScope: CoroutineScope = supervisedScope(Dispatchers.Main.immediate)

    private val database = AtemkraftDatabase.build(context)

    val settingsRepository: SettingsRepository = SettingsRepository(context)

    /**
     * Beim Bearbeiten eines Musters fallen dessen Detailseiten-Anpassungen weg; die dort gewählte
     * Dauer übernimmt der Editor als Startwert.
     */
    val savedPatternsRepository: SavedPatternsRepository = SavedPatternsRepository(
        dao = database.savedPatternDao(),
        clearOverrides = settingsRepository::clearExerciseIntervals,
        overrideMinutes = { id -> settingsRepository.exerciseIntervals(id).first()?.duration },
    )
    val exerciseRepository: ExerciseRepository = ExerciseRepository(savedPatternsRepository)
    val logbookRepository: LogbookRepository = LogbookRepository(database.logbookDao())

    /** Katalog neuronaler Stimmen (Download/Verwaltung); Modelle werden bei Bedarf nachgeladen. */
    val voiceModelManager: VoiceModelManager = VoiceModelManager(context)

    /** Spielt die kurzen Vorhör-Clips der Stimmen (vor dem Download). */
    val voiceSamplePlayer: VoiceSamplePlayer = VoiceSamplePlayer(context)

    /** Vorhören des Gongs in den Einstellungen (eigene Instanz, unabhängig von laufenden Sessions). */
    val gongPreviewPlayer: ToneCuePlayer = ToneCuePlayer()

    /** Vibration: Phasen der Atem-Session, Tipp-Tick am Kreis, Rasten des Zeitrings (eine Instanz, AUDIO-04). */
    val hapticPlayer: HapticPlayer = HapticPlayer(context)

    /** Ein Foreground-Service für beide Abläufe; jeder hält ihn über einen eigenen Halter. */
    private val playbackService = PlaybackServiceLinks(context)

    /** App-weiter Meditations-Ablauf (überlebt Tab-Wechsel und läuft mit dem Foreground-Service). */
    val meditationController: MeditationController = MeditationController(
        settingsRepository = settingsRepository,
        logbook = logbookRepository,
        audio = MeditationAudioCoordinator(context, voiceModelManager),
        service = playbackService.link("meditation"),
    )

    /**
     * App-weiter Ablauf der Atemübung, gebaut wie der Meditations-Ablauf: überlebt das Beenden der
     * Activity und das Wegwischen, läuft mit demselben Foreground-Service.
     */
    val breathSessionController: BreathSessionController = BreathSessionController(
        audio = SessionAudioCoordinator(
            context = context,
            settings = settingsRepository.cueSettings,
            hapticPlayer = hapticPlayer,
        ),
        logbook = logbookRepository,
        service = playbackService.link("breath"),
    )

    /** „Danach in die Stille“: startet nach der Atemübung die Meditation (auch im Hintergrund, s. dort). */
    val silenceAfterBreath: SilenceAfterBreath = SilenceAfterBreath(
        breath = breathSessionController,
        meditation = meditationController,
        meditationConfig = settingsRepository.meditationSettings,
        canStartNow = playbackService::canStartNow,
    )

    /**
     * Fabrik des [SettingsViewModel], des einzigen ViewModels: zieht Repositories, Stimmen und
     * Player aus diesem Container. Liegt hier statt im ViewModel, damit `ui/` weder die
     * Application noch den Container kennt.
     */
    val settingsViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            SettingsViewModel(
                settings = settingsRepository,
                voices = voiceModelManager,
                meditation = meditationController,
                logbook = logbookRepository,
                gongPreview = gongPreviewPlayer,
                voiceSamples = voiceSamplePlayer,
            )
        }
    }
}
