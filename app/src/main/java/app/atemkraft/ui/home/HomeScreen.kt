package app.atemkraft.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.domain.BreathingArea
import app.atemkraft.domain.BreathingFamily
import app.atemkraft.domain.DailyPattern
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.ExerciseFilter
import app.atemkraft.domain.ExerciseFilters
import app.atemkraft.domain.PatternSpec
import app.atemkraft.domain.Situation
import app.atemkraft.domain.SituationRecommendation
import app.atemkraft.domain.SituationSearch
import app.atemkraft.domain.area
import app.atemkraft.ui.components.ExerciseCard
import app.atemkraft.ui.components.GroupLabel
import app.atemkraft.ui.components.ScreenHeader
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.components.SettingsAction
import app.atemkraft.ui.components.TabListScaffold
import app.atemkraft.ui.components.color
import app.atemkraft.ui.components.title
import app.atemkraft.ui.theme.Dimens

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
    /** Verweis der Willkommenskarte; öffnet die Übung mit kurzer Dauer statt der Voreinstellung. */
    val onOpenWelcome: (String) -> Unit = onSelect,
    /** „Selbst gestalten ›“ im Blatt „Muster des Tages“: öffnet den Muster-Editor mit [PatternSpec] als Ausgangspunkt. */
    val onDesignFrom: (PatternSpec) -> Unit = {},
)

/**
 * Atmen-Tab: alles, was man starten kann. Von oben: beim ersten Start die Willkommenskarte
 * ([WelcomeCard], abweisbar), das Suchfeld „Wie fühlst du dich?“ mit den sechs Anlässen als Chips
 * ([FeelingBlock]), der Kopf „Übungen“ mit der Filterreihe und darunter die Übungen nach Familien
 * mit ihren kleinen farbigen Gruppenlabels ([GroupLabel]); zwischen Anlässen und „Übungen“ die
 * Kopfzeile „Muster des Tages ›“ ([DailyPatternEntry]).
 * Programme (Infoseiten ohne Start) stehen im Tab „Lernen“.
 *
 * Muster des Tages (Nutzer 2026-10-30): Das Tagesmuster – bewusst spielerisch, ohne eigenes
 * Wirkversprechen – steht als Kopfzeile mit Chevron über „Übungen“, losgelöst von Familien und
 * Filterreihe, und bleibt in jedem Zustand stehen. Sie öffnet das [DailyPatternSheet] mit der
 * Karte des Tagesmusters (↻, ◉ „In Eigene speichern“) und „Selbst gestalten“ mit dem Weg in den
 * Muster-Editor ([HomeActions.onDesignFrom], mit dem Tagesmuster als Ausgangspunkt). So liegt das Gestalten eigener Muster einen Tipp
 * neben der Startseite, ohne Platz in der Liste zu kosten.
 *
 * Ein Zustand (MUSTER-09): Gewählt ist entweder ein Anlass, eine Suche oder ein Bereich – oder
 * nichts, dann steht alles da wie immer (Suchfeld und Chips, „Muster des Tages ›“, „Übungen“ mit
 * Filterreihe, vier Familien). Die Kopfzeile „Muster des Tages ›“ steht in allen Zuständen.
 * - **Anlass:** Ein Tipp auf einen Chip markiert ihn; unter dem Block stehen sein Begründungssatz
 *   (bei der intensiven Situation mit ⚠-Zeile), seine Übungen, „Mehr: <Bereich> ›“
 *   ([AreaLink]) und „Alle Übungen ›“ ([AllExercisesLink], wie ein zweiter Chip-Tipp); die
 *   Familien treten zurück. Ein zweiter Tipp auf denselben Chip
 *   oder System-Zurück zeigt wieder alles.
 * - **Bereich:** „Mehr: …“ zeigt an derselben Stelle das Gruppenlabel der Familie und alle Übungen
 *   des Bereichs ([AreaHeader]); kein Chip ist markiert, die Eingabe leer. ✕ am Label oder Zurück
 *   führt zur Übersicht.
 * - **Suche:** Sobald die Eingabe ein Suchwort enthält ([SituationSearch]), sind die Chips der
 *   Treffer markiert, darunter stehen erst die Empfehlungen der Anlässe, beste zuerst, dann die
 *   direkt getroffenen Bereiche. Ein Chip leert die Eingabe, eine Eingabe hebt die Wahl auf.
 *
 * Im Grundzustand steht über den Familien der Kopf „Übungen“ mit der Filterreihe
 * ([ExerciseFilterRow], MUSTER-12, Nutzer 2026-10-23): Filter quer zu den Familien, UND-verknüpft;
 * die Liste bleibt nach Familien gruppiert, Familien ohne Treffer fallen samt Label weg. Die Wahl lebt nur
 * in `rememberSaveable` und bleibt, während ein Anlass, eine Suche oder ein Bereich gewählt ist –
 * die Reihe ist dann ausgeblendet.
 *
 * Der Bereich eines Anlasses ist die häufigste Familie seiner Übungen ([area]). Das Feld bekommt
 * beim Öffnen keinen Fokus; die Eingabe lebt nur in einem `TextFieldState` aus
 * `rememberTextFieldState` (`rememberSaveable`) – nie in DataStore oder Log (SEC-PRIV-03/04).
 * System-Zurück: Hat das Feld den Fokus, nimmt Zurück nur den Fokus (die Tastatur schließt, die
 * Eingabe bleibt); ohne Fokus leert es Wahl und Suche (Nutzer 2026-10-24).
 *
 * [welcome]: die Übung der Willkommenskarte; null = Karte weggeklickt (oder Übung fehlt).
 * [whileDailySheetOpen] ist komponiert, solange das Blatt offen ist, und zeigt nichts: Dort lädt der
 * NavGraph den Muster-Editor vor (`PatternEditorPreload`), den „Selbst gestalten“ öffnet.
 */
