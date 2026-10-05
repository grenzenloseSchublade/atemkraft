package app.atemkraft.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Sizes

/**
 * Einheitlicher Ein-/Ausklapp-Umschalter: animierter Caret + Label. Ersetzt die früher pro
 * Screen handgebauten „▾/▴"-TextButtons und liefert konsistente TalkBack-Semantik.
 */
@Composable
fun DisclosureToggle(
    text: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = expandedStateText(expanded)
    TextButton(
        onClick = onToggle,
        modifier = modifier.heightIn(min = Sizes.ButtonHeight).semantics {
            stateDescription = state
        },
        contentPadding = PaddingValues(vertical = Dimens.GapTiny),
    ) {
        ExpandCaret(expanded = expanded, tint = LocalContentColor.current, size = Sizes.IconInButton)
        Spacer(Modifier.width(Dimens.GapTiny))
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
