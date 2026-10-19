package app.atemkraft.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Sizes

/**
 * Bündige Kante für Bedienelemente mit unsichtbarem Rand (LAYOUT-07): Das Element meldet sich
 * um [overhang] schmaler und ragt mit diesem Rand über seinen Platz hinaus – am Zeilenende
 * ([flushEnd]) nach rechts, am Zeilenanfang ([flushStart]) nach links (RTL gespiegelt). So steht
 * der Glyph eines Icon-Buttons an der Inhaltskante wie der Caret eines aufklappbaren Kopfs,
 * während die Tippfläche ihre 48 dp behält (A11Y-05). Kein `offset`: Nachbarn rücken mit, statt
 * überdeckt zu werden.
 *
 * Bei Icon-Buttons an einer Hülle (`Box(Modifier.flushEnd()) { AppIconButton(…) }`), nicht am
 * Button selbst: Dessen eigener Knoten behält so seine 48 dp im Layout (A11Y-05, `TIPPFLAECHE`
 * im ScreenshotTest). Der Überstand darf nicht in einem clippenden Elternteil liegen – Listen
 * tragen ihren Rand deshalb als `contentPadding`.
 *
 * Standard ist [Sizes.TouchOverhang] (Icon-Buttons); Text-Buttons reichen ihr Innenpolster herein.
 * In Karten höchstens [Dimens.CardPadding] ([inCard]), damit die Tippfläche nie über den
 * Kartenrand hinausragt, den die Karte abschneidet.
 */
fun Modifier.flushEnd(overhang: Dp = Sizes.TouchOverhang, inCard: Boolean = false): Modifier = flush(overhang, inCard, atStart = false)

/** Spiegelbild zu [flushEnd]: Der unsichtbare Rand ragt am Zeilenanfang über (Zurück-Pfeil, Text-Aktion). */
fun Modifier.flushStart(overhang: Dp = Sizes.TouchOverhang, inCard: Boolean = false): Modifier = flush(overhang, inCard, atStart = true)

private fun Modifier.flush(overhang: Dp, inCard: Boolean, atStart: Boolean): Modifier = layout { measurable, constraints ->
    val limit = if (inCard) minOf(overhang, Dimens.CardPadding) else overhang
    val px = limit.roundToPx()
    val placeable = measurable.measure(constraints)
    val width = (placeable.width - px).coerceAtLeast(0)
    layout(width, placeable.height) {
        // placeRelative spiegelt im RTL: der Überstand liegt dann rechts bzw. links.
        placeable.placeRelative(if (atStart) -px else 0, 0)
    }
}
