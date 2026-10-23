package app.atemkraft.domain

/** Art einer Phase in einem frei gestalteten Muster: Einatmen, Halten oder Ausatmen. */
enum class FreePhaseType { IN, HOLD, OUT }

/** Eine Phase eines frei gestalteten Musters, Dauer in ganzen Sekunden. */
data class FreePhase(val type: FreePhaseType, val seconds: Int)

/** Wo ein Halten steht: oben (voll), unten (leer) oder dazwischen (Pause in Stufen wie Viloma). */
enum class HoldPosition { FULL, EMPTY, BETWEEN }

/** Was an einer freien Folge nicht stimmt ([FreePattern.validate]). */
enum class FreePatternProblem { TOO_FEW_PHASES, TOO_MANY_PHASES, SECONDS_OUT_OF_RANGE, NO_INHALE, NO_EXHALE }

/**
 * Warum ein frei gestaltetes Muster als intensiv gilt ([FreePattern.intensityReasons]); [text] ist
 * die Begründung in Worten für Hinweiskarte und Sicherheitshinweis. Die Halte-Gründe nennen den
 * Füllstand, nicht die Reihenfolge: Eine Pause zwischen zwei Stufen (Viloma) steht nach einem
 * Einatmen, kann aber mit eher leerer Lunge sein.
 */
enum class IntensityReason(val text: String) {
    LONG_HOLD_FULL("Halten mit eher voller Lunge über ${FreePattern.INTENSE_HOLD_FULL_SECONDS} s am Stück"),
    LONG_HOLD_EMPTY("Halten mit eher leerer Lunge über ${FreePattern.INTENSE_HOLD_EMPTY_SECONDS} s am Stück"),
    FAST_BREATHING("mehr als ${FreePattern.INTENSE_BREATHS_PER_MINUTE} Atemzüge pro Minute"),
}

/**
 * Ein eigenes Muster als freie Phasenfolge („Frei gestalten“): Ein, Halten und Aus in beliebiger
 * Reihenfolge, jede Phase ganze Sekunden. Ersetzt für eigene Muster die vier festen Felder
 * ([PatternSpec]) und die Stil-Regeln ([PatternRules]); das Muster des Tages bleibt bei diesen.
 *
 * Regeln ([validate]): 2 bis 12 Phasen, jede 1 bis 30 s, mindestens ein Ein und ein Aus. Mehr
 * nicht – ob ein Muster ruhig oder belebend ist, ordnet die App nur ein ([PatternLore.atemlehreRefs]),
 * und wo es kräftig wird, sagt [intensityReasons].
 *
 * Mehrere Ein (bzw. Aus) hintereinander – Halten dazwischen erlaubt – teilen sich den Anstieg bis
 * „voll“ (bzw. den Abstieg bis „leer“) gleichmäßig: Stufen wie bei Viloma ([levels]).
 */
data class FreePattern(val phases: List<FreePhase>) {

    /** Länge eines Durchlaufs in Sekunden. */
    val totalSeconds: Int get() = phases.sumOf { it.seconds }

    /** Summe aller Ein-Phasen in Sekunden. */
    val inhaleSeconds: Int get() = phases.filter { it.type == FreePhaseType.IN }.sumOf { it.seconds }

    /** Summe aller Aus-Phasen in Sekunden. */
    val exhaleSeconds: Int get() = phases.filter { it.type == FreePhaseType.OUT }.sumOf { it.seconds }

    /** Was gegen die Regeln verstößt; leer heißt gültig. */
    fun validate(): List<FreePatternProblem> = buildList {
        if (phases.size < MIN_PHASES) add(FreePatternProblem.TOO_FEW_PHASES)
        if (phases.size > MAX_PHASES) add(FreePatternProblem.TOO_MANY_PHASES)
        if (phases.any { it.seconds !in SECONDS_RANGE }) add(FreePatternProblem.SECONDS_OUT_OF_RANGE)
        if (phases.none { it.type == FreePhaseType.IN }) add(FreePatternProblem.NO_INHALE)
        if (phases.none { it.type == FreePhaseType.OUT }) add(FreePatternProblem.NO_EXHALE)
    }

