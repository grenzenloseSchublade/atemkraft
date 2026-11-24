package app.atemkraft.ui.patterns

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.atemkraft.R
import app.atemkraft.data.CueSettings
import app.atemkraft.data.PatternVersion
import app.atemkraft.data.SafetySettings
import app.atemkraft.data.SavedPattern
import app.atemkraft.data.SavedPatternsRepository
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.PatternSpec
import app.atemkraft.domain.SessionConfig
import app.atemkraft.ui.components.PushHeader
import app.atemkraft.ui.theme.Motion
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Ziel „Eigenes Muster“ im NavHost (`PatternEditorRoute`): legt den Zustand des Editors an und
 * schreibt ins [repository] (MUSTER-14).
 *
 * Startwerte:
 * - ohne Argumente ein neues Muster (Vorlage 4·6, 5 min),
 * - [fromDaily]: die Zahlen des Musters des Tages ([dailySpec]) als freie Folge,
 * - [patternId]: die Folge des gespeicherten Musters ([SavedPatternsRepository.loadForEdit],
 *   `SavedPattern.pattern`; ältere Muster mit vier Feldern als Folge). Gibt es das
 *   nicht (mehr), ruft die Seite [onMissing] (zurück statt leerer Seite, CODE-05). Nach einem
 *   Konfigurationswechsel gilt der gesicherte Entwurf, auch wenn das Muster inzwischen entfernt ist.
 *
 * Speichern: Im Entwurf legt die Diskette das Muster an ([SavedPatternsRepository.saveCustom]); ab
 * dann – und bei jedem Muster, das es vor dem Öffnen schon gab – schreibt [AutoSave] jede Änderung
 * in denselben Eintrag ([SavedPatternsRepository.update]): entprellt [Motion.AutoSaveSettle] nach
 * der letzten Änderung, sofort beim Verlassen, wenn die App in den Hintergrund geht und vor dem
 * Start. Das erste Schreiben einer Sitzung legt den Stand beim Öffnen als vorherige Fassung ab
 * ([PatternEditorState.previousToKeep]). Die Schreibvorgänge laufen nacheinander in [scope] – dem
 * App-weiten Scope, damit „Zurück“ direkt nach einer Änderung nichts verliert – unter einer Sperre
 * außerhalb der Komposition ([EditorWrites]) und lesen den Entwurf erst, wenn sie dran sind: Ein
 * Doppeltipp legt so nur einen Eintrag an, mehrere ausstehende Schreibvorgänge schreiben nur
 * einmal, und ein Drehen mitten im Anlegen legt kein zweites Muster an. Der Zustand merkt sich
 * genau den geschriebenen Stand ([PatternEditorState.draft]), nicht den nach dem Schreiben.
 *
 * [onStart] startet den Stand; der Aufrufer lässt den Editor dabei offen. Bei einem bestehenden
 * Muster ist vorher geschrieben, die Sitzung läuft also mit dem gespeicherten Stand; ein Entwurf
 * startet als Probe ohne Speichern. Ein intensives Muster fragt vorher den Sicherheitsdialog ab,
 * wenn [safety] es verlangt; die Bestätigung meldet [onConfirmedSafety] (Schlüssel aus Übung und
 * Folge). [cueSettings] und [otherAudioActive] (Atem-Übung oder
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
    onConfirmedSafety: (key: String) -> Unit = {},
) {
    val missing by rememberUpdatedState(onMissing)
    val start by rememberUpdatedState(onStart)
    val editor: @Composable (PatternEditorState) -> Unit = { state ->
        // Bestehendes Muster: ausstehende Änderungen schreiben (nicht anlegen).
        val flush: () -> Unit = {
            if (state.needsWrite) scope.launch { EditorWrites.lock.withLock { write(state, create = false, repository, missing) } }
        }
        AutoSave(state, flush)
        AdoptCreated(state)
        PatternEditorScreen(
            state = state,
            actions = PatternEditorActions(
                onSave = {
                    state.markSaving()
                    // Vor dem Start vermerkt (Hauptthread): Eine nach dem Drehen neue Seite wartet darauf.
                    val done = CompletableDeferred<Unit>()
                    EditorWrites.creating[state.sessionKey] = done
                    scope.launch {
                        try {
                            EditorWrites.lock.withLock { write(state, create = true, repository, missing) }
                        } finally {
                            done.complete(Unit)
                        }
                    }
                },
                // Erst schreiben, dann starten: Die Sitzung läuft mit dem gespeicherten Stand.
                onStart = { exercise, config ->
                    scope.launch {
                        EditorWrites.lock.withLock { write(state, create = false, repository, missing) }
                        start(exercise, config)
                    }
                },
                onOpenLore = onOpenLore,
                onBack = onBack,
                onConfirmedSafety = onConfirmedSafety,
            ),
            cueSettings = cueSettings,
            otherAudioActive = otherAudioActive,
            safety = safety,
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
 * Auto-Speichern eines bestehenden Musters (MUSTER-14): Jede Änderung von Folge, Dauer oder Name
 * (auch Vorlage und Zurücksetzen) schreibt [flush] – erst wenn [Motion.AutoSaveSettle] lang nichts
 * mehr geändert wurde (ein Zug an der Kurve schreibt einmal, nicht je Raste), sofort, wenn die App
 * in den Hintergrund geht oder die Seite verlassen wird (auch Drehen: dann wird früh geschrieben,
 * nicht doppelt). Ein Entwurf schreibt hier nie; [flush] prüft selbst, ob etwas aussteht.
 */
@Composable
private fun AutoSave(state: PatternEditorState, flush: () -> Unit) {
    val currentFlush by rememberUpdatedState(flush)
    LaunchedEffect(state) {
        snapshotFlow { if (state.savedId != null) state.draft() else null }.collectLatest { draft ->
            if (draft != null && state.needsWrite) {
                delay(Motion.AutoSaveSettle)
                currentFlush()
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { currentFlush() }
    DisposableEffect(state) { onDispose { currentFlush() } }
}

/**
 * Schreibvorgänge aller Editor-Seiten, außerhalb der Komposition: Eine nach dem Drehen neue Seite
 * teilt die Sperre mit dem Schreiben, das die alte noch beendet. [created] hält je Sitzung
 * ([PatternEditorState.sessionKey]) das angelegte Muster, [creating] meldet, wann ein Anlegen fertig
 * ist ([AdoptCreated]). Je angelegtem Entwurf ein Eintrag, solange der Prozess lebt – wenige Bytes.
 */
private object EditorWrites {
    val lock = Mutex()
    val created = ConcurrentHashMap<Long, Created>()
    val creating = ConcurrentHashMap<Long, CompletableDeferred<Unit>>()

    /** Unter [id] angelegt, mit genau diesem Stand. */
    data class Created(val id: Long, val written: PatternVersion)
}

/**
 * Drehen mitten im Anlegen: Die neue Seite übernimmt [PatternEditorState.savePending] aus dem
 * Saver, wartet, bis das Anlegen der alten fertig ist, und übernimmt dessen Id – statt wieder als
 * Entwurf mit Diskette dazustehen (zweites Muster beim nächsten Tipp). Kam nichts an (Fehler,
 * Prozesstod), ist sie wieder ein Entwurf.
 */
@Composable
private fun AdoptCreated(state: PatternEditorState) {
    LaunchedEffect(state) {
        if (!state.savePending) return@LaunchedEffect
        EditorWrites.creating[state.sessionKey]?.await()
        if (state.savedId != null) return@LaunchedEffect
        val made = EditorWrites.created[state.sessionKey]
        if (made != null) state.markSaved(made.id, made.written) else state.markSaveFailed()
    }
}

/**
 * Ein Schreibvorgang, wenn er dran ist: Ein Entwurf wird nur mit [create] angelegt (Diskette,
 * „Speichern“ im Dialog) und meldet die Id zurück ([PatternEditorState.markSaved]); ein bestehendes
 * Muster wird aktualisiert, wenn der Entwurf vom zuletzt geschriebenen Stand abweicht – beim ersten
 * Mal je Sitzung mit der vorherigen Fassung ([PatternEditorState.previousToKeep]). Gibt es den
 * Eintrag beim Aktualisieren nicht mehr, [onMissing]. Scheitert das Anlegen, ist der Zustand wieder
 * ein Entwurf ([PatternEditorState.markSaveFailed]), der Fehler geht weiter an den App-Scope
 * (`UncaughtErrors`); ein gescheitertes Aktualisieren holt das nächste Auto-Speichern nach.
 */
private suspend fun write(
    state: PatternEditorState,
    create: Boolean,
    repository: SavedPatternsRepository,
    onMissing: () -> Unit,
) {
    // Hat diese Sitzung schon angelegt (Seite gedreht, bevor sie es erfuhr), gilt dieser Eintrag.
    if (state.savedId == null) EditorWrites.created[state.sessionKey]?.let { state.markSaved(it.id, it.written) }
    // Stand vor dem Schreiben festhalten: Was während des Schreibens geändert wird, schreibt der
    // nächste Vorgang.
    val draft = state.draft()
    val id = state.savedId
    val stored = try {
        when {
            id == null && !create -> return
            id == null -> repository.saveCustom(draft.pattern, draft.minutes, draft.name)
            draft == state.snapshot -> return
            else -> repository.update(id, draft.pattern, draft.minutes, draft.name, previous = state.previousToKeep())
        }
    } catch (e: Exception) {
        if (id == null) state.markSaveFailed()
        throw e
    }
    if (stored == null) {
        onMissing()
        return
    }
    if (id == null) EditorWrites.created[state.sessionKey] = EditorWrites.Created(stored.id, draft)
    state.markSaved(stored.id, draft)
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
