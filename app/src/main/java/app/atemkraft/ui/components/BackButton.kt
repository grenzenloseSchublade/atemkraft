package app.atemkraft.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.atemkraft.R

/**
 * Einheitlicher Zurück-Button (Material-Pfeil, RTL-gespiegelt). Bündig an die Inhaltskante
 * rückt ihn der Aufrufer per [flushStart] ([PushHeader]): Die Tippfläche ragt um
 * `Sizes.TouchOverhang` in den Rand, der Pfeil steht an der Kante.
 */
@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppIconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.action_back),
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }
}
