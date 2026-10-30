package app.atemkraft.ui.detail

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.ui.components.AppBottomSheet

/**
 * Blatt „Anpassen“ der Detailseite (MUSTER-11): darin die Einstellungen ([content], die
 * `SessionAdjustControls` mit Dauer bzw. Runden und – wo erlaubt – „Phasen anpassen“), direkt auf
 * dem Blatt wie „Klang & Stimme“. Jede Änderung gilt sofort und wird gespeichert, ein Bestätigen
 * gibt es nicht; das Zurücksetzen bleibt am Start-Knopf darunter. Schließen per Wischen nach unten,
 * Tipp daneben oder Zurück ([onDismiss]). Gerüst: [AppBottomSheet].
 */
@Composable
internal fun AdjustSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AppBottomSheet(title = stringResource(R.string.adjust_title), onDismiss = onDismiss, content = content)
}
