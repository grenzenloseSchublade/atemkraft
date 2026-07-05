package app.atemkraft.domain

/**
 * Atomare Schritt-Typen einer Atemübung.
 *
 * Deckt die gängigen Techniken ab: Box-Atmung und 4-7-8 brauchen [HOLD_FULL]/
 * [HOLD_EMPTY], Wim-Hof eine offene Retention (siehe [PhaseDuration]), der
 * physiologische Seufzer den gestapelten [INHALE_TOP_UP].
 */
enum class PhaseType {
    /** Einatmen. */
    INHALE,

    /** Ausatmen. */
    EXHALE,

    /** Halten bei voller Lunge (nach dem Einatmen). */
    HOLD_FULL,

    /** Halten bei leerer Lunge (nach dem Ausatmen). */
    HOLD_EMPTY,

    /** Zweiter, gestapelter Atemzug (z. B. physiologischer Seufzer). */
    INHALE_TOP_UP,

    /** Freies, lockeres Normalatmen – z. B. Ruhepause zwischen Feueratmungs-Runden. */
    REST,
}

/** Seite für Techniken mit Nasenwechsel (Nadi Shodhana). */
enum class BreathSide { NONE, LEFT, RIGHT }
