package app.atemkraft.ui.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.data.BuiltInExercises
import app.atemkraft.domain.LearnTopic
import app.atemkraft.domain.Reference
import app.atemkraft.ui.components.BodyText
import app.atemkraft.ui.components.CollapsibleSection
import app.atemkraft.ui.components.ExerciseChips
import app.atemkraft.ui.components.ExpanderCard
import app.atemkraft.ui.components.HintText
import app.atemkraft.ui.components.Labeled
import app.atemkraft.ui.components.ReferenceItem
import app.atemkraft.ui.theme.Dimens

/*
 * Bausteine der Lese-Seiten im Tab „Lernen“ (Pranayama, Geschichte des Atmens, Körper): dieselbe
 * Karte für jedes Thema und derselbe Quellen-Abschnitt am Ende, damit die Kapitel gleich lesen.
 */

/**
 * Ein Thema als Karte ([LearnTopic]): über dem Titel die Zeitangabe ([LearnTopic.period], nur in
 * der Geschichte), Titel und Teaser; aufgeklappt die Absätze, die Quelle und – wo es welche gibt –
 * „In der App“ mit je einem Verweis-Chip zur Übung und dem Satz, der auf eine Seite zeigt
 * ([LearnTopic.seeAlso]), wie auf den Muster-Karten der Atemlehre.
 */
@Composable
internal fun TopicCard(topic: LearnTopic, expanded: Boolean, onOpenExercise: (String) -> Unit) {
    ExpanderCard(
        title = topic.title,
        appetizer = topic.teaser,
        initiallyExpanded = expanded,
        header = topic.period?.let { period -> { HintText(period) } },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
            topic.paragraphs.forEach { BodyText(it) }
            topic.source?.let { HintText(it) }
            if (topic.exerciseIds.isNotEmpty() || topic.seeAlso != null) {
                Labeled(stringResource(R.string.atemlehre_label_app)) {
                    ExerciseChips(topic.exerciseIds, BuiltInExercises.all, onOpenExercise)
                    topic.seeAlso?.let { HintText(it) }
                }
            }
        }
    }
}

/**
 * Quellen einer Lese-Seite zum Aufklappen: Bücher und Texte unter [textsLabel] (Pranayama „Lehre“,
 * Geschichte und Körper „Literatur“), Aufsätze mit DOI dort als [textRefs]; darunter die Studien aus
 * `Refs`. Standard zu, [initiallyExpanded] für den Screenshot-Test.
 */
@Composable
internal fun LearnSources(
    teaser: String,
    textsLabel: String,
    texts: List<String>,
    studies: List<Reference>,
    initiallyExpanded: Boolean,
    textRefs: List<Reference> = emptyList(),
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    CollapsibleSection(
        title = stringResource(R.string.atemlehre_sources),
        expanded = expanded,
        onExpandedChange = { expanded = it },
        teaser = teaser,
        contentSpacing = Dimens.GapSmall,
    ) {
        Labeled(textsLabel) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny)) {
                texts.forEach { HintText(it) }
                textRefs.forEach { ReferenceItem(it) }
            }
        }
        Labeled(stringResource(R.string.atemlehre_sources_studies)) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
                studies.forEach { ReferenceItem(it) }
            }
        }
    }
}
