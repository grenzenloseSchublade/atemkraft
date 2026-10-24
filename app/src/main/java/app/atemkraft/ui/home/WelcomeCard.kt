package app.atemkraft.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.domain.Exercise
import app.atemkraft.ui.components.AppIconButton
import app.atemkraft.ui.components.AppTextButton
import app.atemkraft.ui.components.BodyText
import app.atemkraft.ui.components.CardHeader
import app.atemkraft.ui.components.ForwardLinkText
import app.atemkraft.ui.components.HintText
import app.atemkraft.ui.components.flushStart
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY

/**
 * Willkommenskarte beim ersten Start oben im Atmen-Tab (Nutzer 2026-10-24): kein mehrseitiges
 * Onboarding, sondern eine ruhige Karte mit dem Einstieg ([exercise], die Resonanz-Atmung als
 * Verweis in `secondary`, FARBE-04) und dem Gesundheitshinweis, den sonst nur „Über“ trägt – vor
 * der ersten Sitzung, nicht drei Ebenen tief.
 *
 * Nicht klickbar; die eine Aktion „Hinweis schließen“ (✕) steht auf der Titelzeile wie bei jeder
 * Karte mit Aktionen ([CardHeader], STYLEGUIDE §4.5): Nur der Titel hält ihre Breite frei, der
 * Text darunter läuft durch. Weggeklickt bleibt sie weg (DataStore, `SettingsRepository.welcomeDismissed`).
 */
@Composable
internal fun WelcomeCard(exercise: Exercise, onOpen: () -> Unit, onDismiss: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            CardHeader(title = stringResource(R.string.welcome_title), actionCount = 1) {
                AppIconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_welcome_dismiss),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                    )
                }
            }
            Spacer(Modifier.height(Dimens.GapTiny))
            BodyText(stringResource(R.string.welcome_body))
            // Bündig: Das Polster des Text-Buttons ragt in den Rand.
            AppTextButton(onClick = onOpen, modifier = Modifier.flushStart(inCard = true)) {
                ForwardLinkText(text = exercise.name, color = MaterialTheme.colorScheme.secondary)
            }
            HintText(stringResource(R.string.welcome_health))
        }
    }
}
