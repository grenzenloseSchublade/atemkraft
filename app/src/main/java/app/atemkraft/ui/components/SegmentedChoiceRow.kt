package app.atemkraft.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Einheitliche Einfachauswahl-Zeile (SegmentedButtons über die volle Breite) – ersetzt die
 * mehrfach kopierten `SingleChoiceSegmentedButtonRow`-Blöcke in den Einstellungen.
 */
@Composable
fun <T> SegmentedChoiceRow(
    items: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            SegmentedButton(
                selected = item == selected,
                onClick = { onSelect(item) },
                shape = SegmentedButtonDefaults.itemShape(index, items.size),
            ) {
                Text(label(item))
            }
        }
    }
}
