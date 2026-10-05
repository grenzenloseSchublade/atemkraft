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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY

/**
 * Einheitlicher Kopf der Top-Level-Tabs: Screen-Titel, optionaler Untertitel und rechts
 * höchstens eine Icon-Aktion ([action], ein `AppIconButton`), immer an derselben Stelle –
 * das Zahnrad ([SettingsAction]) bei Tabs mit eigenen Einstellungen, die Lupe im
 * Situationen-Tab.
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
) {
    Row(
        // Einheitlicher Abstand oben auf Tab-Screens.
        modifier = modifier.fillMaxWidth().padding(top = Dimens.ScreenTop),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitleContent != null) {
                subtitleContent()
            } else if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
                )
            }
        }
        action?.invoke()
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
        )
    }
}
