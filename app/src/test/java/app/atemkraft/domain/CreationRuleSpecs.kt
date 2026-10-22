package app.atemkraft.domain

/**
 * Für Tests: alle Muster in ganzen Sekunden, die die Erzeugungsregeln einhalten ([withinCreationRules]),
 * ruhig und sanft aktivierend – jedes Muster, das als Muster des Tages (und damit gespeichert oder
 * als ältere eigene Vier-Felder-Zeile) vorkommen kann.
 */
val creationRuleSpecs: List<PatternSpec> = buildList {
    for (activating in listOf(false, true)) {
        for (inhale in PatternRules.INHALE_MIN..PatternRules.EXHALE_MAX) {
            for (exhale in PatternRules.EXHALE_MIN..PatternRules.EXHALE_MAX) {
                for (holdFull in 0..PatternRules.CALM_HOLD_FULL_MAX) {
                    for (holdEmpty in 0..PatternRules.CALM_HOLD_EMPTY_MAX) {
                        add(
                            PatternSpec(
                                inhale = inhale.toDouble(),
                                holdFull = holdFull.takeIf { it > 0 }?.toDouble(),
                                exhale = exhale.toDouble(),
                                holdEmpty = holdEmpty.takeIf { it > 0 }?.toDouble(),
                                activating = activating,
                            ),
                        )
                    }
                }
            }
        }
    }
}.filter { it.withinCreationRules() }

/** Ein typisches ruhiges Muster mit Halten (4·1·6) und ein sanft aktivierendes ohne (4·3). */
val calmWithHold = PatternSpec(4.0, 1.0, 6.0, null, activating = false)
val activeWithoutHold = PatternSpec(4.0, null, 3.0, null, activating = true)
