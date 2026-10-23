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
import app.atemkraft.domain.PhaseDuration
import app.atemkraft.domain.RuntimePhase
import app.atemkraft.domain.SessionProgress
import app.atemkraft.domain.buildTimeline
import app.atemkraft.playback.MeditationUiState
import app.atemkraft.playback.SessionStatus
import app.atemkraft.playback.SessionUiState
import app.atemkraft.ui.home.HomeScreen
import app.atemkraft.ui.meditation.MeditationOverlay
import app.atemkraft.ui.session.SessionOverlay

/*
 * Szenen, die mehrere Screenshot-Tests teilen ([ScreenshotTest], [StoreScreenshotTest]): Tab
 * „Atmen“ mit den eingebauten Daten und die Vollbild-Sitzungen in einem festen Zustand.
 */

/**
 * Tab „Atmen“ mit allen angeleiteten Übungen, Anlässen, Bereichen und dem festen Muster des Tages;
 * mit [welcome] die Willkommenskarte des ersten Starts (Resonanz-Atmung).
 */
@Composable
internal fun BreatheTab(welcome: Boolean = false) {
    HomeScreen(
        exercisesByFamily = BreathingFamily.entries
            .map { f -> f to BuiltInExercises.all.filter { it.family == f && it.guided } }
            .filter { (_, list) -> list.isNotEmpty() },
        situations = Situations.all,
        areas = Areas.all,
        resolve = { id -> BuiltInExercises.all.firstOrNull { it.id == id } },
        yogaExerciseIds = Pranayama.exerciseIds,
        daily = ScreenshotHarness.daily,
        dailySaved = false,
        onRegenerateDaily = {},
        onDailySavedChange = {},
        onSelect = {},
        onOpenSettings = {},
        welcome = if (welcome) BuiltInExercises.all.first { it.id == "resonanz" } else null,
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
        phaseLevel = current.level,
        phaseLabel = current.label,
        phaseNote = current.note,
        remainingMs = totalMs?.let { it - elapsedMs } ?: 0L,
        phaseTotalMs = totalMs ?: 0L,
        elapsedMs = if (totalMs != null) 0L else elapsedMs,
        roundIndex = current.roundIndex,
        roundCount = current.roundCount,
        nextPhaseType = timeline.getOrNull(index + 1)?.type,
        patternHint = exercise.instructionHint,
        progress = SessionProgress(timeline).at(index, if (totalMs != null) elapsedMs else 0L),
        // Kopfzeile mit „Danach in die Stille“ neben dem Ton (nicht bei Atmen im Gehen).
        silenceAfterAllowed = exercise.silenceAfterAllowed,
        silenceAfter = exercise.silenceAfterAllowed,
    )
}

/** Vollbild-Session ohne automatische Ruhe, damit nichts von selbst ausblendet. */
@Composable
internal fun SessionShot(state: SessionUiState) = SessionOverlay(
    state = state,
    display = SessionDisplay(autoCalm = false),
    onMinimize = {},
    onToggleMute = {},
    onTogglePause = {},
    onTapHaptic = {},
    onContinue = {},
    onRestart = {},
    onStop = {},
    onOpenLogbook = {},
)

/** Vollbild-Meditation ohne automatische Ruhe. */
@Composable
internal fun MeditationShot(state: MeditationUiState) = MeditationOverlay(
    state = state,
    display = MeditationDisplay(autoCalm = false),
    onMinimize = {},
    onToggleMute = {},
    onTogglePause = {},
    onTapHaptic = {},
    onRestart = {},
    onEnd = {},
    onOpenLogbook = {},
)
