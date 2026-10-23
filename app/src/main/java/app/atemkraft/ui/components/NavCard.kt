package app.atemkraft.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes

/**
 * Karte, die woandershin führt (Tab „Lernen“: Kapitel; Einstellungen: „Über Atemkraft“; Über:
 * Quellcode und Datenschutz): [title] als [CardTitle] (`titleMedium`, Überschrift für TalkBack,
 * A11Y-01) in [titleColor], darunter optional [hint] als [HintText], rechts der [ForwardChevron].
 * Die ganze Karte ist der Button (Tippfläche ≥ 48 dp), Polster `CardPadding`.
 *
 * [external]: Das Ziel öffnet der Browser – statt des Chevrons das Symbol „öffnet im Browser“,
 * TalkBack sagt „Link öffnen“ an.
 */
@Composable
fun NavCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    external: Boolean = false,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.MinTouchTarget)
                .clickable(
                    role = Role.Button,
                    onClickLabel = if (external) stringResource(R.string.about_open_link) else null,
                    onClick = onClick,
                )
                .padding(Dimens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                CardTitle(title, color = titleColor)
                if (hint != null) HintText(hint)
            }
            val tint = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY)
            if (external) {
                Icon(
                    painter = painterResource(R.drawable.ic_open_in_new),
                    contentDescription = null, // dekorativ: Name und Hinweis „Link öffnen“ trägt die Karte
                    tint = tint,
                    modifier = Modifier.size(Sizes.IconSmall),
                )
            } else {
                ForwardChevron(tint = tint)
            }
        }
    }
}

@Preview
@Composable
private fun NavCardPreview() {
    AtemkraftTheme {
        NavCard(title = stringResource(R.string.settings_atemlehre), hint = stringResource(R.string.learn_atemlehre_hint), onClick = {})
    }
}
