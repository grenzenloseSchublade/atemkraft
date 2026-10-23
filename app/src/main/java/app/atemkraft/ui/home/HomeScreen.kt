package app.atemkraft.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import app.atemkraft.domain.BreathingArea
import app.atemkraft.domain.BreathingFamily
import app.atemkraft.domain.DailyPattern
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.ExerciseFilter
import app.atemkraft.domain.ExerciseFilters
import app.atemkraft.domain.PatternTexts
import app.atemkraft.domain.Situation
import app.atemkraft.domain.SituationRecommendation
import app.atemkraft.domain.SituationSearch
import app.atemkraft.domain.area
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
import app.atemkraft.ui.components.headingColor
import app.atemkraft.ui.components.title
import app.atemkraft.ui.theme.SECONDARY

/**
 * Was der Atmen-Tab zeigt und gleich bleibt, solange die App läuft: die angeleiteten Übungen nach
 * Familie, Anlässe, Bereiche, die Übungen der Seite „Pranayama“ (Filter „Aus dem Yoga“) und das
 * Nachschlagen einer Übung per Id ([resolve]).
 */
@Immutable
class HomeCatalog(
    val exercisesByFamily: List<Pair<BreathingFamily, List<Exercise>>>,
    val situations: List<SituationRecommendation>,
    val areas: List<BreathingArea>,
    val yogaExerciseIds: Set<String>,
    val resolve: (String) -> Exercise?,
)

/** Was der Atmen-Tab auslöst (CODE-02). */
@Immutable
data class HomeActions(
    val onRegenerateDaily: () -> Unit,
    val onDailySavedChange: (Boolean) -> Unit,
    val onSelect: (String) -> Unit,
    val onOpenSettings: () -> Unit,
    val onDismissWelcome: () -> Unit = {},
)

/**
 * Atmen-Tab: alles, was man starten kann. Von oben: beim ersten Start die Willkommenskarte
 * ([WelcomeCard], abweisbar), der Block „Wie fühlst du dich?“ (Suchfeld und die sechs Anlässe als
 * Chips, [FeelingBlock]), das Muster des Tages, dann „Alle Übungen“ mit der Filterreihe und
 * darunter die Übungen nach Familien mit ihren farbigen Köpfen.
 * Programme (Infoseiten ohne Start) stehen im Tab „Lernen“.
 *
 * Reihenfolge (Nutzer 2026-10-24): Der Einstieg nach Befinden steht zuerst, das Muster des Tages
 * – bewusst spielerisch, ohne eigenes Wirkversprechen – rutscht darunter und bleibt auf dem ersten
 * Bildschirm sichtbar. Den Weg in den Muster-Editor gibt es im Tab „Eigene“ („+“) und über den
 * Stift auf der Detailseite des Tagesmusters; der Atmen-Tab trägt keinen eigenen Link mehr.
 *
 * Ein Zustand (MUSTER-09): Gewählt ist entweder ein Anlass, eine Suche oder ein Bereich – oder
 * nichts, dann steht alles da wie immer (Block, Muster des Tages, vier Familien).
 * - **Anlass:** Ein Tipp auf einen Chip markiert ihn; unter dem Block stehen sein Begründungssatz
 *   (bei der intensiven Situation mit ⚠-Zeile), seine Übungen und „Mehr: <Bereich> ›“
 *   ([AreaLink]); Muster des Tages und Familien treten zurück. Ein zweiter Tipp auf denselben Chip
 *   oder System-Zurück zeigt wieder alles.
 * - **Bereich:** „Mehr: …“ zeigt an derselben Stelle den Familienkopf und alle Übungen des
 *   Bereichs ([AreaHeader]); kein Chip ist markiert, die Eingabe leer. ✕ im Kopf oder Zurück
 *   führt zur Übersicht.
 * - **Suche:** Sobald die Eingabe ein Suchwort enthält ([SituationSearch]), sind die Chips der
 *   Treffer markiert, darunter stehen erst die Empfehlungen der Anlässe, beste zuerst, dann die
 *   direkt getroffenen Bereiche. Ein Chip leert die Eingabe, eine Eingabe hebt die Wahl auf.
 *
 * Im Grundzustand steht über den Familien der Kopf „Alle Übungen“ mit der Filterreihe
 * ([ExerciseFilterRow], MUSTER-12, Nutzer 2026-10-23): Filter quer zu den Familien, UND-verknüpft;
 * die Liste bleibt nach Familien gruppiert, Familien ohne Treffer fallen samt Kopf weg. Die Wahl
 * lebt nur in `rememberSaveable` und bleibt, während ein Anlass, eine Suche oder ein Bereich
 * gewählt ist – die Reihe ist dann ausgeblendet.
 *
 * Der Bereich eines Anlasses ist die häufigste Familie seiner Übungen ([area]). Das Feld bekommt
 * beim Öffnen keinen Fokus; die Eingabe lebt nur in `rememberSaveable` – nie in DataStore oder
 * Log (SEC-PRIV-03/04). System-Zurück leert zuerst Wahl und Suche und schließt die Tastatur.
 *
 * [welcome]: die Übung der Willkommenskarte; null = Karte weggeklickt (oder Übung fehlt).
 */
