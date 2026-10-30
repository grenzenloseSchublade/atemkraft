package app.atemkraft.ui.screenshots

import androidx.compose.runtime.Composable
import app.atemkraft.data.Areas
import app.atemkraft.data.BuiltInExercises
import app.atemkraft.data.MeditationDisplay
import app.atemkraft.data.Pranayama
import app.atemkraft.data.SessionDisplay
import app.atemkraft.data.Situations
import app.atemkraft.domain.BreathingFamily
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.Feeling
import app.atemkraft.domain.PhaseDuration
import app.atemkraft.domain.RuntimePhase
import app.atemkraft.domain.SessionKind
import app.atemkraft.domain.SessionLogEntry
import app.atemkraft.domain.SessionProgress
import app.atemkraft.domain.buildTimeline
import app.atemkraft.domain.estimatedTotalSeconds
import app.atemkraft.playback.MeditationUiState
import app.atemkraft.playback.SessionStatus
import app.atemkraft.playback.SessionUiState
import app.atemkraft.ui.components.FeelingAsk
import app.atemkraft.ui.home.HomeActions
import app.atemkraft.ui.home.HomeCatalog
import app.atemkraft.ui.home.HomeScreen
import app.atemkraft.ui.learn.LearnScreen
import app.atemkraft.ui.meditation.MeditationActions
import app.atemkraft.ui.meditation.MeditationOverlay
import app.atemkraft.ui.own.OwnActions
import app.atemkraft.ui.own.OwnPatternsScreen
import app.atemkraft.ui.session.SessionActions
import app.atemkraft.ui.session.SessionOverlay
import java.time.ZoneId

/*
 * Szenen, die mehrere Screenshot-Tests teilen ([ScreenshotTest], [StoreScreenshotTest],
 * [TabMatrixTest]): die Tabs mit den eingebauten Daten und die Vollbild-Sitzungen in einem festen
 * Zustand.
 */

/**
 * Tab „Atmen“ mit allen angeleiteten Übungen, Anlässen, Bereichen und dem festen Muster des Tages;
 * mit [welcome] die Willkommenskarte des ersten Starts (Resonanz-Atmung).
 */
@Composable
internal fun BreatheTab(welcome: Boolean = false) {
    HomeScreen(
        catalog = HomeCatalog(
            exercisesByFamily = BreathingFamily.entries
                .map { f -> f to BuiltInExercises.all.filter { it.family == f && it.guided } }
                .filter { (_, list) -> list.isNotEmpty() },
            situations = Situations.all,
            areas = Areas.all,
            yogaExerciseIds = Pranayama.exerciseIds,
            resolve = { id -> BuiltInExercises.all.firstOrNull { it.id == id } },
        ),
        daily = ScreenshotHarness.daily,
        dailySaved = false,
        actions = HomeActions(onRegenerateDaily = {}, onDailySavedChange = {}, onSelect = {}, onOpenSettings = {}),
        welcome = if (welcome) BuiltInExercises.all.first { it.id == "resonanz" } else null,
    )
}

/**
 * Tab „Eigene“ mit Verlauf ([logEntries]), „Tut mir gut“ mit den zwei längsten Übungsnamen,
 * „Zuletzt: …“ mit dem längsten und den gespeicherten Mustern (eines mit langem Namen).
 */
@Composable
internal fun OwnTab() {
    val favorites = BuiltInExercises.all.sortedByDescending { it.name.length }.take(2)
    OwnPatternsScreen(
        savedPatterns = ScreenshotHarness.saved,
        actions = OwnActions(onNewPattern = {}, onSelect = {}, onDeletePattern = {}, onOpenSettings = {}),
        favorites = favorites,
        logEntries = logEntries(),
        lastExercise = favorites.first(),
        today = ScreenshotHarness.DAY,
    )
}

/** Tab „Lernen“ mit seinen Kapiteln. */
@Composable
internal fun LearnTab() = LearnScreen(onOpenSettings = {}, onOpenAtemlehre = {}, onOpenPranayama = {}, onOpenPrograms = {}, onOpenGlossary = {})

/**
 * Beispieleinträge für Logbuch und „Eigene“: relativ zum festen DAY, damit jede Gruppe vorkommt
 * (Heute, Gestern, Letzte 7 Tage, zwei Monate).
 */
