package app.atemkraft.data

import app.atemkraft.domain.BreathSide
import app.atemkraft.domain.BreathingFamily
import app.atemkraft.domain.EvidenceTag
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.Phase
import app.atemkraft.domain.PhaseDuration
import app.atemkraft.domain.PhaseType
import app.atemkraft.domain.Segment

/**
 * Die Atemtechniken (Basis: Vorlage F3_Atmung, ergänzt um Sitali und Atmen im Gehen) –
 * Protokolle und Quellen sind durch Recherche bestätigt bzw. korrigiert. Reihenfolge
 * folgt den vier Familien.
 *
 * Getaktete Techniken (guided = true) haben einen Atemkreis-Ablauf; Programme/
 * Gewohnheiten (SKY, Buteyko, Nasenatmung) sind Infoseiten ohne festen Takt.
 */
object BuiltInExercises {

    private fun secs(value: Double): PhaseDuration = PhaseDuration.Fixed((value * 1000).toLong())

    // ── A · Herunterregeln (vagal) ───────────────────────────────────────────

    private val resonance = Exercise(
        id = "resonanz",
        name = "Resonanz-Atmung",
        family = BreathingFamily.DOWNREGULATE,
        tag = EvidenceTag.BEST_EVIDENCE,
        shortDescription = "Gleichmäßig ~6 Atemzüge/Minute: 4 s ein, 6 s aus, ohne Halten. " +
            "Bei dieser Rate geraten Herzschlag und Blutdruck in Resonanz – HRV und " +
            "Baroreflex werden maximal.",
        effect = "Das tägliche Grundlagen-Werkzeug: die höchste HRV-Wirkung unter den " +
            "langsamen Techniken.",
        effectDetail = "Die hohe HRV-Wirkung entsteht, weil bei ~6 Atemzügen pro Minute (≈0,1 Hz) " +
            "Atem, Herzschlag und Blutdruck-Regelkreis (Baroreflex) im selben Takt schwingen – " +
            "die HRV erreicht ihr Maximum. " +
            "Über Wochen geübt kann das den Ruhe-Vagustonus und die Baroreflex-Empfindlichkeit " +
            "anheben (Goessl 2017: deutliche Wirkung auf Stress und Angst). Die „5,5/min“ sind " +
            "nur ein gerundeter Startwert – deine persönliche Resonanz liegt meist zwischen 4,5 " +
            "und 6,5/min, etwas Ausprobieren lohnt sich. Anders als 4-7-8 oder Box kommt " +
            "Resonanz ohne Halten aus und ist damit das ruhige tägliche Grundlagen-Werkzeug. " +
            "Wichtig ist gleichmäßiges, nicht zu tiefes Atmen, um nicht versehentlich zu " +
            "hyperventilieren.",
        instructionHint = "~4 s durch die Nase ein, ~6 s ruhig aus, ohne zu pressen. " +
            "Finde ggf. deine persönliche Resonanz (oft 5,5/min).",
        references = listOf(Refs.shaffer2020, Refs.lehrer2014, Refs.goessl2017),
        rounds = 1,
        segments = listOf(
            Segment(
                repeat = 60, // ~10 min bei 10 s pro Atemzug
                phases = listOf(
                    Phase(PhaseType.INHALE, secs(4.0)),
                    Phase(PhaseType.EXHALE, secs(6.0)),
                ),
            ),
        ),
    )

    private val cyclicSighing = Exercise(
        id = "cyclic-sighing",
        name = "Cyclic Sighing",
        family = BreathingFamily.DOWNREGULATE,
        tag = EvidenceTag.BEST_EVIDENCE,
        shortDescription = "Dieselbe Technik wie der Physiological Sigh, aber als ~5-minütige " +
            "Dauerpraxis: ein voller Einatem durch die Nase, ein kurzer zweiter oben drauf " +
            "(füllt die Lunge ganz), dann ein langer Ausatem durch den Mund – fortlaufend.",
        effect = "Hebt in einer Stanford-Studie die Stimmung und senkt die Atemfrequenz am " +
            "deutlichsten.",
        effectDetail = "In der Stanford-RCT (Balban 2023, 100 ausgewertete Personen, 5 min/Tag " +
            "über 28 Tage) hob Cyclic Sighing die Stimmung etwas stärker als Achtsamkeit, " +
            "Box-Atmung oder kontrollierte Hyperventilation und senkte die Atemfrequenz am " +
            "deutlichsten. Der Mechanismus dahinter: Der zweite kurze Einatem bläht " +
            "Lungenbläschen wieder auf, die im flachen Atem teils kollabieren; der lange " +
            "Ausatem maximiert die CO₂-Abgabe und senkt über den Vagus die Herzfrequenz. " +
            "Das ist bislang eine einzelne, autoren-nahe Studie " +
            "ohne unabhängige Replikation – „am wirksamsten“ ist also ein vorsichtiges " +
            "Zwischenergebnis, kein Endurteil. Als tägliche 5-Minuten-Praxis das stärkste der " +
            "kurzen Stimmungs-Werkzeuge; dieselbe Mechanik einzeln im Stressmoment heißt " +
            "„Physiological Sigh“.",
        instructionHint = "1 voller Einatem durch die Nase → kurzer zweiter Einatem ganz oben " +
            "drauf → langsam vollständig durch den Mund aus. ~5 min am Stück, langer Ausatem.",
        references = listOf(Refs.balban2023),
        rounds = 1,
        segments = listOf(
            Segment(
                repeat = 24, // ~5 min bei ~12,5 s pro Zyklus
                phases = listOf(
                    Phase(PhaseType.INHALE, secs(4.0), note = "durch die Nase"),
                    Phase(PhaseType.INHALE_TOP_UP, secs(1.5), note = "durch die Nase"),
                    Phase(PhaseType.EXHALE, secs(7.0), note = "lang, durch den Mund"),
                ),
            ),
        ),
    )

