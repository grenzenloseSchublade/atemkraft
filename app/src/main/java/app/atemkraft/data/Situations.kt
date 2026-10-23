package app.atemkraft.data

import app.atemkraft.domain.Situation
import app.atemkraft.domain.SituationRecommendation

/**
 * Situations-Empfehlungen aus der F3-Matrix. IDs verweisen auf [BuiltInExercises].
 *
 * `keywords` sind die kuratierten Befindens-Begriffe der Suche (SituationSearch): Alltagswörter
 * und Anlässe in Laiensprache, je Begriff genau eine Situation, rund 35 je Situation. Kein
 * Begriff fängt einen Begriff einer anderen Situation an (Ausnahmen „nachts“/„Nachtschicht“ und
 * „Ruhe“/„ruhelos“, gelöst über „genauester Begriff gewinnt“); Wörter mit Doppelsinn stehen nur
 * als Wendung da („erstes Date“ statt „Date“, das sonst „Daten“ träfe; „im Stau“ statt „Stau“
 * wegen „Staub“). Leitplanken (geprüft in
 * SituationSearchTest und ContentRulesTest): keine Symptome oder Diagnosen, nichts, was etwas
 * verspricht; Müdigkeit führt zu „Erschöpft oder ausgelaugt“; die Warn-Situation „Wach &
 * energiegeladen werden“ bekommt keine einladenden Begriffe und trifft nur über ihren Titel.
 * Die breiteren Wörter der Bereiche (Familien) stehen in [Areas] und belegen kein Wort doppelt.
 */
object Situations {

    /**
     * Sicherheitssatz intensiver Empfehlungen (Anlass „Wach & energiegeladen werden“, Bereich
     * B · Hochregeln), als ⚠-Zeile über den Übungen. Nur, was die Hinweise von Wim Hof und
     * Feueratmung beide sagen: gesund und ausgeruht (Wim Hof), im Sitzen (beide erlauben es,
     * Feueratmung nur so), nie beim Fahren (beide), nie im oder am Wasser (Wim Hof „im/am“,
     * Feueratmung „im“).
     */
    const val INTENSE_WARNING = "Nur üben, wenn du dich gesund und ausgeruht fühlst – im Sitzen, " +
        "nie beim Fahren oder im/am Wasser."

    /**
     * Notfall-Satz des Anlasses „Kurzatmig“, als ⚠-Zeile über den Übungen (TEXT-08): Wer akut
     * keine Luft bekommt, tippt genau diesen Chip an. Atemnot mit Brustschmerz kann ein
     * Herzinfarkt oder eine Lungenembolie sein; für Asthma gilt der eigene Notfallplan (wie bei
     * Buteyko). Er macht den Anlass nicht intensiv ([SituationRecommendation.intense]).
     */
    const val BREATHLESS_WARNING = "Bei plötzlicher, starker Atemnot oder Brustschmerz: Notruf 112, " +
        "keine Übung. Bei Asthma gilt dein Notfallplan."

