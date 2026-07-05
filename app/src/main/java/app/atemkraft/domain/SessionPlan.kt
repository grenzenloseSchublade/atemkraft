package app.atemkraft.domain

/**
 * Eine bereits aufgelöste Phase im Ablauf einer konkreten Session: alle Runden
 * und Wiederholungen sind ausmultipliziert, [perRound] ist angewandt. Der
 * SessionViewModel arbeitet nur noch auf dieser flachen Liste.
 */
data class RuntimePhase(
    val type: PhaseType,
    val duration: PhaseDuration,
    val label: String?,
    val note: String?,
    val side: BreathSide,
    /** 0-basierter Index der Runde, zu der diese Phase gehört. */
    val roundIndex: Int,
    /** Gesamtzahl der Runden (für Anzeige „Runde x von y"). */
    val roundCount: Int,
)

/**
 * Multipliziert eine [Exercise] in die flache, abspielbare Phasenliste aus.
 * Reihenfolge: Runde → Segment → Wiederholung → Phase.
 */
fun Exercise.buildTimeline(): List<RuntimePhase> {
    val timeline = ArrayList<RuntimePhase>()
    for (round in 0 until rounds) {
        for (segment in segments) {
            repeat(segment.repeat) {
                for (phase in segment.phases) {
                    val resolved = perRound(phase, round)
                    timeline.add(
                        RuntimePhase(
                            type = resolved.type,
                            duration = resolved.duration,
                            label = resolved.label,
                            note = resolved.note,
                            side = resolved.side,
                            roundIndex = round,
                            roundCount = rounds,
                        ),
                    )
                }
            }
        }
    }
    return timeline
}

/** Rundenbasiert = mehr als eine Runde; sonst kontinuierlich (über Minuten anpassbar). */
val Exercise.isRoundBased: Boolean get() = rounds > 1

/** Dauer eines Zyklus (eine Segment-Wiederholung) in Sekunden, aus festen Phasen. */
fun Exercise.cycleSeconds(): Int {
    val segment = segments.firstOrNull() ?: return 1
    val millis = segment.phases.sumOf { phase ->
        (phase.duration as? PhaseDuration.Fixed)?.millis ?: 0L
    }
    return (millis / 1000L).toInt().coerceAtLeast(1)
}

/** Vorgabe-Minuten einer kontinuierlichen Übung (aus repeat × Zyklusdauer). */
fun Exercise.defaultMinutes(): Int {
    val segment = segments.firstOrNull() ?: return 5
    return ((segment.repeat * cycleSeconds()) / 60).coerceAtLeast(1)
}

/**
 * Wendet die [SessionConfig] an: rundenbasiert → Rundenzahl überschreiben;
 * kontinuierlich → Segment-Wiederholung aus den gewünschten Minuten berechnen.
 */
fun Exercise.adjusted(config: SessionConfig): Exercise {
    // 1) Optionale Phasenlängen überschreiben (nur feste Dauern).
    val hasOverrides = config.inhaleSeconds != null ||
        config.exhaleSeconds != null || config.holdSeconds != null
    val base = if (!hasOverrides) this else copy(
        segments = segments.map { segment ->
            segment.copy(
                phases = segment.phases.map { phase ->
                    val secs = when (phase.type) {
                        PhaseType.INHALE -> config.inhaleSeconds
                        PhaseType.EXHALE -> config.exhaleSeconds
                        PhaseType.HOLD_FULL, PhaseType.HOLD_EMPTY -> config.holdSeconds
                        else -> null
                    }
                    if (secs != null && phase.duration is PhaseDuration.Fixed) {
                        phase.copy(duration = PhaseDuration.Fixed((secs * 1000).toLong()))
                    } else {
                        phase
                    }
                },
            )
        },
    )

    // 2) Runden (rundenbasiert) bzw. Minuten → Wiederholungen aus dem neuen Zyklus.
    if (base.isRoundBased) {
        val chosen = (config.rounds ?: base.rounds).coerceIn(1, 20)
        return base.copy(rounds = chosen)
    }
    val segment = base.segments.firstOrNull() ?: return base
    val minutes = (config.minutes ?: base.defaultMinutes()).coerceIn(1, 30)
    val repeat = ((minutes * 60) / base.cycleSeconds()).coerceAtLeast(1)
    return base.copy(segments = listOf(segment.copy(repeat = repeat)))
}