    private val physiologicalSigh = Exercise(
        id = "physiological-sigh",
        name = "Physiological Sigh",
        family = BreathingFamily.DOWNREGULATE,
        tag = EvidenceTag.BEST_EVIDENCE,
        shortDescription = "Die Sofort-Notbremse: ein voller Einatem durch die Nase, dann ein " +
            "kurzer zweiter oben drauf (bläht kollabierte Lungenbläschen wieder auf), gefolgt " +
            "von einem langen Ausatem durch den Mund. Senkt akute Anspannung in Sekunden.",
        effect = "Die Kurzform von Cyclic Sighing für den akuten Stressmoment – schon 1–3 " +
            "Atemzüge genügen.",
        effectDetail = "Warum wenige Atemzüge reichen: Der zweite Einatem öffnet kollabierte " +
            "Lungenbläschen wieder und stellt die Dehnbarkeit der Lunge her; der betont lange " +
            "Ausatem erhöht die vagale Bremswirkung und verlangsamt den Herzschlag. Vom " +
            "Huberman-Lab wird der Doppel-Seufzer als schnellster akuter Stresslöser " +
            "beschrieben – das ist physiologisch plausibel, aber nur indirekt belegt: Die " +
            "Studienlage (Balban 2023) prüfte die 5-Minuten-Praxis (Cyclic Sighing), nicht den " +
            "einzelnen Atemzug. Dass der Körper solche Doppel-Seufzer von selbst erzeugt, ist " +
            "dagegen gut belegt (Li 2016 fand die auslösenden Hirnstamm-Neurone). Als kurzes, " +
            "risikoarmes Werkzeug bei aufkommender Anspannung 1–3× einsetzen, danach normal " +
            "weiteratmen.",
        instructionHint = "1× voll durch die Nase ein → kurzer zweiter Einatem ganz oben drauf " +
            "→ langer Ausatem durch den Mund. Bei Anspannung 1–3×, danach normal weiter.",
        references = listOf(Refs.li2016, Refs.balban2023),
        rounds = 3,
        segments = listOf(
            Segment(
                phases = listOf(
                    Phase(PhaseType.INHALE, secs(2.0), note = "durch die Nase"),
                    Phase(PhaseType.INHALE_TOP_UP, secs(1.0), note = "durch die Nase"),
                    Phase(PhaseType.EXHALE, secs(5.0), note = "lang, durch den Mund"),
                ),
            ),
        ),
    )

    private val fourSevenEight = Exercise(
        id = "4-7-8",
        name = "4-7-8-Atmung",
        family = BreathingFamily.DOWNREGULATE,
        shortDescription = "Langer Ausatem mit Atempause: 4 s ein, 7 s halten, 8 s aus. " +
            "Gut zum Herunterfahren und Einschlafen.",
        effect = "Beruhigt akut und hilft beim Herunterfahren und Einschlafen.",
        effectDetail = "Der entscheidende Wirkstoff hinter der Beruhigung ist der lange Ausatem (8 s), nicht das " +
            "Halten: Ausatem länger als Einatem verschiebt die Balance über den Vagus Richtung " +
            "Parasympathikus und verstärkt die RSA. Akut sinken Stress, Herzfrequenz und " +
            "Blutdruck (Vierra 2022, kleine Akutstudie). Im direkten Vergleich hob gleichmäßiges " +
            "6/min die HRV allerdings stärker als 4-7-8 oder Box (Vergleichsstudie 2025) – 4-7-8 " +
            "ist eher die Runterfahr- und Einschlaf-Variante als ein HRV-Training. Das populäre " +
            "„natürliche Beruhigungsmittel“ (Andrew Weil) ist eine anschauliche, aber nicht durch " +
            "Studien belegte Formulierung. Wenige Zyklen genügen; das 7-Sekunden-Halten bei " +
            "Bedarf verkürzen.",
        instructionHint = "4 s durch die Nase ein · 7 s halten · 8 s durch den Mund aus · " +
            "3–4 Zyklen. Zungenspitze hinter den oberen Schneidezähnen; Halten ggf. verkürzen.",
        cautions = listOf(
            "Nicht im Stehen üben.",
            "Wenige Zyklen genügen; zu viele können durch den CO₂-Anstieg unangenehm werden.",
        ),
        references = listOf(Refs.vierra2022, Refs.compare2025),
        rounds = 4,
        segments = listOf(
            Segment(
                phases = listOf(
                    Phase(PhaseType.INHALE, secs(4.0), note = "durch die Nase"),
                    Phase(PhaseType.HOLD_FULL, secs(7.0), note = "voll"),
                    Phase(PhaseType.EXHALE, secs(8.0), note = "durch den Mund"),
                ),
            ),
        ),
    )

