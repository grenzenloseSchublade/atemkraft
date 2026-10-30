package app.atemkraft.ui.own

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import app.atemkraft.ui.log.LogHistory
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes
import java.time.LocalDate

/**
 * Was der Tab „Eigene“ auslöst (CODE-02): neues Muster, Übung öffnen, Muster löschen, Einstellungen,
 * Logbuch; [onEditPattern] öffnet den Muster-Editor für ein gespeichertes Muster (DB-Id, Stift auf
 * seiner Karte).
 */
@Immutable
data class OwnActions(
    val onNewPattern: () -> Unit,
    val onSelect: (String) -> Unit,
    val onDeletePattern: (Long) -> Unit,
    val onOpenSettings: () -> Unit,
    val onOpenLogbook: () -> Unit = {},
    val onEditPattern: (Long) -> Unit = {},
)

/**
 * Tab „Eigene“, der persönliche Bereich (Nutzer 2026-10-23; Aufbau 2026-10-24, Vorschlag B): drei
 * gleiche Köpfe. Oben „Eigene Kreationen“ (gespeicherte Tagesmuster und selbst zusammengestellte
 * Muster, neueste zuerst wie aus der Datenbank) mit „+“ rechts im Kopf (öffnet den Muster-Editor) –
 * „Eigene“ heißt eigene Muster, das Gestalten steht vorn (Nutzer 2026-10-30). Dann „Verlauf“ als
 * tippbarer Kopf mit Chevron – die ganze Kopfzeile führt zur Logbuch-Seite ([onOpenLogbook]) –,
 * darunter ohne Karte die Wochenpunkte und die Bilanz ([LogHistory]; ohne Sitzungen nur der Satz),
 * zuletzt „Tut mir gut“ (die Übungen mit der persönlichen Markierung, [favorites]). Ohne eigene Kreationen bleibt der Kopf mit „+“ stehen, darunter eine
 * ruhige Karte mit dem Satz des Leerzustands und dem Verweis „Muster gestalten ›“ ([EmptyCreationsCard];
 * MUSTER-04, kein großer Knopf).
 *
 * Einklappbar (MUSTER-03) erst, wenn beide Abschnitte gefüllt sind, mit Caret und der Anzahl im
 * Kopf („Tut mir gut · 4“); beide stehen zu Beginn offen, die Wahl gilt für die Sitzung
 * (`rememberSaveable`), nicht dauerhaft. Ein einzelner Abschnitt steht offen ohne Caret. Alle
 * Köpfe weiß (FARBE-07).
 *
 * Jede Kreationen-Karte trägt in der Ecke den Stift (Editor für dieses Muster) und daneben
 * Löschen, zweistufig (MUSTER-05; [SavedPatternCard]). Der Kopf gleicht den anderen Tabs:
 * Titel und Zahnrad zu den Einstellungen. [today] nur für reproduzierbare Screenshots.
 *
 * [lastExercise]: die zuletzt geübte Übung, die es noch gibt (eingebaute Übung oder vorhandenes
 * gespeichertes Muster; keine Meditation, kein Tagesmuster, kein gelöschtes Muster) – unter dem
 * Verlauf als Verweis „Zuletzt: <Name> ›“ zu ihrer Detailseite (Auswertung 2026-10-24, UX #3:
 * „das von gestern Abend nochmal“ ohne Umweg über das Logbuch); null = kein Verweis.
 * [showStreak]: Einstellung „Serie zeigen“; aus = keine Tage in Folge im Verlauf.
 */
