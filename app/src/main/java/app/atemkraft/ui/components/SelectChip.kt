package app.atemkraft.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Sizes

/**
 * Einheitlicher Auswahl-Chip (Single-/Multi-Select-Pille) auf Basis von Material3 FilterChip.
 * Sichtbar [Sizes.ChipHeight] hoch (wächst mit großer Schrift), Tippfläche 48 dp.
 */
@Composable
fun SelectChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        // Mindesthöhe statt Höhe: ersetzt die feste M3-Mindesthöhe 32, ohne Inhalt zu kappen.
        modifier = modifier.heightIn(min = Sizes.ChipHeight),
        colors = FilterChipDefaults.filterChipColors(),
    )
}

@Preview
@Composable
private fun SelectChipPreview() {
    AtemkraftTheme { SelectChip(label = stringResource(R.string.meditation_mode_timed), selected = true, onClick = {}) }
}
