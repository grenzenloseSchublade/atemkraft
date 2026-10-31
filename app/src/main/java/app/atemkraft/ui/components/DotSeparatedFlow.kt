package app.atemkraft.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable

/**
 * Teile nebeneinander, getrennt durch [separator] („·“), jede Zeile für sich zentriert. Passt ein
 * Teil nicht mehr in die Zeile, beginnt er die nächste – und der Trenner entfällt an dieser
 * Stelle: Er steht nur zwischen zwei Teilen derselben Zeile, keine Zeile beginnt oder endet mit
 * „·“ (LAYOUT-03, Regel in `domain/TextConventions.kt` bei `DOT_SPLIT`). Gemeinsam für den Kopf
 * der Atem-Session („Name · Runde 3 von 8“) und die Bilanz am Ende.
 */
@Composable
fun DotSeparatedFlow(
    parts: List<@Composable () -> Unit>,
    separator: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        contents = listOf(
            { parts.forEach { it() } },
            { repeat((parts.size - 1).coerceAtLeast(0)) { separator() } },
        ),
        modifier = modifier,
    ) { (partMeasurables, separatorMeasurables), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val partP = partMeasurables.map { it.measure(loose) }
        val sepP = separatorMeasurables.map { it.measure(loose) }
        // Zeilen gierig füllen; ein Trenner kommt nur vor einen Teil, der in derselben Zeile bleibt.
        val lines = mutableListOf<MutableList<Int>>()
        var lineWidth = 0
        partP.forEachIndexed { i, p ->
            val sepW = if (i > 0) sepP[i - 1].width else 0
            if (lines.isEmpty() || lineWidth + sepW + p.width > constraints.maxWidth) {
                lines += mutableListOf(i)
                lineWidth = p.width
            } else {
                lines.last() += i
                lineWidth += sepW + p.width
            }
        }
        fun widthOf(line: List<Int>) = line.sumOf { partP[it].width } + line.drop(1).sumOf { sepP[it - 1].width }
        fun heightOf(line: List<Int>) = line.maxOf { i -> maxOf(partP[i].height, if (i != line.first()) sepP[i - 1].height else 0) }
        val width = (lines.maxOfOrNull(::widthOf) ?: 0).coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = lines.sumOf(::heightOf).coerceAtLeast(constraints.minHeight)
        layout(width, height) {
            var y = 0
            lines.forEach { line ->
                val lineH = heightOf(line)
                var x = (width - widthOf(line)) / 2
                line.forEach { i ->
                    if (i != line.first()) x = placeCentered(sepP[i - 1], x, y, lineH)
                    x = placeCentered(partP[i], x, y, lineH)
                }
                y += lineH
            }
        }
    }
}

/** Setzt [p] bei [x] vertikal mittig in die Zeile ab [y] und gibt das rechte Ende zurück. */
private fun Placeable.PlacementScope.placeCentered(p: Placeable, x: Int, y: Int, lineH: Int): Int {
    p.placeRelative(x, y + (lineH - p.height) / 2)
    return x + p.width
}
