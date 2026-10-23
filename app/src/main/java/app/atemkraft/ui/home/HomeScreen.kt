package app.atemkraft.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.items
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
import app.atemkraft.domain.Situation
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
import app.atemkraft.ui.theme.SECONDARY

/**
 * Atmen-Tab: alles, was man starten kann. Von oben: Muster des Tages, der Block „Wie fühlst du
 * dich?“ (Suchfeld und die sechs Anlässe als Chips, [FeelingBlock]), dann die Übungen nach
 * Familien – deren Köpfe sind die einzige große Überschriften-Ebene unter dem Muster des Tages.
 * Programme (Infoseiten ohne Start) stehen im Tab „Lernen“.
 *
 * Ein Zustand, nicht zwei (MUSTER-09): Ein Tipp auf einen Chip wählt den Anlass – unter dem Block
 * stehen sein Begründungssatz und seine Übungen, die Familien treten zurück; ein zweiter Tipp auf
 * denselben Chip oder System-Zurück zeigt wieder alles. Sobald die Eingabe ein Suchwort enthält
 * ([SituationSearch]), leuchten die Chips der Treffer, die übrigen werden blass, und darunter
 * stehen die Empfehlungen der Treffer, beste zuerst. Ein Chip leert die Eingabe, eine Eingabe hebt
 * die Wahl auf. Das Feld bekommt beim Öffnen keinen Fokus; die Eingabe lebt nur in
 * `rememberSaveable` – nie in DataStore oder Log (SEC-PRIV-03/04). System-Zurück leert zuerst
 * Wahl und Suche und schließt die Tastatur.
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
    var chosen by rememberSaveable { mutableStateOf<Situation?>(null) }
    val results = remember(query, situations) { SituationSearch.search(query, situations) }
    val focusManager = LocalFocusManager.current
    val showAll = {
        query = ""
        chosen = null
        focusManager.clearFocus()
    }
    BackHandler(enabled = query.isNotEmpty() || chosen != null, onBack = showAll)
    // Was unter dem Block steht: der gewählte Anlass, die Suchtreffer oder nichts (dann die Familien).
    val shown: List<SituationRecommendation>? = when {
        chosen != null -> situations.filter { it.situation == chosen }
        else -> results
    }
    val highlighted = shown.orEmpty().map { it.situation }.toSet()
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

        // Einstieg „nach Befinden“ ohne eigene Überschrift: Feld und Anlässe in einer Fläche.
        if (situations.isNotEmpty()) {
            item(key = "feeling") {
                FeelingBlock(
                    query = query,
                    onQueryChange = { text ->
                        query = text
                        if (text.isNotEmpty()) chosen = null
                    },
                    situations = situations,
                    highlighted = highlighted,
                    onChipClick = { situation ->
                        // Zweiter Tipp auf denselben Chip zeigt wieder alles; ein Chip leert die Eingabe.
                        chosen = if (chosen == situation) null else situation
                        query = ""
                        focusManager.clearFocus()
                    },
                )
            }
        }
        if (results != null) {
            item(key = "search-status") {
                SearchStatus(count = results.size, onShowAll = showAll)
            }
        }
        if (shown != null) {
            val withTitle = shown.size > 1
            shown.forEach { rec ->
                item(key = "rec-${rec.situation.name}") { RecommendationIntro(rec, withTitle) }
                items(rec.exerciseIds.mapNotNull(resolve), key = { "rec-${rec.situation.name}-${it.id}" }) { exercise ->
                    ExerciseCard(exercise = exercise, onClick = { onSelect(exercise.id) }, marked = exercise.id in markedIds)
                }
            }
        } else {
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
