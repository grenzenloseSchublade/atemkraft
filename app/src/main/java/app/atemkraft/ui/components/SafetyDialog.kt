package app.atemkraft.ui.components

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import app.atemkraft.R
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

/**
 * Start hinter dem Sicherheitsdialog: Die zurückgegebene Funktion startet direkt ([proceed]),
 * oder – wenn [require] (`SafetySettings.requiresConfirm`) – zeigt erst den [SafetyDialog] mit
 * [cautions]. Bestätigen merkt sich die Bestätigung ([onConfirm], z. B. in den Einstellungen)
 * und startet dann; Abbrechen schließt nur. Der offene Dialog überlebt Rotation und
 * Prozess-Neustart (CODE-03). Die eine Abfolge für Detailseite und Muster-Editor.
 */
@Composable
fun rememberSafetyGate(
    require: Boolean,
    cautions: List<String>,
    onConfirm: () -> Unit,
    proceed: () -> Unit,
): () -> Unit {
    var showing by rememberSaveable { mutableStateOf(false) }
    if (showing) {
        SafetyDialog(
            cautions = cautions,
            onConfirm = {
                showing = false
                onConfirm()
                proceed()
            },
            onDismiss = { showing = false },
        )
    }
    val currentRequire by rememberUpdatedState(require)
    val currentProceed by rememberUpdatedState(proceed)
    return remember { { if (currentRequire) showing = true else currentProceed() } }
}
