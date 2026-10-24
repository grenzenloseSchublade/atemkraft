package app.atemkraft.domain

/**
 * Eine Technik im Praxisteil der Seite „Pranayama“: [name] wie in der App (Eigenname, aufrecht),
 * [sanskrit] in IAST zwischen Sternchen, [pronunciation] als deutsche Lesehilfe („udsch-dschá-jii“),
 * [teaser] ein geschlossener Satz (≤ ~95 Zeichen). Aufgeklappt: [tradition] (Herkunft, zuschreibend
 * formuliert), [howTo] (Anleitung), [studies] (ehrliche Einordnung mit Autor und Jahr), [safety].
 * [exerciseId] verweist auf eine Übung der App („Üben ›“); ohne sie ist die Technik nur erklärt
 * (z. B. Bandhas). [moreExerciseIds] sind weitere Übungen derselben Technik (Viloma II neben
 * Viloma I), je ein eigener Verweis. Die Studienlage kommt bei einer Technik mit Übung aus der
 * Übung (`Pranayama.evidenceOf`), damit Seite und Detailseite nie auseinanderlaufen; [evidence]
 * gilt nur für Techniken ohne Übung, null = nicht eingestuft.
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
    val moreExerciseIds: List<String> = emptyList(),
    val evidence: EvidenceLevel? = null,
)
