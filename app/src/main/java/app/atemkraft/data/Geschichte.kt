package app.atemkraft.data

import app.atemkraft.domain.LearnSection
import app.atemkraft.domain.LearnTopic
import app.atemkraft.domain.Reference

/** Ein Eintrag der Zeitleiste: [year] fett („1924“, „1950er“), dahinter ein Halbsatz [text]. */
data class HistoryEvent(val year: String, val text: String)

/**
 * Inhalte des Kapitels „Geschichte des Atmens“ im Tab „Lernen“ (Entwurf
 * docs/recherche/2026-10-25-geschichte-koerper-entwurf.md, Eigentümer 2026-10-25): wie der Atem
 * zur Übung wurde, in Indien, in anderen Traditionen und im Westen, dazu eine Zeitleiste und die
 * Frage nach dem Alter des Yoga. Datierungen mit „etwa“ oder „um“, Legenden als solche benannt;
 * die Lehre steht zuschreibend, Studien mit „(Name Jahr)“ und Eintrag in [Refs] (TEXT-12, -13).
 *
 * Sanskrit und Pāli klein in IAST zwischen Sternchen, Chinesisch in Pinyin ebenso (TEXT-14);
 * Personen, Texttitel, Schulen und Techniken (Tummo, Qigong, Zen) aufrecht.
 */
object Geschichte {

    val intro = "So wurde der Atem zur Übung: in Texten, bei Lehrern und im Labor, so weit die Forschung " +
        "es heute weiß. Jahreszahlen sind oft Schätzungen; was offen ist, steht dabei."

    // ---- Indien ----

