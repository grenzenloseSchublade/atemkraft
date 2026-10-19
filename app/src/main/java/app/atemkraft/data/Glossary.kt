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

    /** Aus der Yoga-Lehre: Begriffe der Tradition, zuschreibend erklärt (keine Wirkbehauptung). */
    val yogaTerms = listOf(
        GlossaryTerm(
            "Pranayama",
            "Die Lenkung des Atems im Yoga; das Wort verbindet Prana (Lebenskraft) mit Ayama " +
                "(Ausdehnung). Bei Patanjali die vierte Stufe des Yoga-Wegs.",
        ),
        GlossaryTerm(
            "Puraka\u00A0· Rechaka\u00A0· Kumbhaka",
            "Einatmen, Ausatmen und Anhalten. Antara Kumbhaka ist das Halten mit voller, Bahya " +
                "Kumbhaka das Halten mit leerer Lunge.",
        ),
        GlossaryTerm(
            "Brmhana\u00A0· Langhana\u00A0· Samana",
            "Längeres Einatmen gilt als nährend und belebend (Brmhana), längeres Ausatmen als " +
                "beruhigend und kühlend (Langhana), gleich langes als ausgleichend (Samana). Begriffe " +
                "aus der Lehre von Krishnamacharya und Desikachar.",
        ),
        GlossaryTerm(
            "Prana\u00A0· Apana",
            "Zwei Strömungen der Lebenskraft: Prana nimmt auf und gehört zum Einatmen, Apana lässt " +
                "los und gehört zum Ausatmen.",
        ),
        GlossaryTerm(
            "Ida\u00A0· Pingala",
            "Zwei Hauptkanäle der Lebenskraft: Ida links, Mond, kühlend; Pingala rechts, Sonne, " +
                "wärmend. Die Wechselatmung soll sie ausgleichen.",
        ),
        GlossaryTerm(
            "Sama Vritti\u00A0· Vishama Vritti",
            "Atmen in gleich langen Phasen (Sama Vritti) oder in ungleichen Verhältnissen " +
                "(Vishama Vritti).",
        ),
        GlossaryTerm(
            "Bandha",
            "Ein Muskelverschluss, der beim Anhalten die Lebenskraft lenken soll, etwa das gesenkte " +
                "Kinn (Jalandhara) oder der eingezogene Bauch (Uddiyana).",
        ),
    )
}
