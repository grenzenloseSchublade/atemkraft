package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import app.atemkraft.ui.theme.Dimens

/**
 * Gerüst jedes Bottom Sheets (MUSTER-11, Anatomie „Bottom Sheet“ in STYLEGUIDE §4.5) an einer
 * Stelle: ganz geöffnet, Fläche `surface`, eigener Griff, darunter eine scrollende Spalte mit Rand
 * `ScreenPadding` seitlich und `ScreenBottom` unten (über den Systemleisten, die das Blatt selbst
 * freihält). Oben steht [title] als [SectionHeader] mit `top = 0`, [intro] als dessen Teaser
 * (`bodySmall` `SECONDARY`, ganze Sätze, höchstens 95 Zeichen), dann `HeaderGap` bis zum Inhalt.
 *
 * Der Inhalt ([content]) folgt der Anatomie: Steuerungen stehen direkt auf dem Blatt, ohne Karte
 * und ohne Trenner, ihre Labels an derselben Textkante wie der Titel; gegliedert wird mit
 * [SubLabel] und Abstand. Karten nur für Dinge, die man öffnet oder startet (Muster, [NavCard]).
 * Jede Änderung gilt sofort, ein Bestätigen gibt es nicht.
 *
 * Der Griff ist dekorativ: Der M3-Griff wäre als Knoten nur 32 dp breit (A11Y-05), ohne Aktion
 * wäre er ein leerer Stopp (A11Y-06) – geschlossen wird per Wischen, Tipp daneben („Blatt
 * schließen“) oder Zurück ([onDismiss]). TalkBack liest die Spalte vor dem Griff
 * (`traversalIndex`), so ist der Titel das erste Element und bekommt beim Öffnen den Fokus.
 * [sheetState] gibt der Aufrufer nur, wenn er das Blatt selbst schließt, bevor er etwas meldet
 * (Startpunkt: `hide()`, dann `onPick`); sonst ganz geöffnet ohne Zwischenstufe. Weil `SheetState`
 * in der Signatur steht, brauchen Aufrufer `@OptIn(ExperimentalMaterial3Api::class)`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    intro: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface, dragHandle = null) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BottomSheetDefaults.DragHandle(Modifier.semantics { hideFromAccessibility() })
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    isTraversalGroup = true
                    traversalIndex = -1f
                }
                .verticalScroll(rememberScrollState())
                .padding(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, bottom = Dimens.ScreenBottom),
        ) {
            SectionHeader(title = title, teaser = intro, top = 0.dp)
            Spacer(Modifier.height(Dimens.HeaderGap))
            content()
        }
    }
}
