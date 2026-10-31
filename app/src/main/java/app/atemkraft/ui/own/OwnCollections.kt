package app.atemkraft.ui.own

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.data.SavedPattern
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.OwnCollection
import app.atemkraft.domain.breathRhythm
import app.atemkraft.domain.defaultMinutes
import app.atemkraft.ui.components.AppIconButton
import app.atemkraft.ui.components.CardHeader
import app.atemkraft.ui.components.CardPager
import app.atemkraft.ui.components.CardTitle
import app.atemkraft.ui.components.CautionChip
import app.atemkraft.ui.components.CharacterChip
import app.atemkraft.ui.components.ConfirmIconButton
import app.atemkraft.ui.components.ExerciseCurve
import app.atemkraft.ui.components.HintText
import app.atemkraft.ui.components.PersonalMarkIndicator
import app.atemkraft.ui.components.RhythmCurve
import app.atemkraft.ui.components.SecondaryText
import app.atemkraft.ui.components.SegmentedChoiceRow
import app.atemkraft.ui.components.accentBorder
import app.atemkraft.ui.components.color
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes

/**
 * Sammlungen im Tab „Eigene“ (Nutzer 2026-10-31): oben ein Umschalter wie Tabs in der Seite
 * „Eigene Kreationen · 4 | Tut mir gut · 3“ ([SegmentedChoiceRow], Auswahl ohne Häkchen; die Wahl
 * bleibt, [selected]), darunter für die gewählte Sammlung Karteikarten ([CardPager]): eine große
 * Karte je Eintrag, seitlich wischen, die nächste ragt herein.
 *
 * Kreationen: vorn die Karte „Neues Muster“ (das Gestalten steht am Anfang), dahinter je
 * gespeichertem Muster eine Karte mit Stift und Löschen ([CreationCard]); ohne Kreationen trägt die
 * „+“-Karte den Satz des Leerzustands (MUSTER-04). „Tut mir gut“: je markierter Übung eine Karte
 * mit ◉ ([FavoriteCard]); ohne Markierung eine ruhige Karte mit dem Satz, wie man markiert.
 *
 * Unter jedem Pager „Alle 4 ›“ und als letzte Karte „Alle ansehen ›“: beide öffnen die ganze
 * Sammlung als Liste auf einer eigenen Seite ([OwnCreationsScreen], [OwnFavoritesScreen]).
 *
 * Je Sammlung ein eigener [PagerState] ([creationsPager], [favoritesPager]), vom Aufrufer gehalten:
 * Umschalten und Tab-Wechsel lassen die Lage stehen.
 */
@Composable
internal fun OwnCollections(
    selected: OwnCollection,
    onSelect: (OwnCollection) -> Unit,
    patterns: List<SavedPattern>,
    favorites: List<Exercise>,
    creationsPager: PagerState,
    favoritesPager: PagerState,
    actions: OwnActions,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.HeaderGap)) {
        val creations = collectionLabel(stringResource(R.string.own_creations), patterns.size)
        val favoritesLabel = collectionLabel(stringResource(R.string.own_favorites), favorites.size)
        SegmentedChoiceRow(items = OwnCollection.entries, selected = selected, onSelect = onSelect) { collection ->
            if (collection == OwnCollection.CREATIONS) creations else favoritesLabel
        }
        when (selected) {
            OwnCollection.CREATIONS -> CardPager(
                pageCount = patterns.size + 1,
                state = creationsPager,
                key = { index -> if (index == 0) NEW_PATTERN_KEY else patterns[index - 1].id },
                allLabel = stringResource(R.string.own_all_count, patterns.size),
                onAll = actions.onOpenAllCreations.takeIf { patterns.isNotEmpty() },
            ) { index ->
                if (index == 0) {
                    NewPatternCard(onClick = actions.onNewPattern, empty = patterns.isEmpty())
                } else {
                    val pattern = patterns[index - 1]
                    CreationCard(
                        pattern = pattern,
                        onClick = { actions.onSelect(pattern.exercise.id) },
                        onEdit = { actions.onEditPattern(pattern.id) },
                        onDelete = { actions.onDeletePattern(pattern.id) },
                    )
                }
            }

            OwnCollection.FAVORITES -> if (favorites.isEmpty()) {
                QuietCard { HintText(stringResource(R.string.own_favorites_empty)) }
            } else {
                CardPager(
                    pageCount = favorites.size,
                    state = favoritesPager,
                    key = { favorites[it].id },
                    allLabel = stringResource(R.string.own_all_count, favorites.size),
                    onAll = actions.onOpenAllFavorites,
                ) { index ->
                    val exercise = favorites[index]
                    FavoriteCard(exercise = exercise, onClick = { actions.onSelect(exercise.id) })
                }
            }
        }
    }
}

