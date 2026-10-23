package app.atemkraft.data

import app.atemkraft.domain.EvidenceLevel
import app.atemkraft.domain.PranayamaTechnique
import app.atemkraft.domain.PranayamaTopic
import app.atemkraft.domain.Reference

/**
 * Inhalte der Seite „Pranayama“ im Tab „Lernen“: Grundlagen der Yoga-Lehre und die Techniken mit
 * Anleitung (Nutzer 2026-10-23: Theorie mit Praxis verbinden). Das ist Lehre, keine Studienlage;
 * was Studien sagen, steht je Technik unter [PranayamaTechnique.studies] mit Quelle in [studyRefs].
 * Die Detailseite einer Übung bleibt die ausführliche Quelle, diese Seite ordnet ein.
 *
 * Grundlage: Patanjalis Yoga-Sutra, Hatha Yoga Pradipika, Gheranda Samhita, Krishnamacharya/
 * Desikachar, Iyengar, Kuvalayananda; Verse deutsch zitiert („2,51“). Die Phasen selbst erklärt
 * die Seite „Atemlehre“ ([Atemlehre.phases]); hier steht nur der Verweis.
 *
 * Sanskrit klein in IAST zwischen Sternchen (TEXT-14), Namen von Techniken, Personen und
 * Texttitel aufrecht; die Wortbedeutung („*viloma* heißt …“) gilt dem Wort, also kursiv.
 */
object Pranayama {

    val intro = "Hier treffen Yoga-Lehre und Übung zusammen: was *prāṇāyāma* ist und wie du die " +
        "Techniken übst. Das ist Lehre, keine Studienlage; was Studien sagen, steht jeweils unter „Studien“."

    // ---- Grundlagen ----

    val topicsTeaser = "Was *prāṇāyāma* ist, wie du beginnst und worauf du achtest."

