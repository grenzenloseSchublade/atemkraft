package app.atemkraft.ui.session

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.atemkraft.AtemkraftApplication
import app.atemkraft.cue.CueEvent
import app.atemkraft.cue.toCueEvent
import app.atemkraft.data.ExerciseRepository
import app.atemkraft.data.LogbookRepository
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.PhaseDuration
import app.atemkraft.domain.PhaseType
import app.atemkraft.domain.RuntimePhase
import app.atemkraft.domain.SessionConfig
import app.atemkraft.domain.SessionLogEntry
import app.atemkraft.domain.adjusted
import app.atemkraft.domain.buildTimeline
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min

enum class SessionStatus { IDLE, PREPARING, RUNNING, PAUSED, WAITING_FOR_USER, FINISHED }

/** Audio-Info zum Phasenbeginn für den durchgehenden Ton (Typ + Dauer; open = ohne feste Dauer). */
data class PhaseAudio(val type: PhaseType, val durationMs: Long, val open: Boolean)

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
)

/**
 * Steuert den zeitlichen Ablauf einer Atemübung. Der Timer läuft im
 * [viewModelScope] und übersteht damit Rotation/Recomposition. Phasen mit fester
 * Dauer sind an die Monotonuhr ([SystemClock.elapsedRealtime]) verankert, sodass
 * sich kein Drift aufsummiert.
 */
