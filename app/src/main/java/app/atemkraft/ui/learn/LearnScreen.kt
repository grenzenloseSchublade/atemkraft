package app.atemkraft.ui.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.domain.Exercise
import app.atemkraft.ui.components.ExerciseCard
import app.atemkraft.ui.components.HintText
import app.atemkraft.ui.components.NavCard
import app.atemkraft.ui.components.ScreenHeader
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.components.TabListScaffold
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.components.headingColor
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY

/**
 * Tab „Lernen“ (Nutzer 2026-10-23): Yoga-Theorie und Praxis an einem Ort, ehrlich eingeordnet.
 * Drei Kapitel als Karten, jede öffnet ihre Seite: Atemlehre (wie die Lehre Ein, Aus und Halten
 * deutet), Pranayama (Grundlagen und Techniken, mit „Üben ›“ zur Übung) und Begriffe. Vorher
 * standen Atemlehre und Begriffe oben in den Einstellungen.
 *
 * Darunter der Abschnitt „Programme & Gewohnheiten“: Infoseiten ohne Startknopf (SKY, Buteyko,
 * Nasenatmung) als [ExerciseCard], sie öffnen die Detailseite ([onOpenExercise]). Seit sie hier
 * stehen, gilt: Atmen = alles, was man starten kann; Lernen = alles, was man liest.
 *
 * Kapiteltitel und Abschnittskopf sind die eine Kopf-Ebene des Screens und gehen reihum durch den
 * Kopf-Ring (FARBE-07): Atemlehre 0, Pranayama 1, Begriffe 2, Programme 3.
 */
@Composable
fun LearnScreen(
    onOpenAtemlehre: () -> Unit,
    onOpenPranayama: () -> Unit,
    onOpenGlossary: () -> Unit,
    programs: List<Exercise> = emptyList(),
    onOpenExercise: (String) -> Unit = {},
    markedIds: Set<String> = emptySet(),
) {
    // Der Kopf steht fest über der Liste; liegt Inhalt darunter, zeigt er eine Haarlinie.
    TabListScaffold(
        header = { scrolled -> ScreenHeader(title = stringResource(R.string.tab_learn), scrolled = scrolled) },
        subtitle = stringResource(R.string.learn_subtitle),
    ) {
        item(key = "atemlehre") {
            ChapterCard(
                title = stringResource(R.string.settings_atemlehre),
                hint = stringResource(R.string.learn_atemlehre_hint),
                heading = 0,
                onClick = onOpenAtemlehre,
            )
        }
        item(key = "pranayama") {
            ChapterCard(
                title = stringResource(R.string.pranayama_title),
                hint = stringResource(R.string.learn_pranayama_hint),
                heading = 1,
                onClick = onOpenPranayama,
            )
        }
        item(key = "glossary") {
            ChapterCard(
                title = stringResource(R.string.settings_glossary),
                hint = stringResource(R.string.learn_glossary_hint),
                heading = 2,
                onClick = onOpenGlossary,
            )
        }
        if (programs.isNotEmpty()) {
            item(key = "programs-header") {
                SectionHeader(title = stringResource(R.string.learn_programs), color = headingColor(PROGRAMS_HEADING))
            }
            items(programs, key = { it.id }) { program ->
                ExerciseCard(exercise = program, onClick = { onOpenExercise(program.id) }, marked = program.id in markedIds)
            }
        }
    }
}

/** Lage des Programm-Kopfs im Kopf-Ring: nach den drei Kapitelkarten (FARBE-07). */
private const val PROGRAMS_HEADING = 3

/**
 * Ein Kapitel als [NavCard]: Titel in der Farbe seiner Lage ([heading], FARBE-07), darunter ein
 * Satz, was drin steht, rechts der Chevron.
 */
@Composable
private fun ChapterCard(title: String, hint: String, heading: Int, onClick: () -> Unit) {
    NavCard(title = title, hint = hint, titleColor = headingColor(heading), onClick = onClick)
}

@Preview
@Composable
private fun LearnScreenPreview() {
    AtemkraftTheme { LearnScreen(onOpenAtemlehre = {}, onOpenPranayama = {}, onOpenGlossary = {}) }
}