    private val bhramari = Exercise(
        id = "bhramari",
        name = "Bhramari (Summen)",
        family = BreathingFamily.DOWNREGULATE,
        shortDescription = "Ein langer, summender Ausatem („Bienenatem“). Die Vibration und der " +
            "lange tonisierte Ausatem wirken stark vagal und steigern das nasale Stickstoffmonoxid.",
        effect = "Der summende Ausatem senkt unmittelbar Blutdruck und Herzfrequenz.",
        effectDetail = "Hinter der unmittelbaren Beruhigung wirken zwei Dinge zusammen: der " +
            "lange, gegen Widerstand summende Ausatem " +
            "beruhigt über den Vagus (mehr HF-Anteil, langsamerer Puls), und die Vibration im " +
            "Rachen erhöht das nasale Stickstoffmonoxid stark – Summen steigert es etwa um das " +
            "15-Fache (Weitzberg 2002). Dieser NO-Effekt ist allerdings lokal in den " +
            "Nasennebenhöhlen; ein systemischer oder gar krankheitsheilender Nutzen ist nicht " +
            "belegt. Die klinischen Studien sind meist klein und unverblindet, zeigen aber " +
            "konsistent eine sofortige Senkung von Blutdruck und Puls. Ein ruhiger „Bienenatem“ " +
            "mit lockerem Kiefer und geschlossenem Mund.",
        instructionHint = "Durch die Nase einatmen → mit geschlossenem Mund lang summend " +
            "durch die Nase ausatmen · 5–10 Runden, Mund geschlossen, Kiefer locker.",
        references = listOf(Refs.pramanik2010, Refs.weitzberg2002),
        rounds = 6,
        segments = listOf(
            Segment(
                phases = listOf(
                    Phase(PhaseType.INHALE, secs(5.0), note = "durch die Nase"),
                    Phase(PhaseType.EXHALE, secs(10.0), note = "summend, durch die Nase"),
                ),
            ),
        ),
    )

    private val diaphragmatic = Exercise(
        id = "zwerchfell",
        name = "Zwerchfellatmung",
        family = BreathingFamily.DOWNREGULATE,
        shortDescription = "Die Grundlagentechnik: langsam in den Bauch atmen (Zwerchfell) statt " +
            "in die Brust. Basis, auf der alle anderen Techniken aufbauen.",
        effect = "Die ruhige Basis aller Techniken: senkt Anspannung und verlangsamt den Atem " +
            "von selbst.",
        effectDetail = "Die Anspannung sinkt, weil Bauchatmung (Zwerchfell) mehr Luft mit " +
            "weniger Aufwand bewegt: Wer so atmet, wird automatisch langsamer – und unterhalb " +
            "von ~10 Atemzügen pro Minute verschiebt sich die Balance Richtung Parasympathikus, " +
            "die HRV steigt (Zaccaro 2018). In einem achtwöchigen Training (Ma 2017, n=40) verbesserten sich " +
            "Aufmerksamkeit und Stimmung und der Cortisolspiegel sank – allerdings mit intensivem, " +
            "angeleitetem Üben, nicht durch „Bauchatmung“ allein. „Zwerchfellatmung“ ist eher ein " +
            "Sammelbegriff; die isolierte Wirkung ist daher schwer zu beziffern. Sie ist die " +
            "Grundlage, auf der die anderen Techniken aufbauen. Bei schwerer COPD mit überblähter " +
            "Lunge kann betont tiefes Bauchatmen die Atemnot eher verstärken – dann sanft dosieren.",
        instructionHint = "Hand auf den Bauch · langsam einatmen, sodass sich der Bauch hebt " +
            "(nicht die Brust) · langsam durch gespitzte Lippen ausatmen · einige Minuten.",
        references = listOf(Refs.zaccaro2018, Refs.ma2017),
        rounds = 1,
        segments = listOf(
            Segment(
                repeat = 36, // ~6 min bei 10 s pro Atemzug
                phases = listOf(
                    Phase(PhaseType.INHALE, secs(4.0), note = "in den Bauch"),
                    Phase(PhaseType.EXHALE, secs(6.0), note = "durch gespitzte Lippen"),
                ),
            ),
        ),
    )

