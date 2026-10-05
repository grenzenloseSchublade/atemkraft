package app.atemkraft.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens

/**
 * Einheitlicher Abschnitts-Header: farbiger Akzentbalken + Titel. App-weit genutzt (Atmen,
 * Situationen, Meditation), damit Sektionsüberschriften überall identisch lesen.
 *
 * [trailingAction] ist eine optionale Aktion für den ganzen Abschnitt (z. B. „Muster neu
 * generieren“), rechts bündig als `AppIconButton`. Sie steht unten bündig neben dem Kopf samt
 * Abstand darüber: So belegt die 48-dp-Tippfläche den Platz, den der Kopf ohnehin hat, und
 * der Abschnitt rückt nicht nach unten.
 */
@Composable
fun SectionHeader(
    title: String,
    color: Color,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(top = Dimens.SectionHeaderTop, bottom = Dimens.GapSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
        ) {
            Box(
                modifier = Modifier
                    // Balken so hoch wie die Schrift – folgt TEXT_SCALE und der Systemschriftgröße.
                    .size(width = 4.dp, height = with(LocalDensity.current) { MaterialTheme.typography.headlineSmall.fontSize.toDp() })
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
            )
            WholeWordText(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = color,
                modifier = Modifier.semantics { heading() },
            )
        }
        trailingAction?.invoke()
    }
}

@Preview
@Composable
private fun SectionHeaderPreview() {
    AtemkraftTheme {
        val accent = MaterialTheme.colorScheme.secondary
        SectionHeader(title = stringResource(R.string.home_daily_pattern), color = accent) {
            AppIconButton(onClick = {}) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.cd_pattern_regenerate), tint = accent)
            }
        }
    }
}
