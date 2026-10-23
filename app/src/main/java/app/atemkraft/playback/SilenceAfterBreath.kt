package app.atemkraft.playback

import app.atemkraft.domain.MeditationConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * „Danach in die Stille“ (Nutzer 2026-10-23): Nach einer Atemübung geht es nahtlos in die stille
 * Meditation, eingestellt wie zuletzt im Tab Meditation ([meditationConfig]; die Stimme wendet der
 * [MeditationController] selbst an). Der Übergang hängt am Ablauf, nicht an der Oberfläche: Der
 * [BreathSessionController] meldet „fertig“ ([BreathSessionController.finished]), dieser Koordinator
 * startet die Meditation – auch hinter dem Sperrbildschirm sofort statt verspätet beim nächsten
 * Sichtbarwerden der App. Der Logbuch-Eintrag der Atemübung steht dann schon.
 *
 * Mit [silenceAfter] (Einstellung) automatisch am Ende; ohne sie über den Link „In der Stille
 * bleiben ›“ am Ende-Screen ([intoSilence]), beides auf demselben Weg. Nie nach Übungen mit
 * [app.atemkraft.domain.Exercise.silenceAfterAllowed] = false (Atmen im Gehen) und nie über eine
 * schon offene Meditation hinweg.
 *
 * **Hintergrund:** Den Foreground-Service darf Android ab 12 nicht aus dem Hintergrund starten. Die
 * Atem-Session läuft selbst mit dem [PlaybackService]; die Meditation übernimmt ihn, bevor die
 * Atem-Session ihn freigibt – der Übergang ist damit auch bei gesperrtem Bildschirm erlaubt
 * ([canStartNow]). Läuft kein Service und ist die App nicht sichtbar, wird der Übergang verworfen
 * statt verspätet nachgeholt: Eine Meditation, die Minuten später von selbst beginnt, wäre
 * überraschender als keine.
 *
 * **Benachrichtigung:** Die Erlaubnis fragt die Oberfläche beim ersten Start einer Sitzung ab, auch
 * einer Atemübung (ARC-6) – zum Übergang ist sie damit schon geklärt; der Koordinator fragt nie.
 */
class SilenceAfterBreath(
    private val breath: BreathSessionController,
    private val meditation: MeditationController,
    private val silenceAfter: Flow<Boolean>,
    private val meditationConfig: Flow<MeditationConfig>,
    private val canStartNow: () -> Boolean,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    /** Gesetzt nach einem Übergang, bis die Oberfläche ihn mit [consumeHandover] abholt. */
    private var handedOver = false

    init {
        scope.launch {
            breath.finished.collect { exercise ->
                if (exercise.silenceAfterAllowed && silenceAfter.first()) enter()
            }
        }
    }

    /** „In der Stille bleiben ›“ am Ende-Screen: derselbe Übergang, auf Tipp. */
    fun intoSilence() {
        if (!breath.state.value.silenceAfterAllowed) return
        scope.launch { enter() }
    }

    /**
     * Gab es seit dem letzten Aufruf einen Übergang? Die Oberfläche übernimmt dann den Zustand des
     * Session-Overlays (offen oder minimiert) für die Meditation – einmal je Übergang.
     */
    fun consumeHandover(): Boolean = handedOver.also { handedOver = false }

    private suspend fun enter() {
        if (meditation.status.value != MeditationStatus.IDLE) return
        val config = meditationConfig.first()
        // Nach dem Lesen erneut prüfen: Inzwischen kann die Atem-Session verworfen oder neu
        // gestartet worden sein; dann gibt es nichts mehr zu übergeben.
        if (breath.status.value != SessionStatus.FINISHED) return
        if (meditation.status.value != MeditationStatus.IDLE || !canStartNow()) return
        // Erst die Meditation (übernimmt den laufenden Foreground-Service), dann die Atem-Session
        // verwerfen (gibt ihren Halter frei) – in dieser Reihenfolge endet der Service nie dazwischen.
        meditation.start(config)
        handedOver = true
        breath.stop()
    }
}
