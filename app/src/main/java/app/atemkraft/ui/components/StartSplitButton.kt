package app.atemkraft.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
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
 *
 * Löst man „Zurücksetzen“ per Tastatur oder D-Pad aus, verschwindet der Knopf unter dem Fokus:
 * Der Fokus geht dann auf „Starten“ statt ins Leere (A11Y-07). Bei Touch hat der Knopf keinen
 * Fokus, dort ändert sich nichts.
 */
@Composable
fun StartSplitButton(
    label: String,
    resetVisible: Boolean,
    onStart: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val startFocus = remember { FocusRequester() }
    var resetFocused by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap),
    ) {
        Button(onClick = onStart, modifier = Modifier.weight(1f).heightIn(min = Sizes.ButtonHeight).focusRequester(startFocus)) {
            StartLabel(label)
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
                onClick = {
                    if (resetFocused) startFocus.requestFocus()
                    onReset()
                },
                modifier = Modifier.heightIn(min = Sizes.ButtonHeight).onFocusChanged { resetFocused = it.hasFocus },
                contentPadding = PaddingValues(horizontal = Dimens.CardPadding, vertical = Dimens.GapSmall),
            ) {
                // Stil der Button-Beschriftung, den M3 hier wie bei „Starten“ bereitstellt. Zeilenhöhe
                // wie Compose sie setzt: bei nichtlinearer Systemschrift (Android 14+) im Verhältnis
                // zur skalierten Schriftgröße, nicht als eigener sp-Wert.
                val style = LocalTextStyle.current
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

/**
 * Beschriftung „Starten · ca. 5 min“: passt sie nicht in eine Zeile (großer Schrift, Reset
 * daneben), stehen Start und Dauer ohne Punkt in zwei Zeilen – keine Zeile endet auf „·“
 * (LAYOUT-03, wie die Status-Zeilen der Abschnittsköpfe).
 */
@Composable
private fun StartLabel(label: String) {
    val style = LocalTextStyle.current
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val fits = measurer.measure(label, style, softWrap = false, maxLines = 1).size.width <= constraints.maxWidth
        val shown = if (fits) label else label.split(LABEL_SEPARATOR).joinToString("\n")
        Text(shown, textAlign = TextAlign.Center)
    }
}

/** Trennpunkt zwischen Start und Dauer, mit (auch geschützten) Leerzeichen drumherum. */
private val LABEL_SEPARATOR = Regex("[\\s\u00A0]*·[\\s\u00A0]*")

@Preview
@Composable
private fun StartSplitButtonPreview() {
    AtemkraftTheme { StartSplitButton(label = stringResource(R.string.action_start), resetVisible = true, onStart = {}, onReset = {}) }
}
