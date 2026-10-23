package app.atemkraft.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Sizes

/**
 * Zurücksetzen, das nur bei Abweichungen da ist (MUSTER-01): Kreispfeil, tonal gefüllt, gleitet
 * mit [visible] von [slideFrom] herein bzw. dorthin hinaus (Icon-only, TalkBack „Zurücksetzen“).
 * Löst man ihn per Tastatur oder D-Pad aus, verschwindet er unter dem Fokus: Dann ruft er zuerst
 * [onFocusHandoff] – der Aufrufer setzt den Fokus auf den Nachbarn (Starten bzw. Play) –, danach
 * [onReset] (A11Y-07). Bei Touch hat der Knopf keinen Fokus, dort ändert sich nichts.
 *
 * [compact]: Kreis wie die Stepper-Knöpfe (`AppIconButton`, Leiste des Muster-Editors); sonst ein
 * Tonal-Button mit derselben Mindesthöhe und demselben senkrechten Polster wie „Starten“ und
 * einem Icon-Feld so hoch wie dessen Textzeile ([StartSplitButton]) – damit wachsen beide bei
 * großer Schrift gleich mit, ohne `IntrinsicSize.Min` (das mäße die 48-dp-Tippfläche mit).
 */
@Composable
fun SlidingReset(
    visible: Boolean,
    onReset: () -> Unit,
    onFocusHandoff: () -> Unit,
    modifier: Modifier = Modifier,
    slideFrom: Alignment.Horizontal = Alignment.Start,
    compact: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val onClick = {
        if (focused) onFocusHandoff()
        onReset()
    }
    val tracked = Modifier.onFocusChanged { focused = it.hasFocus }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = expandHorizontally(expandFrom = slideFrom) + fadeIn(),
        exit = shrinkHorizontally(shrinkTowards = slideFrom) + fadeOut(),
    ) {
        if (compact) {
            AppIconButton(onClick = onClick, modifier = tracked, colors = IconButtonDefaults.filledTonalIconButtonColors()) {
                Icon(painterResource(R.drawable.ic_reset), contentDescription = stringResource(R.string.adjust_reset))
            }
        } else {
            FilledTonalButton(
                onClick = onClick,
                modifier = Modifier.heightIn(min = Sizes.ButtonHeight).then(tracked),
                contentPadding = PaddingValues(horizontal = Dimens.CardPadding, vertical = Dimens.GapSmall),
            ) {
                // Stil der Button-Beschriftung, den M3 hier wie bei „Starten“ bereitstellt; die
                // Zeile gemessen, wie „Starten“ sie rendert (auch bei nichtlinearer Schrift).
                Box(Modifier.heightIn(min = rememberFirstLineHeight(LocalTextStyle.current)), contentAlignment = Alignment.Center) {
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
