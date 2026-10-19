package app.atemkraft.ui

import androidx.compose.runtime.Composable
import app.atemkraft.data.IntervalOverrides
import app.atemkraft.domain.Exercise
import app.atemkraft.playback.MeditationUiState
import app.atemkraft.ui.detail.ExerciseDetailScreen
import app.atemkraft.ui.meditation.MeditationOverlay
import app.atemkraft.ui.session.SessionOverlay
import app.atemkraft.ui.session.SessionUiState

/*
 * Screens mit vorbelegten Callbacks für Tests (TEST-4): Ein neuer Parameter einer Screen-Funktion
 * ändert nur diese Datei, nicht jeden Test. Offen bleibt, was Tests variieren oder beobachten.
 * Vorsicht: Ein neuer Pflichtparameter, der im Test sinnvoll belegt sein muss, gehört als
 * Parameter hierher, nicht als stiller Default.
 */

/** Detailseite ohne Sicherheitsdialog und ohne gespeicherte Anpassung. */
@Composable
internal fun TestDetail(
    exercise: Exercise,
    savedIntervals: IntervalOverrides? = null,
    requireSafetyConfirm: Boolean = false,
    onOpenLore: ((patternId: String) -> Unit)? = null,
    onMarkedChange: ((Boolean) -> Unit)? = null,
) = ExerciseDetailScreen(
    exercise = exercise,
    requireSafetyConfirm = requireSafetyConfirm,
    savedIntervals = savedIntervals,
    onIntervalsChange = { _, _, _, _ -> },
    onIntervalsReset = {},
    onConfirmedSafety = {},
    onStart = {},
    onBack = {},
    onOpenLore = onOpenLore,
    onMarkedChange = onMarkedChange,
)

/** Vollbild der Atem-Session. */
@Composable
internal fun TestSessionOverlay(
    state: SessionUiState,
    showNextPhase: Boolean = false,
    onMinimize: () -> Unit = {},
    onToggleMute: () -> Unit = {},
    onTogglePause: () -> Unit = {},
    onTapHaptic: () -> Unit = {},
    onContinue: () -> Unit = {},
    onRestart: () -> Unit = {},
    onStop: () -> Unit = {},
) = SessionOverlay(
    state = state,
    showNextPhase = showNextPhase,
    onMinimize = onMinimize,
    onToggleMute = onToggleMute,
    onTogglePause = onTogglePause,
    onTapHaptic = onTapHaptic,
    onContinue = onContinue,
    onRestart = onRestart,
    onStop = onStop,
)

/** Vollbild der Meditation. */
@Composable
internal fun TestMeditationOverlay(
    state: MeditationUiState,
    onMinimize: () -> Unit = {},
    onToggleMute: () -> Unit = {},
    onTogglePause: () -> Unit = {},
    onTapHaptic: () -> Unit = {},
    onRestart: () -> Unit = {},
    onEnd: () -> Unit = {},
) = MeditationOverlay(
    state = state,
    onMinimize = onMinimize,
    onToggleMute = onToggleMute,
    onTogglePause = onTogglePause,
    onTapHaptic = onTapHaptic,
    onRestart = onRestart,
    onEnd = onEnd,
)
