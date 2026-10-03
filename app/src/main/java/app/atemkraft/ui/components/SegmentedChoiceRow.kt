package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

/**
 * Einheitliche Einfachauswahl – ersetzt die mehrfach kopierten
 * `SingleChoiceSegmentedButtonRow`-Blöcke in den Einstellungen.
 *
 * Normalfall: SegmentedButtons über die volle Breite. Passt eine Beschriftung (inklusive
 * Häkchen des ausgewählten Segments) nicht einzeilig in ihr gleich breites Segment, wird
 * stattdessen eine senkrechte Liste mit Radiobuttons gezeigt – Beschriftungen brechen nie
 * im Wort um (LAYOUT-03).
 */
@Composable
fun <T> SegmentedChoiceRow(
    items: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
) {
    SubcomposeLayout(Modifier.fillMaxWidth()) { constraints ->
        // Jede Beschriftung einzeln als ausgewähltes Segment messen (breiteste Variante).
        val needed = subcompose("probe") {
            items.forEachIndexed { index, item ->
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = true,
                        onClick = {},
                        shape = SegmentedButtonDefaults.itemShape(index, items.size),
                    ) { Text(label(item), maxLines = 1) }
                }
            }
        }.maxOf { it.maxIntrinsicWidth(Constraints.Infinity) }
        val fits = needed * items.size <= constraints.maxWidth

        val placeables = subcompose(fits) {
            if (fits) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    items.forEachIndexed { index, item ->
                        SegmentedButton(
                            selected = item == selected,
                            onClick = { onSelect(item) },
                            shape = SegmentedButtonDefaults.itemShape(index, items.size),
                        ) { Text(label(item), maxLines = 1) }
                    }
                }
            } else {
                Column(Modifier.fillMaxWidth().selectableGroup()) {
                    items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(
                                    selected = item == selected,
                                    onClick = { onSelect(item) },
                                    role = Role.RadioButton,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = item == selected, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = label(item),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }.map { it.measure(constraints.copy(minHeight = 0)) }

        layout(constraints.maxWidth, placeables.sumOf { it.height }) {
            var y = 0
            placeables.forEach {
                it.placeRelative(0, y)
                y += it.height
            }
        }
    }
}
