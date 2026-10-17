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
 * @param level Füllstand der Lunge von Anfang bis Ende der Phase, nur für Atmen in Stufen
 *   (Viloma): Einatmen 0 → ⅓, Halten bei ⅓ … Ohne Angabe gilt der Phasentyp (Einatmen leer →
 *   voll, Halten voll = voll …) – alle anderen Übungen bleiben, wie sie sind.
 */
data class Phase(
    val type: PhaseType,
    val duration: PhaseDuration,
    val label: String? = null,
    val note: String? = null,
    val side: BreathSide = BreathSide.NONE,
    val level: BreathLevel? = null,
)

/**
 * Füllstand der Lunge am Anfang ([from]) und Ende ([to]) einer Phase: 0 = ausgeatmet,
 * 1 = eingeatmet. Beim Halten sind beide gleich – der Atemkreis bleibt an dieser Stelle stehen.
 */
data class BreathLevel(val from: Float, val to: Float) {
    init {
        require(from in 0f..1f && to in 0f..1f) { "Füllstand außerhalb 0..1: $from → $to" }
    }

    companion object {
        /** Stillstand bei [level] (Pause mitten im Atemzug). */
        fun at(level: Float) = BreathLevel(level, level)
    }
}
