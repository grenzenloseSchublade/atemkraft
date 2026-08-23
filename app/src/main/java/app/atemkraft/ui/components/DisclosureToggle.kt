package app.atemkraft.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

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
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "caret")
    TextButton(
        onClick = onToggle,
        modifier = modifier.semantics {
            stateDescription = if (expanded) "Erweitert" else "Eingeklappt"
        },
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier.rotate(rotation),
        )
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
