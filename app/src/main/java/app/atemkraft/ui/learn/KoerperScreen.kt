package app.atemkraft.ui.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.data.Koerper
import app.atemkraft.ui.components.PushScaffold
import app.atemkraft.ui.components.ScreenSubtitle
import app.atemkraft.ui.theme.Dimens

/**
 * Kapitel „Körper“ im Tab „Lernen“ (Eigentümer 2026-10-25): was beim Atmen im Körper geschieht.
 * Einleitung, darunter die Karten ([TopicCard]) ohne Abschnittsköpfe – das Kapitel hat nur eine
 * Ebene –, als letzte „Grenzen einer App: wann Fachleute“ ([Koerper.limits]); unten die Quellen
 * (Literatur und Studien). Einleitung → Karten `SectionGap` (Blöcke verschiedenen Typs), Karte ↔
 * Karte `ListGap` (§4.5).
 *
 * [expanded] öffnet alle Karten und die Quellen von Anfang an (Screenshot-Test).
 */
@Composable
fun KoerperScreen(onBack: () -> Unit, onOpenExercise: (String) -> Unit, expanded: Boolean = false) {
    PushScaffold(onBack = onBack, title = stringResource(R.string.body_title)) {
        ScreenSubtitle(Koerper.intro)
        Spacer(Modifier.height(Dimens.SectionGap))
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.ListGap)) {
            Koerper.topics.forEach { TopicCard(it, expanded, onOpenExercise) }
        }
        LearnSources(
            teaser = Koerper.sourcesTeaser,
            textsLabel = stringResource(R.string.learn_sources_literature),
            texts = Koerper.textSources,
            textRefs = Koerper.literatureRefs,
            studies = Koerper.studyRefs,
            initiallyExpanded = expanded,
        )
    }
}