@Composable
fun OwnPatternsScreen(
    savedPatterns: List<SavedPattern>,
    actions: OwnActions,
    favorites: List<Exercise> = emptyList(),
    logEntries: List<SessionLogEntry> = emptyList(),
    lastExercise: Exercise? = null,
    showStreak: Boolean = true,
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
    ) {
        // „Eigene“ heißt eigene Muster: die Kreationen zuerst, mit „+“ im Kopf (Nutzer 2026-10-30).
        item(key = "my-patterns") {
            OwnSection(
                title = stringResource(R.string.own_creations),
                expanded = patternsExpanded.takeIf { collapsible },
                onExpandedChange = { patternsExpanded = it },
                status = patternsStatus.takeIf { collapsible },
                statusDescription = patternsDescription,
                // „Neues Muster“ gehört zum Abschnitt: Icon-only im Kopf (AppIconButton) in
                // onSurface wie jede Kopf-Aktion; im Leerzustand kommt der Verweis in der Karte dazu.
                trailingAction = {
                    AppIconButton(onClick = actions.onNewPattern) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.own_new_pattern), tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
            ) {
                if (savedPatterns.isEmpty()) {
                    EmptyCreationsCard(onNewPattern = actions.onNewPattern)
                }
                savedPatterns.forEach { pattern ->
                    // Je Muster ein eigener Zustand des zweistufigen Löschens, auch wenn die Liste sich ändert.
                    key(pattern.id) {
                        SavedPatternCard(
                            pattern = pattern,
                            onClick = { actions.onSelect(pattern.exercise.id) },
                            onEdit = { actions.onEditPattern(pattern.id) },
                            onDelete = { actions.onDeletePattern(pattern.id) },
                        )
                    }
                }
            }
        }

        item(key = "history") {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Die ganze Kopfzeile ist der Button zur Logbuch-Seite (Chevron, MUSTER-08).
                SectionHeader(
                    title = stringResource(R.string.own_history),
                    onClick = actions.onOpenLogbook,
                    onClickLabel = stringResource(R.string.cd_open_logbook),
                )
                // Kopf → Inhalt HeaderGap; den GapSmall darunter polstert die tippbare Zeile selbst.
                Spacer(Modifier.height(Dimens.HeaderGap - Dimens.GapSmall))
                LogHistory(entries = logEntries, today = today, showStreak = showStreak)
                if (lastExercise != null) {
                    // Bündig: Das Polster des Text-Buttons ragt in den Rand.
                    AppTextButton(onClick = { actions.onSelect(lastExercise.id) }, modifier = Modifier.flushStart()) {
                        ForwardLinkText(
                            text = stringResource(R.string.own_last_practiced, lastExercise.name),
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }

        if (favorites.isNotEmpty()) {
            item(key = "favorites") {
                OwnSection(
                    title = stringResource(R.string.own_favorites),
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
    }
}

/**
 * Leerzustand unter „Eigene Kreationen“ (MUSTER-04): eine ruhige Fläche (`surfaceContainerLow`,
 * Kartenform, Polster `CardPadding`) mit dem Satz „Noch keine eigenen Kreationen …“ und darunter
 * der Einstieg „Muster gestalten ›“ als Verweis ([ForwardLinkText] im bündigen [AppTextButton] in
 * `secondary`, wie jeder Verweis im Inhalt und der Verweis der Willkommenskarte, FARBE-04) – kein großer Knopf (Nutzer 2026-10-24); das
 * „+“ im Kopf bleibt der zweite Weg und mit Kreationen der einzige.
 */
@Composable
private fun EmptyCreationsCard(onNewPattern: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            HintText(stringResource(R.string.own_empty))
            // Bündig: Das Polster des Text-Buttons ragt in den Rand.
            AppTextButton(onClick = onNewPattern, modifier = Modifier.flushStart(inCard = true)) {
                ForwardLinkText(text = stringResource(R.string.own_design_pattern), color = MaterialTheme.colorScheme.secondary)
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
    expanded: Boolean?,
    onExpandedChange: (Boolean) -> Unit,
    status: String?,
    statusDescription: String,
    trailingAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    CollapsibleSection(
        title = title,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        status = status,
        statusDescription = statusDescription.takeIf { status != null },
        spacingAbove = Dimens.ListGap,
        trailingAction = trailingAction,
        content = content,
    )
}

/**
 * Karte eines gespeicherten Musters: aufgebaut wie das Muster des Tages ([PatternCard]), in der
 * Ecke zwei Aktionen nebeneinander (KOMP-02, Ausnahme Kreationen-Karte, Nutzer 2026-10-24): links
 * der Stift („Muster <Name> bearbeiten“, öffnet den Editor für dieses Muster wie „Anpassen“ auf
 * seiner Detailseite), rechts Löschen – zweistufig, ein Fehltipp löscht nichts (MUSTER-05). Beide
 * `AppIconButton`-groß in `onSurface` `SECONDARY`; die Karte wird nicht höher, der Text hält
 * rechts die Breite beider Tippflächen frei. Ein Tipp auf die Karte öffnet weiter die Detailseite.
 */
@Composable
private fun SavedPatternCard(
    pattern: SavedPattern,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val name = pattern.exercise.name
    PatternCard(exercise = pattern.exercise, patternLine = pattern.patternLine, onClick = onClick, actionCount = 2) {
        Row {
            AppIconButton(onClick = onEdit) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.cd_pattern_edit_named, name),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                )
            }
            ConfirmIconButton(
                name = name,
                description = stringResource(R.string.cd_pattern_delete, name),
                onConfirm = onDelete,
            )
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
