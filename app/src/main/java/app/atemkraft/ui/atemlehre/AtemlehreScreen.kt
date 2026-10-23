package app.atemkraft.ui.atemlehre

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import app.atemkraft.R
import app.atemkraft.data.Atemlehre
import app.atemkraft.data.BuiltInExercises
import app.atemkraft.domain.DOT_SEPARATOR
import app.atemkraft.domain.LorePattern
import app.atemkraft.domain.LorePhase
import app.atemkraft.domain.PhaseType
import app.atemkraft.domain.RhythmStep
import app.atemkraft.ui.components.BodyText
import app.atemkraft.ui.components.Bullet
import app.atemkraft.ui.components.CautionChip
import app.atemkraft.ui.components.CollapsibleSection
import app.atemkraft.ui.components.ExerciseChips
import app.atemkraft.ui.components.ExpanderCard
import app.atemkraft.ui.components.HintText
import app.atemkraft.ui.components.Labeled
import app.atemkraft.ui.components.PushScaffold
import app.atemkraft.ui.components.ReferenceItem
import app.atemkraft.ui.components.ScreenSubtitle
import app.atemkraft.ui.components.SecondaryText
import app.atemkraft.ui.components.headingColor
import app.atemkraft.ui.components.sanskritText
import app.atemkraft.ui.theme.BODY
import app.atemkraft.ui.theme.BreathExhale
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes
import app.atemkraft.ui.theme.TERTIARY
import app.atemkraft.ui.theme.WarnAmber
import kotlinx.coroutines.flow.first

/**
 * Atemlehre: wie die Yoga-Tradition Einatmen, Ausatmen und Halten deutet (TEXT-13). Vier
 * Phasen, die fünf vāyus (Bild und Liste, [VayusContent]), Grundregeln, Muster-Karten mit
 * Rhythmusbild, weitere Einflüsse, Quellen. Sanskrit steht kursiv (TEXT-14, [sanskritText]). Lehre, keine
 * Studienlage – was Studien sagen, steht je Karte unter „Studien“.
 *
 * Gliederung: eine Überschrift je Abschnitt (`SectionHeader`); vāyus, Grundregeln, Einflüsse und
 * Quellen klappen über ihren Kopf auf (MUSTER-03). Die Muster stehen eine Ebene tiefer unter
 * „Die Muster“, jedes in einer eigenen `ExpanderCard` (Rhythmusbild und Titel gehören
 * sichtbar zusammen).
 *
 * Farbe trägt hier eine Bedeutung, die Atemphase (Einatmen Cyan, Ausatmen [BreathExhale] Koralle,
 * Halten gestrichelt; in der Phasen-Karte in Worten erklärt, FARBE-05). Die Abschnittsköpfe gehen
 * wie auf jedem Screen reihum durch den Kopf-Ring ([headingColor], ohne Bedeutung), die Mustertitel
 * darunter stehen neutral (FARBE-07).
 *
 * [focus]: Kennung einer Muster-Karte (Sprung aus der Detailseite) – die Karte steht dann
 * aufgeklappt mit ihrem Rhythmusbild oben im Bild.
 */