    val topics = listOf(
        PranayamaTopic(
            id = "was",
            title = "Was Pranayama ist",
            teaser = "*prāṇāyāma* heißt in der Yoga-Lehre, den Atem bewusst zu lenken und zu weiten.",
            paragraphs = listOf(
                "Das Wort verbindet *prāṇa*, Atem und Lebenskraft, mit *āyāma*, Ausdehnung. Gemeint ist " +
                    "ein Atem, den du bewusst lenkst, verlangsamst und manchmal anhältst.",
                "In Patanjalis Yoga-Sutra ist *prāṇāyāma* das vierte von acht Gliedern, nach den " +
                    "Haltungen (*āsana*). Patanjali beschreibt es als Unterbrechen des gewohnten Ein und " +
                    "Aus; nach Ort, Zeit und Zahl geregelt, wird der Atem „lang und fein“ (2,49–2,50).",
                "Jeder Atemzug hat seine Phasen: Einatmen (*pūraka*), Halten (*kumbhaka*) und Ausatmen " +
                    "(*recaka*). Wie die Lehre sie deutet, steht auf der Seite „Atemlehre“.",
            ),
            source = "Patanjali: Yoga-Sutra 2,29; 2,49–2,50",
        ),
        PranayamaTopic(
            id = "haltung",
            title = "Haltung und Vorbereitung",
            teaser = "Übe aufrecht sitzend oder liegend, mit leerem Magen, freier Nase und in Ruhe.",
            paragraphs = listOf(
                "Sitz aufrecht auf einem Stuhl oder Kissen, Schultern und Gesicht weich. Liegen geht " +
                    "auch und ist für den Anfang oft leichter; Iyengar lehrt viele Übungen zuerst im Liegen.",
                "Die Lehre rät, mit leerem Magen zu üben, also nicht direkt nach dem Essen. Die Nase " +
                    "sollte frei sein; ist sie verstopft, lass Übungen durch ein Nasenloch aus. Ein ruhiger " +
                    "Ort, an dem dich niemand stört, hilft beim Dranbleiben.",
                "Geatmet wird in der Regel durch die Nase. Ausnahmen sind Übungen wie Sitali, bei denen " +
                    "der Mund ausdrücklich mitspielt.",
            ),
            source = "B. K. S. Iyengar: Light on Pranayama",
        ),
        PranayamaTopic(
            id = "aufbau",
            title = "Schrittweise aufbauen",
            teaser = "*krama* heißt Schritt für Schritt: erst ohne Halten, dann mit, nie mit Zwang.",
            paragraphs = listOf(
                "„Wie man Löwe, Elefant und Tiger nur langsam zähmt, so auch den Atem“, heißt es in der " +
                    "Hatha Yoga Pradipika (2,15). Der Text warnt selbst: Falsch geübt, bringe die " +
                    "Atemübung Beschwerden wie Husten oder Kopfschmerz (2,16–2,17).",
                "Eine übliche Reihenfolge: Zuerst lernst du langsames Atmen ohne Halten. Dann kommt " +
                    "kurzes Halten nach dem Einatmen dazu (*antara kumbhaka*), viel später Halten nach dem " +
                    "Ausatmen (*bāhya kumbhaka*). Als Faustregel für den Anfang: Halte höchstens so lange, " +
                    "wie du einatmest.",
                "Prüfstein aus der Lehre von Krishnamacharya und Desikachar: Das Ausatmen nach dem Halten " +
                    "bleibt ruhig und gleichmäßig. Wird es hastig oder rau, war das Halten zu lang.",
            ),
            source = "Hatha Yoga Pradipika 2,15–2,17; T. K. V. Desikachar: The Heart of Yoga",
        ),
        PranayamaTopic(
            id = "verhaeltnisse",
            title = "Verhältnisse",
            teaser = "Gleich lange Phasen heißen *sama vṛtti*, ungleiche *viṣama vṛtti*.",
            paragraphs = listOf(
                "Iyengar unterscheidet *sama vṛtti*, den Atem in gleich langen Phasen, von *viṣama " +
                    "vṛtti*, dem Atem in ungleichen Verhältnissen. Das eine gilt als ordnend, das andere " +
                    "verschiebt den Schwerpunkt auf Aufnehmen oder Loslassen.",
                "Das bekannteste ungleiche Maß ist 1\u00A0:\u00A04\u00A0:\u00A02: einatmen, viermal so lange halten, doppelt " +
                    "so lange ausatmen. " + Atemlehre.CLASSIC_RATIO_ORIGIN + " Es ist ein Fernziel nach langer Übung, kein Startwert.",
                "Ob das Verhältnis zählt, ist in Studien uneinheitlich. Längeres Ausatmen fühlte sich in " +
                    "einer Studie entspannender an und hob die Herzratenvariabilität (Van Diest 2014, " +
                    "n\u00A0=\u00A030; ähnlich Bae 2021). Andere fanden bei langsamem Atmen keinen Unterschied " +
                    "zwischen gleich langen und ungleichen Phasen (Birdee 2023, Meehan & Shaffer 2024). " +
                    "Klarer ist: Das langsame Tempo zählt mehr.",
            ),
            source = "B. K. S. Iyengar: Light on Pranayama; Gheranda Samhita, Kap. 5 (Versangaben je nach Ausgabe)",
        ),
        PranayamaTopic(
            id = "tempo",
            title = "Langsam und schnell",
            teaser = "Langsames Atmen ist gut untersucht; schnelle Techniken tragen ein eigenes Risiko.",
            paragraphs = listOf(
                "Was Studien vor allem stützen, ist langsames Atmen allgemein, etwa 6 Atemzüge pro " +
                    "Minute: Die Herzratenvariabilität steigt, viele fühlen sich ruhiger (Zaccaro 2018, " +
                    "Laborde 2022). Über viele Atemformen hinweg berichteten Übende etwas weniger Stress, " +
                    "ein kleiner bis mittlerer Effekt (Fincham 2023a, Meta-Analyse). Das Tempo scheint " +
                    "wichtiger als die einzelne Technik.",
                "Kapalabhati, schnelle Bhastrika und die Wim-Hof-Atmung sind eine eigene Klasse: Sie " +
                    "atmen mehr ab, als der Körper braucht. Das senkt das CO₂ im Blut, aktiviert und kann " +
                    "Schwindel, Kribbeln und starke Gefühle auslösen (Fincham 2023b, Übersicht zu schnellem " +
                    "Atmen). In Studien bei psychischen Erkrankungen traten Nebenwirkungen häufiger bei " +
                    "schnellen als bei langsamen Techniken auf (Mütze 2025).",
                "Schnelle Techniken gehören nie ins oder ans Wasser, nicht ans Steuer und nicht in Zeiten, " +
                    "in denen du dich krank oder erschöpft fühlst.",
            ),
        ),
        PranayamaTopic(
            id = "stille",
            title = "Übergang in die Stille",
            teaser = "Nach der Lehre bereitet *prāṇāyāma* den Geist auf die Sammlung vor, auf *dhāraṇā*.",
            paragraphs = listOf(
                "Patanjali schreibt der Übung zwei Folgen zu: Der Schleier über dem inneren Licht wird " +
                    "dünner, und der Geist wird fähig zur Sammlung, *dhāraṇā* (Yoga-Sutra 2,52–2,53). " +
                    "*prāṇāyāma* gilt damit als Brücke zwischen Körper und Meditation.",
                "In der App kannst du das nachbilden: Mit „Danach in die Stille“ (Einstellungen › " +
                    "Atemübung, oder oben in der laufenden Übung) geht die Atemübung nahtlos in eine " +
                    "stille Meditation über.",
                "Die Hatha-Lehre kennt auch *kevala kumbhaka*, eine Atemruhe, die von selbst entsteht, " +
                    "wenn der Geist still wird. Sie ist ein Bild der Lehre, kein Ziel zum Erzwingen: Lass " +
                    "den Atem in der Stille einfach kommen und gehen.",
            ),
            source = "Patanjali: Yoga-Sutra 2,52–2,53",
        ),
        PranayamaTopic(
            id = "sicherheit",
            title = "Sicherheit",
            teaser = "Nie im oder am Wasser, nie beim Fahren; bei Schwindel aufhören und normal weiteratmen.",
            paragraphs = listOf(
                "Atemübungen gehören nie ins oder ans Wasser und nie ans Steuer. Wer vor dem Tauchen " +
                    "schnell atmet und dann die Luft anhält, kann ohne Vorwarnung bewusstlos werden; eine " +
                    "Auswertung aus New York fand 16 solche Unfälle, darunter tödliche (Boyd 2015).",
                "Bei Schwindel, Kribbeln, Druck oder Atemnot hörst du auf und atmest normal weiter. Nie " +
                    "pressen: Der Atem bleibt weich, auch beim Halten.",
                "Bei Schwangerschaft, Bluthochdruck, Herz-, Lungen- oder Augenerkrankungen wie Glaukom " +
                    "(grüner Star), bei Epilepsie und bei Panik-, Angst- oder anderen psychischen " +
                    "Erkrankungen gilt: ohne Anleitung keine langen Halte, keine schnellen Techniken und " +
                    "keine Bandhas. Schnelles Atmen kann Panikgefühle auslösen. Langsames Atmen ohne " +
                    "Halten gilt als risikoarm; im Zweifel ärztlich abklären.",
            ),
        ),
    )

