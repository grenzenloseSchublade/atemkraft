package app.atemkraft.ui.situations

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import app.atemkraft.R
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.SituationRecommendation
import app.atemkraft.domain.SituationSearch
import app.atemkraft.ui.components.AppIconButton
import app.atemkraft.ui.components.AppTextButton
import app.atemkraft.ui.components.CollapsibleSection
import app.atemkraft.ui.components.ExerciseCard
import app.atemkraft.ui.components.ScreenHeader
import app.atemkraft.ui.components.ScreenSubtitle
import app.atemkraft.ui.components.SearchField
import app.atemkraft.ui.components.headingColor
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY

/**
 * Situationen-Tab: „welche Atmung wann“ als Befindens-Übersicht. Gleiche Optik wie der Atmen-Tab
 * (Abschnitts-Header + Übungs-Karten), nur nach Lage gruppiert – mit Begründungssatz als
 * Entscheidungshilfe. Die Situationen sind aufklappbar und starten zu. „Meine Muster“ stehen
 * im eigenen Tab „Eigene“ (`ui/own/OwnPatternsScreen`).
 *
 * Befindens-Suche (MUSTER-09): Die Lupe im Kopf ersetzt den Untertitel durch ein Suchfeld.
 * Sobald die Eingabe ein Suchwort enthält ([SituationSearch]), stehen nur die passenden
 * Situationen da, aufgeklappt und beste zuerst. Die Eingabe lebt nur in `rememberSaveable` –
 * nie in DataStore oder Log (SEC-PRIV-03/04). System-Zurück schließt zuerst die Suche.
 */
@Composable
fun SituationsScreen(
    recommendations: List<SituationRecommendation>,
    resolve: (String) -> Exercise?,
    onSelect: (String) -> Unit,
    markedIds: Set<String> = emptySet(),
) {
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    // Fokus nur nach dem Tipp auf die Lupe – nicht nach Drehen oder Zurückscrollen.
    var focusPending by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val results = remember(query, recommendations) { SituationSearch.search(query, recommendations) }
    val closeSearch = {
        query = ""
        searchOpen = false
    }
    BackHandler(enabled = searchOpen, onBack = closeSearch)
    // Der Kopf steht fest über der Liste; liegt Inhalt darunter, zeigt er eine Haarlinie.
    val listState = rememberLazyListState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = stringResource(R.string.tab_situations),
                action = if (searchOpen) {
                    null
                } else {
                    {
                        AppIconButton(onClick = {
                            searchOpen = true
                            focusPending = true
                        }) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = stringResource(R.string.cd_search_open),
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                },
                scrolled = listState.canScrollBackward,
                // Das Suchfeld steht fest unter der Zeile: Getippt wird oben, die Treffer scrollen.
                below = if (searchOpen) {
                    {
                        SearchField(
                            query = query,
                            onQueryChange = { query = it },
                            placeholder = stringResource(R.string.situations_search_placeholder),
                            // Erst leeren, dann schließen: Wer neu tippen will, behält das Feld.
                            clearDescription = stringResource(
                                if (query.isEmpty()) R.string.cd_search_close else R.string.cd_search_clear,
                            ),
                            onClear = { if (query.isEmpty()) closeSearch() else query = "" },
                            focusRequester = focus,
                        )
                        LaunchedEffect(focusPending) {
                            if (focusPending) {
                                focus.requestFocus()
                                focusPending = false
                            }
                        }
                    }
                } else {
                    null
                },
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                // Rand als contentPadding, nicht als Modifier: Die Liste reicht bis an den Bildrand, und
                // bündige Icon-Buttons (flushEnd) ragen mit ihrer Tippfläche in den Rand, ohne dass der
                // Listen-Clip sie anschneidet.
                contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.ListGap),
            ) {
                // Bei offener Suche ersetzt das Suchfeld unter dem Kopf den Untertitel.
                if (!searchOpen) {
                    item(key = "subtitle") { ScreenSubtitle(stringResource(R.string.situations_subtitle)) }
                }

                if (results != null) {
                    item(key = "search-status") {
                        SearchStatus(count = results.size, onShowAll = closeSearch)
                    }
                    // Eigene Schlüssel: Treffer starten aufgeklappt, ohne den Zustand der
                    // Übersicht zu überschreiben.
                    // Köpfe reihum nach ihrer Lage unter den gezeigten Abschnitten (FARBE-07).
                    itemsIndexed(results, key = { _, rec -> "q-${rec.situation.name}" }) { index, rec ->
                        SituationSection(rec, index, initiallyExpanded = true, resolve = resolve, markedIds = markedIds, onSelect = onSelect)
                    }
                } else {
                    // Standard: alle zu – so liest sich der Tab als kurze Befindens-Übersicht statt
                    // als langes Scrollen.
                    // Köpfe reihum nach ihrer Lage (FARBE-07).
                    itemsIndexed(recommendations, key = { _, rec -> "s-${rec.situation.name}" }) { index, rec ->
                        SituationSection(
                            rec,
                            index,
                            initiallyExpanded = false,
                            resolve = resolve,
                            markedIds = markedIds,
                            onSelect = onSelect,
                        )
                    }
                }

                item { Spacer(Modifier.height(Dimens.ScreenBottom)) }
            }
        }
    }
}

/**
 * Eine Situation als aufklappbare Zeile (Titel + Begründung); die Übungen stehen erst nach dem
 * Antippen da. rememberSaveable im Lazy-Item hält den Zustand über Scrollen, Tab-Wechsel und
 * Drehen.
 */
@Composable
private fun SituationSection(
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
        // Reihum ohne Bedeutung, auch für die Warn-Situation: Die Warnung trägt der Text (FARBE-07).
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
private fun SearchStatus(count: Int, onShowAll: () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
        Text(
            text = if (count == 0) {
                stringResource(R.string.situations_search_none)
            } else {
                pluralStringResource(R.plurals.situations_search_count, count, count)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (count == 0) {
            AppTextButton(onClick = onShowAll) { Text(stringResource(R.string.action_show_all)) }
        }
    }
}