@Composable
fun AtemlehreScreen(onBack: () -> Unit, onOpenExercise: (String) -> Unit, focus: String? = null) {
    val scrollState = rememberScrollState()
    val focusId = focus?.takeIf { Atemlehre.pattern(it) != null }
    // Einmal zur Karte springen; gemerkt, damit die Rückkehr aus einer Übung die Position hält.
    // Die Lage der Karte wird gegen den Scroll-Inhalt gemessen, nicht gegen ihre direkte Spalte:
    // Seit die Muster in einer eigenen Kartenspalte stehen, wäre „positionInParent“ zu klein.
    var focusY by remember { mutableIntStateOf(-1) }
    var content by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var focusDone by rememberSaveable { mutableStateOf(focusId == null) }
    val topMarginPx = with(LocalDensity.current) { Dimens.CardPadding.roundToPx() }
    LaunchedEffect(focusDone) {
        if (focusDone) return@LaunchedEffect
        val y = snapshotFlow { focusY }.first { it >= 0 }
        scrollState.animateScrollTo((y - topMarginPx).coerceAtLeast(0))
        focusDone = true
    }

    PushScaffold(
        onBack = onBack,
        title = stringResource(R.string.atemlehre_title),
        scrollState = scrollState,
        contentModifier = Modifier.onPlaced { content = it },
    ) {
        ScreenSubtitle(Atemlehre.intro)

        // Köpfe reihum nach ihrer Lage (FARBE-07): Phasen 0, vāyus 1, Grundregeln 2, Muster 3,
        // Einflüsse 4, Quellen 5.
        CollapsibleSection(title = stringResource(R.string.atemlehre_phases), color = headingColor(0)) {
            PhasesCard(Atemlehre.phases)
        }

        ExpandableSection(stringResource(R.string.atemlehre_vayus), Atemlehre.vayusTeaser, headingColor(1)) {
            VayusContent()
        }

        ExpandableSection(stringResource(R.string.atemlehre_rules), Atemlehre.rulesTeaser, headingColor(2)) {
            RulesContent()
        }

        // Jedes Muster als eigene Karte (Rhythmusbild, Takt, Titel, Teaser): So ist klar, welcher
        // Balken zu welchem Titel gehört. Die Lage der Karten wird gegen den Scroll-Inhalt
        // gemessen und dient als Sprungziel.
        CollapsibleSection(title = stringResource(R.string.atemlehre_patterns), color = headingColor(3)) {
            Atemlehre.patterns.forEach { pattern ->
                LorePatternCard(
                    pattern = pattern,
                    focused = pattern.id == focusId,
                    onOpenExercise = onOpenExercise,
                    modifier = if (pattern.id == focusId) {
                        Modifier.onPlaced { card ->
                            content?.let { focusY = it.localPositionOf(card, Offset.Zero).y.toInt() }
                        }
                    } else {
                        Modifier
                    },
                )
            }
        }

        ExpandableSection(stringResource(R.string.atemlehre_influences), Atemlehre.influencesTeaser, headingColor(4)) {
            InfluencesContent(onOpenExercise)
        }
        ExpandableSection(stringResource(R.string.atemlehre_sources), Atemlehre.sourcesTeaser, headingColor(5)) {
            SourcesContent()
        }
    }
}

/**
 * Abschnitt mit aufklappbarem Kopf (MUSTER-03): Titel und Teaser im `SectionHeader`, der Inhalt
 * darunter. Standard zu, Zustand per `rememberSaveable` (überlebt den Weg in eine Übung und zurück).
 */
@Composable
private fun ExpandableSection(title: String, teaser: String, accent: Color, content: @Composable () -> Unit) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    CollapsibleSection(
        title = title,
        color = accent,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        teaser = teaser,
        contentSpacing = Dimens.GapSmall,
    ) { content() }
}

/**
 * Ein Muster als Karte: Rhythmusbild und Takt oben, darunter Titel und Teaser zum Aufklappen. Der
 * Sprung aus der Detailseite ([focused]) öffnet die Karte gleich.
 */
@Composable
private fun LorePatternCard(
    pattern: LorePattern,
    focused: Boolean,
    onOpenExercise: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ExpanderCard(
        title = pattern.title,
        appetizer = pattern.teaser,
        modifier = modifier,
        initiallyExpanded = focused,
        header = {
            RhythmBar(pattern.rhythm)
            Spacer(Modifier.height(Dimens.GapTiny))
            HintText(pattern.counts)
        },
    ) {
        PatternContent(pattern, onOpenExercise)
    }
}

/** Die vier Phasen in einer Karte: Farbmarke wie im Rhythmusbild, Name · Sanskrit (kursiv), Bedeutung. */
@Composable
private fun PhasesCard(phases: List<LorePhase>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
        ) {
            val dim = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY)
            phases.forEach { phase ->
                val name = buildAnnotatedString {
                    append(phase.name)
                    withStyle(SpanStyle(color = dim)) {
                        append(DOT_SEPARATOR)
                        append(sanskritText(phase.sanskrit))
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.CardPadding),
                ) {
                    // Marke so breit wie ein Icon, so hoch wie das Rhythmusbild.
                    PhaseMark(type = phase.type, modifier = Modifier.size(width = Sizes.IconDefault, height = Dimens.GapSmall))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        SecondaryText(phase.meaning)
                    }
                }
            }
        }
    }
}

/** Die vier Regeln mit fettem Kern, dann Samana und der Prüfstein. */
@Composable
private fun RulesContent() {
    // Fett über das Gewicht einer Titel-Rolle (TYPO-01: kein eigenes FontWeight).
    val bold = SpanStyle(fontWeight = MaterialTheme.typography.titleSmall.fontWeight)
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny)) {
            Atemlehre.rules.forEach { (lead, rest) ->
                Bullet(
                    buildAnnotatedString {
                        withStyle(bold) { append(sanskritText(lead)) }
                        append(sanskritText(rest))
                    },
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = BODY),
                )
            }
        }
        BodyText(Atemlehre.samanaNote)
        HintText(Atemlehre.rulesCheck)
    }
}

