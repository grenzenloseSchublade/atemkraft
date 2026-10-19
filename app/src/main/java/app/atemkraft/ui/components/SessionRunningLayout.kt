package app.atemkraft.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Gemeinsames Gerüst der laufenden Atem- und Meditations-Session: Kopf oben, Kreis mittig,
 * Steuerung unten. Der Kreis wird nach dem Platz bemessen, der zwischen Kopf und Steuerung
 * wirklich frei ist – bei großer Schrift oder untereinander gestapelten Buttons wird er kleiner,
 * statt von ihnen überdeckt zu werden (LAYOUT-03).
 *
 * Im Querformat (breiter als hoch) zwei Spalten: links der Kreis mit allem darunter über die
 * ganze Höhe, rechts oben der Kopf und unten die Steuerung. Übereinander wäre dort kein Platz für
 * Kopf, Kreis, Anleitungszeile und Knöpfe.
 *
 * Der Kopf darf wachsen (aufgeklappte Anleitung) und legt sich dann über den Kreisrand, statt
 * den Kreis zu verschieben: Reserviert wird nur seine eingeklappte Höhe.
 *
 * @param centerExtra Höhe, die [center] zusätzlich zum Kreis braucht (z. B. Anleitungszeile und
 *   „Als Nächstes“ unter dem Kreis).
 * @param chromeVisible false blendet Kopf und Steuerung sanft aus (Ruhe-Ansicht, [HideableSlot]);
 *   gemessen werden sie weiter, der Kreis bleibt also, wo er ist. Sichtbar fangen beide Tipps
 *   neben ihren Knöpfen ab, damit ein knapp verfehlter Knopf nicht alles ausblendet.
 * @param center Inhalt mit Kreis; bekommt die Kantenlänge des Kreises.
 */
@Composable
fun SessionRunningLayout(
    top: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    centerExtra: Dp = 0.dp,
    chromeVisible: Boolean = true,
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
        if (w > h) {
            sidewaysLayout(w, h, loose, centerExtra, chromeVisible, top, bottom, center)
        } else {
            val topP = subcompose("top") { HideableSlot(chromeVisible, absorbTaps = true, content = top) }.map { it.measure(loose) }
            val bottomP = subcompose("bottom") { HideableSlot(chromeVisible, absorbTaps = true, content = bottom) }.map { it.measure(loose) }
            val topH = topP.maxOfOrNull { it.height } ?: 0
            val bottomH = bottomP.maxOfOrNull { it.height } ?: 0
            collapsedTop[0] = min(collapsedTop[0], topH)
            val reservedTop = collapsedTop[0]

            val gap = GAP.roundToPx()
            val free = h - reservedTop - bottomH - centerExtra.roundToPx() - 2 * gap
            val side = minOf(w * 0.9f, h * 0.62f, free.toFloat()).coerceAtLeast(MIN_SIDE.toPx())
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
}

/** Querformat: links Kreis samt Zeilen darunter, rechts oben Kopf, unten Steuerung. */
private fun SubcomposeMeasureScope.sidewaysLayout(
    w: Int,
    h: Int,
    loose: Constraints,
    centerExtra: Dp,
    chromeVisible: Boolean,
    top: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
    center: @Composable (circleSide: Dp) -> Unit,
): MeasureResult {
    val gap = GAP.roundToPx()
    val leftW = (w - gap) / 2
    val rightW = w - leftW - gap
    val right = loose.copy(maxWidth = rightW)
    val topP = subcompose("top") { HideableSlot(chromeVisible, absorbTaps = true, content = top) }.map { it.measure(right) }
    val bottomP = subcompose("bottom") { HideableSlot(chromeVisible, absorbTaps = true, content = bottom) }.map { it.measure(right) }
    val free = h - centerExtra.roundToPx() - 2 * gap
    val side = minOf(leftW * 0.9f, free.toFloat()).coerceAtLeast(MIN_SIDE.toPx())
    val centerP = subcompose("center") { center(side.toDp()) }.map { it.measure(loose.copy(maxWidth = leftW)) }
    val centerH = centerP.maxOfOrNull { it.height } ?: 0
    return layout(w, h) {
        val y = ((h - centerH) / 2).coerceAtLeast(0)
        centerP.forEach { it.placeRelative((leftW - it.width) / 2, y) }
        val rightX = leftW + gap
        bottomP.forEach { it.placeRelative(rightX + (rightW - it.width) / 2, h - it.height) }
        topP.forEach { it.placeRelative(rightX + (rightW - it.width) / 2, 0, zIndex = 1f) }
    }
}

/** Abstand zwischen Kopf, Kreis und Steuerung (bzw. den Spalten im Querformat). */
private val GAP = 16.dp

/** Kleinste Kantenlänge des Kreises; darunter wäre er nicht mehr lesbar. */
private val MIN_SIDE = 96.dp
