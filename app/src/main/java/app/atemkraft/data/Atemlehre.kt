package app.atemkraft.data

import app.atemkraft.domain.LoreInfluence
import app.atemkraft.domain.LorePattern
import app.atemkraft.domain.LorePhase
import app.atemkraft.domain.PhaseType
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.Reference
import app.atemkraft.domain.RhythmStep

/**
 * Inhalte der Seite „Atemlehre“: wie die Yoga-Lehre Einatmen, Ausatmen und Halten deutet
 * (TEXT-13). Das ist Lehre, keine Studienlage; was Studien sagen, steht je Karte unter
 * [LorePattern.studies] mit Quelle in [studyRefs].
 *
 * Grundlage: Krishnamacharya/Desikachar (Brmhana, Langhana, Samana), Hatha Yoga Pradipika,
 * Patanjalis Yoga-Sutra, Iyengar, Kuvalayananda. Verse deutsch zitiert („2,68“), geprüft gegen
 * die Übersetzung auf ashtangayoga.info (2026-10-17).
 */
object Atemlehre {

    val intro = "So deutet die Yoga-Tradition Einatmen, Ausatmen und Halten. Das ist Lehre, " +
        "keine Studienlage; was Studien sagen, steht jeweils unter „Studien“."

    val phases = listOf(
        LorePhase(PhaseType.INHALE, "Einatmen", "Puraka", "Aufnehmen. In der Lehre Prana: Kraft kommt herein."),
        LorePhase(PhaseType.HOLD_FULL, "Halten voll", "Antara Kumbhaka", "Fülle bewahren, sammeln."),
        LorePhase(PhaseType.EXHALE, "Ausatmen", "Rechaka", "Loslassen. In der Lehre Apana: Verbrauchtes geht hinaus."),
        LorePhase(PhaseType.HOLD_EMPTY, "Halten leer", "Bahya Kumbhaka", "Leere zulassen, still werden."),
    )

    // ---- Grundregeln ----

    val rulesTeaser = "Einatmen nimmt auf, Ausatmen lässt los; Halten verstärkt, Tempo dosiert."

    /** Je Regel: fett gesetzter Kern, dann die Erläuterung. */
    val rules = listOf(
        "Längeres Einatmen ist Brmhana" to " („nähren“): gilt als belebend und wärmend.",
        "Längeres Ausatmen ist Langhana" to " („leicht machen“): gilt als beruhigend und kühlend.",
        "Halten verstärkt die Phase davor." to " Voll halten vertieft Brmhana, leer halten vertieft Langhana.",
        "Tempo dosiert." to " Schnell gilt als Feuer und Hitze, langsam als fein und still.",
    )
    val samanaNote = "Sind Ein- und Ausatmen gleich lang, heißt das Samana: Ausgleich."
    val rulesCheck = "Prüfstein aus der Lehre von Krishnamacharya und Desikachar: Wird das " +
        "Ausatmen nach dem Halten hastig oder rau, war das Verhältnis zu ehrgeizig. Langsam steigern."

    // ---- Muster ----

    private fun inhale(s: Double, passive: Boolean = false) = RhythmStep(PhaseType.INHALE, s, passive)
    private fun exhale(s: Double) = RhythmStep(PhaseType.EXHALE, s)
    private fun holdFull(s: Double) = RhythmStep(PhaseType.HOLD_FULL, s)
    private fun holdEmpty(s: Double) = RhythmStep(PhaseType.HOLD_EMPTY, s)

