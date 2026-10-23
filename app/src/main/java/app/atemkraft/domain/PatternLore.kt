package app.atemkraft.domain

/**
 * Einordnung eines Musters in die Atemlehre ([PatternLore.atemlehreRefs]):
 * [patternId] ist die Hauptkarte (Brmhana, Langhana oder Samana), [morePatternIds] die Karten
 * der Haltephasen („halten-voll“, „halten-leer“) in dieser Reihenfolge.
 */
data class AtemlehreRefs(val patternId: String, val morePatternIds: List<String>)

/**
 * Einordnung generierter und eigener Muster in die Atemlehre ([atemlehreRefs]) samt Teaser und
 * Detail für den Abschnitt „Aus der Yoga-Lehre“.
 */
object PatternLore {

    /**
     * Einordnung nach der Atemlehre ([atemlehreRefs]) mit Teaser und Detail. Der Teaser hängt nur
     * am Verhältnis der Phasen, der erste Satz des Details an der Herkunft.
     */
    internal fun loreFor(spec: PatternSpec, origin: PatternOrigin): YogaLore {
        val refs = atemlehreRefs(spec)
        val patternId = refs.patternId
        val teaser = teaserFor(patternId)
        val detail = buildString {
            append(
                when (origin) {
                    PatternOrigin.TODAY -> "Das Muster ist zufällig erzeugt"
                    PatternOrigin.SAVED_DAILY -> "Das Muster stammt aus einem Muster des Tages"
                    PatternOrigin.CUSTOM -> "Du hast das Muster selbst zusammengestellt"
                },
            )
            append("; die Einordnung folgt nur dem Verhältnis der Phasen.")
            if (spec.holdFull != null) append(holdFullSentence(patternId))
            if (spec.holdEmpty != null) append(HOLD_EMPTY_SENTENCE)
        }
        return YogaLore(teaser = teaser, detail = detail, patternId = patternId, morePatternIds = refs.morePatternIds)
    }

    /** Teaser der Einordnung je Hauptkarte (≤ 95 Zeichen, zuschreibend formuliert). */
    private fun teaserFor(patternId: String): String = when (patternId) {
        LoreIds.BRMHANA -> "Längeres Einatmen gilt in der Yoga-Lehre als *bṛṃhaṇa*: belebend und wärmend."
        LoreIds.LANGHANA -> "Längeres Ausatmen gilt in der Yoga-Lehre als *laṅghana*: beruhigend und kühlend."
        else -> "Gleich langes Ein- und Ausatmen gilt in der Yoga-Lehre als *samāna*: ausgleichend."
    }

    /** Satz zum Halten nach dem Einatmen, mit führendem Leerzeichen. */
    private fun holdFullSentence(patternId: String): String = if (patternId == LoreIds.BRMHANA) {
        " Das Halten nach dem Einatmen verstärkt nach der Lehre das Belebende."
    } else {
        " Das Halten nach dem Einatmen gilt in der Lehre als sammelnd und verstärkt eher das " +
            "Belebende; das Muster mischt also beides."
    }

    /** Satz zum Halten nach dem Ausatmen, mit führendem Leerzeichen. */
    private const val HOLD_EMPTY_SENTENCE =
        " Das Halten nach dem Ausatmen gilt als Weg in die Stille und verstärkt das Beruhigende."

    /**
     * Einordnung eines Musters in die Atemlehre: folgt nur dem Verhältnis der Phasen, nicht einer
     * Quelle. Eine Logik für Muster des Tages und gespeicherte Muster mit vier Feldern; freie
     * Folgen ordnet [atemlehreRefs] mit [FreePattern] ein.
     *
     * - Ausatmen kürzer als Einatmen → „brmhana“, länger → „langhana“, gleich → „samana“. Auch
     *   aktivierende Muster mit gleich langem Ein- und Ausatmen (gespeichert bis 1.6.2) sind
     *   Samana: Das Tempo allein macht sie nach der Lehre nicht zu Brmhana.
     * - Ein Halten verweist zusätzlich auf seine Karte: erst „halten-voll“, dann „halten-leer“.
     *
     * Die Kennungen sind die Muster-Karten der Atemlehre ([LoreIds]).
     */
    fun atemlehreRefs(spec: PatternSpec): AtemlehreRefs {
        val patternId = when {
            spec.exhale < spec.inhale -> LoreIds.BRMHANA
            spec.exhale > spec.inhale -> LoreIds.LANGHANA
            else -> LoreIds.SAMANA
        }
        val holds = listOfNotNull(
            LoreIds.HALTEN_VOLL.takeIf { spec.holdFull != null },
            LoreIds.HALTEN_LEER.takeIf { spec.holdEmpty != null },
        )
        return AtemlehreRefs(patternId = patternId, morePatternIds = holds)
    }

    /**
     * Einordnung einer freien Folge in die Atemlehre nach den Summen der Phasen: Aus insgesamt
     * länger als Ein → „langhana“, kürzer → „brmhana“, gleich → „samana“. Dazu „halten-voll“,
     * wenn es ein Halten oben gibt, und „halten-leer“ für ein Halten unten, in dieser Reihenfolge.
     * Pausen zwischen Stufen ([HoldPosition.BETWEEN]) verweisen auf keine Karte.
     */
    fun atemlehreRefs(pattern: FreePattern): AtemlehreRefs {
        val patternId = when {
            pattern.exhaleSeconds > pattern.inhaleSeconds -> LoreIds.LANGHANA
            pattern.exhaleSeconds < pattern.inhaleSeconds -> LoreIds.BRMHANA
            else -> LoreIds.SAMANA
        }
        val positions = pattern.phases.indices.mapNotNull { pattern.holdPositionAt(it) }
        val holds = listOfNotNull(
            LoreIds.HALTEN_VOLL.takeIf { HoldPosition.FULL in positions },
            LoreIds.HALTEN_LEER.takeIf { HoldPosition.EMPTY in positions },
        )
        return AtemlehreRefs(patternId = patternId, morePatternIds = holds)
    }

    /** Einordnung einer freien Folge mit Teaser und Detail (siehe [atemlehreRefs] für freie Folgen). */
    internal fun loreFor(pattern: FreePattern, refs: AtemlehreRefs): YogaLore {
        val detail = buildString {
            append(
                "Du hast das Muster selbst zusammengestellt; die Einordnung folgt nur der Summe der " +
                    "Phasen (Einatmen ${pattern.inhaleSeconds}${PatternTexts.NBSP}s, Ausatmen ${pattern.exhaleSeconds}${PatternTexts.NBSP}s).",
            )
            if (LoreIds.HALTEN_VOLL in refs.morePatternIds) append(holdFullSentence(refs.patternId))
            if (LoreIds.HALTEN_LEER in refs.morePatternIds) append(HOLD_EMPTY_SENTENCE)
        }
        return YogaLore(
            teaser = teaserFor(refs.patternId),
            detail = detail,
            patternId = refs.patternId,
            morePatternIds = refs.morePatternIds,
        )
    }
}
