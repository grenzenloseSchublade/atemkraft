package app.atemkraft.ui.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import app.atemkraft.R
import app.atemkraft.data.Geschichte
import app.atemkraft.ui.components.Bullet
import app.atemkraft.ui.components.CollapsibleSection
import app.atemkraft.ui.components.HintText
import app.atemkraft.ui.components.Labeled
import app.atemkraft.ui.components.PushScaffold
import app.atemkraft.ui.components.ScreenSubtitle
import app.atemkraft.ui.components.sanskritText
import app.atemkraft.ui.theme.BODY
import app.atemkraft.ui.theme.Dimens

/**
 * Kapitel „Geschichte des Atmens“ im Tab „Lernen“ (Eigentümer 2026-10-25): wie der Atem zur Übung
 * wurde. Gebaut wie die Seite „Pranayama“: Einleitung, je Abschnitt ein fester Kopf mit Teaser
 * (Indien, Andere Wege, Der Westen, Heute) und darunter die Karten ([TopicCard], Zeitangabe über
 * dem Titel); in „Heute“ zuerst die Zeitleiste als schlichte Liste (Jahr fett, dann ein Halbsatz,
 * keine eigene Karte), darunter die Karte zum Alter des Yoga. Unten die Quellen: Literatur und
 * Studien. Köpfe weiß, Kartentitel neutral (FARBE-07).
 *
 * [expanded] öffnet alle Karten und die Quellen von Anfang an (Screenshot-Test).
 */
@Composable
fun GeschichteScreen(onBack: () -> Unit, onOpenExercise: (String) -> Unit, expanded: Boolean = false) {
    PushScaffold(onBack = onBack, title = stringResource(R.string.history_title)) {
        ScreenSubtitle(Geschichte.intro)
        Geschichte.sections.forEach { section ->
            CollapsibleSection(title = section.title, teaser = section.teaser) {
                section.topics.forEach { TopicCard(it, expanded, onOpenExercise) }
            }
        }
        CollapsibleSection(title = Geschichte.today.title, teaser = Geschichte.today.teaser) {
            Timeline()
            Geschichte.today.topics.forEach { TopicCard(it, expanded, onOpenExercise) }
        }
        LearnSources(
            teaser = Geschichte.sourcesTeaser,
            textsLabel = stringResource(R.string.learn_sources_literature),
            texts = Geschichte.textSources,
            studies = Geschichte.studyRefs,
            initiallyExpanded = expanded,
        )
    }
}

/**
 * Zeitleiste unter „Zeitleiste“: je Eintrag ein Punkt mit fettem Jahr und Halbsatz wie die
 * Grundregeln der Atemlehre, darunter der Nachsatz zu Mustern ohne sicheres Jahr.
 */
@Composable
private fun Timeline() {
    // Fett über das Gewicht einer Titel-Rolle (TYPO-01: kein eigenes FontWeight).
    val bold = SpanStyle(fontWeight = MaterialTheme.typography.titleSmall.fontWeight)
    Labeled(stringResource(R.string.history_timeline)) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny)) {
            Geschichte.timeline.forEach { event ->
                Bullet(
                    buildAnnotatedString {
                        withStyle(bold) { append(event.year) }
                        append(" ")
                        append(sanskritText(event.text))
                    },
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = BODY),
                )
            }
            HintText(Geschichte.timelineNote)
        }
    }
}