    // ---- Techniken ----

    val techniquesTeaser = "Sieben Techniken zum Üben, drei zum Verstehen: je mit Lehre, Anleitung und Studien."

    val techniques = listOf(
        PranayamaTechnique(
            id = "ujjayi",
            name = "Ujjayi (Siegesatem)",
            sanskrit = "*ujjāyī*",
            pronunciation = "udsch-dschá-jii",
            teaser = "Bei Ujjayi gilt das leise Rauschen der Kehle in der Yoga-Lehre als wärmend und sammelnd.",
            tradition = "*ujjāyī* heißt „die Siegreiche“, von *ud*, hinauf, und *ji*, siegen. Die Hatha Yoga " +
                "Pradipika beschreibt, wie der Atem mit geschlossenem Mund langsam eingezogen wird, hörbar " +
                "von der Kehle bis zum Herzen; dann folgen Halten und Ausatmen durch das linke Nasenloch, und " +
                "die Übung fache das Körperfeuer an (2,51–2,52). Üben lasse es sich auch im Gehen und Stehen " +
                "(2,53). Bei Krishnamacharya und seinem Sohn Desikachar ist Ujjayi die Grundtechnik fast jeder " +
                "Atemübung, auch während der Haltungen. Auch Iyengar baut seine Atemlehre darauf auf. Nach der " +
                "Atemlehre gilt das Rauschen der Kehle als wärmend und sammelnd, das längere Ausatmen als *laṅghana*.",
            howTo = "Schließe den Mund und atme durch die Nase. Verenge die Kehle ein wenig, wie wenn du " +
                "ein „haaa“ flüsterst oder einen Spiegel anhauchst, nur mit geschlossenem Mund. So entsteht " +
                "beim Ein- und Ausatmen ein leises, gleichmäßiges Rauschen, das nur du hörst. Gesicht und " +
                "Kiefer bleiben weich. Atme langsam, etwa 4\u00A0s ein und 6\u00A0s aus. Ujjayi lässt sich mit jedem " +
                "langsamen Muster der App verbinden. Fällt es schwer, übe das Rauschen zuerst nur beim Ausatmen.",
            studies = "Zu Ujjayi gibt es wenige, kleine Studien. Bei 17 Yoga-Anfängern hob langsames Atmen " +
                "die Empfindlichkeit des Baroreflexes, eines Regelkreises des Blutdrucks; mit Rauschen nur beim Ausatmen " +
                "stieg sie etwas weniger, mit Rauschen beim Ein- und Ausatmen nicht deutlich (Mason 2013). Bei 12 Menschen " +
                "mit Querschnittlähmung hob Ujjayi die Sauerstoffsättigung leicht (Mazur 2024, explorativ). " +
                "Den Nutzen trägt vermutlich vor allem das langsame Tempo; das Rauschen hilft, den Atem " +
                "gleichmäßig zu halten.",
            safety = "Nicht pressen, kein Valsalva-Pressen: Die Kehle ist nur leicht verengt, der Atem " +
                "fließt immer. Bei Schwindel, Druck im Kopf oder Atemnot aufhören und normal weiteratmen.",
            source = "Hatha Yoga Pradipika 2,51–2,53",
            exerciseId = "ujjayi",
        ),
        PranayamaTechnique(
            id = "dirgha",
            name = "Dirgha (dreiteilige Atmung)",
            sanskrit = "*dīrgha*",
            pronunciation = "díir-gha",
            teaser = "Bei Dirgha füllt der Atem nacheinander Bauch, Flanken und Brust, ganz ohne Pausen.",
            tradition = "*dīrgha* heißt „lang“. Die dreiteilige Atmung ist moderne Lehre, verbreitet etwa " +
                "durch die Sivananda-Schule und das Integral Yoga; eine klassische Textstelle gibt es dafür " +
                "nicht. Dort gilt sie als Grundübung, mit der du den ganzen Atemraum kennenlernst, bevor " +
                "andere Techniken folgen.",
            howTo = "Lege eine Hand auf den Bauch, eine auf die Brust. Atme ein und lass zuerst den Bauch " +
                "sich heben, dann die Flanken weiten, zuletzt die Brust. Die drei Teile fließen ohne Pausen " +
                "ineinander. Beim Ausatmen umgekehrt: Brust, Flanken, Bauch. Bleib bequem, nicht bis zum " +
                "Anschlag. In der App etwa 6\u00A0s ein und 6\u00A0s aus.",
            studies = "Eigene Studien zur dreiteiligen Atmung gibt es nicht. Einordnen lässt sie sich nur " +
                "indirekt: Sie ist langsames Atmen, und das hebt in vielen Studien die " +
                "Herzratenvariabilität (Zaccaro 2018). Die drei Teile sind ein Bild für die Aufmerksamkeit; " +
                "die Lunge füllt sich als Ganzes.",
            safety = "Nicht zu tief atmen, sonst kommt leicht Schwindel. Bei schwerer COPD mit überblähter " +
                "Lunge sanft dosieren.",
            source = "Moderne Lehre: Sivananda-Schule, Integral Yoga",
            exerciseId = "dirgha",
        ),
        PranayamaTechnique(
            id = "nadi-shodhana",
            name = "Nadi Shodhana (Wechselatmung)",
            sanskrit = "*nāḍī śodhana*",
            pronunciation = "náa-di schóo-dha-na",
            teaser = "Die Wechselatmung gilt in der Yoga-Lehre als Ausgleich von Sonne (rechts) und Mond (links).",
            tradition = "*nāḍī śodhana* heißt „Reinigung der Kanäle“. Nach der Lehre fließt *prāṇa* durch " +
                "*iḍā* links, Mond und kühlend, und *piṅgalā* rechts, Sonne und wärmend. Die Hatha Yoga " +
                "Pradipika übt den Wechsel mit langem Halten (2,7–2,10); spätere Lehrer zählen oft im Maß " +
                "1\u00A0:\u00A04\u00A0:\u00A02.",
            howTo = "Mit der rechten Hand: Der Daumen schließt das rechte, der Ringfinger das linke " +
                "Nasenloch. Links ein, rechts aus, rechts ein, links aus; das ist eine Runde. Langsam und " +
                "ohne Halten, in der App 5\u00A0s ein und 6\u00A0s aus. Die App gibt Takt und Seitenwechsel vor.",
            studies = "Eine Meta-Analyse fand einen niedrigeren Blutdruck, den oberen Wert um etwa 7\u00A0mmHg, " +
                "bei großer Streuung und kaum verblindeten Studien (Nam 2024, 6 Studien, n\u00A0=\u00A0525). " +
                "Wahrscheinlich trägt das langsame Atmen den Effekt, nicht der Seitenwechsel. Dass einzelne " +
                "Nasenlöcher gezielt Nervensystem oder Hirnhälften steuern, ist nicht belegt.",
            safety = "Bei verstopfter Nase auslassen. Nicht pressen und nichts erzwingen.",
            source = "Hatha Yoga Pradipika 2,7–2,10",
            exerciseId = "nadi-shodhana",
        ),
        PranayamaTechnique(
            id = "viloma",
            name = "Viloma (Atmen in Stufen)",
            sanskrit = "*viloma*",
            pronunciation = "wi-lóo-ma",
            teaser = "Viloma teilt einen Atemzug in Stufen mit kurzen Pausen und gilt als sanfter Einstieg.",
            tradition = "*viloma* heißt „gegen den Strich“. Iyengar beschreibt zwei Stufen: Bei Viloma I " +
                "wird das Einatmen in Abschnitte mit kurzen Pausen geteilt, bei Viloma II das Ausatmen. " +
                "Stufen beim Einatmen gelten als aufbauend (*bṛṃhaṇa*), Stufen beim Ausatmen als lösend " +
                "(*laṅghana*). Die Hatha Yoga Pradipika kennt Viloma nicht.",
            howTo = "Anfangs im Liegen. Viloma I: 2\u00A0s ein, 2\u00A0s Pause, 2\u00A0s ein, 2\u00A0s Pause, 2\u00A0s ein bis " +
                "ganz voll, dann 8\u00A0s in einem Zug aus. In den Pausen entweicht keine Luft, die Kehle bleibt " +
                "weich. Viloma II geht umgekehrt; beide stehen als eigene Übungen in der App.",
            studies = "Zu Viloma selbst gibt es keine Studien; die Studien zu „Anuloma Viloma“ betreffen " +
                "die Wechselatmung. Viloma ist sehr langsames Atmen, gut drei Atemzüge pro Minute, und " +
                "langsames Atmen ist allgemein gut untersucht (Zaccaro 2018). Ob die Pausen etwas " +
                "beitragen, ist offen.",
            safety = "Nie pressen. Bei Schwindel, Druck oder Atemnot normal weiteratmen. Bei Herz- oder " +
                "Lungenerkrankung und in der Schwangerschaft vorher ärztlich abklären.",
            source = "B. K. S. Iyengar: Light on Pranayama, Kap. 20",
            exerciseId = "viloma-ein",
            moreExerciseIds = listOf("viloma-aus"),
        ),
        PranayamaTechnique(
            id = "bhramari",
            name = "Bhramari (Summen)",
            sanskrit = "*bhrāmarī*",
            pronunciation = "bráa-ma-rii",
            teaser = "Bhramari, der Bienenatem, gilt in der Yoga-Lehre als beruhigend und nach innen lenkend.",
            tradition = "*bhrāmarī* heißt „Biene“. Die Hatha Yoga Pradipika vergleicht das Einatmen mit " +
                "dem Brummen der männlichen, das langsame Ausatmen mit dem Summen der weiblichen Biene " +
                "(2,68). Der Klang soll die Aufmerksamkeit nach innen ziehen. Nach der Atemlehre ist die " +
                "Übung *laṅghana*, weil das Ausatmen viel länger dauert.",
            howTo = "Durch die Nase einatmen, dann mit geschlossenem Mund lang und summend ausatmen, wie " +
                "ein leises „mmm“. Kiefer locker, die Zähne berühren sich nicht. Manche verschließen dabei " +
                "die Ohren mit den Daumen. In der App 5\u00A0s ein und 10\u00A0s summend aus.",
            studies = "In kleinen, unverblindeten Studien war der Blutdruck direkt danach niedriger " +
                "(Pramanik 2010). Summen erhöht das Stickstoffmonoxid in der Nase etwa 15-fach (Weitzberg " +
                "2002); das ist ein örtlicher Effekt, ein Nutzen für den ganzen Körper ist nicht belegt. " +
                "Bei Geübten hob Bhramari in einer Studie die Stimmung (Chetry 2026).",
            safety = "Leise summen, nicht pressen. Bei Schwindel aufhören und normal weiteratmen.",
            source = "Hatha Yoga Pradipika 2,68",
            exerciseId = "bhramari",
        ),
        PranayamaTechnique(
            id = "sitali",
            name = "Sitali und Sitkari (Kühlatmung)",
            sanskrit = "*śītalī*, *sītkārī*",
            pronunciation = "schíi-ta-lii, ssiit-káa-rii",
            teaser = "Sitali und Sitkari gelten in der Yoga-Lehre als kühlend: Die Luft strömt über Zunge oder Zähne.",
            tradition = "*śītalī* heißt „die Kühlende“. Die Hatha Yoga Pradipika beschreibt, wie der Atem " +
                "über die Zunge eingesogen, angehalten und durch die Nase ausgeatmet wird (2,57–2,58); " +
                "die Zunge zu rollen ist heute die übliche Ausführung. Bei Sitkari zieht man die Luft zischend durch den Mund ein (2,54–2,56). " +
                "Ayurveda rät zu beiden bei Hitze und Reizbarkeit (*pitta*).",
            howTo = "Sitali: Die Zunge zu einem Röhrchen rollen, langsam darüber einatmen, den Mund " +
                "schließen und durch die Nase ausatmen. Sitkari, wenn sich die Zunge nicht rollen lässt: " +
                "durch die locker aufeinanderliegenden Zähne einatmen, die Lippen leicht geöffnet. In der " +
                "App 4\u00A0s ein und 6\u00A0s aus, ohne Halten.",
            studies = "Eine kleine, unverblindete Studie fand bei Bluthochdruck niedrigere Werte (Shetty 2017); " +
                "verglichen wurde mit einer Warteliste, nicht mit einfachem langsamem Atmen. Wörtlich kühlt die Übung nicht: " +
                "Körpertemperatur und Energieumsatz stiegen in einer Messung sogar leicht (Telles 2020). " +
                "Kühl fühlt sich vor allem die einströmende Luft an.",
            safety = "Eher nicht bei Kälte, Atemwegsinfekt, Asthma oder niedrigem Blutdruck.",
            source = "Hatha Yoga Pradipika 2,54–2,58",
            exerciseId = "sitali",
        ),
        PranayamaTechnique(
            id = "kapalabhati",
            name = "Feueratmung (Kapalabhati)",
            sanskrit = "*kapālabhāti*",
            pronunciation = "ka-paa-la-bháa-ti",
            teaser = "Kapalabhati gilt in der Hatha-Lehre als Reinigung; als schnelle Technik braucht es Vorsicht.",
            tradition = "*kapālabhāti* heißt „leuchtender Schädel“. Die Hatha Yoga Pradipika zählt es zu " +
                "den sechs Reinigungen (*ṣaṭkarma*), nicht zu den *prāṇāyāmas*, und vergleicht es mit dem " +
                "Blasebalg eines Schmieds (2,35). Nach der Atemlehre ist schnelles Atmen Feuer: Es gilt als " +
                "stark wärmend und belebend.",
            howTo = "Im Sitzen, auf leeren Magen: etwa ein kräftiger Ausatemstoß pro Sekunde durch die " +
                "Nase, der Bauch zieht dabei kurz nach innen; das Einatmen geschieht von selbst. In der App " +
                "30 Stöße, dann 45\u00A0s locker atmen, drei Runden.",
            studies = "Kaum untersucht. Die Studien sind klein, oft unkontrolliert und weichen in Tempo und " +
                "Ausführung häufig vom Text ab (Chetry 2022). Dass schnelles Atmen aktiviert, ist " +
                "plausibel; eine Reinigung oder Entgiftung im wörtlichen Sinn ist nicht belegt.",
            safety = "Nie im oder am Wasser, nie beim Fahren, nur im Sitzen. Nicht bei Schwangerschaft, " +
                "unkontrolliertem Bluthochdruck, Herzerkrankung, Epilepsie, Glaukom (grüner Star), " +
                "Hernie (Eingeweidebruch) oder Panikstörung. Bei Schwindel sofort aufhören; nach " +
                "kräftiger, langer Ausführung ist sehr selten ein Lungenkollaps (Pneumothorax) " +
                "beschrieben (Johnson 2004).",
            source = "Hatha Yoga Pradipika 2,35",
            exerciseId = "feueratmung",
        ),
        PranayamaTechnique(
            id = "surya-chandra",
            name = "Surya und Chandra Bhedana",
            sanskrit = "*sūrya bhedana*, *candra bhedana*",
            pronunciation = "súur-ja bhée-da-na, tschán-dra bhée-da-na",
            teaser = "Einatmen rechts gilt als Sonne, links als Mond; Studien finden keinen Unterschied.",
            tradition = "*sūrya bhedana* heißt „Durchstoßen der Sonne“: rechts einatmen, halten, links " +
                "ausatmen. Die Hatha Yoga Pradipika zählt es zu ihren acht *kumbhakas*: Es reinige den Schädel " +
                "und vertreibe *vāta*-Störungen und Parasiten (2,48–2,50). Als wärmend gilt es erst in späterer " +
                "Lehre. Das Gegenstück *candra bhedana*, links ein und rechts aus, stammt ebenfalls aus " +
                "späterer Lehre, etwa bei Iyengar; es gilt als kühlend und beruhigend.",
            howTo = "Mit der Hand wie bei der Wechselatmung: Für Surya Bhedana immer rechts ein und links " +
                "aus, für Chandra Bhedana umgekehrt. Langsam und anfangs ohne Halten. In der App gibt es " +
                "dafür keine eigene Übung.",
            studies = "Zwei Studien mit 47 geübten Männern verglichen beide Formen direkt: " +
                "Herzratenvariabilität und Sauerstoffverbrauch änderten sich rechts und links gleich, der " +
                "Atem wurde langsamer (Telles 2024, Singh 2024). Dass die Sonnenseite aktiviert und die " +
                "Mondseite beruhigt, bestätigen sie nicht. Beide Studien stammen aus derselben " +
                "Forschungsgruppe und schlossen nur Männer ein.",
            safety = "Bei verstopfter Nase auslassen. Lange Halte nur mit Anleitung.",
            source = "Hatha Yoga Pradipika 2,48–2,50",
        ),
        PranayamaTechnique(
            id = "bhastrika",
            name = "Bhastrika (Blasebalg)",
            sanskrit = "*bhastrikā*",
            pronunciation = "bhás-tri-kaa",
            teaser = "Bhastrika gilt in der Yoga-Lehre als Feuer; in der schnellen Form ist es Hyperventilation.",
            tradition = "*bhastrikā* heißt „Blasebalg“. Die Hatha Yoga Pradipika beschreibt kräftiges, " +
                "schnelles Ein- und Ausatmen wie beim Blasebalg eines Schmieds, gefolgt von einem langen " +
                "Halten (2,59–2,67). Nach der Lehre facht es *agni* an, das innere Feuer am Nabel.",
            howTo = "In der App gibt es Bhastrika bewusst nicht. Wer es lernen will, lernt es mit Lehrer, " +
                "im Sitzen, anfangs langsam, mit wenigen Atemzügen und ohne langes Halten.",
            studies = "Was Studien „Bhastrika“ nennen, weicht oft vom Text ab, vor allem in Tempo und " +
                "Atemtiefe (Chetry 2022); die Ergebnisse lassen sich daher schwer vergleichen. In einer " +
                "Studie hob ein tiefes, kaum verlangsamtes Bhastrika die Herzratenvariabilität, schnelles " +
                "Atmen senkte sie (Chetry 2026, Geübte, nur Sofortwirkung). Die schnelle Form senkt das " +
                "CO₂ im Blut wie die Wim-Hof-Atmung.",
            safety = "Wie bei Kapalabhati: nie im oder am Wasser, nie beim Fahren, nur im Sitzen. Nicht bei " +
                "Schwangerschaft, Bluthochdruck, Herzerkrankung, Epilepsie, Glaukom (grüner Star) oder " +
                "Panikstörung. Schwindel und Kribbeln sind das Zeichen, aufzuhören.",
            source = "Hatha Yoga Pradipika 2,59–2,67",
        ),
        PranayamaTechnique(
            id = "bandhas",
            name = "Bandhas (Verschlüsse)",
            sanskrit = "*jālandhara*, *uḍḍīyāna*, *mūla bandha*",
            pronunciation = "bán-dha",
            teaser = "Bandhas sind Verschlüsse an Kehle, Bauch und Beckenboden; die App übt sie bewusst nicht.",
            tradition = "*bandha* heißt „Verschluss“. Die Hatha Yoga Pradipika nennt drei: den Kinnverschluss " +
                "*jālandhara bandha* am Ende des Einatmens, den Bauchverschluss *uḍḍīyāna bandha* am Ende " +
                "des Haltens und den Beckenbodenverschluss *mūla bandha* (2,45–2,46). Nach der Lehre halten " +
                "sie *prāṇa* im Körper und lenken ihn; ausführlich beschreibt sie das dritte Kapitel.",
            howTo = "Keine Übung in der App, mit Absicht: Bandhas lernt man nur mit Lehrer, der die " +
                "Ausführung sieht. Falsch geübt, führen sie leicht zum Pressen.",
            studies = "Kaum untersucht. Ein Verschluss der Kehle bei vollem Atem kann dem Pressen " +
                "(Valsalva) ähneln, das den Blutdruck schwanken lässt.",
            safety = "Nicht in der Schwangerschaft, bei Bluthochdruck, Herz- oder Augenerkrankungen wie " +
                "Glaukom (grüner Star), bei Hernie (Eingeweidebruch) oder nach Bauchoperationen. Immer auf leeren Magen und nur mit Anleitung.",
            source = "Hatha Yoga Pradipika 2,45–2,46; Kap. 3",
        ),
    )

