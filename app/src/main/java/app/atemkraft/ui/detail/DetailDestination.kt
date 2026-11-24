package app.atemkraft.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.atemkraft.R
import app.atemkraft.data.BuiltInExercises
import app.atemkraft.data.ExerciseRepository
import app.atemkraft.data.SafetySettings
import app.atemkraft.data.SavedPatternsRepository
import app.atemkraft.data.SettingsRepository
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.ExerciseIds
import app.atemkraft.domain.GeneratedExercises
import app.atemkraft.domain.SessionConfig
import app.atemkraft.ui.navigation.PatternEditorRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
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
 * - „Danach Stille“: Vorgabe aus den Einstellungen – aus, wenn „Danach in die Stille“ aus ist, sonst
 *   die Dauer des Tabs Meditation (bei offener Sitzung die zuletzt gewählte feste Dauer).
 * - Anpassungen der Intervalle merkt sich [settings] pro Übung – außer beim Muster des Tages:
 *   dessen Id ist konstant, der Inhalt wechselt aber täglich; gespeicherte Werte gälten sonst
 *   morgen für ein anderes Muster.
 * - Markieren („Tut mir gut“, [markedIds]) nur eingebaute Übungen: Ihre Id ist stabil und ihre
 *   Karten stehen in den Listen; Muster wechseln oder lassen sich löschen.
 * - Sicherheitsdialog vor dem Start bei `caution`, solange er nicht bestätigt bzw. in den
 *   Einstellungen abgeschaltet ist ([safety]); die Bestätigung wird gemerkt.
 * - Stift nur an generierten Mustern: das Tagesmuster als eigenes anpassen, ein gespeichertes
 *   bearbeiten. [onOpenEditor] bekommt die Route des Editors; der Aufrufer navigiert.
 * - Gespeicherte und eigene Muster (Eigentümer 2026-11-07): Löschen im Kopf neben dem Stift
 *   (zweistufig, MUSTER-05; danach geht die Seite über [onMissing]), am Ende „Duplizieren“ (legt
 *   eine Kopie an und öffnet deren Detailseite über [onOpenDetail]) und „Vorherige Fassung
 *   wiederherstellen“ (nur, wenn es eine gibt und sie abweicht; tauscht den Stand mit ihr).
 *
 * [onStart] startet die Übung; der Aufrufer schließt die Seite dabei. [initialMinutes]: Dauer für
 * diesen Einstieg (`DetailRoute.minutes`), nicht als Anpassung gespeichert. [covered]: Ein
 * Vollbild-Overlay (Session, Meditation) liegt über der Seite; ihr Rhythmus-Punkt steht dann still.
 */
@Composable
internal fun DetailDestination(
    exerciseId: String,
    initialMinutes: Int?,
    exercises: ExerciseRepository,
    savedPatterns: SavedPatternsRepository,
    settings: SettingsRepository,
    scope: CoroutineScope,
    safety: SafetySettings,
    markedIds: Set<String>,
    onStart: (Exercise, SessionConfig) -> Unit,
    onOpenLore: (patternId: String) -> Unit,
    onOpenEditor: (PatternEditorRoute) -> Unit,
    onOpenDetail: (exerciseId: String) -> Unit,
    onBack: () -> Unit,
    onMissing: () -> Unit,
    covered: Boolean = false,
) {
    val id = exerciseId
    val missing by rememberUpdatedState(onMissing)
    val lookup = rememberDetailExercise(
        id = id,
        resolve = exercises::byId,
        live = { key -> ExerciseIds.savedIdOf(key)?.let { savedPatterns.observe(key) } },
    )
    val exercise = lookup.exercise
    if (exercise == null) {
        if (lookup.missing) LaunchedEffect(id) { missing() }
        return
    }
    val savedPatternId = ExerciseIds.savedIdOf(id)
    val saved by remember(savedPatternId) { savedPatternId?.let(savedPatterns::observeSaved) ?: flowOf(null) }
        .collectAsStateWithLifecycle(initialValue = null)
    val resources = LocalResources.current
    val requireSafety = safety.requiresConfirm(exercise)
    val persistIntervals = id != ExerciseIds.DAILY
    val markable = exercise in BuiltInExercises.all
    val savedIntervals by remember(id) { settings.exerciseIntervals(id) }.collectAsStateWithLifecycle(initialValue = null)
    val silenceDefault by remember(settings) {
        combine(settings.sessionDisplay, settings.meditationSettings) { display, meditation ->
            if (display.silenceAfter) meditation.minutes else 0
        }
    }.collectAsStateWithLifecycle(initialValue = null)
    ExerciseDetailScreen(
        exercise = exercise,
        requireSafetyConfirm = requireSafety,
        adjustInputs = DetailAdjustInputs(saved = if (persistIntervals) savedIntervals else null, silenceDefault = silenceDefault),
        actions = DetailActions(
            onIntervalsChange = { overrides ->
                if (persistIntervals) {
                    scope.launch { settings.setExerciseIntervals(id, overrides) }
                }
            },
            onIntervalsReset = {
                if (persistIntervals) {
                    scope.launch { settings.clearExerciseIntervals(id) }
                }
            },
            onConfirmedSafety = {
                // Bestätigt gilt für diese Übung, bei Mustern mit genau dieser Folge (MUSTER-06).
                scope.launch { settings.acknowledgeSafety(SafetySettings.keyOf(exercise)) }
            },
            onStart = { config -> onStart(exercise, config) },
            onBack = onBack,
            onOpenLore = onOpenLore,
            onMarkedChange = if (markable) {
                { marked -> scope.launch { settings.setExerciseMarked(id, marked) } }
            } else {
                null
            },
            onEditPattern = when {
                // Die Zahlen genau dieses Musters mitgeben: Ein erneutes Nachschlagen im Editor wäre
                // nach Mitternacht schon das nächste.
                id == ExerciseIds.DAILY -> {
                    { onOpenEditor(GeneratedExercises.specOf(exercise)?.let(PatternEditorRoute::of) ?: PatternEditorRoute(fromDaily = true)) }
                }

                savedPatternId != null -> {
                    { onOpenEditor(PatternEditorRoute(patternId = savedPatternId)) }
                }

                else -> null
            },
            onDeletePattern = savedPatternId?.let { patternId ->
                { scope.launch { savedPatterns.delete(patternId) } }
            },
            onDuplicate = savedPatternId?.let { patternId ->
                {
                    scope.launch {
                        val copy = savedPatterns.duplicate(patternId) { name -> resources.getString(R.string.pattern_copy_name, name) }
                        copy?.let { onOpenDetail(ExerciseIds.saved(it.id)) }
                    }
                }
            },
            onRestorePrevious = savedPatternId?.takeIf { saved?.canRestore == true }?.let { patternId ->
                { scope.launch { savedPatterns.restorePrevious(patternId) } }
            },
        ),
        marked = id in markedIds,
        initialMinutes = initialMinutes,
        covered = covered,
    )
}
