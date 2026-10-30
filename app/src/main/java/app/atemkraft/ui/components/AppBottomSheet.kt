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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import app.atemkraft.ui.theme.Dimens
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
 * Der Griff ist der M3-Griff (`BottomSheetDefaults.DragHandle`: Strich 32 × 4 mittig in einer
 * 48 dp hohen Zone, M3-Maß, kein eigener Abstand); der Titel folgt ohne weiteren Abstand. Er ist
 * dekorativ: Der M3-Griff wäre als Knoten nur 32 dp breit (A11Y-05), ohne Aktion
 * wäre er ein leerer Stopp (A11Y-06) – geschlossen wird per Wischen, Tipp daneben („Blatt
 * schließen“) oder Zurück ([onDismiss]). TalkBack liest die Spalte vor dem Griff
 * (`traversalIndex`), so ist der Titel das erste Element und bekommt beim Öffnen den Fokus.
 *
 * Erst schließen, dann handeln: Ein Element, das woandershin führt oder etwas Großes öffnet
 * (Detailseite, Editor, Stimmen-Katalog, eine Wahl, nach der der Fokus wandert), ruft
 * [AppBottomSheetScope.closeThen] statt seiner Aktion. Sonst baut die neue Seite ihren ersten Frame
 * im selben Moment, in dem das Blatt-Fenster ohne Animation verschwindet und die Seite hineingleitet
 * (Messung am A54, 2026-10-30, Blatt → Editor: 6 % Ruckler, p99 97–150 ms). Der Zustand des Blatts
 * gehört deshalb [AppBottomSheet] allein; Aufrufer legen keinen `SheetState` an (`sheet-direct`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    intro: String? = null,
    content: @Composable AppBottomSheetScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    // Einmal schließen, danach handeln (AppBottomSheetScope.closeThen); ein zweiter Tipp entfällt.
    val closeThen: (() -> Unit) -> Unit = remember(sheetState, scope) {
        var closing = false
        { action ->
            if (!closing) {
                closing = true
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    // Hat das Blatt die Komposition schon verlassen (Drehen), gilt der Tipp nicht mehr.
                    if (scope.isActive) {
                        currentOnDismiss()
                        action()
                    }
                }
            }
        }
    }
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
            val sheetScope = remember(this, closeThen) { SheetContentScope(this, closeThen) }
            SectionHeader(title = title, teaser = intro, top = 0.dp)
            Spacer(Modifier.height(Dimens.HeaderGap))
            sheetScope.content()
        }
    }
}

/**
 * Inhalt eines [AppBottomSheet]: eine Spalte ([ColumnScope]) und der eine Weg, das Blatt vor einer
 * Aktion zu schließen. In einer inneren `Column`/`Row` ist der Bereich nicht implizit erreichbar
 * (`LayoutScopeMarker`); dort vorher `val sheet = this` merken und `sheet.closeThen { … }` rufen.
 */
@Stable
interface AppBottomSheetScope : ColumnScope {
    /**
     * Schließt das Blatt mit seiner Animation (`hide()`), meldet dann `onDismiss` und führt danach
     * [action] aus – im selben Frame, keine zusätzliche Wartezeit. Ein zweiter Aufruf während des
     * Schließens entfällt (Doppeltipp öffnet nichts doppelt). Unterbricht Wischen das Schließen,
     * gilt der Tipp trotzdem; verlässt das Blatt vorher die Komposition (Drehen), entfällt die Aktion.
     */
    fun closeThen(action: () -> Unit)
}

private class SheetContentScope(
    column: ColumnScope,
    private val close: (() -> Unit) -> Unit,
) : AppBottomSheetScope,
    ColumnScope by column {
    override fun closeThen(action: () -> Unit) = close(action)
}
