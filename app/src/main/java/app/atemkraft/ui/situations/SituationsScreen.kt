package app.atemkraft.ui.situations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.SituationRecommendation
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.atemkraft.data.SavedPattern
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.defaultMinutes
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.theme.NeonCyan
import app.atemkraft.ui.home.ExerciseCard
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.WarnAmber

/**
 * Situationen-Tab: „welche Atmung wann". Gleiche Optik wie der Atmen-Tab (Abschnitts-Header +
 * Übungs-Karten), nur nach Lage gruppiert – mit Begründungssatz als Entscheidungshilfe.
 */
@Composable
fun SituationsScreen(
    recommendations: List<SituationRecommendation>,
    savedPatterns: List<SavedPattern>,
    resolve: (String) -> Exercise?,
    onSelect: (String) -> Unit,
    onDeleteSaved: (Long) -> Unit,
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
                Text(
                    text = stringResource(R.string.tab_situations),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.situations_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
                )
            }

            // Vom Nutzer gespeicherte generierte Muster – bewusst hier (nicht im Atmen-Tab).
            if (savedPatterns.isNotEmpty()) {
                item(key = "my-patterns-header") {
                    SectionHeader(
                        title = stringResource(R.string.situations_my_patterns),
                        color = NeonCyan,
                    )
                }
                items(savedPatterns, key = { "saved-" + it.id }) { pattern ->
                    SavedPatternCard(
                        pattern = pattern,
                        onClick = { onSelect(pattern.exercise.id) },
                        onDelete = { onDeleteSaved(pattern.id) },
                    )
                }
            }

            recommendations.forEach { rec ->
                item(key = "h-${rec.situation.name}") {
                    val accent = if (rec.warn) WarnAmber else MaterialTheme.colorScheme.primary
                    SectionHeader(title = rec.title, color = accent)
                    Text(
                        text = rec.rationale,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                items(
                    items = rec.exerciseIds.mapNotNull(resolve),
                    key = { "${rec.situation.name}-${it.id}" },
                ) { exercise ->
                    ExerciseCard(exercise = exercise, onClick = { onSelect(exercise.id) })
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

/** Karte eines gespeicherten Musters: Name, Muster-Zeile, Löschen. */
@Composable
private fun SavedPatternCard(
    pattern: SavedPattern,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val exercise = pattern.exercise
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                WholeWordText(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = exercise.instructionHint.orEmpty() + " · " +
                        stringResource(R.string.duration_approx, exercise.defaultMinutes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                )
            }
            val cd = stringResource(R.string.cd_pattern_delete)
            IconButton(onClick = onDelete, modifier = Modifier.semantics { contentDescription = cd }) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                )
            }
        }
    }
}
