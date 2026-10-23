package app.atemkraft.ui.patterns

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.data.CueSettings
import app.atemkraft.data.SafetySettings
import app.atemkraft.data.SavedPattern
import app.atemkraft.data.SavedPatternsRepository
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.PatternSpec
import app.atemkraft.domain.SessionConfig
import app.atemkraft.ui.components.PushHeader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Ziel „Eigenes Muster“ im NavHost (`PatternEditorRoute`): legt den Zustand des Editors an und
 * schreibt das Lesezeichen ins [repository].
 *
 * Startwerte:
 * - ohne Argumente ein neues Muster (Vorlage 4·6, 5 min),
 * - [fromDaily]: die Zahlen des Musters des Tages ([dailySpec]) als freie Folge,
 * - [patternId]: die Folge des gespeicherten Musters ([SavedPatternsRepository.loadForEdit],
 *   `SavedPattern.pattern`; ältere Muster mit vier Feldern als Folge). Gibt es das
 *   nicht (mehr), ruft die Seite [onMissing] (zurück statt leerer Seite, CODE-05). Nach einem
 *   Konfigurationswechsel gilt der gesicherte Entwurf, auch wenn das Muster inzwischen entfernt ist.
 *
 * Lesezeichen: Das erste Speichern legt das Muster an ([SavedPatternsRepository.saveCustom]),
 * jedes weitere schreibt in denselben Eintrag ([SavedPatternsRepository.update]); Entfernen löscht
 * ihn. Die Schreibvorgänge laufen nacheinander in [scope] – dem App-weiten Scope, damit „Zurück“
 * direkt nach dem Tipp nichts verliert – und lesen den Entwurf erst, wenn sie dran sind: Ein
 * Doppeltipp legt so nur einen Eintrag an. Das Lesezeichen merkt sich genau den geschriebenen
 * Stand ([PatternEditorState.draft]), nicht den nach dem Schreiben.
 *
 * [onStart] startet den Entwurf ohne Speichern; der Aufrufer lässt den Editor dabei offen. Ein
 * intensives Muster fragt vorher den Sicherheitsdialog ab, wenn [safety] es verlangt; die
 * Bestätigung meldet [onConfirmedSafety]. [cueSettings] und [otherAudioActive] (Atem-Übung oder
 * Meditation läuft, auch minimiert) steuern den Ton der Vorschau.
 */
@Composable
internal fun PatternEditorDestination(
    patternId: Long?,
    fromDaily: Boolean,
    repository: SavedPatternsRepository,
    dailySpec: () -> PatternSpec,
    scope: CoroutineScope,
    onStart: (Exercise, SessionConfig) -> Unit,
    onOpenLore: (patternId: String) -> Unit,
    onBack: () -> Unit,
    onMissing: () -> Unit,
    cueSettings: CueSettings = CueSettings(),
    otherAudioActive: Boolean = false,
    safety: SafetySettings = SafetySettings(),
    onConfirmedSafety: (exerciseId: String) -> Unit = {},
) {
    val writes = remember { Mutex() }
    val missing by rememberUpdatedState(onMissing)
    val editor: @Composable (PatternEditorState) -> Unit = { state ->
        PatternEditorScreen(
            state = state,
            onSavedChange = { save ->
                scope.launch { writes.withLock { write(state, save, repository, missing) } }
            },
            onStart = onStart,
            onOpenLore = onOpenLore,
            onBack = onBack,
            cueSettings = cueSettings,
            otherAudioActive = otherAudioActive,
            safety = safety,
            onConfirmedSafety = onConfirmedSafety,
        )
    }

    if (patternId == null) {
        val state = rememberPatternEditorState(fromDaily) { prefix ->
            if (fromDaily) PatternEditorState.fromDaily(prefix, dailySpec()) else PatternEditorState.new(prefix)
        }
        editor(state)
        return
    }

    // Schon einmal gezeigt: rememberSaveable bringt den Entwurf zurück, kein erneutes Laden.
    var opened by rememberSaveable(patternId) { mutableStateOf(false) }
    val load by produceState(if (opened) EditLoad.Restored else EditLoad.Pending, patternId) {
        if (value == EditLoad.Pending) {
            value = repository.loadForEdit(patternId)?.let { EditLoad.Found(it) } ?: EditLoad.Missing
        }
    }
    when (val current = load) {
        EditLoad.Pending -> Loading(onBack)

        EditLoad.Missing -> {
            Loading(onBack)
            LaunchedEffect(Unit) { missing() }
        }

        else -> {
            val found = (current as? EditLoad.Found)?.pattern
            val state = rememberPatternEditorState(patternId) { prefix ->
                if (found != null) {
                    PatternEditorState.forEdit(prefix, found.id, found.pattern, found.minutes, found.exercise.name)
                } else {
                    PatternEditorState.new(prefix)
                }
            }
            LaunchedEffect(Unit) { opened = true }
            editor(state)
        }
    }
}

/**
 * Ein Tipp aufs Lesezeichen: [save] legt an bzw. aktualisiert denselben Eintrag und meldet die Id
 * zurück ([PatternEditorState.markSaved]); sonst wird der Eintrag gelöscht
 * ([PatternEditorState.markRemoved]). Gibt es den Eintrag beim Aktualisieren nicht mehr, [onMissing].
 */
private suspend fun write(
    state: PatternEditorState,
    save: Boolean,
    repository: SavedPatternsRepository,
    onMissing: () -> Unit,
) {
    if (!save) {
        state.savedId?.let { repository.delete(it) }
        state.markRemoved()
        return
    }
    // Stand vor dem Schreiben festhalten: Was während des Schreibens geändert wird, ist nicht
    // gespeichert und darf das Lesezeichen nicht füllen.
    val draft = state.draft()
    val id = state.savedId
    val stored = if (id == null) {
        repository.saveCustom(draft.pattern, draft.minutes, draft.name)
    } else {
        repository.update(id, draft.pattern, draft.minutes, draft.name)
    }
    if (stored == null) onMissing() else state.markSaved(stored.id, draft)
}

/** Kopf ohne Inhalt, solange das gespeicherte Muster lädt (Bruchteile einer Sekunde). */
@Composable
private fun Loading(onBack: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        PushHeader(onBack = onBack, title = stringResource(R.string.pattern_editor_title))
    }
}

/** Ladezustand des Bearbeiten-Modus. */
private sealed interface EditLoad {
    data object Pending : EditLoad

    /** Der Entwurf kommt aus dem gesicherten Zustand (Konfigurationswechsel, Prozesstod). */
    data object Restored : EditLoad

    data object Missing : EditLoad

    data class Found(val pattern: SavedPattern) : EditLoad
}
