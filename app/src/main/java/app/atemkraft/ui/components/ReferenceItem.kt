package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.atemkraft.domain.Reference
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.TERTIARY

/** Einheitliche Darstellung einer Literaturquelle (Kurznachweis + Kennung). App-weit (Detail, Einstellungen). */
@Composable
fun ReferenceItem(reference: Reference) {
    Column {
        Text(
            text = reference.citation,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
        )
        reference.identifier?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = TERTIARY),
            )
        }
    }
}
