package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY

/**
 * Einheitlicher Kopf der Top-Level-Tabs: Screen-Titel, optionaler Untertitel und rechts die
 * Icon-Aktionen, immer an derselben Stelle: außen höchstens eine Aktion ([action], ein
 * `AppIconButton`) – das Zahnrad ([SettingsAction]) bzw. die Lupe im Situationen-Tab –, links
 * daneben der Kalender zum Logbuch ([onOpenLogbook], [LogbookAction], in jedem Tab, Nutzer
 * 2026-10-22). Das äußere Icon steht bündig an der Inhaltskante ([flushEnd]). Der Titel bricht nie im Wort
 * ([WholeWordText]); neben zwei Icons wird er bei großer Schrift notfalls kleiner.
 *
 * [subtitleContent] ersetzt den Untertitel (z. B. durch das [SearchField]): Der Kopf wechselt
 * nur seinen Zustand, es entsteht keine neue Fläche (PRIN-03).
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
    subtitleContent: (@Composable () -> Unit)? = null,
    onOpenLogbook: (() -> Unit)? = null,
) {
    Row(
        // Einheitlicher Abstand oben auf Tab-Screens.
        modifier = modifier.fillMaxWidth().padding(top = Dimens.ScreenTop),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            WholeWordText(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitleContent != null) {
                subtitleContent()
            } else if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
                )
            }
        }
        // Glyph an der Inhaltskante wie der Caret der Abschnittsköpfe darunter; die Tippfläche
        // ragt in den Screen-Rand (LAYOUT-07).
        if (action != null || onOpenLogbook != null) {
            Row(Modifier.flushEnd(), verticalAlignment = Alignment.CenterVertically) {
                if (onOpenLogbook != null) LogbookAction(onClick = onOpenLogbook)
                if (action != null) action()
            }
        }
    }
}

/**
 * Kalender für [ScreenHeader]: öffnet das Logbuch (cd „Logbuch“), in jedem Tab links neben der
 * übrigen Kopf-Aktion (Nachtrag 2, Nutzer 2026-10-22).
 */
@Composable
fun LogbookAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppIconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(R.drawable.ic_calendar),
            contentDescription = stringResource(R.string.settings_log_title),
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** Zahnrad für [ScreenHeader]`.action`: öffnet die Einstellungen (cd `settings_title`). */
@Composable
fun SettingsAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppIconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = stringResource(R.string.settings_title),
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Preview
@Composable
private fun ScreenHeaderPreview() {
    AtemkraftTheme {
        ScreenHeader(
            title = stringResource(R.string.meditation_title),
            subtitle = stringResource(R.string.meditation_subtitle),
            action = { SettingsAction(onClick = {}) },
            onOpenLogbook = {},
        )
    }
}
