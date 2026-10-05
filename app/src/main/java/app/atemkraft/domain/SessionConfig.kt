package app.atemkraft.domain

/**
 * Vom Nutzer beim Start gewählte Anpassung. Genau eine Größe ist je nach Übung relevant:
 * [rounds] bei rundenbasierten, [minutes] bei kontinuierlichen Übungen. null = Default.
 */
data class SessionConfig(
    val rounds: Int? = null,
    val minutes: Int? = null,
    // Optionale Feineinstellung der Phasenlängen (Sekunden); null = Vorgabe der Übung.
    val inhaleSeconds: Double? = null,
    val exhaleSeconds: Double? = null,
    val holdSeconds: Double? = null,
)

/**
 * Erlaubte Bereiche der Session-Anpassung (Detailseite). Eine Quelle für die Stepper und für
 * alles, was gespeicherte Anpassungen von außen übernimmt (Import) – dort wird geklemmt bzw.
 * verworfen, damit nichts die Grenzen der Oberfläche umgeht.
 */
object AdjustLimits {
    val MINUTES = 1..30
    val ROUNDS = 1..15
    val INHALE_S = 2..12
    val HOLD_S = 1..20
    val EXHALE_S = 2..15
}
