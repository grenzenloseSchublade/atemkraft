package app.atemkraft.ui.own

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.data.SavedPattern
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.OwnCollection
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.SessionLogEntry
import app.atemkraft.ui.components.ScreenHeader
import app.atemkraft.ui.components.SettingsAction
import app.atemkraft.ui.components.TabListScaffold
import app.atemkraft.ui.components.pageTabsTopSlack
import app.atemkraft.ui.log.LogHistory
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import java.time.LocalDate

/**
 * Was der Tab „Eigene“ auslöst (CODE-02): neues Muster, Übung öffnen, Muster löschen, Einstellungen,
 * Logbuch; [onEditPattern] öffnet den Muster-Editor für ein gespeichertes Muster (DB-Id, Stift auf
 * seiner Karte); [onWeeksOpenChange] merkt, ob der Verlauf vier Wochen zeigt,
 * [onCollectionChange] die gewählte Sammlung; [onOpenAllCreations]/[onOpenAllFavorites] öffnen die
 * ganze Sammlung als Liste.
 */
@Immutable
data class OwnActions(
    val onNewPattern: () -> Unit,
    val onSelect: (String) -> Unit,
    val onDeletePattern: (Long) -> Unit,
    val onOpenSettings: () -> Unit,
    val onOpenLogbook: () -> Unit = {},
    val onEditPattern: (Long) -> Unit = {},
    val onWeeksOpenChange: (Boolean) -> Unit = {},
    val onCollectionChange: (OwnCollection) -> Unit = {},
    val onOpenAllCreations: () -> Unit = {},
    val onOpenAllFavorites: () -> Unit = {},
)

/**
 * Ansichtswahl des Tabs „Eigene“: [showStreak] (Einstellung „Serie zeigen“; aus = keine Tage in
 * Folge im Verlauf), [weeksOpen] (Verlauf mit vier Wochen) und die gewählte Sammlung [collection] –
 * die letzten beiden dauerhaft gemerkt (DataStore, Nutzer 2026-10-31).
 */
@Immutable
data class OwnView(
    val showStreak: Boolean = true,
    val weeksOpen: Boolean = false,
    val collection: OwnCollection = OwnCollection.CREATIONS,
)

/**
 * Tab „Eigene“, der persönliche Bereich (Nutzer 2026-10-23; neu geordnet 2026-10-31, weil die
 * gestapelten Abschnitte fad wirkten und ihr Klappzustand nicht blieb).
 *
 * Oben ohne eigenen Kopf (STYLEGUIDE §4.3, Tab: der erste Eintrag ist Inhalt; Eigentümer
 * 2026-10-31) die Kalenderwoche, aufklappbar bis vier Wochen ([OwnView.weeksOpen], dauerhaft gemerkt), die
 * Bilanz mit „Logbuch ›“ ([OwnActions.onOpenLogbook]) und „Zuletzt geübt“ mit bis zu drei Übungen ([recentExercises]; [LogHistory]). Darunter
 * die Sammlungen ([OwnCollections]): ein Umschalter wie Tabs in der Seite „Eigene Kreationen · 4 |
 * Tut mir gut · 3“ ([OwnView.collection], dauerhaft gemerkt) und für die gewählte Sammlung Karteikarten
 * zum Wischen – Kreationen mit der Karte „Neues Muster“ vorn und Stift und Löschen je Muster,
 * „Tut mir gut“ mit ◉. Verlauf bleibt zuerst (Nutzer 2026-10-30).
 *
 * [today] nur für reproduzierbare Screenshots.
 */
@Composable
fun OwnPatternsScreen(
    savedPatterns: List<SavedPattern>,
    actions: OwnActions,
    favorites: List<Exercise> = emptyList(),
    logEntries: List<SessionLogEntry> = emptyList(),
    recentExercises: List<Exercise> = emptyList(),
    view: OwnView = OwnView(),
    today: LocalDate = LocalDate.now(),
) {
    // Die Lage jeder Sammlung überlebt Umschalten und Tab-Wechsel (gespeicherter Zustand des Tabs).
    val creationsRow = rememberLazyListState()
    val favoritesRow = rememberLazyListState()
    // Der Kopf steht fest über der Liste; liegt Inhalt darunter, zeigt er eine Haarlinie.
    TabListScaffold(
        header = {
            ScreenHeader(
                title = stringResource(R.string.own_title),
                action = { SettingsAction(onClick = actions.onOpenSettings) },
            )
        },
    ) {
        item(key = "history") {
            // Erster Eintrag ohne Kopf: Er bringt seinen Abstand zum Seitentitel selbst mit (Tab, top = 0).
            Column(modifier = Modifier.fillMaxWidth().padding(top = Dimens.GapSmall)) {
                LogHistory(
                    entries = logEntries,
                    today = today,
                    showStreak = view.showStreak,
                    weeksOpen = view.weeksOpen,
                    onWeeksOpenChange = actions.onWeeksOpenChange,
                    recent = recentExercises,
                    onOpenExercise = actions.onSelect,
                    onOpenLogbook = actions.onOpenLogbook,
                )
            }
        }

        // Unter dem Verlauf die Sammlungen: Umschalter, darunter Karteikarten (Nutzer 2026-10-31).
        item(key = "collections") {
            // Sichtbar SectionHeaderTop bis zum Namen der Tabs wie vor jedem Abschnitt: abzüglich ListGap
            // der Liste und des Leerraums, den die 48-dp-Tippfläche der Tabs über dem Namen lässt.
            val top = (Dimens.SectionHeaderTop - Dimens.ListGap - pageTabsTopSlack()).coerceAtLeast(0.dp)
            Column(modifier = Modifier.fillMaxWidth().padding(top = top)) {
                OwnCollections(
                    selected = view.collection,
                    onSelect = actions.onCollectionChange,
                    patterns = savedPatterns,
                    favorites = favorites,
                    creationsRow = creationsRow,
                    favoritesRow = favoritesRow,
                    actions = actions,
                )
            }
        }
    }
}

@Preview
@Composable
private fun OwnPatternsScreenPreview() {
    val patterns = listOf(11L, 42L).mapIndexed { i, seed ->
        RandomPatternGenerator.forSeed(seed).let { SavedPattern(id = i + 1L, exercise = it.exercise, spec = it.spec) }
    }
    AtemkraftTheme {
        OwnPatternsScreen(savedPatterns = patterns, actions = OwnActions(onNewPattern = {}, onSelect = {}, onDeletePattern = {}, onOpenSettings = {}))
    }
}

@Preview
@Composable
private fun OwnPatternsScreenEmptyPreview() {
    AtemkraftTheme {
        OwnPatternsScreen(savedPatterns = emptyList(), actions = OwnActions(onNewPattern = {}, onSelect = {}, onDeletePattern = {}, onOpenSettings = {}))
    }
}
