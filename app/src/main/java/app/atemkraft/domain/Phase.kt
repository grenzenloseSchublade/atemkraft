package app.atemkraft.domain

/**
 * Eine einzelne Phase einer Übung.
 *
 * @param type Art der Phase (Einatmen, Halten, …).
 * @param duration Dauer bzw. Beendigungsart, siehe [PhaseDuration].
 * @param label Optionaler eigener Text fürs Hauptwort; ist er null, wird das Standard-Label
 *   aus [PhaseType] verwendet (in der UI lokalisiert aufgelöst).
 * @param note Optionale Zusatzinfo (z. B. „durch den Mund", „links", „summend"), die in der
 *   Session klein unter dem Hauptwort steht – nur wenn gesetzt.
 * @param side Optionale Seite für Nasenwechsel-Techniken.
 */
data class Phase(
    val type: PhaseType,
    val duration: PhaseDuration,
    val label: String? = null,
    val note: String? = null,
    val side: BreathSide = BreathSide.NONE,
)
