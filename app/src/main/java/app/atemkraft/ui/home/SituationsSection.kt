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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
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
import app.atemkraft.domain.BreathingArea
import app.atemkraft.domain.BreathingFamily
import app.atemkraft.domain.Situation
import app.atemkraft.domain.SituationRecommendation
import app.atemkraft.ui.components.AppIconButton
import app.atemkraft.ui.components.AppTextButton
import app.atemkraft.ui.components.ChoiceChip
import app.atemkraft.ui.components.ChoiceChipRow
import app.atemkraft.ui.components.ForwardLinkText
import app.atemkraft.ui.components.SearchField
import app.atemkraft.ui.components.SecondaryText
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.components.SubLabel
import app.atemkraft.ui.components.WarnText
import app.atemkraft.ui.components.color
import app.atemkraft.ui.components.flushStart
import app.atemkraft.ui.components.title
import app.atemkraft.ui.theme.Dimens

/**
 * Block „Wie fühlst du dich?“ im Atmen-Tab (MUSTER-09): eine leicht abgesetzte Fläche ohne eigene
 * Überschrift (`surfaceContainerLow`, Rand `BorderThin` in `outlineVariant`, `shapes.medium`,
 * Polster `CardPadding`) mit dem Suchfeld und darunter den Anlässen als [ChoiceChip]s.
 *
 * Abstände: Feld → Chips `GapSmall` – das Feld hat unter seiner Grundlinie schon rund 12 dp
 * unsichtbaren Raum (Tippfläche 48 dp), dazu 4 dp Tippfläche über jedem Chip; `ListGap` risse
 * Feld und Chips sichtbar auseinander. Chips nebeneinander `GapSmall`, untereinander sichtbar
 * ebenfalls 8 dp, höchstens zwei Zeilen ([ChoiceChipRow]); passen sie nicht, scrollen beide
 * Zeilen gemeinsam seitlich bis an den Kartenrand, mit Verlauf (Nutzer 2026-10-23).
 *
 * [highlighted]: markierte Chips (gewählter Anlass oder Suchtreffer); die übrigen bleiben
 * unverändert. Die Markierung ändert keine Größe – die Reihe bricht in jedem Zustand gleich um.
 * Alle sechs Chips sehen gleich aus und stehen in der Reihenfolge der Daten; die Warnung der
 * intensiven Situation steht erst in ihrer Empfehlung ([RecommendationIntro]).
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
            // Scrollen die Chips, reicht ihre Fläche bis an den Kartenrand (Polster rechts aufgehoben).
            ChoiceChipRow(groupName = name, items = situations, endBleed = Dimens.CardPadding) { rec, order ->
                ChoiceChip(
                    label = rec.shortLabel,
                    selected = rec.situation in highlighted,
                    onClick = { onChipClick(rec.situation) },
                    modifier = order,
                )
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
 * Kopf einer Empfehlung unter dem Block: der Begründungssatz, darunter ihre ⚠-Zeile ([WarnText],
 * FARBE-05), wenn sie eine hat (intensive Situation, Notfall-Satz bei „Kurzatmig“) – die Chips
 * selbst sind neutral. [withTitle]:
 * Stehen mehrere Ergebnisse untereinander (Suchtreffer), nennt ein [SubLabel] den Anlass; bei
 * einem einzelnen trägt ihn der markierte Chip. Darunter folgen die Übungskarten als eigene
 * Listeneinträge und zuletzt der Verweis auf den Bereich ([AreaLink]).
 */
@Composable
internal fun RecommendationIntro(rec: SituationRecommendation, withTitle: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (withTitle) {
            SubLabel(rec.title, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(Dimens.GapTiny))
        }
        SecondaryText(rec.rationale)
        rec.warning?.let { warning ->
            Spacer(Modifier.height(Dimens.GapTiny))
            WarnText(warning)
        }
    }
}

/**
 * Verweis „Mehr aus A · Herunterregeln ›“ unter einer Empfehlung (MUSTER-09): führt an derselben
 * Stelle zu allen Übungen ihres Bereichs. In der Farbe der Familie statt in Cyan – die Farbe sagt
 * hier, wohin es geht, wie der Familienkopf, zu dem der Verweis führt (Ausnahme in FARBE-04).
 * Bündig: Das Polster des Text-Buttons ragt in den Rand.
 */
@Composable
internal fun AreaLink(family: BreathingFamily, onClick: () -> Unit) {
    AppTextButton(onClick = onClick, modifier = Modifier.flushStart()) {
        ForwardLinkText(text = stringResource(R.string.situations_area_link, family.title()), color = family.color())
    }
}

/**
 * Kopf eines Bereichs unter dem Block: der Familienkopf wie in der Liste, bei intensiven Übungen
 * mit der ⚠-Zeile darunter. [onClose] (nur wenn der Bereich über [AreaLink] gewählt ist): ✕ im
 * Kopf führt zurück zur Übersicht, wie System-Zurück – kein Chip ist dann markiert, über den man
 * zurückkäme.
 */
@Composable
internal fun AreaHeader(area: BreathingArea, onClose: (() -> Unit)?) {
    val family = area.family
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = family.title(),
            color = family.color(),
            trailingAction = onClose?.let { close ->
                {
                    AppIconButton(onClick = close) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_show_all), tint = family.color())
                    }
                }
            },
        )
        area.warning?.let { warning ->
            Spacer(Modifier.height(Dimens.GapTiny))
            WarnText(warning)
        }
    }
}

/**
 * Trefferstatus unter dem Block, als Live-Region angesagt (A11Y-04): „2 Anlässe“, „1 Anlass,
 * 1 Bereich“, „1 Bereich“ – kurz genug, dass er nie umbricht. Ohne Treffer „Nichts gefunden“ mit
 * „Alle zeigen“ (MUSTER-04) – keine geratene Empfehlung.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SearchStatus(situations: Int, areas: Int, onShowAll: () -> Unit) {
    val none = situations == 0 && areas == 0
    val parts = listOfNotNull(
        if (situations > 0) pluralStringResource(R.plurals.situations_search_situations, situations, situations) else null,
        if (areas > 0) pluralStringResource(R.plurals.situations_search_areas, areas, areas) else null,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
        SecondaryText(
            text = if (none) stringResource(R.string.situations_search_none) else parts.joinToString(", "),
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (none) {
            AppTextButton(onClick = onShowAll) { Text(stringResource(R.string.action_show_all)) }
        }
    }
}
