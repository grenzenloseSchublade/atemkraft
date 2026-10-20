package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SingleChoiceSegmentedButtonRowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SegmentSelected
import app.atemkraft.ui.theme.Sizes
import kotlin.math.floor

/**
 * Einheitliche Einfachauswahl – ersetzt die mehrfach kopierten
 * `SingleChoiceSegmentedButtonRow`-Blöcke in den Einstellungen.
 *
 * Normalfall: SegmentedButtons über die volle Breite. Die Auswahl zeigen überall gleich die helle
 * Fläche [SegmentSelected] (≥ 3:1 gegen das ungewählte Segment) und die fettere Beschriftung – nie
 * ein Häkchen, damit alle Reihen gleich aussehen und lange Beschriftungen Platz haben (Nutzer
 * 2026-10-20). Passt eine Beschriftung nicht einzeilig in ihr gleich breites Segment, bleibt die
 * Reihe eine Reihe: Alle Beschriftungen werden gemeinsam leicht verkleinert, höchstens bis
 * [MIN_LABEL_SCALE]. Erst wenn auch das nicht reicht, kommt eine senkrechte Liste mit
 * Radiobuttons. Beschriftungen brechen nie im Wort um (LAYOUT-03).
 *
 * Mehr als vier Optionen nur als Raster über [columns]:
 * gleich breite Segmente in Reihen à [columns], gemeinsame Auswahl, gemeinsamer Rückfall auf die
 * Radioliste. Die Reihen stehen ohne eigene Lücke übereinander – der Abstand ist der Rest der
 * 48-dp-Tippfläche, enger ginge nur mit überlappenden Tippflächen (A11Y-05). Ein [selected], das
 * nicht in [items] liegt, markiert keines.
 *
 * [groupLabel] verknüpft die sichtbare Überschrift darüber („Lautstärke“) mit der Gruppe (A11Y-03):
 * als Name des Gruppen-Knotens. Der ist für TalkBack ein eigener Halt (Text ohne fokussierbaren
 * Vorfahren), deshalb trägt die sichtbare Überschrift [choiceGroupHeading] – sonst käme der Name
 * zweimal hintereinander.
 */