    /** Hält die Folge die Regeln ein (siehe [validate])? */
    fun isValid(): Boolean = validate().isEmpty()

    /**
     * Füllstand der Lunge je Phase (0 = leer, 1 = voll), wie `levels()` im Design „Frei gestalten“,
     * aber zyklisch betrachtet – die Folge wiederholt sich ja: Ein Ein steigt um seinen Anteil am
     * restlichen Weg bis voll – geteilt durch die Zahl der Ein bis zum nächsten Aus (Halten
     * dazwischen zählt nicht) –, ein Aus fällt entsprechend bis leer. Das letzte Ein eines Laufs
     * endet also genau bei voll, das letzte Aus genau bei leer; Halten behält den Füllstand.
     *
     * Ein Lauf darf über das Ende der Folge reichen: Bei „Ein 4 · Aus 6 · Ein 4“ sind das letzte
     * und das erste Ein zwei Stufen eines Atemzugs (0 → ½ am Ende, ½ → 1 am Anfang). So springt der
     * Füllstand beim Wiederholen nie ([cycleOrder]). Beginnt die Folge mit einem Lauf, startet sie
     * leer (Ein) bzw. voll (Aus) wie im Design.
     */
    fun levels(): List<BreathLevel> {
        if (phases.isEmpty()) return emptyList()
        val order = cycleOrder()
        val result = arrayOfNulls<BreathLevel>(phases.size)
        var level = if (phases[order.first()].type == FreePhaseType.OUT) 1.0 else 0.0
        order.forEachIndexed { position, index ->
            val phase = phases[index]
            val start = level
            if (phase.type != FreePhaseType.HOLD) {
                val remaining = runLength(order, position)
                val target = if (phase.type == FreePhaseType.IN) 1.0 else 0.0
                // Das letzte Glied des Laufs landet exakt am Ziel, ohne Rundungsrest.
                level = if (remaining == 1) target else level + (target - level) / remaining
            }
            result[index] = BreathLevel(start.toFloat(), level.toFloat())
        }
        return result.map { checkNotNull(it) }
    }

    /**
     * Die Phasen-Indizes in der Reihenfolge eines Durchlaufs, zyklisch so gedreht, dass sie mit dem
     * Anfang eines Laufs beginnen: mit der ersten Ein- oder Aus-Phase, vor der (über das Ende der
     * Folge hinweg, Halten übersprungen) der Gegentyp steht. Ein Lauf oder ein Block aus Halten
     * reicht in dieser Reihenfolge nie über ihr Ende. Bei Folgen, deren erstes und letztes Ein/Aus
     * verschieden sind, ist das die Folge selbst ab dem ersten Ein/Aus.
     */
    fun cycleOrder(): List<Int> {
        val breaths = phases.indices.filter { phases[it].type != FreePhaseType.HOLD }
        val start = breaths.indices.firstOrNull { k ->
            phases[breaths[k]].type != phases[breaths[(k - 1 + breaths.size) % breaths.size]].type
        }?.let { breaths[it] } ?: breaths.firstOrNull() ?: 0
        return phases.indices.map { (start + it) % phases.size }
    }

    /** Wie viele Phasen vom Typ der Phase an [position] in [order] ab dort noch folgen, bis der Gegentyp kommt. */
    private fun runLength(order: List<Int>, position: Int): Int {
        val type = phases[order[position]].type
        var count = 0
        for (m in position until order.size) {
            when (phases[order[m]].type) {
                type -> count++
                FreePhaseType.HOLD -> Unit
                else -> return count
            }
        }
        return count
    }

    /**
     * Gehört Phase [index] zur oberen Seite (Einatem-Seite)? Einatmen immer, Ausatmen nie, ein
     * Halten ab halb voller Lunge. Die eine Regel für Phasenfarbe (Kurve, Ablauf, Mini-Kurve,
     * Status: oben `secondary`, unten `BreathExhale`), Lage der Beschriftung in der Kurve, die
     * Schwelle für „intensiv“ ([intensityReasons]) und die Spalten „Halten voll/leer“.
     */
    fun isUpperSide(index: Int): Boolean = when (phases[index].type) {
        FreePhaseType.IN -> true
        FreePhaseType.OUT -> false
        FreePhaseType.HOLD -> isUpperLevel(levels()[index].from)
    }

