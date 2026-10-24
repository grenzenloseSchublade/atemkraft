package app.atemkraft.data

import app.atemkraft.domain.LearnTopic
import app.atemkraft.domain.Reference

/**
 * Inhalte des Kapitels „Körper“ im Tab „Lernen“ (Entwurf
 * docs/recherche/2026-10-25-geschichte-koerper-entwurf.md, Eigentümer 2026-10-25): was beim Atmen
 * im Körper geschieht, in Laiensprache – Atemmechanik, CO₂ als Atemantrieb, die Nase, die Modelle
 * für langsames Atmen, Atem und Gefühl. Lehrbuchwissen steht als solches, Modelle und Vermutungen
 * sind benannt; Studien mit „(Name Jahr)“ und Eintrag in [Refs] (TEXT-07, -12).
 *
 * Die letzte Karte [limits] sagt, wo eine App aufhört und Fachleute gefragt sind; die Karte
 * „Sicherheit“ der Seite „Pranayama“ verweist hierher (Eigentümer 2026-10-25). Sie übernimmt den
 * Disclaimer aus SEC-LEGAL-02 wörtlich.
 */
object Koerper {

    val intro = "Was beim Atmen im Körper geschieht, in Laiensprache und mit Quellen. Vieles ist " +
        "Lehrbuchwissen; wo es Modell oder Vermutung ist, steht es dabei."

    /** Disclaimer aus SEC-LEGAL-02, wörtlich wie in der Store-Beschreibung. */
    private const val DISCLAIMER = "Kein Medizinprodukt; diagnostiziert, behandelt, heilt oder verhindert keine " +
        "Erkrankung. Für medizinischen Rat wende dich an Fachpersonal."

    /** Die Grenzen der App, Schlusskarte des Kapitels. */
    val limits = LearnTopic(
        id = "grenzen",
        title = "Grenzen einer App: wann Fachleute",
        teaser = "Die App zeigt Atemübungen; sie stellt keine Diagnose und ersetzt keine Behandlung.",
        paragraphs = listOf(
            "$DISCLAIMER Atemkraft misst nichts und sagt dir nicht, was dir fehlt. Was sie kann: Übungen " +
                "zeigen, anleiten und ehrlich einordnen, was Studien dazu wissen.",
            "Manche Beschwerden brauchen Fachleute. Dauerhaft zu schnelles oder unruhiges Atmen mit " +
                "Kurzatmigkeit, Seufzen und Schwindel (dysfunktionale Atmung): ärztlich abklären, dann " +
                "Physiotherapie mit Atemschwerpunkt; Atemübungen allein sind dafür kaum untersucht (Jones " +
                "2013). Wiederkehrende Panikattacken: Psychotherapie, dort auch Atemtraining mit CO₂-Messung " +
                "(Meuret 2010). Asthma oder COPD: ärztliche Behandlung und Physiotherapie mit " +
                "Atemschwerpunkt; Buteyko oder Lippenbremse kommen dazu, nicht an die Stelle. Schnarchen mit " +
                "Atemaussetzern oder Tagesmüdigkeit: Schlaflabor.",
            "Schnelle Techniken haben eigene Nebenwirkungen: Schwindel, Kribbeln, starke Gefühle, selten " +
                "Ohnmacht. In Studien mit psychisch Erkrankten traten sie häufiger auf als bei langsamen " +
                "Techniken (Mütze 2025). Bei Schwindel, Druck oder Atemnot: aufhören, normal weiteratmen; " +
                "hält es an, ärztlich abklären.",
        ),
        exerciseIds = listOf("buteyko", "lippenbremse"),
        seeAlso = "Siehe Seite „Pranayama“, Karte „Sicherheit“.",
    )

