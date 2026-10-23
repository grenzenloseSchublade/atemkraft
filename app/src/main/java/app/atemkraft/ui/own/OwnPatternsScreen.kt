package app.atemkraft.ui.own

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import app.atemkraft.ui.components.AppIconButton
import app.atemkraft.ui.components.AppOutlinedButton
import app.atemkraft.ui.components.CollapsibleSection
import app.atemkraft.ui.components.ConfirmIconButton
import app.atemkraft.ui.components.ExerciseCard
import app.atemkraft.ui.components.HintText
import app.atemkraft.ui.components.PatternCard
import app.atemkraft.ui.components.ScreenHeader
import app.atemkraft.ui.components.SettingsAction
import app.atemkraft.ui.components.TabListScaffold
import app.atemkraft.ui.components.headingColor
import app.atemkraft.ui.log.LogHistoryCard
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Sizes
import java.time.LocalDate

/** Was der Tab „Eigene“ auslöst (CODE-02): neues Muster, Übung öffnen, Muster löschen, Einstellungen, Logbuch. */
@Immutable
data class OwnActions(
    val onNewPattern: () -> Unit,
    val onSelect: (String) -> Unit,
    val onDeletePattern: (Long) -> Unit,
    val onOpenSettings: () -> Unit,
    val onOpenLogbook: () -> Unit = {},
)

/**
 * Tab „Eigene“, der persönliche Bereich (Nutzer 2026-10-23): oben die Karte „Verlauf“ als
 * Einstieg ins Logbuch ([LogHistoryCard]: Wochenpunkte und Bilanz, die ganze Karte führt zur
 * Logbuch-Seite, [onOpenLogbook]; ohne Sitzungen ein Satz, die Karte bleibt), darunter bis zu zwei
 * Abschnitte – „Tut mir gut“ (die Übungen mit der persönlichen Markierung, [favorites]) und „Eigene
 * Kreationen“ (gespeicherte Tagesmuster und selbst zusammengestellte Muster, neueste zuerst wie aus
 * der Datenbank). „Neues Muster“ (öffnet den Muster-Editor) ist das „+“ im Kopf von „Eigene
 * Kreationen“; ohne eigene Kreationen gibt es den Kopf nicht, dann steht ein kurzer Satz mit dem
 * vollen Knopf „+ Neues Muster“ da.
 *
 * Einklappbar (MUSTER-03) erst, wenn beide Abschnitte da sind, mit Caret und der Anzahl im Kopf
 * („Tut mir gut · 4“); beide stehen zu Beginn offen, die Wahl gilt für die Sitzung
 * (`rememberSaveable`), nicht dauerhaft. Ein einzelner Abschnitt steht offen ohne Caret. Köpfe
 * reihum nach ihrer Lage (FARBE-07); der Titel der Karte „Verlauf“ steht neutral wie jeder
 * Kartentitel.
 *
 * Löschen bleibt zweistufig in der Kartenecke (MUSTER-05). Der Kopf gleicht den anderen Tabs:
 * Titel, Untertitel, Zahnrad zu den Einstellungen. [today] nur für reproduzierbare Screenshots.
 */
@Composable
fun OwnPatternsScreen(
    savedPatterns: List<SavedPattern>,
    actions: OwnActions,
    favorites: List<Exercise> = emptyList(),
    logEntries: List<SessionLogEntry> = emptyList(),
    today: LocalDate = LocalDate.now(),
) {
    var favoritesExpanded by rememberSaveable { mutableStateOf(true) }
    var patternsExpanded by rememberSaveable { mutableStateOf(true) }
    val collapsible = favorites.isNotEmpty() && savedPatterns.isNotEmpty()
    val favoritesStatus = stringResource(R.string.situations_my_patterns_count, favorites.size)
    val favoritesDescription = pluralStringResource(R.plurals.cd_own_favorites_count, favorites.size, favorites.size)
    val patternsStatus = stringResource(R.string.situations_my_patterns_count, savedPatterns.size)
    val patternsDescription = pluralStringResource(R.plurals.cd_own_patterns_count, savedPatterns.size, savedPatterns.size)

    // Der Kopf steht fest über der Liste; liegt Inhalt darunter, zeigt er eine Haarlinie.
    TabListScaffold(
        header = { scrolled ->
            ScreenHeader(
                title = stringResource(R.string.own_title),
                action = { SettingsAction(onClick = actions.onOpenSettings) },
                scrolled = scrolled,
            )
        },
        subtitle = stringResource(R.string.own_subtitle),
    ) {
        item(key = "history") {
            LogHistoryCard(entries = logEntries, onClick = actions.onOpenLogbook, today = today)
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
                        ExerciseCard(exercise = exercise, marked = true, onClick = { actions.onSelect(exercise.id) })
                    }
                }
            }
        }

        if (savedPatterns.isNotEmpty()) {
            val color = headingColor(head)
            item(key = "my-patterns") {
                OwnSection(
                    title = stringResource(R.string.own_creations),
                    color = color,
                    expanded = patternsExpanded.takeIf { collapsible },
                    onExpandedChange = { patternsExpanded = it },
                    status = patternsStatus.takeIf { collapsible },
                    statusDescription = patternsDescription,
                    // „Neues Muster“ gehört zum Abschnitt: Icon-only im Kopf, so groß wie „Neu
                    // generieren“ beim Muster des Tages (AppIconButton), in der Farbe des Kopfs.
                    trailingAction = {
                        AppIconButton(onClick = actions.onNewPattern) {
                            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.own_new_pattern), tint = color)
                        }
                    },
                ) {
                    savedPatterns.forEach { pattern ->
                        // Je Muster ein eigener Zustand des zweistufigen Löschens, auch wenn die Liste sich ändert.
                        key(pattern.id) {
                            SavedPatternCard(
                                pattern = pattern,
                                onClick = { actions.onSelect(pattern.exercise.id) },
                                onDelete = { actions.onDeletePattern(pattern.id) },
                            )
                        }
                    }
                }
            }
        } else {
            // Ohne eigene Kreationen kein leerer Kopf: Satz und voller Knopf als Einstieg.
            item(key = "new-pattern") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(Dimens.SectionGap))
                    HintText(stringResource(R.string.own_empty))
                    Spacer(Modifier.height(Dimens.GapSmall))
                    NewPatternButton(onClick = actions.onNewPattern)
                }
            }
        }
    }
}

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
    trailingAction: (@Composable () -> Unit)? = null,
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
        trailingAction = trailingAction,
        content = content,
    )
}

/** „+ Neues Muster“ über die volle Breite: Einstieg, solange es keine eigenen Kreationen gibt; umrandet wie die Daten-Knöpfe. */
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