@Composable
fun HomeScreen(
    catalog: HomeCatalog,
    daily: DailyPattern,
    dailySaved: Boolean,
    actions: HomeActions,
    markedIds: Set<String> = emptySet(),
    welcome: Exercise? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    // Höchstens eins von beiden ist gesetzt, und nur bei leerer Eingabe (siehe KDoc).
    var chosen by rememberSaveable { mutableStateOf<Situation?>(null) }
    var chosenArea by rememberSaveable { mutableStateOf<BreathingFamily?>(null) }
    var filters by rememberSaveable { mutableStateOf(emptySet<ExerciseFilter>()) }
    val results = remember(query, catalog.situations, catalog.areas) { SituationSearch.search(query, catalog.situations, catalog.areas) }
    val focusManager = LocalFocusManager.current
    val showAll = {
        query = ""
        chosen = null
        chosenArea = null
        focusManager.clearFocus()
    }
    BackHandler(enabled = query.isNotEmpty() || chosen != null || chosenArea != null, onBack = showAll)
    // Was unter dem Block steht: der gewählte Anlass, der gewählte Bereich, die Suchtreffer oder
    // nichts (dann die Familien).
    val shownSituations: List<SituationRecommendation> = when {
        chosen != null -> catalog.situations.filter { it.situation == chosen }
        chosenArea != null -> emptyList()
        else -> results?.situations.orEmpty()
    }
    val shownAreas: List<BreathingArea> = when {
        chosenArea != null -> catalog.areas.filter { it.family == chosenArea }
        chosen != null -> emptyList()
        else -> results?.areas.orEmpty()
    }
    val filtering = chosen != null || chosenArea != null || results != null
    val highlighted = shownSituations.map { it.situation }.toSet()
    val familyExercises = catalog.exercisesByFamily.toMap()
    val filtered = remember(catalog.exercisesByFamily, filters, catalog.yogaExerciseIds) { ExerciseFilters.apply(catalog.exercisesByFamily, filters, catalog.yogaExerciseIds) }
    // Der Kopf steht fest über der Liste; liegt Inhalt darunter, zeigt er eine Haarlinie.
    TabListScaffold(
        header = { scrolled ->
            ScreenHeader(
                title = stringResource(R.string.tab_breathe),
                action = { SettingsAction(onClick = actions.onOpenSettings) },
                scrolled = scrolled,
            )
        },
        subtitle = stringResource(R.string.home_subtitle),
    ) {
        // Erster Start: eine ruhige, abweisbare Karte statt eines Onboardings (Nutzer 2026-10-24).
        if (welcome != null) {
            item(key = "welcome") {
                WelcomeCard(exercise = welcome, onOpen = { actions.onSelect(welcome.id) }, onDismiss = actions.onDismissWelcome)
            }
        }
        // Einstieg „nach Befinden“ ohne eigene Überschrift: Feld und Anlässe in einer Fläche.
        if (catalog.situations.isNotEmpty()) {
            item(key = "feeling") {
                FeelingBlock(
                    query = query,
                    onQueryChange = { text ->
                        query = text
                        if (text.isNotEmpty()) {
                            chosen = null
                            chosenArea = null
                        }
                    },
                    situations = catalog.situations,
                    highlighted = highlighted,
                    onChipClick = { situation ->
                        // Zweiter Tipp auf denselben Chip zeigt wieder alles; ein Chip leert die Eingabe.
                        chosen = if (chosen == situation) null else situation
                        chosenArea = null
                        query = ""
                        focusManager.clearFocus()
                    },
                )
            }
        }
        if (results != null && chosen == null && chosenArea == null) {
            item(key = "search-status") {
                SearchStatus(situations = results.situations.size, areas = results.areas.size, onShowAll = showAll)
            }
        }
        if (filtering) {
            val withTitle = shownSituations.size + shownAreas.size > 1
            val directAreas = shownAreas.map { it.family }.toSet()
            shownSituations.forEach { rec ->
                item(key = "rec-${rec.situation.name}") { RecommendationIntro(rec, withTitle) }
                items(rec.exerciseIds.mapNotNull(catalog.resolve), key = { "rec-${rec.situation.name}-${it.id}" }) { exercise ->
                    ExerciseCard(exercise = exercise, onClick = { actions.onSelect(exercise.id) }, marked = exercise.id in markedIds)
                }
                // Steht der Bereich ohnehin als Treffer darunter, braucht es keinen Verweis darauf.
                val area = rec.area(catalog.resolve)
                if (area != null && area !in directAreas) {
                    item(key = "rec-${rec.situation.name}-area") {
                        AreaLink(family = area) {
                            chosen = null
                            chosenArea = area
                            query = ""
                            focusManager.clearFocus()
                        }
                    }
                }
            }
            shownAreas.forEach { area ->
                item(key = "area-${area.family.name}") {
                    AreaHeader(area = area, onClose = if (chosenArea != null) showAll else null)
                }
                items(familyExercises[area.family].orEmpty(), key = { "area-${area.family.name}-${it.id}" }) { exercise ->
                    ExerciseCard(exercise = exercise, onClick = { actions.onSelect(exercise.id) }, marked = exercise.id in markedIds)
                }
            }
        } else {
            item(key = "daily-header") {
                // „Neu generieren“ betrifft den ganzen Abschnitt („gib mir ein anderes“), nicht
                // die Karte – deshalb im Kopf, die Karte behält nur ihre eine Aktion (KOMP-02).
                SectionHeader(
                    title = stringResource(R.string.home_daily_pattern),
                    color = MaterialTheme.colorScheme.secondary,
                ) {
                    AppIconButton(onClick = actions.onRegenerateDaily) {
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
                    onClick = { actions.onSelect(daily.exercise.id) },
                    onSavedChange = actions.onDailySavedChange,
                )
            }
            item(key = "all-header") {
                // Kein Familienkopf: erster Ton des Kopf-Rings (FARBE-07, Ausnahme Atmen-Tab).
                SectionHeader(title = stringResource(R.string.home_all_exercises), color = headingColor(0))
            }
            item(key = "filters") {
                ExerciseFilterRow(active = filters, onToggle = { filters = if (it in filters) filters - it else filters + it })
            }
            if (filters.isNotEmpty()) {
                item(key = "filter-status") {
                    FilterStatus(count = filtered.sumOf { it.second.size }, onReset = { filters = emptySet() })
                }
            }
            filtered.forEach { (family, exercises) ->
                item(key = "header-${family.name}") {
                    SectionHeader(title = family.title(), color = family.color())
                }
                items(exercises, key = { it.id }) { exercise ->
                    ExerciseCard(exercise = exercise, onClick = { actions.onSelect(exercise.id) }, marked = exercise.id in markedIds)
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
        // Gespeicherte Muster erscheinen im Tab „Eigene“ unter „Eigene Kreationen“ – der
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