    private val india = LearnSection(
        id = "indien",
        title = "Indien",
        teaser = "Vom Atem als Lebenskraft zur Technik mit Halten und Maß: etwa 2500 Jahre in vier Karten.",
        topics = listOf(
            LearnTopic(
                id = "upanishaden",
                title = "Atem als Leben: *prāṇa* in den Upaniṣaden",
                period = "etwa 7.–6. Jahrhundert v. Chr.",
                teaser = "In den Upaniṣaden ist *prāṇa* der Atem, der alle Lebenskräfte trägt, aber noch keine Übung.",
                paragraphs = listOf(
                    "Die ältesten Upaniṣaden entstanden etwa im 7. bis 6. Jahrhundert v. Chr. In der " +
                        "Chāndogya-Upaniṣad (5,1) streiten die Lebenskräfte, wer die wichtigste ist. Sprache, " +
                        "Auge, Ohr und Denken verlassen den Körper nacheinander, und er lebt weiter. Als der " +
                        "Atem gehen will, reißt er alle anderen mit. So wird *prāṇa* zum Sinnbild des Lebens selbst.",
                    "Die Praśna-Upaniṣad (3) teilt *prāṇa* in fünf Strömungen. Daher stammen die *vāyus*, " +
                        "die die Seite „Atemlehre“ beschreibt: ein Bild der Lehre, keine Anatomie.",
                    "Eine Technik ist das noch nicht. Die Texte deuten den Atem, sie leiten nicht an. Übungen " +
                        "mit Zählen, Halten und Verhältnissen kommen erst viele Jahrhunderte später.",
                ),
                source = "Chāndogya-Upaniṣad 5,1; Praśna-Upaniṣad 3; Olivelle: The Early Upaniṣads (1998)",
            ),
            LearnTopic(
                id = "patanjali",
                title = "Patanjali: der Atem als viertes Glied",
                period = "etwa 4.–5. Jahrhundert n. Chr.",
                teaser = "Patanjali macht den Atem zur Stufe auf dem Weg zur Sammlung; fünf Sätze genügen ihm dafür.",
                paragraphs = listOf(
                    "Das Yoga-Sutra beschreibt acht Glieder des Yoga; *prāṇāyāma* ist das vierte. Fünf Sätze " +
                        "widmet Patanjali dem Atem: Er wird nach Ort, Zeit und Zahl geregelt und „lang und " +
                        "fein“ (2,49–2,50). Yoga ist hier vor allem Schulung des Geistes; Körperhaltungen " +
                        "nehmen wenig Raum ein.",
                    "Wann der Text entstand, ist umstritten. Die Indologie datiert ihn heute meist auf etwa " +
                        "325 bis 425 n. Chr. (Maas 2013); ältere Angaben reichen bis ins 2. Jahrhundert v. Chr. " +
                        "Sicher ist nur: Er ist viele Jahrhunderte jünger als die Upaniṣaden.",
                    "Auch die Bhagavadgītā kennt den Atem als Opfer: Manche gießen den Einatem in den Ausatem " +
                        "und umgekehrt und halten beide an (4,29). Eine Anleitung ist das ebenso wenig; wie " +
                        "geübt wird, bleibt dem Lehrer überlassen.",
                ),
                source = "Patanjali: Yoga-Sutra 2,29; 2,49–2,53; Bhagavadgītā 4,29; Maas: A Concise " +
                    "Historiography of Classical Yoga Philosophy (2013)",
                seeAlso = "Siehe Seite „Pranayama“, Karte „Was Pranayama ist“.",
            ),
            LearnTopic(
                id = "hatha",
                title = "Haṭha: der Atem wird Körpertechnik",
                period = "etwa 11.–15. Jahrhundert",
                teaser = "Erst im Haṭha-Yoga des Mittelalters werden Halten, Verschlüsse und Kanäle zur Methode.",
                paragraphs = listOf(
                    "Der älteste bekannte Haṭha-Text ist die Amṛtasiddhi aus dem 11. Jahrhundert, entstanden " +
                        "in einem buddhistisch-tantrischen Umfeld (Mallinson & Szántó 2022). *haṭha* heißt " +
                        "„Kraft, Nachdruck“ (Birch 2011); die Deutung als „Sonne und Mond“ ist eine spätere " +
                        "Volksetymologie.",
                    "Die Hatha Yoga Pradipika, die diese App oft zitiert, sammelt ältere Lehren und entstand " +
                        "etwa in der Mitte des 15. Jahrhunderts. Sie beschreibt acht *kumbhakas*, drei " +
                        "*bandhas* und ein Netz von Kanälen (*nāḍīs*), durch die *prāṇa* fließen soll: ein " +
                        "Weltbild der Lehre, keine Anatomie.",
                    "Später folgen die Śivasaṃhitā (etwa 14.–15. Jahrhundert) und die Gheranda Samhita (17. " +
                        "oder 18. Jahrhundert), aus der das Maß 16 : 64 : 32 stammt. Was heute als " +
                        "„uralt“ gilt, ist also einige Jahrhunderte alt.",
                ),
                source = "Mallinson & Singleton: Roots of Yoga (2017); Mallinson & Szántó: The Amṛtasiddhi " +
                    "(2022); Birch: The Meaning of haṭha in Early Haṭhayoga (2011); Hatha Yoga Pradipika Kap. 2–3",
                exerciseIds = listOf("nadi-shodhana", "ujjayi", "bhramari"),
                seeAlso = "Siehe Seite „Atemlehre“, Karte „Das klassische Maß 1 : 4 : 2“.",
            ),
            LearnTopic(
                id = "lehrer",
                title = "Lehrer des 20. Jahrhunderts",
                period = "1920er–1980er",
                teaser = "Krishnamacharya, Iyengar und Sivananda formten das Pranayama, das heute gelehrt wird.",
                paragraphs = listOf(
                    "In den 1930ern unterrichtete Krishnamacharya am Palast von Mysore. Er verband " +
                        "Haṭha-Techniken mit Patanjali und passte die Übung dem Schüler an. Singleton (2010) " +
                        "zeigt, wie stark die Haltungen dieser Zeit von westlicher Gymnastik und Körperkultur " +
                        "mitgeprägt waren. Vieles in Krishnamacharyas Lebenslauf ist nicht belegt.",
                    "Seine Schüler trugen das weiter: B. K. S. Iyengar (Light on Pranayama, 1981) mit Viloma " +
                        "und *sama vṛtti* und T. K. V. Desikachar mit der Viniyoga-Lehre, aus der *bṛṃhaṇa* " +
                        "und *laṅghana* der Atemlehre stammen.",
                    "Unabhängig davon lehrte Swami Sivananda in Rishikesh (Science of Pranayama, 1935); seine " +
                        "Schule brachte Yoga in den 1960ern in den Westen, aus ihrem Umfeld stammt die " +
                        "dreiteilige Atmung (Dirgha). Kuvalayananda gründete 1924 das erste Yoga-Labor.",
                ),
                source = "Singleton: Yoga Body (2010); De Michelis: A History of Modern Yoga (2004)",
                exerciseIds = listOf("viloma-ein", "viloma-aus", "dirgha", "ujjayi"),
                seeAlso = "Siehe Seite „Atemlehre“, Abschnitt „Die Grundregeln“.",
            ),
        ),
    )

