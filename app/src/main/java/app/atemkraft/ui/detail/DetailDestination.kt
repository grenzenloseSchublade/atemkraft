package app.atemkraft.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.atemkraft.data.BuiltInExercises
import app.atemkraft.data.ExerciseRepository
import app.atemkraft.data.SafetySettings
import app.atemkraft.data.SavedPatternsRepository
import app.atemkraft.data.SettingsRepository
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.SessionConfig
import app.atemkraft.ui.navigation.PatternEditorRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Ziel „Detailseite“ im NavHost (`DetailRoute`): löst die Übung [exerciseId] auf und schreibt, was
 * die Seite ändert, in die Repositories – in [scope], dem App-weiten Scope, damit
 * „Zurück“ direkt nach dem Tipp nichts verliert.
 *
 * - Auflösen: eingebaute Übungen und das Muster des Tages einmal über [exercises] (ARC-5),
 *   gespeicherte Muster folgen der DB ([savedPatterns]) und zeigen nach dem Bearbeiten im Editor
 *   die neuen Werte ([rememberDetailExercise]). Gibt es die Übung nicht (mehr), ruft die Seite
 *   [onMissing] (zurück statt leerer Seite, CODE-05); lädt das Muster noch, bleibt sie kurz leer.
 * - Anpassungen der Intervalle merkt sich [settings] pro Übung – außer beim Muster des Tages:
 *   dessen Id ist konstant, der Inhalt wechselt aber täglich; gespeicherte Werte gälten sonst
 *   morgen für ein anderes Muster.
 * - Markieren („Tut mir gut“, [markedIds]) nur eingebaute Übungen: Ihre Id ist stabil und ihre
 *   Karten stehen in den Listen; Muster wechseln oder lassen sich löschen.
 * - Sicherheitsdialog vor dem Start bei `caution`, solange er nicht bestätigt bzw. in den
 *   Einstellungen abgeschaltet ist ([safety]); die Bestätigung wird gemerkt.
 * - Stift nur an generierten Mustern: das Tagesmuster als eigenes anpassen, ein gespeichertes
 *   bearbeiten. [onOpenEditor] bekommt die Route des Editors; der Aufrufer navigiert.
 *
 * [onStart] startet die Übung; der Aufrufer schließt die Seite dabei.
 */
@Composable
internal fun DetailDestination(
    exerciseId: String,
    exercises: ExerciseRepository,
    savedPatterns: SavedPatternsRepository,
    settings: SettingsRepository,
    scope: CoroutineScope,
    safety: SafetySettings,
    markedIds: Set<String>,
    onStart: (Exercise, SessionConfig) -> Unit,
    onOpenLore: (patternId: String) -> Unit,
    onOpenEditor: (PatternEditorRoute) -> Unit,
    onBack: () -> Unit,
    onMissing: () -> Unit,
) {
    val id = exerciseId
    val missing by rememberUpdatedState(onMissing)
    val lookup = rememberDetailExercise(
        id = id,
        resolve = exercises::byId,
        live = { key -> SavedPatternsRepository.idOf(key)?.let { savedPatterns.observe(key) } },
    )
    val exercise = lookup.exercise
    if (exercise == null) {
        if (lookup.missing) LaunchedEffect(id) { missing() }
        return
    }
    val savedPatternId = SavedPatternsRepository.idOf(id)
    val requireSafety = exercise.caution && (!safety.acknowledged || safety.showWarning)
    val persistIntervals = id != RandomPatternGenerator.ID
    val markable = exercise in BuiltInExercises.all
    val savedIntervals by remember(id) { settings.exerciseIntervals(id) }.collectAsStateWithLifecycle(initialValue = null)
    ExerciseDetailScreen(
        exercise = exercise,
        requireSafetyConfirm = requireSafety,
        savedIntervals = if (persistIntervals) savedIntervals else null,
        onIntervalsChange = { duration, inhale, hold, exhale ->
            if (persistIntervals) {
                scope.launch { settings.setExerciseIntervals(id, duration, inhale, hold, exhale) }
            }
        },
        onIntervalsReset = {
            if (persistIntervals) {
                scope.launch { settings.clearExerciseIntervals(id) }
            }
        },
        onConfirmedSafety = {
            scope.launch { settings.setSafetyAcknowledged(true) }
        },
        onStart = { config -> onStart(exercise, config) },
        onBack = onBack,
        onOpenLore = onOpenLore,
        marked = id in markedIds,
        onMarkedChange = if (markable) {
            { marked -> scope.launch { settings.setExerciseMarked(id, marked) } }
        } else {
            null
        },
        onEditPattern = when {
            // Die Zahlen genau dieses Musters mitgeben: Ein erneutes Nachschlagen im Editor wäre
            // nach Mitternacht schon das nächste.
            id == RandomPatternGenerator.ID -> {
                { onOpenEditor(RandomPatternGenerator.specOf(exercise)?.let(PatternEditorRoute::of) ?: PatternEditorRoute(fromDaily = true)) }
            }

            savedPatternId != null -> {
                { onOpenEditor(PatternEditorRoute(patternId = savedPatternId)) }
            }

            else -> null
        },
    )
}