    /**
     * Ids der Übungen, die eine Technik dieser Seite üben ([PranayamaTechnique.exerciseId] und
     * [PranayamaTechnique.moreExerciseIds]): Grundlage des Filters „Aus dem Yoga“ im Atmen-Tab
     * (`ExerciseFilter.YOGA`, Nutzer 2026-10-23).
     */
    val exerciseIds: Set<String> by lazy {
        techniques.flatMap { listOfNotNull(it.exerciseId) + it.moreExerciseIds }.toSet()
    }

    /**
     * Studienlage einer Technik: bei einer Technik mit Übung die Stufe der Übung (eine Quelle für
     * Seite und Detailseite, TEXT-07), sonst [PranayamaTechnique.evidence].
     */
    fun evidenceOf(technique: PranayamaTechnique): EvidenceLevel? = technique.exerciseId
        ?.let { id -> BuiltInExercises.all.firstOrNull { it.id == id }?.evidence }
        ?: technique.evidence

    // ---- Quellen ----

    val sourcesTeaser = "Klassische Texte für die Lehre, Studien für die Einordnung."

    /** Klassische Texte und Lehrbücher, Verse deutsch zitiert. */
    val textSources = listOf(
        "Patanjali: Yoga-Sutra 2,29; 2,49–2,53",
        "Svatmarama: Hatha Yoga Pradipika 2,7–2,10; 2,15–2,17; 2,35; 2,44–2,68; Kap. 3",
        "Gheranda Samhita, Kap. 5",
        "T. K. V. Desikachar: The Heart of Yoga (1995)",
        "B. K. S. Iyengar: Light on Pranayama (1981)",
        "Swami Kuvalayananda: Pranayama (1966)",
    )

    /** Studien, auf die sich die „Studien“-Texte der Seite stützen. */
    val studyRefs: List<Reference> = listOf(
        Refs.zaccaro2018,
        Refs.laborde2022,
        Refs.fincham2023a,
        Refs.fincham2023b,
        Refs.muetze2025,
        Refs.vanDiest2014,
        Refs.bae2021,
        Refs.birdee2023,
        Refs.meehan2024,
        Refs.boyd2015,
        Refs.mason2013,
        Refs.mazur2024,
        Refs.nam2024,
        Refs.pramanik2010,
        Refs.weitzberg2002,
        Refs.chetry2026,
        Refs.shetty2017,
        Refs.telles2020,
        Refs.chetry2022,
        Refs.kapalabhatiCase2004,
        Refs.telles2024,
        Refs.singh2024,
    )
}