@Composable
fun HomeScreen(
    catalog: HomeCatalog,
    daily: DailyPattern,
    dailySaved: Boolean,
    actions: HomeActions,
    markedIds: Set<String> = emptySet(),
    welcome: Exercise? = null,
    whileDailySheetOpen: @Composable () -> Unit = {},
) {
    // Die Eingabe lebt allein in diesem Zustand (rememberTextFieldState = rememberSaveable, SEC-PRIV-03/04);
    // das Feld hält keinen zweiten Wert daneben (Nutzer 2026-10-24).
    val query = rememberTextFieldState()
    val queryText = query.text.toString()
    // Höchstens eins von beiden ist gesetzt, und nur bei leerer Eingabe (siehe KDoc).
    var chosen by rememberSaveable { mutableStateOf<Situation?>(null) }
    var chosenArea by rememberSaveable { mutableStateOf<BreathingFamily?>(null) }
    var filters by rememberSaveable { mutableStateOf(emptySet<ExerciseFilter>()) }
    var dailySheetOpen by rememberSaveable { mutableStateOf(false) }
    val results = remember(queryText, catalog.situations, catalog.areas) { SituationSearch.search(queryText, catalog.situations, catalog.areas) }
    val focusManager = LocalFocusManager.current
    val queryFocus = remember { MutableInteractionSource() }
    val queryFocused by queryFocus.collectIsFocusedAsState()
    // Eine Eingabe hebt die Wahl auf – nur Eingaben des Nutzers: Leert ein Chip das Feld
    // (clearText), läuft die Transformation nicht.
    val onInput = InputTransformation {
        if (length > 0) {
            chosen = null
            chosenArea = null
        }
    }
    val showAll = {
        query.clearText()
        chosen = null
        chosenArea = null
        focusManager.clearFocus()
    }
    // Zurück mit Fokus im Feld schließt nur die Tastatur (Fokus weg), die Eingabe bleibt – erst
    // ohne Fokus leert Zurück Wahl und Suche (Nutzer 2026-10-24).
    BackHandler(enabled = queryFocused || queryText.isNotEmpty() || chosen != null || chosenArea != null) {
        if (queryFocused) focusManager.clearFocus() else showAll()
    }
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
    ) {
        // Erster Start: eine ruhige, abweisbare Karte statt eines Onboardings (Nutzer 2026-10-24).
        if (welcome != null) {
            item(key = "welcome") {
                WelcomeCard(exercise = welcome, onOpen = { actions.onOpenWelcome(welcome.id) }, onDismiss = actions.onDismissWelcome)
            }
        }
        // Einstieg „nach Befinden“ ohne eigene Überschrift: Feld und Anlässe in einer Fläche.
        if (catalog.situations.isNotEmpty()) {
            item(key = "feeling") {
                FeelingBlock(
                    query = query,
                    queryFocus = queryFocus,
                    onInput = onInput,
                    situations = catalog.situations,
                    highlighted = highlighted,
                    onChipClick = { situation ->
                        // Zweiter Tipp auf denselben Chip zeigt wieder alles; ein Chip leert die Eingabe.
                        chosen = if (chosen == situation) null else situation
                        chosenArea = null
                        query.clearText()
                        focusManager.clearFocus()
                    },
                )
            }
        }
        // „Muster des Tages ›“ steht in jedem Zustand an derselben Stelle und öffnet das Blatt mit
        // Tagesmuster und „Selbst gestalten“ (Nutzer 2026-10-30). Darunter trennt eine Linie den
        // Einstieg von der Liste; ihr Kopf heißt „Übungen“ oder, mit Anlass, Suche oder Bereich,
        // „Passende Übungen“ – so liest sich nichts als Inhalt des Tagesmusters.
        item(key = "daily") {
            Column(modifier = Modifier.fillMaxWidth()) {
                DailyPatternEntry(daily = daily, onClick = { dailySheetOpen = true })
                // Die tippbare Zeile polstert unten GapSmall selbst; sichtbar bleibt SectionGap.
                Spacer(Modifier.height(Dimens.GapSmall))
                // Trenner zwischen zwei Bereichen der Seite, kein Karten-Trenner: DividerSubtle war hier kaum zu sehen.
                HorizontalDivider(thickness = Dimens.BorderSection, color = MaterialTheme.colorScheme.outlineVariant) // Abweichung KOMP-03: Bereichs-Trenner in der Haarlinie des Seitenkopfs
            }
        }
        if (filtering) {
            item(key = "results-head") {
                SectionHeader(title = stringResource(R.string.home_results), top = HEADER_BELOW_DIVIDER)
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
                // Bei einem gewählten Anlass führt darunter „Alle Übungen ›“ zurück zur ganzen
                // Liste; beide Verweise stehen ohne Abstand untereinander (je 48 dp Tippfläche).
                val area = rec.area(catalog.resolve)?.takeIf { it !in directAreas }
                val showAllLink = chosen != null
                if (area != null || showAllLink) {
                    item(key = "rec-${rec.situation.name}-links") {
                        Column {
                            if (area != null) {
                                AreaLink(family = area) {
                                    chosen = null
                                    chosenArea = area
                                    query.clearText()
                                    focusManager.clearFocus()
                                }
                            }
                            if (showAllLink) AllExercisesLink(onClick = showAll)
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
            // Ohne Filter steht das erste Gruppenlabel im Eintrag des Kopfs (siehe unten).
            val firstFamily = filtered.firstOrNull()?.first?.takeIf { filters.isEmpty() }
            // Kopf „Übungen“, Filterreihe und Trefferstatus bzw. das erste Gruppenlabel in einem
            // Eintrag: Die Filter-Chips bringen über und unter sich je GapSmall Tippfläche mit; mit
            // dem ListGap der Liste stünden sie sichtbar 20 dp vom Kopf ab. So sind es oben 12
            // (HeaderGap – der Spacer ergänzt die unsichtbaren 8) und unten bis zum Gruppenlabel 16
            // (SectionGap).
            item(key = "exercises") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(title = stringResource(R.string.home_all_exercises), top = HEADER_BELOW_DIVIDER)
                    Spacer(Modifier.height(Dimens.GapTiny))
                    ExerciseFilterRow(active = filters, onToggle = { filters = if (it in filters) filters - it else filters + it })
                    if (filters.isNotEmpty()) {
                        Spacer(Modifier.height(Dimens.GapTiny))
                        FilterStatus(count = filtered.sumOf { it.second.size }, onReset = { filters = emptySet() })
                    } else if (firstFamily != null) {
                        GroupLabel(title = firstFamily.title(), color = firstFamily.color(), top = GROUP_LABEL_BELOW_CHIPS)
                    }
                }
            }
            filtered.forEachIndexed { index, (family, exercises) ->
                if (index > 0 || firstFamily == null) {
                    item(key = "header-${family.name}") {
                        GroupLabel(title = family.title(), color = family.color(), top = GROUP_LABEL_LIST_TOP)
                    }
                }
                items(exercises, key = { it.id }) { exercise ->
                    ExerciseCard(exercise = exercise, onClick = { actions.onSelect(exercise.id) }, marked = exercise.id in markedIds)
                }
            }
        }
    }
    if (dailySheetOpen) {
        DailyPatternSheet(
            daily = daily,
            saved = dailySaved,
            // Das Blatt schließt sich selbst, bevor es diese meldet (AppBottomSheetScope.closeThen).
            onOpen = { actions.onSelect(daily.exercise.id) },
            onSavedChange = actions.onDailySavedChange,
            onRegenerate = actions.onRegenerateDaily,
            onNewPattern = { actions.onDesignFrom(daily.spec) },
            onDismiss = { dailySheetOpen = false },
        )
        whileDailySheetOpen()
    }
}

/**
 * Abstand über dem Kopf „Übungen“ bzw. „Passende Übungen“ unter der Linie: Mit dem ListGap der
 * Liste stehen sichtbar SectionGap zwischen Linie und Kopf, wie über der Linie.
 */
private val HEADER_BELOW_DIVIDER = Dimens.SectionGap - Dimens.ListGap

/**
 * Abstand über dem ersten Gruppenlabel direkt unter der Filterreihe: Die Chips tragen unter sich
 * GapSmall unsichtbare Tippfläche, sichtbar bleibt so SectionGap.
 */
private val GROUP_LABEL_BELOW_CHIPS = Dimens.SectionGap - Dimens.GapSmall
