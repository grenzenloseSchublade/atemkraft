package app.atemkraft.playback

import android.os.SystemClock
import app.atemkraft.cue.CueEvent
import app.atemkraft.cue.toCueEvent
import app.atemkraft.data.LogbookRepository
import app.atemkraft.domain.BreathLevel
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.PhaseDuration
import app.atemkraft.domain.PhaseType
import app.atemkraft.domain.RuntimePhase
import app.atemkraft.domain.SessionClock
import app.atemkraft.domain.SessionConfig
import app.atemkraft.domain.SessionLogEntry
import app.atemkraft.domain.SessionProgress
import app.atemkraft.domain.SessionSummary
import app.atemkraft.domain.SessionTiming
import app.atemkraft.domain.adjusted
import app.atemkraft.domain.buildTimeline
import app.atemkraft.domain.sessionSummary
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min

/** Ablauf-Zustand einer Atem-Session; der [SessionAudioCoordinator] und die UI folgen ihm. */
enum class SessionStatus { IDLE, PREPARING, RUNNING, PAUSED, WAITING_FOR_USER, FINISHED }

/** Audio-Info zum Phasenbeginn für den durchgehenden Ton (Typ + Dauer; open = ohne feste Dauer). */
data class PhaseAudio(val type: PhaseType, val durationMs: Long, val open: Boolean, val level: BreathLevel? = null)

/**
 * Sichtbarer Zustand der laufenden Session. Die UI ist eine reine Funktion davon –
 * insbesondere wird der Atemkreis aus [phaseType], [remainingMs] und [phaseTotalMs]
 * abgeleitet (kein separater Animations-Timer, damit nichts auseinanderläuft und
 * Pause die Animation einfriert).
 */
data class SessionUiState(
    val exerciseName: String = "",
    val status: SessionStatus = SessionStatus.IDLE,
    val phaseType: PhaseType? = null,
    /** Füllstand für Atmen in Stufen (Viloma); null = Kreis nach [phaseType]. */
    val phaseLevel: BreathLevel? = null,
    val phaseLabel: String? = null,
    val phaseNote: String? = null,
    /** Countdown der aktuellen Phase mit fester Dauer (ms). */
    val remainingMs: Long = 0L,
    /** Gesamtdauer der aktuellen Phase mit fester Dauer (ms); 0 bei offenen Phasen. */
    val phaseTotalMs: Long = 0L,
    /** Hochzählende verstrichene Zeit bei offenen Phasen (ms). */
    val elapsedMs: Long = 0L,
    val roundIndex: Int = 0,
    val roundCount: Int = 1,
    /** Verbleibende Sekunden im Start-Countdown (nur bei [SessionStatus.PREPARING]). */
    val countdown: Int = 0,
    /** Typ der kommenden Phase (für die dezente „Als Nächstes"-Anzeige); null am Ende. */
    val nextPhaseType: PhaseType? = null,
    /** Kurze Muster-Anleitung (für die Übersicht im Start-Countdown). */
    val patternHint: String? = null,
    /** Transienter In-Session-Stummschalter (übersteuert den Ton, nicht die Einstellung). */
    val muted: Boolean = false,
    /** Fortschritt der Gesamtsitzung 0..1 (Zeit bzw. Runden, siehe [SessionProgress]); steht in der Pause still. */
    val progress: Float = 0f,
    /** Bilanz für den Ende-Bildschirm; nur bei [SessionStatus.FINISHED] gesetzt. */
    val summary: SessionSummary? = null,
    /** Darf nach dieser Übung die stille Meditation folgen ([Exercise.silenceAfterAllowed])? */
    val silenceAfterAllowed: Boolean = false,
)

/**
 * App-weiter Ablauf einer Atemübung, gebaut wie der [MeditationController]: Er hängt an einem
 * eigenen [scope] im AppContainer statt an einem ViewModel der Activity. Damit übersteht die Session
 * Rotation, das Beenden der Activity (Zurück auf einem Tab) und das Wegwischen der App; zusammen mit
 * dem [PlaybackService] (Foreground, `mediaPlayback`) laufen Takt und Töne bei ausgeschaltetem
 * Bildschirm und nach der Home-Taste weiter. Phasen mit fester Dauer sind an die Monotonuhr [now]
 * verankert (in der App [SystemClock.elapsedRealtime], im Test die virtuelle Zeit), sodass sich kein
 * Drift aufsummiert; Uhr und Dispatcher sind für Tests injizierbar.
 *
 * Am natürlichen Ende schreibt er den Logbuch-Eintrag selbst und meldet die Übung über [finished] –
 * daran hängt „Danach in die Stille“ ([SilenceAfterBreath]), unabhängig davon, ob die Oberfläche
 * gerade sichtbar ist.
 */
