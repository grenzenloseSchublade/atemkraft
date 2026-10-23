package app.atemkraft.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.CHIP_FILL
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SegmentSelected
import app.atemkraft.ui.theme.Sizes
import app.atemkraft.ui.theme.WarnAmber

/** Test-Tag der Reihe aus [ChoiceChip]s: Der ScreenshotTest prüft, dass kein Chip beim Markieren oder Abblenden verrutscht. */
const val CHOICE_CHIPS_TAG = "choice-chips"

/**
 * Auswählbarer Chip (Anlässe im Tab „Atmen“): sichtbar [Sizes.ButtonHeight] hoch wie die Segmente
 * einer `SegmentedChoiceRow` (auch eine Auswahl), Tippfläche 48 dp; Form `shapes.small`,
 * Beschriftung `labelLarge` einzeilig über [WholeWordText] (verkleinert statt im Wort zu brechen).
 *
 * **Misst in jedem Zustand gleich** (Nutzer 2026-10-23: Chips verrutschen nie). Gewählt füllt sich
 * der Chip ganz mit [SegmentSelected], Rand in derselben Farbe – wie ein gewähltes Segment, ein
 * ruhiger Farbton statt Lila mit pinkem Rand (Nutzer 2026-10-23: „das Highlighting ist nicht so
 * schön“). Fläche und Rand gleiten über (Default-Spring, MOTION-04). Alles gezeichnet, nichts
 * davon geht ins Layout. Keine fette Schrift, kein Häkchen, kein ✕: Sie änderten die Breite und
 * damit den Umbruch der Reihe. Nicht gewählte Chips bleiben in der Suche unverändert, nicht
 * abgeblendet: Die gefüllten Treffer heben sich genug ab.
 *
 * [warn]: die intensive Situation – Beschriftung und Rand in `WarnAmber` wie der Charakter-Chip
 * „intensiv“; gewählt füllt sie sich mit `WarnAmber` in [CHIP_FILL], Rand bleibt gelb, die
 * Beschriftung steht in `onSurface` (gut lesbar auf der zarten Fläche).
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
    warn: Boolean = false,
) {
    val shape = MaterialTheme.shapes.small
    val selectedFill = if (warn) WarnAmber.copy(alpha = CHIP_FILL) else SegmentSelected
    val fill by animateColorAsState(if (selected) selectedFill else Color.Transparent, label = "chipFill")
    val border by animateColorAsState(
        when {
            warn -> WarnAmber
            selected -> SegmentSelected
            else -> MaterialTheme.colorScheme.outline
        },
        label = "chipBorder",
    )
    val text = if (warn && !selected) WarnAmber else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            // Reihenfolge wie AppIconButton (LAYOUT-06): erst die 48-dp-Tippfläche, dann die sichtbare Pille.
            .minimumInteractiveComponentSize()
            .heightIn(min = Sizes.ButtonHeight)
            .clip(shape)
            .background(fill)
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
            ChoiceChip(label = "Fokus", selected = false, onClick = {})
            ChoiceChip(label = "Wach & energiegeladen", selected = false, onClick = {}, warn = true)
        }
    }
}
