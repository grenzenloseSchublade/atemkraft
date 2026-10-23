package app.atemkraft.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import app.atemkraft.R
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.SituationRecommendation
import app.atemkraft.ui.components.AppTextButton
import app.atemkraft.ui.components.CollapsibleSection
import app.atemkraft.ui.components.ExerciseCard
import app.atemkraft.ui.components.SearchField
import app.atemkraft.ui.components.SecondaryText
import app.atemkraft.ui.components.headingColor
import app.atemkraft.ui.theme.Dimens

/**
 * Befindens-Suche im Atmen-Tab (MUSTER-09): Feld immer sichtbar, kein Fokus beim Öffnen des Tabs.
 * Das X erscheint erst mit einer Eingabe und leert sie; das Feld behält dabei den Fokus, wer neu
 * tippen will, tippt weiter. Den Text hält der Aufrufer nur in `rememberSaveable` (SEC-PRIV-03/04).
 */
@Composable
internal fun SituationSearchField(query: String, onQueryChange: (String) -> Unit) {
    SearchField(
        query = query,
        onQueryChange = onQueryChange,
        placeholder = stringResource(R.string.situations_search_placeholder),
        clearDescription = stringResource(R.string.cd_search_clear),
        onClear = if (query.isEmpty()) null else ({ onQueryChange("") }),
    )
}

/**
 * Ein Anlass als aufklappbare Zeile (Titel + Begründung); die Übungen stehen erst nach dem
 * Antippen da. rememberSaveable im Lazy-Item hält den Zustand über Scrollen, Tab-Wechsel und
 * Drehen. Köpfe reihum nach ihrer Lage unter den gezeigten Anlässen ([headIndex], FARBE-07),
 * auch für die Warn-Situation: Die Warnung trägt der Text.
 */
@Composable
internal fun SituationSection(
    rec: SituationRecommendation,
    headIndex: Int,
    initiallyExpanded: Boolean,
    resolve: (String) -> Exercise?,
    markedIds: Set<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    CollapsibleSection(
        title = rec.title,
        color = headingColor(headIndex),
        expanded = expanded,
        onExpandedChange = { expanded = it },
        teaser = rec.rationale,
        spacingAbove = Dimens.ListGap,
    ) {
        rec.exerciseIds.mapNotNull(resolve).forEach { exercise ->
            ExerciseCard(exercise = exercise, onClick = { onSelect(exercise.id) }, marked = exercise.id in markedIds)
        }
    }
}

/**
 * Trefferzahl unter dem Suchfeld, als Live-Region angesagt (A11Y-04). Ohne Treffer „Nichts
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
