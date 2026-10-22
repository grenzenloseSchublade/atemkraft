package app.atemkraft.data

/** Ein Glossar-Eintrag: Begriff und Erklärung. */
data class GlossaryTerm(val name: String, val definition: String)

/**
 * Inhalte des Glossars „Begriffe erklärt“ in zwei Gruppen. Liegt in der Datenschicht wie die
 * Übungstexte, damit ContentRulesTest dieselben Regeln prüft (TEXT-04 … -10, -13).
 */
object Glossary {
    /** Körper & Studien: Begriffe aus Physiologie und Studienlage. */
    val bodyTerms = listOf(
        GlossaryTerm(
            "Studienlage",
            "Wie gut eine Atemtechnik untersucht ist; jede trägt dazu ein Etikett (Muster nicht, sie " +
                "sind spielerische Variationen). „gut belegt“: Eine Auswertung vieler Studien oder " +
                "mehrere unabhängige Studien zeigen dasselbe. „in Studien geprüft“: Es gibt Studien, " +
                "aber wenige, kleine oder nur mit bestimmten Gruppen. „kaum untersucht“: Die Wirkweise " +
                "ist plausibel, direkte Studien fehlen weitgehend. Das Etikett sagt, wie sicher das " +
                "Wissen ist, nicht wie stark eine Technik wirkt.",
        ),
        GlossaryTerm(
            "Meta-Analyse",
            "Eine Auswertung, die die Ergebnisse mehrerer Studien zur selben Frage zusammenrechnet. " +
                "Sie ist nur so verlässlich wie die Studien, die sie einschließt.",
        ),
        GlossaryTerm(
            "RCT – randomisierte kontrollierte Studie",
            "Eine Studie, in der das Los entscheidet, wer die Übung macht und wer zum Vergleich etwas " +
                "anderes oder nichts. So lässt sich ein Unterschied am ehesten der Übung zuschreiben.",
        ),
        GlossaryTerm(
            "verblindet / unverblindet",
            "Verblindet heißt: Die Teilnehmenden, oft auch die Auswertenden, wissen nicht, wer zu " +
                "welcher Gruppe gehört. Bei Atemübungen ist das kaum möglich; in unverblindeten Studien " +
                "kann die Erwartung das Ergebnis mitprägen.",
        ),
        GlossaryTerm(
            "HRV – Herzratenvariabilität",
            "Die natürliche Schwankung der Zeit zwischen zwei Herzschlägen. Eine höhere HRV gilt als " +
                "Zeichen für Erholung, Stressresistenz und einen aktiven Vagus.",
        ),
        GlossaryTerm(
            "Vagus / Vagustonus",
            "Der Vagusnerv ist der Hauptnerv des Ruhe-Systems (Parasympathikus). Ein hoher " +
                "Vagustonus gilt als Zeichen guter Erholungsfähigkeit.",
        ),
        GlossaryTerm(
            "Parasympathikus / Sympathikus",
            "Die zwei Gegenspieler des vegetativen Nervensystems: der Parasympathikus beruhigt " +
                "(„Bremse“), der Sympathikus aktiviert („Gas“).",
        ),
        GlossaryTerm(
            "Baroreflex",
            "Ein Regelkreis, der den Blutdruck stabil hält. Langsames Atmen um 6 Atemzüge pro Minute " +
                "bringt ihn in Resonanz; ob er dadurch auf Dauer trainiert wird, ist nur in kleinen " +
                "Studien untersucht.",
        ),
        GlossaryTerm(
            "RSA – Respiratorische Sinusarrhythmie",
            "Die Herzfrequenz steigt beim Einatmen und sinkt beim Ausatmen. Eine ausgeprägte RSA " +
                "spricht für einen guten Vagustonus.",
        ),
        GlossaryTerm(
            "CO₂-Toleranz",
            "Wie gut der Körper einen leichten Anstieg von Kohlendioxid verträgt. Die Buteyko-Lehre " +
                "nimmt an, dass eine höhere Toleranz ruhigeres Atmen ermöglicht; Studien bestätigen " +
                "das bisher nicht.",
        ),
        GlossaryTerm(
            "Hyperventilation",
            "Mehr atmen, als der Körper gerade braucht. Dabei sinkt das Kohlendioxid im Blut; das " +
                "kann Kribbeln, Schwindel und Benommenheit auslösen. Wim Hof und die Feueratmung nutzen " +
                "das bewusst, bei ruhigen Übungen ist es unerwünscht.",
        ),
        GlossaryTerm(
            "Hormese",
            "Die Idee, dass kleine, dosierte Belastungsreize (z. B. Wim Hof) den Körper zur Anpassung " +
                "anregen. Für Atemtechniken ist das kaum untersucht.",
        ),
        GlossaryTerm(
            "COPD",
            "Chronisch obstruktive Lungenerkrankung: dauerhaft verengte Atemwege, häufig nach langem " +
                "Rauchen. Die Studien zur Lippenbremse untersuchen vor allem Menschen mit COPD.",
        ),
        GlossaryTerm(
            "PEM – Post-Exertional Malaise",
            "Verschlechterung des Zustands nach Anstrengung (z. B. bei ME/CFS oder Long COVID). Dann nur " +
                "sanfte Übungen zum Herunterregeln.",
        ),
    )

