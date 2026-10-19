package app.atemkraft.domain

/**
 * Bilanz einer abgeschlossenen Sitzung – nur echte Werte. [durationMs] ist die aktive Atemzeit
 * (Pausen zählen nicht, wie im Logbuch); [breaths] und [rounds] sind null, wenn die Übung sie
 * nicht sinnvoll hergibt (keine Runden bzw. keine gezählten Atemzüge).
 */
data class SessionSummary(val durationMs: Long, val breaths: Int?, val rounds: Int?)

/**
 * Fortschritt der Gesamtsitzung (0..1) aus der Position im Ablauf. Rundenbasiert zählt jede Runde
 * gleich viel (passt zu „Runde x von y“), innerhalb der Runde gewichtet nach festen Phasen-Dauern.
 * Bei einer Runde ist das genau verstrichene ÷ gesamte Zeit. Offene Phasen (Wim-Hof-Halt) haben
 * keine bekannte Dauer – dort steht der Fortschritt still; hat eine Runde gar keine feste Dauer,
 * zählt jede Phase gleich.
 */
internal class SessionProgress(timeline: List<RuntimePhase>) {
    private val roundCount = timeline.firstOrNull()?.roundCount?.coerceAtLeast(1) ?: 1
    private val roundOf = IntArray(timeline.size) { timeline[it].roundIndex.coerceIn(0, roundCount - 1) }
    private val fixedMs = LongArray(timeline.size) { (timeline[it].duration as? PhaseDuration.Fixed)?.millis ?: 0L }

    /** Feste Dauer vor der Phase innerhalb ihrer Runde bzw. ihre Position in der Runde. */
    private val msBefore = LongArray(timeline.size)
    private val phasesBefore = IntArray(timeline.size)
    private val roundMs = LongArray(roundCount)
    private val roundPhases = IntArray(roundCount)

    init {
        for (i in timeline.indices) {
            val r = roundOf[i]
            msBefore[i] = roundMs[r]
            phasesBefore[i] = roundPhases[r]
            roundMs[r] += fixedMs[i]
            roundPhases[r]++
        }
    }

    /** Fortschritt in Phase [index], nachdem [phaseElapsedMs] ihrer festen Dauer verstrichen sind. */
    fun at(index: Int, phaseElapsedMs: Long): Float {
        if (index !in roundOf.indices) return if (index < 0) 0f else 1f
        val r = roundOf[index]
        val inRound = if (roundMs[r] > 0L) {
            (msBefore[index] + phaseElapsedMs.coerceIn(0L, fixedMs[index])).toFloat() / roundMs[r]
        } else {
            phasesBefore[index].toFloat() / roundPhases[r]
        }
        return ((r + inRound) / roundCount).coerceIn(0f, 1f)
    }
}

/**
 * Gezählte Atemzüge: jedes Einatmen, das auf ein Ausatmen folgt (oder den Ablauf beginnt). Gestufte
 * Einatmung (Viloma I) und der Nachschub beim Seufzer bleiben so ein Atemzug; Halten und freies
 * Atmen ([PhaseType.REST]) zählen nicht.
 */
internal fun List<RuntimePhase>.breathCount(): Int {
    var count = 0
    var lastWasInhale = false
    for (phase in this) {
        when (phase.type) {
            PhaseType.INHALE -> {
                if (!lastWasInhale) count++
                lastWasInhale = true
            }

            PhaseType.EXHALE -> lastWasInhale = false

            else -> Unit
        }
    }
    return count
}

/** Bilanz eines vollständig durchlaufenen Ablaufs; Runden nur bei rundenbasierten Übungen. */
internal fun sessionSummary(timeline: List<RuntimePhase>, durationMs: Long): SessionSummary {
    val rounds = timeline.firstOrNull()?.roundCount ?: 0
    return SessionSummary(
        durationMs = durationMs.coerceAtLeast(0L),
        breaths = timeline.breathCount().takeIf { it > 0 },
        rounds = rounds.takeIf { it > 1 },
    )
}
