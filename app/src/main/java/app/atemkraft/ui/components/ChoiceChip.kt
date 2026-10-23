package app.atemkraft.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SegmentSelected
import app.atemkraft.ui.theme.Sizes

/** Test-Tag der Reihe aus [ChoiceChip]s: Der ScreenshotTest prüft, dass kein Chip beim Markieren oder Abblenden verrutscht. */
const val CHOICE_CHIPS_TAG = "choice-chips"

/**
 * Auswählbarer Chip (Anlässe im Tab „Atmen“): sichtbar [Sizes.ButtonHeight] hoch wie die Segmente
 * einer `SegmentedChoiceRow` (auch eine Auswahl), Tippfläche 48 dp; Form `shapes.small`, Polster
 * waagerecht [Dimens.ChipPadding], Beschriftung `labelLarge` in einer Zeile – nie verkleinert,
 * nie umbrochen: Der Chip ist so breit wie seine Beschriftung; reicht der Platz nicht, scrollt
 * die [ChoiceChipRow] (Nutzer 2026-10-23).
 *
 * **Misst in jedem Zustand gleich** (Nutzer 2026-10-23: Chips verrutschen nie). Gewählt füllt sich
 * der Chip ganz mit [SegmentSelected], Rand in derselben Farbe – wie ein gewähltes Segment, ein
 * ruhiger Farbton statt Lila mit pinkem Rand (Nutzer 2026-10-23: „das Highlighting ist nicht so
 * schön“). Fläche und Rand gleiten über (Default-Spring, MOTION-04). Alles gezeichnet, nichts
 * davon geht ins Layout. Keine fette Schrift, kein Häkchen, kein ✕: Sie änderten die Breite und
 * damit den Umbruch der Reihe. Nicht gewählte Chips bleiben in der Suche unverändert, nicht
 * abgeblendet: Die gefüllten Treffer heben sich genug ab.
 *
 * Kein Warnton: Alle Chips sehen gleich aus (Nutzer 2026-10-23); die Warnung einer intensiven
 * Wahl steht erst als ⚠-Zeile in der Empfehlung darunter ([WarnText]).
 *
 * TalkBack: Rolle Optionsfeld, „ausgewählt“/„nicht ausgewählt“; die Reihe ist eine
 * [ChoiceChipRow] (`selectableGroup`).
 */
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.small
    val fill by animateColorAsState(if (selected) SegmentSelected else Color.Transparent, label = "chipFill")
    val border by animateColorAsState(if (selected) SegmentSelected else MaterialTheme.colorScheme.outline, label = "chipBorder")
    Box(
        modifier = modifier
            // Reihenfolge wie AppIconButton (LAYOUT-06): erst die 48-dp-Tippfläche, dann die sichtbare Pille.
            .minimumInteractiveComponentSize()
            .heightIn(min = Sizes.ButtonHeight)
            .clip(shape)
            .background(fill)
            .border(Dimens.BorderChip, border, shape)
            .selectable(selected = selected, role = Role.RadioButton, interactionSource = null, indication = ripple(), onClick = onClick)
            // Fest, damit die Rundung keine Glyphen anschneidet (LAYOUT-03); schmaler als ein
            // M3-Chip (16), damit die sechs Anlässe auf 360 dp in zwei Zeilen passen.
            .padding(horizontal = Dimens.ChipPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.testTag(CHIP_TEXT_TAG),
        )
    }
}

/**
 * Reihe aus [ChoiceChip]s, **immer höchstens zwei Zeilen** (Nutzer 2026-10-23, per Mockup):
 * nebeneinander `GapSmall`; untereinander kein Layout-Abstand – die 48-dp-Tippflächen der 40 dp
 * hohen Chips lassen sichtbar je 4 dp über und unter jedem Chip, also ebenfalls 8 dp.
 *
 * - **Passt alles in zwei Zeilen**, stehen die Chips in ihrer Reihenfolge wie in einer `FlowRow`;
 *   nichts scrollt.
 * - **Sonst** (kleiner Bildschirm, große Systemschrift) bleiben es genau zwei Zeilen, die
 *   gemeinsam seitlich scrollen. Die Chips wechseln dann spaltenweise ab (1 oben, 2 unten, 3
 *   oben …), damit die Anfänge beider Zeilen sichtbar sind. Die Fläche reicht um [endBleed] über
 *   den Rand am Ende hinaus (in einer Karte deren Polster: Der angeschnittene Chip endet an der
 *   Kartenkante); ganz gescrollt steht der letzte Chip wieder an der Inhaltskante. An der Seite,
 *   auf der noch Chips liegen, läuft der Inhalt in einem Verlauf ([Dimens.ScrollEdgeFade]) in
 *   Transparenz aus – die Kartenfläche scheint durch; am Ende kein Verlauf.
 *
 * Nie ein Umbruch im Chip, nie kleinere Schrift, nie eine dritte Zeile.
 *
 * TalkBack und Tastatur: Auswahlgruppe [groupName] (`selectableGroup`) und eigene
 * Traversal-Gruppe; die Chips folgen auch im Scrollfall ihrer Reihenfolge in [items]
 * (`traversalIndex`, Kompositionsreihenfolge), ein fokussierter Chip scrollt von selbst ins Bild.
 * [chip] baut den Chip zu einem Eintrag und hängt den übergebenen Modifier `order` an ihn.
 */
