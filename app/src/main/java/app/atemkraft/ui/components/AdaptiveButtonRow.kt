package app.atemkraft.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import app.atemkraft.ui.theme.Dimens

/**
 * Reihe von Buttons, deren Beschriftung nie umbricht (LAYOUT-03); der erste gilt als Primäraktion,
 * außer [primaryFirst] ist aus (gleichrangige Buttons). Gemessen wird die
 * einzeilige Breite jedes Buttons (`maxIntrinsicWidth`):
 * 1. passen alle in gleich breite Spalten → gleich breit (Normalfall, ruhiges Bild),
 * 2. passen sie nur nach Inhalt verteilt → jeder bekommt seine Breite plus gleichen Anteil am Rest,
 * 3. sonst, wenn die übrigen nebeneinander passen → erster (Primär-)Button allein in voller
 *    Breite, die übrigen darunter in einer Reihe (spart eine Zeile Höhe) – nur mit
 *    [primaryFirst]; ohne ihn (Buttons ohne Rangfolge, Muster-Editor: + Ein / + Halten / + Aus)
 *    sähe der erste wie eine Hauptaktion aus, dort geht es direkt zu Stufe 4,
 * 4. sonst → alle untereinander in voller Breite, in Lesereihenfolge.
 * Alle Buttons einer Reihe teilen die Zeilenhöhe (einschließlich 48-dp-Tippfläche); sichtbar
 * sind sie gleich hoch, weil ihre Beschriftungen einzeilig sind und dieselbe Mindesthöhe haben.
 *
 * [leading] ist eine kurze Beschriftung vor der Reihe (Muster-Editor: „Nach Phase 2:“). Passen
 * Beschriftung (einzeilig) und Buttons nach Stufe 1 oder 2 nebeneinander, steht sie links davor,
 * mittig zur Reihe; sonst steht sie in voller Breite darüber (darf dort zwischen Wörtern
 * umbrechen) und die Buttons verteilen sich darunter wie ohne sie. Gemessen wird dafür die
 * einzeilige Breite von [leading]: Was dort stehen kann, reserviert der Aufrufer darin (z. B.
 * `widthIn(min = rememberWidestTextWidth(…))`), damit die Reihe nicht umspringt.
 *
 * [leadingStacked] ist der Alternativinhalt, wenn [leading] nicht in die Zeile passt: Er steht
 * dann statt [leading] in voller Breite über den Buttons (Muster-Editor: „Nach Phase 2:“ links in
 * der gestrichelten Linie). [aboveInline] steht nur in der einzeiligen Anordnung über der Reihe
 * (Muster-Editor: die gestrichelte Linie allein, die sonst [leadingStacked] trägt). Beide sind
 * vom Inhalt [spacing] entfernt wie die Beschriftung darüber.
 */
@Composable
fun AdaptiveButtonRow(
    modifier: Modifier = Modifier,
    spacing: Dp = Dimens.ListGap,
    leading: (@Composable () -> Unit)? = null,
    leadingStacked: (@Composable () -> Unit)? = null,
    aboveInline: (@Composable () -> Unit)? = null,
    primaryFirst: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (leading == null || (leadingStacked == null && aboveInline == null)) {
        Layout(contents = listOf(leading ?: {}, content), modifier = modifier) { (leadingMeasurables, measurables), constraints ->
            val label = leadingMeasurables.firstOrNull()
            arrangeButtons(
                measurables = measurables,
                constraints = constraints,
                gap = spacing.roundToPx(),
                primaryFirst = primaryFirst,
                labelWidth = label?.maxIntrinsicWidth(Constraints.Infinity),
                inlineLabel = { width -> label?.measure(Constraints(maxWidth = width)) },
                above = { null },
                stackedLabel = { width -> label?.measure(Constraints(maxWidth = width)) },
            )
        }
        return
    }
    // Mit Alternativen wird nur komponiert, was zu sehen ist (ein Knoten je Text für TalkBack und
    // die Screenshot-Prüfung); die Beschriftung misst vorab eine Probe ([widestOf]).
    SubcomposeLayout(modifier) { constraints ->
        val gap = spacing.roundToPx()
        val loose = { width: Int -> Constraints(maxWidth = width) }
        arrangeButtons(
            measurables = subcompose(AdaptiveSlot.CONTENT, content),
            constraints = constraints,
            gap = gap,
            primaryFirst = primaryFirst,
            labelWidth = widestOf(AdaptiveSlot.PROBE, leading),
            inlineLabel = { width -> subcompose(AdaptiveSlot.LEADING, leading).firstOrNull()?.measure(loose(width)) },
            above = { width -> aboveInline?.let { subcompose(AdaptiveSlot.ABOVE, it).firstOrNull()?.measure(loose(width)) } },
            stackedLabel = { width -> subcompose(AdaptiveSlot.STACKED, leadingStacked ?: leading).firstOrNull()?.measure(loose(width)) },
        )
    }
}