    /** Die Karten des Kapitels, [limits] als letzte. */
    val topics = listOf(
        LearnTopic(
            id = "luft",
            title = "Wie die Luft in den Körper kommt",
            teaser = "Das Zwerchfell zieht die Luft hinein; ausatmen geschieht in Ruhe von selbst.",
            paragraphs = listOf(
                "Die Lunge hat keine eigenen Muskeln. Beim Einatmen spannt sich das Zwerchfell, eine " +
                    "Muskelkuppel unter den Lungen, und senkt sich; die Zwischenrippenmuskeln heben den " +
                    "Brustkorb. Der Raum wird größer, und Luft strömt nach. In Ruhe leistet das Zwerchfell " +
                    "den größten Teil der Arbeit.",
                "Ausatmen braucht in Ruhe keine Kraft: Lunge und Brustkorb federn zurück wie ein " +
                    "losgelassener Luftballon. Erst bei Anstrengung, beim Singen oder bei Übungen wie " +
                    "Kapalabhati pressen die Bauchmuskeln die Luft aktiv hinaus, und Hilfsmuskeln an Hals " +
                    "und Schultern helfen beim Einatmen.",
                "„Bauchatmung“ ist deshalb ein Bild: Luft gelangt nie in den Bauch. Senkt sich das " +
                    "Zwerchfell, weichen die Organe nach vorn, und die Bauchdecke hebt sich. Die Lunge füllt " +
                    "sich dabei als Ganzes; Bauch, Flanken und Brust bei Dirgha lenken die Aufmerksamkeit, " +
                    "nicht die Luft.",
            ),
            source = "Lehrbuch der Physiologie (z. B. Silbernagl & Despopoulos: Taschenatlas Physiologie); " +
                "De Troyer & Boriek: Mechanics of the Respiratory Muscles (2011)",
            exerciseIds = listOf("zwerchfell", "dirgha", "feueratmung"),
        ),
        LearnTopic(
            id = "co2",
            title = "CO₂ ist der Atemantrieb",
            teaser = "Nicht Sauerstoffmangel treibt den Atem, sondern steigendes CO₂; daher kommt der Lufthunger.",
            paragraphs = listOf(
                "Fühler im Hirnstamm messen laufend das Kohlendioxid im Blut, genauer den Säuregrad, den es " +
                    "erzeugt. Steigt CO₂, wächst der Drang zu atmen. Sauerstoff spielt erst eine Rolle, wenn " +
                    "er deutlich fällt. Deshalb kommt der Lufthunger beim Halten vom CO₂, lange bevor " +
                    "Sauerstoff knapp wird.",
                "Wer schneller atmet, als der Körper braucht, atmet CO₂ ab: Hypokapnie. Gefäße im Gehirn " +
                    "verengen sich, Kribbeln, Schwindel und Benommenheit folgen. Das ist der Kern schneller " +
                    "Techniken und der Grund, warum sie nicht ins Wasser gehören: Nach Hyperventilation " +
                    "setzt der Atemreiz zu spät ein (Boyd 2015).",
                "Oft wird der Bohr-Effekt (1904) bemüht: Mehr CO₂ im Gewebe lässt das Blut Sauerstoff " +
                    "leichter abgeben. Das ist Lehrbuch. Dass sich diese Abgabe durch " +
                    "„CO₂-Toleranz-Training“ im Alltag verbessert, ist nicht gezeigt; in Ruhe ist das Blut " +
                    "ohnehin fast voll gesättigt, und unter Buteyko änderte sich das CO₂ nicht (Bowler 1998).",
            ),
            source = "Lehrbuch der Physiologie; Bohr, Hasselbalch & Krogh (1904)",
            exerciseIds = listOf("buteyko", "wim-hof"),
            seeAlso = "Siehe Seite „Begriffe erklärt“: „CO₂-Toleranz“ und „Hypokapnie“.",
        ),
        LearnTopic(
            id = "nase",
            title = "Die Nase",
            teaser = "Die Nase wärmt, filtert und liefert Stickstoffmonoxid; vieles andere ist kaum untersucht.",
            paragraphs = listOf(
                "Die Nase erwärmt und befeuchtet die Luft und filtert Staub. In den Nebenhöhlen entsteht " +
                    "laufend Stickstoffmonoxid (NO), das mit der Luft in die Lunge gelangt und dort Gefäße " +
                    "weitet; bei sechs von acht Gesunden lag der Sauerstoffwert mit Nasenatmung etwa 10 % " +
                    "höher (Lundberg 1996). Summen vervielfacht das NO (Weitzberg 2002).",
                "Hirnstrom-Messungen bei sieben Epilepsie-Patienten zeigten, dass Nasenatmung Rhythmen in " +
                    "Riechhirn, Mandelkern und Hippocampus taktet, Mundatmung nicht; in Aufgaben erkannten " +
                    "Gesunde ängstliche Gesichter beim Einatmen durch die Nase etwas schneller (Zelano " +
                    "2016). Das ist spannend und klein; „besseres Gedächtnis durch Nasenatmung“ folgt daraus nicht.",
                "Mund zukleben im Schlaf ist ein Trend aus sozialen Medien. Eine Übersicht fand 10 kleine " +
                    "Studien mit 213 Personen: Zwei zeigten weniger Atemaussetzer bei leichter Schlafapnoe, " +
                    "andere nichts; bei verstopfter Nase droht Erstickung (Rhee 2025). Schnarchen mit " +
                    "Atemaussetzern gehört ins Schlaflabor, nicht unter Klebeband.",
            ),
            exerciseIds = listOf("nasenatmung", "bhramari"),
        ),
        LearnTopic(
            id = "modelle",
            title = "Vagus, Baroreflex, RSA: Modelle für langsames Atmen",
            teaser = "Drei Modelle erklären, wie langsames Atmen beruhigt; keines ist abschließend bewiesen.",
            paragraphs = listOf(
                "Beim Einatmen schlägt das Herz etwas schneller, beim Ausatmen langsamer. Dieses Schwingen " +
                    "heißt respiratorische Sinusarrhythmie (RSA) und läuft über den Vagusnerv, den " +
                    "Hauptnerv des Ruhe-Systems. Bei langsamem Atmen wird die Schwingung groß; genau das " +
                    "misst die Herzratenvariabilität (HRV).",
                "Das zweite Modell ist der Baroreflex, der Regelkreis des Blutdrucks. Er antwortet mit " +
                    "etwa 5 s Verzögerung; bei etwa 6 Atemzügen pro Minute geraten Atem, Blutdruck und " +
                    "Puls in Resonanz und schwingen gemeinsam (Vaschillo 2002, Lehrer & Gevirtz 2014). Das " +
                    "dritte: Dehnungsfühler der Lunge melden langes Einatmen an den Hirnstamm und dämpfen " +
                    "dort (Noble & Hochman 2019; Gerritsen & Band 2018).",
                "Alle drei sind Modelle: plausibel, in Teilen gemessen, als Ganzes nicht bewiesen. Sicher " +
                    "ist, dass langsames Atmen die HRV hebt (Laborde 2022) und viele sich danach ruhiger " +
                    "fühlen (Zaccaro 2018). „Den Vagus stärken“ ist dagegen eine Redensart: Die HRV zeigt " +
                    "den Zustand von jetzt, keinen trainierten Nerv.",
            ),
            exerciseIds = listOf("resonanz"),
            seeAlso = "Siehe Seite „Begriffe erklärt“: Vagus, Baroreflex, RSA und HRV.",
        ),
        LearnTopic(
            id = "gefuehl",
            title = "Atem und Gefühl: Panik und Hyperventilation",
            teaser = "Jedes Gefühl hat sein Atemmuster; bei Panik ist tiefes Durchatmen oft der falsche Weg.",
            paragraphs = listOf(
                "Angst atmet schnell und flach, Trauer in Seufzern, Ärger kräftig: Bei 36 Schauspielern " +
                    "ließen sich sechs Grundgefühle an ihrem Atemmuster unterscheiden (Bloch 1991, klein). " +
                    "Umgekehrt kann ein Atemmuster ein Gefühl anstoßen; auch das ist nur in kleinen Studien gezeigt.",
                "Bei Panik atmen viele zu viel: Das CO₂ sinkt, Kribbeln, Schwindel und Herzklopfen folgen " +
                    "und wirken wie Beweise für die Gefahr. Der Rat „tief durchatmen“ verstärkt das oft. In " +
                    "zwei Studien lernten Menschen mit Panikstörung mit einem CO₂-Messgerät, langsamer und " +
                    "flacher zu atmen; die Beschwerden gingen zurück, und der CO₂-Anstieg ging der Besserung " +
                    "voraus (Meuret 2009, 2010; je etwa 40 Personen).",
                "Daraus folgt die Regel der App: Bei akutem Stress langsames Atmen ohne Halten und ohne " +
                    "tiefe Züge, zum Beispiel ein langes Ausatmen. Eine Panikstörung ist eine Erkrankung und " +
                    "gehört in die Hände von Fachleuten; die App begleitet, sie ersetzt keine Behandlung.",
            ),
            exerciseIds = listOf("cyclic-sighing", "resonanz"),
            seeAlso = "Siehe Anlass „Akuter Stress“ im Tab „Atmen“ und Seite „Atemlehre“, Karte „Ein kurz, Aus lang“.",
        ),
        limits,
    )

