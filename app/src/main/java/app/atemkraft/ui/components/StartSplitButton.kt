package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.domain.splitAtDots
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
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap),
    ) {
        Button(onClick = onStart, modifier = Modifier.weight(1f).heightIn(min = Sizes.ButtonHeight).focusRequester(startFocus)) {
            StartLabel(label)
        }
        SlidingReset(visible = resetVisible, onReset = onReset, onFocusHandoff = { startFocus.requestFocus() })
    }
}

/**
 * Beschriftung „Starten · ca. 5 min“: passt sie nicht in eine Zeile (großer Schrift, Reset
 * daneben), stehen Start und Dauer ohne Punkt in zwei Zeilen – keine Zeile endet auf „·“
 * (LAYOUT-03, wie die Status-Zeilen der Abschnittsköpfe).
 */
@Composable
private fun StartLabel(label: String) {
    val labelWidth = rememberWidestTextWidth(listOf(label), LocalTextStyle.current)
    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val fits = labelWidth <= maxWidth
        val shown = if (fits) label else label.splitAtDots().joinToString("\n")
        Text(shown, textAlign = TextAlign.Center)
    }
}

@Preview
@Composable
private fun StartSplitButtonPreview() {
    AtemkraftTheme { StartSplitButton(label = stringResource(R.string.action_start), resetVisible = true, onStart = {}, onReset = {}) }
}