    /**
     * Aus der Yoga-Lehre: Begriffe der Tradition, zuschreibend erklärt (keine Wirkbehauptung).
     * Sanskrit klein, kursiv und mit Längenzeichen (TEXT-14). Jede Angabe steht nur einmal: Was
     * *prāṇa* ist, sagt sein eigener Eintrag; die fünf *vāyus* je kurz, das Bild dazu in der Atemlehre.
     */
    val yogaTerms = listOf(
        GlossaryTerm(
            "*prāṇāyāma*",
            "Die Lenkung des Atems im Yoga; das Wort verbindet *prāṇa* mit *āyāma* (Ausdehnung). " +
                "Bei Patanjali die vierte Stufe des Yoga-Wegs.",
        ),
        GlossaryTerm(
            "*pūraka*\u00A0· *recaka*\u00A0· *kumbhaka*",
            "Einatmen, Ausatmen und Anhalten. *antara kumbhaka* ist das Halten mit voller, " +
                "*bāhya kumbhaka* das Halten mit leerer Lunge.",
        ),
        GlossaryTerm(
            "*bṛṃhaṇa*\u00A0· *laṅghana*\u00A0· *samāna*",
            "Längeres Einatmen gilt als nährend und belebend (*bṛṃhaṇa*), längeres Ausatmen als " +
                "beruhigend und kühlend (*laṅghana*), gleich langes als ausgleichend (*samāna*). Begriffe " +
                "aus der Lehre von Krishnamacharya und Desikachar.",
        ),
        GlossaryTerm(
            "*vāyu*",
            "„Wind“: In der Yoga-Lehre fließt *prāṇa* in fünf Strömungen, den *vāyus*, jede mit " +
                "eigenem Ort und eigener Richtung. Ein Bild der Lehre, keine Anatomie.",
        ),
        GlossaryTerm(
            "*prāṇa*",
            "Die Lebenskraft, die nach der Lehre mit dem Atem in den Körper kommt. Als *vāyu* die " +
                "aufnehmende Strömung im Brustraum, nach innen.",
        ),
        GlossaryTerm(
            "*apāna*",
            "Der *vāyu* in Becken und unterem Bauch, nach unten und außen; gilt als die abgebende " +
                "Kraft: Ausatmen, Ausscheiden.",
        ),
        GlossaryTerm(
            "*samāna*",
            "Der *vāyu* der Nabelmitte, zur Mitte hin; gilt als die verdauende, ausgleichende Kraft.",
        ),
        GlossaryTerm(
            "*udāna*",
            "Der *vāyu* in Hals und Kopf, nach oben; gilt als die Kraft von Stimme und Aufrichtung.",
        ),
        GlossaryTerm(
            "*vyāna*",
            "Der *vāyu*, der den ganzen Körper durchzieht, von der Mitte nach außen; gilt als die " +
                "verteilende Kraft.",
        ),
        GlossaryTerm(
            "*iḍā*\u00A0· *piṅgalā*",
            "Zwei Hauptkanäle (*nāḍīs*) von *prāṇa*: *iḍā* links, Mond, kühlend; *piṅgalā* rechts, " +
                "Sonne, wärmend. Die Wechselatmung soll sie ausgleichen.",
        ),
        GlossaryTerm(
            "*sama vṛtti*\u00A0· *viṣama vṛtti*",
            "Atmen in gleich langen Phasen (*sama vṛtti*) oder in ungleichen Verhältnissen " +
                "(*viṣama vṛtti*).",
        ),
        GlossaryTerm(
            "*bandha*",
            "Ein Muskelverschluss, der beim Anhalten *prāṇa* lenken soll, etwa das gesenkte Kinn " +
                "(*jālandhara bandha*) oder der eingezogene Bauch (*uḍḍīyāna bandha*).",
        ),
    )
}