/** Listen-Schlüssel der Karte „Neues Muster“ (die Muster tragen ihre Datenbank-Id). */
private const val NEW_PATTERN_KEY = "new-pattern"

/** „Eigene Kreationen · 4“ – der Punkt hängt geschützt am Titel (TEXT-10); ohne Einträge nur der Titel. */
@Composable
private fun collectionLabel(title: String, count: Int): String = if (count > 0) title + " " + stringResource(R.string.situations_my_patterns_count, count) else title

/**
 * Karte „Neues Muster“: „+“ und Titel mittig auf ruhiger Fläche (`surfaceContainerLow`, ohne Rand –
 * eine Einladung, kein Hauptknopf, MUSTER-04); mit [empty] darunter der Satz des Leerzustands.
 */
@Composable
private fun NewPatternCard(onClick: () -> Unit, empty: Boolean) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Dimens.CardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null, // dekorativ: Die Karte heißt „Neues Muster“.
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(Sizes.IconDefault),
            )
            Spacer(Modifier.height(Dimens.GapSmall))
            CardTitle(stringResource(R.string.own_new_pattern))
            if (empty) {
                Spacer(Modifier.height(Dimens.GapTiny))
                HintText(stringResource(R.string.own_empty))
            }
        }
    }
}

/**
 * Karte eines gespeicherten Musters: Titelzeile ([CardHeader]) mit Charakter-Chip, Stift (Editor
 * für dieses Muster) und Löschen (zweistufig, MUSTER-05, außen); darunter die Kurve eines Atemzugs
 * mit ihrer Phasenzeile ([RhythmCurve], ruhend – die Karte zeigt die Form, kein Takt), unten
 * „ca. 5 min“. Ein Tipp auf die Karte öffnet die Detailseite. Rand `secondary` wie das Muster des
 * Tages.
 */
@Composable
private fun CreationCard(pattern: SavedPattern, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val exercise = pattern.exercise
    val name = exercise.name
    Card(onClick = onClick, modifier = Modifier.fillMaxSize(), border = accentBorder(MaterialTheme.colorScheme.secondary)) {
        Column(modifier = Modifier.fillMaxSize().padding(Dimens.CardPadding)) {
            CardHeader(
                title = name,
                chips = { CharacterChip(intense = exercise.caution, activating = exercise.activating) },
                actionCount = 2,
            ) {
                Row {
                    AppIconButton(onClick = onEdit) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.cd_pattern_edit_named, name),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                        )
                    }
                    ConfirmIconButton(name = name, description = stringResource(R.string.cd_pattern_delete, name), onConfirm = onDelete)
                }
            }
            CardBody(exercise)
        }
    }
}

/**
 * Karte einer Übung aus „Tut mir gut“: Titelzeile mit ⚠-Chip bei Vorsicht und ◉ als Zeichen
 * ([PersonalMarkIndicator]), darunter Kurve und Dauer wie bei den Kreationen; Rand in der
 * Familienfarbe wie jede Übungskarte. Ein Tipp öffnet die Detailseite.
 */
@Composable
private fun FavoriteCard(exercise: Exercise, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxSize(), border = accentBorder(exercise.family.color())) {
        Column(modifier = Modifier.fillMaxSize().padding(Dimens.CardPadding)) {
            CardHeader(
                title = exercise.name,
                chips = if (exercise.caution) ({ CautionChip() }) else null,
                indicator = { PersonalMarkIndicator() },
            )
            CardBody(exercise)
        }
    }
}

/**
 * Inhalt unter der Titelzeile: die Kurve eines Atemzugs samt Phasenzeile ([RhythmCurve] ohne
 * laufenden Punkt; ohne Takt die [ExerciseCurve] mit dem Familienpunkt), unten „ca. 10 min“.
 * Titelzeile, Grafik und Text sind drei Blöcke: zwischen ihnen `SectionGap` (Eigentümer
 * 2026-10-31: „die Abstände sind sehr gering“), Phasenzeile → Dauer `GapTiny` wie Titel → Metazeile.
 */
@Composable
private fun ColumnScope.CardBody(exercise: Exercise) {
    val rhythm = remember(exercise) { exercise.breathRhythm() }
    Spacer(Modifier.height(Dimens.SectionGap))
    if (rhythm != null) {
        RhythmCurve(rhythm = rhythm, running = false, lineGap = Dimens.SectionGap)
        Spacer(Modifier.height(Dimens.GapTiny))
    } else {
        ExerciseCurve(exercise)
        Spacer(Modifier.height(Dimens.SectionGap))
    }
    SecondaryText(stringResource(R.string.duration_approx, exercise.defaultMinutes()))
}

/** Ruhige Fläche ohne Aktion (Leerzustand „Tut mir gut“), gebaut wie die Karte „Neues Muster“. */
@Composable
private fun QuietCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) { content() }
    }
}
