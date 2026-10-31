package app.atemkraft.ui.own

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.data.SavedPattern
import app.atemkraft.domain.Exercise
import app.atemkraft.ui.components.AppIconButton
import app.atemkraft.ui.components.ConfirmIconButton
import app.atemkraft.ui.components.ExerciseRow
import app.atemkraft.ui.components.ExerciseRows
import app.atemkraft.ui.components.HintText
import app.atemkraft.ui.components.PersonalMarkIndicator
import app.atemkraft.ui.components.PushScaffold
import app.atemkraft.ui.theme.SECONDARY

/**
 * Alle eigenen Kreationen als Liste (Nutzer 2026-10-31; aus dem Tab „Eigene“ über „Alle 4 ›“ oder
 * die letzte Karteikarte): Push-Seite „Eigene Kreationen“ mit „+“ im Kopf (Neues Muster), darunter
 * eine Karte mit je einer [ExerciseRow] pro Muster, neueste zuerst – Mini-Kurve, Name, Dauer, am
 * Ende Stift und Löschen (zweistufig, MUSTER-05). Ein Tipp auf die Zeile öffnet die Detailseite.
 * Ist die Sammlung leer (alles gelöscht), steht nur der Satz des Leerzustands.
 */
@Composable
fun OwnCreationsScreen(
    patterns: List<SavedPattern>,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onNewPattern: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PushScaffold(
        onBack = onBack,
        modifier = modifier,
        title = stringResource(R.string.own_creations),
        action = {
            AppIconButton(onClick = onNewPattern) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.own_new_pattern), tint = MaterialTheme.colorScheme.onSurface)
            }
        },
    ) {
        if (patterns.isEmpty()) {
            HintText(stringResource(R.string.own_empty))
            return@PushScaffold
        }
        ExerciseRows(
            rows = patterns.map { pattern ->
                {
                    // Je Muster ein eigener Zustand des zweistufigen Löschens, auch wenn die Liste sich ändert.
                    key(pattern.id) {
                        val name = pattern.exercise.name
                        ExerciseRow(exercise = pattern.exercise, onClick = { onSelect(pattern.exercise.id) }) {
                            Row {
                                AppIconButton(onClick = { onEdit(pattern.id) }) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = stringResource(R.string.cd_pattern_edit_named, name),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                                    )
                                }
                                ConfirmIconButton(name = name, description = stringResource(R.string.cd_pattern_delete, name), onConfirm = { onDelete(pattern.id) })
                            }
                        }
                    }
                }
            },
        )
    }
}

/**
 * Alle Übungen aus „Tut mir gut“ als Liste (Nutzer 2026-10-31): Push-Seite „Tut mir gut“, je
 * markierter Übung eine [ExerciseRow] mit ◉ am Ende; ein Tipp öffnet die Detailseite (dort hebt
 * ◉ die Markierung auf). Leer: ein Satz, wie man markiert.
 */
@Composable
fun OwnFavoritesScreen(favorites: List<Exercise>, onBack: () -> Unit, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    PushScaffold(onBack = onBack, modifier = modifier, title = stringResource(R.string.own_favorites)) {
        if (favorites.isEmpty()) {
            HintText(stringResource(R.string.own_favorites_empty))
            return@PushScaffold
        }
        ExerciseRows(
            rows = favorites.map { exercise ->
                { ExerciseRow(exercise = exercise, onClick = { onSelect(exercise.id) }) { PersonalMarkIndicator() } }
            },
        )
    }
}
