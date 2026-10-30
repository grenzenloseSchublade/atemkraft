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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
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
 * [horizontal]/[vertical] steuern die Innenabstände; mit `horizontal = 0.dp` (Karte mit eigenem
 * Polster, Einstellungen, Blatt „Anpassen“) ragt der unsichtbare Rand von „+“ in deren Polster,
 * das Label steht an der Textkante des Behälters.
 *
 * TalkBack hört Name und Einheit ausgeschrieben (A11Y-06): [spokenLabel] ist der gesprochene Name
 * („Einatmen“), [valueUnit] ein Plural für den Wert („4 Sekunden“, sichtbar „4 s“); ohne ihn die
 * blanke Zahl. [spokenSuffix] hängt an die Ansage des Werts einen
 * Zustand an, den die Änderung mit sich bringt („…: 11 Sekunden, intensiv“) – so bleibt es eine
 * Ansage je Tipp. [announce] = false nimmt die Live-Region weg, solange ein anderer Knoten die
 * Änderung schon ansagt (Muster-Editor: Phase entfernt). [valueText] zeigt den Wert anders als die
 * Zahl (Uhrzeit „08:15“ für den Schritt 33, mit Einheit „5 s“), [spokenValue] sagt ihn dann in
 * Worten („8:15 Uhr“). [widestValues] sind die breitesten möglichen Werte („30 s“): Die Wertspalte
 * hält ihre Breite, damit die Knöpfe beim Steppen nicht springen (§4.5 „Nichts springt“); dieselbe
 * Liste in mehreren Steppern untereinander hält deren Knöpfe in einer Flucht. Das Label
 * steht in `bodyMedium` wie jede Zeile mit Steuerelement (`ToggleRow`, `SubLabel`, Anatomie §4.5).
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
    spokenSuffix: String? = null,
    announce: Boolean = true,
    valueText: String = value.toString(),
    spokenValue: String? = null,
    widestValues: List<String> = emptyList(),
) {
    // Der Knopf-Kreis ist kleiner als seine 48-dp-Tippfläche: Den unsichtbaren Rand rechts
    // vom Innenabstand abziehen, damit der sichtbare Kreis an der Inhaltskante steht.
    val inset = Sizes.CircleOverhang
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(CONTROL_ROW_TAG)
            .padding(start = horizontal, end = (horizontal - inset).coerceAtLeast(0.dp), top = vertical, bottom = vertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // weight: Die Knopfgruppe wird zuerst vermessen und behält ihre volle Breite; bei
        // wenig Platz (360 dp, große Schrift) bricht das Label zwischen ganzen Wörtern um,
        // statt „+“ aus der Zeile zu drücken.
        StepperLabel(label, Modifier.weight(1f).padding(end = Dimens.GapSmall))
        val spoken = spokenValue ?: if (valueUnit != null) pluralStringResource(valueUnit, value, value) else value.toString()
        val valueDescription = stepperValueDescription(spokenLabel, spoken, spokenSuffix)
        // Ohne eigenes Polster (in einer Karte mit Innenabstand) ragt der Rand des „+“-Knopfs über.
        val edge = if (horizontal < inset) Modifier.flushEnd(inset - horizontal) else Modifier
        val valueStyle = MaterialTheme.typography.titleMedium
        val reserve = if (widestValues.isEmpty()) 0.dp else rememberWidestTextWidth(widestValues + valueText, valueStyle)
        StepButtons(
            spokenLabel,
            valueDescription,
            value,
            range,
            onChange,
            announce,
            edge,
            valueText,
            valueStyle,
            valueMinWidth = maxOf(Sizes.StepperValueMinWidth, reserve),
        )
    }
}

/**
 * Stepper ohne sichtbares Label („−  5 min  +“) für knappe Zeilen, in denen der Zusammenhang das
 * Label trägt (Muster-Editor: Dauer in der Leiste unten, Sekunden der gewählten Phase in der
 * Werkzeugleiste). [valueText] ist der sichtbare Wert samt Einheit („5 min“, „4 s“, `titleSmall`); TalkBack
 * hört wie beim [Stepper] [spokenLabel] mit dem Wert in Worten ([valueUnit]) und optional
 * [spokenSuffix]. Knöpfe und Tippflächen wie beim [Stepper], kein eigenes Polster. [flushEnd]:
 * Am Zeilenende steht der sichtbare Kreis von „+“ bündig an der Inhaltskante (LAYOUT-07).
 */
@Composable
fun CompactStepper(
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    valueText: String,
    spokenLabel: String,
    modifier: Modifier = Modifier,
    @PluralsRes valueUnit: Int? = null,
    spokenSuffix: String? = null,
    announce: Boolean = true,
    flushEnd: Boolean = false,
) {
    val spokenValue = if (valueUnit != null) pluralStringResource(valueUnit, value, value) else value.toString()
    val valueDescription = stepperValueDescription(spokenLabel, spokenValue, spokenSuffix)
    val edge = if (flushEnd) Modifier.flushEnd(Sizes.CircleOverhang) else Modifier
    StepButtons(spokenLabel, valueDescription, value, range, onChange, announce, modifier.then(edge), valueText, MaterialTheme.typography.titleSmall)
}

/**
 * TalkBack-Text des Werts („Einatmen: 4 Sekunden“, mit [suffix] „…, höher als empfohlen“). Satzbau
 * und Trennzeichen aus `strings.xml` (TEXT-01), damit eine Übersetzung die Reihenfolge ändern kann.
 */
@Composable
private fun stepperValueDescription(label: String, value: String, suffix: String?): String {
    val base = stringResource(R.string.cd_stepper_value, label, value)
    return if (suffix != null) stringResource(R.string.cd_stepper_value_suffix, base, suffix) else base
}

@Composable
private fun StepperLabel(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
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
    announce: Boolean,
    modifier: Modifier = Modifier,
    valueText: String = value.toString(),
    valueStyle: TextStyle = MaterialTheme.typography.titleMedium,
    valueMinWidth: Dp = Sizes.StepperValueMinWidth,
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
            Icon(painterResource(R.drawable.ic_remove), contentDescription = stringResource(R.string.cd_stepper_button, label, decreaseLabel))
        }
        Text(
            text = valueText,
            style = valueStyle,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            // Mindestbreite statt fester Breite: bei großer System-Schrift / dreistelligen
            // Werten (oder mit Einheit, „30 s“) wächst die Box, statt den Wert abzuschneiden.
            modifier = Modifier
                .widthIn(min = valueMinWidth)
                .semantics {
                    contentDescription = valueDescription
                    if (announce) liveRegion = LiveRegionMode.Polite
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
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_stepper_button, label, increaseLabel))
        }
    }
}

@Preview
@Composable
private fun CompactStepperPreview() {
    AtemkraftTheme {
        CompactStepper(value = 5, range = 1..30, onChange = {}, valueText = stringResource(R.string.pattern_editor_minutes, 5), spokenLabel = stringResource(R.string.cd_adjust_duration))
    }
}

@Preview
@Composable
private fun StepperPreview() {
    AtemkraftTheme { Stepper(label = stringResource(R.string.adjust_rounds), value = 4, range = 1..10, onChange = {}) }
}
