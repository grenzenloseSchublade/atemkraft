package app.atemkraft.ui.components

import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.atemkraft.R

/**
 * Einheitlicher Zurück-Button (Material-Pfeil, RTL-gespiegelt). Der −12dp-Versatz richtet
 * das Glyph an der 20dp-Inhaltskante aus (IconButton hat 12dp Innenabstand bei 48dp Ziel).
 */
@Composable
fun BackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.offset(x = (-12).dp)) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.action_back),
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }
}
