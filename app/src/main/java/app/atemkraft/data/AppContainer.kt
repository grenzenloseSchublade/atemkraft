package app.atemkraft.data

import android.content.Context
import app.atemkraft.cue.HapticPlayer
import app.atemkraft.cue.ToneCuePlayer
import app.atemkraft.cue.tts.VoiceModelManager
import app.atemkraft.cue.tts.VoiceSamplePlayer
import app.atemkraft.data.local.AtemkraftDatabase
import app.atemkraft.ui.meditation.MeditationAudioCoordinator
import app.atemkraft.ui.meditation.MeditationController

/**
 * Manuelles DI: hält die Repositories einmalig (keine Instanziierung in Composables).
 * Wird von [app.atemkraft.AtemkraftApplication] erzeugt und an ViewModels/Composables
 * weitergereicht.
 */
class AppContainer(context: Context) {

    private val database = AtemkraftDatabase.build(context)

    val savedPatternsRepository: SavedPatternsRepository =
        SavedPatternsRepository(database.savedPatternDao())
    val exerciseRepository: ExerciseRepository = ExerciseRepository(savedPatternsRepository)
    val settingsRepository: SettingsRepository = SettingsRepository(context)
    val logbookRepository: LogbookRepository = LogbookRepository(database.logbookDao())

    /** Katalog neuronaler Stimmen (Download/Verwaltung); Modelle werden bei Bedarf nachgeladen. */
    val voiceModelManager: VoiceModelManager = VoiceModelManager(context)

    /** Spielt die kurzen Vorhör-Clips der Stimmen (vor dem Download). */
    val voiceSamplePlayer: VoiceSamplePlayer = VoiceSamplePlayer(context)

    /** Vorhören des Gongs in den Einstellungen (eigene Instanz, unabhängig von laufenden Sessions). */
    val gongPreviewPlayer: ToneCuePlayer = ToneCuePlayer()

    /** Tipp-Tick am Atem- und Meditationskreis (eine Instanz, nicht pro Composition, AUDIO-04). */
    val hapticPlayer: HapticPlayer = HapticPlayer(context)

    /** App-weiter Meditations-Ablauf (überlebt Tab-Wechsel und läuft mit dem Foreground-Service). */
    val meditationController: MeditationController = MeditationController(
        context = context,
        settingsRepository = settingsRepository,
        logbook = logbookRepository,
        audio = MeditationAudioCoordinator(context, voiceModelManager),
    )
}
