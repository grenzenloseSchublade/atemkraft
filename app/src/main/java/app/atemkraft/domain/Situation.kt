package app.atemkraft.domain

/** Lebenslagen aus der F3-Situations-Matrix („welche Atmung wann"). */
enum class Situation {
    ACUTE_STRESS,
    SLEEP,
    FOCUS,
    HIGH_PHASE,
    CRASH,
    BREATHLESSNESS,
}

/**
 * Empfehlung für eine Situation: Titel, Ein-Satz-Begründung und die empfohlenen
 * Übungs-IDs (Reihenfolge = Priorität). [warn] markiert intensive Empfehlungen.
 * [shortLabel] ist die kurze Beschriftung des Anlass-Chips im Tab „Atmen“ („Einschlafen“ statt
 * „Vor dem Einschlafen“); ohne eigene gilt der Titel.
 *
 * [keywords] sind kuratierte Befindens-Begriffe für die Suche im Tab „Atmen“
 * ([SituationSearch]): Alltagswörter in Laiensprache, keine Symptome oder Diagnosen. Ein
 * Begriff aus mehreren Wörtern trifft nur als ganze Wendung („wach liegen“), damit seine
 * Einzelwörter nicht woanders hinführen. Eine [warn]-Situation bekommt keine Begriffe.
 */
data class SituationRecommendation(
    val situation: Situation,
    val title: String,
    val rationale: String,
    val exerciseIds: List<String>,
    val warn: Boolean = false,
    val keywords: List<String> = emptyList(),
    val shortLabel: String = title,
)
