package app.atemkraft.domain

import kotlin.math.roundToInt

/**
 * Eine bereits aufgelöste Phase im Ablauf einer konkreten Session: alle Runden
 * und Wiederholungen sind ausmultipliziert, [perRound] ist angewandt. Der
 * BreathSessionController arbeitet nur noch auf dieser flachen Liste.
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
    /** Füllstand für Atmen in Stufen, siehe [Phase.level]; null = nach Phasentyp. */
    val level: BreathLevel? = null,
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
                            level = resolved.level,
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

/**
 * Grobe Gesamtdauer der Übung in Sekunden: Summe aller festen Phasen-Dauern über die
 * ausmultiplizierte Timeline. Offene Phasen ([PhaseDuration.OpenEnded]/[UntilUrge], z. B.
 * Wim-Hof-Retention) haben keine feste Dauer und fließen nicht ein – die Rückgabe ist
 * daher ein Näherungswert (Untergrenze), erkennbar an [hasOpenPhases].
 */
fun Exercise.estimatedTotalSeconds(): Int {
    val millis = buildTimeline().sumOf { phase ->
        (phase.duration as? PhaseDuration.Fixed)?.millis ?: 0L
    }
    return (millis / 1000L).toInt()
}

/** true, wenn die Übung offene Phasen enthält (Gesamtdauer dann nur Näherung/Untergrenze). */
val Exercise.hasOpenPhases: Boolean
    get() = segments.any { seg -> seg.phases.any { it.duration !is PhaseDuration.Fixed } }

/** Dauer eines Zyklus (eine Segment-Wiederholung) in Sekunden, aus festen Phasen. */
fun Exercise.cycleSeconds(): Int = (cycleMillis() / 1000L).toInt().coerceAtLeast(1)

/**
 * Exakte Zyklusdauer des ersten Segments in Millisekunden (mindestens 1 s). Grundlage für Minuten
 * und Wiederholungen: Abgeschnitten auf ganze Sekunden liefe ein 11,5-s-Zyklus sonst rund 4 %
 * länger als gewählt (gespeicherte Muster können 0,5-s-Phasen haben).
 */
fun Exercise.cycleMillis(): Long {
    val segment = segments.firstOrNull() ?: return 1000L
    val millis = segment.phases.sumOf { phase ->
        (phase.duration as? PhaseDuration.Fixed)?.millis ?: 0L
    }
    return millis.coerceAtLeast(1000L)
}

/** Vorgabe-Minuten einer kontinuierlichen Übung (aus repeat × Zyklusdauer, gerundet). */
fun Exercise.defaultMinutes(): Int {
    val segment = segments.firstOrNull() ?: return 5
    return (segment.repeat * cycleMillis() / 60_000.0).roundToInt().coerceAtLeast(1)
}

/**
 * Vorgaben einer Übung für die Anpassung auf der Detailseite: Runden bzw. Minuten und die
 * Sekunden der festen Phasen. null = für diese Phase gibt es keinen Stepper – die Übung hat sie
 * nicht mit fester Dauer, oder ihre Phasen sind nicht einzeln anpassbar ([phasesAdjustable]).
 * Liegt neben [adjusted], damit Anzeige und Anwendung dieselben Sekunden je Typ sehen
 * ([fixedMillisOf]).
 */
data class AdjustDefaults(
    val roundBased: Boolean,
    val duration: Int,
    val inhale: Int?,
    val hold: Int?,
    val exhale: Int?,
) {
    /** Grenzen des Dauer-Steppers: Runden oder Minuten. */
    val durationRange: IntRange get() = if (roundBased) AdjustLimits.ROUNDS else AdjustLimits.MINUTES

    companion object {
        fun of(exercise: Exercise): AdjustDefaults {
            fun seconds(type: PhaseType): Int? = exercise.fixedMillisOf(type)
                ?.takeIf { exercise.phasesAdjustable }
                ?.let { (it / 1000.0).roundToInt() }
            return AdjustDefaults(
                roundBased = exercise.isRoundBased,
                duration = if (exercise.isRoundBased) exercise.rounds else exercise.defaultMinutes(),
                inhale = seconds(PhaseType.INHALE),
                hold = seconds(PhaseType.HOLD_FULL) ?: seconds(PhaseType.HOLD_EMPTY),
                exhale = seconds(PhaseType.EXHALE),
            )
        }
    }
}