    private val sitali = Exercise(
        id = "sitali",
        name = "Sitali (Kühlatmung)",
        family = BreathingFamily.DOWNREGULATE,
        shortDescription = "Einatmen über die gerollte Zunge, ausatmen durch die Nase – die " +
            "einströmende Luft fühlt sich kühl und frisch an. Eine angenehme Variante des " +
            "langsamen, ausatem-betonten Atmens.",
        effect = "Beruhigt wie die anderen langsamen Techniken – mit angenehm kühlem Sinnesreiz.",
        effectDetail = "Die Wirkung kommt – wie bei den anderen ruhigen Techniken – aus dem " +
            "langsamen, ausatem-betonten Atmen, das die Balance Richtung Parasympathikus " +
            "verschiebt; der kühle Luftstrom über die feuchte Zunge ist ein angenehmer " +
            "Sinnesreiz, der beim Dranbleiben hilft. Die Evidenz speziell zu Sitali ist dünn: " +
            "Eine kleine, unverblindete Studie fand eine Blutdrucksenkung (Shetty), das Ergebnis " +
            "unterscheidet sich aber kaum von einfachem langsamem Atmen. Wichtig zur Ehrlichkeit: Der Name verspricht „Kühlung“, doch eine Messung " +
            "(Telles 2020) fand die Körpertemperatur sogar leicht erhöht – es ist ein gefühlt " +
            "kühler Atem, keine echte Abkühlung des Körpers.",
        instructionHint = "Zunge zu einem Röhrchen rollen, ~4 s kühl darüber einatmen → Mund zu, " +
            "~6 s ruhig durch die Nase aus. Wer die Zunge nicht rollen kann: durch die locker " +
            "zusammengelegten Zähne einatmen (Sitkari).",
        cautions = listOf(
            "Zungenrollen ist nicht jedem möglich – dann die Sitkari-Variante (durch die Zähne).",
            "Eher nicht bei Kälte, Atemwegsinfekt, Asthma oder niedrigem Blutdruck.",
        ),
        references = listOf(Refs.shetty2019, Refs.telles2020),
        rounds = 1,
        segments = listOf(
            Segment(
                repeat = 30, // ~5 min bei 10 s pro Atemzug
                phases = listOf(
                    Phase(PhaseType.INHALE, secs(4.0), note = "kühl über die Zunge"),
                    Phase(PhaseType.EXHALE, secs(6.0), note = "durch die Nase"),
                ),
            ),
        ),
    )

    // ── B · Hochregeln (sympathisch) ─────────────────────────────────────────

    private val wimHof = Exercise(
        id = "wim-hof",
        name = "Wim Hof",
        family = BreathingFamily.UPREGULATE,
        tag = EvidenceTag.CAUTION,
        shortDescription = "Kontrollierte Hyperventilation plus Atemanhalten: 30 tiefe Atemzüge, " +
            "dann Halten auf leerer Lunge bis zum Atemreiz, dann tief einatmen und 15 s halten. " +
            "Ein hormetischer Stressor – nur in stabiler, guter Phase.",
        effect = "Dämpft im Experiment die akute Entzündungsreaktion; ein Heileffekt ist " +
            "nicht belegt.",
        effectDetail = "Die schnellen tiefen Atemzüge senken das CO₂ (Hypokapnie – daher Kribbeln " +
            "und Schwindel), das anschließende Halten erzeugt eine kurze, dosierte " +
            "Sauerstoffknappheit. Der Reiz löst eine Sympathikus-/Adrenalin-Welle aus, die " +
            "entzündungshemmendes IL-10 hochfährt. In der Endotoxin-Studie (Kox 2014, 24 Männer) " +
            "fielen die Entzündungsmarker deutlich (TNF-α −53 %, IL-6 −57 %), IL-10 stieg " +
            "(+194 %) und die Grippe-Symptome waren milder; eine Pilotstudie (Zwaag 2022) " +
            "isolierte die Atmung als Haupttreiber. Zur Einordnung: Die systematische " +
            "Übersicht (2024) stuft die Gesamtevidenz als „sehr niedrig“ ein (winzige, fast nur " +
            "männliche Stichproben); ein chronischer Heileffekt ist nicht belegt. Es ist ein " +
            "hormetischer Stressor – das Gegenteil von langsamem Beruhigungsatmen – und gehört " +
            "nur in eine klar stabile, gute Phase, nie ins oder ans Wasser.",
        instructionHint = "30 tiefe Atemzüge (tief ein, locker durch den Mund aus, nicht ganz leeren) → ausatmen und halten, so lange " +
            "angenehm → tief einatmen, 15 s halten · 3 Runden · nur im Sitzen/Liegen.",
        cautions = listOf(
            "Nur im Sitzen oder Liegen – nie im/am Wasser, beim Duschen, Fahren oder Stehen " +
                "(Ohnmachtsgefahr).",
            "Kribbeln und Schwindel sind normal; bei zu viel sofort aufhören und normal atmen.",
            "Nur in klar stabilen, guten Phasen – nicht im Schub, Crash oder akuten Infekt.",
            "Bei Epilepsie, Herz-Kreislauf-Erkrankung oder Schwangerschaft vorher ärztlich abklären.",
        ),
        references = listOf(Refs.kox2014, Refs.zwaag2022, Refs.whmReview2024),
        rounds = 3,
        segments = listOf(
            Segment(
                repeat = 30,
                phases = listOf(
                    Phase(PhaseType.INHALE, secs(1.5), note = "tief"),
                    Phase(PhaseType.EXHALE, secs(1.5), note = "locker, durch den Mund"),
                ),
            ),
            Segment(
                phases = listOf(
                    Phase(PhaseType.EXHALE, secs(2.0), note = "ganz"),
                    Phase(
                        PhaseType.HOLD_EMPTY,
                        PhaseDuration.UntilUrge,
                        note = "leer – tippen beim Atemreiz",
                    ),
                    Phase(PhaseType.INHALE, secs(2.0), note = "tief"),
                    Phase(PhaseType.HOLD_FULL, secs(15.0), note = "voll"),
                ),
            ),
        ),
    )

