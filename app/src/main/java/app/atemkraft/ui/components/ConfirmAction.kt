package app.atemkraft.ui.components

import android.view.accessibility.AccessibilityManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.SECONDARY
import kotlinx.coroutines.delay

/** Rückfall aus dem Bestätigungszustand in den Ausgangszustand (MUSTER-05). */
private const val CONFIRM_TIMEOUT_MS = 4_000L

/**
 * Zustand einer zerstörenden Aktion, die zweistufig im selben Element bestätigt wird
 * (MUSTER-05): [tap] schärft beim ersten Mal ([armed]), erst der zweite Tipp führt die Aktion
 * aus. [focusModifier] gehört an das bedienbare Element – solange es Fokus hat, fällt der
 * Zustand nicht zurück. Anlegen mit [rememberArmedConfirm].
 */
@Stable
class ArmedConfirm internal constructor() {
    var armed by mutableStateOf(false)
        private set
    internal var focused by mutableStateOf(false)

    /** Erster Aufruf schärft, der zweite entschärft und führt [onConfirm] aus. */
    fun tap(onConfirm: () -> Unit) {
        if (armed) {
            armed = false
            onConfirm()
        } else {
            armed = true
        }
    }

    internal fun disarm() {
        armed = false
    }

    /** Beobachtet den Fokus des Elements (Tastatur, D-Pad); vor dessen clickable setzen. */
    val focusModifier: Modifier = Modifier.onFocusChanged { focused = it.hasFocus }
}

/**
 * Zweistufige Bestätigung (MUSTER-05). Ohne zweiten Tipp fällt sie nach 4 s zurück – nicht,
 * solange ein Bedienungshilfe-Dienst läuft (TalkBack, Switch Access brauchen länger;
 * [rememberAccessibilityActive]) oder das Element Fokus hat. Bewusst `remember`: Wer die Seite
 * verlässt, kommt entschärft zurück. In Listen pro Eintrag in `key(id)` aufrufen, damit der
 * Zustand beim Löschen nicht auf den nächsten Eintrag übergeht.
 */
@Composable
fun rememberArmedConfirm(): ArmedConfirm {
    val state = remember { ArmedConfirm() }
    val a11yActive = rememberAccessibilityActive()
    LaunchedEffect(state.armed, state.focused, a11yActive) {
        if (state.armed && !state.focused && !a11yActive) {
            delay(CONFIRM_TIMEOUT_MS)
            state.disarm()
        }
    }
    return state
}

/**
 * Ob irgendein Bedienungshilfe-Dienst läuft (TalkBack, Switch Access, Voice Access …); folgt
 * Änderungen live. Bewusst breiter als Touch-Erkundung: Switch Access schaltet sie nicht ein. Dass
 * auch andere Dienste (z. B. ein Passwortmanager) zählen, kostet nur den Rückfall nach 4 s bzw. die
 * Ruhe-Ansicht der Sitzung ([CalmHost]) – die sichere Richtung.
 */
@Composable
fun rememberAccessibilityActive(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(AccessibilityManager::class.java) }
    var enabled by remember(manager) { mutableStateOf(manager?.isEnabled == true) }
    DisposableEffect(manager) {
        val listener = AccessibilityManager.AccessibilityStateChangeListener { enabled = it }
        manager?.addAccessibilityStateChangeListener(listener)
        onDispose { manager?.removeAccessibilityStateChangeListener(listener) }
    }
    return enabled
}

/**
 * Löschen-Knopf, zweistufig im selben Element (MUSTER-05): Der erste Tipp wechselt das Icon
 * (Papierkorb → Papierkorb mit Kreuz, Farbe `error`) und die Beschreibung zu „‚[name]‘ wirklich
 * löschen?“, die Live-Region sagt das an, ohne dass der Fokus springt (A11Y-04). Erst der zweite
 * Tipp ruft [onConfirm]. [description] nennt Art und Namen („Muster Ruhiger Fluss löschen“), damit
 * TalkBack und Voice Access die Knöpfe einer Liste unterscheiden (A11Y-06). [confirmDescription]
 * ersetzt die Bestätigungsfrage, wenn „löschen“ nicht passt („Logbuch wirklich leeren?“). Gleich
 * groß in beiden Zuständen, die Karte springt nicht.
 */
@Composable
fun ConfirmIconButton(
    name: String,
    description: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmDescription: String = stringResource(R.string.cd_delete_confirm, name),
) {
    val confirm = rememberArmedConfirm()
    AppIconButton(
        onClick = { confirm.tap(onConfirm) },
        modifier = modifier
            .then(confirm.focusModifier)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        if (confirm.armed) {
            Icon(
                painterResource(R.drawable.ic_delete_forever),
                contentDescription = confirmDescription,
                tint = MaterialTheme.colorScheme.error,
            )
        } else {
            Icon(
                Icons.Filled.Delete,
                contentDescription = description,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
            )
        }
    }
}

@Preview
@Composable
private fun ConfirmIconButtonPreview() {
    AtemkraftTheme {
        ConfirmIconButton(
            name = "Thorsten",
            description = stringResource(R.string.cd_voice_delete, "Thorsten"),
            onConfirm = {},
        )
    }
}
