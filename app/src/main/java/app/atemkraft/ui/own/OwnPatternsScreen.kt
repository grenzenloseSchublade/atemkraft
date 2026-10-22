package app.atemkraft.ui.own

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.data.SavedPattern
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.ui.components.AppOutlinedButton
import app.atemkraft.ui.components.ConfirmIconButton
import app.atemkraft.ui.components.HintText
import app.atemkraft.ui.components.ScreenHeader
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.components.SettingsAction
import app.atemkraft.ui.components.headingColor
import app.atemkraft.ui.home.PatternCard
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Sizes

/**
 * Tab „Eigene“: oben „Neues Muster“ (öffnet den Muster-Editor), darunter „Meine Muster“ –
 * gespeicherte Tagesmuster und selbst zusammengestellte Muster, neueste zuerst wie aus der
 * Datenbank. Die Liste steht immer offen: Hier ist sie der Inhalt des Tabs, nicht ein Abschnitt
 * neben den Situationen. Ohne Muster steht ein kurzer Satz da, darunter derselbe Knopf.
 *
 * Löschen bleibt zweistufig in der Kartenecke (MUSTER-05). Der Kopf gleicht den anderen Tabs:
 * Titel, Untertitel, Zahnrad zu den Einstellungen (den Kalender zum Logbuch trägt nur Atmen).
 */
@Composable
fun OwnPatternsScreen(
    savedPatterns: List<SavedPattern>,
    onNewPattern: () -> Unit,
    onSelect: (String) -> Unit,
    onDeletePattern: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // Rand als contentPadding, nicht als Modifier: Die Liste reicht bis an den Bildrand, und
            // bündige Icon-Buttons (flushEnd) ragen mit ihrer Tippfläche in den Rand, ohne dass der
            // Listen-Clip sie anschneidet.
            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ListGap),
        ) {
            item {
                ScreenHeader(
                    title = stringResource(R.string.own_title),
                    subtitle = stringResource(R.string.own_subtitle),
                    action = { SettingsAction(onClick = onOpenSettings) },
                )
            }

            if (savedPatterns.isEmpty()) {
                item(key = "empty") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(Modifier.height(Dimens.SectionGap))
                        HintText(stringResource(R.string.own_empty))
                        Spacer(Modifier.height(Dimens.GapSmall))
                        NewPatternButton(onClick = onNewPattern)
                    }
                }
            } else {
                item(key = "new-pattern") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(Modifier.height(Dimens.SectionGap))
                        NewPatternButton(onClick = onNewPattern)
                    }
                }
                // Der einzige Abschnittskopf des Tabs, also der erste im Ring (FARBE-07).
                item(key = "my-patterns") {
                    SectionHeader(title = stringResource(R.string.situations_my_patterns), color = headingColor(0))
                }
                items(savedPatterns, key = { "pattern-${it.id}" }) { pattern ->
                    SavedPatternCard(
                        pattern = pattern,
                        onClick = { onSelect(pattern.exercise.id) },
                        onDelete = { onDeletePattern(pattern.id) },
                    )
                }
            }

            item { Spacer(Modifier.height(Dimens.ScreenBottom)) }
        }
    }
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
