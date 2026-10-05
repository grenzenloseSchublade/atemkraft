package app.atemkraft.domain

import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.random.Random

/** Die nackten Parameter eines generierten Musters (Sekunden) – persistierbar. */
data class PatternSpec(
    val inhale: Double,
    val holdFull: Double?,
    val exhale: Double,
    val holdEmpty: Double?,
    val activating: Boolean,
)

/**
 * Liegt das Muster in den Leitplanken des Generators (siehe [RandomPatternGenerator])? Eine
 * Quelle für Generator-Test und Import: Von außen kommende Muster außerhalb dieser Grenzen
 * werden nicht übernommen, weil ihre Beschreibung („Halten ≤ 4 s" …) sonst falsch wäre und sie
 * ohne Sicherheitsabfrage starten.
 */
fun PatternSpec.withinGuardrails(): Boolean {
    val phases = listOfNotNull(inhale, holdFull, exhale, holdEmpty)
    if (phases.any { !it.isFinite() || it < 0.0 || it * 2 != Math.rint(it * 2) }) return false
    if (inhale < 2.0 || exhale < 2.0 || exhale > 8.0) return false
    val cycle = phases.sum()
    return if (activating) {
        holdEmpty == null && (holdFull ?: 0.0) <= 2.0 && exhale <= inhale && cycle in 6.0..9.0
    } else {
        exhale >= inhale && (holdFull ?: 0.0) <= 4.0 && (holdEmpty ?: 0.0) <= 2.0 && cycle in 8.5..13.0
    }
}

/** Das Tagesmuster: die generierte Übung + ihre Parameter (für Chip + Speichern). */
data class DailyPattern(val exercise: Exercise, val spec: PatternSpec) {
    val activating: Boolean get() = spec.activating
}

/**
 * Erzeugt das „Muster des Tages": ein zufälliges, aber physiologisch plausibles Atemmuster,
 * **deterministisch aus dem Datum** (gleicher Tag ⇒ gleiches Muster). Bewusst als spielerische
 * Abwechslung positioniert – ohne Wirkversprechen (die Studienlage favorisiert für regelmäßiges
 * Üben EIN konstantes langsames Muster, siehe effectDetail-Text).
 *
 * Leitplanken (Russo 2017, Zaccaro 2018, Pranayama-Konventionen):
 * - ruhig: Zyklus 8,5–13 s (≈ 4,6–7 Atemzüge/min), Ausatmen ≥ Einatmen, Halten ≤ 4 s
 *   (nach dem Ausatmen ≤ 2 s);
 * - sanft aktivierend: Einatmen-betont, Zyklus 6–9 s, Halten ≤ 2 s – nie Hyperventilation.
 */
object RandomPatternGenerator {

    const val ID = "muster-des-tages"

    fun forDate(date: LocalDate): DailyPattern = forSeed(date.toEpochDay())

    /** Muster aus beliebigem Seed – für „Neu generieren". */
    fun forSeed(seed: Long): DailyPattern {
        val random = Random(seed)
        return when (random.nextInt(100)) {
            in 0 until 19 -> calmNoHold(random)
            in 19 until 38 -> calmHoldFull(random)
            in 38 until 57 -> calm478Like(random)
            in 57 until 75 -> calmSoftBox(random)
            else -> gentlyActivating(random) // ~25 %
        }
    }

    // ---- Stile (alle Werte in Sekunden, 0,5-s-Raster) ----

    private fun calmNoHold(r: Random): DailyPattern {
        val inhale = r.halfSteps(3.5, 5.5)
        var exhale = (inhale * r.ratio(1.1, 1.6)).toHalfSteps().coerceIn(inhale, 8.0)
        exhale = fitCalmCycle(inhale, exhale, holds = 0.0)
        return build(inhale, null, exhale, null, activating = false)
    }

    private fun calmHoldFull(r: Random): DailyPattern {
        val inhale = r.halfSteps(3.5, 5.0)
        // Halten so kappen, dass selbst mit minimalem Ausatmen (= Einatmen) Zyklus <= 13 s bleibt.
        val hold = r.halfSteps(1.0, minOf(4.0, CALM_CYCLE_MAX - 2 * inhale))
        var exhale = (inhale * r.ratio(1.1, 1.6)).toHalfSteps().coerceIn(inhale, 8.0)
        exhale = fitCalmCycle(inhale, exhale, holds = hold)
        return build(inhale, hold, exhale, null, activating = false)
    }

