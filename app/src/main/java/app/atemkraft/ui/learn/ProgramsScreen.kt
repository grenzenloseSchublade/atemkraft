package app.atemkraft.ui.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.data.BuiltInExercises
import app.atemkraft.domain.Exercise
import app.atemkraft.ui.components.ExerciseCard
import app.atemkraft.ui.components.PushScaffold
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens

/**
 * Seite „Programme & Gewohnheiten“ im Tab „Lernen“ (Nutzer 2026-10-23): die Infoseiten ohne
 * Startknopf (SKY, Buteyko, Nasenatmung) als [ExerciseCard], Karte ↔ Karte `ListGap`. Jede öffnet
 * ihre Detailseite ([onOpenExercise]) und zeigt die Markierung „Tut mir gut“ ([markedIds]). Ohne
 * Abschnittskopf: Die Seite hat nur diese eine Liste, ihr Titel steht im Kopf.
 */
@Composable
fun ProgramsScreen(
    programs: List<Exercise>,
    onBack: () -> Unit,
    onOpenExercise: (String) -> Unit,
    markedIds: Set<String> = emptySet(),
) {
    PushScaffold(onBack = onBack, title = stringResource(R.string.learn_programs)) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.ListGap)) {
            programs.forEach { program ->
                ExerciseCard(exercise = program, onClick = { onOpenExercise(program.id) }, marked = program.id in markedIds)
            }
        }
    }
}

@Preview
@Composable
private fun ProgramsScreenPreview() {
    AtemkraftTheme { ProgramsScreen(programs = BuiltInExercises.all.filter { !it.guided }, onBack = {}, onOpenExercise = {}) }
}
