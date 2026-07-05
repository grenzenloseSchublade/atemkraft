package app.atemkraft.domain

import kotlinx.serialization.Serializable

/**
 * Die vier Familien aus der Quelle (F3_Atmung): zwei Grundrichtungen plus zwei
 * ergänzende Gruppen. Bestimmt Sortierung, Farbe und Überschrift im Startscreen.
 */
@Serializable
enum class BreathingFamily {
    /** A · Herunterregeln (vagal / parasympathisch). */
    DOWNREGULATE,

    /** B · Hochregeln (sympathisch / energetisierend). */
    UPREGULATE,

    /** C · Balancieren / strukturierte Programme. */
    BALANCE,

    /** D · Funktionell – Atemmuster & CO₂-Toleranz. */
    FUNCTIONAL,
}

/** Optionaler Evidenz-/Sicherheits-Hinweis als Badge an der Übung. */
enum class EvidenceTag {
    /** Besonders gut belegt. */
    BEST_EVIDENCE,

    /** Nur in stabiler Phase / mit Vorsicht. */
    CAUTION,
}

/**
 * Eine Literaturquelle. [citation] ist der lesbare Kurznachweis,
 * [identifier] die maschinenlesbare Kennung (DOI/PMID/PMCID), beide aus der
 * verifizierten Recherche.
 */
data class Reference(
    val citation: String,
    val identifier: String? = null,
)