    /** Wo das Halten an [index] steht; `null`, wenn dort kein Halten ist. */
    fun holdPositionAt(index: Int): HoldPosition? = holdPositions()[index]

    private fun holdPositions(): List<HoldPosition?> {
        val levels = levels()
        return phases.mapIndexed { i, phase ->
            if (phase.type != FreePhaseType.HOLD) {
                null
            } else {
                val level = levels[i].from
                when {
                    level >= 1f - LEVEL_EPSILON -> HoldPosition.FULL
                    level <= LEVEL_EPSILON -> HoldPosition.EMPTY
                    else -> HoldPosition.BETWEEN
                }
            }
        }
    }

    /**
     * Name der Phase [index]: „Einatmen“, „Ausatmen“, „Halten voll“ (oben), „Halten leer“ (unten),
     * sonst „Halten“ (Pause zwischen zwei Stufen).
     */
    fun labelAt(index: Int): String = when (phases[index].type) {
        FreePhaseType.IN -> LABEL_IN

        FreePhaseType.OUT -> LABEL_OUT

        FreePhaseType.HOLD -> when (holdPositionAt(index)) {
            HoldPosition.FULL -> LABEL_HOLD_FULL
            HoldPosition.EMPTY -> LABEL_HOLD_EMPTY
            else -> LABEL_HOLD
        }
    }

    /** Muster-Kurzform aus den Sekunden, z. B. „4·1·6“ (Vorschlagsname „Eigenes Muster 4·1·6“). */
    fun numbers(): String = phases.joinToString("·") { it.seconds.toString() }

    /**
     * Muster in Worten, z. B. „Einatmen 4 s · Halten voll 1 s · Ausatmen 6 s“, mit geschütztem
     * Leerzeichen vor „s“ (TEXT-10) und [DOT_SEPARATOR].
     */
    fun hint(): String = phases.indices.joinToString(DOT_SEPARATOR) { "${labelAt(it)} ${phases[it].seconds} s" }

    /** Beschreibung für TalkBack, z. B. „Einatmen 4 Sekunden, Halten voll 1 Sekunde, Ausatmen 6 Sekunden“. */
    fun spokenDescription(): String = phases.indices.joinToString(", ") {
        val seconds = phases[it].seconds
        "${labelAt(it)} $seconds ${if (seconds == 1) "Sekunde" else "Sekunden"}"
    }

    /**
     * Zahl der Atemzüge je Durchlauf: jeder Lauf von Ein (Halten dazwischen zählt nicht) ist ein
     * Atemzug, wie im Design – zyklisch gezählt ([cycleOrder]), damit ein Lauf über das Ende der
     * Folge nicht doppelt zählt.
     */
    fun breathCount(): Int {
        var runs = 0
        var previous: FreePhaseType? = null
        cycleOrder().forEach { index ->
            val type = phases[index].type
            if (type == FreePhaseType.IN && previous != FreePhaseType.IN) runs++
            if (type != FreePhaseType.HOLD) previous = type
        }
        return runs
    }

    /** Atemzüge pro Minute ([breathCount] je [totalSeconds]); 0 bei einer leeren Folge. */
    fun breathsPerMinute(): Double = if (totalSeconds <= 0) 0.0 else breathCount() * 60.0 / totalSeconds

    /** Atemzüge pro Minute auf eine Nachkommastelle mit deutschem Komma („6“, „4,6“). */
    fun breathsPerMinuteText(): String = breathsPerMinute().formatDecimal()