    // ---- Andere Wege ----

    private val otherPaths = LearnSection(
        id = "andere-wege",
        title = "Andere Wege",
        teaser = "Buddha, Zen und Dao schauen dem Atem zu; Tummo in Tibet lenkt ihn kräftig.",
        topics = listOf(
            LearnTopic(
                id = "beobachten",
                title = "Beobachten statt lenken: Buddha, Zen, Dao und Tibet",
                teaser = "Andere Traditionen lenken den Atem nicht, sondern schauen ihm zu; Tummo ist die Ausnahme.",
                paragraphs = listOf(
                    "Der Buddha lehrt in der Rede über die Achtsamkeit auf den Atem (*ānāpānasati*, Majjhima " +
                        "Nikāya 118) sechzehn Schritte. Der erste: Ist der Atem lang, weiß man „er ist lang“; " +
                        "ist er kurz, weiß man „er ist kurz“. Gelenkt wird nichts. Im Zen zählen Anfänger oft " +
                        "die Ausatemzüge bis zehn und beginnen von vorn.",
                    "Im Daoismus heißt es bei Zhuangzi, der wahre Mensch atme „bis in die Fersen“: ein Bild " +
                        "für tiefe Ruhe, keine Anleitung. Atemübungen (*xíngqì*) gab es in China seit der " +
                        "Antike; das Sammelwort „Qigong“ entstand erst nach 1949 (Palmer 2007).",
                    "Tibetisches Tummo ist der Gegenpol: kräftiges Atmen mit Halten und Vorstellungsbildern. " +
                        "Bei geübten Mönchen stieg die Körpertemperatur messbar, bis 38,3 °C, und nur bei der " +
                        "kraftvollen Form (Kozhevnikov 2013, kleine Studie). Wim Hof bezieht sich auf diese Praxis.",
                ),
                source = "Majjhima Nikāya 118 (Anālayo: Mindfulness of Breathing, 2019); Zhuangzi Kap. 6; " +
                    "Palmer: Qigong Fever (2007)",
                exerciseIds = listOf("wim-hof"),
                seeAlso = "Dem Atem nur zuschauen kannst du in einer stillen Sitzung im Tab „Meditation“.",
            ),
        ),
    )

    // ---- Der Westen ----