    private val fireBreath = Exercise(
        id = "feueratmung",
        name = "Feueratmung (Kapalabhati)",
        family = BreathingFamily.UPREGULATE,
        tag = EvidenceTag.CAUTION,
        shortDescription = "Schnelle, kräftige Ausatemstöße aus dem Bauch, der Einatem geschieht " +
            "passiv (yogischer „Blasebalg“). Energetisierend – für Wachheit am Morgen, nicht zur " +
            "Beruhigung.",
        effect = "Macht wach: Die schnellen Ausatemstöße aktivieren den Sympathikus.",
        effectDetail = "Der Wachheits-Schub entsteht, weil die schnellen, aktiven Ausatemstöße " +
            "bei passivem Einatem die Ventilation weit über den Bedarf treiben – das senkt kurz " +
            "das CO₂ und aktiviert den Sympathikus (höherer Puls, gesteigerte Aufmerksamkeit). " +
            "Studien dazu sind klein, " +
            "oft unkontrolliert und kurzfristig; traditionelle „Reinigungs-/Detox“-Aussagen sind " +
            "nicht belegt. Verwandt mit der Wim-Hof-Atmung, aber ohne deren lange Atemanhalte. Bei " +
            "kräftiger, langer Ausführung ist sehr selten ein Pneumothorax beschrieben " +
            "(Fallbericht Johnson 2004) – auf leeren Magen, im Sitzen und nicht übertreiben.",
        instructionHint = "Etwa 1 kräftiger Ausatemstoß pro Sekunde durch die Nase, passiver " +
            "Einatem · 30 Stöße, dann ~45 s locker atmen · 3 Runden.",
        cautions = listOf(
            "Nur im Sitzen, auf leeren Magen – nie im Wasser oder beim Fahren.",
            "Nicht bei Schwangerschaft, unkontrolliertem Bluthochdruck, Herzerkrankung, " +
                "Epilepsie, Glaukom oder Hernie.",
            "Bei Schwindel sofort aufhören (selten: Pneumothorax bei kräftiger, langer Ausführung).",
        ),
        references = listOf(Refs.zaccaro2018, Refs.kapalabhatiCase2004),
        rounds = 3,
        segments = listOf(
            Segment(
                repeat = 30,
                phases = listOf(
                    Phase(PhaseType.EXHALE, secs(0.5), note = "Stoß, durch die Nase"),
                    Phase(PhaseType.INHALE, secs(0.5), note = "passiv, durch die Nase"),
                ),
            ),
            Segment(
                phases = listOf(
                    Phase(PhaseType.REST, secs(45.0)),
                ),
            ),
        ),
    )

    // ── C · Balancieren / Programme ──────────────────────────────────────────

