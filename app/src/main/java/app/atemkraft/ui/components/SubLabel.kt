package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens

/** Unter-Überschrift innerhalb einer Karte oder eines Abschnitts (eine Ebene unter [SectionHeader]). */
@Composable
fun SubLabel(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color, modifier = modifier)
}

@Preview
@Composable
private fun SubLabelPreview() {
    AtemkraftTheme { SubLabel("Gong") }
}

/** Beschrifteter Block („Lehre“, „Typisch“ …): [SubLabel] als Unter-Überschrift, darunter der Inhalt. */
@Composable
internal fun Labeled(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier) {
        SubLabel(label, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(Dimens.GapHairline))
        content()
    }
}
