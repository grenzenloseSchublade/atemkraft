package app.atemkraft.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SegmentSelected
import app.atemkraft.ui.theme.Sizes
import app.atemkraft.ui.theme.TERTIARY
import app.atemkraft.ui.theme.WarnAmber

/** Test-Tag der Reihe aus [ChoiceChip]s: Der ScreenshotTest prüft, dass kein Chip beim Markieren oder Abblenden verrutscht. */
const val CHOICE_CHIPS_TAG = "choice-chips"

/**
 * Auswählbarer Chip (Anlässe im Tab „Atmen“): sichtbar [Sizes.ButtonHeight] hoch wie die Segmente
 * einer `SegmentedChoiceRow` (auch eine Auswahl), Tippfläche 48 dp; Form `shapes.small`,
 * Beschriftung `labelLarge` einzeilig über [WholeWordText] (verkleinert statt im Wort zu brechen).
 *
 * **Misst in jedem Zustand gleich** (Nutzer 2026-10-23: Chips verrutschen nie). Die Auswahl zeigen
 * nur Fläche [SegmentSelected] (≥ 3:1 gegen den Grund) und Randfarbe (Akzent statt `outline`,
 * FARBE-05), [dimmed] nur die Deckkraft ([TERTIARY]) – alles gezeichnet, nichts davon geht ins
 * Layout. Keine fette Schrift, kein Häkchen, kein ✕: Sie änderten die Breite und damit den
 * Umbruch der Reihe. Die Größe hängt so nur an Beschriftung, Schrift und verfügbarer Breite.
 *
 * [warn]: die intensive Situation – Beschriftung und Rand in `WarnAmber` wie der Charakter-Chip
 * „intensiv“; gewählt steht die Beschriftung in `onSurface` (`WarnAmber` hielte auf der
 * Auswahlfläche keine 4,5:1), der gelbe Rand bleibt.
 *
 * TalkBack: Rolle Optionsfeld, „ausgewählt“/„nicht ausgewählt“; die Reihe ist eine
 * [ChoiceChipRow] (`selectableGroup`).
 */
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
    warn: Boolean = false,
) {
    val shape = MaterialTheme.shapes.small
    val accent = if (warn) WarnAmber else MaterialTheme.colorScheme.primary
    val border = if (selected || warn) accent else MaterialTheme.colorScheme.outline
    val text = if (warn && !selected) WarnAmber else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            // Reihenfolge wie AppIconButton (LAYOUT-06): erst die 48-dp-Tippfläche, dann die sichtbare Pille.
            .minimumInteractiveComponentSize()
            .heightIn(min = Sizes.ButtonHeight)
            .alpha(if (dimmed) TERTIARY else 1f)
            .clip(shape)
            .background(if (selected) SegmentSelected else Color.Transparent)
            .border(Dimens.BorderChip, border, shape)
            .selectable(selected = selected, role = Role.RadioButton, interactionSource = null, indication = ripple(), onClick = onClick)
            // Polster wie ein M3-Chip (8 + 8); fest, damit die Rundung keine Glyphen anschneidet (LAYOUT-03).
            .padding(horizontal = Dimens.CardPadding),
        contentAlignment = Alignment.Center,
    ) {
        WholeWordText(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = text,
            singleLine = true,
            modifier = Modifier.testTag(CHIP_TEXT_TAG),
        )
    }
}

/**
 * Reihe aus [ChoiceChip]s: `FlowRow`, nebeneinander `GapSmall`; untereinander kein Layout-Abstand –
 * die 48-dp-Tippflächen der 40 dp hohen Chips lassen sichtbar je 4 dp über und unter jedem Chip,
 * also ebenfalls 8 dp zwischen zwei Reihen. Bei großer Schrift bricht die Reihe zwischen den
 * Chips um, nie in einem. [groupName] benennt die Gruppe für TalkBack (`selectableGroup`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChoiceChipRow(groupName: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FlowRow(
        modifier = modifier
            .testTag(CHOICE_CHIPS_TAG)
            .selectableGroup()
            .semantics { contentDescription = groupName },
        horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
    ) { content() }
}

@Preview
@Composable
private fun ChoiceChipPreview() {
    AtemkraftTheme {
        ChoiceChipRow(groupName = "Anlass") {
            ChoiceChip(label = "Einschlafen", selected = true, onClick = {})
            ChoiceChip(label = "Fokus", selected = false, onClick = {}, dimmed = true)
            ChoiceChip(label = "Wach & energiegeladen", selected = false, onClick = {}, warn = true)
        }
    }
}