/** Teile von [AdaptiveButtonRow] mit Alternativen. */
private enum class AdaptiveSlot { CONTENT, PROBE, LEADING, ABOVE, STACKED }

/**
 * Die Stufen von [AdaptiveButtonRow] für [measurables]. [labelWidth] ist die einzeilige Breite der
 * Beschriftung (`null` ohne); [inlineLabel], [above] und [stackedLabel] messen erst, wenn ihre
 * Anordnung gilt – jeweils für die übergebene Breite.
 */
private fun MeasureScope.arrangeButtons(
    measurables: List<Measurable>,
    constraints: Constraints,
    gap: Int,
    primaryFirst: Boolean,
    labelWidth: Int?,
    inlineLabel: (Int) -> Placeable?,
    above: (Int) -> Placeable?,
    stackedLabel: (Int) -> Placeable?,
): MeasureResult {
    val n = measurables.size
    val width = constraints.maxWidth
    if (n == 0) {
        val placeable = labelWidth?.let { stackedLabel(width) }
        return layout(placeable?.width ?: 0, placeable?.height ?: 0) { placeable?.placeRelative(0, 0) }
    }
    val natural = measurables.map { it.maxIntrinsicWidth(Constraints.Infinity) }

    // Beschriftung links vor der Reihe, wenn beides in eine Zeile passt.
    if (labelWidth != null) {
        val inline = rowWidths(natural, width - labelWidth - gap - gap * (n - 1))
        if (inline != null) {
            val abovePlaceable = above(width)
            val y0 = abovePlaceable?.let { it.height + gap } ?: 0
            val row = placeRow(measurables, inline, gap)
            val labelPlaceable = checkNotNull(inlineLabel(labelWidth))
            val height = maxOf(row.height, labelPlaceable.height)
            return layout(width, y0 + height) {
                abovePlaceable?.placeRelative(0, 0)
                labelPlaceable.placeRelative(0, y0 + (height - labelPlaceable.height) / 2)
                row.place(this, y0 + (height - row.height) / 2, x0 = labelWidth + gap)
            }
        }
    }
    val labelPlaceable = labelWidth?.let { stackedLabel(width) }
    val top = labelPlaceable?.let { it.height + gap } ?: 0
    val available = width - gap * (n - 1)

    val widths = rowWidths(natural, available)
    if (widths != null) {
        val row = placeRow(measurables, widths, gap)
        return layout(width, top + row.height) {
            labelPlaceable?.placeRelative(0, 0)
            row.place(this, top)
        }
    }

    // Stufe 3: Primär-Button oben, Rest als Reihe darunter.
    if (primaryFirst && n > 2) {
        val restWidths = rowWidths(natural.drop(1), width - gap * (n - 2))
        if (restWidths != null) {
            val first = measurables[0].measure(Constraints.fixedWidth(width))
            val rest = placeRow(measurables.drop(1), restWidths, gap)
            val height = top + first.height + gap + rest.height
            return layout(width, height) {
                labelPlaceable?.placeRelative(0, 0)
                first.placeRelative(0, top)
                rest.place(this, top + first.height + gap)
            }
        }
    }

    // Stufe 4: alle untereinander.
    val placeables = measurables.map { it.measure(Constraints.fixedWidth(width)) }
    val height = top + placeables.sumOf { it.height } + gap * (n - 1)
    return layout(width, height) {
        labelPlaceable?.placeRelative(0, 0)
        var y = top
        placeables.forEach { p ->
            p.placeRelative(0, y)
            y += p.height + gap
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
