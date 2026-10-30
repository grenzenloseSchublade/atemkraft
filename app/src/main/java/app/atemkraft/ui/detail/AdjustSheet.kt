package app.atemkraft.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.theme.Dimens

/**
 * Blatt „Anpassen“ der Detailseite (MUSTER-11): darin die Einstell-Karte ([content], die
 * `SessionAdjustCard` mit Dauer bzw. Runden und – wo erlaubt – „Phasen anpassen“). Jede Änderung
 * gilt sofort und wird gespeichert, ein Bestätigen gibt es nicht; das Zurücksetzen bleibt am
 * Start-Knopf darunter. Schließen per Wischen nach unten, Tipp daneben oder Zurück ([onDismiss]).
 *
 * Gerüst wie [app.atemkraft.ui.meditation.SoundSheet] (Anatomie „Bottom Sheet“, §4.5): ganz
 * geöffnet, eigener stummer Griff, Titel als Abschnittskopf mit `top = 0`, TalkBack beginnt beim
 * Titel; bei großer Schrift quer scrollt der Inhalt des Blatts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdjustSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface, dragHandle = null) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            // Stumm für TalkBack wie im SoundSheet (A11Y-05, -06).
            BottomSheetDefaults.DragHandle(Modifier.semantics { invisibleToUser() })
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Vor dem Griff gelesen: TalkBack beginnt beim Titel (MUSTER-11).
                .semantics {
                    isTraversalGroup = true
                    traversalIndex = -1f
                }
                .verticalScroll(rememberScrollState())
                .padding(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, bottom = Dimens.ScreenBottom),
        ) {
            SectionHeader(title = stringResource(R.string.adjust_title), top = 0.dp)
            Spacer(Modifier.height(Dimens.HeaderGap))
            content()
        }
    }
}
