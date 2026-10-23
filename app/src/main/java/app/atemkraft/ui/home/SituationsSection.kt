package app.atemkraft.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import app.atemkraft.R
import app.atemkraft.domain.Situation
import app.atemkraft.domain.SituationRecommendation
import app.atemkraft.ui.components.AppTextButton
import app.atemkraft.ui.components.ChoiceChip
import app.atemkraft.ui.components.ChoiceChipRow
import app.atemkraft.ui.components.SearchField
import app.atemkraft.ui.components.SecondaryText
import app.atemkraft.ui.components.SubLabel
import app.atemkraft.ui.theme.Dimens

/**
 * Block „Wie fühlst du dich?“ im Atmen-Tab (MUSTER-09): eine leicht abgesetzte Fläche ohne eigene
 * Überschrift (`surfaceContainerLow`, Rand `BorderThin` in `outlineVariant`, `shapes.medium`,
 * Polster `CardPadding`) mit dem Suchfeld und darunter den Anlässen als [ChoiceChip]s.
 *
 * Abstände: Feld → Chips `GapSmall` – das Feld hat unter seiner Grundlinie schon rund 12 dp
 * unsichtbaren Raum (Tippfläche 48 dp), dazu 4 dp Tippfläche über jedem Chip; `ListGap` risse
 * Feld und Chips sichtbar auseinander. Chips nebeneinander `GapSmall`, untereinander sichtbar
 * ebenfalls 8 dp ([ChoiceChipRow]).
 *
 * [highlighted]: markierte Chips (gewählter Anlass oder Suchtreffer); die übrigen bleiben
 * unverändert. Die Markierung ändert keine Größe – die Reihe bricht in jedem Zustand gleich um. Die intensive Situation steht zuletzt und trägt den Warnton.
 *
 * TalkBack: Der Block ist eine Gruppe (`isTraversalGroup`), die Chips eine Auswahlgruppe mit dem
 * Namen „Wie fühlst du dich?“.
 */
@Composable
internal fun FeelingBlock(
    query: String,
    onQueryChange: (String) -> Unit,
    situations: List<SituationRecommendation>,
    highlighted: Set<Situation>,
    onChipClick: (Situation) -> Unit,
) {
    val name = stringResource(R.string.situations_search_placeholder)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            SituationSearchField(query = query, onQueryChange = onQueryChange)
            Spacer(Modifier.height(Dimens.GapSmall))
            ChoiceChipRow(groupName = name) {
                // Stabil sortiert: Die intensive Situation rückt ans Ende, die übrigen behalten ihre Reihenfolge.
                situations.sortedBy { it.warn }.forEach { rec ->
                    ChoiceChip(
                        label = rec.shortLabel,
                        selected = rec.situation in highlighted,
                        onClick = { onChipClick(rec.situation) },
                        warn = rec.warn,
                    )
                }
            }
        }
    }
}

/**
 * Befindens-Suche (MUSTER-09): kein Fokus beim Öffnen des Tabs. Das X erscheint erst mit einer
 * Eingabe und leert sie; das Feld behält dabei den Fokus, wer neu tippen will, tippt weiter. Den
 * Text hält der Aufrufer nur in `rememberSaveable` (SEC-PRIV-03/04).
 */
@Composable
private fun SituationSearchField(query: String, onQueryChange: (String) -> Unit) {
    SearchField(
        query = query,
        onQueryChange = onQueryChange,
        placeholder = stringResource(R.string.situations_search_placeholder),
        clearDescription = stringResource(R.string.cd_search_clear),
        onClear = if (query.isEmpty()) null else ({ onQueryChange("") }),
    )
}

/**
 * Kopf einer Empfehlung unter dem Block: der Begründungssatz, bei der intensiven Situation samt
 * Vorbehalt („Nur üben, wenn …“). [withTitle]: Stehen mehrere Empfehlungen untereinander
 * (Suchtreffer), nennt ein [SubLabel] den Anlass; bei einer einzelnen trägt ihn der markierte Chip.
 * Darunter folgen die Übungskarten als eigene Listeneinträge.
 */
@Composable
internal fun RecommendationIntro(rec: SituationRecommendation, withTitle: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (withTitle) {
            SubLabel(rec.title, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(Dimens.GapTiny))
        }
        SecondaryText(rec.rationale)
    }
}

/**
 * Trefferzahl unter dem Block, als Live-Region angesagt (A11Y-04). Ohne Treffer „Nichts
 * gefunden“ mit „Alle zeigen“ (MUSTER-04) – keine geratene Empfehlung.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SearchStatus(count: Int, onShowAll: () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
        SecondaryText(
            text = if (count == 0) {
                stringResource(R.string.situations_search_none)
            } else {
                pluralStringResource(R.plurals.situations_search_count, count, count)
            },
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (count == 0) {
            AppTextButton(onClick = onShowAll) { Text(stringResource(R.string.action_show_all)) }
        }
    }
}
