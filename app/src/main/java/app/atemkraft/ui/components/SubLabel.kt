package app.atemkraft.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Unter-Überschrift innerhalb einer Karte oder eines Abschnitts (eine Ebene unter [SectionHeader]). */
@Composable
fun SubLabel(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge, color = color, modifier = modifier)
}
