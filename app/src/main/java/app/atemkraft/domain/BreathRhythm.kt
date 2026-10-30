package app.atemkraft.domain

/**
 * Ein Abschnitt der Rhythmuskurve: Phase [type] (Name [label] wie in der Session, sonst nach Typ),
 * [millis] lang, Füllstand der Lunge von [level] `from` bis `to` (0 = leer, 1 = voll).
 */
data class BreathStep(val type: PhaseType, val label: String?, val millis: Long, val level: BreathLevel) {
    /**
     * Obere Seite (Einatem-Farbe)? Einatmen und Nachatmen immer, Ausatmen nie, ein Halten ab halb
     * voller Lunge – dieselbe Regel wie in der Kurve des Muster-Editors ([FreePattern.isUpperLevel]).
     */
    val upper: Boolean
        get() = when (type) {
            PhaseType.INHALE, PhaseType.INHALE_TOP_UP -> true
            PhaseType.EXHALE -> false
            else -> FreePattern.isUpperLevel(level.from)
        }

    /** Halten (gestrichelt): Füllstand bleibt stehen. */
    val hold: Boolean get() = type == PhaseType.HOLD_FULL || type == PhaseType.HOLD_EMPTY
}

/**
 * Ein Atemzug einer Übung als Kurve für die Detailseite ([breathRhythm]): [steps] in ihrer
 * Reihenfolge, Füllstände stetig wie beim Atemkreis ([CircleFractions]). [firstPartOnly]: Die
 * Übung hat je Runde mehrere Teile (Wim Hof: Atemzüge, dann Halten bis zum Atemreiz); gezeigt
 * ist nur die Atemfolge, mit der jede Runde beginnt.
 */
data class BreathRhythm(val steps: List<BreathStep>, val firstPartOnly: Boolean = false) {
    /** Dauer des Atemzugs in Millisekunden. */
    val cycleMillis: Long = steps.sumOf { it.millis }

    /** Füllstand nach [millis] ab Beginn des Atemzugs (läuft im Kreis), linear je Abschnitt wie die Kurve. */
    fun levelAt(millis: Long): Float {
        if (cycleMillis <= 0L) return 0f
        var rest = millis.mod(cycleMillis)
        steps.forEach { step ->
            if (rest < step.millis) {
                val fraction = rest.toFloat() / step.millis
                return step.level.from + (step.level.to - step.level.from) * fraction
            }
            rest -= step.millis
        }
        return steps.last().level.to
    }

    /**
     * Gleiche Abschnitte direkt hintereinander zusammengefasst, mit ihrer Anzahl – für die Zeile
     * unter der Kurve („3 × Einatmen 2 s“ bei Dirgha statt dreimal dasselbe).
     */
    fun groups(): List<Pair<BreathStep, Int>> = buildList {
        steps.forEach { step ->
            val last = lastOrNull()
            if (last != null && last.first.sameAs(step)) set(lastIndex, last.first to last.second + 1) else add(step to 1)
        }
    }

    private fun BreathStep.sameAs(other: BreathStep) = type == other.type && label == other.label && millis == other.millis
}

/**
 * Ein Atemzug der Übung für die Rhythmuskurve oben auf der Detailseite – oder null, wo er nichts
 * Wahres zeigen würde (Nutzer 2026-10-30):
 * - Programme ohne Takt (`guided = false`): keine Kurve.
 * - Ein Teil je Runde: seine Phasen; zwei gleiche Hälften, die sich nur in der Seite
 *   unterscheiden (Nadi Shodhana), zählen einmal wie auf der Karte ([cardBreath]).
 * - Mehrere Teile je Runde (Wim Hof): nur der erste, [BreathRhythm.firstPartOnly] – auch bei einer
 *   gewählten Runde, die ebenso damit beginnt (sonst verschwände die Kurve beim Runden-Stepper).
 *   Kontinuierliche Übungen kürzt [adjusted] ohnehin auf ihren ersten Teil.
 * - Offene Phasen (Halten bis zum Tipp), Phasen unter 1 s (Feueratmung: ein Punkt, der hin und
 *   her zuckt, wäre nicht ruhig) und freies Atmen ([PhaseType.REST]): keine Kurve.
 *
 * Atmen in Stufen (Viloma, Dirgha) zeigt seine Stufen aus [Phase.level], Doppel-Einatmen (Seufzer)
 * steigt erst bis [CircleFractions.PARTIAL_FULL], dann bis voll – wie der Atemkreis.
 */
fun Exercise.breathRhythm(): BreathRhythm? {
    if (!guided) return null
    val first = segments.firstOrNull() ?: return null
    val firstPartOnly = segments.size > 1
    val phases = first.phases.oneHalfIfMirrored().map { perRound(it, 0) }
    if (phases.isEmpty()) return null
    if (phases.any { it.type == PhaseType.REST }) return null
    if (phases.any { (it.duration as? PhaseDuration.Fixed)?.millis?.let { ms -> ms < MIN_STEP_MILLIS } != false }) return null
    val timeline = CircleFractions.assign(
        phases.map { phase ->
            RuntimePhase(phase.type, phase.duration, phase.label, phase.note, phase.side, roundIndex = 0, roundCount = 1, level = phase.level)
        },
    )
    val steps = timeline.map { phase ->
        BreathStep(phase.type, phase.label, (phase.duration as PhaseDuration.Fixed).millis, phase.circle)
    }
    return BreathRhythm(steps, firstPartOnly)
}

/**
 * Ein Durchlauf der freien Folge als Atemzug für die kleinen Kurven (Mini-Kurve der Vorlagen und des
 * Musters des Tages): Füllstände aus [FreePattern.levels], Halten ab halb voll als „Halten voll“,
 * sonst „Halten leer“ – so gilt dieselbe Seite wie in der Kurve des Editors
 * ([FreePattern.isUpperSide] = [BreathStep.upper]).
 */
fun FreePattern.breathRhythm(): BreathRhythm {
    val levels = levels()
    return BreathRhythm(
        phases.mapIndexed { i, phase ->
            val type = when (phase.type) {
                FreePhaseType.IN -> PhaseType.INHALE
                FreePhaseType.OUT -> PhaseType.EXHALE
                FreePhaseType.HOLD -> if (FreePattern.isUpperLevel(levels[i].from)) PhaseType.HOLD_FULL else PhaseType.HOLD_EMPTY
            }
            BreathStep(type, null, phase.seconds * MILLIS_PER_SECOND, levels[i])
        },
    )
}

private const val MILLIS_PER_SECOND = 1000L

/** Kürzeste Phase, die die Kurve zeigt (1 s). */
private const val MIN_STEP_MILLIS = 1000L

/**
 * Die erste Hälfte, wenn die Folge aus zwei Hälften mit gleichen Typen und Dauern besteht, die sich
 * nur in der Seite unterscheiden (Nadi Shodhana: links ein, rechts aus, rechts ein, links aus);
 * sonst die Folge selbst. Gemeinsam für [cardBreath] und [breathRhythm].
 */
internal fun List<Phase>.oneHalfIfMirrored(): List<Phase> {
    val half = size / 2
    val halves = size % 2 == 0 && size >= 4 &&
        take(half).zip(drop(half)).all { (a, b) -> a.type == b.type && a.duration == b.duration }
    return if (halves) take(half) else this
}