    val patterns = listOf(
        LorePattern(
            id = "langhana",
            title = "Ein kurz, Aus lang",
            linkName = "Langhana",
            rhythm = listOf(inhale(4.0), exhale(6.0)),
            counts = "z. B. Ein 4\u00A0· Aus 6",
            teaser = "Langhana: Längeres Ausatmen gilt in der Yoga-Lehre als beruhigend und kühlend.",
            teaching = "Der Schwerpunkt liegt beim Loslassen (Apana). Langhana heißt „leicht " +
                "machen, reduzieren“. Die Lehre setzt es ein, wenn zu viel da ist: Hitze, Unruhe, " +
                "Ärger, kreisende Gedanken. Das Ausatmen wird schrittweise verlängert, bis es etwa " +
                "doppelt so lang ist wie das Einatmen.",
            typical = "Abends, vor dem Schlafen, bei Unruhe, Hitze oder Ärger.",
            studies = "Beim Ausatmen schlägt das Herz langsamer, die Richtung passt. Ob ein langes " +
                "Ausatmen mehr bringt als gleich langes, langsames Atmen, ist offen: Eine Studie " +
                "fand mehr Herzratenvariabilität (Bae 2021), andere keinen Unterschied " +
                "(Meehan & Shaffer 2024, Birdee 2023).",
            exerciseIds = listOf("resonanz", "bhramari", "cyclic-sighing", "zwerchfell", "sitali", "lippenbremse", RandomPatternGenerator.ID),
        ),
        LorePattern(
            id = "brmhana",
            title = "Ein lang, Aus kurz",
            linkName = "Brmhana",
            // Spiegelbild von Langhana (Ein 4 · Aus 6), gleich lang wie Samana (5 · 5): Die drei
            // Balken dauern je 10 s, Ein und Aus lassen sich direkt vergleichen (Nutzer 2026-10-21).
            rhythm = listOf(inhale(6.0), exhale(4.0)),
            counts = "z. B. Ein 6\u00A0· Aus 4",
            teaser = "Brmhana: Längeres Einatmen gilt in der Yoga-Lehre als belebend und wärmend.",
            teaching = "Der Schwerpunkt liegt beim Aufnehmen (Prana). Brmhana heißt „nähren, " +
                "ausdehnen“. Die Lehre setzt es ein, wenn zu wenig da ist: Müdigkeit, Schwere, " +
                "Antriebslosigkeit, Kältegefühl. Die Brust weitet sich, der Körper richtet sich auf.",
            typical = "Morgens, bei Müdigkeit, Schwere oder Trägheit.",
            studies = "Beim Einatmen steigt der Puls kurz an, die Richtung ist plausibel. Gezielt " +
                "untersucht ist langes Einatmen kaum.",
            exerciseIds = listOf(RandomPatternGenerator.ID),
            appNote = "Das Muster des Tages ist an manchen Tagen sanft aktivierend; dann atmet es länger ein als aus.",
        ),
        LorePattern(
            id = "samana",
            title = "Ein gleich Aus, langsam",
            linkName = "Samana",
            rhythm = listOf(inhale(5.0), exhale(5.0)),
            counts = "z. B. Ein 5\u00A0· Aus 5",
            teaser = "Samana: Gleich langes, langsames Atmen gilt als ausgleichend und sammelnd.",
            teaching = "Weder aufbauen noch reduzieren: Ein- und Ausatmen halten sich die Waage. " +
                "Patanjali beschreibt das Ziel als Atem, der „lang und fein“ wird (Yoga-Sutra " +
                "2,50). Gilt als Grundlage, bevor man Verhältnisse verschiebt, und als Vorbereitung " +
                "auf Meditation.",
            typical = "Zum Ankommen, vor der Meditation, wenn man die Mitte sucht.",
            studies = "Der am besten untersuchte Teil: Langsames Atmen um 6 Atemzüge pro Minute erhöht " +
                "die Herzratenvariabilität, das zeigt eine Auswertung von 223 Studien (Laborde " +
                "2022). Das Tempo zählt dabei mehr als das Verhältnis. Über viele Atemformen hinweg " +
                "berichteten Übende in 12 Studien etwas weniger Stress als ohne Atemübung, ein " +
                "kleiner bis mittlerer Effekt (Fincham 2023).",
            exerciseIds = listOf("nadi-shodhana"),
        ),
        LorePattern(
            id = "feuer",
            title = "Ein gleich Aus, schnell",
            linkName = "Feuer",
            rhythm = List(8) { listOf(inhale(1.0), exhale(1.0)) }.flatten(),
            counts = "je etwa 1 s, kräftig",
            teaser = "Schnelles, kräftiges Atmen gilt in der Yoga-Lehre als Feuer: stark wärmend.",
            teaching = "Bhastrika, der „Blasebalg“: Ein- und Ausatmen gleich kräftig und schnell. " +
                "Nach der Lehre facht es Agni an, das innere Feuer am Nabel. Die Hatha Yoga " +
                "Pradipika (2,59–2,67) zählt es zu ihren acht Kumbhakas; dort folgt dem Blasebalg " +
                "ein Halten.",
            typical = "Bei Trägheit und Kälte, im Sitzen.",
            studies = "Schnelles Atmen senkte in einer Studie die Herzratenvariabilität, es " +
                "aktiviert (Chetry 2026). Es senkt auch das CO₂ im Blut; Schwindel und Kribbeln " +
                "sind möglich.",
            appNote = "Keine eigene Übung; verwandt ist die Atemphase von Wim Hof.",
            caution = true,
            warning = "Nie im oder am Wasser, nicht beim Fahren.",
        ),
        LorePattern(
            id = "kapalabhati",
            title = "Aus aktiv, Ein passiv, schnell",
            linkName = "Kapalabhati",
            rhythm = List(8) { listOf(exhale(1.0), inhale(1.0, passive = true)) }.flatten(),
            counts = "Stoß aus, Einatmen von selbst",
            teaser = "Kurze, aktive Ausatemstöße gelten in der Yoga-Lehre als wärmend und klärend.",
            teaching = "Kapalabhati heißt „leuchtender Schädel“. Die Hatha Yoga Pradipika (2,35) " +
                "zählt es zu den sechs Reinigungen (Shatkarma) und vergleicht es mit dem Blasebalg " +
                "eines Schmieds: Ein- und Ausatmen sehr schnell. Heute wird meist nur das Ausatmen " +
                "aktiv geübt, das Einatmen geschieht von selbst.",
            typical = "Bei Trägheit, morgens.",
            studies = "Kaum untersucht. Dass schnelles Atmen aktiviert, ist plausibel.",
            exerciseIds = listOf("feueratmung"),
            caution = true,
        ),
        LorePattern(
            id = "halten-voll",
            title = "Mit Halten nach dem Einatmen",
            linkName = "Halten voll",
            rhythm = listOf(inhale(4.0), holdFull(4.0), exhale(6.0)),
            counts = "z. B. Ein 4\u00A0· Halten 4\u00A0· Aus 6",
            teaser = "Halten nach dem Einatmen vertieft Brmhana: Fülle bewahren, Kraft sammeln.",
            teaching = "Antara Kumbhaka hält Prana im Körper. Die Hatha-Lehre sieht im Halten den " +
                "Kern des Pranayama: „Bewegt sich der Atem, bewegt sich der Geist; ist der Atem " +
                "still, wird der Geist still“ (Hatha Yoga Pradipika 2,2). Traditionell mit " +
                "gesenktem Kinn (Jalandhara Bandha). Die Kehle bleibt weich, nicht pressen.",
            typical = "Zum Sammeln und Konzentrieren.",
            studies = "Zum Halten selbst gibt es kaum Daten. Pressen bei geschlossener Kehle lässt " +
                "den Blutdruck schwanken.",
            exerciseIds = listOf("4-7-8", RandomPatternGenerator.ID),
            appNote = "4-7-8 verbindet volles Halten mit langem Ausatmen: erst sammeln, dann loslassen.",
            origin = "Die Zuordnung zu Brmhana folgt der Viniyoga-Lehre; andere Schulen gewichten anders.",
        ),
        LorePattern(
            id = "halten-leer",
            title = "Mit Halten nach dem Ausatmen",
            linkName = "Halten leer",
            // Dieselben Werte wie „Mit Halten nach dem Einatmen“: Beide Balken sind 14 s lang, Ein
            // und Aus stehen untereinander, nur der Ort des Haltens unterscheidet sich (Nutzer 2026-10-21).
            rhythm = listOf(inhale(4.0), exhale(6.0), holdEmpty(4.0)),
            counts = "z. B. Ein 4\u00A0· Aus 6\u00A0· Halten 4",
            teaser = "Halten nach dem Ausatmen vertieft Langhana: Leere zulassen, still werden.",
            teaching = "Bahya Kumbhaka gilt als feiner und anspruchsvoller als das Halten mit " +
                "voller Lunge. Patanjali nennt Ausatmen und Anhalten als Weg zu einem klaren Geist " +
                "(Yoga-Sutra 1,34). Desikachar beschreibt die Pause nach dem Ausatmen als Zeit, in " +
                "der das innere Feuer Verbrauchtes verbrennt. Traditionell mit eingezogenem Bauch " +
                "(Uddiyana Bandha).",
            typical = "Für Geübte, in ruhiger Verfassung.",
            studies = "Kaum Daten. Mit leerer Lunge ist der Sauerstoffvorrat klein, Atemnot kommt früher.",
            exerciseIds = listOf("box-4-4-4-4", RandomPatternGenerator.ID),
            appNote = "Bei der Box-Atmung ist die leere Pause kurz. Wim Hof hält ebenfalls leer, " +
                "aber nach schnellem Atmen; das ist ein anderer Zusammenhang.",
            origin = "Die Zuordnung zu Langhana folgt der Viniyoga-Lehre; andere Schulen gewichten anders.",
        ),
        LorePattern(
            id = "sama-vritti",
            title = "Alle vier Phasen gleich",
            linkName = "Sama Vritti",
            rhythm = listOf(inhale(4.0), holdFull(4.0), exhale(4.0), holdEmpty(4.0)),
            counts = "z. B. 4\u00A0· 4\u00A0· 4\u00A0· 4",
            teaser = "Alle vier Phasen gleich lang heißt Sama Vritti: Ordnung und Gleichmaß.",
            teaching = "Sama Vritti, der gleichmäßige Atem, in der Form mit Pausen: Fülle und Leere " +
                "bekommen gleich viel Raum. B. K. S. Iyengar stellt ihm Vishama Vritti gegenüber, " +
                "das Atmen in ungleichen Verhältnissen. Die Lehre sieht darin eine Struktur, die " +
                "den Geist ordnet.",
            typical = "Zum Fokussieren, wenn es drunter und drüber geht.",
            studies = "In einer Studie hob täglich 5 Minuten Box-Atmung die Stimmung, aber weniger " +
                "als Cyclic Sighing (Balban 2023).",
            exerciseIds = listOf("box-4-4-4-4"),
        ),
        LorePattern(
            id = "klassisch",
            title = "Das klassische Maß 1 : 4 : 2",
            linkName = "1 : 4 : 2",
            rhythm = listOf(inhale(4.0), holdFull(16.0), exhale(8.0)),
            counts = "z. B. Ein 4\u00A0· Halten 16\u00A0· Aus 8",
            teaser = "1 : 4 : 2 gilt als klassisches Maß: kurz aufnehmen, lange halten, doppelt so lang lösen.",
            teaching = "Gezählt wird in Matras, einer traditionellen Zeiteinheit. Kuvalayananda " +
                "nennt 1 : 4 : 2 das „meistbevorzugte“ Maß und rät Anfängern zu 1 : 2 : 2. Geübt " +
                "wird es meist in der Wechselatmung. Die Hatha Yoga Pradipika beschreibt Stufen " +
                "der Übung: Anfangs schwitzt der Übende, später zittert er (2,12).",
            typical = "Lange Praxis, am besten mit Lehrer.",
            studies = "Als Verhältnis nicht untersucht. Lange Pausen sind nichts für den Einstieg.",
            exerciseIds = listOf("nadi-shodhana"),
            appNote = "In der App ohne Halten.",
            origin = "Wo das Maß zuerst steht, ist unklar; genannt wird die Shandilya-Upanishad, nicht geprüft.",
        ),
        // Viloma I und II je als eigene Karte wie die beiden Halten-Karten: gegensätzliche
        // Richtung, eigener Rhythmus (Nutzer 2026-10-21). Werte wie die Übungen in der App.
        LorePattern(
            id = "viloma-ein",
            title = "Einatmen in Stufen (Viloma I)",
            linkName = "Viloma I",
            rhythm = listOf(inhale(2.0), holdFull(2.0), inhale(2.0), holdFull(2.0), inhale(2.0), exhale(8.0)),
            counts = "z. B. Ein in drei Stufen\u00A0· Aus 8",
            teaser = "Einatmen in Stufen gilt als sanftes Brmhana: aufbauen, ohne zu drängen.",
            teaching = "Viloma heißt „gegen den Strich“: Das Einatmen wird in kleine Abschnitte mit " +
                "kurzen Pausen geteilt, danach fließt das Ausatmen in einem Zug. Stufen beim " +
                "Einatmen gelten als aufbauend (Brmhana). Iyengar teilt in Abschnitte von 2–3 s " +
                "mit ebenso langen Pausen, bis die Lunge voll ist. Die Pausen sind kurzes Halten, " +
                "aber mitten im Einatmen statt nach dem vollen Atemzug.",
            typical = "Zum Einstieg, im Liegen, wenn lange Phasen noch nicht gehen.",
            studies = "Nicht untersucht; die Studien zu „Anuloma Viloma“ betreffen die Wechselatmung.",
            exerciseIds = listOf("viloma-ein"),
            appNote = "In der App mit drei Stufen; der Atemkreis wächst stufenweise und bleibt in den Pausen stehen.",
        ),
        LorePattern(
            id = "viloma-aus",
            title = "Ausatmen in Stufen (Viloma II)",
            linkName = "Viloma II",
            rhythm = listOf(inhale(6.0), exhale(2.0), holdEmpty(2.0), exhale(2.0), holdEmpty(2.0), exhale(2.0)),
            counts = "z. B. Ein 6\u00A0· Aus in drei Stufen",
            teaser = "Ausatmen in Stufen gilt als sanftes Langhana: loslassen, leicht werden.",
            teaching = "Umgekehrt zu Viloma I: Das Einatmen fließt in einem Zug, das Ausatmen wird in " +
                "Abschnitte mit kurzen Pausen geteilt, bis die Lunge leer ist. Stufen beim " +
                "Ausatmen gelten als lösend (Langhana). Iyengar beschreibt die Übung für Anfänger " +
                "im Liegen und nennt als Erfahrung „Leichtigkeit“. Die Pausen sind kurzes Halten, " +
                "aber mitten im Ausatmen statt nach dem leeren Atemzug.",
            typical = "Zum Einstieg, im Liegen, zum Loslassen.",
            studies = "Nicht untersucht; die Studien zu „Anuloma Viloma“ betreffen die Wechselatmung.",
            exerciseIds = listOf("viloma-aus"),
            appNote = "In der App mit drei Stufen; der Atemkreis schrumpft stufenweise und bleibt in den Pausen stehen.",
        ),
    )