internal fun logEntries(): List<SessionLogEntry> {
    fun at(daysAgo: Long, hour: Int, minute: Int): Long = ScreenshotHarness.DAY.minusDays(daysAgo).atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val daysAgo = listOf(0L, 1L, 3L, 5L, 12L, 40L)
    // Runden und Dauer wie vom BreathSessionController geloggt.
    return BuiltInExercises.all.take(6).mapIndexed { i, e ->
        SessionLogEntry(
            id = i + 1L,
            exerciseId = e.id,
            exerciseName = e.name,
            family = e.family,
            startedAtEpochMs = at(daysAgo[i], 7, 30),
            durationMs = if (e.rounds > 1) e.estimatedTotalSeconds() * 1_000L else (5 + i) * 60_000L + 5_000L,
            roundsCompleted = e.rounds,
            // Befinden an einigen Einträgen, das längste Wort dabei; die übrigen unbeantwortet.
            feeling = listOf(Feeling.CALMER, null, Feeling.SAME, Feeling.MORE_TIRED, null, Feeling.RESTLESS)[i],
        )
    } + SessionLogEntry(
        id = 99,
        exerciseId = "meditation",
        exerciseName = "Meditation",
        family = null,
        startedAtEpochMs = at(1, 21, 15),
        durationMs = 20 * 60_000L,
        roundsCompleted = 0,
        kind = SessionKind.MEDITATION,
        feeling = Feeling.SAME,
    )
}

/**
 * Zustand der laufenden Session wie im BreathSessionController (runFixedPhase/runUserPacedPhase): die
 * erste Phase des Ablaufs von [exercise], auf die [phase] zutrifft, davon [elapsedMs] verstrichen
 * (bei offenen Phasen hochgezählt).
 */
internal fun sessionAt(exercise: Exercise, elapsedMs: Long, phase: (RuntimePhase) -> Boolean): SessionUiState {
    val timeline = exercise.buildTimeline()
    val index = timeline.indexOfFirst(phase)
    val current = timeline[index]
    val totalMs = (current.duration as? PhaseDuration.Fixed)?.millis
    return SessionUiState(
        exerciseName = exercise.name,
        status = if (totalMs != null) SessionStatus.RUNNING else SessionStatus.WAITING_FOR_USER,
        phaseType = current.type,
        circle = current.circle,
        phaseLabel = current.label,
        phaseNote = current.note,
        remainingMs = totalMs?.let { it - elapsedMs } ?: 0L,
        phaseTotalMs = totalMs ?: 0L,
        elapsedMs = if (totalMs != null) 0L else elapsedMs,
        roundIndex = current.roundIndex,
        roundCount = current.roundCount,
        nextPhaseType = timeline.getOrNull(index + 1)?.type,
        nextPhaseLabel = timeline.getOrNull(index + 1)?.label,
        patternHint = exercise.instructionHint,
        progress = SessionProgress(timeline).at(index, if (totalMs != null) elapsedMs else 0L),
        // Kopfzeile mit „Danach in die Stille“ neben dem Ton (nicht bei Atmen im Gehen).
        silenceAfterAllowed = exercise.silenceAfterAllowed,
        silenceAfter = exercise.silenceAfterAllowed,
    )
}

/**
 * Vollbild-Session ohne automatische Ruhe, damit nichts von selbst ausblendet; mit [feelingAsk] fragt
 * der Ende-Screen nach dem Befinden.
 */
@Composable
internal fun SessionShot(state: SessionUiState, feelingAsk: FeelingAsk? = null) = SessionOverlay(
    state = state,
    display = SessionDisplay(autoCalm = false),
    actions = SessionActions(
        onTogglePause = {},
        onTapHaptic = {},
        onContinue = {},
        onRestart = {},
        onStop = {},
        onOpenLogbook = {},
        onIntoSilence = {},
        feelingAsk = feelingAsk,
    ),
    onMinimize = {},
    onToggleMute = {},
)

/** Vollbild-Meditation ohne automatische Ruhe; mit [feelingAsk] wie [SessionShot]. */
@Composable
internal fun MeditationShot(state: MeditationUiState, feelingAsk: FeelingAsk? = null) = MeditationOverlay(
    state = state,
    display = MeditationDisplay(autoCalm = false),
    actions = MeditationActions(onTogglePause = {}, onTapHaptic = {}, onRestart = {}, onEnd = {}, onOpenLogbook = {}, feelingAsk = feelingAsk),
    onMinimize = {},
    onToggleMute = {},
)
