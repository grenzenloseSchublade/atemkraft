package app.atemkraft.data

import app.atemkraft.domain.BreathingFamily
import app.atemkraft.domain.Exercise

/**
 * Liefert die verfügbaren Übungen. Aktuell aus [BuiltInExercises] (im Code);
 * später kann hier transparent eine JSON-/DataStore-Quelle dazukommen.
 */
class ExerciseRepository {

    fun all(): List<Exercise> = BuiltInExercises.all

    fun byId(id: String): Exercise? = BuiltInExercises.all.firstOrNull { it.id == id }

    /** Übungen nach Familie gruppiert, in fester Reihenfolge A → D. */
    fun byFamily(): List<Pair<BreathingFamily, List<Exercise>>> =
        BreathingFamily.entries.map { family ->
            family to BuiltInExercises.all.filter { it.family == family }
        }.filter { (_, list) -> list.isNotEmpty() }
}
