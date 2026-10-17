package app.atemkraft.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.SuggestionChip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.atemkraft.ui.theme.Sizes

/**
 * Antippbarer Verweis-Chip (öffnet z. B. eine Übung), auf Basis von Material3 SuggestionChip.
 * Sichtbar [Sizes.ChipHeight] hoch (wächst mit großer Schrift), Tippfläche 48 dp. Die Beschriftung
 * bleibt einzeilig: Lange Namen („Nadi Shodhana (Wechselatmung)“ bei großer Schrift auf 360 dp)
 * werden leicht verkleinert statt umbrochen; der Text trägt [CHIP_TEXT_TAG], damit der
 * ScreenshotTest einen trotzdem umbrechenden Chip meldet.
 */
@Composable
fun LinkChip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SuggestionChip(
        onClick = onClick,
        label = { WholeWordText(text = label, singleLine = true, modifier = Modifier.testTag(CHIP_TEXT_TAG)) },
        // Mindesthöhe statt Höhe: ersetzt die feste M3-Mindesthöhe, ohne Inhalt zu kappen.
        modifier = modifier.heightIn(min = Sizes.ChipHeight),
    )
}
