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
 */
data class SituationRecommendation(
    val situation: Situation,
    val title: String,
    val rationale: String,
    val exerciseIds: List<String>,
    val warn: Boolean = false,
)