    private val box = Exercise(
        id = "box-4-4-4-4",
        name = "Box-Atmung",
        family = BreathingFamily.BALANCE,
        shortDescription = "Gleich lange Phasen: 4 s ein, 4 s halten, 4 s aus, 4 s halten. " +
            "Balanciert das Nervensystem auf ruhig, aber wach – das „taktische“ Atmen.",
        effect = "Senkt akut die Anspannung und hält dich dabei wach und konzentriert.",
        effectDetail = "„Ruhig, aber wach“ entsteht, weil gleich lange Phasen mit zwei Pausen " +
            "den Atem auf einen langen, ruhigen Zyklus takten und so HRV und Baroreflex " +
            "stützen – die Belege stammen vor allem aus " +
            "der allgemeinen Slow-Breathing-Forschung, kaum aus Box-spezifischen Studien. In der " +
            "Stanford-Studie senkte Box die Anspannung, war aber schwächer als Cyclic Sighing. " +
            "Die bekannte „Navy-SEAL“-Erzählung ist Folklore, kein Studienbeleg – überzeugend ist " +
            "Box als einfaches, symmetrisches Muster für „ruhig, aber wach“ vor einer Aufgabe. " +
            "Achtung: Die Halte-Pausen können bei Panik Lufthunger auslösen; dann besser eine " +
            "Technik ohne Halten (z. B. Resonanz). Für reine HRV ist 6/min stärker.",
        instructionHint = "4 s ein · 4 s halten · 4 s aus · 4 s halten · ~2–5 min. " +
            "Durch die Nase. Zu lang? Nimm 3-3-3-3. Ideal vor einer Aufgabe – ruhig und konzentriert.",
        references = listOf(Refs.balban2023),
        rounds = 8, // ~2 min
        segments = listOf(
            Segment(
                phases = listOf(
                    Phase(PhaseType.INHALE, secs(4.0)),
                    Phase(PhaseType.HOLD_FULL, secs(4.0), note = "voll"),
                    Phase(PhaseType.EXHALE, secs(4.0)),
                    Phase(PhaseType.HOLD_EMPTY, secs(4.0), note = "leer"),
                ),
            ),
        ),
    )

    private val nadiShodhana = Exercise(
        id = "nadi-shodhana",
        name = "Nadi Shodhana (Wechselatmung)",
        family = BreathingFamily.BALANCE,
        shortDescription = "Abwechselndes Atmen durch je ein Nasenloch, gesteuert mit dem Finger. " +
            "Wirkt ausgleichend und zentrierend.",
        effect = "Wirkt ausgleichend und zentrierend – getragen vom langsamen Atemrhythmus.",
        effectDetail = "Wahrscheinlicher Wirkstoff ist das langsame, ausatem-betonte Atmen, nicht " +
            "der Nasenloch-Wechsel selbst – die Vorstellung, einzelne Nasenlöcher steuerten gezielt " +
            "Sympathikus bzw. Gehirnhälften, ist spekulativ. Eine Meta-Analyse (Nam 2024, 6 RCTs, " +
            "n=525) fand eine Blutdrucksenkung (SBP ~−7 mmHg), bei allerdings hoher Heterogenität " +
            "und kaum Verblindung; kleinere Studien berichten auch günstige HRV-Effekte, meist " +
            "in niedriger bis moderater Qualität. Der Wechsel-Rhythmus und das Mitführen der Hand binden die " +
            "Aufmerksamkeit und helfen beim Zentrieren – das fördert die Praxis, auch wenn der " +
            "spezifische Mechanismus offen ist. Die App gibt nur Takt und Seitenwechsel vor, ohne " +
            "erzwungenes Halten.",
        instructionHint = "Rechtes Nasenloch zu, links ein → links zu, rechts aus → rechts ein → " +
            "links aus · langsam · ~5 min. Die App gibt nur den Takt + Seitenwechsel vor.",
        cautions = listOf(
            "Braucht eine freie Hand zum Schließen der Nasenlöcher – kein freihändiger Ablauf.",
            "Bei verstopfter Nase schwer umsetzbar – dann auslassen.",
        ),
        references = listOf(Refs.nam2024, Refs.nadiShodhana2024, Refs.nadiBhramari2023),
        rounds = 1,
        segments = listOf(
            Segment(
                repeat = 15, // ~5,5 min
                phases = listOf(
                    Phase(PhaseType.INHALE, secs(5.0), note = "links", side = BreathSide.LEFT),
                    Phase(PhaseType.EXHALE, secs(6.0), note = "rechts", side = BreathSide.RIGHT),
                    Phase(PhaseType.INHALE, secs(5.0), note = "rechts", side = BreathSide.RIGHT),
                    Phase(PhaseType.EXHALE, secs(6.0), note = "links", side = BreathSide.LEFT),
                ),
            ),
        ),
    )

