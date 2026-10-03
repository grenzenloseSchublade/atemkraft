package app.atemkraft.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Gemeinsames Gerüst der laufenden Atem- und Meditations-Session: Kopf oben, Kreis mittig,
 * Steuerung unten. Der Kreis wird nach dem Platz bemessen, der zwischen Kopf und Steuerung
 * wirklich frei ist – bei großer Schrift oder untereinander gestapelten Buttons wird er kleiner,
 * statt von ihnen überdeckt zu werden (LAYOUT-03).
 *
 * Der Kopf darf wachsen (aufgeklappte Anleitung) und legt sich dann über den Kreisrand, statt
 * den Kreis zu verschieben: Reserviert wird nur seine eingeklappte Höhe.
 *
 * @param centerExtra Höhe, die [center] zusätzlich zum Kreis braucht (z. B. „Als Nächstes“).
 * @param center Inhalt mit Kreis; bekommt die Kantenlänge des Kreises.
 */
@Composable
fun SessionRunningLayout(
    modifier: Modifier = Modifier,
    centerExtra: Dp = 0.dp,
    top: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
    center: @Composable (circleSide: Dp) -> Unit,
) {
    // Kleinste je gemessene Kopfhöhe = eingeklappter Zustand; pro Fenstergröße neu.
    val collapsedTop = remember { intArrayOf(Int.MAX_VALUE, -1, -1) }
    SubcomposeLayout(modifier) { constraints ->
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        if (collapsedTop[1] != w || collapsedTop[2] != h) {
            collapsedTop[0] = Int.MAX_VALUE
            collapsedTop[1] = w
            collapsedTop[2] = h
        }
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val topP = subcompose("top", top).map { it.measure(loose) }
        val bottomP = subcompose("bottom", bottom).map { it.measure(loose) }
        val topH = topP.maxOfOrNull { it.height } ?: 0
        val bottomH = bottomP.maxOfOrNull { it.height } ?: 0
        collapsedTop[0] = min(collapsedTop[0], topH)
        val reservedTop = collapsedTop[0]

        val gap = 16.dp.roundToPx()
        val free = h - reservedTop - bottomH - centerExtra.roundToPx() - 2 * gap
        val side = minOf(w * 0.9f, h * 0.62f, free.toFloat()).coerceAtLeast(96.dp.toPx())
        val centerP = subcompose("center") { center(side.toDp()) }.map { it.measure(loose) }
        val centerH = centerP.maxOfOrNull { it.height } ?: 0

        layout(w, h) {
            // Mitte des freien Bereichs zwischen eingeklapptem Kopf und Steuerung.
            val regionTop = reservedTop + gap
            val regionBottom = h - bottomH - gap
            val y = (regionTop + (regionBottom - regionTop - centerH) / 2).coerceAtLeast(regionTop)
            centerP.forEach { it.placeRelative((w - it.width) / 2, y) }
            bottomP.forEach { it.placeRelative((w - it.width) / 2, h - it.height) }
            // Kopf zuletzt und mit zIndex: liegt über dem Kreis, falls er aufgeklappt hineinragt.
            topP.forEach { it.placeRelative((w - it.width) / 2, 0, zIndex = 1f) }
        }
    }
}

