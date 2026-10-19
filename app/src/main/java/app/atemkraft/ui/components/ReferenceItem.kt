package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.atemkraft.domain.Reference
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.TERTIARY

/** Einheitliche Darstellung einer Literaturquelle (Kurznachweis + Kennung). App-weit (Detail, Einstellungen). */
@Composable
fun ReferenceItem(reference: Reference, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = reference.citation,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
        )
        reference.identifier?.let {
            // DOI-Ketten wie „10.17761/2025-D-25-00006“ bieten dem Zeilenumbruch keine Stelle;
            // WholeWordText verkleinert sie, statt mitten in der Zahl zu brechen (LAYOUT-03).
            WholeWordText(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = TERTIARY),
            )
        }
    }
}