    private val west = LearnSection(
        id = "westen",
        title = "Der Westen",
        teaser = "Labor, Weniger-Atmen, Zulassen und schnelles Atmen: vier Wege seit 1924.",
        topics = listOf(
            LearnTopic(
                id = "labor",
                title = "Atem im Labor: von 1924 zur Resonanz-Atmung",
                period = "1924–heute",
                teaser = "Seit 1924 wird Pranayama gemessen; aus dem Labor stammt die Zahl 6 Atemzüge pro Minute.",
                paragraphs = listOf(
                    "1924 gründete Swami Kuvalayananda in Lonavla das Kaivalyadhama und die Zeitschrift Yoga " +
                        "Mimamsa. Er röntgte Übende und maß den Druck im Bauch bei *uḍḍīyāna*: Yoga sollte sich " +
                        "mit den Mitteln der Wissenschaft prüfen lassen. 1970 ließ sich Swami Rama im " +
                        "Menninger-Institut in Kansas verkabeln; die Versuche gaben der Biofeedback-Forschung " +
                        "Schwung (Green & Green 1977). Einzelne Zahlen von damals sind nicht nachgeprüft.",
                    "2002 fanden Vaschillo, Lehrer und Kollegen, dass Puls und Blutdruck bei einem Atemtempo " +
                        "um 0,1 Hz, also etwa 6 Atemzügen pro Minute, besonders stark schwingen. Der Grund " +
                        "ist der Baroreflex, ein Regelkreis des Blutdrucks, der mit etwa 5 s Verzögerung " +
                        "antwortet; langsames Atmen trifft seinen Takt (Lehrer & Gevirtz 2014).",
                    "Daraus wurden das HRV-Biofeedback und die Resonanz-Atmung dieser App. Sie ist die Übung " +
                        "mit der besten Studienlage: Eine Auswertung von 223 Studien zeigt, dass langsames " +
                        "Atmen die Herzratenvariabilität hebt (Laborde 2022).",
                ),
                source = "Green & Green: Beyond Biofeedback (1977); Alter: Yoga in Modern India (2004)",
                exerciseIds = listOf("resonanz"),
            ),
            LearnTopic(
                id = "weniger",
                title = "Weniger atmen: Buteyko und Papworth",
                period = "1950er–2000er",
                teaser = "Weniger atmen: In Asthma-Studien besserte sich das Befinden, die Lungenfunktion nicht.",
                paragraphs = listOf(
                    "Der Arzt Konstantin Buteyko entwickelte seine Methode in den 1950ern in der Sowjetunion. " +
                        "Seine Annahme: Viele Menschen atmen dauerhaft zu viel und haben deshalb zu wenig CO₂ " +
                        "im Blut, und darin sah er die Ursache vieler Beschwerden. Also übte er reduziertes, " +
                        "ruhiges Nasenatmen mit kurzen Atempausen.",
                    "Etwa zur gleichen Zeit entstand in England die Papworth-Methode: Physiotherapeuten " +
                        "lehrten Asthma-Patienten Zwerchfell- und Nasenatmung samt Entspannung. In der ersten " +
                        "kontrollierten Studie (Holloway & West 2007, 85 Personen) berichteten sie weniger " +
                        "Beschwerden und weniger Angst; die Lungenfunktion änderte sich nicht.",
                    "Für Buteyko gilt Ähnliches: Lebensqualität besser, Lungenfunktion unklar (Santino 2020, " +
                        "Cochrane); das CO₂ stieg in einer verblindeten Studie nicht (Bowler 1998). Beide Wege " +
                        "ergänzen eine ärztliche Behandlung, sie ersetzen sie nicht.",
                ),
                source = "Holloway & West (2007); Santino et al. (2020, Cochrane)",
                exerciseIds = listOf("buteyko", "zwerchfell", "lippenbremse"),
            ),
            LearnTopic(
                id = "zulassen",
                title = "Atem zulassen: Middendorf und die Atempädagogik",
                period = "1920er–1980er",
                teaser = "Ilse Middendorf lehrte, den Atem kommen zu lassen statt ihn zu lenken: ein dritter Weg.",
                paragraphs = listOf(
                    "Neben dem Lenken der Yoga-Lehre und dem Messen des Labors steht ein dritter Weg: den " +
                        "Atem zulassen. Ilse Middendorf (1910–2009) lernte bei Cornelis Veening und gründete " +
                        "1965 in Berlin ihr Institut; ihr Buch „Der Erfahrbare Atem“ erschien 1984.",
                    "Sie unterscheidet drei Weisen zu atmen: willentlich gelenkt, unbewusst und erfahrbar, " +
                        "also zugelassen und mit Aufmerksamkeit begleitet. Geübt wird sitzend oder in " +
                        "Bewegung, oft mit Dehnungen und Vokalen, ohne Zählen.",
                    "Im gleichen Umfeld entstanden die Stimm- und Atemarbeit von Schlaffhorst-Andersen und das " +
                        "Autogene Training von Schultz (1932). Studien zur Atempädagogik gibt es kaum; ihr " +
                        "Wert liegt im Spüren, nicht in Messwerten.",
                ),
                source = "Middendorf: Der Erfahrbare Atem (1984)",
                exerciseIds = listOf("zwerchfell"),
                seeAlso = "Den Atem kommen lassen, ohne zu zählen, kannst du in einer stillen Sitzung im Tab " +
                    "„Meditation“.",
            ),
            LearnTopic(
                id = "schnell",
                title = "Schnell atmen als Erlebnis: Holotrop, Rebirthing, Wim Hof",
                period = "1970er–heute",
                teaser = "Seit den 1970ern nutzen Methoden schnelles Atmen für starke Erlebnisse; die Belege sind dünn.",
                paragraphs = listOf(
                    "Stanislav Grof entwickelte in den 1970ern das Holotrope Atmen: lange, schnelle Atmung " +
                        "mit Musik, um veränderte Bewusstseinszustände zu erreichen. Etwa gleichzeitig " +
                        "entstand Leonard Orrs Rebirthing, das mit verbundenem Atmen Geburtserinnerungen " +
                        "wecken soll; dafür gibt es keinen Beleg.",
                    "Körperlich ist beides Hyperventilation: Das CO₂ im Blut sinkt, Kribbeln, Schwindel, " +
                        "starke Gefühle und Krämpfe können folgen (Fincham 2023b). Zur Wirkung gibt es vor " +
                        "allem Hypothesen, keine kontrollierten Studien (Rhinewine & Williams 2007).",
                    "Wim Hof verband ab den 2000ern schnelles Atmen, Halten und Kälte. Eine Studie zeigte eine " +
                        "gedämpfte Entzündungsreaktion (Kox 2014, 24 Männer); eine Übersicht stuft die Belege " +
                        "insgesamt als sehr niedrig ein (Almahayni 2024). Für alle gilt: nie im oder am " +
                        "Wasser, nicht allein bei Vorerkrankung.",
                ),
                source = "Grof & Grof: Holotropic Breathwork (2010); Fincham et al. (2023b)",
                exerciseIds = listOf("wim-hof"),
                seeAlso = "Siehe Seite „Pranayama“, Karte „Langsam und schnell“.",
            ),
        ),
    )

