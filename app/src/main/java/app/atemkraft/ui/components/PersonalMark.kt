package app.atemkraft.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes

/*
 * Das eine Zeichen für „zu mir“ (Auswertung 2026-10-24, Abschnitt 3.2): der Atemkreis des Pacers,
 * leer als Ring, gesetzt gefüllt, im Akzent `secondary` wie jeder gesetzte Zustand (KOMP-02). Er
 * steht für die persönliche Markierung „tut mir gut“ (die eigene Einschätzung neben der
 * Studienlage) und fürs Speichern eines Musters „in Eigene“ (Tagesmuster, Muster-Editor) – beides
 * landet im Tab „Eigene“. Kein Herz, kein Lesezeichen.
 */

/**
 * Der Atemkreis als Icon eines Schalters: [filled] gefüllt in `secondary`, sonst Ring in
 * `onSurface` · `SECONDARY`. Für [PersonalMarkToggle] und die Speichern-Schalter der Muster
 * (in einem [AppIconToggle]); [contentDescription] nennt die Aktion (ICON-02).
 */
@Composable
fun BreathMarkIcon(filled: Boolean, contentDescription: String, modifier: Modifier = Modifier) {
    Icon(
        painterResource(if (filled) R.drawable.ic_breath_mark_filled else R.drawable.ic_breath_mark),
        contentDescription = contentDescription,
        tint = if (filled) {
            MaterialTheme.colorScheme.secondary
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY)
        },
        modifier = modifier,
    )
}

/** Atemkreis auf der Karte, nur wenn markiert; TalkBack liest ihn als „Tut mir gut“. */
@Composable
fun PersonalMarkIndicator(modifier: Modifier = Modifier) {
    Icon(
        painterResource(R.drawable.ic_breath_mark_filled),
        contentDescription = stringResource(R.string.cd_personal_mark),
        tint = MaterialTheme.colorScheme.secondary,
        modifier = modifier.size(Sizes.IconSmall),
    )
}

/** Schalter auf der Detailseite: Ring = nicht markiert, gefüllt = markiert. */
@Composable
fun PersonalMarkToggle(marked: Boolean, onMarkedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    AppIconToggle(
        checked = marked,
        onCheckedChange = onMarkedChange,
        stateDescription = stringResource(if (marked) R.string.state_marked else R.string.state_not_marked),
        modifier = modifier,
    ) {
        BreathMarkIcon(filled = marked, contentDescription = stringResource(R.string.cd_personal_mark))
    }
}

@Preview
@Composable
private fun PersonalMarkPreview() {
    AtemkraftTheme {
        PersonalMarkToggle(marked = true, onMarkedChange = {})
    }
}

@Preview
@Composable
private fun BreathMarkIconPreview() {
    AtemkraftTheme {
        BreathMarkIcon(filled = false, contentDescription = stringResource(R.string.cd_pattern_save))
    }
}
