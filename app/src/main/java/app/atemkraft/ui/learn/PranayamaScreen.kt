package app.atemkraft.ui.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.data.BuiltInExercises
import app.atemkraft.data.Pranayama
import app.atemkraft.domain.PranayamaTechnique
import app.atemkraft.domain.PranayamaTopic
import app.atemkraft.ui.components.BodyText
import app.atemkraft.ui.components.CollapsibleSection
import app.atemkraft.ui.components.EvidenceChip
import app.atemkraft.ui.components.ExerciseChips
import app.atemkraft.ui.components.ExpanderSection
import app.atemkraft.ui.components.HintText
import app.atemkraft.ui.components.Labeled
import app.atemkraft.ui.components.PushScaffold
import app.atemkraft.ui.components.ReferenceItem
import app.atemkraft.ui.components.ScreenSubtitle
import app.atemkraft.ui.components.headingColor
import app.atemkraft.ui.theme.Dimens

/**
 * Seite „Pranayama“ im Tab „Lernen“ (Nutzer 2026-10-23): Yoga-Theorie mit Praxis verbunden. Oben
 * die Grundlagen (was Pranayama ist, Haltung, Aufbau, Verhältnisse, langsam und schnell, Übergang
 * in die Stille, Sicherheit), je als Karte zum Aufklappen; dann die Techniken – Name, Sanskrit und
 * Aussprache, Teaser, aufgeklappt Tradition, Anleitung, Studien und Sicherheit, dazu „Üben ›“ zur
 * Übung der App, wo es eine gibt. Unten die Quellen. Lehre und Studienlage stehen getrennt (TEXT-13);
 * die Studienlage einer Technik trägt das Etikett wie auf der Detailseite.
 *
 * Köpfe reihum nach ihrer Lage (FARBE-07): Grundlagen 0, Techniken 1, Quellen 2; Kartentitel neutral.
 * [expanded] öffnet alle Karten und die Quellen von Anfang an (Screenshot-Test).
 */
@Composable
fun PranayamaScreen(onBack: () -> Unit, onOpenExercise: (String) -> Unit, expanded: Boolean = false) {
    PushScaffold(onBack = onBack, title = stringResource(R.string.pranayama_title)) {
        ScreenSubtitle(Pranayama.intro)
        CollapsibleSection(title = stringResource(R.string.pranayama_basics), color = headingColor(0), teaser = Pranayama.topicsTeaser) {
            Pranayama.topics.forEach { TopicCard(it, expanded) }
        }
        CollapsibleSection(title = stringResource(R.string.pranayama_techniques), color = headingColor(1), teaser = Pranayama.techniquesTeaser) {
            Pranayama.techniques.forEach { TechniqueCard(it, onOpenExercise, expanded) }
        }
        SourcesSection(headingColor(2), expanded)
    }
}

/** Ein Grundlagen-Thema als Karte: Titel und Teaser, aufgeklappt die Absätze und die Lehrquelle. */
@Composable
private fun TopicCard(topic: PranayamaTopic, expanded: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(start = Dimens.CardPadding, end = Dimens.CardPadding)) {
            ExpanderSection(
                title = topic.title,
                appetizer = topic.teaser,
                accent = MaterialTheme.colorScheme.onSurface,
                initiallyExpanded = expanded,
                divider = false,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
                    topic.paragraphs.forEach { BodyText(it) }
                    topic.source?.let { HintText(it) }
                }
            }
        }
    }
}

/**
 * Eine Technik als Karte: Titel und Teaser; aufgeklappt Sanskrit und Aussprache, das Etikett der
 * Studienlage (aus der Übung, wo es eine gibt), dann Tradition, „So geht’s“, Studien, Sicherheit
 * und – wo es Übungen gibt – „Üben“ mit je einem Verweis-Chip zur Detailseite.
 */
@Composable
private fun TechniqueCard(technique: PranayamaTechnique, onOpenExercise: (String) -> Unit, expanded: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(start = Dimens.CardPadding, end = Dimens.CardPadding)) {
            ExpanderSection(
                title = technique.name,
                appetizer = technique.teaser,
                accent = MaterialTheme.colorScheme.onSurface,
                initiallyExpanded = expanded,
                divider = false,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
                    val spoken = technique.pronunciation?.let { stringResource(R.string.pranayama_pronunciation, it) }
                    HintText(listOfNotNull(technique.sanskrit, spoken).joinToString(PRONUNCIATION_SEPARATOR))
                    Pranayama.evidenceOf(technique)?.let { EvidenceChip(it) }
                    Labeled(stringResource(R.string.pranayama_label_tradition)) {
                        Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny)) {
                            BodyText(technique.tradition)
                            technique.source?.let { HintText(it) }
                        }
                    }
                    Labeled(stringResource(R.string.pranayama_label_how)) { BodyText(technique.howTo) }
                    Labeled(stringResource(R.string.atemlehre_label_studies)) { BodyText(technique.studies) }
                    Labeled(stringResource(R.string.pranayama_label_safety)) { BodyText(technique.safety) }
                    technique.exerciseId?.let { id ->
                        Labeled(stringResource(R.string.pranayama_label_practice)) {
                            ExerciseChips(listOf(id) + technique.moreExerciseIds, BuiltInExercises.all, onOpenExercise)
                        }
                    }
                }
            }
        }
    }
}

/** Quellen: klassische Texte (Lehre) und Studien (Einordnung), getrennt beschriftet, zum Aufklappen. */
@Composable
private fun SourcesSection(accent: Color, initiallyExpanded: Boolean) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    CollapsibleSection(
        title = stringResource(R.string.atemlehre_sources),
        color = accent,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        teaser = Pranayama.sourcesTeaser,
        contentSpacing = Dimens.GapSmall,
    ) {
        Labeled(stringResource(R.string.atemlehre_sources_texts)) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny)) {
                Pranayama.textSources.forEach { HintText(it) }
            }
        }
        Labeled(stringResource(R.string.atemlehre_sources_studies)) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
                Pranayama.studyRefs.forEach { ReferenceItem(it) }
            }
        }
    }
}

/** Zwischen Sanskrit und Aussprache: „*ujjāyī* · gesprochen …“. */
private const val PRONUNCIATION_SEPARATOR = " · "
