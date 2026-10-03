package app.atemkraft.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import app.atemkraft.ui.theme.Dimens

/**
 * Reihe gleichrangiger Buttons, deren Beschriftung nie umbricht (LAYOUT-03). Gemessen wird die
 * einzeilige Breite jedes Buttons (`maxIntrinsicWidth`):
 * 1. passen alle in gleich breite Spalten → gleich breit (Normalfall, ruhiges Bild),
 * 2. passen sie nur nach Inhalt verteilt → jeder bekommt seine Breite plus gleichen Anteil am Rest,
 * 3. sonst, wenn die übrigen nebeneinander passen → erster (Primär-)Button allein in voller
 *    Breite, die übrigen darunter in einer Reihe (spart eine Zeile Höhe),
 * 4. sonst → alle untereinander in voller Breite, in Lesereihenfolge.
 * Alle Buttons einer Reihe bekommen dieselbe Höhe.
 */
@Composable
fun AdaptiveButtonRow(
    modifier: Modifier = Modifier,
    spacing: Dp = Dimens.ListGap,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val n = measurables.size
        if (n == 0) return@Layout layout(0, 0) {}
        val width = constraints.maxWidth
        val natural = measurables.map { it.maxIntrinsicWidth(Constraints.Infinity) }
        val available = width - gap * (n - 1)

        val widths = rowWidths(natural, available)
        if (widths != null) {
            val row = placeRow(measurables, widths, gap)
            return@Layout layout(width, row.height) { row.place(this, 0) }
        }

        // Stufe 3: Primär-Button oben, Rest als Reihe darunter.
        if (n > 2) {
            val restWidths = rowWidths(natural.drop(1), width - gap * (n - 2))
            if (restWidths != null) {
                val first = measurables[0].measure(Constraints.fixedWidth(width))
                val rest = placeRow(measurables.drop(1), restWidths, gap)
                val height = first.height + gap + rest.height
                return@Layout layout(width, height) {
                    first.placeRelative(0, 0)
                    rest.place(this, first.height + gap)
                }
            }
        }

        // Stufe 4: alle untereinander.
        val placeables = measurables.map { it.measure(Constraints.fixedWidth(width)) }
        val height = placeables.sumOf { it.height } + gap * (n - 1)
        layout(width, height) {
            var y = 0
            placeables.forEach { p ->
                p.placeRelative(0, y)
                y += p.height + gap
            }
        }
    }
}

/** Breiten für eine Reihe (Stufe 1 oder 2) oder null, wenn nicht alle einzeilig passen. */
private fun rowWidths(natural: List<Int>, available: Int): List<Int>? {
    val n = natural.size
    return when {
        natural.max() * n <= available -> List(n) { i -> available / n + if (i < available % n) 1 else 0 }

        natural.sum() <= available -> {
            val extra = available - natural.sum()
            natural.mapIndexed { i, w -> w + extra / n + if (i < extra % n) 1 else 0 }
        }

        else -> null
    }
}

private class MeasuredRow(val placeables: List<Placeable>, val height: Int, val gap: Int) {
    fun place(scope: Placeable.PlacementScope, y: Int) = with(scope) {
        var x = 0
        placeables.forEach { p ->
            p.placeRelative(x, y + (height - p.height) / 2)
            x += p.width + gap
        }
    }
}

/** Misst eine Reihe mit festen Breiten; alle Buttons bekommen die Höhe des höchsten. */
private fun placeRow(measurables: List<Measurable>, widths: List<Int>, gap: Int): MeasuredRow {
    val rowHeight = measurables.indices.maxOf { measurables[it].minIntrinsicHeight(widths[it]) }
    val placeables = measurables.mapIndexed { i, m -> m.measure(Constraints.fixed(widths[i], rowHeight)) }
    return MeasuredRow(placeables, placeables.maxOf { it.height }, gap)
}
