package app.atemkraft.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density

/**
 * Spalte für den ersten Bildschirm einer Scroll-Seite (LAYOUT-08, MUSTER-10): [top] und [bottom]
 * in ihrer natürlichen Höhe, dazwischen [center] in voller Breite mit der Höhe, die
 * [centerHeight] für den freien Platz der Ansicht nennt – `viewportHeight − top − bottom` in px,
 * [Constraints.Infinity], wenn die Ansicht unbegrenzt ist. Wird das Ganze höher als die Ansicht,
 * scrollt der Aufrufer (Eltern-`verticalScroll`, in dem die Höhe sonst unbegrenzt wäre) – so wird
 * nie etwas abgeschnitten.
 *
 * Tab Meditation: der Zeitring füllt den Platz, mindestens seine Mindestgröße. Muster-Editor:
 * Werte, Kurve, Vorschau und Werkzeugleiste – die Kurve so hoch, dass alles passt, zwischen ihrer
 * Mindest- und Wunschhöhe.
 *
 * [bottomReserve] (px) bleibt unter [bottom] frei, solange [center] dann noch hineinpasst; sonst
 * gibt die Spalte ihn als Erstes her. Der Editor hält so die Tippfläche des Griffs der Tab-Leiste
 * über der Startleiste frei, ohne die Mindesthöhe der Kurve zu opfern (MUSTER-10).
 */
@Composable
fun FillViewportColumn(
    viewportHeight: Int,
    centerHeight: Density.(free: Int) -> Int,
    top: @Composable () -> Unit,
    center: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    bottomReserve: Int = 0,
) {
    Layout(contents = listOf(top, center, bottom), modifier = modifier) { (topSlot, centerSlot, bottomSlot), constraints ->
        val width = constraints.maxWidth
        val loose = Constraints(maxWidth = width)
        val heads = topSlot.map { it.measure(loose) }
        val feet = bottomSlot.map { it.measure(loose) }
        fun List<Placeable>.height() = sumOf { it.height }
        val free = if (viewportHeight == Constraints.Infinity) Constraints.Infinity else viewportHeight - heads.height() - feet.height()
        // Mit Reserve, wenn die Mitte dann noch passt; sonst ohne.
        val reserved = if (free == Constraints.Infinity || bottomReserve <= 0) 0 else bottomReserve
        val withReserve = if (reserved > 0) centerHeight(free - reserved) else 0
        val reserve = if (reserved > 0 && withReserve <= free - reserved) reserved else 0
        val middle = if (reserve > 0) withReserve else centerHeight(free)
        val centers = centerSlot.map { it.measure(Constraints.fixed(width, middle)) }
        layout(width, heads.height() + middle + feet.height() + reserve) {
            var y = 0
            heads.forEach {
                it.placeRelative(0, y)
                y += it.height
            }
            centers.forEach { it.placeRelative(0, y) }
            y += middle
            feet.forEach {
                it.placeRelative(0, y)
                y += it.height
            }
        }
    }
}
