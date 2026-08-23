package app.atemkraft.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.domain.BreathingFamily
import app.atemkraft.domain.Exercise
import app.atemkraft.ui.components.InfoChip
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.components.TagChip
import app.atemkraft.ui.theme.SECONDARY

/** Atmen-Tab: startbare Übungen nach Familien + Abschnitt „Programme & Wissen". */
@Composable
fun HomeScreen(
    exercisesByFamily: List<Pair<BreathingFamily, List<Exercise>>>,
    programs: List<Exercise>,
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
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.tab_breathe),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = stringResource(R.string.home_subtitle),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
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

            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun ExerciseCard(exercise: Exercise, onClick: () -> Unit) {
    val accent = exercise.family.color()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    ) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                )
                exercise.tag?.let { TagChip(it) }
                if (!exercise.guided) InfoChip()
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = exercise.shortDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
            )
        }
    }
}
