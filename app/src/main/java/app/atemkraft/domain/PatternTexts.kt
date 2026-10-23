package app.atemkraft.domain

import kotlin.math.roundToInt

/**
 * Deutsche Inhaltstexte generierter und eigener Muster: Kurzform, Phasen in Worten, Texte je
 * Herkunft ([PatternOrigin]), Grenzen-Sätze und Sicherheitshinweise. domain/ kennt keine
 * Ressourcen; die Texte von [PatternOrigin.TODAY] sind bytegleich mit Version 1.6.8
 * (DailyPatternGoldenTest).
 */
object PatternTexts {

    /** Muster-Kurzform, z. B. „4·2·6" – auch für Namen gespeicherter Muster. */
    fun numbersOf(spec: PatternSpec): String = listOfNotNull(spec.inhale, spec.holdFull, spec.exhale, spec.holdEmpty)
        .joinToString("·") { it.fmt() }

    /**
     * Muster in Worten, z. B. „Einatmen 4 s · Halten 2 s · Ausatmen 6 s". Zwischen Zahl und „s"
     * steht ein geschütztes Leerzeichen ([NBSP]): Bei großer Schrift bricht die Zeile sonst
     * zwischen „1" und „s" um (TEXT-10). Teile werden mit [DOT_SEPARATOR] verbunden.
     */
    fun hintFor(spec: PatternSpec): String = buildList {
        add("Einatmen ${spec.inhale.fmt()}${NBSP}s")
        spec.holdFull?.let { add("Halten ${it.fmt()}${NBSP}s") }
        add("Ausatmen ${spec.exhale.fmt()}${NBSP}s")
        spec.holdEmpty?.let { add("Halten ${it.fmt()}${NBSP}s") }
    }.joinToString(DOT_SEPARATOR)

    /** Geschütztes Leerzeichen zwischen Zahl und Einheit. */
    internal const val NBSP = '\u00A0'

    /** Die Texte, die von der Herkunft abhängen. */
    internal class OriginTexts(val shortDescription: String, val effect: String, val effectDetail: String)

    /** Muster des Tages: bytegleich mit Version 1.6.8 – nicht anfassen (DailyPatternGoldenTest). */
    internal fun todayTexts(hint: String, character: String, activating: Boolean): OriginTexts {
        val bellyNote = if (activating) "" else " Bauchatmung empfohlen – ganz wie es angenehm ist."
        return OriginTexts(
            shortDescription = "$hint – heute $character.$bellyNote Jeden Tag ein neues, " +
                "zufällig erzeugtes Muster in physiologisch üblichen Grenzen.",
            effect = "Spielerische Abwechslung: ein generiertes Muster zum Ausprobieren – " +
                "ohne spezifisches Wirkversprechen.",
            effectDetail = "Das Muster entsteht deterministisch aus dem Datum: Stil und " +
                "Phasenlängen werden zufällig gewählt, aber in Grenzen gehalten, die der " +
                "Forschung zu langsamem Atmen entsprechen (ruhig: 4,6–6,7 Atemzüge pro Minute, " +
                "Ausatmen ≥ Einatmen, Halten ≤ 4 s; aktivierend: Einatmen länger, nie forciert). " +
                "Für regelmäßiges Üben ist ein konstantes Muster – etwa die Resonanz-Atmung – " +
                "die besser belegte Wahl; das Tagesmuster ist die Einladung, Neues zu entdecken.",
        )
    }

    /** Gespeichertes Muster des Tages: ohne „heute“, „jeden Tag neu“ und „aus dem Datum“. */
    internal fun savedDailyTexts(hint: String, character: String, spec: PatternSpec) = OriginTexts(
        shortDescription = "$hint – $character.${bellyInvitation(spec.activating)} " +
            "Gespeichert aus einem Muster des Tages, in den Leitplanken der App.",
        effect = "Dieses Muster stammt aus einem Muster des Tages und liegt in den Leitplanken der App.",
        effectDetail = "Das Muster stammt aus einem Muster des Tages. ${limitsSentence(spec)}$STUDY_NOTE",
    )

    /** Älteres eigenes Muster mit vier Feldern (Editor vor „Frei gestalten“): ehrlich selbst zusammengestellt, ohne Evidenzbehauptung. */
    internal fun customTexts(hint: String, character: String, spec: PatternSpec) = OriginTexts(
        shortDescription = "$hint – $character.${bellyInvitation(spec.activating)} " +
            "Von dir zusammengestellt, in den Leitplanken der App.",
        effect = "Dieses Muster hast du selbst zusammengestellt – in den Leitplanken der App.",
        effectDetail = if (spec.withinCreationRules()) {
            "Beim Zusammenstellen lässt die App nur Muster in festen Grenzen zu. ${limitsSentence(spec)}$STUDY_NOTE"
        } else {
            "${limitsSentence(spec)}$STUDY_NOTE"
        },
    )

