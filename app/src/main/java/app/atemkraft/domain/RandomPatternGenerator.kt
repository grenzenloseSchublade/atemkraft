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
 *
 * Bewusst weiter als der heutige Generator: 0,5-s-Werte und aktivierende Muster mit gleich
 * langem Ein- und Ausatmen erzeugte er bis Version 1.6.2; gespeicherte und exportierte Muster
 * aus dieser Zeit bleiben gültig.
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
 * - ruhig: Zyklus 9–13 s (≈ 4,6–6,7 Atemzüge/min), Ausatmen ≥ Einatmen, Halten ≤ 4 s
 *   (nach dem Ausatmen ≤ 2 s);
 * - sanft aktivierend: Einatmen länger als Ausatmen (Brmhana der Atemlehre), Zyklus 6–9 s,
 *   Halten ≤ 2 s – nie Hyperventilation.
 *
 * Alle Phasen in ganzen Sekunden: So zählt man mit, der Countdown der Session stimmt mit der
 * Angabe überein, „Intervalle anpassen“ (ganze Sekunden) zeigt dieselben Werte, und die
 * Verhältnisse sind ganzzahlig wie in der Atemlehre (4 : 6, 4 : 8). Halbe Sekunden spürt man
 * beim Atmen kaum, und für die Studienlage zählt das Tempo, nicht das genaue Verhältnis.
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

    // ---- Stile (alle Werte in ganzen Sekunden) ----

    private fun calmNoHold(r: Random): DailyPattern {
        val inhale = r.seconds(3, 5)
        val exhale = fitCalmCycle(inhale, r.exhaleFor(inhale, 1.0, 2.0), holds = 0)
        return build(inhale, null, exhale, null, activating = false)
    }

    private fun calmHoldFull(r: Random): DailyPattern {
        val inhale = r.seconds(3, 5)
        // Halten so kappen, dass selbst mit minimalem Ausatmen (= Einatmen) Zyklus <= 13 s bleibt.
        val hold = r.seconds(1, minOf(4, CALM_CYCLE_MAX - 2 * inhale))
        val exhale = fitCalmCycle(inhale, r.exhaleFor(inhale, 1.2, 2.0), holds = hold)
        return build(inhale, hold, exhale, null, activating = false)
    }

    /** Nach 4-7-8: kurzes Einatmen, Halten, doppelt so langes Ausatmen – in 13 s nur mit 3 s Einatmen. */
    private fun calm478Like(r: Random): DailyPattern {
        val inhale = 3
        val hold = r.seconds(2, 4)
        val exhale = fitCalmCycle(inhale, 2 * inhale, holds = hold)
        return build(inhale, hold, exhale, null, activating = false)
    }

    private fun calmSoftBox(r: Random): DailyPattern {
        // Enge Grenzen, damit auch der Maximal-Wurf (4 + 3 + 5 + 2) mit dem Nachjustieren <= 13 s bleibt.
        val inhale = r.seconds(3, 4)
        val holdFull = r.seconds(2, 3)
        val holdEmpty = r.seconds(1, 2)
        val exhale = fitCalmCycle(inhale, r.seconds(inhale, inhale + 1), holds = holdFull + holdEmpty)
        return build(inhale, holdFull, exhale, holdEmpty, activating = false)
    }

    private fun gentlyActivating(r: Random): DailyPattern {
        val inhale = r.seconds(3, 5)
        // Ausatmen immer kürzer als Einatmen (Brmhana), aber nie unter 2 s.
        var exhale = r.seconds(maxOf(ACTIVE_EXHALE_MIN, inhale - 2), inhale - 1)
        var hold = if (r.nextBoolean()) r.seconds(1, 2) else 0
        // Zyklus sanft in 6–9 s halten: erst über das Halten, dann über das Ausatmen.
        while (inhale + hold + exhale < ACTIVE_CYCLE_MIN && hold < 2) hold++
        while (inhale + hold + exhale < ACTIVE_CYCLE_MIN && exhale < inhale - 1) exhale++
        while (inhale + hold + exhale > ACTIVE_CYCLE_MAX && hold > 0) hold--
        while (inhale + hold + exhale > ACTIVE_CYCLE_MAX && exhale > ACTIVE_EXHALE_MIN) exhale--
        return build(inhale, hold.takeIf { it > 0 }, exhale, null, activating = true)
    }

    /** Ausatmen so nachjustieren, dass der ruhige Zyklus in 9–13 s liegt (Aus ≥ Ein bleibt). */
    private fun fitCalmCycle(inhale: Int, exhaleStart: Int, holds: Int): Int {
        var exhale = exhaleStart.coerceIn(inhale, CALM_EXHALE_MAX)
        while (inhale + holds + exhale < CALM_CYCLE_MIN && exhale < CALM_EXHALE_MAX) exhale++
        while (inhale + holds + exhale > CALM_CYCLE_MAX && exhale > inhale) exhale--
        return exhale
    }

    // ---- Exercise-Bau ----

    private fun build(
        inhale: Int,
        holdFull: Int?,
        exhale: Int,
        holdEmpty: Int?,
        activating: Boolean,
    ): DailyPattern {
        val spec = PatternSpec(inhale.toDouble(), holdFull?.toDouble(), exhale.toDouble(), holdEmpty?.toDouble(), activating)
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
                "Forschung zu langsamem Atmen entsprechen (ruhig: 4,6–6,7 Atemzüge/min, " +
                "Ausatmen ≥ Einatmen, Halten ≤ 4 s; aktivierend: Einatmen länger, nie forciert). " +
                "Für regelmäßiges Üben ist ein konstantes Muster – etwa die Resonanz-Atmung – " +
                "die besser belegte Wahl; das Tagesmuster ist die Einladung, Neues zu entdecken.",
            segments = listOf(Segment(phases = phases, repeat = repeat)),
            rounds = 1,
            instructionHint = fullHint,
            lore = loreFor(spec),
        )
    }

    /**
     * Einordnung nach der Atemlehre: folgt nur dem Verhältnis der Phasen, nicht einer Quelle.
     * Die Kennungen sind die Muster-Karten der Atemlehre (Datenschicht, hier nur als String);
     * ein Halten verweist zusätzlich auf seine Karte.
     */
    private fun loreFor(spec: PatternSpec): YogaLore {
        val (patternId, teaser) = when {
            spec.exhale < spec.inhale ->
                "brmhana" to
                    "Längeres Einatmen gilt in der Yoga-Lehre als Brmhana: belebend und wärmend."

            spec.exhale > spec.inhale ->
                "langhana" to
                    "Längeres Ausatmen gilt in der Yoga-Lehre als Langhana: beruhigend und kühlend."

            // Auch aktivierende Muster mit gleich langem Ein- und Ausatmen (gespeichert bis 1.6.2):
            // Das Verhältnis ist Samana, das Tempo allein macht es nach der Lehre nicht zu Brmhana.
            else ->
                "samana" to
                    "Gleich langes Ein- und Ausatmen gilt in der Yoga-Lehre als Samana: ausgleichend."
        }
        val detail = buildString {
            append("Das Muster ist zufällig erzeugt; die Einordnung folgt nur dem Verhältnis der Phasen.")
            if (spec.holdFull != null) {
                append(
                    if (patternId == "brmhana") {
                        " Das Halten nach dem Einatmen verstärkt nach der Lehre das Belebende."
                    } else {
                        " Das Halten nach dem Einatmen gilt in der Lehre als sammelnd und verstärkt eher das " +
                            "Belebende; das Muster mischt also beides."
                    },
                )
            }
            if (spec.holdEmpty != null) {
                append(" Das Halten nach dem Ausatmen gilt als Weg in die Stille und verstärkt das Beruhigende.")
            }
        }
        val holds = listOfNotNull(
            "halten-voll".takeIf { spec.holdFull != null },
            "halten-leer".takeIf { spec.holdEmpty != null },
        )
        return YogaLore(teaser = teaser, detail = detail, patternId = patternId, morePatternIds = holds)
    }

    // ---- Helfer ----

    private fun secs(value: Double) = PhaseDuration.Fixed((value * 1000).toLong())

    /** Ganze Sekunden zwischen [min] und [max] (inklusive). */
    private fun Random.seconds(min: Int, max: Int): Int = nextInt(min, max + 1)

    /** Ausatmen als Vielfaches des Einatmens (Verhältnis zufällig in [min]..[max]), gerundet. */
    private fun Random.exhaleFor(inhale: Int, min: Double, max: Double): Int = (inhale * (min + nextDouble() * (max - min))).roundToInt()

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
    private const val CALM_CYCLE_MIN = 9
    private const val CALM_CYCLE_MAX = 13
    private const val CALM_EXHALE_MAX = 8
    private const val ACTIVE_CYCLE_MIN = 6
    private const val ACTIVE_CYCLE_MAX = 9
    private const val ACTIVE_EXHALE_MIN = 2
}
