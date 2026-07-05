package app.atemkraft.domain

/** Ton-Modus der Session. */
enum class SoundMode {
    /** Kein Ton. */
    OFF,

    /** Kurze Töne bei jedem Phasenwechsel. */
    CUES,

    /** Durchgehender Sinuston, dessen Tonhöhe der Phase folgt (kein Wechsel-Beep). */
    CONTINUOUS,
}
