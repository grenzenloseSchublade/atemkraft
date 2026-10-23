package app.atemkraft.domain

/**
 * Filter der Übungsliste im Atmen-Tab (MUSTER-12, Nutzer 2026-10-23): quer zu den Familien, nie
 * eine Familie selbst – die Familienköpfe gliedern die Liste weiter. Aufgenommen ist nur, was die
 * 17 angeleiteten Übungen sinnvoll teilt (Stand 2026-10-23: 4, 12 und 8 Treffer); verworfen
 * wurden „gut belegt“ (nur Resonanz-Atmung, und die Studienlage gehört auf die Detailseite,
 * TEXT-07) und „sanft“ (alle bis auf die zwei intensiven).
 */
enum class ExerciseFilter {
    /**
     * „Bis 3 min“: Standarddauer höchstens [ExerciseFilters.SHORT_MAX_MINUTES], gerechnet wie am
     * Startknopf ([estimatedTotalSeconds], [minutesRounded]); ohne offene Phasen, deren Dauer
     * nur eine Untergrenze ist (Wim Hof). Eigene Anpassungen einer Übung zählen nicht.
     */
    SHORT,

    /** „Ohne Halten“: keine Phase „Halten“ (voll oder leer) im Ablauf, auch nicht in Stufen. */
    NO_HOLD,

    /**
     * „Aus dem Yoga“: Technik der Seite „Pranayama“ (Ids aus den Daten). Nicht schon jede Übung
     * mit Abschnitt „Aus der Yoga-Lehre“: Box-Atmung und 4-7-8 sind dort nur verwandt.
     */
    YOGA,
}

/** Filterlogik der Übungsliste, rein und ohne Android (MUSTER-12). */
object ExerciseFilters {
    /** Obergrenze für [ExerciseFilter.SHORT] in ganzen Minuten. */
    const val SHORT_MAX_MINUTES = 3

    /** Trifft [filter] auf [exercise] zu? [yogaIds]: Übungen der Seite „Pranayama“. */
    fun matches(filter: ExerciseFilter, exercise: Exercise, yogaIds: Set<String>): Boolean = when (filter) {
        ExerciseFilter.SHORT -> !exercise.hasOpenPhases && minutesRounded(exercise.estimatedTotalSeconds()) <= SHORT_MAX_MINUTES
        ExerciseFilter.NO_HOLD -> exercise.buildTimeline().none { it.type == PhaseType.HOLD_FULL || it.type == PhaseType.HOLD_EMPTY }
        ExerciseFilter.YOGA -> exercise.id in yogaIds
    }

    /**
     * Die Gruppen nach Familie, gefiltert mit allen [active] Filtern zugleich (UND); Familien ohne
     * Treffer fallen weg, die Reihenfolge bleibt. Ohne Filter unverändert.
     */
    fun apply(
        groups: List<Pair<BreathingFamily, List<Exercise>>>,
        active: Set<ExerciseFilter>,
        yogaIds: Set<String>,
    ): List<Pair<BreathingFamily, List<Exercise>>> {
        if (active.isEmpty()) return groups
        return groups
            .map { (family, list) -> family to list.filter { e -> active.all { matches(it, e, yogaIds) } } }
            .filter { (_, list) -> list.isNotEmpty() }
    }
}
