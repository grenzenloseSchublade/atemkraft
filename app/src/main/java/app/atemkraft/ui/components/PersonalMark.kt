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
 * Persönliche Markierung „tut mir gut“: die eigene Einschätzung neben der Studienlage. Kein Herz
 * und kein Lesezeichen (das heißt hier „Muster speichern“), sondern der Atemkreis des Pacers –
 * leer als Ring, markiert gefüllt. Im Akzent `secondary` wie jeder gesetzte Zustand (KOMP-02).
 */

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
        Icon(
            painterResource(if (marked) R.drawable.ic_breath_mark_filled else R.drawable.ic_breath_mark),
            contentDescription = stringResource(R.string.cd_personal_mark),
            tint = if (marked) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY)
            },
        )
    }
}

@Preview
@Composable
private fun PersonalMarkPreview() {
    AtemkraftTheme {
        PersonalMarkToggle(marked = true, onMarkedChange = {})
    }
}
