package app.atemkraft.domain

/** Wie deutlich der durchgehende Ton den Phasenwechsel betont (Lautstärke-Zäsur). */
enum class TransitionEmphasis {
    /** Kaum hörbar – fast nahtloses Gleiten. */
    SOFT,

    /** Mittlere, deutlich wahrnehmbare Zäsur. */
    MEDIUM,

    /** Tiefere, markantere Zäsur. */
    STRONG,
}
