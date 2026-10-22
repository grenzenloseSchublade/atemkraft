package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes

/**
 * Geteilte „Now-Playing"-Leiste über der NavigationBar (Titel + Status + ˄), tippen öffnet die
 * jeweilige Vollbild-Ansicht. App-weit genutzt von Atem-Session ([app.atemkraft.ui.session.MiniSessionBar])
 * und Meditation ([app.atemkraft.ui.meditation.MiniMeditationBar]).
 *
 * Nichts wird gekürzt (LAYOUT-03): Ein langer Übungsname bricht an Wortgrenzen um, statt auf
 * eine unleserliche Größe zu schrumpfen – die Leiste wird dann höher. Der kurze Status („Läuft ·
 * 3:12“) bleibt einzeilig und verkleinert sich notfalls. Der Titel (auch ein eigener Mustername)
 * steht wörtlich, ohne Sanskrit-Auszeichnung (TEXT-14).
 */
@Composable
fun MiniNowPlayingBar(title: String, statusText: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            // Zwei Textzeilen plus Polster ergeben schon ≈ 50 dp; die Mindesthöhe sichert nur
            // das Touch-Ziel (früher 60 dp mit sichtbarem Leerraum).
            .heightIn(min = Dimens.MinTouchTarget),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.GapSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                WholeWordText(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = SECONDARY),
                    marks = false,
                )
                WholeWordText(
                    text = statusText,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    singleLine = true,
                )
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = stringResource(R.string.mini_open),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Sizes.IconDefault),
            )
        }
    }
}
