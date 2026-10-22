package app.atemkraft.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens

/**
 * Großer runder Start-Knopf „Übung starten“ für knappe Leisten (Muster-Editor, Nachtrag 6):
 * gefüllter Kreis in `primary` ([Dimens.SessionSideButton], so groß wie die Seitenknöpfe der
 * Session, also selbst Tippfläche), Play-Dreieck in `onPrimary` ([Dimens.SessionSideIcon]). Die
 * einzige gefüllte Neon-Fläche des Screens (PRIN-02) und deutlich anders als eine kleine,
 * ungefüllte Vorschau-▶. [description] ist der ganze Wortlaut für TalkBack („Übung starten, ca. 5
 * Minuten“).
 */
@Composable
fun StartPlayButton(onClick: () -> Unit, description: String, modifier: Modifier = Modifier) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(Dimens.SessionSideButton),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = description, modifier = Modifier.size(Dimens.SessionSideIcon))
    }
}

@Preview
@Composable
private fun StartPlayButtonPreview() {
    AtemkraftTheme { StartPlayButton(onClick = {}, description = "Übung starten, ca. 5 Minuten") }
}