/**
 * Lassen sich die Phasenlängen einzeln anpassen (Stepper „Einatmen“, „Halten“, „Ausatmen“)? Nein
 * bei Atmen in Stufen ([Phase.level], Dirgha, Viloma: mehrere kurze Einatem- und Pausen-Phasen),
 * bei Phasen unter 1 s (Feueratmung: 0,5 s würde zum kleinsten Stepper-Schritt 1 s) und wenn
 * Phasen eines Typs verschieden lang sind. Ein Stepper zeigte dort nur die erste Phase eines Typs,
 * [adjusted] überschriebe aber alle – „Einatmen 2 s“ plus ein Schritt verlängerte das Einatmen um
 * 3 s, Pausen erschienen als „Halten voll“. Dann bleibt nur die Dauer bzw. die Rundenzahl.
 */
val Exercise.phasesAdjustable: Boolean
    get() {
        val phases = segments.flatMap { it.phases }
        if (phases.any { it.level != null }) return false
        if (phases.any { (it.duration as? PhaseDuration.Fixed)?.millis?.let { ms -> ms < 1000L } == true }) return false
        return ADJUSTABLE_TYPES.all { type ->
            phases.filter { it.type == type }.mapNotNull { (it.duration as? PhaseDuration.Fixed)?.millis }.distinct().size <= 1
        }
    }

/** Phasentypen mit eigenem Stepper (Halten leer folgt dem Halten-Stepper, siehe [adjusted]). */
private val ADJUSTABLE_TYPES = listOf(PhaseType.INHALE, PhaseType.HOLD_FULL, PhaseType.HOLD_EMPTY, PhaseType.EXHALE)

/**
 * Wendet die [SessionConfig] an: rundenbasiert → Rundenzahl überschreiben;
 * kontinuierlich → Segment-Wiederholung aus den gewünschten Minuten berechnen. Phasenlängen nur bei
 * [phasesAdjustable] – eine alte gespeicherte Anpassung einer Stufen-Übung bleibt so wirkungslos.
 * Runden und Minuten bleiben in den Grenzen der Stepper ([AdjustLimits]).
 */
fun Exercise.adjusted(config: SessionConfig): Exercise {
    // 1) Optionale Phasenlängen überschreiben (nur feste Dauern).
    val hasOverrides = config.inhaleSeconds != null ||
        config.exhaleSeconds != null || config.holdSeconds != null
    val base = if (!hasOverrides || !phasesAdjustable) {
        this
    } else {
        // Halten leer folgt dem Halten-Stepper nur, wenn es so lang ist wie Halten voll (Box) oder
        // allein steht. Sind beide verschieden (Tagesmuster: voll 2–3 s, leer 1–2 s), bleibt das
        // kurze Halten nach dem Ausatmen bei seiner Vorgabe – sonst würde es mitwachsen und seine
        // Grenze (≤ 2 s) verlassen.
        val holdFullMs = fixedMillisOf(PhaseType.HOLD_FULL)
        val holdEmptyMs = fixedMillisOf(PhaseType.HOLD_EMPTY)
        val holdEmptyFollows = holdFullMs == null || holdFullMs == holdEmptyMs
        copy(
            segments = segments.map { segment ->
                segment.copy(
                    phases = segment.phases.map { phase ->
                        val secs = when (phase.type) {
                            PhaseType.INHALE -> config.inhaleSeconds
                            PhaseType.EXHALE -> config.exhaleSeconds
                            PhaseType.HOLD_FULL -> config.holdSeconds
                            PhaseType.HOLD_EMPTY -> config.holdSeconds.takeIf { holdEmptyFollows }
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
    }

    // 2) Runden (rundenbasiert) bzw. Minuten → Wiederholungen aus dem neuen Zyklus.
    if (base.isRoundBased) {
        val chosen = (config.rounds ?: base.rounds).coerceIn(AdjustLimits.ROUNDS)
        return base.copy(rounds = chosen)
    }
    val segment = base.segments.firstOrNull() ?: return base
    val minutes = (config.minutes ?: base.defaultMinutes()).coerceIn(AdjustLimits.MINUTES)
    val repeat = (minutes * 60_000.0 / base.cycleMillis()).roundToInt().coerceAtLeast(1)
    return base.copy(segments = listOf(segment.copy(repeat = repeat)))
}

/**
 * Feste Dauer der Phasen vom Typ [type] über alle Segmente – die eine Quelle für die Sekunden je
 * Typ in [AdjustDefaults] und [adjusted]. null, wenn die Übung den Typ nicht mit fester Dauer hat;
 * sind die Phasen verschieden lang, gilt die erste (dann ist [phasesAdjustable] ohnehin false).
 */
private fun Exercise.fixedMillisOf(type: PhaseType): Long? = segments.asSequence()
    .flatMap { it.phases }
    .firstNotNullOfOrNull { phase -> (phase.duration as? PhaseDuration.Fixed)?.takeIf { phase.type == type }?.millis }