    private val sky = Exercise(
        id = "sky",
        name = "Sudarshan Kriya (SKY)",
        family = BreathingFamily.BALANCE,
        guided = false,
        shortDescription = "Ein strukturiertes, angeleitetes Atemprogramm aus einer Abfolge " +
            "getakteter Atemmuster. Der genaue Kriya-Rhythmus ist nicht öffentlich und wird per " +
            "geführter Audio im Kurs vermittelt.",
        effect = "Als Gesamtprogramm mit antidepressiver Wirkung in klinischen Studien geprüft.",
        effectDetail = "SKY ist keine Einzeltechnik, sondern ein angeleitetes Programm aus " +
            "langsamen und schnellen Atemstufen, das die autonome Balance verschiebt (mehr " +
            "Vagustonus, weniger Sympathikus). Die klassische RCT (Janakiramaiah 2000) zeigte bei " +
            "schwerer Depression eine Remission von 67 % – schwächer als EKT (93 %), aber in der " +
            "Größenordnung von Imipramin. Neuere, sauberere Studien (Seppälä 2020, n=131) fanden " +
            "Vorteile bei Stress und Wohlbefinden. Viel Evidenz stammt von programm-nahen " +
            "Forschenden und ist niedrig bis moderat – die Effekte sind real, aber vermutlich " +
            "überzeichnet. Der genaue Kriya-Rhythmus wird im Kurs per Audio vermittelt und lässt " +
            "sich nicht in einen einfachen Takt übersetzen.",
        instructionHint = "Braucht Anleitung (Kurs/App von Art of Living) – lässt sich nicht in " +
            "einen einfachen getakteten Ablauf übersetzen.",
        references = listOf(Refs.sky2000, Refs.seppala2020),
    )

    // ── D · Funktionell ──────────────────────────────────────────────────────

    private val pursedLip = Exercise(
        id = "lippenbremse",
        name = "Lippenbremse",
        family = BreathingFamily.FUNCTIONAL,
        shortDescription = "Ausatmen gegen leicht gespitzte Lippen verlangsamt den Ausatem und " +
            "hält die Atemwege offen. Sofort-Werkzeug bei Atemnot oder hektischem Atmen.",
        effect = "Das etablierte Sofort-Werkzeug bei Atemnot und hektischem Atmen.",
        effectDetail = "Warum sie bei Atemnot hilft: Der Gegendruck der gespitzten Lippen hält " +
            "die kleinen Atemwege beim " +
            "Ausatmen offen und verlängert den Ausatem – das senkt Atemfrequenz und Atemnot, " +
            "besonders bei COPD und unter Belastung. Eine Meta-Analyse (Mayer 2018) bestätigt das " +
            "langsamere, ruhigere Atmen, fand aber keinen verlässlichen Gewinn bei Gehstrecke oder " +
            "Sauerstoffsättigung; spürbar belastbarer werden nur ein Teil der Übenden. Akut senkt " +
            "die Technik in einer RCT auch Blutdruck und Puls (Mitsungnern 2021). Sehr sicher und " +
            "sofort einsetzbar bei hektischem Atmen – ohne den Ausatem zu pressen.",
        instructionHint = "~2 s durch die Nase ein → ~4 s langsam gegen gespitzte Lippen aus " +
            "(Ausatem doppelt so lang).",
        references = listOf(Refs.pursedLip2021, Refs.mayer2018),
        rounds = 1,
        segments = listOf(
            Segment(
                repeat = 40, // ~4 min
                phases = listOf(
                    Phase(PhaseType.INHALE, secs(2.0), note = "durch die Nase"),
                    Phase(PhaseType.EXHALE, secs(4.0), note = "gegen die Lippen"),
                ),
            ),
        ),
    )

    private val walkingBreath = Exercise(
        id = "walking-breath",
        name = "Atmen im Gehen",
        family = BreathingFamily.FUNCTIONAL,
        shortDescription = "Langsames, getaktetes Atmen im Schritt-Rhythmus: ~3 Schritte ein, " +
            "~6 Schritte aus. Ein alltagstaugliches Werkzeug, das du im Gehen anwenden kannst.",
        effect = "Beruhigt unterwegs: langsames Atmen im Takt der Schritte.",
        effectDetail = "Das Prinzip ist dasselbe wie bei den anderen ruhigen Techniken: ein " +
            "Ausatem, der länger ist als der Einatem, wirkt über den Vagus beruhigend (Zaccaro " +
            "2018). Neu ist nur das Format – der Atem koppelt an die Schritte, und das Mitzählen " +
            "der Schritte bindet die Aufmerksamkeit, was hilft, sich aus Grübeln oder Anspannung " +
            "zu lösen. Ehrlich: Es gibt keinen eigenen Studienbeleg für genau diese Technik; ihr " +
            "Wert liegt in der Alltagstauglichkeit – langsam atmen, während du dich bewegst oder " +
            "weg von einem Auslöser gehst. Tempo nicht zu hoch wählen, der Ausatem bleibt länger " +
            "als der Einatem.",
        instructionHint = "Im gleichmäßigen Gehen ~3 Schritte lang einatmen, ~6 Schritte lang " +
            "ausatmen. Zählt sich von allein; bei Bedarf die Schrittzahl an dein Tempo anpassen " +
            "(Ausatem länger als Einatem).",
        cautions = listOf(
            "Tempo nicht zu hoch wählen – sonst droht Hyperventilation/Schwindel.",
            "Kein Ersatz für eine Behandlung bei Panikstörung.",
        ),
        references = listOf(Refs.zaccaro2018),
        rounds = 1,
        segments = listOf(
            Segment(
                repeat = 30, // ~4,5 min bei 9 s pro Atemzug
                phases = listOf(
                    Phase(PhaseType.INHALE, secs(3.0), note = "3 Schritte"),
                    Phase(PhaseType.EXHALE, secs(6.0), note = "6 Schritte"),
                ),
            ),
        ),
    )