    /**
     * Warum das Muster als intensiv gilt, in fester Reihenfolge; leer heißt nicht intensiv.
     * Gezählt wird, wie lange man am Stück nicht atmet: Halten direkt hintereinander – auch über
     * das Ende der Folge zum Anfang ([cycleOrder]) – zählen zusammen. Wie im Design gehört ein
     * Halten zur oberen Schwelle, wenn die Lunge mindestens halb voll ist, sonst zur unteren
     * ([isUpperSide]).
     * Intensiv ist Halten voll über 10 s, Halten leer über 8 s oder mehr als 20 Atemzüge pro
     * Minute (ganzzahlig verglichen, ohne Rundung).
     */
    fun intensityReasons(): List<IntensityReason> {
        val levels = levels()
        var longestFull = 0
        var longestEmpty = 0
        var block = 0
        var blockFull = false
        fun endBlock() {
            if (blockFull) longestFull = maxOf(longestFull, block) else longestEmpty = maxOf(longestEmpty, block)
            block = 0
        }
        cycleOrder().forEach { i ->
            if (phases[i].type == FreePhaseType.HOLD) {
                if (block == 0) blockFull = isUpperLevel(levels[i].from)
                block += phases[i].seconds
            } else if (block > 0) {
                endBlock()
            }
        }
        if (block > 0) endBlock()
        return buildList {
            if (longestFull > INTENSE_HOLD_FULL_SECONDS) add(IntensityReason.LONG_HOLD_FULL)
            if (longestEmpty > INTENSE_HOLD_EMPTY_SECONDS) add(IntensityReason.LONG_HOLD_EMPTY)
            // breathCount · 60 / total > 20  ⇔  breathCount · 60 > 20 · total
            if (totalSeconds > 0 && breathCount() * 60 > INTENSE_BREATHS_PER_MINUTE * totalSeconds) {
                add(IntensityReason.FAST_BREATHING)
            }
        }
    }

    /** Gilt das Muster als intensiv (Sicherheitsdialog vor dem Start, siehe [intensityReasons])? */
    val isIntense: Boolean get() = intensityReasons().isNotEmpty()

    /**
     * Text der Hinweiskarte im Editor, `null` wenn nicht intensiv: „Intensiv: <Gründe>. Vor dem
     * Start erscheint ein Sicherheitshinweis, solange er in den Einstellungen nicht abgeschaltet
     * ist. Bei Schwindel abbrechen und normal weiteratmen.“ – ehrlich, denn wer ihn bestätigt und
     * dann abgeschaltet hat, sieht ihn nicht mehr.
     */
    fun intensityNote(): String? {
        val reasons = intensityReasons()
        if (reasons.isEmpty()) return null
        return "Intensiv: ${reasons.joinToString(", ") { it.text }}. Vor dem Start erscheint ein " +
            "Sicherheitshinweis, solange er in den Einstellungen nicht abgeschaltet ist. $DIZZINESS_SENTENCE"
    }

    /** Feste Länge der Zeitachse der Atemkurve: max(12, gerade Zahl ≥ Gesamtdauer + 6) Sekunden. */
    fun axisSeconds(): Int {
        val withRoom = totalSeconds + AXIS_ROOM_SECONDS
        return maxOf(AXIS_MIN_SECONDS, (withRoom + 1) / 2 * 2)
    }

    // ---- Bearbeiten (Editor): jede Änderung bleibt in den Regeln ----

    /** Phase [index] auf [seconds] gesetzt, geklemmt auf 1–30 s. */
    fun withSeconds(index: Int, seconds: Int): FreePattern {
        if (index !in phases.indices) return this
        val clamped = seconds.coerceIn(SECONDS_RANGE)
        return FreePattern(phases.mapIndexed { i, phase -> if (i == index) phase.copy(seconds = clamped) else phase })
    }

    /** Ist noch Platz für eine weitere Phase (höchstens 12)? */
    val canAdd: Boolean get() = phases.size < MAX_PHASES

    /**
     * Stelle, an der eine neue Phase eingefügt wird: hinter der gewählten Phase [selected], ohne
     * Auswahl am Ende.
     */
    fun insertIndex(selected: Int?): Int = ((selected ?: phases.lastIndex) + 1).coerceIn(0, phases.size)

    /**
     * Eine neue Phase vom Typ [type] hinter [selected] (ohne Auswahl am Ende): Ein und Aus mit
     * 4 s, Halten mit 2 s. Bei 12 Phasen unverändert.
     */
    fun inserted(selected: Int?, type: FreePhaseType): FreePattern {
        if (!canAdd) return this
        val seconds = if (type == FreePhaseType.HOLD) NEW_HOLD_SECONDS else NEW_BREATH_SECONDS
        return FreePattern(phases.toMutableList().apply { add(insertIndex(selected), FreePhase(type, seconds)) })
    }

