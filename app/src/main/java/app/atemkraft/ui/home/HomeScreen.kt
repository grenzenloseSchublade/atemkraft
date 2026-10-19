package app.atemkraft.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.domain.BreathingFamily
import app.atemkraft.domain.DOT_SEPARATOR
import app.atemkraft.domain.DailyPattern
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.PatternSpec
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.defaultMinutes
import app.atemkraft.ui.components.AppIconButton
import app.atemkraft.ui.components.AppIconToggle
import app.atemkraft.ui.components.CautionChip
import app.atemkraft.ui.components.CharacterChip
import app.atemkraft.ui.components.InfoChip
import app.atemkraft.ui.components.PersonalMarkIndicator
import app.atemkraft.ui.components.ScreenHeader
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.components.SettingsAction
import app.atemkraft.ui.components.TitleWithChips
import app.atemkraft.ui.components.accentBorder
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY

/** Atmen-Tab: startbare Übungen nach Familien + Abschnitt „Programme & Wissen". */
@Composable
fun HomeScreen(
    exercisesByFamily: List<Pair<BreathingFamily, List<Exercise>>>,
    programs: List<Exercise>,
    daily: DailyPattern,
    dailySaved: Boolean,
    onRegenerateDaily: () -> Unit,
    onDailySavedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    onOpenSettings: () -> Unit,
    markedIds: Set<String> = emptySet(),
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
                    title = stringResource(R.string.tab_breathe),
                    subtitle = stringResource(R.string.home_subtitle),
                    action = { SettingsAction(onClick = onOpenSettings) },
                )
            }

            item(key = "daily-pattern") {
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
                DailyPatternCard(
                    daily = daily,
                    saved = dailySaved,
                    onClick = { onSelect(daily.exercise.id) },
                    onSavedChange = onDailySavedChange,
                )
            }

            exercisesByFamily.forEach { (family, exercises) ->
                item(key = "header-${family.name}") {
                    SectionHeader(title = family.title(), color = family.color())
                }
                items(exercises, key = { it.id }) { exercise ->
                    ExerciseCard(exercise = exercise, onClick = { onSelect(exercise.id) }, marked = exercise.id in markedIds)
                }
            }

            if (programs.isNotEmpty()) {
                item(key = "header-programs") {
                    SectionHeader(
                        title = stringResource(R.string.home_programs),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
                    )
                }
                items(programs, key = { it.id }) { program ->
                    ExerciseCard(exercise = program, onClick = { onSelect(program.id) }, marked = program.id in markedIds)
                }
            }

            item { Spacer(Modifier.height(Dimens.ScreenBottom)) }
        }
    }
}

/**
 * Übungskarte in Listen. Die Studienlage steht nur auf der Detailseite: Auf der Liste trüge fast
 * jede Karte dasselbe Etikett. Die Karte zeigt, was vor dem Antippen zählt – Vorsicht, „Programm“
 * und die persönliche Markierung ([marked], Atemkreis rechts auf Höhe der ersten Titelzeile).
 */
@Composable
fun ExerciseCard(exercise: Exercise, marked: Boolean = false, onClick: () -> Unit) {
    val accent = exercise.family.color()
    val titleStyle = MaterialTheme.typography.titleMedium
    // Card(onClick) statt Modifier.clickable: Ripple folgt der Kartenform, Tippfläche ≥ 48 dp
    // bringt die M3-Surface selbst mit (KOMP-02).
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = accentBorder(accent),
    ) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
                TitleWithChips(
                    title = exercise.name,
                    style = titleStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    chips = if (!exercise.caution && exercise.guided) {
                        null
                    } else {
                        {
                            if (exercise.caution) CautionChip()
                            if (!exercise.guided) InfoChip()
                        }
                    },
                )
                if (marked) {
                    // Box in Höhe einer Titelzeile: Der Kreis steht mittig zur ersten Zeile,
                    // auch wenn Titel und Chips umbrechen.
                    Box(
                        modifier = Modifier.height(with(LocalDensity.current) { titleStyle.lineHeight.toDp() }),
                        contentAlignment = Alignment.Center,
                    ) { PersonalMarkIndicator() }
                }
            }
            Spacer(Modifier.height(Dimens.GapTiny))
            Text(
                text = exercise.shortDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
            )
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
    PatternCard(exercise = daily.exercise, spec = daily.spec, onClick = onClick) {
        // Gespeicherte Muster erscheinen im Situationen-Tab unter „Meine Muster“ – der
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

/**
 * Karte eines generierten Musters – Muster des Tages und „Meine Muster“ sehen gleich aus: Name
 * mit Charakter-Chip, darunter die kurze Musterzeile mit Dauer, Rand in Cyan (eigene Muster).
 * [action] ist die eine weitere Aktion (KOMP-02) und liegt als Overlay in der Kartenecke statt
 * in der Titelzeile: Die volle 48-dp-Tippfläche liegt über dem Karten-Padding, die Karte wird
 * nicht höher als ihr Text. Der Text hält rechts die Breite der Tippfläche frei, damit nichts
 * darunter liegt.
 */
@Composable
fun PatternCard(
    exercise: Exercise,
    spec: PatternSpec,
    onClick: () -> Unit,
    action: @Composable () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = accentBorder(MaterialTheme.colorScheme.secondary),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(
                    start = Dimens.CardPadding,
                    top = Dimens.CardPadding,
                    end = Dimens.MinTouchTarget,
                    bottom = Dimens.CardPadding,
                ),
            ) {
                TitleWithChips(
                    title = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                ) {
                    CharacterChip(intense = false, activating = exercise.activating)
                }
                Spacer(Modifier.height(Dimens.GapTiny))
                // Kurze Musterzeile statt instructionHint: Die Einladung zur Bauchatmung steht
                // auf der Detailseite unter „Anleitung“, nicht in jeder Karte.
                Text(
                    text = RandomPatternGenerator.hintFor(spec) + DOT_SEPARATOR +
                        stringResource(R.string.duration_approx, exercise.defaultMinutes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                )
            }
            Box(modifier = Modifier.align(Alignment.TopEnd)) { action() }
        }
    }
}