@Composable
fun <T> SegmentedChoiceRow(
    items: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = items.size,
    groupLabel: String? = null,
    label: @Composable (T) -> String,
) {
    val rows = items.chunked(columns)
    val group = if (groupLabel != null) Modifier.semantics { contentDescription = groupLabel } else Modifier
    SubcomposeLayout(modifier.fillMaxWidth().then(group)) { constraints ->
        // Jede Beschriftung einzeln als ausgewähltes Segment messen (fett = breiteste Variante).
        // Die Zeile misst mit width(IntrinsicSize.Min), also nur das längste Wort –
        // deshalb in der Probe untrennbar machen, sonst gilt „Start & Ende“ als so breit wie „Start“.
        val plain = subcompose("probe") {
            rows.forEach { row ->
                row.forEachIndexed { index, item ->
                    SingleChoiceSegmentedButtonRow {
                        Segment(label(item).unbreakable(), selected = true, onClick = {}, index = index, count = row.size)
                    }
                }
            }
        }.maxOf { it.maxIntrinsicWidth(Constraints.Infinity) }
        val slot = constraints.maxWidth / columns
        // Polster und Rand des Segments bleiben, nur die Schrift wird kleiner: gemeinsamer Faktor,
        // damit die breiteste Beschriftung gerade ins Segment passt (gleichrangig = gleich groß).
        // labelLarge ist die Rolle, die M3 in Segmenten setzt.
        val text = subcompose("text") {
            items.forEach {
                Text(label(it).unbreakable(), style = MaterialTheme.typography.labelLarge.copy(fontWeight = SELECTED_WEIGHT), maxLines = 1)
            }
        }.maxOf { it.maxIntrinsicWidth(Constraints.Infinity) }
        val fitScale = floor((slot - (plain - text)).toFloat() / text * SCALE_STEPS) / SCALE_STEPS
        val mode = when {
            plain <= slot -> ChoiceMode.SEGMENTS
            fitScale >= MIN_LABEL_SCALE -> ChoiceMode.SEGMENTS_SMALLER
            else -> ChoiceMode.RADIO_LIST
        }
        val labelScale = if (mode == ChoiceMode.SEGMENTS_SMALLER) fitScale else 1f

        val placeables = subcompose(mode to labelScale) {
            if (mode != ChoiceMode.RADIO_LIST) {
                // Je Reihe ein Placeable; layout() unten stapelt sie.
                rows.forEach { row ->
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        row.forEachIndexed { index, item ->
                            Segment(
                                text = label(item),
                                selected = item == selected,
                                labelScale = labelScale,
                                onClick = { onSelect(item) },
                                index = index,
                                count = row.size,
                            )
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
                            // Nur Anzeige, die Tippfläche trägt die Zeile.
                            RadioButton(selected = item == selected, onClick = null)
                            Spacer(Modifier.width(Dimens.GapSmall))
                            Text(
                                text = label(item),
                                style = MaterialTheme.typography.bodyMedium,
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

/**
 * M3 misst das Segment als Icon-Reserve + Beschriftung und setzt die Beschriftung ohne Icon um die
 * halbe Reserve nach links. Hier meldet sie sich um die Reserve schmaler (nie unter 0) und wird so
 * platziert, dass sie mittig in der gemeldeten Breite plus Reserve steht.
 */
private fun Modifier.withoutIconReserve(): Modifier = layout { measurable, constraints ->
    val reserve = Sizes.SegmentIconReserve.roundToPx()
    val placeable = measurable.measure(constraints.copy(minWidth = 0))
    val reported = (placeable.width - reserve).coerceAtLeast(0)
    layout(reported, placeable.height) {
        placeable.place((reported - placeable.width) / 2, 0)
    }
}

/**
 * Für die sichtbare Überschrift über einer [SegmentedChoiceRow] mit `groupLabel`: Sie ist nur Optik,
 * TalkBack liest denselben Text als Namen der Gruppe direkt darunter. Nicht `clearAndSetSemantics`,
 * damit das Text-Layout für die Umbruch- und Zuschnittprüfung im Screenshot-Test sichtbar bleibt.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.choiceGroupHeading(): Modifier = semantics { invisibleToUser() }

/** Darstellung je nach Platz, vom vollständigsten zum sparsamsten. */
private enum class ChoiceMode { SEGMENTS, SEGMENTS_SMALLER, RADIO_LIST }

/** Beschriftung des gewählten Segments: zweites Merkmal neben der Fläche, nicht nur Farbe (A11Y). */
private val SELECTED_WEIGHT = FontWeight.SemiBold // Abweichung TYPO-01: Auswahl-Merkmal neben der Fläche (FARBE-05)

/** Untergrenze der gemeinsamen Verkleinerung; darunter ist die Radioliste lesbarer. */
private const val MIN_LABEL_SCALE = 0.85f

/** Faktor in Hundertsteln abrunden, damit Rundung die Beschriftung nie über das Segment schiebt. */
private const val SCALE_STEPS = 100f

/** Leerzeichen und Bindestriche geschützt, damit die Mindestbreite die ganze Beschriftung ist. */
private fun String.unbreakable(): String = replace(' ', '\u00A0').replace('-', '\u2011')

/**
 * Ein Segment; Probe und Anzeige nutzen dasselbe, damit die Breitenmessung stimmt. Ohne Häkchen
 * gibt die Beschriftung den Platz zurück, den M3 (1.3) auch ohne Icon dafür reserviert: Sie meldet
 * sich um [Sizes.SegmentIconReserve] schmaler und zeichnet sich so versetzt, dass sie mittig im
 * Segment steht. Das Segment ist dann nur so breit wie Beschriftung und Polster.
 */
@Composable
private fun SingleChoiceSegmentedButtonRowScope.Segment(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    index: Int,
    count: Int,
    labelScale: Float = 1f,
) {
    SegmentedButton(
        selected = selected,
        onClick = onClick,
        shape = SegmentedButtonDefaults.itemShape(index, count),
        // Die Fläche allein trägt die Auswahl, sobald das Häkchen keinen Platz hat – deshalb
        // heller als secondaryContainer (WCAG 1.4.11), in jeder Stufe gleich.
        colors = SegmentedButtonDefaults.colors(activeContainerColor = SegmentSelected),
        modifier = Modifier.heightIn(min = Sizes.ButtonHeight),
        // Kein Häkchen (s. o.): Fläche und Schriftstärke tragen die Auswahl.
        icon = {},
    ) {
        val style = LocalTextStyle.current.let { if (selected) it.copy(fontWeight = SELECTED_WEIGHT) else it }
        Text(
            text = text,
            maxLines = 1,
            modifier = Modifier.withoutIconReserve(),
            style = if (labelScale < 1f) {
                style.copy(
                    fontSize = style.fontSize * labelScale, // Abweichung TYPO-01: verkleinert die M3-Rolle des Segments
                    lineHeight = style.lineHeight * labelScale, // Abweichung TYPO-01: verkleinert die M3-Rolle des Segments
                )
            } else {
                style
            },
        )
    }
}