    /** Darf Phase [index] weg, ohne dass die Folge ungültig wird (2 Phasen, ein Ein, ein Aus)? */
    fun canRemove(index: Int): Boolean = index in phases.indices && removedUnchecked(index).isValid()

    /** Die Folge ohne Phase [index]; unverändert, wenn sie danach ungültig wäre ([canRemove]). */
    fun removed(index: Int): FreePattern = if (canRemove(index)) removedUnchecked(index) else this

    private fun removedUnchecked(index: Int) = FreePattern(phases.filterIndexed { i, _ -> i != index })

    /**
     * Kompakte Textform zum Speichern und für das Backup, z. B. „in:4,hold:1,out:6“. Gegenstück
     * zu [parse].
     */
    fun toCompact(): String = phases.joinToString(",") { "${it.type.compactName()}:${it.seconds}" }

    companion object {
        const val MIN_SECONDS = 1
        const val MAX_SECONDS = 30
        val SECONDS_RANGE = MIN_SECONDS..MAX_SECONDS
        const val MIN_PHASES = 2
        const val MAX_PHASES = 12

        /** Länge einer neu hinzugefügten Ein- oder Aus-Phase. */
        const val NEW_BREATH_SECONDS = 4

        /** Länge eines neu hinzugefügten Haltens. */
        const val NEW_HOLD_SECONDS = 2

        // Schwellen für „intensiv“ (Nachtrag 4, jeweils echt darüber).
        const val INTENSE_HOLD_FULL_SECONDS = 10
        const val INTENSE_HOLD_EMPTY_SECONDS = 8
        const val INTENSE_BREATHS_PER_MINUTE = 20

        // Namen aus [PhaseNames] (spiegelt strings.xml `phase_*`).
        private const val LABEL_IN = PhaseNames.INHALE
        private const val LABEL_OUT = PhaseNames.EXHALE
        private const val LABEL_HOLD_FULL = PhaseNames.HOLD_FULL
        private const val LABEL_HOLD_EMPTY = PhaseNames.HOLD_EMPTY
        private const val LABEL_HOLD = PhaseNames.HOLD

        /**
         * Alle Namen, die [labelAt] liefern kann – für Layouts, die Platz für den längsten
         * reservieren, damit nichts springt, wenn die Auswahl oder die Vorschau die Phase wechselt.
         */
        val PHASE_LABELS = listOf(LABEL_IN, LABEL_OUT, LABEL_HOLD_FULL, LABEL_HOLD_EMPTY, LABEL_HOLD)

        /** Der Sicherheitssatz für intensive Muster (Editor-Hinweis, Sicherheitsdialog, Texte). */
        const val DIZZINESS_SENTENCE = "Bei Schwindel abbrechen und normal weiteratmen."

        private const val AXIS_MIN_SECONDS = 12
        private const val AXIS_ROOM_SECONDS = 6
        private const val LEVEL_EPSILON = 1e-4f
        private const val HALF = 0.5f

        /** Ab halb voll gehört ein Füllstand zur oberen Seite ([isUpperSide]). */
        private fun isUpperLevel(level: Float): Boolean = level >= HALF

        /** Längster Text, den [parse] annimmt: 12 × „hold:30“ und Kommas, großzügig gerundet. */
        private const val MAX_COMPACT_LENGTH = 200

        /** Startpunkt eines neuen Musters: Ein 4, Aus 6. */
        val DEFAULT: FreePattern get() = FreePatternTemplates.all.first().pattern

        /**
         * Liest die Textform von [toCompact], robust gegen Leerzeichen und Groß-/Kleinschreibung
         * („ IN : 4 , out:6 “). `null` bei allem anderen: unbekannte Phase, keine ganze Zahl
         * (auch „4.5“, „-1“, „+4“), leere Teile, zu lang – und bei einer Folge, die die Regeln
         * nicht einhält ([validate]). So kommt nie ein ungültiges Muster aus Datenbank oder Import.
         */
        fun parse(text: String?): FreePattern? {
            if (text == null || text.length > MAX_COMPACT_LENGTH) return null
            val parts = text.split(',')
            if (parts.size !in MIN_PHASES..MAX_PHASES) return null
            val phases = parts.map { part -> parsePhase(part) ?: return null }
            return FreePattern(phases).takeIf { it.isValid() }
        }

        private fun parsePhase(part: String): FreePhase? {
            val pieces = part.split(':')
            if (pieces.size != 2) return null
            val type = when (pieces[0].trim().lowercase()) {
                "in" -> FreePhaseType.IN
                "hold" -> FreePhaseType.HOLD
                "out" -> FreePhaseType.OUT
                else -> return null
            }
            val digits = pieces[1].trim()
            if (digits.isEmpty() || digits.length > 2 || digits.any { it !in '0'..'9' }) return null
            return FreePhase(type, digits.toInt())
        }

        private fun FreePhaseType.compactName(): String = when (this) {
            FreePhaseType.IN -> "in"
            FreePhaseType.HOLD -> "hold"
            FreePhaseType.OUT -> "out"
        }
    }
}

