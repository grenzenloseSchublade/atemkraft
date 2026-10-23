package app.atemkraft.ui.own

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.data.SavedPattern
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.SessionLogEntry
import app.atemkraft.ui.components.AppOutlinedButton
import app.atemkraft.ui.components.AppTextButton
import app.atemkraft.ui.components.CollapsibleSection
import app.atemkraft.ui.components.ConfirmIconButton
import app.atemkraft.ui.components.ExerciseCard
import app.atemkraft.ui.components.ForwardLinkText
import app.atemkraft.ui.components.HintText
import app.atemkraft.ui.components.PatternCard
import app.atemkraft.ui.components.ScreenHeader
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.components.SettingsAction
import app.atemkraft.ui.components.TabListScaffold
import app.atemkraft.ui.components.flushStart
import app.atemkraft.ui.components.headingColor
import app.atemkraft.ui.log.LogStatsCard
import app.atemkraft.ui.log.RecentLogCard
import app.atemkraft.ui.log.logSummary
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Sizes
import java.time.LocalDate

/**
 * Tab „Eigene“, der persönliche Bereich: oben fest „Neues Muster“ (öffnet den Muster-Editor), dann
 * bis zu drei Abschnitte – „Tut mir gut“ (die Übungen mit der persönlichen Markierung,
 * [favorites], Nutzer 2026-10-23), „Meine Muster“ (gespeicherte Tagesmuster und selbst
 * zusammengestellte Muster, neueste zuerst wie aus der Datenbank) und „Logbuch“ ([logEntries]:
 * Kennzahlen mit Wochenübersicht, die letzten [RECENT_LOG_ENTRIES] Sitzungen und „Alle Sitzungen ›“
 * zur Logbuch-Seite, [onOpenLogbook]). Leere Abschnitte erscheinen nicht; ohne Muster steht über
 * dem Knopf ein kurzer Satz.
 *
 * Einklappbar (MUSTER-03, Nutzer 2026-10-23) erst ab zwei sichtbaren Abschnitten, mit Caret und
 * der Anzahl bzw. kurzen Bilanz im Kopf („Tut mir gut · 4“, „Logbuch · 3 Tage in Folge“). Muster und
 * Markierungen stehen zu Beginn offen, das Logbuch zu; die Wahl gilt für die Sitzung
 * (`rememberSaveable`), nicht dauerhaft. Ein einzelner Abschnitt steht offen ohne Caret.
 *
 * Löschen bleibt zweistufig in der Kartenecke (MUSTER-05). Der Kopf gleicht den anderen Tabs:
 * Titel, Untertitel, Zahnrad zu den Einstellungen; einen Kalender braucht er nicht, der Abschnitt
 * „Logbuch“ führt zum Logbuch. [today] nur für reproduzierbare Screenshots.
 */
