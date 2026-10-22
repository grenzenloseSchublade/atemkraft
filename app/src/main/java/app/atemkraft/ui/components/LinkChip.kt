package app.atemkraft.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Sizes

/**
 * Antippbarer Verweis-Chip (öffnet z. B. eine Übung), auf Basis von Material3 SuggestionChip.
 * Sichtbar [Sizes.ChipHeight] hoch (wächst mit großer Schrift), Tippfläche 48 dp. Die Beschriftung
 * bleibt einzeilig: Lange Namen („Nadi Shodhana (Wechselatmung)“ bei großer Schrift auf 360 dp)
 * werden verkleinert statt umbrochen – bis [CHIP_MIN_SCALE], tiefer als sonst, weil der Chip auf
 * voller Breite keinen anderen Ausweg hat; bei fontScale 2,0 ist das noch etwa Standardgröße.
 * Der Text trägt [CHIP_TEXT_TAG], damit der ScreenshotTest einen trotzdem umbrechenden Chip meldet.
 */
@Composable
fun LinkChip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SuggestionChip(
        onClick = onClick,
        label = { WholeWordText(text = label, singleLine = true, minScale = CHIP_MIN_SCALE, modifier = Modifier.testTag(CHIP_TEXT_TAG)) },
        // Mindesthöhe statt Höhe: ersetzt die feste M3-Mindesthöhe, ohne Inhalt zu kappen.
        modifier = modifier.heightIn(min = Sizes.ChipHeight),
        // Rahmen in outline wie bis M3 1.3; outlineVariant (Standard ab 1.4) ist auf dem dunklen
        // Grund kaum zu sehen, der Chip wirkte dann nicht mehr antippbar.
        border = SuggestionChipDefaults.suggestionChipBorder(
            enabled = true,
            borderColor = MaterialTheme.colorScheme.outline,
            borderWidth = Dimens.BorderChip,
        ),
    )
}

/** Untergrenze der Chip-Beschriftung (WholeWordText sonst 0,7, Tab-Leiste 0,5). */
private const val CHIP_MIN_SCALE = 0.6f