    private fun calm478Like(r: Random): DailyPattern {
        val inhale = r.halfSteps(3.0, 4.0)
        val hold = r.halfSteps(3.0, 4.0)
        var exhale = (inhale * r.ratio(1.7, 2.0)).toHalfSteps().coerceIn(inhale, 8.0)
        exhale = fitCalmCycle(inhale, exhale, holds = hold)
        return build(inhale, hold, exhale, null, activating = false)
    }

    private fun calmSoftBox(r: Random): DailyPattern {
        // Enge Grenzen, damit auch der Maximal-Wurf (Ein + H + Aus(=Ein+1) + H) <= 13 s bleibt.
        val inhale = r.halfSteps(3.5, 4.0)
        val holdFull = r.halfSteps(2.0, 2.5)
        val holdEmpty = r.halfSteps(1.0, 1.5)
        var exhale = r.halfSteps(inhale, inhale + 1.0)
        exhale = fitCalmCycle(inhale, exhale, holds = holdFull + holdEmpty)
        return build(inhale, holdFull, exhale, holdEmpty, activating = false)
    }

    private fun gentlyActivating(r: Random): DailyPattern {
        val inhale = r.halfSteps(3.0, 4.5)
        val hold = if (r.nextBoolean()) r.halfSteps(0.5, 2.0) else 0.0
        var exhale = r.halfSteps(2.5, inhale)
        // Zyklus sanft in 6–9 s halten (Ausatmen innerhalb seiner Grenzen anpassen).
        while (inhale + hold + exhale < 6.0 && exhale < inhale) exhale += 0.5
        while (inhale + hold + exhale > 9.0 && exhale > 2.5) exhale -= 0.5
        return build(inhale, hold.takeIf { it > 0.0 }, exhale, null, activating = true)
    }

    /** Ausatmen so nachjustieren, dass der ruhige Zyklus in 8,5–13 s liegt (Aus ≥ Ein bleibt). */
    private fun fitCalmCycle(inhale: Double, exhaleStart: Double, holds: Double): Double {
        var exhale = exhaleStart
        while (inhale + holds + exhale < CALM_CYCLE_MIN && exhale < 8.0) exhale += 0.5
        while (inhale + holds + exhale > CALM_CYCLE_MAX && exhale > inhale) exhale -= 0.5
        return exhale
    }

    // ---- Exercise-Bau ----

    private fun build(
        inhale: Double,
        holdFull: Double?,
        exhale: Double,
        holdEmpty: Double?,
        activating: Boolean,
    ): DailyPattern {
        val spec = PatternSpec(inhale, holdFull, exhale, holdEmpty, activating)
        return DailyPattern(exerciseFrom(spec, ID, "Tagesmuster ${numbersOf(spec)}"), spec)
    }

    /** Muster-Kurzform, z. B. „4·2·6" – auch für Namen gespeicherter Muster. */
    fun numbersOf(spec: PatternSpec): String = listOfNotNull(spec.inhale, spec.holdFull, spec.exhale, spec.holdEmpty)
        .joinToString("·") { it.fmt() }

    /**
     * Muster in Worten, z. B. „Einatmen 4 s · Halten 2 s · Ausatmen 6 s". Zwischen Zahl und „s"
     * steht ein geschütztes Leerzeichen ([NBSP]): Bei großer Schrift bricht die Zeile sonst
     * zwischen „1" und „s" um (TEXT-10). Teile werden mit [HINT_SEPARATOR] verbunden.
     */
    fun hintFor(spec: PatternSpec): String = buildList {
        add("Einatmen ${spec.inhale.fmt()}${NBSP}s")
        spec.holdFull?.let { add("Halten ${it.fmt()}${NBSP}s") }
        add("Ausatmen ${spec.exhale.fmt()}${NBSP}s")
        spec.holdEmpty?.let { add("Halten ${it.fmt()}${NBSP}s") }
    }.joinToString(HINT_SEPARATOR)

    /**
     * Trenner zwischen Teilen einer Hinweiszeile („… 6,5 s · ca. 4 min“). Das geschützte
     * Leerzeichen vor dem Punkt hält ihn am Zeilenende – eine Zeile beginnt nie mit „·“.
     */
    const val HINT_SEPARATOR = "\u00A0· "

