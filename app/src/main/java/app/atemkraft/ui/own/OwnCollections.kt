package app.atemkraft.ui.own

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import app.atemkraft.R
import app.atemkraft.data.SavedPattern
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.OwnCollection
import app.atemkraft.domain.breathRhythm
import app.atemkraft.domain.defaultMinutes
import app.atemkraft.ui.components.AppIconButton
import app.atemkraft.ui.components.CardHeader
import app.atemkraft.ui.components.CardPager
import app.atemkraft.ui.components.CardSet
import app.atemkraft.ui.components.CautionChip
import app.atemkraft.ui.components.CharacterChip
import app.atemkraft.ui.components.ConfirmIconButton
import app.atemkraft.ui.components.ExerciseCurve
import app.atemkraft.ui.components.HintText
import app.atemkraft.ui.components.PageTabs
import app.atemkraft.ui.components.PersonalMarkIndicator
import app.atemkraft.ui.components.RhythmCurve
import app.atemkraft.ui.components.SecondaryText
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.components.accentBorder
import app.atemkraft.ui.components.color
import app.atemkraft.ui.components.rememberFirstLineHeight
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes

/**
 * Sammlungen im Tab „Meins“ (Nutzer 2026-10-31): oben Tabs in der Seite „Eigene Kreationen 4 ·
 * Tut mir gut 3“ ([PageTabs]: der gewählte pink mit Strich, die Zahl leise; die Wahl bleibt,
 * [selected]), darunter für die gewählte Sammlung Karteikarten ([CardPager]): eine Karte je Eintrag,
 * seitlich wischen, jede rastet an der Textkante ein, die nächste ragt herein.
 *
 * Kreationen: ganz links die schmale Kachel „+ Neu“ ([NewPatternTile], Eigentümer 2026-10-31: nicht
 * als erste volle Karte), daneben vollständig die erste Kreation, je gespeichertem Muster eine Karte
 * mit Stift und Löschen ([CreationCard]); ohne Kreationen steht neben der Kachel eine ruhige Karte mit
 * dem Satz des Leerzustands (MUSTER-04). „Tut mir gut“: je markierter Übung eine Karte mit ◉
 * ([FavoriteCard]), gleich breit wie die Kreationen; ohne Markierung eine ruhige Karte mit dem Satz,
 * wie man markiert.
 *
 * Unter jeder Reihe „Alle 4 ›“ und als letzte Karte „Alle ansehen ›“: beide öffnen die ganze
 * Sammlung als Liste auf einer eigenen Seite ([OwnCreationsScreen], [OwnFavoritesScreen]).
 *
 * Je Sammlung ein eigener [LazyListState] ([creationsRow], [favoritesRow]), vom Aufrufer gehalten:
 * Umschalten und Tab-Wechsel lassen die Lage stehen.
 */
@Composable
internal fun OwnCollections(
    selected: OwnCollection,
    onSelect: (OwnCollection) -> Unit,
    patterns: List<SavedPattern>,
    favorites: List<Exercise>,
    creationsRow: LazyListState,
    favoritesRow: LazyListState,
    actions: OwnActions,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.HeaderGap)) {
        val creationsName = stringResource(R.string.own_creations)
        val favoritesName = stringResource(R.string.own_favorites)
        PageTabs(
            items = OwnCollection.entries,
            selected = selected,
            onSelect = onSelect,
            label = { collection -> if (collection == OwnCollection.CREATIONS) creationsName else favoritesName },
            counts = { collection ->
                (if (collection == OwnCollection.CREATIONS) patterns.size else favorites.size).takeIf { it > 0 }
            },
        )
        // Beide Sammlungen stehen gleich hoch: Jede Reihe misst die Karten der anderen mit.
        val creationCards = CardSet(patterns.size) { index -> CreationCard(patterns[index], onClick = {}, onEdit = {}, onDelete = {}) }
        val favoriteCards = CardSet(favorites.size) { index -> FavoriteCard(favorites[index], onClick = {}) }
        when (selected) {
            OwnCollection.CREATIONS -> CardPager(
                pageCount = patterns.size.coerceAtLeast(1),
                state = creationsRow,
                key = { index -> if (patterns.isEmpty()) EMPTY_KEY else patterns[index].id },
                allLabel = stringResource(R.string.own_all_count, patterns.size),
                onAll = actions.onOpenAllCreations.takeIf { patterns.isNotEmpty() },
                sameHeightAs = favoriteCards,
                leading = { NewPatternTile(onClick = actions.onNewPattern) },
            ) { index ->
                if (patterns.isEmpty()) {
                    QuietCard(fill = true) { HintText(stringResource(R.string.own_empty)) }
                } else {
                    val pattern = patterns[index]
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
                    state = favoritesRow,
                    key = { favorites[it].id },
                    allLabel = stringResource(R.string.own_all_count, favorites.size),
                    onAll = actions.onOpenAllFavorites,
                    sameHeightAs = creationCards,
                ) { index ->
                    val exercise = favorites[index]
                    FavoriteCard(exercise = exercise, onClick = { actions.onSelect(exercise.id) })
                }
            }
        }
    }
}

/** Listen-Schlüssel der ruhigen Karte ohne Kreationen (die Muster tragen ihre Datenbank-Id). */
private const val EMPTY_KEY = "empty"