    /**
     * Die Grenzen, die [spec] tatsächlich einhält, mit Leerzeichen dahinter: die strengen
     * Erzeugungsregeln ([rulesSentence]) nur, wenn das Muster sie einhält. Ältere gespeicherte
     * Muster (bis 1.6.2: halbe Sekunden, ruhiger Atemzug ab 8,5 s, aktivierend mit gleich langem
     * Ein- und Ausatmen) und importierte liegen oft nur in den weiteren Leitplanken
     * ([guardrailsSentence]); außerhalb auch dieser steht gar kein Grenzen-Satz – der Text
     * behauptet nie etwas, das für genau dieses Muster nicht gilt.
     */
    private fun limitsSentence(spec: PatternSpec): String = when {
        spec.withinCreationRules() -> "${rulesSentence(spec.activating)} "
        spec.withinGuardrails() -> "${guardrailsSentence(spec.activating)} "
        else -> ""
    }

    /** Die weiteren Leitplanken ([withinGuardrails]) des Charakters in Worten. */
    private fun guardrailsSentence(activating: Boolean): String = if (activating) {
        "Es liegt in den weiteren Leitplanken der App: sanft aktivierende Muster bei " +
            "${breathsPerMinute(GUARD_ACTIVE_CYCLE_MAX)} bis ${breathsPerMinute(GUARD_ACTIVE_CYCLE_MIN)} " +
            "Atemzügen pro Minute, das Einatmen mindestens so lang wie das Ausatmen und das Halten " +
            "höchstens ${GUARD_ACTIVE_HOLD_MAX.fmt()}${NBSP}s lang – nie forciert."
    } else {
        "Es liegt in den weiteren Leitplanken der App: ruhige Muster bei " +
            "${breathsPerMinute(GUARD_CALM_CYCLE_MAX)} bis ${breathsPerMinute(GUARD_CALM_CYCLE_MIN)} " +
            "Atemzügen pro Minute, das Ausatmen mindestens so lang wie das Einatmen und höchstens " +
            "${PatternRules.EXHALE_MAX}${NBSP}s, Halten voll höchstens ${GUARD_CALM_HOLD_FULL_MAX.fmt()}${NBSP}s und Halten leer höchstens " +
            "${GUARD_CALM_HOLD_EMPTY_MAX.fmt()}${NBSP}s."
    }

    // Zahlen der Leitplanken wie in [withinGuardrails] (nur für den Text).
    private const val GUARD_CALM_CYCLE_MIN = 8.5
    private const val GUARD_CALM_CYCLE_MAX = 13.0
    private const val GUARD_CALM_HOLD_FULL_MAX = 4.0
    private const val GUARD_CALM_HOLD_EMPTY_MAX = 2.0
    private const val GUARD_ACTIVE_CYCLE_MIN = 6.0
    private const val GUARD_ACTIVE_CYCLE_MAX = 9.0
    private const val GUARD_ACTIVE_HOLD_MAX = 2.0

    /** Bauchatmung nur bei ruhigen Mustern und nur als Einladung (mit führendem Leerzeichen). */
    private fun bellyInvitation(activating: Boolean): String = if (activating) "" else " Gern mit Bauchatmung, wenn es angenehm ist."

    /** Die Erzeugungsregeln des Charakters in Worten, Zahlen aus [PatternRules]. */
    private fun rulesSentence(activating: Boolean): String = if (activating) {
        "Sanft aktivierende Muster bleiben bei ${breathsPerMinute(PatternRules.ACTIVE_CYCLE_MAX)} bis " +
            "${breathsPerMinute(PatternRules.ACTIVE_CYCLE_MIN)} Atemzügen pro Minute, das Einatmen ist " +
            "länger als das Ausatmen und das Halten höchstens ${PatternRules.ACTIVE_HOLD_FULL_MAX}${NBSP}s " +
            "lang – nie forciert."
    } else {
        "Ruhige Muster bleiben bei ${breathsPerMinute(PatternRules.CALM_CYCLE_MAX)} bis " +
            "${breathsPerMinute(PatternRules.CALM_CYCLE_MIN)} Atemzügen pro Minute, das Ausatmen ist " +
            "mindestens so lang wie das Einatmen und höchstens ${PatternRules.EXHALE_MAX}${NBSP}s, " +
            "Halten voll höchstens ${PatternRules.CALM_HOLD_FULL_MAX}${NBSP}s und Halten leer höchstens " +
            "${PatternRules.CALM_HOLD_EMPTY_MAX}${NBSP}s."
    }

    /** Atemzüge pro Minute bei [cycleSeconds], auf eine Nachkommastelle („4,6“, „10“). */
    private fun breathsPerMinute(cycleSeconds: Number): String = ((60.0 / cycleSeconds.toDouble() * 10).roundToInt() / 10.0).fmt()

