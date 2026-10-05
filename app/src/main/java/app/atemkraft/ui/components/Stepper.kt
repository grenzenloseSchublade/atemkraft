package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.ui.theme.Dimens

/**
 * Einheitlicher Zahl-Stepper (−  wert  +) mit Label. Wird von der Übungs-Anpassung und dem
 * Meditations-Tab genutzt. Die +/−-Knöpfe sind an [range] gekoppelt; Semantik ist für
 * TalkBack zusammengefasst. [vertical]/[horizontal] steuern die Innenabstände (kompakter,
 * wenn mehrere Stepper in einer Karte gestapelt werden).
 */
@Composable
fun Stepper(
    label: String,
    value: Int,
    range: IntRange,
    horizontal: Dp = Dimens.CardPadding,
    vertical: Dp = Dimens.CardPadding,
    onChange: (Int) -> Unit,
) {
    val decreaseLabel = stringResource(R.string.adjust_decrease)
    val increaseLabel = stringResource(R.string.adjust_increase)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontal, vertical = vertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // weight: Die Knopfgruppe wird zuerst vermessen und behält ihre volle Breite; bei wenig
        // Platz (360 dp, große Schrift) bricht das Label zwischen ganzen Wörtern um, statt „+“
        // aus der Zeile zu drücken.
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(end = Dimens.GapSmall),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
            FilledTonalIconButton(
                onClick = { onChange((value - 1).coerceIn(range)) },
                enabled = value > range.first,
                modifier = Modifier.clearAndSetSemantics { contentDescription = "$label, $decreaseLabel" },
            ) {
                Text("−", style = MaterialTheme.typography.headlineSmall)
            }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                // Mindestbreite statt fester Breite: bei großer System-Schrift / dreistelligen
                // Werten wächst die Box, statt den Wert abzuschneiden.
                modifier = Modifier
                    .widthIn(min = 48.dp)
                    .clearAndSetSemantics { contentDescription = "$label: $value" },
            )
            FilledTonalIconButton(
                onClick = { onChange((value + 1).coerceIn(range)) },
                enabled = value < range.last,
                modifier = Modifier.clearAndSetSemantics { contentDescription = "$label, $increaseLabel" },
            ) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}
