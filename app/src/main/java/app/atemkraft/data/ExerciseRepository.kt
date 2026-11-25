package app.atemkraft.data

import app.atemkraft.domain.BreathingFamily
import app.atemkraft.domain.DailyPattern
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.ExerciseIds
import app.atemkraft.domain.RandomPatternGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * Liefert die verfügbaren Übungen: eingebaute aus [BuiltInExercises] (im Code), gespeicherte über
 * [SavedPatternsRepository]. Zusätzlich das generierte „Muster des Tages“
 * ([RandomPatternGenerator]) als beobachtbarer Zustand [daily]; [today] liefert das Datum, gegen
 * das der Tageswechsel geprüft wird (Tests setzen eine feste Uhr).
 */
class ExerciseRepository(
    private val saved: SavedPatternsRepository,
    private val today: () -> LocalDate = LocalDate::now,
) {

    private var dailyDay: Long
    private val dailyState: MutableStateFlow<DailyPattern>

    init {
        dailyDay = today().toEpochDay()
        dailyState = MutableStateFlow(RandomPatternGenerator.forEpochDay(dailyDay))
    }

    /**
     * Das „Muster des Tages“ – deterministisch aus dem Datum, bis „Neu generieren“
     * ([regenerateDaily]) ein frisches setzt. Den Tageswechsel prüft [currentDaily]; der Wert
     * selbst wechselt nicht von allein um Mitternacht.
     */
    val daily: StateFlow<DailyPattern> = dailyState.asStateFlow()

    /** Das aktuelle Muster des Tages; nach einem Tageswechsel zuerst das des neuen Tages in [daily]. */
    @Synchronized
    fun currentDaily(): DailyPattern {
        val day = today().toEpochDay()
        if (day != dailyDay) {
            dailyDay = day
            dailyState.value = RandomPatternGenerator.forEpochDay(dailyDay)
        }
        return dailyState.value
    }

    /**
     * Ein frisches Muster erzeugen (ersetzt das Tagesmuster bis zum nächsten App-Start bzw.
     * Tageswechsel) – „Neu generieren“-Knopf auf der Karte.
     */
    @Synchronized
    fun regenerateDaily() {
        dailyDay = today().toEpochDay()
        dailyState.value = RandomPatternGenerator.forSeed(System.nanoTime())
    }

    fun all(): List<Exercise> = BuiltInExercises.all

    /**
     * Ob sich [id] als „Tut mir gut“ markieren lässt (◉): eingebaute Übungen, gespeicherte Muster
     * („muster-7“) und die Meditation ([ExerciseIds.MEDITATION]) – alles mit einer Kennung, die
     * bleibt. Nicht das Muster des Tages (wechselt täglich) und nicht der Entwurf im Editor.
     */
    fun isMarkable(id: String): Boolean = id == ExerciseIds.MEDITATION || ExerciseIds.savedIdOf(id) != null || BuiltInExercises.all.any { it.id == id }

    /**
     * Die Übungen aus „Tut mir gut“ ([marked]): eingebaute in Familien-Reihenfolge, danach markierte
     * gespeicherte Muster in der Reihenfolge von [patterns] (neueste zuerst). Die Meditation ist
     * keine Übung; ob sie markiert ist, prüft der Aufrufer.
     */
    fun favorites(marked: Set<String>, patterns: List<SavedPattern>): List<Exercise> = BuiltInExercises.all.filter { it.id in marked } + patterns.map { it.exercise }.filter { it.id in marked }

    fun byId(id: String): Exercise? = when {
        id == ExerciseIds.DAILY -> currentDaily().exercise
        id.startsWith(ExerciseIds.SAVED_PREFIX) -> saved.byExerciseId(id)
        else -> BuiltInExercises.all.firstOrNull { it.id == id }
    }

    /** Übungen nach Familie gruppiert, in fester Reihenfolge A → D. */
    private fun byFamily(): List<Pair<BreathingFamily, List<Exercise>>> = BreathingFamily.entries.map { family ->
        family to BuiltInExercises.all.filter { it.family == family }
    }.filter { (_, list) -> list.isNotEmpty() }

    /** Die angeleiteten Übungen nach Familie (Atmen-Tab); Familien ohne angeleitete fallen weg. */
    val guidedByFamily: List<Pair<BreathingFamily, List<Exercise>>> by lazy {
        byFamily()
            .map { (family, list) -> family to list.filter { it.guided } }
            .filter { (_, list) -> list.isNotEmpty() }
    }

    /** Programme: die eingebauten Übungen ohne Anleitung (Atmen-Tab). */
    val programs: List<Exercise> by lazy { all().filter { !it.guided } }
}
