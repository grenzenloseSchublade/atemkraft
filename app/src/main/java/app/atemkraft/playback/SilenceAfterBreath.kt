package app.atemkraft.playback

import app.atemkraft.domain.MeditationConfig
import app.atemkraft.domain.MeditationMode
import app.atemkraft.domain.supervisedScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * „Danach in die Stille“ (Nutzer 2026-10-23): Nach einer Atemübung geht es nahtlos in die stille
 * Meditation – ohne Countdown und mit einem Gong, nicht zweien ([MeditationEntry]), mit Gong und Stimme wie zuletzt im Tab Meditation ([meditationConfig]; die Stimme
 * wendet der [MeditationController] selbst an). Die Dauer kommt aus dem Blatt „Anpassen“ der Übung
 * ([SessionUiState.silenceMinutes], Eigentümer 2026-11-24), immer als feste Zeit; ohne Wahl dort
 * gilt auch die Dauer (oder die offene Sitzung) des Tabs. Der Übergang hängt am Ablauf, nicht an
 * der Oberfläche: Der [BreathSessionController] meldet „fertig“ ([BreathSessionController.finished]),
 * dieser Koordinator startet die Meditation – auch hinter dem Sperrbildschirm sofort statt verspätet
 * beim nächsten Sichtbarwerden der App. Der Logbuch-Eintrag der Atemübung steht dann schon.
 *
 * Am Ende, wenn die Übung es wünscht ([SessionUiState.silenceAfter]: Startwert aus dem Blatt
 * „Anpassen“ bzw. der Einstellung, in der Übung oben umschaltbar). Der Ende-Screen bietet die Stille
 * nicht mehr an (Eigentümer 2026-11-25: nur das Nötigste) – sie wählt man vorher. Nie nach Übungen mit
 * [app.atemkraft.domain.Exercise.silenceAfterAllowed] = false (Atmen im Gehen) und nie über eine
 * laufende Meditation hinweg; eine schon beendete, die noch am Ende-Screen steht (auch minimiert),
 * räumt der Übergang weg, statt stumm nichts zu tun.
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
    private val meditationConfig: Flow<MeditationConfig>,
    private val canStartNow: () -> Boolean,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) {
    private val scope = supervisedScope(dispatcher)

    private val _handedOver = MutableStateFlow(false)

    /**
     * Gesetzt nach einem Übergang, bis die Oberfläche ihn mit [consumeHandover] abholt. Beobachtbar,
     * weil der Status der Meditation beim Übergang von einem beendeten Ende-Screen aus „aktiv“ bleibt
     * und die Oberfläche an ihm allein den Übergang nicht sähe.
     */
    val handedOver: StateFlow<Boolean> = _handedOver.asStateFlow()

    init {
        scope.launch {
            breath.finished.collect { exercise ->
                if (exercise.silenceAfterAllowed && breath.state.value.silenceAfter) enter()
            }
        }
    }

    /**
     * Gab es seit dem letzten Aufruf einen Übergang? Die Oberfläche übernimmt dann den Zustand des
     * Session-Overlays (offen oder minimiert) für die Meditation – einmal je Übergang.
     */
    fun consumeHandover(): Boolean = _handedOver.value.also { _handedOver.value = false }

    /** Frei für die Stille: keine Meditation oder nur eine schon beendete (Ende-Screen). */
    private fun meditationFree(): Boolean = meditation.state.value.status.let { it == MeditationStatus.IDLE || it == MeditationStatus.FINISHED }

    private suspend fun enter() {
        if (!meditationFree()) return
        val config = meditationConfig.first().let { tab ->
            breath.state.value.silenceMinutes?.let { tab.copy(mode = MeditationMode.TIMED, minutes = it) } ?: tab
        }
        // Nach dem Lesen erneut prüfen: Inzwischen kann die Atem-Session verworfen oder neu
        // gestartet worden sein; dann gibt es nichts mehr zu übergeben.
        if (breath.state.value.status != SessionStatus.FINISHED) return
        if (!meditationFree() || !canStartNow()) return
        // Ein stehender Ende-Screen der Meditation weicht (ihr Eintrag steht schon im Logbuch).
        if (meditation.state.value.status == MeditationStatus.FINISHED) meditation.end()
        // Ein Bogen statt zweier Abläufe (Eigentümer 2026-11-25): kein Countdown; klingt der
        // Abschluss-Gong der Atemübung noch, ist er der Beginn der Stille, sonst schlägt die Stille
        // ihren eigenen Start-Gong (wenn eingestellt). So klingen nie zwei Gongs übereinander.
        val entry = if (breath.finishGongRemainingMs() > 0L) MeditationEntry.ON_BREATH_GONG else MeditationEntry.HANDOVER
        // Erst die Meditation (übernimmt den laufenden Foreground-Service), dann die Atem-Session
        // verwerfen (gibt ihren Halter frei) – in dieser Reihenfolge endet der Service nie dazwischen.
        meditation.start(config, entry)
        _handedOver.value = true
        breath.stop()
    }
}