    /** Die drei Abschnitte mit Karten; „Heute“ ([today]) folgt mit der Zeitleiste davor. */
    val sections = listOf(india, otherPaths, west)

    // ---- Heute ----

    /** Abschnitt „Heute“: oben die Zeitleiste ([timeline], als Liste, ohne Karte), darunter die Karte zum Alter des Yoga. */
    val today = LearnSection(
        id = "heute",
        title = "Heute",
        teaser = "Eine Zeitleiste der bekannten Muster und die Frage, wie alt Yoga wirklich ist.",
        topics = listOf(
            LearnTopic(
                id = "alter",
                title = "Wie alt ist Yoga wirklich, und wem gehört er?",
                teaser = "Für 5000 Jahre Yoga gibt es keinen Beleg; Pranayama als Methode ist mittelalterlich.",
                paragraphs = listOf(
                    "Ein Siegel aus der Indus-Kultur (um 2000 v. Chr.) zeigt eine sitzende Figur; ob das Yoga " +
                        "ist, lässt sich nicht sagen, Fachleute nennen die Deutung spekulativ. Die „5000 " +
                        "Jahre“ der Werbung haben keine Quelle. Atem als Lebenskraft ist etwa 2500 Jahre alt, " +
                        "Atemtechniken mit Halten und Maß etwa 1000, Haltungs-Yoga in heutiger Form knapp 100 " +
                        "(Singleton 2010; Mallinson & Singleton 2017).",
                    "Jung heißt nicht wertlos, alt heißt nicht wahr. Ob eine Übung etwas bringt, zeigen " +
                        "Erfahrung und Studien, nicht ihr Alter. Deshalb trennt die App Lehre und Studienlage.",
                    "Yoga gehört keiner Gruppe allein, meint die Religionsforscherin Andrea Jain (2015); " +
                        "Kritik an Kommerz und an erfundenen Herkunftsgeschichten ist trotzdem berechtigt. Die " +
                        "App versucht Respekt im Kleinen: Sanskrit richtig schreiben, Lehrer und Texte nennen, " +
                        "nichts behaupten, was die Texte nicht sagen.",
                ),
                source = "Singleton: Yoga Body (2010); Mallinson & Singleton: Roots of Yoga (2017); Jain: " +
                    "Selling Yoga (2015)",
                seeAlso = "Siehe „Über Atemkraft“ in den Einstellungen.",
            ),
        ),
    )

