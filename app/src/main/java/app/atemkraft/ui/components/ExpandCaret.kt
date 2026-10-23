package app.atemkraft.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import app.atemkraft.R
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Sizes

/**
 * Caret aller Aufklapp-Elemente (`ExpanderSection`, aufklappbarer `SectionHeader`,
 * `DisclosureToggle` mit [size] `IconInButton`): zeigt
 * zu nach unten, aufgeklappt nach oben und dreht dazwischen. Rein dekorativ (cd `null`) – den
 * Zustand trägt die Zeile per `stateDescription` ([expandedStateText]), sonst läse TalkBack
 * ihn doppelt.
 */
@Composable
internal fun ExpandCaret(
    expanded: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = Sizes.IconDefault,
) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "caret")
    Icon(
        imageVector = Icons.Filled.KeyboardArrowDown,
        contentDescription = null, // dekorativ: den Zustand sagt die Zeile per stateDescription an
        tint = tint,
        modifier = modifier.rotate(rotation).size(size),
    )
}

/**
 * Chevron „führt weiter“ hinter einem Verweis auf eine andere Seite ([NavCard]: Kapitel im Tab
 * „Lernen“, „Über Atemkraft“ in den Einstellungen; Detailseite: „… in der Atemlehre“; Atmen-Tab:
 * eigenes Muster, „Mehr aus <Bereich>“; Ende-Screen: Logbuch) – als Icon statt Textzeichen „›“ (ICON-01),
 * RTL gespiegelt. Rein dekorativ: Den Namen trägt die Zeile bzw. der Button.
 */
@Composable
fun ForwardChevron(tint: Color, modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null, // dekorativ: Name und Rolle trägt die Zeile bzw. der Button
        tint = tint,
        modifier = modifier.size(Sizes.IconInButton),
    )
}

/**
 * Verweistext mit [ForwardChevron] am Ende, als Inhalt eines `AppTextButton` („… in der
 * Atemlehre“). Der Text hält rechts die Breite des Chevrons frei, der Chevron liegt darüber
 * an der Endkante: Er behält seine volle Größe, auch wenn die Beschriftung bei großer Schrift
 * umbricht (LAYOUT-05) – als Icon neben dem Text in der Button-Reihe bekäme er dann 0 dp, als
 * Inline-Inhalt rutschte er allein in eine neue Zeile. Sanskrit kursiv („*laṅghana* in der
 * Atemlehre“, [sanskritText]).
 */
@Composable
fun ForwardLinkText(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.CenterEnd) {
        Text(text = sanskritText(text), color = color, modifier = Modifier.padding(end = Sizes.IconInButton))
        ForwardChevron(tint = color)
    }
}

/** Ein Wortlaut für jeden Aufklapp-Zustand, damit TalkBack ihn überall gleich ansagt. */
@Composable
internal fun expandedStateText(expanded: Boolean): String = stringResource(if (expanded) R.string.state_expanded else R.string.state_collapsed)

@Preview
@Composable
private fun ExpandCaretPreview() {
    AtemkraftTheme {
        ExpandCaret(expanded = false, tint = MaterialTheme.colorScheme.primary)
    }
}
