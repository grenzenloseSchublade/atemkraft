package app.atemkraft.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import app.atemkraft.R
import app.atemkraft.ui.theme.Dimens

/** Blockierender Sicherheits-Hinweis vor intensiven Übungen (Wim Hof, Feueratmung). */
@Composable
fun SafetyDialog(
    cautions: List<String>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
        title = { Text(stringResource(R.string.safety_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny)) {
                cautions.forEach { caution ->
                    Text(
                        text = "• $caution",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.safety_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.safety_dismiss)) } },
        properties = DialogProperties(dismissOnClickOutside = false),
    )
}
