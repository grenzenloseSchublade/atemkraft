package app.atemkraft.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.domain.BreathingFamily
import app.atemkraft.domain.DailyPattern
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.PatternTexts
import app.atemkraft.domain.SituationRecommendation
import app.atemkraft.domain.SituationSearch
import app.atemkraft.ui.components.AppIconButton
import app.atemkraft.ui.components.AppIconToggle
import app.atemkraft.ui.components.AppTextButton
import app.atemkraft.ui.components.ExerciseCard
import app.atemkraft.ui.components.ForwardLinkText
import app.atemkraft.ui.components.PatternCard
import app.atemkraft.ui.components.ScreenHeader
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.components.SettingsAction
import app.atemkraft.ui.components.TabListScaffold
import app.atemkraft.ui.components.color
import app.atemkraft.ui.components.flushStart
import app.atemkraft.ui.components.title
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY

/**
 * Atmen-Tab: alles, was man starten kann. Von oben: Muster des Tages, der Abschnitt „Anlass“ mit
 * der Befindens-Suche und den sechs Anlässen zum Stöbern (aufklappbar, Standard zu), dann die
 * Übungen nach Familien. Programme (Infoseiten ohne Start) stehen im Tab „Lernen“.
 *
 * Befindens-Suche (MUSTER-09): Das Feld steht immer da, ohne Fokus beim Öffnen. Sobald die
 * Eingabe ein Suchwort enthält ([SituationSearch]), stehen unter dem Feld nur die passenden
 * Anlässe, aufgeklappt und beste zuerst; die Anlässe zum Stöbern und die Familien treten solange
 * zurück. Die Eingabe lebt nur in `rememberSaveable` – nie in DataStore oder Log
 * (SEC-PRIV-03/04). System-Zurück leert zuerst die Suche und schließt die Tastatur.
 */
@Composable
fun HomeScreen(
    exercisesByFamily: List<Pair<BreathingFamily, List<Exercise>>>,
    situations: List<SituationRecommendation>,
    resolve: (String) -> Exercise?,
    daily: DailyPattern,
    dailySaved: Boolean,
    onRegenerateDaily: () -> Unit,
    onDailySavedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    onOpenSettings: () -> Unit,
    markedIds: Set<String> = emptySet(),
    // Einstieg in den Muster-Editor unter der Tageskarte; ohne ihn kein Link.
    onOpenPatternEditor: (() -> Unit)? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query, situations) { SituationSearch.search(query, situations) }
    val focusManager = LocalFocusManager.current
    val clearSearch = {
        query = ""
        focusManager.clearFocus()
    }
    BackHandler(enabled = query.isNotEmpty(), onBack = clearSearch)
    // Der Kopf steht fest über der Liste; liegt Inhalt darunter, zeigt er eine Haarlinie.
    TabListScaffold(
        header = { scrolled ->
            ScreenHeader(
                title = stringResource(R.string.tab_breathe),
                action = { SettingsAction(onClick = onOpenSettings) },
                scrolled = scrolled,
            )
        },
        subtitle = stringResource(R.string.home_subtitle),
    ) {
        item(key = "daily-header") {
            // „Neu generieren“ betrifft den ganzen Abschnitt („gib mir ein anderes“), nicht
            // die Karte – deshalb im Kopf, die Karte behält nur ihre eine Aktion (KOMP-02).
            SectionHeader(
                title = stringResource(R.string.home_daily_pattern),
                color = MaterialTheme.colorScheme.secondary,
            ) {
                AppIconButton(onClick = onRegenerateDaily) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.cd_pattern_regenerate),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
        item(key = "daily-pattern") {
            DailyPatternCard(
                daily = daily,
                saved = dailySaved,
                onClick = { onSelect(daily.exercise.id) },
                onSavedChange = onDailySavedChange,
            )
            // Eigener Weg zum Editor neben der Karte, nicht in ihr: Die Karte behält ihre
            // eine Aktion (KOMP-02). Bündig: Das Polster des Text-Buttons ragt in den Rand.
            if (onOpenPatternEditor != null) {
                AppTextButton(onClick = onOpenPatternEditor, modifier = Modifier.flushStart()) {
                    ForwardLinkText(
                        text = stringResource(R.string.home_own_pattern_link),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }

        // Einstieg „nach Befinden“: Kopf, Suchfeld und Anlässe bilden einen Abschnitt. Der Kopf
        // steht über dem Feld, damit es nicht zum Muster des Tages darüber zu gehören scheint.
        if (situations.isNotEmpty()) {
            item(key = "situations-header") {
                SectionHeader(title = stringResource(R.string.home_situations), color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY))
            }
            item(key = "situations-search") {
                SituationSearchField(query = query, onQueryChange = { query = it })
            }
        }
        if (results != null) {
            item(key = "search-status") {
                SearchStatus(count = results.size, onShowAll = clearSearch)
            }
            // Eigene Schlüssel: Treffer starten aufgeklappt, ohne den Zustand der Übersicht zu
            // überschreiben. Köpfe reihum nach ihrer Lage unter den Treffern (FARBE-07).
            itemsIndexed(results, key = { _, rec -> "q-${rec.situation.name}" }) { index, rec ->
                SituationSection(rec, index, initiallyExpanded = true, resolve = resolve, markedIds = markedIds, onSelect = onSelect)
            }
        } else {
            // Standard: alle Anlässe zu – der Tab bleibt kurz, die Familien folgen gleich.
            itemsIndexed(situations, key = { _, rec -> "s-${rec.situation.name}" }) { index, rec ->
                SituationSection(rec, index, initiallyExpanded = false, resolve = resolve, markedIds = markedIds, onSelect = onSelect)
            }

            exercisesByFamily.forEach { (family, exercises) ->
                item(key = "header-${family.name}") {
                    SectionHeader(title = family.title(), color = family.color())
                }
                items(exercises, key = { it.id }) { exercise ->
                    ExerciseCard(exercise = exercise, onClick = { onSelect(exercise.id) }, marked = exercise.id in markedIds)
                }
            }
        }
    }
}

/**
 * Karte für das generierte „Muster des Tages": [PatternCard] mit dem Lesezeichen als einziger
 * weiterer Aktion (Speichern ↔ wieder entfernen).
 */
@Composable
private fun DailyPatternCard(
    daily: DailyPattern,
    saved: Boolean,
    onClick: () -> Unit,
    onSavedChange: (Boolean) -> Unit,
) {
    PatternCard(exercise = daily.exercise, patternLine = PatternTexts.hintFor(daily.spec), onClick = onClick) {
        // Gespeicherte Muster erscheinen im Tab „Eigene“ unter „Meine Muster“ – der
        // Atmen-Tab bleibt schlank. Gefüllt im Akzent = gespeichert (Zustandsakzent, KOMP-02);
        // erneutes Tippen nimmt das Muster wieder heraus.
        AppIconToggle(
            checked = saved,
            onCheckedChange = onSavedChange,
            stateDescription = stringResource(if (saved) R.string.state_saved else R.string.state_not_saved),
        ) {
            Icon(
                painterResource(if (saved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark),
                contentDescription = stringResource(R.string.cd_pattern_save),
                tint = if (saved) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY)
                },
            )
        }
    }
}
