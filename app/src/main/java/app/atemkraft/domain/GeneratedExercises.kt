package app.atemkraft.domain

import kotlin.math.roundToInt

/**
 * Bau lauffähiger Übungen aus generierten und eigenen Mustern ([PatternSpec], [FreePattern]) und
 * der Rückweg von der Übung zum Muster (für den Editor). Die Texte kommen aus [PatternTexts], die
 * Einordnung in die Atemlehre aus [PatternLore].
 */
object GeneratedExercises {

    /**
     * Die Phasen einer generierten Übung ([exerciseFrom]) zurück als [PatternSpec] – etwa für den
     * Editor, damit er genau das angezeigte Muster übernimmt. null bei anderen Übungen.
     */
    fun specOf(exercise: Exercise): PatternSpec? {
        if (!exercise.generated) return null
        val phases = exercise.segments.singleOrNull()?.phases ?: return null
        fun seconds(type: PhaseType): Double? = phases.firstOrNull { it.type == type }
            ?.let { (it.duration as? PhaseDuration.Fixed)?.millis?.div(MILLIS_PER_SECOND) }
        val inhale = seconds(PhaseType.INHALE) ?: return null
        val exhale = seconds(PhaseType.EXHALE) ?: return null
        return PatternSpec(inhale, seconds(PhaseType.HOLD_FULL), exhale, seconds(PhaseType.HOLD_EMPTY), exercise.activating)
    }

    private const val MILLIS_PER_SECOND = 1000.0

    /**
     * Baut aus [spec] eine lauffähige Übung – für das Tagesmuster, gespeicherte Tagesmuster und
     * eigene Muster. [origin] wählt die Texte (siehe [PatternOrigin]); die Texte von
     * [PatternOrigin.TODAY] sind bytegleich mit Version 1.6.8 (DailyPatternGoldenTest).
     *
     * [minutes] legt die Vorgabedauer fest (1–30 min, eigene Muster): Die Wiederholungszahl wird
     * daraus berechnet, sodass `defaultMinutes()` genau diese Dauer liefert und die Detailseite
     * kein „angepasst“ zeigt. `null` heißt ≈ 5 min wie beim Muster des Tages.
     */
    fun exerciseFrom(
        spec: PatternSpec,
        id: String,
        name: String,
        origin: PatternOrigin = PatternOrigin.TODAY,
        minutes: Int? = null,
    ): Exercise {
        val phases = buildList {
            add(Phase(PhaseType.INHALE, secs(spec.inhale)))
            // Notiz wie bei Box und 4-7-8: Beide Haltephasen heißen „Halten“, erst „voll“/„leer“
            // darunter unterscheidet sie (TEXT-02).
            spec.holdFull?.let { add(Phase(PhaseType.HOLD_FULL, secs(it), note = "voll")) }
            add(Phase(PhaseType.EXHALE, secs(spec.exhale)))
            spec.holdEmpty?.let { add(Phase(PhaseType.HOLD_EMPTY, secs(it), note = "leer")) }
        }
        val cycleSeconds = spec.inhale + (spec.holdFull ?: 0.0) + spec.exhale + (spec.holdEmpty ?: 0.0)
        val targetSeconds = minutes?.let { it * 60.0 } ?: TARGET_SECONDS
        val repeat = (targetSeconds / cycleSeconds).roundToInt().coerceAtLeast(1)
        val hint = PatternTexts.hintFor(spec)
        // Gleichlautend mit dem Charakter-Chip (strings.xml daily_pattern_gentle_up / detail_calm);
        // domain/ kennt keine Ressourcen.
        val character = if (spec.activating) "sanft aktivierend" else "ruhig"
        // Atemort: ruhige Muster EMPFEHLEN konstant die Bauchatmung (fachlicher Standard);
        // bewusst NICHT randomisiert – und als Einladung formuliert, nicht als Muss.
        // Punkt nach der Phasenzeile: Sonst liest sich „Ausatmen 5 s Wenn du magst …" als ein Satz.
        val fullHint = if (spec.activating) hint else "$hint. ${PatternTexts.BELLY_GUIDANCE}"
        val texts = when (origin) {
            PatternOrigin.TODAY -> PatternTexts.todayTexts(hint, character, spec.activating)
            PatternOrigin.SAVED_DAILY -> PatternTexts.savedDailyTexts(hint, character, spec)
            PatternOrigin.CUSTOM -> PatternTexts.customTexts(hint, character, spec)
        }

        return Exercise(
            id = id,
            name = name,
            family = BreathingFamily.BALANCE,
            shortDescription = texts.shortDescription,
            effect = texts.effect,
            effectDetail = texts.effectDetail,
            segments = listOf(Segment(phases = phases, repeat = repeat)),
            rounds = 1,
            instructionHint = fullHint,
            lore = PatternLore.loreFor(spec, origin),
            generated = true,
            activating = spec.activating,
        )
    }