    /**
     * Keine Evidenzbehauptung für ein einzelnes Muster (TEXT-07): Die Grenzen folgen der
     * Forschung zu langsamem Atmen, das Muster selbst ist nicht untersucht.
     */
    private const val STUDY_NOTE =
        "Die Grenzen orientieren sich an der Forschung zu langsamem Atmen. Eine Studienlage zu " +
            "genau diesem Muster gibt es nicht. Für regelmäßiges Üben ist ein konstantes Muster – " +
            "etwa die Resonanz-Atmung – die besser belegte Wahl."

    /** „6 Atemzüge pro Minute“, „1 Atemzug pro Minute“, „4,6 Atemzüge pro Minute“. */
    internal fun breathsPerMinutePhrase(pattern: FreePattern): String {
        val number = pattern.breathsPerMinuteText()
        return "$number ${if (number == "1") "Atemzug" else "Atemzüge"} pro Minute"
    }

    /**
     * Eigenes Muster als freie Folge: ehrlich selbst zusammengestellt, ohne Wirkversprechen; ein
     * intensives ([reasons] nicht leer) trägt den Satz zum Schwindel.
     */
    internal fun freeTexts(hint: String, perMinute: String, reasons: List<IntensityReason>): OriginTexts {
        val intense = reasons.isNotEmpty()
        return OriginTexts(
            shortDescription = "$hint. $perMinute, von dir selbst zusammengestellt." +
                if (intense) " ${FreePattern.DIZZINESS_SENTENCE}" else "",
            effect = if (intense) {
                "Intensives eigenes Muster: ${FreePattern.DIZZINESS_SENTENCE}"
            } else {
                "Dieses Muster hast du selbst zusammengestellt – ohne Wirkversprechen."
            },
            effectDetail = freeEffectDetail(perMinute, reasons),
        )
    }

    /** Vertiefung: nur, was die App wirklich prüft – keine Stil-Regeln, keine Evidenzbehauptung. */
    private fun freeEffectDetail(perMinute: String, reasons: List<IntensityReason>): String = buildString {
        append(
            "Bei eigenen Mustern prüft die App nur feste Grenzen: ${FreePattern.MIN_PHASES} bis " +
                "${FreePattern.MAX_PHASES} Phasen, jede ${FreePattern.MIN_SECONDS} bis " +
                "${FreePattern.MAX_SECONDS}${NBSP}s lang, mindestens einmal Ein- und Ausatmen. " +
                "Dieses Muster ergibt $perMinute. ",
        )
        if (reasons.isNotEmpty()) {
            append(
                "Es gilt als intensiv (${reasons.joinToString(", ") { it.text }}), deshalb erscheint " +
                    "vor dem Start ein Sicherheitshinweis, solange er in den Einstellungen nicht " +
                    "abgeschaltet ist. ${FreePattern.DIZZINESS_SENTENCE} ",
            )
        }
        append(
            "Eine Studienlage zu genau diesem Muster gibt es nicht. Für regelmäßiges Üben ist ein " +
                "konstantes langsames Muster – etwa die Resonanz-Atmung – die besser belegte Wahl.",
        )
    }

    /**
     * Sicherheitshinweise intensiver eigener Muster; der erste steht auf der Detailseite oben. Wie
     * bei Wim Hof und Viloma gehört die ärztliche Abklärung dazu, bei langem Halten „nie pressen“.
     */
    internal fun freeCautions(reasons: List<IntensityReason>): List<String> = buildList {
        add(FreePattern.DIZZINESS_SENTENCE)
        add("Intensiv durch: ${reasons.joinToString(", ") { it.text }}.")
        add("Nur im Sitzen oder Liegen – nie im oder am Wasser, beim Fahren oder Stehen.")
        add("Nur, wenn du dich gesund und ausgeruht fühlst.")
        add("Bei Epilepsie, Herz-Kreislauf- oder Lungenerkrankung oder in der Schwangerschaft vorher ärztlich abklären.")
        if (IntensityReason.LONG_HOLD_FULL in reasons || IntensityReason.LONG_HOLD_EMPTY in reasons) {
            add("Beim Halten nie pressen – wird es unangenehm, gleich weiteratmen.")
        }
    }

    /** „4" bzw. „4,5" (deutsches Komma, ohne unnötige Null). */
    private fun Double.fmt(): String = if (this % 1.0 == 0.0) toInt().toString() else toString().replace('.', ',')

    /**
     * Bauchatmungs-EMPFEHLUNG der ruhigen Muster (kein Zufall beim Atemort) – bewusst als
     * Einladung formuliert, nicht als Anweisung (Angebot, kein Muss).
     */
    internal const val BELLY_GUIDANCE =
        "Wenn du magst, atme dabei in den Bauch – die Bauchdecke hebt und senkt sich sanft."
}
