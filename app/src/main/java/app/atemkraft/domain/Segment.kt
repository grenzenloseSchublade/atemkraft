package app.atemkraft.domain

/**
 * Ein Block aus Phasen, der innerhalb einer Runde [repeat]-mal wiederholt wird.
 *
 * Beispiel Wim-Hof-„Power-Breaths": ein Segment mit den Phasen [Einatmen, Ausatmen]
 * und repeat = 30. Box-Atmung kommt mit einem einzigen Segment (repeat = 1) aus.
 */
data class Segment(
    val phases: List<Phase>,
    val repeat: Int = 1,
)