    // ---- Frei gestaltete eigene Muster ----

    /**
     * Baut aus einer freien Folge ([FreePattern]) eine lauffähige Übung – für eigene Muster aus
     * dem Editor („Frei gestalten“, Herkunft [PatternOrigin.CUSTOM]). Das Muster des Tages bleibt
     * bei [exerciseFrom] mit [PatternSpec].
     *
     * Phasen: Ein → [PhaseType.INHALE], Aus → [PhaseType.EXHALE], Halten oben →
     * [PhaseType.HOLD_FULL] („voll“), unten → [PhaseType.HOLD_EMPTY] („leer“), dazwischen →
     * [PhaseType.HOLD_FULL] mit Füllstand („Pause“) wie bei Viloma. Einen Füllstand
     * ([Phase.level]) bekommt eine Phase nur, wo er vom Phasentyp abweicht (Stufen); Stufen tragen
     * „1. Stufe“, „2. Stufe“ … als Notiz.
     *
     * [minutes] wie bei [exerciseFrom] (`null` ≈ 5 min); bei Durchläufen über eine Minute stimmt
     * die Vorgabedauer nur auf ganze Durchläufe genau. Ein intensives Muster
     * ([FreePattern.isIntense]) bekommt [Exercise.caution] und Sicherheitshinweise (auch den Satz
     * zur ärztlichen Abklärung wie bei Wim Hof und Viloma), also denselben Sicherheitsdialog vor
     * dem Start. [Exercise.activating] ist gesetzt, wenn die
     * Einordnung Brmhana ist (mehr Ein als Aus). Wirft bei einer ungültigen Folge.
     */
    fun exerciseFrom(pattern: FreePattern, id: String, name: String, minutes: Int? = null): Exercise {
        require(pattern.isValid()) { "Ungültige freie Folge: ${pattern.validate()}" }
        val targetSeconds = minutes?.let { it * 60.0 } ?: TARGET_SECONDS
        val repeat = (targetSeconds / pattern.totalSeconds).roundToInt().coerceAtLeast(1)
        val reasons = pattern.intensityReasons()
        val intense = reasons.isNotEmpty()
        val refs = PatternLore.atemlehreRefs(pattern)
        val activating = refs.patternId == LoreIds.BRMHANA
        val hint = pattern.hint()
        val perMinute = PatternTexts.breathsPerMinutePhrase(pattern)
        val texts = PatternTexts.freeTexts(hint, perMinute, reasons)
        return Exercise(
            id = id,
            name = name,
            family = BreathingFamily.BALANCE,
            shortDescription = texts.shortDescription,
            effect = texts.effect,
            effectDetail = texts.effectDetail,
            segments = listOf(Segment(phases = phasesOf(pattern), repeat = repeat)),
            rounds = 1,
            instructionHint = if (activating || intense) hint else "$hint. ${PatternTexts.BELLY_GUIDANCE}",
            cautions = if (intense) PatternTexts.freeCautions(reasons) else emptyList(),
            caution = intense,
            lore = PatternLore.loreFor(pattern, refs),
            generated = true,
            activating = activating,
        )
    }

