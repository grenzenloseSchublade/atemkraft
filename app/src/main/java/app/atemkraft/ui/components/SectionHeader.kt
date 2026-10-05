package app.atemkraft.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY

/**
 * Einheitlicher Abschnitts-Header: farbiger Akzentbalken + Titel. App-weit genutzt (Atmen,
 * Situationen, Meditation), damit Sektionsüberschriften überall identisch lesen.
 *
 * [trailingAction] ist eine optionale Aktion für den ganzen Abschnitt (z. B. „Muster neu
 * generieren“), rechts bündig als `AppIconButton`. Sie steht unten bündig neben dem Kopf samt
 * Abstand darüber: So belegt die 48-dp-Tippfläche den Platz, den der Kopf ohnehin hat, und
 * der Abschnitt rückt nicht nach unten.
 *
 * [teaser] steht als ganzer Satz unter dem Titel (nie gekürzt, LAYOUT-03). Mit [expanded]
 * wird der Kopf zur aufklappbaren Zeile (MUSTER-03): Caret am Titel, die ganze Zeile samt
 * Teaser ist die Tippfläche (≥ 48 dp, Rolle Button, Ausgeklappt/Eingeklappt), den Inhalt
 * zeigt der Aufrufer darunter. So bleibt ein langer Tab eine kurze Übersicht, ohne neue
 * Fläche – der Abschnittskopf wechselt nur seinen Zustand. `null` heißt: nicht aufklappbar.
 */
@Composable
fun SectionHeader(
    title: String,
    color: Color,
    modifier: Modifier = Modifier,
    teaser: String? = null,
    expanded: Boolean? = null,
    onExpandedChange: (Boolean) -> Unit = {},
    trailingAction: (@Composable () -> Unit)? = null,
) {
    val toggle = if (expanded == null) {
        Modifier
    } else {
        val state = expandedStateText(expanded)
        // Der Abstand über dem Kopf liegt außerhalb der Tippfläche, damit die Welle nicht im
        // Leerraum über dem Titel beginnt; innen polstert GapSmall die Zeile oben und unten.
        Modifier
            .padding(top = Dimens.GapSmall)
            .heightIn(min = Dimens.MinTouchTarget)
            .clickable(role = Role.Button) { onExpandedChange(!expanded) }
            .semantics {
                heading()
                stateDescription = state
            }
    }
    val inner = if (expanded == null) {
        Modifier.padding(top = Dimens.SectionHeaderTop, bottom = Dimens.GapSmall)
    } else {
        Modifier.padding(vertical = Dimens.GapSmall)
    }
    Row(
        modifier = modifier.fillMaxWidth().then(toggle),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f).then(inner)) {
            Row(
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
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                if (expanded != null) ExpandCaret(expanded = expanded, tint = color)
            }
            if (teaser != null) {
                Text(
                    text = teaser,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
                    modifier = Modifier.padding(top = Dimens.GapSmall),
                )
            }
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

@Preview
@Composable
private fun SectionHeaderExpandablePreview() {
    AtemkraftTheme {
        SectionHeader(
            title = stringResource(R.string.situations_my_patterns),
            color = MaterialTheme.colorScheme.primary,
            teaser = stringResource(R.string.situations_subtitle),
            expanded = false,
        )
    }
}
