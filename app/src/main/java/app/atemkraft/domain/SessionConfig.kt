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
