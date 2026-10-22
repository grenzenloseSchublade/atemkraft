package app.atemkraft.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import app.atemkraft.domain.Exercise
import kotlinx.coroutines.flow.Flow

/**
 * Übung einer Detail-Route. [exercise] null und [missing] false heißt: noch nicht geladen (ein
 * gespeichertes Muster beim Kaltstart, bevor die DB antwortet) – nichts zeigen, aber auch nicht
 * zurück. [missing]: Die Übung gibt es nicht (mehr), die Seite geht zurück (CODE-05).
 */
@Immutable
internal data class DetailExercise(val exercise: Exercise?, val missing: Boolean)

/**
 * Löst die Übung der Detailseite [id] auf.
 * - Eingebaute Übungen und das Muster des Tages einmal über [resolve]: Seite, Start und „Von
 *   vorne“ zeigen dasselbe Muster, auch wenn das Tagesmuster um Mitternacht wechselt (ARC-5).
 * - Gespeicherte Muster ([live] liefert einen Flow, sonst null) folgen der DB: Nach dem
 *   Bearbeiten im Editor zeigt die Seite sofort die neuen Werte, nach dem Löschen meldet sie
 *   [DetailExercise.missing]. Bis die DB antwortet, gilt der Stand aus [resolve].
 */
@Composable
internal fun rememberDetailExercise(
    id: String,
    resolve: (String) -> Exercise?,
    live: (String) -> Flow<Exercise?>?,
): DetailExercise {
    val first = remember(id) { resolve(id) }
    val flow = remember(id) { live(id) }
    val state by produceState(DetailExercise(first, missing = first == null && flow == null), id, flow) {
        flow?.collect { value = DetailExercise(it, missing = it == null) }
    }
    return state
}
