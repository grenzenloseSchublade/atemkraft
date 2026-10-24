package app.atemkraft.ui.learn

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.ui.components.NavCard
import app.atemkraft.ui.components.ScreenHeader
import app.atemkraft.ui.components.SettingsAction
import app.atemkraft.ui.components.TabListScaffold
import app.atemkraft.ui.components.headingColor
import app.atemkraft.ui.theme.AtemkraftTheme

/**
 * Tab „Lernen“ (Nutzer 2026-10-23): Yoga-Theorie und Praxis an einem Ort, ehrlich eingeordnet.
 * Sechs gleich gebaute Kapitel als Karten, jede öffnet ihre Seite: Atemlehre (wie die Lehre Ein,
 * Aus und Halten deutet), Pranayama (Grundlagen und Techniken, mit „Üben ›“ zur Übung), Programme
 * & Gewohnheiten (Infoseiten ohne Startknopf wie SKY, Buteyko, Nasenatmung, [ProgramsScreen]),
 * Begriffe, Geschichte des Atmens ([GeschichteScreen]) und Körper ([KoerperScreen], Eigentümer
 * 2026-10-25). Atmen = alles, was man starten kann; Lernen = alles, was man liest.
 *
 * Die Kapiteltitel sind die eine Kopf-Ebene des Screens und gehen reihum durch den Kopf-Ring
 * (FARBE-07): Atemlehre 0, Pranayama 1, Programme 2, Begriffe 3, Geschichte 4, Körper 5 – der Ring
 * hat fünf Töne, Körper beginnt ihn wieder (Pink wie Atemlehre, nicht benachbart). Rechts im Kopf
 * das Zahnrad wie in den anderen drei Tabs (Nutzer 2026-10-24: von hier gab es keinen Weg in die
 * Einstellungen).
 */
@Composable
fun LearnScreen(
    onOpenSettings: () -> Unit,
    onOpenAtemlehre: () -> Unit,
    onOpenPranayama: () -> Unit,
    onOpenPrograms: () -> Unit,
    onOpenGlossary: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenBody: () -> Unit,
) {
    // Der Kopf steht fest über der Liste; liegt Inhalt darunter, zeigt er eine Haarlinie.
    TabListScaffold(
        header = { scrolled ->
            ScreenHeader(
                title = stringResource(R.string.tab_learn),
                action = { SettingsAction(onClick = onOpenSettings) },
                scrolled = scrolled,
            )
        },
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
        item(key = "programs") {
            ChapterCard(
                title = stringResource(R.string.learn_programs),
                hint = stringResource(R.string.learn_programs_hint),
                heading = 2,
                onClick = onOpenPrograms,
            )
        }
        item(key = "glossary") {
            ChapterCard(
                title = stringResource(R.string.settings_glossary),
                hint = stringResource(R.string.learn_glossary_hint),
                heading = 3,
                onClick = onOpenGlossary,
            )
        }
        item(key = "history") {
            ChapterCard(
                title = stringResource(R.string.history_title),
                hint = stringResource(R.string.learn_history_hint),
                heading = 4,
                onClick = onOpenHistory,
            )
        }
        item(key = "body") {
            ChapterCard(
                title = stringResource(R.string.body_title),
                hint = stringResource(R.string.learn_body_hint),
                heading = 5,
                onClick = onOpenBody,
            )
        }
    }
}

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
    AtemkraftTheme { LearnScreen(onOpenSettings = {}, onOpenAtemlehre = {}, onOpenPranayama = {}, onOpenPrograms = {}, onOpenGlossary = {}, onOpenHistory = {}, onOpenBody = {}) }
}
