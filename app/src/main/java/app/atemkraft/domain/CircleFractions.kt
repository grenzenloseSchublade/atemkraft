package app.atemkraft.domain

/**
 * Füllstand des Atemkreises je Phase (0 = ausgeatmet, 1 = eingeatmet), stetig über die
 * Phasengrenzen (MOTION-03, Backlog S-11): Jede Phase beginnt, wo die vorige endete. Vorher sprang
 * der Kreis, wo der Phasentyp allein den Start bestimmte – Vorbereitung (halb) → Einatmen (leer),
 * Einatmen (voll) → Nachatmen beim Seufzer (leer), Pause der Feueratmung (halb) → Ausatmen (voll),
 * Wim Hof „locker“ (leer) → Ausatmen „ganz“ (voll), Halten voll → Einatmen (leer).
 *
 * Regeln: Die erste Phase beginnt an ihrem natürlichen Start ([natural]); die Vorbereitung zeigt
 * genau diesen Stand. Einatmen endet voll, Ausatmen leer – außer es folgt dieselbe Bewegung
 * (Einatmen → Nachatmen, Ausatmen → Ausatmen „ganz“): Dann endet die erste vor dem Anschlag
 * ([PARTIAL_FULL], [PARTIAL_EMPTY]), damit die zweite noch sichtbar wächst bzw. schrumpft. Halten
 * steht, die Pause gleitet zur Mitte ([REST]). Offene Phasen (Halten bis zum Atemimpuls) stehen,
 * weil ihr Fortschritt nie läuft. Atmen in Stufen ([Phase.level]) behält seine Füllstände aus den
 * Daten.
 */
object CircleFractions {
    /** Ende eines Einatmens, auf das ein Nachatmen folgt (Seufzer). */
    const val PARTIAL_FULL = 0.8f

    /** Ende eines Ausatmens, auf das ein weiteres Ausatmen folgt (Wim Hof „ganz“). */
    const val PARTIAL_EMPTY = 0.2f

    /** Ziel einer Pause (Feueratmung zwischen den Runden). */
    const val REST = 0.5f

    /** Füllstand, an dem eine Phase ohne Vorgänger beginnt. */
    fun natural(type: PhaseType): Float = when (type) {
        PhaseType.INHALE, PhaseType.INHALE_TOP_UP, PhaseType.HOLD_EMPTY -> 0f
        PhaseType.EXHALE, PhaseType.HOLD_FULL -> 1f
        PhaseType.REST -> REST
    }

    /** Setzt [RuntimePhase.circle] für jede Phase des Ablaufs. */
    fun assign(timeline: List<RuntimePhase>): List<RuntimePhase> {
        var previousEnd: Float? = null
        return timeline.mapIndexed { index, phase ->
            val circle = phase.level ?: run {
                val from = previousEnd ?: natural(phase.type)
                val to = if (phase.duration is PhaseDuration.Fixed) target(phase.type, timeline.getOrNull(index + 1)?.type, from) else from
                BreathLevel(from, to)
            }
            previousEnd = circle.to
            phase.copy(circle = circle)
        }
    }

    private fun target(type: PhaseType, next: PhaseType?, from: Float): Float = when (type) {
        PhaseType.INHALE -> if (next == PhaseType.INHALE_TOP_UP) PARTIAL_FULL else 1f
        PhaseType.INHALE_TOP_UP -> 1f
        PhaseType.EXHALE -> if (next == PhaseType.EXHALE) PARTIAL_EMPTY else 0f
        PhaseType.HOLD_FULL, PhaseType.HOLD_EMPTY -> from
        PhaseType.REST -> REST
    }
}
