package app.atemkraft.ui.components

import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Sizes

/**
 * Einheitlicher Zurück-Button (Material-Pfeil, RTL-gespiegelt). Der Versatz um den Rand
 * zwischen 48-dp-Tippfläche und Icon richtet das Icon an der Inhaltskante
 * (Dimens.ScreenPadding) aus – er folgt der Icon-Größe, statt 24 dp vorauszusetzen.
 */
@Composable
fun BackButton(onClick: () -> Unit) {
    AppIconButton(onClick = onClick, modifier = Modifier.offset(x = -(Dimens.MinTouchTarget - Sizes.IconDefault) / 2)) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.action_back),
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }
}