    val all: List<SituationRecommendation> = listOf(
        SituationRecommendation(
            situation = Situation.ACUTE_STRESS,
            title = "Akuter Stress",
            // Kurz, damit die sechs Chips auf 360 dp in zwei Zeilen passen (Nutzer 2026-10-23).
            shortLabel = "Stress",
            rationale = "Ein langer Ausatem für den akuten Moment – kurz, einfach und risikoarm.",
            exerciseIds = listOf("physiological-sigh", "cyclic-sighing"),
            keywords = listOf(
                "Stress", "gestresst", "stressig", "Angst", "Ängste", "ängstlich", "angespannt", "Anspannung",
                "überfordert", "Überforderung", "unter Druck",
                "Zeitdruck", "Termindruck", "Deadline", "Streit", "Konflikt", "Krach", "Ärger", "wütend",
                "Wut", "genervt", "gereizt", "ausrasten", "aufgewühlt", "Schreck", "erschrocken",
                "schlechte Nachricht", "Chef", "Zahnarzt", "Arzttermin", "Blutabnahme", "Spritze",
                "Flug", "Fliegen", "Flugangst", "Aufzug", "Menschenmenge", "im Stau", "Berufsverkehr", "Lärm",
                "Frust", "Zoff", "ungeduldig", "Verspätung", "Wartezimmer", "Behörde",
            ),
        ),
        SituationRecommendation(
            situation = Situation.BREATHLESSNESS,
            title = "Kurzatmig oder aufgeregt",
            shortLabel = "Kurzatmig",
            // Nur Übungen mit Start (UX-Review 2026-10-23): Buteyko und Nasenatmung sind Infoseiten
            // ohne Takt und wären hier eine Sackgasse; der Satz verweist auf den Bereich darunter
            // („Mehr aus …“), in dem beide stehen.
            rationale = "Bringt hektisches, flaches Atmen wieder in einen ruhigen, " +
                "gleichmäßigen Rhythmus. Buteyko und Nasenatmung sind Trainings über Wochen; du " +
                "findest sie im Bereich darunter.",
            exerciseIds = listOf("lippenbremse", "walking-breath"),
            warning = BREATHLESS_WARNING,
            keywords = listOf(
                "nervös", "Nervosität", "Aufregung", "hektisch", "Hektik", "gehetzt", "abgehetzt", "unruhig",
                "Unruhe", "ruhelos", "rastlos", "getrieben", "zappelig", "hibbelig", "Lampenfieber", "Auftritt", "Bühne", "Rede",
                "erstes Date", "Hochzeit", "Vorfreude", "außer Atem", "aus der Puste", "schnaufen", "Treppe",
                "Treppensteigen", "bergauf", "Spaziergang", "spazieren", "Joggen", "Jogging", "Laufen", "Training", "Sport",
                "aufgedreht", "kribbelig", "Vorspielen", "Wandern",
                // Bewusst nicht: „Schwimmen“ (Atemübungen nie im oder am Wasser), „Fahrrad“,
                // „Radfahren“ (Atmen im Gehen lenkt im Verkehr ab).
            ),
        ),
        SituationRecommendation(
            situation = Situation.SLEEP,
            title = "Vor dem Einschlafen",
            shortLabel = "Einschlafen",
            rationale = "Langer, ruhiger Ausatem zum Herunterfahren vor dem Schlafen.",
            exerciseIds = listOf("4-7-8", "resonanz", "sitali"),
            keywords = listOf(
                "Schlaf", "schlafen", "Einschlafen", "kann nicht schlafen", "wach liegen", "liege wach",
                "durchschlafen", "aufwachen", "aufgewacht", "früh wach", "nachts", "abends", "Abendroutine",
                "Schlafenszeit", "Bett", "hinlegen", "Gedanken", "Gedankenkarussell", "Gedanken kreisen",
                "grübeln", "grüble", "Grübelei", "Sorgen", "Kopf voll", "abschalten", "runterkommen", "herunterfahren",
                "runterfahren", "loslassen", "Ruhe", "zur Ruhe kommen", "entspannen", "Entspannung",
                "Kopfkino", "Nickerchen", "Mittagsschlaf", "Albtraum", "Schäfchen zählen",
            ),
        ),
        SituationRecommendation(
            situation = Situation.FOCUS,
            title = "Konzentration & Fokus",
            shortLabel = "Fokus",
            rationale = "Ruhig und zugleich wach – ein gleichmäßiger Takt vor einer Aufgabe.",
            exerciseIds = listOf("box-4-4-4-4"),
            keywords = listOf(
                "Prüfung", "Prüfungsangst", "Klausur", "Test", "Examen", "Fahrprüfung", "Führerschein",
                "lernen", "lesen", "Hausaufgaben", "Schule", "Uni", "Universität", "Studium", "Arbeit", "Aufgabe", "Abgabe",
                "Büro", "Homeoffice", "Schreibtisch", "Meeting", "Besprechung", "Gespräch", "Bewerbung",
                "Vorstellungsgespräch", "Interview", "Vortrag", "Präsentation", "Wettkampf", "Turnier",
                "abgelenkt", "zerstreut", "unkonzentriert", "konzentrieren", "fokussieren", "klar denken",
                "Referat", "Projekt", "Abschlussarbeit", "Steuererklärung", "Videocall",
            ),
        ),
        SituationRecommendation(
            situation = Situation.HIGH_PHASE,
            title = "Wach & energiegeladen werden",
            shortLabel = "Wach werden",
            // „soll“: Dass die Feueratmung wach macht, ist kaum untersucht (TEXT-07).
            rationale = "Ein kräftiger Atem-Reiz, der anregen und wach machen soll.",
            exerciseIds = listOf("wim-hof", "feueratmung"),
            warning = INTENSE_WARNING,
            intense = true,
            // Bewusst keine keywords: Sie würden Leute aktiv zu Hyperventilationstechniken
            // schicken, obwohl die Situation selbst einschränkt („nur, wenn du dich gesund …“).
        ),
        SituationRecommendation(
            situation = Situation.CRASH,
            title = "Erschöpft oder ausgelaugt",
            shortLabel = "Erschöpft",
            // Der Nachsatz meint Erschöpfung nach Anstrengung (PEM, Glossar), ohne Diagnose zu nennen.
            rationale = "Nur sanfte, beruhigende Atmung – nichts Anstrengendes, das zusätzlich " +
                "auslaugt; das gilt besonders, wenn Anstrengung dich tageweise zurückwirft.",
            exerciseIds = listOf("resonanz", "cyclic-sighing"),
            keywords = listOf(
                "müde", "Müdigkeit", "Erschöpfung", "schlapp", "kaputt", "erledigt", "fertig", "platt",
                "erschlagen", "ausgepowert", "groggy", "kraftlos", "energielos", "Mittagstief",
                "Nachmittagstief", "Feierabend", "nach der Arbeit", "langer Tag", "schlecht geschlafen",
                "Nachtschicht", "Schichtdienst", "Schichtarbeit", "Jetlag", "Pause", "Auszeit", "erholen", "Erholung",
                "auftanken", "Kraft tanken", "durchatmen", "geschafft", "Akku leer", "Durchhänger", "neben der Spur",
                "Überstunden",
            ),
        ),
    )
}
