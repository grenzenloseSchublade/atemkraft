package app.atemkraft.domain

/**
 * Eine Lese-Karte im Tab „Lernen“: Grundlagen der Seite „Pranayama“, die Karten der Kapitel
 * „Geschichte des Atmens“ und „Körper“. [teaser] ist ein geschlossener Satz (≤ 95 Zeichen,
 * TEXT-06), [paragraphs] führen denselben Punkt aus und beginnen nicht mit dem Teaser; [source]
 * nennt die Quelle (Vers, Buch oder Aufsatz). [period] ist die Zeitangabe einer Geschichtskarte
 * über dem Titel („etwa 11.–15. Jahrhundert“). [exerciseIds] verweisen auf Übungen der App (je ein
 * Verweis-Chip), [seeAlso] ist ein Satz, der auf eine Seite der App zeigt („Siehe Seite
 * ‚Atemlehre‘ …“): Seitenlinks gibt es nicht, nur Übungs-Chips (Eigentümer 2026-10-25).
 * Sanskrit klein in IAST zwischen Sternchen (TEXT-14).
 */
data class LearnTopic(
    val id: String,
    val title: String,
    val teaser: String,
    val paragraphs: List<String>,
    val source: String? = null,
    val period: String? = null,
    val exerciseIds: List<String> = emptyList(),
    val seeAlso: String? = null,
)

/** Ein Abschnitt eines Lern-Kapitels: Kopf mit [title] und [teaser] (ganzer Satz), darunter [topics] als Karten. */
data class LearnSection(
    val id: String,
    val title: String,
    val teaser: String,
    val topics: List<LearnTopic>,
)
