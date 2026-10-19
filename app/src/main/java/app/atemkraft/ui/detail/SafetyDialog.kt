package app.atemkraft.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import app.atemkraft.R
import app.atemkraft.ui.components.AppTextButton
import app.atemkraft.ui.components.Bullet
import app.atemkraft.ui.theme.Dimens

/**
 * Blockierender Sicherheits-Hinweis vor intensiven Übungen (Wim Hof, Feueratmung). Die
 * Hinweisliste scrollt (MUSTER-06): Der M3-Dialog gibt dem Text nur den Rest der Höhe und
 * scrollt nicht selbst – bei großer Schrift oder quer würde der letzte Hinweis sonst still
 * abgeschnitten (LAYOUT-03).
 */
@Composable
fun SafetyDialog(
    cautions: List<String>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Warning, contentDescription = null) }, // dekorativ: der Titel sagt es
        title = { Text(stringResource(R.string.safety_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny),
            ) {
                cautions.forEach { Bullet(it) }
            }
        },
        confirmButton = { AppTextButton(onClick = onConfirm) { Text(stringResource(R.string.safety_confirm)) } },
        dismissButton = { AppTextButton(onClick = onDismiss) { Text(stringResource(R.string.safety_dismiss)) } },
        properties = DialogProperties(dismissOnClickOutside = false),
    )
}
