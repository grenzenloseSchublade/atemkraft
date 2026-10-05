package app.atemkraft.ui.situations

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.data.SavedPattern
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.SituationRecommendation
import app.atemkraft.domain.SituationSearch
import app.atemkraft.domain.defaultMinutes
import app.atemkraft.ui.components.AppIconButton
import app.atemkraft.ui.components.AppTextButton
import app.atemkraft.ui.components.ScreenHeader
import app.atemkraft.ui.components.SearchField
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.home.ExerciseCard
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.NeonCyan
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.WarnAmber

/**
 * Situationen-Tab: „welche Atmung wann“ als Befindens-Übersicht. Gleiche Optik wie der Atmen-Tab
 * (Abschnitts-Header + Übungs-Karten), nur nach Lage gruppiert – mit Begründungssatz als
 * Entscheidungshilfe. Die Situationen sind aufklappbar und starten zu; „Meine Muster“ steht
 * offen darüber, weil man sie selbst angelegt hat und direkt wiederfinden will.
 *
 * Befindens-Suche (MUSTER-09): Die Lupe im Kopf ersetzt den Untertitel durch ein Suchfeld.
 * Sobald die Eingabe ein Suchwort enthält ([SituationSearch]), stehen nur die passenden
 * Situationen da, aufgeklappt und beste zuerst; „Meine Muster“ tritt zurück, weil die Suche
 * nur über das Befinden läuft. Die Eingabe lebt nur in `rememberSaveable` – nie in DataStore
 * oder Log (SEC-PRIV-03/04). System-Zurück schließt zuerst die Suche.
 */
@Composable
fun SituationsScreen(
    recommendations: List<SituationRecommendation>,
    savedPatterns: List<SavedPattern>,
    resolve: (String) -> Exercise?,
    onSelect: (String) -> Unit,
    onDeleteSaved: (Long) -> Unit,
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ListGap),
        ) {
            item {
                ScreenHeader(
                    title = stringResource(R.string.tab_situations),
                    subtitle = stringResource(R.string.situations_subtitle),
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
                    subtitleContent = if (searchOpen) {
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
            }

            if (results != null) {
                item(key = "search-status") {
                    SearchStatus(count = results.size, onShowAll = closeSearch)
                }
                // Eigene Schlüssel: Treffer starten aufgeklappt, ohne den Zustand der
                // Übersicht zu überschreiben.
                items(results, key = { "q-${it.situation.name}" }) { rec ->
                    SituationSection(rec, initiallyExpanded = true, resolve = resolve, onSelect = onSelect)
                }
            } else {
                // Vom Nutzer gespeicherte generierte Muster – bewusst hier (nicht im Atmen-Tab).
                if (savedPatterns.isNotEmpty()) {
                    item(key = "my-patterns-header") {
                        SectionHeader(
                            title = stringResource(R.string.situations_my_patterns),
                            color = NeonCyan,
                        )
                    }
                    items(savedPatterns, key = { "saved-" + it.id }) { pattern ->
                        SavedPatternCard(
                            pattern = pattern,
                            onClick = { onSelect(pattern.exercise.id) },
                            onDelete = { onDeleteSaved(pattern.id) },
                        )
                    }
                }

                // Standard: alle zu – so liest sich der Tab als kurze Befindens-Übersicht statt
                // als langes Scrollen.
                items(recommendations, key = { "s-${it.situation.name}" }) { rec ->
                    SituationSection(rec, initiallyExpanded = false, resolve = resolve, onSelect = onSelect)
                }
            }

            item { Spacer(Modifier.height(Dimens.ScreenBottom)) }
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
    initiallyExpanded: Boolean,
    resolve: (String) -> Exercise?,
    onSelect: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val accent = if (rec.warn) WarnAmber else MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier.animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(Dimens.ListGap),
    ) {
        SectionHeader(
            title = rec.title,
            color = accent,
            teaser = rec.rationale,
            expanded = expanded,
            onExpandedChange = { expanded = it },
        )
        if (expanded) {
            rec.exerciseIds.mapNotNull(resolve).forEach { exercise ->
                ExerciseCard(exercise = exercise, onClick = { onSelect(exercise.id) })
            }
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
            style = MaterialTheme.typography.bodyMedium,
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

/** Karte eines gespeicherten Musters: Name, Muster-Zeile, Löschen. */
@Composable
private fun SavedPatternCard(
    pattern: SavedPattern,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val exercise = pattern.exercise
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                WholeWordText(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Dimens.GapTiny))
                Text(
                    text = exercise.instructionHint.orEmpty() + RandomPatternGenerator.HINT_SEPARATOR +
                        stringResource(R.string.duration_approx, exercise.defaultMinutes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                )
            }
            AppIconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.cd_pattern_delete),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                )
            }
        }
    }
}
