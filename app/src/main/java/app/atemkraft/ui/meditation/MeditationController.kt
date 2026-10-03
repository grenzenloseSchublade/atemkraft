package app.atemkraft.ui.meditation

import android.app.NotificationManager
import android.content.Context
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import app.atemkraft.data.LogbookRepository
import app.atemkraft.data.MeditationCues
import app.atemkraft.data.SettingsRepository
import app.atemkraft.domain.MeditationConfig
import app.atemkraft.domain.MeditationMode
import app.atemkraft.domain.SessionKind
import app.atemkraft.domain.SessionLogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.random.Random

enum class MeditationStatus { IDLE, PREPARING, RUNNING, PAUSED, FINISHED }

/**
 * Sichtbarer Zustand einer Meditations-Sitzung. Im [MeditationMode.TIMED] zählt
 * [remainingMs] herunter; im [MeditationMode.FREE] zählt [elapsedMs] hoch.
 */
data class MeditationUiState(
    val mode: MeditationMode = MeditationMode.TIMED,
    val status: MeditationStatus = MeditationStatus.IDLE,
    val remainingMs: Long = 0L,
    val elapsedMs: Long = 0L,
    /** Verbleibende Sekunden im Start-Countdown (nur bei [MeditationStatus.PREPARING]). */
    val countdown: Int = 0,
    /** Transienter In-Session-Stummschalter (übersteuert Gong/Sprache, ohne die Einstellung zu ändern). */
    val muted: Boolean = false,
)

/**
 * App-weiter Ablauf einer Meditation. Bewusst **nicht** an ein ViewModel/den UI-Lifecycle
 * gebunden, sondern an einen eigenen [scope] – zusammen mit dem [MeditationService]
 * (Foreground) läuft der Timer damit zuverlässig weiter, auch wenn der Bildschirm aus ist
 * oder die Activity im Hintergrund pausiert. An die Monotonuhr verankert (kein Drift).
 * Gongs/Sprache laufen über den [MeditationAudioCoordinator].
 */
