package app.atemkraft.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