    /** Geschütztes Leerzeichen zwischen Zahl und Einheit. */
    private const val NBSP = '\u00A0'

    /** Baut aus [spec] eine lauffähige Übung – für das Tagesmuster UND gespeicherte Muster. */
    fun exerciseFrom(spec: PatternSpec, id: String, name: String): Exercise {
        val phases = buildList {
            add(Phase(PhaseType.INHALE, secs(spec.inhale)))
            spec.holdFull?.let { add(Phase(PhaseType.HOLD_FULL, secs(it))) }
            add(Phase(PhaseType.EXHALE, secs(spec.exhale)))
            spec.holdEmpty?.let { add(Phase(PhaseType.HOLD_EMPTY, secs(it))) }
        }
        val cycleSeconds = spec.inhale + (spec.holdFull ?: 0.0) + spec.exhale + (spec.holdEmpty ?: 0.0)
        val repeat = (TARGET_SECONDS / cycleSeconds).roundToInt().coerceAtLeast(1)
        val hint = hintFor(spec)
        val character = if (spec.activating) "sanft aktivierend" else "ruhig"
        // Atemort: ruhige Muster EMPFEHLEN konstant die Bauchatmung (fachlicher Standard);
        // bewusst NICHT randomisiert – und als Einladung formuliert, nicht als Muss.
        // Punkt nach der Phasenzeile: Sonst liest sich „Ausatmen 5 s Wenn du magst …" als ein Satz.
        val fullHint = if (spec.activating) hint else "$hint. $BELLY_GUIDANCE"
        val bellyNote = if (spec.activating) "" else " Bauchatmung empfohlen – ganz wie es angenehm ist."

        return Exercise(
            id = id,
            name = name,
            family = BreathingFamily.BALANCE,
            shortDescription = "$hint — heute $character.$bellyNote Jeden Tag ein neues, " +
                "zufällig erzeugtes Muster in physiologisch üblichen Grenzen.",
            effect = "Spielerische Abwechslung: ein generiertes Muster zum Ausprobieren – " +
                "ohne spezifisches Wirkversprechen.",
            effectDetail = "Das Muster entsteht deterministisch aus dem Datum: Stil und " +
                "Phasenlängen werden zufällig gewählt, aber in Grenzen gehalten, die der " +
                "Forschung zu langsamem Atmen entsprechen (ruhig: 4,6–7 Atemzüge/min, " +
                "Ausatmen ≥ Einatmen, Halten ≤ 4 s; aktivierend: zügiger, nie forciert). " +
                "Für regelmäßiges Üben ist ein konstantes Muster – etwa die Resonanz-Atmung – " +
                "die besser belegte Wahl; das Tagesmuster ist die Einladung, Neues zu entdecken.",
            segments = listOf(Segment(phases = phases, repeat = repeat)),
            rounds = 1,
            instructionHint = fullHint,
        )
    }

    // ---- Helfer ----

    private fun secs(value: Double) = PhaseDuration.Fixed((value * 1000).toLong())

    /** Zufallswert im 0,5-s-Raster (inklusive Grenzen). */
    private fun Random.halfSteps(min: Double, max: Double): Double {
        val lo = (min * 2).roundToInt()
        val hi = (max * 2).roundToInt()
        return nextInt(lo, hi + 1) / 2.0
    }

    private fun Random.ratio(min: Double, max: Double): Double = min + nextDouble() * (max - min)

    private fun Double.toHalfSteps(): Double = (this * 2).roundToInt() / 2.0

    /** „4" bzw. „4,5" (deutsches Komma, ohne unnötige Null). */
    private fun Double.fmt(): String = if (this % 1.0 == 0.0) toInt().toString() else toString().replace('.', ',')

    /**
     * Bauchatmungs-EMPFEHLUNG der ruhigen Muster (kein Zufall beim Atemort) – bewusst als
     * Einladung formuliert, nicht als Anweisung (Angebot, kein Muss).
     */
    private const val BELLY_GUIDANCE =
        "Wenn du magst, atme dabei in den Bauch – die Bauchdecke hebt und senkt sich sanft."

    /** Ziel-Vorgabedauer ≈ 5 min (Nutzer regelt im Detail 1–30 min). */
    private const val TARGET_SECONDS = 300.0
    private const val CALM_CYCLE_MIN = 8.5
    private const val CALM_CYCLE_MAX = 13.0
}