/**
 * Schmale Kachel „+ Neu“ links vor den Kreationen (Eigentümer 2026-10-31): „+“ und das Wort „Neu“
 * mittig auf ruhiger Fläche (`surfaceContainerLow`, ohne Rand – eine Einladung, kein Hauptknopf,
 * MUSTER-04), so hoch wie die Karten. TalkBack hört „Neues Muster“ (das kurze Wort ist dort stumm);
 * ein Tipp öffnet den Muster-Editor. Rand innen `GapSmall`, damit „Neu“ auch bei Schrift 2,0 in die
 * Kachel passt.
 */
@Composable
private fun NewPatternTile(onClick: () -> Unit) {
    val name = stringResource(R.string.own_new_pattern)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxSize().semantics { contentDescription = name },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Dimens.GapSmall),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null, // dekorativ: Die Kachel heißt „Neues Muster“.
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(Sizes.IconDefault),
            )
            Spacer(Modifier.height(Dimens.GapSmall))
            WholeWordText(
                text = stringResource(R.string.own_new_short),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { hideFromAccessibility() },
            )
        }
    }
}

/**
 * Karte eines gespeicherten Musters: Titelzeile ([CardHeader]) mit Stift (Editor für dieses
 * Muster) und Löschen (zweistufig, MUSTER-05, außen); darunter die Kurve eines Atemzugs mit ihrer
 * Phasenzeile ([RhythmCurve], ruhend – die Karte zeigt die Form, kein Takt), unten „ca. 5 min“ und
 * der Charakter-Chip. Ein Tipp auf die Karte öffnet die Detailseite. Rand `secondary` wie das Muster des
 * Tages.
 */
@Composable
private fun CreationCard(pattern: SavedPattern, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val exercise = pattern.exercise
    val name = exercise.name
    Card(onClick = onClick, modifier = Modifier.fillMaxSize(), border = accentBorder(MaterialTheme.colorScheme.secondary)) {
        Column(modifier = Modifier.fillMaxSize().padding(Dimens.CardPadding)) {
            // Reserve als Hülle, nicht als Modifier des Kopfs: Eine Mindesthöhe im Kopf drückte die Chips zusammen.
            Box(Modifier.heightIn(min = titleReserve())) {
                CardHeader(
                    title = name,
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
            }
            CardBody(exercise) { CharacterChip(intense = exercise.caution, activating = exercise.activating) }
        }
    }
}

/**
 * Karte einer Übung aus „Tut mir gut“: Titelzeile mit ◉ als Zeichen ([PersonalMarkIndicator]),
 * darunter Kurve, Dauer und bei Vorsicht der ⚠-Chip wie bei den Kreationen; Rand in der
 * Familienfarbe wie jede Übungskarte. Ein Tipp öffnet die Detailseite.
 */
@Composable
private fun FavoriteCard(exercise: Exercise, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxSize(), border = accentBorder(exercise.family.color())) {
        Column(modifier = Modifier.fillMaxSize().padding(Dimens.CardPadding)) {
            Box(Modifier.heightIn(min = titleReserve())) {
                CardHeader(
                    title = exercise.name,
                    indicator = { PersonalMarkIndicator() },
                )
            }
            CardBody(exercise, chips = if (exercise.caution) ({ CautionChip() }) else null)
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
private fun ColumnScope.CardBody(exercise: Exercise, chips: (@Composable () -> Unit)? = null) {
    val rhythm = remember(exercise) { exercise.breathRhythm() }
    Spacer(Modifier.height(Dimens.SectionGap))
    if (rhythm != null) {
        RhythmCurve(rhythm = rhythm, running = false, lineGap = Dimens.SectionGap, lineReserve = RESERVED_LINES)
        Spacer(Modifier.height(Dimens.GapTiny))
    } else {
        ExerciseCurve(exercise)
        Spacer(Modifier.height(Dimens.SectionGap))
    }
    // Chips unten in voller Kartenbreite, nicht im Kopf: Neben Stift und Löschen blieb bei großer
    // Schrift zu wenig Platz, „sanft aktivierend“ brach um (LAYOUT-03, 2026-10-31).
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
        verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        SecondaryText(stringResource(R.string.duration_approx, exercise.defaultMinutes()))
        chips?.invoke()
    }
}

/**
 * Feste Plätze der Karteikarte (Eigentümer 2026-10-31): Titelzeile und Phasenzeile halten je
 * [RESERVED_LINES] Zeilen frei, so steht die Kurve in jeder Karte an derselben Stelle, kurzer oder
 * langer Name. Längeres wächst trotzdem (kein Wort bricht, LAYOUT-03); die gleiche Kartenhöhe über
 * beide Sammlungen setzt dann der [CardPager].
 */
private const val RESERVED_LINES = 2

/** Höhe zweier Titelzeilen (`titleMedium` wie [CardTitle] im [CardHeader]), gemessen mit der aktuellen Schrift. */
@Composable
private fun titleReserve(): Dp = rememberFirstLineHeight(MaterialTheme.typography.titleMedium) * RESERVED_LINES

/** Ruhige Fläche ohne Aktion (Leerzustände; mit [fill] füllt sie ihre Karte in der Reihe), gebaut wie die Kachel „Neu“. */
@Composable
private fun QuietCard(fill: Boolean = false, content: @Composable () -> Unit) {
    Card(
        modifier = if (fill) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) { content() }
    }
}
