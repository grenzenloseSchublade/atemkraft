package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY

/**
 * Fester Kopf der Top-Level-Tabs: dieselbe Zeile wie der Kopf der Push-Seiten ([HeaderBar],
 * Titel `headlineSmall`), nur ohne Pfeil – beim Wechsel springt nichts. Rechts die Icon-Aktionen,
 * immer an derselben Stelle: außen höchstens eine Aktion ([action], ein `AppIconButton`) – das
 * Zahnrad ([SettingsAction]) bzw. die Lupe im Situationen-Tab –, links daneben der Kalender zum
 * Logbuch ([onOpenLogbook], [LogbookAction], nur im Atmen-Tab links neben dem Zahnrad, Nutzer
 * 2026-10-22). Das äußere Icon steht bündig an der Inhaltskante ([flushEnd]). Der Titel bleibt eine
 * Zeile und läuft notfalls als Laufschrift ([HeaderTitle]).
 *
 * Der Kopf steht außerhalb der Liste und scrollt nie weg (Nutzer 2026-10-23: „Rahmen fest, Inhalt
 * scrollt“), damit Zahnrad, Kalender und Lupe immer erreichbar sind. Der Untertitel gehört zum
 * Inhalt ([ScreenSubtitle], erster Eintrag der Liste). [below] steht fest unter der Zeile (das
 * [SearchField] des Situationen-Tabs, solange die Suche offen ist): Getippt wird oben, die Treffer
 * scrollen. [scrolled] (Inhalt liegt unter dem Kopf) blendet die Haarlinie an der Unterkante ein.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    onOpenLogbook: (() -> Unit)? = null,
    scrolled: Boolean = false,
    below: (@Composable () -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().scrolledHairline(scrolled)) {
        HeaderBar {
            HeaderTitle(title, Modifier.weight(1f))
            // Glyph an der Inhaltskante wie der Caret der Abschnittsköpfe darunter; die Tippfläche
            // ragt in den Screen-Rand (LAYOUT-07).
            if (action != null || onOpenLogbook != null) {
                Row(Modifier.flushEnd(), verticalAlignment = Alignment.CenterVertically) {
                    if (onOpenLogbook != null) LogbookAction(onClick = onOpenLogbook)
                    if (action != null) action()
                }
            }
        }
        if (below != null) {
            Box(Modifier.padding(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, bottom = Dimens.GapSmall)) { below() }
        }
    }
}

/** Untertitel eines Tabs: erster Eintrag des scrollenden Inhalts unter dem festen [ScreenHeader]. */
@Composable
fun ScreenSubtitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
        modifier = modifier,
    )
}

/**
 * Kalender für [ScreenHeader]: öffnet das Logbuch (cd „Logbuch“), nur im Atmen-Tab links neben dem
 * Zahnrad (Nutzer 2026-10-22).
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
            title = stringResource(R.string.tab_breathe),
            action = { SettingsAction(onClick = {}) },
            onOpenLogbook = {},
        )
    }
}
