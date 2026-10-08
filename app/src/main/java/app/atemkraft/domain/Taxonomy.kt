package app.atemkraft.domain

import kotlinx.serialization.Serializable

/**
 * Die vier Familien aus der Quelle (F3_Atmung): zwei Grundrichtungen plus zwei
 * ergänzende Gruppen. Bestimmt Sortierung, Farbe und Überschrift im Startscreen.
 */
@Serializable
enum class BreathingFamily {
    /** A · Herunterregeln (fachlich: vagal / parasympathisch). */
    DOWNREGULATE,

    /** B · Hochregeln (fachlich: sympathisch / energetisierend). */
    UPREGULATE,

    /** C · Balancieren / strukturierte Programme. */
    BALANCE,

    /** D · Funktionell – Atemmuster & CO₂-Toleranz. */
    FUNCTIONAL,
}

/** Art eines Logbuch-Eintrags: getaktete Atemübung oder stille Meditation. */
enum class SessionKind { BREATHING, MEDITATION }

/**
 * Studienlage einer eingebauten Übung (TEXT-07), sichtbar als Etikett auf Karte und Detailseite.
 * Jede eingebaute Übung hat genau eine Stufe; generierte Muster haben keine (`null`), weil sie
 * spielerische Variationen ohne eigene Studien sind. Die Stufe sagt, wie sicher das Wissen ist,
 * nicht wie stark eine Übung wirkt.
 */
enum class EvidenceLevel {
    /** „gut belegt“: Meta-Analyse oder mehrere unabhängige RCTs zur Anwendung. */
    WELL_SUPPORTED,

    /**
     * „in Studien geprüft“: mindestens ein RCT oder mehrere kleinere Studien, aber dünn, nicht
     * unabhängig wiederholt oder nur in bestimmten Gruppen.
     */
    STUDIED,

    /** „kaum untersucht“: Mechanismus plausibel, wenig direkte Studien. */
    LITTLE_STUDIED,
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
