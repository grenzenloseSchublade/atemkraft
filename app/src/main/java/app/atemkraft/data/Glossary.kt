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
            "HRV – Herzratenvariabilität",
            "Die natürliche Schwankung der Zeit zwischen zwei Herzschlägen. Eine höhere HRV gilt als " +
                "Zeichen für Erholung, Stressresistenz und einen aktiven Vagus.",
        ),
        GlossaryTerm(
            "Vagus / Vagustonus",
            "Der Vagusnerv ist der Hauptnerv des Ruhe-Systems (Parasympathikus). Hoher Vagustonus " +
                "bedeutet bessere Beruhigung, Erholung und HRV.",
        ),
        GlossaryTerm(
            "Parasympathikus / Sympathikus",
            "Die zwei Gegenspieler des vegetativen Nervensystems: der Parasympathikus beruhigt " +
                "(„Bremse“), der Sympathikus aktiviert („Gas“).",
        ),
        GlossaryTerm(
            "Baroreflex",
            "Ein Regelkreis, der den Blutdruck stabil hält. Langsames Atmen (~6/min) bringt ihn in " +
                "Resonanz und trainiert ihn.",
        ),
        GlossaryTerm(
            "RSA – Respiratorische Sinusarrhythmie",
            "Die Herzfrequenz steigt beim Einatmen und sinkt beim Ausatmen. Eine ausgeprägte RSA " +
                "spricht für einen guten Vagustonus.",
        ),
        GlossaryTerm(
            "CO₂-Toleranz",
            "Wie gut der Körper einen leichten Anstieg von Kohlendioxid verträgt. Höhere Toleranz " +
                "ermöglicht ruhigeres, effizienteres Atmen.",
        ),
        GlossaryTerm(
            "Hormese",
            "Die Idee, dass kleine, dosierte Belastungsreize (z. B. Wim Hof) den Körper zur Anpassung " +
                "anregen. Für Atemtechniken ist das kaum untersucht.",
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
            "Puraka · Rechaka · Kumbhaka",
            "Einatmen, Ausatmen und Anhalten. Antara Kumbhaka ist das Halten mit voller, Bahya " +
                "Kumbhaka das Halten mit leerer Lunge.",
        ),
        GlossaryTerm(
            "Brmhana · Langhana · Samana",
            "Längeres Einatmen gilt als nährend und belebend (Brmhana), längeres Ausatmen als " +
                "beruhigend und kühlend (Langhana), gleich langes als ausgleichend (Samana). Begriffe " +
                "aus der Lehre von Krishnamacharya und Desikachar.",
        ),
        GlossaryTerm(
            "Prana · Apana",
            "Zwei Strömungen der Lebenskraft: Prana nimmt auf und gehört zum Einatmen, Apana lässt " +
                "los und gehört zum Ausatmen.",
        ),
        GlossaryTerm(
            "Ida · Pingala",
            "Zwei Hauptkanäle der Lebenskraft: Ida links, Mond, kühlend; Pingala rechts, Sonne, " +
                "wärmend. Die Wechselatmung soll sie ausgleichen.",
        ),
        GlossaryTerm(
            "Sama Vritti · Vishama Vritti",
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
