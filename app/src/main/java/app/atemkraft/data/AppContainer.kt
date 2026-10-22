package app.atemkraft.data

import android.content.Context
import app.atemkraft.cue.HapticPlayer
import app.atemkraft.cue.ToneCuePlayer
import app.atemkraft.cue.tts.VoiceModelManager
import app.atemkraft.cue.tts.VoiceSamplePlayer
import app.atemkraft.data.local.AtemkraftDatabase
import app.atemkraft.playback.MeditationAudioCoordinator
import app.atemkraft.playback.MeditationController
import app.atemkraft.playback.MeditationServiceLink
import kotlinx.coroutines.flow.first

/**
 * Manuelles DI: hält die Repositories einmalig (keine Instanziierung in Composables).
 * Wird von [app.atemkraft.AtemkraftApplication] erzeugt und an ViewModels/Composables
 * weitergereicht.
 */
class AppContainer(context: Context) {

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

    /** App-weiter Meditations-Ablauf (überlebt Tab-Wechsel und läuft mit dem Foreground-Service). */
    val meditationController: MeditationController = MeditationController(
        settingsRepository = settingsRepository,
        logbook = logbookRepository,
        audio = MeditationAudioCoordinator(context, voiceModelManager),
        service = MeditationServiceLink(context),
    )
}