    /** Zeitleiste: nur Jahre, die sicher sind (Eigentümer 2026-10-25: schlichte Liste, keine Karte). */
    val timeline = listOf(
        HistoryEvent("1896", "Vivekananda veröffentlicht „Raja Yoga“ und bringt *prāṇa* als Weltenergie in den Westen."),
        HistoryEvent("1924", "Kuvalayananda gründet Kaivalyadhama und die Zeitschrift Yoga Mimamsa: Yoga im Labor."),
        HistoryEvent("1932", "Schultz: Autogenes Training."),
        HistoryEvent("1935", "Sivananda: Science of Pranayama."),
        HistoryEvent("1950er", "Buteyko entwickelt seine Methode in der Sowjetunion."),
        HistoryEvent("1960er", "Papworth-Methode in England."),
        HistoryEvent("1965", "Middendorf gründet ihr Institut in Berlin."),
        HistoryEvent("1970", "Swami Rama im Menninger-Institut: Anstoß für das Biofeedback."),
        HistoryEvent("1981", "Iyengar: Light on Pranayama."),
        HistoryEvent("1980er", "Sudarshan Kriya (Art of Living)."),
        HistoryEvent("1984", "Middendorf: Der Erfahrbare Atem."),
        HistoryEvent("1998", "Bowler: Buteyko erstmals verblindet geprüft."),
        HistoryEvent("2002", "Vaschillo: Resonanz um 0,1 Hz, der Grund für „6 pro Minute“."),
        HistoryEvent("2007", "Holloway & West: erste kontrollierte Studie zur Papworth-Methode."),
        HistoryEvent("2014", "Kox: Wim-Hof-Methode im Labor."),
        HistoryEvent("2016", "Li: der Schaltkreis des Seufzers im Hirnstamm (bei Mäusen)."),
        HistoryEvent("2023", "Balban: Cyclic Sighing in einer kontrollierten Studie."),
    )

    /** Nachsatz unter der Zeitleiste: Muster ohne sicheres Jahr. */
    val timelineNote = "Ohne sicheres Jahr, deshalb nicht in der Leiste: 4-7-8 (Andrew Weil, 1990er oder " +
        "2000er), Box-Atmung (Mark Divine, 2010er; das Muster ist *sama vṛtti*)."

    // ---- Quellen ----

    val sourcesTeaser = "Bücher und Aufsätze zur Geschichte, Studien für die Einordnung."

    /** Bücher und Aufsätze zur Geschichte (ohne DOI). */
    val textSources = listOf(
        "Olivelle P.: The Early Upaniṣads. Annotated Text and Translation. Oxford University Press (1998)",
        "Maas P. A.: A Concise Historiography of Classical Yoga Philosophy. In: Franco E. (Hg.), " +
            "Periodization and Historiography of Indian Philosophy. Wien (2013)",
        "Mallinson J. & Singleton M.: Roots of Yoga. Penguin (2017)",
        "Mallinson J. & Szántó P.-D.: The Amṛtasiddhi and Amṛtasiddhimūla. EFEO/IFP (2022)",
        "Birch J.: The Meaning of haṭha in Early Haṭhayoga. Journal of the American Oriental Society " +
            "131(4):527–554 (2011)",
        "Singleton M.: Yoga Body. The Origins of Modern Posture Practice. Oxford University Press (2010)",
        "De Michelis E.: A History of Modern Yoga. Continuum (2004)",
        "Alter J. S.: Yoga in Modern India. The Body between Science and Philosophy. Princeton University Press (2004)",
        "Jain A. R.: Selling Yoga. From Counterculture to Pop Culture. Oxford University Press (2015)",
        "Anālayo: Mindfulness of Breathing. A Practice Guide and Translations. Windhorse (2019)",
        "Palmer D. A.: Qigong Fever. Body, Science, and Utopia in China. Columbia University Press (2007)",
        "Green E. & Green A.: Beyond Biofeedback. Delacorte (1977)",
        "Middendorf I.: Der Erfahrbare Atem. Eine Atemlehre. Junfermann (1984)",
        "Grof S. & Grof C.: Holotropic Breathwork. A New Approach to Self-Exploration and Therapy. SUNY Press (2010)",
    )

    /** Studien, die die Karten und die Zeitleiste nennen. */
    val studyRefs: List<Reference> = listOf(
        Refs.kozhevnikov2013,
        Refs.vaschillo2002,
        Refs.lehrer2014,
        Refs.laborde2022,
        Refs.holloway2007,
        Refs.buteykoCochrane2020,
        Refs.bowler1998,
        Refs.rhinewine2007,
        Refs.fincham2023b,
        Refs.kox2014,
        Refs.whmReview2024,
        Refs.li2016,
        Refs.balban2023,
    )
}