class BreathSessionController(
    private val audio: BreathAudio,
    private val logbook: LogbookRepository?,
    private val service: ForegroundLink,
    now: () -> Long = SystemClock::elapsedRealtime,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    /** Countdown und aktive Atemzeit, geteilt mit der Meditation ([SessionClock]). */
    private val clock = SessionClock(now)

    private val _state = MutableStateFlow(SessionUiState())
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    /** Nur der Status, ohne die Frame-Ticks des vollen Zustands – für die App-Wurzel (ARC-3). */
    val status: StateFlow<SessionStatus> = _state.map { it.status }
        .stateIn(scope, SharingStarted.Eagerly, SessionStatus.IDLE)

    private val cueEvents = MutableSharedFlow<CueEvent>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Phasenbeginn-Events für den durchgehenden Ton. */
    private val phaseEvents = MutableSharedFlow<PhaseAudio>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val _finished = MutableSharedFlow<Exercise>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Meldet die (angepasste) Übung, sobald sie von selbst zu Ende gelaufen ist – nicht bei [stop]. */
    val finished: SharedFlow<Exercise> = _finished.asSharedFlow()

    init {
        // Audio folgt dem Session-Zustand in diesem Scope statt der UI – läuft damit auch hinter
        // dem Sperrbildschirm weiter und räumt am Session-Ende zuverlässig auf.
        audio.bind(
            scope = scope,
            status = status,
            cues = cueEvents.asSharedFlow(),
            phaseAudio = phaseEvents.asSharedFlow(),
            muted = _state.map { it.muted },
        )
    }

    private var timeline: List<RuntimePhase> = emptyList()
    private var progressTracker = SessionProgress(emptyList())
    private var index = 0
    private var runnerJob: Job? = null

    /** Gibt den Foreground-Service nach dem Ausklingen des Abschluss-Gongs frei. */
    private var releaseJob: Job? = null

    /** Beim Fortsetzen aus der Pause: Restzeit der aktuellen Phase (sonst null). */
    private var resumeRemainingMs: Long? = null

    /** Signal zum Beenden einer offenen Phase (Nutzer tippt „weiter"). */
    private var continueSignal: CompletableDeferred<Unit>? = null

    // Für Neustart („Von vorne"): zuletzt gewählte Übung (unangepasst) + Anpassung.
    private var baseExercise: Exercise? = null
    private var currentConfig: SessionConfig = SessionConfig()

    // Logbuch-Metadaten der laufenden Session.
    private var currentExercise: Exercise? = null
    private var startedAtEpochMs = 0L

    /**
     * Startet [exercise] mit der Anpassung [config]. Die Übung kommt als aufgelöstes Objekt von der
     * Detailseite – genau das Muster, das dort zu sehen war; „Von vorne“ nimmt dasselbe Objekt
     * (kein erneutes Nachschlagen per Id, das um Mitternacht ein anderes Tagesmuster liefern könnte).
     * Startet nur aus der sichtbaren App (Start-Knopf, „Nochmal“): Den Foreground-Service darf
     * Android ab 12 nicht aus dem Hintergrund starten.
     */
    fun start(exercise: Exercise, config: SessionConfig = SessionConfig()) {
        val adjusted = exercise.adjusted(config)
        runnerJob?.cancel()
        releaseJob?.cancel()
        baseExercise = exercise
        currentConfig = config
        timeline = adjusted.buildTimeline()
        progressTracker = SessionProgress(timeline)
        index = 0
        resumeRemainingMs = null
        currentExercise = adjusted
        startedAtEpochMs = System.currentTimeMillis()
        clock.reset()
        _state.value = SessionUiState(
            exerciseName = adjusted.name,
            status = SessionStatus.PREPARING,
            roundCount = adjusted.rounds,
            countdown = SessionTiming.COUNTDOWN_SECONDS,
            patternHint = adjusted.instructionHint,
            silenceAfterAllowed = adjusted.silenceAfterAllowed,
        )
        // Foreground-Service: Dauer-Benachrichtigung, Prozess und CPU bleiben wach (idempotent).
        service.start()
        if (timeline.isEmpty()) {
            finish()
            return
        }
        runnerJob = scope.launch {
            runCountdown()
            runPhaseLoop()
        }
    }

    /** Startet dieselbe Übung mit derselben Anpassung von vorn (inkl. Countdown). */
    fun restart() {
        baseExercise?.let { start(it, currentConfig) }
    }

    private suspend fun runCountdown() = clock.countdown { n ->
        _state.update {
            it.copy(status = SessionStatus.PREPARING, countdown = n, phaseType = null, phaseNote = null)
        }
    }

    fun pause() {
        if (_state.value.status != SessionStatus.RUNNING) return
        runnerJob?.cancel()
        resumeRemainingMs = _state.value.remainingMs
        _state.update { it.copy(status = SessionStatus.PAUSED) }
    }

    fun resume() {
        if (_state.value.status != SessionStatus.PAUSED) return
        _state.update { it.copy(status = SessionStatus.RUNNING) }
        launchRunner()
    }

    /** Beendet eine offene Phase ([PhaseDuration.OpenEnded]/[PhaseDuration.UntilUrge]). */
    fun continueFromUserPaced() {
        continueSignal?.complete(Unit)
    }

    /** Schaltet den Ton der laufenden Session schnell stumm/wieder an (nur transient). */
    fun toggleMute() {
        _state.update { it.copy(muted = !it.muted) }
    }

    /** Beendet bzw. verwirft die Session (auch den Ende-Screen) und gibt den Service frei. */
    fun stop() {
        runnerJob?.cancel()
        releaseJob?.cancel()
        continueSignal?.cancel()
        continueSignal = null
        index = 0
        resumeRemainingMs = null
        currentExercise = null
        // Zuerst IDLE, dann den Service lösen: Sein Benachrichtigungs-Collector überspringt IDLE und
        // postet nach dem Wegräumen nichts mehr nach (wie beim MeditationController).
        _state.update { SessionUiState() }
        service.stop()
    }

    private fun launchRunner() {
        runnerJob?.cancel()
        runnerJob = scope.launch { runPhaseLoop() }
    }

    private suspend fun runPhaseLoop() {
        clock.resume() // Pausen zählen nicht zur aktiven Atemzeit
        // Fortlaufender Zeit-Anker über feste Phasen hinweg: Jede Phase startet dort, wo die
        // vorherige rechnerisch endete – sonst erbt jede Phase bis zu FRAME_MS Überschuss und
        // lange Abläufe (~190 Phasen bei Feueratmung) laufen Sekunden zu lang.
        var nextAnchorMs = clock.now()
        while (index < timeline.size) {
            val phase = timeline[index]
            // Durchgehender Ton: Phasenbeginn (auch beim Fortsetzen, damit der Ton wieder anläuft).
            val fixedMs = (phase.duration as? PhaseDuration.Fixed)?.let { resumeRemainingMs ?: it.millis } ?: 0L
            phaseEvents.tryEmit(PhaseAudio(phase.type, fixedMs, phase.duration !is PhaseDuration.Fixed, phase.level))
            // Cue nur zum echten Phasenbeginn, nicht beim Fortsetzen mitten in der Phase.
            if (resumeRemainingMs == null) {
                cueEvents.tryEmit(phase.type.toCueEvent())
            }
            when (val duration = phase.duration) {
                is PhaseDuration.Fixed -> {
                    val deadline = resumeRemainingMs?.let { clock.now() + it }
                        // Kleiner Catch-up-Deckel: Nach einer längeren Stall-Phase (Prozess
                        // eingefroren) nicht alle verpassten Phasen im Zeitraffer nachholen.
                        ?: (maxOf(nextAnchorMs, clock.now() - CATCHUP_SLACK_MS) + duration.millis)
                    runFixedPhase(phase, deadline, duration.millis)
                    nextAnchorMs = deadline
                }

                PhaseDuration.OpenEnded, PhaseDuration.UntilUrge -> {
                    runUserPacedPhase(phase)
                    nextAnchorMs = clock.now() // menschlich getaktet → neu ankern
                }
            }
            resumeRemainingMs = null
            index++
        }
        finish()
    }

    /** Typ der nächsten Phase im Ablauf (für die „Als Nächstes"-Anzeige). */
    private fun nextPhaseType(): PhaseType? = timeline.getOrNull(index + 1)?.type

    private suspend fun runFixedPhase(phase: RuntimePhase, deadline: Long, totalMs: Long) {
        while (true) {
            val left = deadline - clock.now()
            if (left <= 0L) break
            clock.accrue()
            _state.update {
                it.copy(
                    status = SessionStatus.RUNNING,
                    phaseType = phase.type,
                    phaseLevel = phase.level,
                    phaseLabel = phase.label,
                    phaseNote = phase.note,
                    remainingMs = left,
                    phaseTotalMs = totalMs,
                    elapsedMs = 0L,
                    roundIndex = phase.roundIndex,
                    roundCount = phase.roundCount,
                    nextPhaseType = nextPhaseType(),
                    progress = progressTracker.at(index, totalMs - left),
                )
            }
            delay(min(left, SessionTiming.FRAME_MS))
        }
        _state.update { it.copy(remainingMs = 0L) }
    }

    private suspend fun runUserPacedPhase(phase: RuntimePhase) = coroutineScope {
        val signal = CompletableDeferred<Unit>()
        continueSignal = signal
        val start = clock.now()
        _state.update {
            it.copy(
                status = SessionStatus.WAITING_FOR_USER,
                phaseType = phase.type,
                phaseLevel = phase.level,
                phaseLabel = phase.label,
                phaseNote = phase.note,
                remainingMs = 0L,
                phaseTotalMs = 0L,
                elapsedMs = 0L,
                roundIndex = phase.roundIndex,
                roundCount = phase.roundCount,
                nextPhaseType = nextPhaseType(),
                progress = progressTracker.at(index, 0L),
            )
        }
        val ticker = launch {
            while (isActive) {
                clock.accrue()
                _state.update { it.copy(elapsedMs = clock.now() - start) }
                delay(USER_PACED_TICK_MS)
            }
        }
        try {
            signal.await()
        } finally {
            ticker.cancel()
            continueSignal = null
        }
    }

    private fun finish() {
        clock.accrue()
        cueEvents.tryEmit(CueEvent.FINISH)
        // Logbuch direkt in diesem Scope schreiben (kein UI-Collector nötig) – der Eintrag geht
        // damit auch bei gesperrtem Bildschirm oder ohne Activity nicht verloren.
        val exercise = currentExercise
        if (exercise != null && logbook != null) {
            val entry = SessionLogEntry(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                family = exercise.family,
                startedAtEpochMs = startedAtEpochMs,
                durationMs = clock.activeMs,
                roundsCompleted = exercise.rounds,
            )
            scope.launch { logbook.append(entry) }
        }
        _state.update {
            it.copy(
                status = SessionStatus.FINISHED,
                phaseType = null,
                remainingMs = 0L,
                elapsedMs = 0L,
                progress = 1f,
                summary = sessionSummary(timeline, clock.activeMs),
            )
        }
        // Foreground-Service erst nach dem Ausklingen des Abschluss-Gongs lösen, damit er hinter dem
        // Sperrbildschirm nicht abgeschnitten wird. Übernimmt die Meditation („Danach in die
        // Stille“), hält sie den Service schon, bevor [stop] diesen Halter löst.
        val tailMs = audio.finishTailMs(muted = _state.value.muted)
        releaseJob = scope.launch {
            delay(tailMs)
            service.stop()
        }
        exercise?.let { _finished.tryEmit(it) }
    }

    private companion object {
        /** Max. Aufhol-Spielraum des Phasen-Ankers nach einem Stall (kein Zeitraffer-Nachholen). */
        const val CATCHUP_SLACK_MS = 250L

        /** Takt der hochzählenden Zeit in offenen Phasen (Zehntelsekunde genügt für die Anzeige). */
        const val USER_PACED_TICK_MS = 100L
    }
}

/** Die Audio-Bausteine, die der [BreathSessionController] braucht; in der App der [SessionAudioCoordinator]. */
interface BreathAudio {
    /** Ton, Haptik und Audio-Fokus an den Ablauf binden – einmal, im Scope des Controllers. */
    fun bind(
        scope: CoroutineScope,
        status: Flow<SessionStatus>,
        cues: Flow<CueEvent>,
        phaseAudio: Flow<PhaseAudio>,
        muted: Flow<Boolean>,
    )

    /** Wie lange der Abschluss-Gong noch klingt (ms, mit Reserve); 0 ohne hörbaren Gong. */
    fun finishTailMs(muted: Boolean): Long
}
