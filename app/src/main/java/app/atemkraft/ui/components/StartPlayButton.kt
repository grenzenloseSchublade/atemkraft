package app.atemkraft.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Sizes

/**
 * Start-Knopf „Übung starten“ der Leiste unten im Muster-Editor (Nachtrag 6 und 7): gefüllter Kreis
 * in `primary`, Play-Dreieck in `onPrimary` – wie [AppIconButton] sichtbar [Sizes.IconButtonSize],
 * Tippfläche 48 dp. Gleich groß wie der Dauer-Stepper daneben (ein Größentoken je Leiste,
 * LAYOUT-04); hervorgehoben nur durch die Füllung, die einzige gefüllte Neon-Fläche des Screens
 * (PRIN-02), deutlich anders als die kleine, ungefüllte Vorschau-▶ unter der Kurve. [description]
 * ist der ganze Wortlaut für TalkBack („Übung starten, ca. 5 Minuten“).
 */
@Composable
fun StartPlayButton(onClick: () -> Unit, description: String, modifier: Modifier = Modifier) {
    AppIconButton(
        onClick = onClick,
        modifier = modifier,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = description)
    }
}

@Preview
@Composable
private fun StartPlayButtonPreview() {
    AtemkraftTheme { StartPlayButton(onClick = {}, description = "Übung starten, ca. 5 Minuten") }
}
