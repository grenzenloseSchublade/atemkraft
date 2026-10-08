package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import app.atemkraft.ui.theme.Dimens

/**
 * Titel mit nachgestellten Chips (Evidenz-Tag, Charakter, Info). Als FlowRow: passt ein Chip
 * nicht mehr neben den Titel, rutscht er in die nächste Zeile. Eine `Row` mit
 * `weight(fill = false)` misst die Chips zuerst und lässt dem Titel nur den Rest – bei 384 dp
 * und fontScale 1,1 brach so „Resonanz-Atmung“ mitten im Wort um.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TitleWithChips(
    title: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    chips: (@Composable RowScope.() -> Unit)? = null,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
        verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny),
    ) {
        WholeWordText(
            text = title,
            style = style,
            color = color,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        // null statt leerer Row: ein leeres Item bekäme sonst Abstand bzw. eine Leerzeile.
        if (chips == null) return@FlowRow
        // Chips als Gruppe: rutschen gemeinsam um und bleiben zum Titel vertikal zentriert
        // (FlowRow kennt in foundation 1.7 noch kein itemVerticalAlignment). Innen wieder eine
        // FlowRow: Passen zwei Chips (Studienlage + Vorsicht) nicht in eine Zeile, bricht die
        // Zeile zwischen den Chips um statt im Text des zweiten Chips.
        FlowRow(
            modifier = Modifier.align(Alignment.CenterVertically),
            horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
            verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny),
        ) { chips() }
    }
}
