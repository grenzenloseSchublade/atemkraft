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
 * Übungs-IDs (Reihenfolge = Priorität). [warning] ist der Sicherheitssatz intensiver
 * Empfehlungen: Er steht als ⚠-Zeile über den Übungen, nicht am Anlass-Chip (die Chips sind
 * neutral, MUSTER-09); `null` = nicht intensiv ([warn]).
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
    val warning: String? = null,
    val keywords: List<String> = emptyList(),
    val shortLabel: String = title,
) {
    /** Intensive Empfehlung: Sie trägt einen Sicherheitssatz und keine Suchbegriffe. */
    val warn: Boolean get() = warning != null
}

/**
 * Ein Bereich (eine [BreathingFamily]) als zweite, breitere Ebene im Block „Wie fühlst du dich?“
 * (MUSTER-09): [keywords] sind breite Alltagswörter, über die die Suche den Bereich direkt
 * findet („beruhigen“ → A · Herunterregeln), kuratiert wie die Anlass-Begriffe und nie doppelt
 * mit ihnen belegt. [warning] wie bei [SituationRecommendation]: Der Bereich enthält intensive
 * Übungen, seine Ansicht trägt die ⚠-Zeile, und die Suche zeigt ihn nur, wenn kein anderer
 * kuratierter Begriff passt.
 */
data class BreathingArea(
    val family: BreathingFamily,
    val keywords: List<String>,
    val warning: String? = null,
) {
    val warn: Boolean get() = warning != null
}

/**
 * Bereich einer Empfehlung: die häufigste Familie unter ihren Übungen, bei Gleichstand die der
 * zuerst genannten Übung; `null` ohne auflösbare Übung. Abgeleitet statt als eigenes Feld, weil
 * die Ableitung für alle sechs Anlässe fachlich trägt:
 * - Akuter Stress (Seufzer, zyklisches Seufzen) → A · Herunterregeln: langer Ausatem, beruhigen.
 * - Kurzatmig oder aufgeregt (Lippenbremse, Gehen, Buteyko, Nasenatmung) → D · Funktionell: Es
 *   geht um das Atemmuster selbst, flaches, hektisches Atmen wieder gleichmäßig.
 * - Vor dem Einschlafen (4-7-8, Resonanz, Sitali) → A · Herunterregeln.
 * - Konzentration & Fokus (Box) → C · Balancieren: ruhig und zugleich wach, gleichmäßiger Takt.
 * - Wach & energiegeladen werden (Wim Hof, Feueratmung) → B · Hochregeln.
 * - Erschöpft oder ausgelaugt (Resonanz, zyklisches Seufzen) → A · Herunterregeln: bewusst
 *   nichts Anregendes, sondern sanft und beruhigend – nicht Hochregeln, obwohl man wacher werden
 *   möchte.
 */
fun SituationRecommendation.area(resolve: (String) -> Exercise?): BreathingFamily? {
    val families = exerciseIds.mapNotNull(resolve).map { it.family }
    val counts = families.groupingBy { it }.eachCount()
    return families.distinct().maxByOrNull { counts.getValue(it) } // maxBy: erster bei Gleichstand
}
