package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Sizes

/**
 * Einheitlicher Zahl-Stepper (−  wert  +) mit Label. Wird von der Übungs-Anpassung und dem
 * Meditations-Tab genutzt. Die Knöpfe sind an [range] gekoppelt, behalten Klick und Rolle für
 * TalkBack; der Wert wird bei jeder Änderung angesagt (A11Y-04).
 *
 * [inline] = false (Karten): Label links, Knöpfe als rechte Spalte, der sichtbare Kreis bündig
 * an der Inhaltskante. [inline] = true (Meditations-Tab): Knöpfe direkt hinter dem Label,
 * linksbündig wie die Chips darüber; reicht der Platz nicht, rutschen sie als Gruppe in die
 * nächste Zeile (LAYOUT-03). [horizontal]/[vertical] steuern die Innenabstände.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Stepper(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    inline: Boolean = false,
    horizontal: Dp = Dimens.CardPadding,
    vertical: Dp = Dimens.CardPadding,
) {
    if (inline) {
        FlowRow(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = horizontal, vertical = vertical),
            horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
        ) {
            StepperLabel(label, Modifier.align(Alignment.CenterVertically))
            StepButtons(label, value, range, onChange, Modifier.align(Alignment.CenterVertically))
        }
    } else {
        // Der Knopf-Kreis ist kleiner als seine 48-dp-Tippfläche: Den unsichtbaren Rand rechts
        // vom Innenabstand abziehen, damit der sichtbare Kreis an der Inhaltskante steht.
        val inset = (Dimens.MinTouchTarget - Sizes.IconButtonSize) / 2
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = horizontal, end = (horizontal - inset).coerceAtLeast(0.dp), top = vertical, bottom = vertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // weight: Die Knopfgruppe wird zuerst vermessen und behält ihre volle Breite; bei
            // wenig Platz (360 dp, große Schrift) bricht das Label zwischen ganzen Wörtern um,
            // statt „+“ aus der Zeile zu drücken.
            StepperLabel(label, Modifier.weight(1f).padding(end = Dimens.GapSmall))
            StepButtons(label, value, range, onChange)
        }
    }
}

@Composable
private fun StepperLabel(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

/** „−  wert  +“: Knöpfe als gefüllte [AppIconButton] (Tippfläche 48 dp), Wert dazwischen. */
@Composable
private fun StepButtons(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val decreaseLabel = stringResource(R.string.adjust_decrease)
    val increaseLabel = stringResource(R.string.adjust_increase)
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        AppIconButton(
            onClick = { onChange((value - 1).coerceIn(range)) },
            enabled = value > range.first,
            colors = IconButtonDefaults.filledTonalIconButtonColors(),
        ) {
            Icon(painterResource(R.drawable.ic_remove), contentDescription = "$label, $decreaseLabel")
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            // Mindestbreite statt fester Breite: bei großer System-Schrift / dreistelligen
            // Werten wächst die Box, statt den Wert abzuschneiden.
            modifier = Modifier
                .widthIn(min = Sizes.StepperValueMinWidth)
                .semantics {
                    contentDescription = "$label: $value"
                    liveRegion = LiveRegionMode.Polite
                },
        )
        AppIconButton(
            onClick = { onChange((value + 1).coerceIn(range)) },
            enabled = value < range.last,
            colors = IconButtonDefaults.filledTonalIconButtonColors(),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "$label, $increaseLabel")
        }
    }
}

@Preview
@Composable
private fun StepperPreview() {
    AtemkraftTheme { Stepper(label = stringResource(R.string.adjust_rounds), value = 4, range = 1..10, onChange = {}) }
}
