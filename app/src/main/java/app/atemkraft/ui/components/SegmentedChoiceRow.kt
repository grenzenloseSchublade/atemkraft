package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SingleChoiceSegmentedButtonRowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Sizes

/**
 * Einheitliche Einfachauswahl – ersetzt die mehrfach kopierten
 * `SingleChoiceSegmentedButtonRow`-Blöcke in den Einstellungen.
 *
 * Normalfall: SegmentedButtons über die volle Breite. Passt eine Beschriftung (inklusive
 * Häkchen des ausgewählten Segments) nicht einzeilig in ihr gleich breites Segment, wird
 * stattdessen eine senkrechte Liste mit Radiobuttons gezeigt – Beschriftungen brechen nie
 * im Wort um (LAYOUT-03).
 *
 * Mehr als vier Optionen nur als Raster über [columns] (Meditations-Dauer 4 × 2): gleich breite
 * Segmente in Reihen à [columns], gemeinsame Auswahl, gemeinsamer Rückfall auf die Radioliste.
 * Die Reihen stehen ohne eigene Lücke übereinander – der Abstand ist der Rest der
 * 48-dp-Tippfläche, enger ginge nur mit überlappenden Tippflächen (A11Y-05). Ein [selected], das
 * nicht in [items] liegt (z. B. Stepper-Wert neben Schnellwahl), markiert keines.
 */
@Composable
fun <T> SegmentedChoiceRow(
    items: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = items.size,
    label: @Composable (T) -> String,
) {
    val rows = items.chunked(columns)
    SubcomposeLayout(modifier.fillMaxWidth()) { constraints ->
        // Jede Beschriftung einzeln als ausgewähltes Segment messen (breiteste Variante).
        // Die Zeile misst mit width(IntrinsicSize.Min), also nur das längste Wort – deshalb
        // in der Probe untrennbar machen, sonst gilt „Start & Ende“ als so breit wie „Start“.
        val needed = subcompose("probe") {
            rows.forEach { row ->
                row.forEachIndexed { index, item ->
                    SingleChoiceSegmentedButtonRow {
                        Segment(label(item).unbreakable(), selected = true, onClick = {}, index = index, count = row.size)
                    }
                }
            }
        }.maxOf { it.maxIntrinsicWidth(Constraints.Infinity) }
        val fits = needed * columns <= constraints.maxWidth

        val placeables = subcompose(fits) {
            if (fits) {
                // Je Reihe ein Placeable; layout() unten stapelt sie.
                rows.forEach { row ->
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        row.forEachIndexed { index, item ->
                            Segment(label(item), selected = item == selected, onClick = { onSelect(item) }, index = index, count = row.size)
                        }
                    }
                }
            } else {
                Column(Modifier.fillMaxWidth().selectableGroup()) {
                    items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = Dimens.MinTouchTarget)
                                .selectable(
                                    selected = item == selected,
                                    onClick = { onSelect(item) },
                                    role = Role.RadioButton,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Nur Anzeige (Tippfläche trägt die Zeile), deshalb verkleinert wie der Switch.
                            RadioButton(
                                selected = item == selected,
                                onClick = null,
                                modifier = Modifier.scaledLayout(Sizes.ControlScale),
                            )
                            Spacer(Modifier.width(Dimens.GapSmall))
                            Text(
                                text = label(item),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }.map { it.measure(constraints.copy(minHeight = 0)) }

        layout(constraints.maxWidth, placeables.sumOf { it.height }) {
            var y = 0
            placeables.forEach {
                it.placeRelative(0, y)
                y += it.height
            }
        }
    }
}

/** Leerzeichen und Bindestriche geschützt, damit die Mindestbreite die ganze Beschriftung ist. */
private fun String.unbreakable(): String = replace(' ', '\u00A0').replace('-', '\u2011')

/** Ein Segment; Probe und Anzeige nutzen dasselbe, damit die Breitenmessung stimmt. */
@Composable
private fun SingleChoiceSegmentedButtonRowScope.Segment(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    index: Int,
    count: Int,
) {
    SegmentedButton(
        selected = selected,
        onClick = onClick,
        shape = SegmentedButtonDefaults.itemShape(index, count),
        modifier = Modifier.heightIn(min = Sizes.ButtonHeight),
        icon = {
            // activeContent benannt: ein nachgestelltes Lambda wäre inactiveContent und setzte
            // das Häkchen an die NICHT gewählten Segmente.
            SegmentedButtonDefaults.Icon(
                active = selected,
                activeContent = {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(Sizes.IconInButton)) // dekorativ: Zustand trägt die Rolle RadioButton
                },
            )
        },
    ) { Text(text, maxLines = 1) }
}
