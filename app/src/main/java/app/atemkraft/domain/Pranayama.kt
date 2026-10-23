package app.atemkraft.domain

/**
 * Ein Grundlagen-Abschnitt der Seite „Pranayama“ im Tab „Lernen“ (Nutzer 2026-10-23: Yoga-Theorie
 * mit Praxis verbinden). [teaser] ist ein geschlossener Satz (≤ ~95 Zeichen, TEXT-Teaser-Regel),
 * [paragraphs] führen denselben Punkt aus; [source] nennt die Lehrquelle (Vers oder Buch).
 * Sanskrit klein in IAST zwischen Sternchen (TEXT-14).
 */
data class PranayamaTopic(
    val id: String,
    val title: String,
    val teaser: String,
    val paragraphs: List<String>,
    val source: String? = null,
)

/**
 * Eine Technik im Praxisteil der Seite „Pranayama“: [name] wie in der App (Eigenname, aufrecht),
 * [sanskrit] in IAST zwischen Sternchen, [pronunciation] als deutsche Lesehilfe („udsch-dschá-jii“),
 * [teaser] ein geschlossener Satz (≤ ~95 Zeichen). Aufgeklappt: [tradition] (Herkunft, zuschreibend
 * formuliert), [howTo] (Anleitung), [studies] (ehrliche Einordnung mit Autor und Jahr), [safety].
 * [exerciseId] verweist auf eine Übung der App („Üben ›“); ohne sie ist die Technik nur erklärt
 * (z. B. Bandhas). [evidence] wie auf der Detailseite, null = nicht eingestuft.
 */
data class PranayamaTechnique(
    val id: String,
    val name: String,
    val sanskrit: String,
    val teaser: String,
    val tradition: String,
    val howTo: String,
    val studies: String,
    val safety: String,
    val pronunciation: String? = null,
    val source: String? = null,
    val exerciseId: String? = null,
    val evidence: EvidenceLevel? = null,
)
