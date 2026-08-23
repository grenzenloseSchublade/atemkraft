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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.SituationRecommendation
import app.atemkraft.ui.components.SectionHeader
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
    resolve: (String) -> Exercise?,
    onSelect: (String) -> Unit,
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