@Composable
fun <T> ChoiceChipRow(
    groupName: String,
    items: List<T>,
    modifier: Modifier = Modifier,
    endBleed: Dp = 0.dp,
    chip: @Composable (item: T, order: Modifier) -> Unit,
) {
    val scroll = rememberScrollState()
    val bleed = with(LocalDensity.current) { endBleed.roundToPx() }
    BoxWithConstraints(modifier = modifier.bleedEnd(endBleed)) {
        // Platz für die Chips ohne Scrollen: bis zur Inhaltskante, nicht in den Überstand.
        val limit = (constraints.maxWidth - bleed).coerceAtLeast(0)
        Box(
            Modifier
                .fillMaxWidth()
                .edgeFade(scroll)
                .horizontalScroll(scroll),
        ) {
            Layout(
                modifier = Modifier
                    .testTag(CHOICE_CHIPS_TAG)
                    .selectableGroup()
                    .semantics {
                        contentDescription = groupName
                        isTraversalGroup = true
                    },
                content = {
                    items.forEachIndexed { i, item -> chip(item, Modifier.semantics { traversalIndex = i.toFloat() }) }
                },
            ) { measurables, constraints ->
                val gap = Dimens.GapSmall.roundToPx()
                val chips = measurables.map { it.measure(Constraints()) }
                val widths = chips.map { it.width }
                val flow = flowLines(widths, limit, gap)
                val fits = flow.size <= 2 && widths.all { it <= limit }
                val lines = if (fits) flow else listOf(widths.indices.filter { it % 2 == 0 }, widths.indices.filter { it % 2 == 1 })
                val lineWidth = lines.maxOfOrNull { line -> line.sumOf { widths[it] } + gap * (line.size - 1).coerceAtLeast(0) } ?: 0
                val heights = lines.map { line -> line.maxOfOrNull { chips[it].height } ?: 0 }
                // Im Scrollfall reicht der Inhalt um den Überstand weiter: Ganz gescrollt steht der letzte Chip an der Inhaltskante.
                val width = maxOf(if (fits) lineWidth else lineWidth + bleed, constraints.minWidth)
                val xs = IntArray(chips.size)
                val ys = IntArray(chips.size)
                var y = 0
                lines.forEachIndexed { row, line ->
                    var x = 0
                    line.forEach { i ->
                        xs[i] = x
                        ys[i] = y + (heights[row] - chips[i].height) / 2
                        x += widths[i] + gap
                    }
                    y += heights[row]
                }
                layout(width, heights.sum()) {
                    // In der Reihenfolge der Einträge platzieren, nicht zeilenweise: Danach richtet
                    // sich die Tastatur-Fokusfolge (Tab), auch wenn die Chips abwechselnd stehen.
                    chips.forEachIndexed { i, chip -> chip.placeRelative(xs[i], ys[i]) }
                }
            }
        }
    }
}

/** Zeilen wie eine `FlowRow`: Indizes der Chips je Zeile; ein Chip beginnt eine neue Zeile, wenn er nicht mehr passt. */
private fun flowLines(widths: List<Int>, limit: Int, gap: Int): List<List<Int>> {
    val lines = mutableListOf<MutableList<Int>>()
    var x = 0
    widths.forEachIndexed { i, w ->
        val line = lines.lastOrNull()
        if (line != null && x + gap + w <= limit) {
            line += i
            x += gap + w
        } else {
            lines += mutableListOf(i)
            x = w
        }
    }
    return lines
}

/**
 * Meldet sich um [bleed] schmaler, als es misst, und ragt damit am Ende über das Polster des
 * Behälters hinaus (wie [flushEnd], nur wächst hier der Inhalt in den Rand). Der Behälter
 * schneidet an seiner Kante ab – die Karte an ihrem Rand.
 */
private fun Modifier.bleedEnd(bleed: Dp): Modifier = layout { measurable, constraints ->
    val px = bleed.roundToPx()
    val wide = if (constraints.hasBoundedWidth) constraints.copy(maxWidth = constraints.maxWidth + px) else constraints
    val placeable = measurable.measure(wide)
    layout((placeable.width - px).coerceAtLeast(0), placeable.height) { placeable.placeRelative(0, 0) }
}

/**
 * Weicher Rand einer seitlich scrollenden Fläche: An der Seite, auf der noch Inhalt liegt, läuft
 * er über [Dimens.ScrollEdgeFade] in Transparenz aus (eigene Ebene, `BlendMode.DstIn`) – die
 * Fläche darunter scheint durch, gleich welche Farbe sie hat. Am Ende des Inhalts kein Verlauf.
 */
private fun Modifier.edgeFade(state: ScrollState): Modifier = graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val fade = Dimens.ScrollEdgeFade.toPx().coerceAtMost(size.width / 2)
        // Rückwärts liegt der Anfang: links, im RTL rechts.
        val ltr = layoutDirection == LayoutDirection.Ltr
        val leftOpen = if (ltr) state.canScrollBackward else state.canScrollForward
        val rightOpen = if (ltr) state.canScrollForward else state.canScrollBackward
        if (leftOpen) {
            drawRect(
                brush = Brush.horizontalGradient(listOf(Color.Transparent, Color.Black), startX = 0f, endX = fade),
                size = Size(fade, size.height),
                blendMode = BlendMode.DstIn,
            )
        }
        if (rightOpen) {
            drawRect(
                brush = Brush.horizontalGradient(listOf(Color.Black, Color.Transparent), startX = size.width - fade, endX = size.width),
                topLeft = Offset(size.width - fade, 0f),
                size = Size(fade, size.height),
                blendMode = BlendMode.DstIn,
            )
        }
    }

@Preview
@Composable
private fun ChoiceChipPreview() {
    AtemkraftTheme {
        ChoiceChipRow(groupName = "Anlass", items = (1..6).toList()) { n, order ->
            ChoiceChip(label = "Anlass $n", selected = n == 3, onClick = {}, modifier = order)
        }
    }
}
