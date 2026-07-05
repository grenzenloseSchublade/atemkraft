package app.atemkraft.data

import app.atemkraft.domain.Situation
import app.atemkraft.domain.SituationRecommendation

/** Situations-Empfehlungen aus der F3-Matrix. IDs verweisen auf [BuiltInExercises]. */
object Situations {

    val all: List<SituationRecommendation> = listOf(
        SituationRecommendation(
            situation = Situation.ACUTE_STRESS,
            title = "Akuter Stress oder Panik",
            rationale = "Die schnellste Soforthilfe – beruhigt in Sekunden.",
            exerciseIds = listOf("physiological-sigh", "cyclic-sighing"),
        ),
        SituationRecommendation(
            situation = Situation.BREATHLESSNESS,
            title = "Kurzatmig oder aufgeregt",
            rationale = "Bringt hektisches, flaches Atmen wieder in einen ruhigen, " +
                "gleichmäßigen Rhythmus.",
            exerciseIds = listOf("lippenbremse", "walking-breath", "buteyko", "nasenatmung"),
        ),
        SituationRecommendation(
            situation = Situation.SLEEP,
            title = "Vor dem Einschlafen",
            rationale = "Langer, ruhiger Ausatem zum Herunterfahren – hilft beim Einschlafen.",
            exerciseIds = listOf("4-7-8", "resonanz", "sitali"),
        ),
        SituationRecommendation(
            situation = Situation.FOCUS,
            title = "Konzentration & Fokus",
            rationale = "Ruhig und zugleich wach – schärft den Fokus vor einer Aufgabe.",
            exerciseIds = listOf("box-4-4-4-4"),
        ),
        SituationRecommendation(
            situation = Situation.HIGH_PHASE,
            title = "Wach & energiegeladen werden",
            rationale = "Ein kräftiger, anregender Atem-Reiz, der aktiviert und wach macht. " +
                "Nur üben, wenn du dich stabil und gesund fühlst.",
            exerciseIds = listOf("wim-hof", "feueratmung"),
            warn = true,
        ),
        SituationRecommendation(
            situation = Situation.CRASH,
            title = "Erschöpft oder ausgelaugt",
            rationale = "Nur sanfte, beruhigende Atmung – nichts Anstrengendes, das zusätzlich " +
                "auslaugt.",
            exerciseIds = listOf("resonanz", "cyclic-sighing"),
        ),
    )
}
