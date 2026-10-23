package app.atemkraft.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import app.atemkraft.ui.theme.Dimens

/**
 * Zwei Teile einer Zeile: [leading] links, [end] rechtsbündig. Passen beide in ihrer natürlichen
 * Breite nebeneinander (mit [spacing]), stehen sie in einer Zeile, mittig zueinander, und
 * [leading] bekommt den Rest der Breite; sonst steht [end] rechtsbündig unter [leading] – nichts
 * wird gekürzt oder zusammengedrückt (LAYOUT-03).
 *
 * [leadingReserve] und [endReserve] zeigen, was dort sonst noch stehen kann (andere Namen, längere
 * Werte), jeweils als Geschwister: Sie werden nur gemessen ([MeasureOnly]), nie gezeigt oder
 * vorgelesen. Die Entscheidung ein- oder zweizeilig gilt für die breitesten davon, damit die
 * Zeile nicht umspringt, wenn sich der Inhalt ändert, und was darunter steht, nicht mitrückt.
 *
 * [spacing] ist `GapTiny`: Beginnt [end] mit einem Stepper- oder Icon-Knopf, reicht dessen
 * Tippfläche 4 dp über den sichtbaren Kreis hinaus, sichtbar sind es dann `GapSmall`.
 */
@Composable
fun InlineOrBelow(
    leading: @Composable () -> Unit,
    end: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leadingReserve: @Composable () -> Unit = {},
    endReserve: @Composable () -> Unit = {},
    spacing: Dp = Dimens.GapTiny,
) {
    Layout(
        contents = listOf(leading, end, { MeasureOnly(leadingReserve) }, { MeasureOnly(endReserve) }),
        modifier = modifier.fillMaxWidth(),
    ) { (leadingMeasurables, endMeasurables, leadingReserves, endReserves), constraints ->
        val width = constraints.maxWidth
        val gap = spacing.roundToPx()
        val first = leadingMeasurables.single()
        val second = endMeasurables.single()
        fun widest(measurable: Measurable, reserves: List<Measurable>) = (reserves + measurable).maxOf { it.maxIntrinsicWidth(Constraints.Infinity) }
        val oneLine = widest(first, leadingReserves) + gap + widest(second, endReserves) <= width
        val endPlaceable = second.measure(Constraints(maxWidth = width))
        if (oneLine) {
            val leadingPlaceable = first.measure(Constraints(maxWidth = width - gap - endPlaceable.width))
            val height = maxOf(leadingPlaceable.height, endPlaceable.height)
            layout(width, height) {
                leadingPlaceable.placeRelative(0, (height - leadingPlaceable.height) / 2)
                endPlaceable.placeRelative(width - endPlaceable.width, (height - endPlaceable.height) / 2)
            }
        } else {
            val leadingPlaceable = first.measure(Constraints(maxWidth = width))
            layout(width, leadingPlaceable.height + endPlaceable.height) {
                leadingPlaceable.placeRelative(0, 0)
                endPlaceable.placeRelative(width - endPlaceable.width, leadingPlaceable.height)
            }
        }
    }
}
