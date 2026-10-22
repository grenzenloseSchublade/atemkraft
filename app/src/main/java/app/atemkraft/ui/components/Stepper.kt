package app.atemkraft.ui.components

import androidx.annotation.PluralsRes
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
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
 * Einheitlicher Zahl-Stepper (−  wert  +) mit Label, genutzt von der Übungs-Anpassung. Die
 * Knöpfe sind an [range] gekoppelt, behalten Klick und Rolle für TalkBack; der Wert wird bei
 * jeder Änderung angesagt (A11Y-04).
 *
 * Label links, Knöpfe als rechte Spalte, der sichtbare Kreis bündig an der Inhaltskante.
 * [horizontal]/[vertical] steuern die Innenabstände.
 *
 * TalkBack hört Name und Einheit ausgeschrieben (A11Y-06): [spokenLabel] ist das Label ohne
 * sichtbare Einheit („Einatmen“ statt „Einatmen (s)“), [valueUnit] ein Plural für den Wert
 * („4 Sekunden“); ohne ihn die blanke Zahl. [zeroSpoken] ersetzt den gesprochenen Wert 0
 * („Halten voll: aus“ statt „0 Sekunden“), sichtbar bleibt die „0“.
 */
@Composable
fun Stepper(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    horizontal: Dp = Dimens.CardPadding,
    vertical: Dp = Dimens.CardPadding,
    spokenLabel: String = label,
    @PluralsRes valueUnit: Int? = null,
    zeroSpoken: String? = null,
) {
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
        val spokenValue = when {
            value == 0 && zeroSpoken != null -> zeroSpoken
            valueUnit != null -> pluralStringResource(valueUnit, value, value)
            else -> value.toString()
        }
        StepButtons(spokenLabel, stringResource(R.string.cd_stepper_value, spokenLabel, spokenValue), value, range, onChange)
    }
}

@Composable
private fun StepperLabel(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

/**
 * „−  wert  +“: Knöpfe als gefüllte [AppIconButton] (Tippfläche 48 dp), Wert dazwischen. Am
 * Anschlag wird der Knopf deaktiviert; hatte er den Fokus (Tastatur, D-Pad), geht der Fokus auf
 * den Gegenknopf statt ins Leere (A11Y-07).
 */
@Composable
private fun StepButtons(
    label: String,
    valueDescription: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val decreaseLabel = stringResource(R.string.adjust_decrease)
    val increaseLabel = stringResource(R.string.adjust_increase)
    val minusFocus = remember { FocusRequester() }
    val plusFocus = remember { FocusRequester() }
    var minusFocused by remember { mutableStateOf(false) }
    var plusFocused by remember { mutableStateOf(false) }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        AppIconButton(
            onClick = {
                val next = (value - 1).coerceIn(range)
                if (minusFocused && next == range.first) plusFocus.requestFocus()
                onChange(next)
            },
            modifier = Modifier.focusRequester(minusFocus).onFocusChanged { minusFocused = it.hasFocus },
            enabled = value > range.first,
            colors = IconButtonDefaults.filledTonalIconButtonColors(),
        ) {
            Icon(painterResource(R.drawable.ic_remove), contentDescription = "$label, $decreaseLabel")
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            // Mindestbreite statt fester Breite: bei großer System-Schrift / dreistelligen
            // Werten wächst die Box, statt den Wert abzuschneiden.
            modifier = Modifier
                .widthIn(min = Sizes.StepperValueMinWidth)
                .semantics {
                    contentDescription = valueDescription
                    liveRegion = LiveRegionMode.Polite
                },
        )
        AppIconButton(
            onClick = {
                val next = (value + 1).coerceIn(range)
                if (plusFocused && next == range.last) minusFocus.requestFocus()
                onChange(next)
            },
            modifier = Modifier.focusRequester(plusFocus).onFocusChanged { plusFocused = it.hasFocus },
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
