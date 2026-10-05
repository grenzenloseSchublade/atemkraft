package app.atemkraft.data

import app.atemkraft.domain.Situation
import app.atemkraft.domain.SituationRecommendation

/**
 * Situations-Empfehlungen aus der F3-Matrix. IDs verweisen auf [BuiltInExercises].
 *
 * `keywords` sind die kuratierten Befindens-Begriffe der Suche (SituationSearch): Alltagswörter
 * und Anlässe in Laiensprache, je Begriff genau eine Situation. Leitplanken (geprüft in
 * SituationSearchTest und ContentRulesTest): keine Symptome oder Diagnosen, nichts, was etwas
 * verspricht; Müdigkeit führt zu „Erschöpft oder ausgelaugt“; die Warn-Situation „Wach &
 * energiegeladen werden“ bekommt keine einladenden Begriffe und trifft nur über ihren Titel
 * (nicht über ihren Satz mit dem Vorbehalt „stabil und gesund“).
 */
object Situations {

    val all: List<SituationRecommendation> = listOf(
        SituationRecommendation(
            situation = Situation.ACUTE_STRESS,
            title = "Akuter Stress oder Panik",
            rationale = "Die schnellste Soforthilfe – beruhigt in Sekunden.",
            exerciseIds = listOf("physiological-sigh", "cyclic-sighing"),
            keywords = listOf(
                "Stress", "gestresst", "stressig", "Angst", "angespannt", "überfordert",
                "unter Druck", "Streit", "Ärger", "wütend", "Wut", "aufgewühlt", "Schreck", "erschrocken",
                "schlechte Nachricht", "Zahnarzt",
            ),
        ),
        SituationRecommendation(
            situation = Situation.BREATHLESSNESS,
            title = "Kurzatmig oder aufgeregt",
            rationale = "Bringt hektisches, flaches Atmen wieder in einen ruhigen, " +
                "gleichmäßigen Rhythmus.",
            exerciseIds = listOf("lippenbremse", "walking-breath", "buteyko", "nasenatmung"),
            keywords = listOf(
                "nervös", "Aufregung", "hektisch", "Hektik", "gehetzt", "unruhig", "Unruhe",
                "zappelig", "Lampenfieber", "außer Atem", "aus der Puste", "Treppensteigen", "Spaziergang", "Sport",
            ),
        ),
        SituationRecommendation(
            situation = Situation.SLEEP,
            title = "Vor dem Einschlafen",
            rationale = "Langer, ruhiger Ausatem zum Herunterfahren – hilft beim Einschlafen.",
            exerciseIds = listOf("4-7-8", "resonanz", "sitali"),
            keywords = listOf(
                "schlafen", "kann nicht schlafen", "wach liegen", "liege wach", "durchschlafen",
                "aufgewacht", "nachts", "abends", "Bett", "Gedankenkarussell", "grübeln", "Kopf voll",
                "abschalten", "runterkommen",
            ),
        ),
        SituationRecommendation(
            situation = Situation.FOCUS,
            title = "Konzentration & Fokus",
            rationale = "Ruhig und zugleich wach – schärft den Fokus vor einer Aufgabe.",
            exerciseIds = listOf("box-4-4-4-4"),
            keywords = listOf(
                "Prüfung", "Prüfungsangst", "Klausur", "lernen", "Hausaufgaben", "Arbeit",
                "Aufgabe", "Meeting", "Besprechung", "Gespräch", "Vorstellungsgespräch", "Vortrag",
                "Präsentation", "Wettkampf", "abgelenkt", "zerstreut", "unkonzentriert",
                "konzentrieren",
            ),
        ),
        SituationRecommendation(
            situation = Situation.HIGH_PHASE,
            title = "Wach & energiegeladen werden",
            rationale = "Ein kräftiger, anregender Atem-Reiz, der aktiviert und wach macht. " +
                "Nur üben, wenn du dich stabil und gesund fühlst.",
            exerciseIds = listOf("wim-hof", "feueratmung"),
            warn = true,
            // Bewusst keine keywords: Sie würden Leute aktiv zu Hyperventilationstechniken
            // schicken, obwohl die Situation selbst einschränkt („nur, wenn du dich stabil …“).
        ),
        SituationRecommendation(
            situation = Situation.CRASH,
            title = "Erschöpft oder ausgelaugt",
            rationale = "Nur sanfte, beruhigende Atmung – nichts Anstrengendes, das zusätzlich " +
                "auslaugt.",
            exerciseIds = listOf("resonanz", "cyclic-sighing"),
            keywords = listOf(
                "müde", "schlapp", "kaputt", "erledigt", "fertig", "platt", "kraftlos",
                "energielos", "Nachmittagstief", "Feierabend", "nach der Arbeit", "langer Tag",
                "schlecht geschlafen",
            ),
        ),
    )
}