/**
 * Das Muster als freie Folge, z. B. für „Als eigenes Muster anpassen“ beim Muster des Tages:
 * Ein, Halten voll, Aus, Halten leer in dieser Reihenfolge; fehlendes Halten fällt weg. Ältere
 * Werte mit halben Sekunden werden gerundet ([secondsOf]) und auf 1–30 s geklemmt.
 */
fun PatternSpec.toFreePattern(): FreePattern = FreePattern(
    buildList {
        add(FreePhase(FreePhaseType.IN, secondsOf(PatternField.INHALE).coerceIn(FreePattern.SECONDS_RANGE)))
        secondsOf(PatternField.HOLD_FULL).takeIf { it > 0 }?.let { add(FreePhase(FreePhaseType.HOLD, it.coerceAtMost(FreePattern.MAX_SECONDS))) }
        add(FreePhase(FreePhaseType.OUT, secondsOf(PatternField.EXHALE).coerceIn(FreePattern.SECONDS_RANGE)))
        secondsOf(PatternField.HOLD_EMPTY).takeIf { it > 0 }?.let { add(FreePhase(FreePhaseType.HOLD, it.coerceAtMost(FreePattern.MAX_SECONDS))) }
    },
)

/** Eine Vorlage im aufklappbaren „Startpunkt“ des Editors. */
data class FreeTemplate(val label: String, val pattern: FreePattern)

/** Die Vorlagen für „Startpunkt“ (Nachtrag 4), die erste ist der Start eines neuen Musters. */
object FreePatternTemplates {

    val all: List<FreeTemplate> = listOf(
        template("4·6", FreePhaseType.IN to 4, FreePhaseType.OUT to 6),
        template("Box", FreePhaseType.IN to 4, FreePhaseType.HOLD to 4, FreePhaseType.OUT to 4, FreePhaseType.HOLD to 4),
        template("4-7-8", FreePhaseType.IN to 4, FreePhaseType.HOLD to 7, FreePhaseType.OUT to 8),
        template("Seufzer", FreePhaseType.IN to 3, FreePhaseType.IN to 1, FreePhaseType.OUT to 6),
        template(
            "Viloma I",
            FreePhaseType.IN to 2,
            FreePhaseType.HOLD to 2,
            FreePhaseType.IN to 2,
            FreePhaseType.HOLD to 2,
            FreePhaseType.IN to 2,
            FreePhaseType.HOLD to 2,
            FreePhaseType.OUT to 6,
        ),
        template(
            "Wechsel",
            FreePhaseType.IN to 3,
            FreePhaseType.OUT to 3,
            FreePhaseType.IN to 4,
            FreePhaseType.HOLD to 2,
            FreePhaseType.OUT to 8,
        ),
    )

    /** Die Vorlage, die genau [pattern] entspricht; `null` heißt „angepasst“. */
    fun matching(pattern: FreePattern): FreeTemplate? = all.firstOrNull { it.pattern == pattern }

    private fun template(label: String, vararg phases: Pair<FreePhaseType, Int>) = FreeTemplate(label, FreePattern(phases.map { (type, seconds) -> FreePhase(type, seconds) }))
}