class SessionViewModel(
    private val repository: ExerciseRepository = ExerciseRepository(),
    private val audio: SessionAudioCoordinator? = null,
    private val logbook: LogbookRepository? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(SessionUiState())
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    private val _cues = MutableSharedFlow<CueEvent>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val cues: SharedFlow<CueEvent> = _cues.asSharedFlow()

    /** Phasenbeginn-Events für den durchgehenden Ton. */
    private val _phaseAudio = MutableSharedFlow<PhaseAudio>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val phaseAudio: SharedFlow<PhaseAudio> = _phaseAudio.asSharedFlow()

    init {
        // Audio folgt dem Session-Zustand im viewModelScope statt der UI – läuft damit
        // auch hinter dem Sperrbildschirm weiter und räumt am Session-Ende zuverlässig auf.
        audio?.bind(
            scope = viewModelScope,
            status = _state.map { it.status },
            cues = cues,
            phaseAudio = phaseAudio,
            muted = _state.map { it.muted },
        )
    }

    override fun onCleared() {
        audio?.release()
    }

    private var timeline: List<RuntimePhase> = emptyList()
    private var index = 0
    private var runnerJob: Job? = null

    /** Beim Fortsetzen aus der Pause: Restzeit der aktuellen Phase (sonst null). */
    private var resumeRemainingMs: Long? = null

    /** Signal zum Beenden einer offenen Phase (Nutzer tippt „weiter"). */
    private var continueSignal: CompletableDeferred<Unit>? = null

    // Für Neustart („Von vorne"): zuletzt gewählte Übung + Anpassung.
    private var currentExerciseId: String = ""
    private var currentConfig: SessionConfig = SessionConfig()

    // Logbuch-Metadaten der laufenden Session.
    private var currentExercise: Exercise? = null
    private var startedAtEpochMs = 0L
    private var activeElapsedMs = 0L
    private var lastTickRealtime = 0L

    fun start(exerciseId: String, config: SessionConfig = SessionConfig()) {
        val exercise = (repository.byId(exerciseId) ?: return).adjusted(config)
        runnerJob?.cancel()
        currentExerciseId = exerciseId
        currentConfig = config
        timeline = exercise.buildTimeline()
        index = 0
        resumeRemainingMs = null
        currentExercise = exercise
        startedAtEpochMs = System.currentTimeMillis()
        activeElapsedMs = 0L
        _state.value = SessionUiState(
            exerciseName = exercise.name,
            status = SessionStatus.PREPARING,
            roundCount = exercise.rounds,
            countdown = COUNTDOWN_SECONDS,
            patternHint = exercise.instructionHint,
        )
        if (timeline.isEmpty()) {
            finish()
            return
        }
        runnerJob = viewModelScope.launch {
            runCountdown()
            runPhaseLoop()
        }
    }

    /** Startet dieselbe Übung mit derselben Anpassung von vorn (inkl. Countdown). */
    fun restart() {
        if (currentExerciseId.isNotEmpty()) start(currentExerciseId, currentConfig)
    }

    private suspend fun runCountdown() {
        for (n in COUNTDOWN_SECONDS downTo 1) {
            _state.update {
                it.copy(status = SessionStatus.PREPARING, countdown = n, phaseType = null, phaseNote = null)
            }
            delay(1000L)
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

    fun stop() {
        runnerJob?.cancel()
        continueSignal?.cancel()
        continueSignal = null
        index = 0
        resumeRemainingMs = null
        _state.update { SessionUiState() }
    }

    /** Zählt die seit dem letzten Tick verstrichene Zeit zur aktiven Atemzeit (Pausen zählen nicht,
     *  weil [lastTickRealtime] beim Fortsetzen neu gesetzt wird). */
    private fun accrueActiveTime() {
        val now = SystemClock.elapsedRealtime()
        activeElapsedMs += now - lastTickRealtime
        lastTickRealtime = now
    }

    private fun launchRunner() {
        runnerJob?.cancel()
        runnerJob = viewModelScope.launch { runPhaseLoop() }
    }

    private suspend fun runPhaseLoop() {
        lastTickRealtime = SystemClock.elapsedRealtime()
        // Fortlaufender Zeit-Anker über feste Phasen hinweg: Jede Phase startet dort, wo die
        // vorherige rechnerisch endete – sonst erbt jede Phase bis zu FRAME_MS Überschuss und
        // lange Abläufe (~190 Phasen bei Feueratmung) laufen Sekunden zu lang.
        var nextAnchorMs = SystemClock.elapsedRealtime()
        while (index < timeline.size) {
            val phase = timeline[index]
            // Durchgehender Ton: Phasenbeginn (auch beim Fortsetzen, damit der Ton wieder anläuft).
            val fixedMs = (phase.duration as? PhaseDuration.Fixed)?.let { resumeRemainingMs ?: it.millis } ?: 0L
            _phaseAudio.tryEmit(PhaseAudio(phase.type, fixedMs, phase.duration !is PhaseDuration.Fixed))
            // Cue nur zum echten Phasenbeginn, nicht beim Fortsetzen mitten in der Phase.
            if (resumeRemainingMs == null) {
                _cues.tryEmit(phase.type.toCueEvent())
            }
            when (val duration = phase.duration) {
                is PhaseDuration.Fixed -> {
                    val deadline = if (resumeRemainingMs != null) {
                        SystemClock.elapsedRealtime() + resumeRemainingMs!!
                    } else {
                        // Kleiner Catch-up-Deckel: Nach einer längeren Stall-Phase (Prozess
                        // eingefroren) nicht alle verpassten Phasen im Zeitraffer nachholen.
                        maxOf(nextAnchorMs, SystemClock.elapsedRealtime() - CATCHUP_SLACK_MS) +
                            duration.millis
                    }
                    runFixedPhase(phase, deadline, duration.millis)
                    nextAnchorMs = deadline
                }
                PhaseDuration.OpenEnded, PhaseDuration.UntilUrge -> {
                    runUserPacedPhase(phase)
                    nextAnchorMs = SystemClock.elapsedRealtime() // menschlich getaktet → neu ankern
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
            val left = deadline - SystemClock.elapsedRealtime()
            if (left <= 0L) break
            accrueActiveTime()
            _state.update {
                it.copy(
                    status = SessionStatus.RUNNING,
                    phaseType = phase.type,
                    phaseLabel = phase.label,
                    phaseNote = phase.note,
                    remainingMs = left,
                    phaseTotalMs = totalMs,
                    elapsedMs = 0L,
                    roundIndex = phase.roundIndex,
                    roundCount = phase.roundCount,
                    nextPhaseType = nextPhaseType(),
                )
            }
            delay(min(left, FRAME_MS))
        }
        _state.update { it.copy(remainingMs = 0L) }
    }

    private suspend fun runUserPacedPhase(phase: RuntimePhase) = coroutineScope {
        val signal = CompletableDeferred<Unit>()
        continueSignal = signal
        val start = SystemClock.elapsedRealtime()
        _state.update {
            it.copy(
                status = SessionStatus.WAITING_FOR_USER,
                phaseType = phase.type,
                phaseLabel = phase.label,
                phaseNote = phase.note,
                remainingMs = 0L,
                phaseTotalMs = 0L,
                elapsedMs = 0L,
                roundIndex = phase.roundIndex,
                roundCount = phase.roundCount,
                nextPhaseType = nextPhaseType(),
            )
        }
        val ticker = launch {
            while (isActive) {
                accrueActiveTime()
                _state.update { it.copy(elapsedMs = SystemClock.elapsedRealtime() - start) }
                delay(100L)
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
        accrueActiveTime()
        _cues.tryEmit(CueEvent.FINISH)
        // Logbuch direkt im viewModelScope schreiben (kein UI-Collector nötig) – der
        // Eintrag geht damit auch bei gesperrtem Bildschirm oder Recreation nicht verloren.
        currentExercise?.let { exercise ->
            val entry = SessionLogEntry(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                family = exercise.family,
                startedAtEpochMs = startedAtEpochMs,
                durationMs = activeElapsedMs,
                roundsCompleted = exercise.rounds,
            )
            logbook?.let { viewModelScope.launch { it.append(entry) } }
        }
        _state.update {
            it.copy(
                status = SessionStatus.FINISHED,
                phaseType = null,
                remainingMs = 0L,
                elapsedMs = 0L,
            )
        }
    }

    companion object {
        /** DI: zieht Repositories und Audio-Koordinator aus dem AppContainer/der Application. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AtemkraftApplication
                SessionViewModel(
                    repository = app.container.exerciseRepository,
                    audio = SessionAudioCoordinator(app, app.container.settingsRepository.cueSettings),
                    logbook = app.container.logbookRepository,
                )
            }
        }

        /** ~30 fps Tick – flüssig genug für Kreis + Countdown, schonend für den Akku. */
        private const val FRAME_MS = 33L

        /** Max. Aufhol-Spielraum des Phasen-Ankers nach einem Stall (kein Zeitraffer-Nachholen). */
        private const val CATCHUP_SLACK_MS = 250L

        /** Länge des Start-Countdowns in Sekunden. */
        private const val COUNTDOWN_SECONDS = 3
    }
}
