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
 * Alle Buttons einer Reihe teilen die Zeilenhöhe (einschließlich 48-dp-Tippfläche); sichtbar
 * sind sie gleich hoch, weil ihre Beschriftungen einzeilig sind und dieselbe Mindesthöhe haben.
 *
 * [leading] ist eine kurze Beschriftung vor der Reihe (Muster-Editor: „Neu nach 2:“). Passen
 * Beschriftung (einzeilig) und Buttons nach Stufe 1 oder 2 nebeneinander, steht sie links davor,
 * mittig zur Reihe; sonst steht sie in voller Breite darüber (darf dort zwischen Wörtern
 * umbrechen) und die Buttons verteilen sich darunter wie ohne sie.
 */
@Composable
fun AdaptiveButtonRow(
    modifier: Modifier = Modifier,
    spacing: Dp = Dimens.ListGap,
    leading: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Layout(contents = listOf(leading ?: {}, content), modifier = modifier) { (leadingMeasurables, measurables), constraints ->
        val gap = spacing.roundToPx()
        val n = measurables.size
        val width = constraints.maxWidth
        val label = leadingMeasurables.firstOrNull()
        if (n == 0) {
            val placeable = label?.measure(Constraints(maxWidth = width))
            return@Layout layout(placeable?.width ?: 0, placeable?.height ?: 0) { placeable?.placeRelative(0, 0) }
        }
        val natural = measurables.map { it.maxIntrinsicWidth(Constraints.Infinity) }

        // Beschriftung links vor der Reihe, wenn beides in eine Zeile passt.
        if (label != null) {
            val labelWidth = label.maxIntrinsicWidth(Constraints.Infinity)
            val inline = rowWidths(natural, width - labelWidth - gap - gap * (n - 1))
            if (inline != null) {
                val row = placeRow(measurables, inline, gap)
                val labelPlaceable = label.measure(Constraints(maxWidth = labelWidth))
                val height = maxOf(row.height, labelPlaceable.height)
                return@Layout layout(width, height) {
                    labelPlaceable.placeRelative(0, (height - labelPlaceable.height) / 2)
                    row.place(this, (height - row.height) / 2, x0 = labelWidth + gap)
                }
            }
        }
        val labelPlaceable = label?.measure(Constraints(maxWidth = width))
        val top = labelPlaceable?.let { it.height + gap } ?: 0
        val available = width - gap * (n - 1)

        val widths = rowWidths(natural, available)
        if (widths != null) {
            val row = placeRow(measurables, widths, gap)
            return@Layout layout(width, top + row.height) {
                labelPlaceable?.placeRelative(0, 0)
                row.place(this, top)
            }
        }

        // Stufe 3: Primär-Button oben, Rest als Reihe darunter.
        if (n > 2) {
            val restWidths = rowWidths(natural.drop(1), width - gap * (n - 2))
            if (restWidths != null) {
                val first = measurables[0].measure(Constraints.fixedWidth(width))
                val rest = placeRow(measurables.drop(1), restWidths, gap)
                val height = top + first.height + gap + rest.height
                return@Layout layout(width, height) {
                    labelPlaceable?.placeRelative(0, 0)
                    first.placeRelative(0, top)
                    rest.place(this, top + first.height + gap)
                }
            }
        }

        // Stufe 4: alle untereinander.
        val placeables = measurables.map { it.measure(Constraints.fixedWidth(width)) }
        val height = top + placeables.sumOf { it.height } + gap * (n - 1)
        layout(width, height) {
            labelPlaceable?.placeRelative(0, 0)
            var y = top
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
    fun place(scope: Placeable.PlacementScope, y: Int, x0: Int = 0) = with(scope) {
        var x = x0
        placeables.forEach { p ->
            p.placeRelative(x, y + (height - p.height) / 2)
            x += p.width + gap
        }
    }
}

/**
 * Misst eine Reihe mit festen Breiten. Die Höhe ist nur nach oben begrenzt: Eine feste Höhe
 * würde die minIntrinsicHeight samt 48-dp-Tippfläche an die Buttons durchreichen und sie
 * sichtbar 48 dp hoch machen statt `Sizes.ButtonHeight`.
 */
private fun placeRow(measurables: List<Measurable>, widths: List<Int>, gap: Int): MeasuredRow {
    val rowHeight = measurables.indices.maxOf { measurables[it].minIntrinsicHeight(widths[it]) }
    val placeables = measurables.mapIndexed { i, m ->
        m.measure(Constraints(minWidth = widths[i], maxWidth = widths[i], minHeight = 0, maxHeight = rowHeight))
    }
    return MeasuredRow(placeables, placeables.maxOf { it.height }, gap)
}