    /**
     * Die Phasen einer generierten Übung zurück als freie Folge – für den Editor, beim Muster des
     * Tages ebenso wie bei eigenen Mustern. `null` bei anderen Übungen oder wenn die Phasen keine
     * gültige Folge in ganzen Sekunden ergeben.
     */
    fun freePatternOf(exercise: Exercise): FreePattern? {
        if (!exercise.generated) return null
        val phases = exercise.segments.singleOrNull()?.phases ?: return null
        val free = phases.map { phase ->
            val type = when (phase.type) {
                PhaseType.INHALE -> FreePhaseType.IN
                PhaseType.EXHALE -> FreePhaseType.OUT
                PhaseType.HOLD_FULL, PhaseType.HOLD_EMPTY -> FreePhaseType.HOLD
                else -> return null
            }
            val millis = (phase.duration as? PhaseDuration.Fixed)?.millis ?: return null
            if (millis % WHOLE_SECOND_MILLIS != 0L) return null
            FreePhase(type, (millis / WHOLE_SECOND_MILLIS).toInt())
        }
        return FreePattern(free).takeIf { it.isValid() }
    }

    private fun phasesOf(pattern: FreePattern): List<Phase> {
        val levels = pattern.levels()
        val stages = stageNumbers(pattern)
        return pattern.phases.mapIndexed { i, free ->
            val level = levels[i]
            val duration = PhaseDuration.Fixed(free.seconds * WHOLE_SECOND_MILLIS)
            val stage = stages[i]?.let { "$it. Stufe" }
            when (free.type) {
                FreePhaseType.IN -> Phase(PhaseType.INHALE, duration, note = stage, level = level.takeUnless { it == FULL_INHALE })

                FreePhaseType.OUT -> Phase(PhaseType.EXHALE, duration, note = stage, level = level.takeUnless { it == FULL_EXHALE })

                FreePhaseType.HOLD -> when (pattern.holdPositionAt(i)) {
                    HoldPosition.FULL -> Phase(PhaseType.HOLD_FULL, duration, note = "voll")
                    HoldPosition.EMPTY -> Phase(PhaseType.HOLD_EMPTY, duration, note = "leer")
                    else -> Phase(PhaseType.HOLD_FULL, duration, note = "Pause", level = level)
                }
            }
        }
    }

    /**
     * Stufennummer je Ein bzw. Aus, wenn es zu einem Lauf aus mehreren gehört (Halten dazwischen
     * zählt nicht, zyklisch wie bei [FreePattern.levels]: ein Lauf darf über das Ende der Folge
     * reichen); sonst und bei Halten `null`.
     */
    private fun stageNumbers(pattern: FreePattern): List<Int?> {
        val runIds = IntArray(pattern.phases.size) { -1 }
        var run = -1
        var previous: FreePhaseType? = null
        val order = pattern.cycleOrder()
        order.forEach { i ->
            val phase = pattern.phases[i]
            if (phase.type != FreePhaseType.HOLD) {
                if (phase.type != previous) run++
                runIds[i] = run
                previous = phase.type
            }
        }
        val sizes = runIds.filter { it >= 0 }.groupingBy { it }.eachCount()
        val seen = mutableMapOf<Int, Int>()
        val stages = arrayOfNulls<Int>(pattern.phases.size)
        order.forEach { i ->
            val id = runIds[i]
            if (id >= 0 && (sizes[id] ?: 0) >= 2) stages[i] = (seen.getOrElse(id) { 0 } + 1).also { seen[id] = it }
        }
        return stages.toList()
    }

    private const val WHOLE_SECOND_MILLIS = 1000L
    private val FULL_INHALE = BreathLevel(0f, 1f)
    private val FULL_EXHALE = BreathLevel(1f, 0f)

    private fun secs(value: Double) = PhaseDuration.Fixed((value * 1000).toLong())

    /** Ziel-Vorgabedauer ≈ 5 min (Nutzer regelt im Detail 1–30 min). */
    private const val TARGET_SECONDS = 300.0
}
