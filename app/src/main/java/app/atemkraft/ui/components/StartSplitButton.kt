package app.atemkraft.ui.components

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
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
    // Der Reset nimmt Platz, solange er steht oder noch gleitet: Beim Hinausgleiten stünde
    // „Starten“ sonst schon einzeilig in der noch engen Breite und bräche um.
    val resetState = remember { MutableTransitionState(resetVisible) }
    resetState.targetState = resetVisible
    val resetTakesSpace = resetState.currentState || resetState.targetState
    // Stil der Beschriftung, wie M3 ihn im Button setzt (labelLarge über dem umgebenden Stil).
    val labelWidth = rememberWidestTextWidth(listOf(label), LocalTextStyle.current.merge(MaterialTheme.typography.labelLarge))
    val padding = ButtonDefaults.ContentPadding
    val direction = LocalLayoutDirection.current
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Passt „Starten · ca. 5 min“ in eine Zeile? Entschieden aus der Restbreite (feste Breite des
        // Reset, Polster des Buttons) in derselben Komposition, die auch den Reset baut: So wächst er
        // vom ersten Frame an um dieselben Zeilen mit (ein Größentoken je Leiste), ohne Rückmeldung
        // aus der Messphase. Sonst stehen Start und Dauer ohne Punkt in zwei Zeilen – keine Zeile
        // endet auf „·“ (LAYOUT-03, wie die Status-Zeilen der Abschnittsköpfe).
        val space = with(density) {
            val reset = if (resetTakesSpace) Dimens.ListGap.roundToPx() + ResetWidth.roundToPx() else 0
            constraints.maxWidth - reset -
                padding.calculateLeftPadding(direction).roundToPx() - padding.calculateRightPadding(direction).roundToPx()
        }
        val fits = with(density) { labelWidth.roundToPx() } <= space
        val parts = if (fits) listOf(label) else label.splitAtDots()
        val shown = parts.joinToString("\n")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap),
        ) {
            Button(
                onClick = onStart,
                modifier = Modifier.weight(1f).heightIn(min = Sizes.ButtonHeight).focusRequester(startFocus),
                contentPadding = padding,
            ) {
                Text(shown, textAlign = TextAlign.Center)
            }
            SlidingReset(visibleState = resetState, onReset = onReset, onFocusHandoff = { startFocus.requestFocus() }, lines = parts.size)
        }
    }
}

@Preview
@Composable
private fun StartSplitButtonPreview() {
    AtemkraftTheme { StartSplitButton(label = stringResource(R.string.action_start), resetVisible = true, onStart = {}, onReset = {}) }
}