class MeditationController(
    context: Context,
    private val settingsRepository: SettingsRepository,
    private val logbook: LogbookRepository,
    private val audio: MeditationAudioCoordinator,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(MeditationUiState())
    val state: StateFlow<MeditationUiState> = _state.asStateFlow()

    /** Verfügbarkeit einer deutschen TTS-Stimme (steuert den Sprach-Schalter im UI). */
    val speechAvailable: StateFlow<Boolean> = audio.speechAvailable

    /** Ist die hochwertige Piper-Stimme tatsächlich einsatzbereit? (fürs UI/Preview-Routing) */
    val piperReady = audio.piperReady

    private var currentConfig = MeditationConfig()
    private var totalMs = 0L
    private var runnerJob: Job? = null

    private var resumeRemainingMs: Long? = null
    private var resumeElapsedMs: Long? = null

    private var startedAtEpochMs = 0L
    private var activeElapsedMs = 0L
    private var lastTickRealtime = 0L

    // Intervall-Gong an der AKTIVEN Zeit verankert (pausenbereinigt), damit Pausen nicht driften.
    private var gongIntervalMs = 0L
    private var nextGongAtMs = Long.MAX_VALUE

    /** Verhindert doppeltes finish() (schneller Doppel-Tipp / Notification + Screen). */
    private var finishing = false

    init {
        scope.launch {
            settingsRepository.cueSettings.collect { audio.updateVolume(it.volume) }
        }
        // Persistierte neuronale Stimmen-Wahl anwenden (lädt sie, sobald installiert).
        scope.launch {
            settingsRepository.neuralVoiceId.collect { audio.setActiveVoice(it) }
        }
        // Gong-Ausklang (lang/kurz) übernehmen.
        scope.launch {
            settingsRepository.gongLong.collect { audio.setGongLong(it) }
        }
    }

    /** TTS-Engine lazy vorbereiten – erst beim Betreten des Meditations-Tabs, nicht beim App-Start. */
    fun prepareSpeech() = audio.prepareSpeech()

    /** Aktive neuronale Stimme wählen (null = keine) und persistieren. */
    fun selectNeuralVoice(voiceId: String?) {
        audio.setActiveVoice(voiceId)
        scope.launch { settingsRepository.setNeuralVoiceId(voiceId) }
    }

    /**
     * Stimme entfernen. War sie aktiv, zuerst abwählen (persistiert null, basierend auf dem echten
     * Engine-Zustand – nicht auf einem evtl. verzögerten UI-Wert), dann Engine freigeben + löschen.
     */
    fun deleteNeuralVoice(voiceId: String) {
        if (audio.isActiveVoice(voiceId)) selectNeuralVoice(null)
        audio.deleteVoice(voiceId)
    }

    /** Dieselbe Sitzung noch einmal starten (vom „Nochmal" auf dem Abschluss-Screen). */
    fun restart() = start(currentConfig)

    /** Transienter In-Session-Stummschalter (ohne die Einstellung zu ändern). */
    fun toggleMute() {
        val newMuted = !_state.value.muted
        audio.setMuted(newMuted)
        _state.update { it.copy(muted = newMuted) }
    }

    /** Erzeugt die Sitzung überhaupt Ton? (sonst keinen Audio-Fokus anfordern, fremde Medien nicht ducken). */
    private fun producesAudio(): Boolean = currentConfig.startEndGong || currentConfig.gongEveryMin != null ||
        (currentConfig.speech && speechAvailable.value)

    fun start(config: MeditationConfig) {
        runnerJob?.cancel()
        finishing = false
        audio.setMuted(false) // neue Sitzung startet unstumm
        currentConfig = config
        totalMs = if (config.mode == MeditationMode.TIMED) config.minutes * 60_000L else 0L
        gongIntervalMs = (config.gongEveryMin ?: 0) * 60_000L
        nextGongAtMs = if (gongIntervalMs > 0L) gongIntervalMs else Long.MAX_VALUE
        resumeRemainingMs = null
        resumeElapsedMs = null
        startedAtEpochMs = System.currentTimeMillis()
        activeElapsedMs = 0L
        _state.value = MeditationUiState(
            mode = config.mode,
            status = MeditationStatus.PREPARING,
            countdown = COUNTDOWN_SECONDS,
        )
        // Foreground-Service startet die Dauer-Notification und hält den Prozess wach.
        ContextCompat.startForegroundService(appContext, MeditationService.startIntent(appContext))
        launchActive(fromResume = false)
    }

    fun pause() {
        // Läuft bereits der Abschluss (Timer natürlich ausgelaufen), Pause ignorieren – sonst
        // würde der finish()-Job mitten in Gong/Logbuch abgebrochen („0:00 · Pausiert"-Hänger,
        // bei Resume Doppel-Gong/-Eintrag).
        if (finishing) return
        if (_state.value.status != MeditationStatus.RUNNING) return
        runnerJob?.cancel()
        accrueActiveTime() // aktive Zeit bis zum Pausenzeitpunkt banken, bevor die Uhr einfriert
        if (currentConfig.mode == MeditationMode.TIMED) {
            resumeRemainingMs = _state.value.remainingMs
        } else {
            resumeElapsedMs = _state.value.elapsedMs
        }
        audio.stopSpeech()
        // Fokus während der Pause abgeben, damit fremde Medien nicht dauerhaft gedämpft bleiben;
        // resume() fordert ihn über launchActive() wieder an.
        audio.abandonFocus()
        _state.update { it.copy(status = MeditationStatus.PAUSED) }
    }

    fun resume() {
        if (_state.value.status != MeditationStatus.PAUSED) return
        _state.update { it.copy(status = MeditationStatus.RUNNING) }
        launchActive(fromResume = true)
    }

    /**
     * „Beenden"/Verwerfen: RUNNING/PAUSED im FREE-Modus → regulärer Abschluss (End-Gong,
     * Logbuch); ansonsten (TIMED, Countdown, bereits beendeter/leerer Zustand) → Abbruch bzw.
     * Rückkehr in die Auswahl. Ein bereits FINISHED-Zustand wird nur verworfen (kein
     * erneutes finish() → keine Doppel-Einträge/-Gongs).
     */
    fun end() {
        val status = _state.value.status
        when {
            status == MeditationStatus.RUNNING || status == MeditationStatus.PAUSED ->
                if (currentConfig.mode == MeditationMode.FREE) finishNow() else abort()

            else -> abort() // IDLE, PREPARING, FINISHED → nur verwerfen/zurücksetzen
        }
    }

    /** Vollständiger Abbruch ohne Abschluss (zurück in den Auswahlzustand). */
    fun stop() = abort()

    private fun abort() {
        runnerJob?.cancel()
        finishing = false
        audio.stopSpeech()
        audio.abandonFocus()
        resumeRemainingMs = null
        resumeElapsedMs = null
        // Zuerst IDLE setzen, dann Service/Notification entfernen: So kann der Notification-
        // Collector im Service (er überspringt IDLE) nach unserem cancel() nichts mehr nachposten.
        _state.value = MeditationUiState()
        stopService()
    }

    private fun finishNow() {
        if (finishing) return
        finishing = true
        runnerJob?.cancel()
        runnerJob = scope.launch { finish() }
    }

    private fun stopService() {
        appContext.stopService(MeditationService.startIntent(appContext))
        // Backstop: Die Notification app-seitig direkt entfernen. Der Service räumt sie in
        // onDestroy zwar selbst weg, aber dessen Teardown-Timing ist herstellerabhängig
        // (v. a. Samsung) – dieser Aufruf stellt sicher, dass keine Waise zurückbleibt.
        appContext.getSystemService<NotificationManager>()?.cancel(MeditationService.NOTIF_ID)
    }

    private fun launchActive(fromResume: Boolean) {
        runnerJob = scope.launch {
            if (producesAudio()) audio.requestFocus()
            if (!fromResume) {
                runCountdown()
                if (currentConfig.startEndGong) audio.gong() // Start-Gong nach dem Countdown
            }
            val cfg = currentConfig
            lastTickRealtime = SystemClock.elapsedRealtime()
            coroutineScope {
                // Intervall-Gongs laufen in der Haupt-Tick-Schleife (maybeGong), nicht als
                // eigener Loop – so bleiben sie an der aktiven Zeit verankert und driften nicht.
                val speechJob = if (cfg.speech && speechAvailable.value) {
                    launch { speechLoop(fromResume) }
                } else {
                    null
                }
                if (cfg.mode == MeditationMode.TIMED) {
                    runTimed()
                    speechJob?.cancel()
                } else {
                    runFree() // läuft bis zum Abbruch/Beenden (Job-Cancel)
                }
            }
            // Nur der TIMED-Modus endet von selbst; FREE endet über finishNow().
            // finishing SOFORT setzen (nicht erst in finish()): sperrt pause() gegen den Race
            // „Pause-Tipp im Moment des natürlichen Endes".
            if (cfg.mode == MeditationMode.TIMED) {
                finishing = true
                finish()
            }
        }
    }

    private suspend fun runCountdown() {
        for (n in COUNTDOWN_SECONDS downTo 1) {
            _state.update { it.copy(status = MeditationStatus.PREPARING, countdown = n) }
            delay(1000L)
        }
    }

    private suspend fun runTimed() {
        val remaining = resumeRemainingMs ?: totalMs
        resumeRemainingMs = null
        val deadline = SystemClock.elapsedRealtime() + remaining
        while (true) {
            val left = deadline - SystemClock.elapsedRealtime()
            if (left <= 0L) break
            accrueActiveTime()
            maybeGong()
            _state.update {
                it.copy(
                    status = MeditationStatus.RUNNING,
                    remainingMs = left,
                    elapsedMs = totalMs - left,
                )
            }
            delay(min(left, FRAME_MS))
        }
        _state.update { it.copy(remainingMs = 0L, elapsedMs = totalMs) }
    }

    private suspend fun runFree() {
        val startElapsed = resumeElapsedMs ?: 0L
        resumeElapsedMs = null
        val base = SystemClock.elapsedRealtime() - startElapsed
        while (true) {
            accrueActiveTime()
            maybeGong()
            _state.update {
                it.copy(
                    status = MeditationStatus.RUNNING,
                    elapsedMs = SystemClock.elapsedRealtime() - base,
                )
            }
            delay(TICK_MS)
        }
    }

    /**
     * Intervall-Gong an der aktiven (pausenbereinigten) Zeit: feuert, sobald [activeElapsedMs]
     * die nächste n·[gongIntervalMs]-Marke erreicht. Im TIMED-Modus nicht am/hinter dem Ende
     * (dort übernimmt der End-Gong). Pausen driften dadurch nicht mehr.
     */
    private fun maybeGong() {
        if (gongIntervalMs <= 0L) return
        while (activeElapsedMs >= nextGongAtMs) {
            if (currentConfig.mode == MeditationMode.TIMED && nextGongAtMs >= totalMs) {
                nextGongAtMs = Long.MAX_VALUE
                break
            }
            audio.gong()
            nextGongAtMs += gongIntervalMs
        }
    }

    /** Sprach-Anleitung in unregelmäßigen Abständen; ein Satz früh, dann spärlicher. */
    private suspend fun speechLoop(fromResume: Boolean) {
        var last = -1
        delay(
            if (fromResume) {
                Random.nextLong(GAP_MIN_MS, GAP_MAX_MS)
            } else {
                Random.nextLong(FIRST_MIN_MS, FIRST_MAX_MS)
            },
        )
        while (true) {
            val idx = nextCueIndex(last)
            last = idx
            audio.speak(MeditationCues.de[idx])
            delay(Random.nextLong(GAP_MIN_MS, GAP_MAX_MS))
        }
    }

    private fun nextCueIndex(last: Int): Int {
        val size = MeditationCues.de.size
        if (size <= 1) return 0
        var idx = Random.nextInt(size)
        if (idx == last) idx = (idx + 1) % size
        return idx
    }

    private suspend fun finish() {
        // Aus der Pause beendet: pause() hat die aktive Zeit bereits gebankt – nicht erneut
        // (sonst würde die gesamte Pausendauer als Meditationszeit mitgezählt).
        if (_state.value.status != MeditationStatus.PAUSED) accrueActiveTime()
        audio.stopSpeech() // eine ggf. noch laufende Ansage nicht über den End-Gong sprechen lassen
        // Nur für einen tatsächlich hörbaren End-Gong den Fokus (erneut) anfordern.
        if (currentConfig.startEndGong && !_state.value.muted) audio.requestFocus()
        if (currentConfig.startEndGong) audio.gong()
        // Nur nennenswerte Sitzungen protokollieren (verhindert Ein-Sekunden-Einträge).
        if (activeElapsedMs >= MIN_LOG_MS) {
            logbook.append(
                SessionLogEntry(
                    exerciseId = MEDITATION_ID,
                    exerciseName = MEDITATION_NAME,
                    family = null,
                    startedAtEpochMs = startedAtEpochMs,
                    durationMs = activeElapsedMs,
                    roundsCompleted = 0,
                    kind = SessionKind.MEDITATION,
                ),
            )
        }
        _state.update { it.copy(status = MeditationStatus.FINISHED, remainingMs = 0L) }
        // Foreground-Service erst NACH dem Ausklingen des Gongs beenden, damit er auch bei
        // gesperrtem Bildschirm nicht abgeschnitten wird; ohne End-Gong entfällt die Wartezeit.
        if (currentConfig.startEndGong) delay(audio.gongTotalMs().toLong() + FINISH_ABANDON_MARGIN_MS)
        audio.abandonFocus()
        stopService()
    }

    /** Verstrichene aktive Zeit fortschreiben (Pausen zählen nicht, da beim Fortsetzen neu gesetzt). */
    private fun accrueActiveTime() {
        val now = SystemClock.elapsedRealtime()
        activeElapsedMs += now - lastTickRealtime
        lastTickRealtime = now
    }

    companion object {
        private const val COUNTDOWN_SECONDS = 3

        /** Kennung/Name der Meditation im Logbuch (App ist deutschsprachig). */
        private const val MEDITATION_ID = "meditation"
        private const val MEDITATION_NAME = "Meditation"

        /** Mindest-Dauer, ab der eine Sitzung ins Logbuch kommt. */
        private const val MIN_LOG_MS = 10_000L

        /** ~30 fps für den Countdown-Ring; schonend für den Akku. */
        private const val FRAME_MS = 33L

        /** Stoppuhr-Tick im FREE-Modus (5 fps genügt für Sekundenanzeige). */
        private const val TICK_MS = 200L

        /** Erst-Ansage früh (Ankommen), dann größere Abstände. */
        private const val FIRST_MIN_MS = 20_000L
        private const val FIRST_MAX_MS = 40_000L
        private const val GAP_MIN_MS = 45_000L
        private const val GAP_MAX_MS = 120_000L

        /**
         * Reserve über die Gong-Gesamtdauer hinaus, bevor Fokus/Service enden – damit der End-Gong
         * (Ton + Stille-Puffer, je nach Profil) sicher komplett ausklingt.
         */
        private const val FINISH_ABANDON_MARGIN_MS = 500L
    }
}

/** m:ss-Format der Meditations-Zeiten (geteilt von Screen, Mini-Leiste und Service). */
internal fun formatMeditationTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
