package app.atemkraft.domain

import kotlinx.serialization.Serializable

/**
 * Befinden nach einer Sitzung als Selbstauskunft (SECURITY SEC-LEGAL-04): relativ zu vorher, ein
 * Tipp am Ende-Screen, überspringbar. Gespeichert unter dem Namen am Logbuch-Eintrag (CODE-05) –
 * Konstanten deshalb nie umbenennen. Die App rechnet nie damit: keine Summe, kein Durchschnitt, kein
 * Trend, keine Empfehlung; sie zeigt nur das Wort am Eintrag.
 */
@Serializable
enum class Feeling {
    CALMER,
    SAME,
    RESTLESS,
    MORE_AWAKE,
    MORE_TIRED,
    ;

    companion object {
        /** Nach beruhigenden und ausgleichenden Übungen und der Meditation: „ruhiger · wie vorher · unruhiger“. */
        val CALM_SCALE: List<Feeling> = listOf(CALMER, SAME, RESTLESS)

        /** Nach aktivierenden Übungen: „wacher · wie vorher · müder“. */
        val ACTIVATING_SCALE: List<Feeling> = listOf(MORE_AWAKE, SAME, MORE_TIRED)

        /**
         * Die passende Reihe: aktivierend bei der Familie „Aktivieren“ und bei sanft aktivierenden
         * eigenen Mustern ([Exercise.activating]), sonst die ruhige (auch für die Meditation).
         */
        fun scaleFor(family: BreathingFamily?, activating: Boolean): List<Feeling> = if (activating || family == BreathingFamily.UPREGULATE) ACTIVATING_SCALE else CALM_SCALE

        /** Gespeicherter Name → Wert; unbekannt (z. B. aus einer neueren Version) = nicht beantwortet. */
        fun fromName(name: String?): Feeling? = entries.firstOrNull { it.name == name }
    }
}

/**
 * Ob der Ende-Screen nach dem Befinden fragt (Einstellung „Befinden am Ende fragen“). [UNSET]: noch
 * nie angeboten – beim ersten regulären Ende erscheint die Reihe mit einem Satz, dass man sie
 * abschalten kann, danach gilt [ON] (Nutzerentscheidung 2026-10-24: einmal angeboten, sonst Schalter).
 */
enum class FeelingPrompt { UNSET, ON, OFF }
