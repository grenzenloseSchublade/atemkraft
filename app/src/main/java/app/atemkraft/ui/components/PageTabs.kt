package app.atemkraft.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes

/**
 * Tabs in der Seite (Eigentümer 2026-10-31, Tab „Meins“: „Eigene Kreationen 5 · Tut mir gut 7“):
 * Namen nebeneinander, der gewählte in `primary` (Pink = Auswahl, FARBE-04; 5,98 auf dem Grund) mit
 * einem pinken Strich darunter ([Sizes.PageTabIndicator]), die anderen gedämpft (`onSurface`
 * `SECONDARY`). Hinter dem Namen leise die Zahl ([counts], `bodySmall` `SECONDARY`). Name
 * `titleSmall` wie das Gruppenlabel, das Gewicht bleibt bei Auswahl gleich (TYPO-01). Jeder Tab ist
 * mindestens `MinTouchTarget` hoch, `Role.Tab` mit Auswahlzustand in einer `selectableGroup`;
 * TalkBack hört „Eigene Kreationen, 5“ – Auswahl ohne Häkchen (A11Y-03).
 *
 * Passen nicht alle Tabs in eine Zeile (große Schrift), rückt der nächste ganz in die nächste Zeile
 * (`FlowRow`); ein Name bricht nie (LAYOUT-03). Nicht für Werte (Dauer, Klang): dafür
 * [SegmentedChoiceRow].
 */
@Composable
fun <T> PageTabs(
    items: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    counts: (T) -> Int? = { null },
) {
    FlowRow(
        modifier = modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
    ) {
        items.forEach { item ->
            PageTab(name = label(item), count = counts(item), selected = item == selected, onClick = { onSelect(item) })
        }
    }
}

@Composable
private fun PageTab(name: String, count: Int?, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY)
    val spoken = if (count != null) "$name, $count" else name
    var labelWidth by remember { mutableIntStateOf(0) }
    Column(
        modifier = Modifier
            .heightIn(min = Dimens.MinTouchTarget)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = spoken },
        verticalArrangement = Arrangement.Bottom,
    ) {
        Row(modifier = Modifier.onSizeChanged { labelWidth = it.width }, verticalAlignment = Alignment.Bottom) {
            WholeWordText(text = name, style = MaterialTheme.typography.titleSmall, color = color)
            if (count != null) {
                Spacer(Modifier.width(Dimens.GapTiny))
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                )
            }
        }
        Spacer(Modifier.height(Dimens.GapSmall))
        // Jeder Tab hält die Höhe des Strichs, nur der gewählte zeigt ihn: Beim Wechsel springt nichts.
        val stripe = with(LocalDensity.current) { labelWidth.toDp() }
        Box(
            Modifier
                .width(stripe)
                .height(Sizes.PageTabIndicator)
                .then(if (selected) Modifier.background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall) else Modifier),
        )
    }
}

@Preview
@Composable
private fun PageTabsPreview() {
    AtemkraftTheme {
        val names = listOf(stringResource(R.string.own_creations), stringResource(R.string.own_favorites))
        PageTabs(items = names, selected = names.first(), onSelect = {}, label = { it }, counts = { if (it == names.last()) 7 else 5 })
    }
}
