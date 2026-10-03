package app.atemkraft.data

import app.atemkraft.domain.BreathingFamily
import app.atemkraft.domain.DailyPattern
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.RandomPatternGenerator
import java.time.LocalDate

/**
 * Liefert die verfügbaren Übungen. Aktuell aus [BuiltInExercises] (im Code);
 * später kann hier transparent eine JSON-/DataStore-Quelle dazukommen. Zusätzlich das
 * generierte „Muster des Tages" ([RandomPatternGenerator]), pro Tag gecacht.
 */
class ExerciseRepository(private val saved: SavedPatternsRepository) {

    private var dailyCache: Pair<Long, DailyPattern>? = null

    /** Das „Muster des Tages" – deterministisch aus dem Datum, pro Tag gecacht. */
    fun daily(): DailyPattern {
        val key = LocalDate.now().toEpochDay()
        dailyCache?.let { (cachedKey, pattern) -> if (cachedKey == key) return pattern }
        return RandomPatternGenerator.forDate(LocalDate.now()).also { dailyCache = key to it }
    }

    /**
     * Ein frisches Muster erzeugen (ersetzt das Tagesmuster bis zum nächsten App-Start bzw.
     * Tageswechsel) – „Neu generieren"-Knopf auf der Karte.
     */
    fun regenerateDaily(): DailyPattern {
        val key = LocalDate.now().toEpochDay()
        val fresh = RandomPatternGenerator.forSeed(System.nanoTime())
        dailyCache = key to fresh
        return fresh
    }

    fun all(): List<Exercise> = BuiltInExercises.all

    fun byId(id: String): Exercise? = when {
        id == RandomPatternGenerator.ID -> daily().exercise
        id.startsWith(SavedPatternsRepository.ID_PREFIX) -> saved.byExerciseId(id)
        else -> BuiltInExercises.all.firstOrNull { it.id == id }
    }

    /** Übungen nach Familie gruppiert, in fester Reihenfolge A → D. */
    fun byFamily(): List<Pair<BreathingFamily, List<Exercise>>> = BreathingFamily.entries.map { family ->
        family to BuiltInExercises.all.filter { it.family == family }
    }.filter { (_, list) -> list.isNotEmpty() }
}
