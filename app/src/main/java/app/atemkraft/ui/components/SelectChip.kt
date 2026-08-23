package app.atemkraft.ui.components

import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/** Einheitlicher Auswahl-Chip (Single-/Multi-Select-Pille) auf Basis von Material3 FilterChip. */
@Composable
fun SelectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(),
    )
}
