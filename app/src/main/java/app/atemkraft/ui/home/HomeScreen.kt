package app.atemkraft.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.domain.BreathingFamily
import app.atemkraft.domain.DailyPattern
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.defaultMinutes
import app.atemkraft.ui.components.AppIconButton
import app.atemkraft.ui.components.AppIconToggle
import app.atemkraft.ui.components.InfoChip
import app.atemkraft.ui.components.ScreenHeader
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.components.SettingsAction
import app.atemkraft.ui.components.TagChip
import app.atemkraft.ui.components.TitleWithChips
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.NeonCyan
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.WarnAmber

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
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.ScreenPadding),
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
                    color = NeonCyan,
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
                    ExerciseCard(exercise = exercise, onClick = { onSelect(exercise.id) })
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
                    ExerciseCard(exercise = program, onClick = { onSelect(program.id) })
                }
            }

            item { Spacer(Modifier.height(Dimens.ScreenBottom)) }
        }
    }
}

@Composable
fun ExerciseCard(exercise: Exercise, onClick: () -> Unit) {
    val accent = exercise.family.color()
    // Card(onClick) statt Modifier.clickable: Ripple folgt der Kartenform, Tippfläche ≥ 48 dp
    // bringt die M3-Surface selbst mit (KOMP-02).
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    ) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            TitleWithChips(
                title = exercise.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                chips = if (exercise.tag == null && exercise.guided) {
                    null
                } else {
                    {
                        exercise.tag?.let { TagChip(it) }
                        if (!exercise.guided) InfoChip()
                    }
                },
            )
            Spacer(Modifier.height(Dimens.GapTiny))
            Text(
                text = exercise.shortDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
            )
        }
    }
}

/**
 * Karte für das generierte „Muster des Tages": Name, Charakter-Chip, Muster + Dauer und als
 * einzige weitere Aktion das Lesezeichen (Speichern ↔ wieder entfernen).
 */
@Composable
private fun DailyPatternCard(
    daily: DailyPattern,
    saved: Boolean,
    onClick: () -> Unit,
    onSavedChange: (Boolean) -> Unit,
) {
    val exercise = daily.exercise
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f)),
    ) {
        // Lesezeichen als Overlay in der Kartenecke statt in der Titelzeile: Die volle
        // 48-dp-Tippfläche liegt über dem Karten-Padding, die Karte wird nicht höher als ihr
        // Text. Der Text hält rechts die Breite der Tippfläche frei, damit nichts darunter liegt.
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
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                ) {
                    CharacterChip(activating = daily.activating)
                }
                Spacer(Modifier.height(Dimens.GapTiny))
                Text(
                    text = RandomPatternGenerator.hintFor(daily.spec) + RandomPatternGenerator.HINT_SEPARATOR +
                        stringResource(R.string.duration_approx, exercise.defaultMinutes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                )
            }
            // Gespeicherte Muster erscheinen im Situationen-Tab unter „Meine Muster“ – der
            // Atmen-Tab bleibt schlank. Gefüllt im Akzent = gespeichert (Zustandsakzent, KOMP-02);
            // erneutes Tippen nimmt das Muster wieder heraus.
            AppIconToggle(
                checked = saved,
                onCheckedChange = onSavedChange,
                stateDescription = stringResource(if (saved) R.string.state_saved else R.string.state_not_saved),
                modifier = Modifier.align(Alignment.TopEnd),
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
}

/** Kleiner Charakter-Chip: „ruhig" (cyan) bzw. „sanft aktivierend" (amber). */
@Composable
private fun CharacterChip(activating: Boolean) {
    val color = if (activating) WarnAmber else NeonCyan
    Surface(
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = stringResource(
                if (activating) R.string.daily_pattern_gentle_up else R.string.daily_pattern_calm,
            ),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = Dimens.GapSmall, vertical = Dimens.GapTiny),
        )
    }
}
