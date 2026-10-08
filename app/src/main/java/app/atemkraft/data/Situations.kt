package app.atemkraft.data

import app.atemkraft.domain.Situation
import app.atemkraft.domain.SituationRecommendation

/**
 * Situations-Empfehlungen aus der F3-Matrix. IDs verweisen auf [BuiltInExercises].
 *
 * `keywords` sind die kuratierten Befindens-Begriffe der Suche (SituationSearch): Alltagswörter
 * und Anlässe in Laiensprache, je Begriff genau eine Situation, rund 30 je Situation. Kein
 * Begriff fängt einen Begriff einer anderen Situation an (Ausnahmen „nachts“/„Nachtschicht“ und
 * „Ruhe“/„ruhelos“, gelöst über „genauester Begriff gewinnt“); Wörter mit Doppelsinn stehen nur
 * als Wendung da („erstes Date“ statt „Date“, das sonst „Daten“ träfe; „im Stau“ statt „Stau“
 * wegen „Staub“). Leitplanken (geprüft in
 * SituationSearchTest und ContentRulesTest): keine Symptome oder Diagnosen, nichts, was etwas
 * verspricht; Müdigkeit führt zu „Erschöpft oder ausgelaugt“; die Warn-Situation „Wach &
 * energiegeladen werden“ bekommt keine einladenden Begriffe und trifft nur über ihren Titel
 * (nicht über ihren Satz mit dem Vorbehalt „gesund und ausgeruht“).
 */
object Situations {

    val all: List<SituationRecommendation> = listOf(
        SituationRecommendation(
            situation = Situation.ACUTE_STRESS,
            title = "Akuter Stress",
            rationale = "Ein langer Ausatem für den akuten Moment – kurz, einfach und risikoarm.",
            exerciseIds = listOf("physiological-sigh", "cyclic-sighing"),
            keywords = listOf(
                "Stress", "gestresst", "stressig", "Angst", "Ängste", "ängstlich", "angespannt", "Anspannung",
                "überfordert", "Überforderung", "unter Druck",
                "Zeitdruck", "Termindruck", "Deadline", "Streit", "Konflikt", "Krach", "Ärger", "wütend",
                "Wut", "genervt", "gereizt", "ausrasten", "aufgewühlt", "Schreck", "erschrocken",
                "schlechte Nachricht", "Chef", "Zahnarzt", "Arzttermin", "Blutabnahme", "Spritze",
                "Flug", "Fliegen", "Flugangst", "Aufzug", "Menschenmenge", "im Stau", "Berufsverkehr", "Lärm",
            ),
        ),
        SituationRecommendation(
            situation = Situation.BREATHLESSNESS,
            title = "Kurzatmig oder aufgeregt",
            rationale = "Bringt hektisches, flaches Atmen wieder in einen ruhigen, " +
                "gleichmäßigen Rhythmus.",
            exerciseIds = listOf("lippenbremse", "walking-breath", "buteyko", "nasenatmung"),
            keywords = listOf(
                "nervös", "Nervosität", "Aufregung", "hektisch", "Hektik", "gehetzt", "abgehetzt", "unruhig",
                "Unruhe", "ruhelos", "rastlos", "getrieben", "zappelig", "hibbelig", "Lampenfieber", "Auftritt", "Bühne", "Rede",
                "erstes Date", "Hochzeit", "Vorfreude", "außer Atem", "aus der Puste", "schnaufen", "Treppe",
                "Treppensteigen", "bergauf", "Spaziergang", "spazieren", "Fahrrad", "Joggen", "Jogging", "Laufen", "Training", "Sport",
            ),
        ),
        SituationRecommendation(
            situation = Situation.SLEEP,
            title = "Vor dem Einschlafen",
            rationale = "Langer, ruhiger Ausatem zum Herunterfahren vor dem Schlafen.",
            exerciseIds = listOf("4-7-8", "resonanz", "sitali"),
            keywords = listOf(
                "Schlaf", "schlafen", "Einschlafen", "kann nicht schlafen", "wach liegen", "liege wach",
                "durchschlafen", "aufwachen", "aufgewacht", "früh wach", "nachts", "abends", "Abendroutine",
                "Schlafenszeit", "Bett", "hinlegen", "Gedanken", "Gedankenkarussell", "Gedanken kreisen",
                "grübeln", "grüble", "Grübelei", "Sorgen", "Kopf voll", "abschalten", "runterkommen", "herunterfahren",
                "runterfahren", "loslassen", "Ruhe", "zur Ruhe kommen", "entspannen", "Entspannung",
            ),
        ),
        SituationRecommendation(
            situation = Situation.FOCUS,
            title = "Konzentration & Fokus",
            rationale = "Ruhig und zugleich wach – ein gleichmäßiger Takt vor einer Aufgabe.",
            exerciseIds = listOf("box-4-4-4-4"),
            keywords = listOf(
                "Prüfung", "Prüfungsangst", "Klausur", "Test", "Examen", "Fahrprüfung", "Führerschein",
                "lernen", "lesen", "Hausaufgaben", "Schule", "Uni", "Universität", "Studium", "Arbeit", "Aufgabe", "Abgabe",
                "Büro", "Homeoffice", "Schreibtisch", "Meeting", "Besprechung", "Gespräch", "Bewerbung",
                "Vorstellungsgespräch", "Interview", "Vortrag", "Präsentation", "Wettkampf", "Turnier",
                "abgelenkt", "zerstreut", "unkonzentriert", "konzentrieren", "fokussieren", "klar denken",
            ),
        ),
        SituationRecommendation(
            situation = Situation.HIGH_PHASE,
            title = "Wach & energiegeladen werden",
            rationale = "Ein kräftiger, anregender Atem-Reiz, der aktiviert und wach macht. " +
                "Nur üben, wenn du dich gesund und ausgeruht fühlst.",
            exerciseIds = listOf("wim-hof", "feueratmung"),
            warn = true,
            // Bewusst keine keywords: Sie würden Leute aktiv zu Hyperventilationstechniken
            // schicken, obwohl die Situation selbst einschränkt („nur, wenn du dich gesund …“).
        ),
        SituationRecommendation(
            situation = Situation.CRASH,
            title = "Erschöpft oder ausgelaugt",
            rationale = "Nur sanfte, beruhigende Atmung – nichts Anstrengendes, das zusätzlich " +
                "auslaugt.",
            exerciseIds = listOf("resonanz", "cyclic-sighing"),
            keywords = listOf(
                "müde", "Müdigkeit", "Erschöpfung", "schlapp", "kaputt", "erledigt", "fertig", "platt",
                "erschlagen", "ausgepowert", "groggy", "kraftlos", "energielos", "Mittagstief",
                "Nachmittagstief", "Feierabend", "nach der Arbeit", "langer Tag", "schlecht geschlafen",
                "Nachtschicht", "Schichtdienst", "Schichtarbeit", "Jetlag", "Pause", "Auszeit", "erholen", "Erholung",
                "auftanken", "Kraft tanken", "durchatmen",
            ),
        ),
    )
}
