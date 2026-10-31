package app.atemkraft.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes

/**
 * Tabs in der Seite (Eigentümer 2026-10-31, Tab „Meins“: „Eigene Kreationen · Tut mir gut“) als
 * feste Tabs nach Material 3 (für 2–6 Tabs): Jeder Tab bekommt gleich viel Breite über die ganze
 * Zeile, der Name steht mittig in `titleSmall` (M3-Tab-Beschriftung), das Gewicht bleibt bei Auswahl
 * gleich (TYPO-01). Der gewählte in `primary` (Pink = Auswahl, FARBE-04; 5,98 auf dem Grund) mit
 * einem pinken Strich darunter, so breit wie der Name ([Sizes.PageTabIndicator]); die anderen
 * gedämpft (`onSurface` `SECONDARY`). Keine Zahl im Tab: Die Anzahl steht unter den Karten.
 * Linksbündig dicht nebeneinander lasen sich zwei Tabs wie eine Zeile (Eigentümer: „zu nah
 * beieinander“).
 *
 * Jeder Tab ist mindestens `MinTouchTarget` hoch, `Role.Tab` mit Auswahlzustand in einer
 * `selectableGroup` – Auswahl ohne Häkchen (A11Y-03). Bei großer Schrift bricht ein Name in seiner
 * Spalte zwischen den Wörtern um, nie im Wort (LAYOUT-03). Nicht für Werte (Dauer, Klang): dafür
 * [SegmentedChoiceRow].
 */
@Composable
fun <T> PageTabs(
    items: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth().selectableGroup()) {
        items.forEach { item ->
            PageTab(name = label(item), selected = item == selected, onClick = { onSelect(item) }, modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Leerraum über dem Namen, den die Mindesthöhe der Tippfläche (48) einem Tab gibt: Der Name steht
 * unten, darüber bleibt, was Name, `GapSmall` und Strich nicht füllen. Wer über den Tabs einen
 * sichtbaren Abstand setzt (`SectionHeaderTop`), zieht ihn ab – wie beim tippbaren Abschnittskopf.
 */
@Composable
fun pageTabsTopSlack(): Dp {
    val content = rememberFirstLineHeight(MaterialTheme.typography.titleSmall) + Dimens.GapSmall + Sizes.PageTabIndicator
    return (Dimens.MinTouchTarget - content).coerceAtLeast(0.dp)
}

@Composable
private fun PageTab(name: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY)
    var labelWidth by remember { mutableIntStateOf(0) }
    Column(
        modifier = modifier
            .heightIn(min = Dimens.MinTouchTarget)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WholeWordText(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            color = color,
            textAlign = TextAlign.Center,
            modifier = Modifier.onSizeChanged { labelWidth = it.width },
        )
        Spacer(Modifier.height(Dimens.GapSmall))
        // Jeder Tab hält die Höhe des Strichs, nur der gewählte zeigt ihn: Beim Wechsel springt nichts.
        val stripe = with(LocalDensity.current) { labelWidth.toDp() }
        Box(
            Modifier
                .width(stripe)
                .height(Sizes.PageTabIndicator)
                .then(if (selected) Modifier.background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall) else Modifier),
        )
    }
}

@Preview
@Composable
private fun PageTabsPreview() {
    AtemkraftTheme {
        val names = listOf(stringResource(R.string.own_creations), stringResource(R.string.own_favorites))
        PageTabs(items = names, selected = names.first(), onSelect = {}, label = { it })
    }
}