    private val buteyko = Exercise(
        id = "buteyko",
        name = "Buteyko",
        family = BreathingFamily.FUNCTIONAL,
        guided = false,
        shortDescription = "Ein Atemmuster-Training hin zu reduzierter, ruhiger, nasaler Atmung, " +
            "um die CO₂-Toleranz zu erhöhen und chronisches Überatmen zu korrigieren. Ein " +
            "Wochen-Programm, kein Akut-Trick.",
        effect = "Lindert bei Asthma die Symptome und senkt den Bedarf an Notfallspray.",
        effectDetail = "Buteyko trainiert bewusst reduziertes, ruhiges Nasenatmen samt kurzer " +
            "Atempausen („Control Pause“) mit dem Ziel, die CO₂-Toleranz zu erhöhen. Die " +
            "Cochrane-Übersicht (2020) zeigt: Symptome, Lebensqualität und Bedarf an " +
            "Notfallspray bessern sich, die objektive Lungenfunktion (FEV1) jedoch nicht. Die " +
            "angenommene CO₂-Korrektur ließ sich in Studien nicht bestätigen (Bowler 1998) – der " +
            "Nutzen kommt eher aus ruhigerem Atmen und weniger Atem-Aufwand. Relevant ist das " +
            "Training vor allem bei Asthma und dysfunktionaler Atmung/Atemnot. Wichtig: ein " +
            "ergänzendes Wochen-Training, kein Akut-Trick – und Cortison-Sprays niemals " +
            "eigenmächtig reduzieren.",
        instructionHint = "Bewusst weniger und ruhiger durch die Nase atmen, sanfte „Lufthunger“-" +
            "Phasen, über Wochen. Messbarer Anker: die Control Pause (Atemhalte-Zeit).",
        references = listOf(Refs.buteykoPrem2013, Refs.buteykoCochrane2020),
    )

    private val nasalBreathing = Exercise(
        id = "nasenatmung",
        name = "Nasenatmung (Basis)",
        family = BreathingFamily.FUNCTIONAL,
        guided = false,
        shortDescription = "Konsequentes Atmen durch die Nase – Tag und Nacht. Erzeugt " +
            "Stickstoffmonoxid, befeuchtet und filtert die Luft. Das Fundament aller anderen " +
            "Techniken, keine eigene Sitzung.",
        effect = "Das Fundament: Die Nase filtert und befeuchtet die Luft und liefert " +
            "Stickstoffmonoxid mit.",
        effectDetail = "In den Nasennebenhöhlen entsteht laufend Stickstoffmonoxid (NO), das mit " +
            "der eingeatmeten Luft in die Lunge gelangt und dort Gefäße und Bronchien weitet – das " +
            "verbessert die Sauerstoffaufnahme (Lundberg 1996: messbar höherer Sauerstoffwert bei " +
            "Nasen- gegenüber Mundatmung). Dazu wärmt, befeuchtet und filtert die Nase die Luft " +
            "und bremst durch ihren Widerstand das Atemtempo. Die Belege sind vor allem " +
            "physiologisch; große Studien zu harten Gesundheits-Endpunkten fehlen. Für Alltag " +
            "und Schlaf ist die Nasenatmung der Mundatmung vorzuziehen; bei harter körperlicher " +
            "Belastung ist der Wechsel zur Mundatmung normal und sinnvoll.",
        instructionHint = "Konsequent durch die Nase atmen, auch bei leichter Belastung und " +
            "idealerweise im Schlaf. Bei chronisch verstopfter Nase die Ursache angehen.",
        references = listOf(Refs.nasalNo1996),
    )

    /** Alle Übungen in Familien-Reihenfolge (A → D). */
    val all: List<Exercise> = listOf(
        resonance, cyclicSighing, physiologicalSigh, fourSevenEight, bhramari, diaphragmatic, sitali,
        wimHof, fireBreath,
        box, nadiShodhana, sky,
        pursedLip, walkingBreath, buteyko, nasalBreathing,
    )
}