    fun pattern(id: String): LorePattern? = patterns.firstOrNull { it.id == id }

    // ---- Weitere Einflüsse ----

    val influencesTeaser = "Auch der Weg der Luft und die Tageszeit gelten in der Lehre als wichtig."
    val influences = listOf(
        LoreInfluence(
            "Atemweg",
            "Rechtes Nasenloch: Sonne (Pingala), gilt als wärmend. Linkes Nasenloch: Mond (Ida), " +
                "gilt als kühlend. Über die gerollte Zunge (Sitali): kühlend. Mit leicht verengter " +
                "Kehle (Ujjayi): wärmend und sammelnd. Summend (Bhramari): lenkt nach innen.",
            listOf("sitali", "bhramari", "nadi-shodhana"),
        ),
        LoreInfluence("Tageszeit", "Morgens eher Brmhana, abends eher Langhana."),
        LoreInfluence(
            "Typ (Ayurveda)",
            "Bei Hitze und Reizbarkeit (Pitta) Langhana. Bei Schwere und Trägheit (Kapha) " +
                "Brmhana. Bei Unruhe und Zerstreutheit (Vata) gleichmäßig und langsam, ohne lange Pausen.",
        ),
    )
    val influencesQuote = "„Wie man Löwe, Elefant und Tiger nur langsam zähmt, so auch den Atem“ " +
        "(Hatha Yoga Pradipika 2,15)."

    // ---- Quellen ----

    val sourcesTeaser = "Klassische Texte für die Lehre, Studien für die Einordnung."

    /** Klassische Texte und Lehrbücher, Verse deutsch zitiert. */
    val textSources = listOf(
        "Patanjali: Yoga-Sutra 1,34; 2,49–2,53",
        "Svatmarama: Hatha Yoga Pradipika 2,2; 2,7–2,10; 2,12; 2,15; 2,35; 2,44; 2,51–2,68",
        "T. K. V. Desikachar: The Heart of Yoga (1995)",
        "B. K. S. Iyengar: Light on Pranayama (1981)",
        "Swami Kuvalayananda: Pranayama (1966)",
    )

    /** Studien, auf die sich die „Studien“-Zeilen der Karten stützen. */
    val studyRefs: List<Reference> = listOf(
        Refs.laborde2022,
        Refs.fincham2023,
        Refs.bae2021,
        Refs.meehan2024,
        Refs.birdee2023,
        Refs.balban2023,
        Refs.chetry2026,
    )
}