@Composable
fun OwnPatternsScreen(
    savedPatterns: List<SavedPattern>,
    onNewPattern: () -> Unit,
    onSelect: (String) -> Unit,
    onDeletePattern: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    favorites: List<Exercise> = emptyList(),
    logEntries: List<SessionLogEntry> = emptyList(),
    onOpenLogbook: () -> Unit = {},
    today: LocalDate = LocalDate.now(),
) {
    var favoritesExpanded by rememberSaveable { mutableStateOf(true) }
    var patternsExpanded by rememberSaveable { mutableStateOf(true) }
    var logExpanded by rememberSaveable { mutableStateOf(false) }
    val collapsible = listOf(favorites.isNotEmpty(), savedPatterns.isNotEmpty(), logEntries.isNotEmpty()).count { it } >= 2
    val favoritesStatus = stringResource(R.string.situations_my_patterns_count, favorites.size)
    val favoritesDescription = pluralStringResource(R.plurals.cd_own_favorites_count, favorites.size, favorites.size)
    val patternsStatus = stringResource(R.string.situations_my_patterns_count, savedPatterns.size)
    val patternsDescription = pluralStringResource(R.plurals.cd_own_patterns_count, savedPatterns.size, savedPatterns.size)
    val logSummary = logSummary(logEntries, today)
    val logStatus = stringResource(R.string.own_log_status, logSummary)

    // Der Kopf steht fest über der Liste; liegt Inhalt darunter, zeigt er eine Haarlinie.
    TabListScaffold(
        header = { scrolled ->
            ScreenHeader(
                title = stringResource(R.string.own_title),
                action = { SettingsAction(onClick = onOpenSettings) },
                scrolled = scrolled,
            )
        },
        subtitle = stringResource(R.string.own_subtitle),
    ) {
        item(key = "new-pattern") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(Dimens.SectionGap))
                if (savedPatterns.isEmpty()) {
                    HintText(stringResource(R.string.own_empty))
                    Spacer(Modifier.height(Dimens.GapSmall))
                }
                NewPatternButton(onClick = onNewPattern)
            }
        }

        // Köpfe reihum nach ihrer Lage (FARBE-07): Es zählen nur die sichtbaren Abschnitte.
        var head = 0
        if (favorites.isNotEmpty()) {
            val color = headingColor(head++)
            item(key = "favorites") {
                OwnSection(
                    title = stringResource(R.string.own_favorites),
                    color = color,
                    expanded = favoritesExpanded.takeIf { collapsible },
                    onExpandedChange = { favoritesExpanded = it },
                    status = favoritesStatus.takeIf { collapsible },
                    statusDescription = favoritesDescription,
                ) {
                    favorites.forEach { exercise ->
                        ExerciseCard(exercise = exercise, marked = true, onClick = { onSelect(exercise.id) })
                    }
                }
            }
        }

        if (savedPatterns.isNotEmpty()) {
            val color = headingColor(head++)
            item(key = "my-patterns") {
                OwnSection(
                    title = stringResource(R.string.situations_my_patterns),
                    color = color,
                    expanded = patternsExpanded.takeIf { collapsible },
                    onExpandedChange = { patternsExpanded = it },
                    status = patternsStatus.takeIf { collapsible },
                    statusDescription = patternsDescription,
                ) {
                    savedPatterns.forEach { pattern ->
                        // Je Muster ein eigener Zustand des zweistufigen Löschens, auch wenn die Liste sich ändert.
                        key(pattern.id) {
                            SavedPatternCard(
                                pattern = pattern,
                                onClick = { onSelect(pattern.exercise.id) },
                                onDelete = { onDeletePattern(pattern.id) },
                            )
                        }
                    }
                }
            }
        }

        if (logEntries.isNotEmpty()) {
            val color = headingColor(head)
            item(key = "logbook") {
                OwnSection(
                    title = stringResource(R.string.settings_log_title),
                    color = color,
                    expanded = logExpanded.takeIf { collapsible },
                    onExpandedChange = { logExpanded = it },
                    status = logStatus.takeIf { collapsible },
                    statusDescription = logSummary,
                ) {
                    LogStatsCard(entries = logEntries, today = today)
                    RecentLogCard(entries = logEntries, limit = RECENT_LOG_ENTRIES, today = today)
                    // Bündig: Das Polster des Text-Buttons ragt in den Rand.
                    AppTextButton(onClick = onOpenLogbook, modifier = Modifier.flushStart()) {
                        ForwardLinkText(text = stringResource(R.string.own_log_all), color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

/** Wie viele Sitzungen der Abschnitt „Logbuch“ zeigt; alle stehen auf der Logbuch-Seite. */
private const val RECENT_LOG_ENTRIES = 3

/**
 * Ein Abschnitt des Tabs als ein Listeneintrag ([CollapsibleSection]); `expanded = null` heißt fest
 * offen ohne Caret (nur ein Abschnitt sichtbar). Den Abstand darüber setzt schon die Liste.
 */
@Composable
private fun OwnSection(
    title: String,
    color: Color,
    expanded: Boolean?,
    onExpandedChange: (Boolean) -> Unit,
    status: String?,
    statusDescription: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    CollapsibleSection(
        title = title,
        color = color,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        status = status,
        statusDescription = statusDescription.takeIf { status != null },
        spacingAbove = Dimens.ListGap,
        content = content,
    )
}

/** „+ Neues Muster“ über die volle Breite: die Hauptaktion des Tabs, umrandet wie die Daten-Knöpfe. */
@Composable
private fun NewPatternButton(onClick: () -> Unit) {
    AppOutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(Sizes.IconInButton)) // dekorativ: der Text benennt den Knopf
        Spacer(Modifier.width(Dimens.GapSmall))
        Text(stringResource(R.string.own_new_pattern))
    }
}

/**
 * Karte eines gespeicherten Musters: aufgebaut wie das Muster des Tages ([PatternCard]), in der
 * Ecke Löschen – zweistufig, ein Fehltipp löscht nichts (MUSTER-05).
 */
@Composable
private fun SavedPatternCard(
    pattern: SavedPattern,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val name = pattern.exercise.name
    PatternCard(exercise = pattern.exercise, patternLine = pattern.patternLine, onClick = onClick) {
        ConfirmIconButton(
            name = name,
            description = stringResource(R.string.cd_pattern_delete, name),
            onConfirm = onDelete,
        )
    }
}

@Preview
@Composable
private fun OwnPatternsScreenPreview() {
    val patterns = listOf(11L, 42L).mapIndexed { i, seed ->
        RandomPatternGenerator.forSeed(seed).let { SavedPattern(id = i + 1L, exercise = it.exercise, spec = it.spec) }
    }
    AtemkraftTheme {
        OwnPatternsScreen(savedPatterns = patterns, onNewPattern = {}, onSelect = {}, onDeletePattern = {}, onOpenSettings = {})
    }
}

@Preview
@Composable
private fun OwnPatternsScreenEmptyPreview() {
    AtemkraftTheme {
        OwnPatternsScreen(savedPatterns = emptyList(), onNewPattern = {}, onSelect = {}, onDeletePattern = {}, onOpenSettings = {})
    }
}
