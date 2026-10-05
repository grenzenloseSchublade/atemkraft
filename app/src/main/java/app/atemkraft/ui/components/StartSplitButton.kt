package app.atemkraft.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Sizes

/**
 * Gepinnter Start als Split-Button: links Starten in voller Restbreite, rechts gleitet – nur
 * wenn [resetVisible] – ein Kreispfeil zum Zurücksetzen herein (Icon-only, TalkBack über
 * contentDescription). Beide Knöpfe sind gleich hoch – auch bei großer Schrift, wenn „Starten“
 * über [Sizes.ButtonHeight] hinauswächst. Überall verwenden, wo eine konfigurierbare Session
 * gestartet wird.
 */
@Composable
fun StartSplitButton(
    label: String,
    resetVisible: Boolean,
    onStart: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap),
    ) {
        Button(onClick = onStart, modifier = Modifier.weight(1f).heightIn(min = Sizes.ButtonHeight)) {
            Text(label)
        }
        AnimatedVisibility(
            visible = resetVisible,
            enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
            exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(),
        ) {
            // Gleiche Höhe ohne IntrinsicSize.Min: Das würde die 48-dp-Tippfläche mitmessen und
            // beide Knöpfe sichtbar auf 48 aufblähen. Stattdessen dieselbe Mindesthöhe, dasselbe
            // senkrechte Polster wie „Starten“ (M3: 8 dp) und ein Icon-Feld so hoch wie dessen
            // Textzeile – damit wachsen beide bei großer Schrift gleich mit.
            FilledTonalButton(
                onClick = onReset,
                modifier = Modifier.heightIn(min = Sizes.ButtonHeight),
                contentPadding = PaddingValues(horizontal = Dimens.CardPadding, vertical = Dimens.GapSmall),
            ) {
                // Zeilenhöhe wie Compose sie setzt: bei nichtlinearer Systemschrift (Android 14+)
                // im Verhältnis zur skalierten Schriftgröße, nicht als eigener sp-Wert.
                val style = MaterialTheme.typography.labelLarge
                val labelLine = with(LocalDensity.current) { style.fontSize.toDp() * (style.lineHeight.value / style.fontSize.value) }
                Box(Modifier.heightIn(min = labelLine), contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_reset),
                        contentDescription = stringResource(R.string.adjust_reset),
                        modifier = Modifier.size(Sizes.IconInButton),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun StartSplitButtonPreview() {
    AtemkraftTheme { StartSplitButton(label = stringResource(R.string.action_start), resetVisible = true, onStart = {}, onReset = {}) }
}