    // ---- Quellen ----

    val sourcesTeaser = "Lehrbücher für die Grundlagen, Studien für die Einordnung."

    /** Lehrbuch zur Physiologie (ohne Kennung). */
    val textSources = listOf(
        "Silbernagl S. & Despopoulos A.: Taschenatlas Physiologie. Thieme (aktuelle Auflage)",
    )

    /**
     * Aufsätze zur Physiologie mit DOI (geprüft 2026-10-25 gegen Crossref), unter „Literatur“ wie
     * [textSources]; als [Reference], damit die Kennung wie bei den Studien eine eigene Zeile hat.
     */
    val literatureRefs: List<Reference> = listOf(
        Reference(
            "De Troyer A. & Boriek A. M. (2011): Mechanics of the Respiratory Muscles. Comprehensive " +
                "Physiology 1(3):1273–1300. Lehrbuch-Übersicht zu Zwerchfell, Rippen- und Bauchmuskeln.",
            "doi:10.1002/cphy.c100009",
        ),
        Reference(
            "Bohr C., Hasselbalch K. & Krogh A. (1904): Ueber einen in biologischer Beziehung wichtigen " +
                "Einfluss, den die Kohlensäurespannung des Blutes auf dessen Sauerstoffbindung übt. " +
                "Skandinavisches Archiv für Physiologie 16(2):402–412. Die erste Beschreibung des Bohr-Effekts.",
            "doi:10.1111/j.1748-1716.1904.tb01382.x",
        ),
    )

    /** Studien, auf die sich die Karten stützen. */
    val studyRefs: List<Reference> = listOf(
        Refs.boyd2015,
        Refs.bowler1998,
        Refs.nasalNo1996,
        Refs.weitzberg2002,
        Refs.zelano2016,
        Refs.rhee2025,
        Refs.vaschillo2002,
        Refs.lehrer2014,
        Refs.noble2019,
        Refs.gerritsen2018,
        Refs.laborde2022,
        Refs.zaccaro2018,
        Refs.bloch1991,
        Refs.meuret2009,
        Refs.meuret2010,
        Refs.jones2013,
        Refs.muetze2025,
    )
}