/** Aufgeklappte Muster-Karte: Lehre, Typisch, Studien, In der App, Herkunft. */
@Composable
private fun PatternContent(pattern: LorePattern, onOpenExercise: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
        Labeled(stringResource(R.string.atemlehre_label_teaching)) { BodyText(pattern.teaching) }
        Labeled(stringResource(R.string.atemlehre_label_typical)) {
            BodyText(pattern.typical)
            if (pattern.caution) {
                Spacer(Modifier.height(Dimens.GapTiny))
                CautionChip()
            }
            pattern.warning?.let { warning ->
                Spacer(Modifier.height(Dimens.GapTiny))
                Text(
                    text = "⚠ " + warning,
                    style = MaterialTheme.typography.bodySmall,
                    color = WarnAmber,
                )
            }
        }
        Labeled(stringResource(R.string.atemlehre_label_studies)) { BodyText(pattern.studies) }
        if (pattern.exerciseIds.isNotEmpty() || pattern.appNote != null) {
            Labeled(stringResource(R.string.atemlehre_label_app)) {
                ExerciseChips(pattern.exerciseIds, BuiltInExercises.all, onOpenExercise)
                pattern.appNote?.let { HintText(it) }
            }
        }
        pattern.origin?.let { origin ->
            HintText(origin)
        }
    }
}

/** Atemweg, Tageszeit, Typ – je mit Beschriftung, Text und ggf. Übungen; dann das Zitat. */
@Composable
private fun InfluencesContent(onOpenExercise: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
        Atemlehre.influences.forEach { influence ->
            Labeled(influence.label) {
                BodyText(influence.text)
                ExerciseChips(influence.exerciseIds, BuiltInExercises.all, onOpenExercise)
            }
        }
        HintText(Atemlehre.influencesQuote)
    }
}

/** Klassische Texte (Lehre) und Studien (Einordnung), getrennt beschriftet. */
@Composable
private fun SourcesContent() {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
        Labeled(stringResource(R.string.atemlehre_sources_texts)) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny)) {
                Atemlehre.textSources.forEach { source ->
                    HintText(source)
                }
            }
        }
        Labeled(stringResource(R.string.atemlehre_sources_studies)) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
                Atemlehre.studyRefs.forEach { ReferenceItem(it) }
            }
        }
    }
}

/**
 * Rhythmusbild eines Musters: ein Abschnitt je Phase, Breite nach Sekunden. Ein- und Ausatmen
 * gefüllt, Halten gestrichelt; passives Einatmen blasser. Rein visuell – die Werte stehen als
 * Text darunter.
 */
@Composable
private fun RhythmBar(steps: List<RhythmStep>) {
    Row(
        modifier = Modifier.fillMaxWidth().height(Dimens.GapSmall),
        horizontalArrangement = Arrangement.spacedBy(Dimens.GapHairline),
    ) {
        steps.forEach { step ->
            PhaseMark(
                type = step.type,
                passive = step.passive,
                modifier = Modifier.weight(step.seconds.toFloat()).fillMaxHeight(),
            )
        }
    }
}

/** Ein Phasen-Abschnitt: Einatmen Cyan, Ausatmen Koralle gefüllt; Halten als gestrichelter Umriss. */
@Composable
private fun PhaseMark(type: PhaseType, modifier: Modifier = Modifier, passive: Boolean = false) {
    // secondary = Cyan (Theme.kt); Ausatmen in eigenem Ton (Koralle), nie in der Markenfarbe.
    val inhale = MaterialTheme.colorScheme.secondary
    val exhale = BreathExhale
    val base: Color = when (type) {
        PhaseType.INHALE, PhaseType.INHALE_TOP_UP, PhaseType.HOLD_FULL -> inhale
        PhaseType.EXHALE, PhaseType.HOLD_EMPTY -> exhale
        PhaseType.REST -> MaterialTheme.colorScheme.onBackground.copy(alpha = TERTIARY)
    }
    val color = if (passive) base.copy(alpha = TERTIARY) else base
    val dashed = type == PhaseType.HOLD_FULL || type == PhaseType.HOLD_EMPTY
    Box(
        modifier = modifier.drawBehind {
            val radius = CornerRadius(Dimens.GapHairline.toPx())
            if (!dashed) {
                drawRoundRect(color = color, cornerRadius = radius)
            } else {
                val stroke = Dimens.GapHairline.toPx()
                val dash = Dimens.GapTiny.toPx()
                // Strich nach innen versetzt, damit der Umriss so breit ist wie ein gefüllter Abschnitt.
                drawRoundRect(
                    color = color,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = radius,
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash / 2))),
                )
            }
        },
    )
}
