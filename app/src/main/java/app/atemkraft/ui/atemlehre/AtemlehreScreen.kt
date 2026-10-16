package app.atemkraft.ui.atemlehre

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import app.atemkraft.R
import app.atemkraft.data.Atemlehre
import app.atemkraft.data.BuiltInExercises
import app.atemkraft.domain.LorePattern
import app.atemkraft.domain.LorePhase
import app.atemkraft.domain.PhaseType
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.RhythmStep
import app.atemkraft.ui.components.CautionChip
import app.atemkraft.ui.components.ExpanderSection
import app.atemkraft.ui.components.LinkChip
import app.atemkraft.ui.components.PushHeader
import app.atemkraft.ui.components.ReferenceItem
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes
import app.atemkraft.ui.theme.TERTIARY
import app.atemkraft.ui.theme.WarnAmber
import kotlinx.coroutines.flow.first

/** Ziffern gleich breit, damit „Ein 4 · Aus 6“ unter dem Rhythmusbild ruhig steht. */
private const val TABULAR_NUMS = "tnum"

/**
 * Atemlehre: wie die Yoga-Tradition Einatmen, Ausatmen und Halten deutet (TEXT-13). Vier
 * Phasen, Grundregeln, Muster-Karten mit Rhythmusbild, weitere Einflüsse, Quellen. Lehre, keine
 * Studienlage – was Studien sagen, steht je Karte unter „Studien“.
 *
 * Bewusst einfarbig: Alle Abschnittstitel tragen dieselbe Akzentfarbe; Farbe tragen nur die
 * Phasen selbst (Einatmen Cyan, Ausatmen Magenta, Halten gestrichelt), wie im Atemkreis.
 *
 * [focus]: Kennung einer Muster-Karte (Sprung aus der Detailseite) – die Karte steht dann
 * aufgeklappt mit ihrem Rhythmusbild oben im Bild.
 */
@Composable
fun AtemlehreScreen(onBack: () -> Unit, onOpenExercise: (String) -> Unit, focus: String? = null) {
    val scrollState = rememberScrollState()
    val accent = MaterialTheme.colorScheme.onBackground
    val focusId = focus?.takeIf { Atemlehre.pattern(it) != null }
    // Einmal zur Karte springen; gemerkt, damit die Rückkehr aus einer Übung die Position hält.
    var focusY by remember { mutableIntStateOf(-1) }
    var focusDone by rememberSaveable { mutableStateOf(focusId == null) }
    val topMarginPx = with(LocalDensity.current) { Dimens.CardPadding.roundToPx() }
    LaunchedEffect(focusDone) {
        if (focusDone) return@LaunchedEffect
        val y = snapshotFlow { focusY }.first { it >= 0 }
        scrollState.animateScrollTo((y - topMarginPx).coerceAtLeast(0))
        focusDone = true
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            PushHeader(onBack = onBack, title = stringResource(R.string.atemlehre_title))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = Dimens.ScreenPadding),
            ) {
                Spacer(Modifier.height(Dimens.SectionGap))
                Text(
                    text = Atemlehre.intro,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
                )

                SectionHeader(title = stringResource(R.string.atemlehre_phases), color = accent)
                PhasesCard(Atemlehre.phases)

                SectionHeader(title = stringResource(R.string.atemlehre_rules), color = accent)
                ExpanderSection(title = Atemlehre.rulesTitle, appetizer = Atemlehre.rulesTeaser, accent = accent) {
                    RulesContent()
                }

                SectionHeader(title = stringResource(R.string.atemlehre_patterns), color = accent)
                // Jede Karte direkt in der Scroll-Spalte: ihre Lage dient als Sprungziel.
                Atemlehre.patterns.forEach { pattern ->
                    val isFocus = pattern.id == focusId
                    Column(
                        modifier = if (isFocus) {
                            Modifier.onPlaced { focusY = it.positionInParent().y.toInt() }
                        } else {
                            Modifier
                        },
                    ) {
                        Spacer(Modifier.height(Dimens.GapSmall))
                        RhythmBar(pattern.rhythm)
                        Spacer(Modifier.height(Dimens.GapTiny))
                        Text(
                            text = pattern.counts,
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = TABULAR_NUMS),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
                        )
                        ExpanderSection(
                            title = pattern.title,
                            appetizer = pattern.teaser,
                            accent = accent,
                            initiallyExpanded = isFocus,
                        ) {
                            PatternContent(pattern, onOpenExercise)
                        }
                    }
                }

                SectionHeader(title = stringResource(R.string.atemlehre_influences), color = accent)
                ExpanderSection(title = Atemlehre.influencesTitle, appetizer = Atemlehre.influencesTeaser, accent = accent) {
                    InfluencesContent(onOpenExercise)
                }

                SectionHeader(title = stringResource(R.string.atemlehre_sources), color = accent)
                ExpanderSection(title = Atemlehre.sourcesTitle, appetizer = Atemlehre.sourcesTeaser, accent = accent) {
                    SourcesContent()
                }
                Spacer(Modifier.height(Dimens.ScreenBottom))
            }
        }
    }
}

/** Die vier Phasen in einer Karte: Farbmarke wie im Rhythmusbild, Name · Sanskrit, Bedeutung. */
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
                    withStyle(SpanStyle(color = dim)) { append(" · " + phase.sanskrit) }
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
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = phase.meaning,
                            style = MaterialTheme.typography.bodyMedium,
                            color = dim,
                        )
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
    val bold = SpanStyle(fontWeight = MaterialTheme.typography.titleMedium.fontWeight)
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny)) {
            Atemlehre.rules.forEach { (lead, rest) ->
                Bullet(
                    buildAnnotatedString {
                        withStyle(bold) { append(lead) }
                        append(rest)
                    },
                )
            }
        }
        Body(Atemlehre.samanaNote)
        Dim(Atemlehre.rulesCheck)
    }
}

/** Aufgeklappte Muster-Karte: Lehre, Typisch, Studien, In der App, Herkunft. */
@Composable
private fun PatternContent(pattern: LorePattern, onOpenExercise: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
        Labeled(stringResource(R.string.atemlehre_label_teaching)) { Body(pattern.teaching) }
        Labeled(stringResource(R.string.atemlehre_label_typical)) {
            Body(pattern.typical)
            if (pattern.caution) {
                Spacer(Modifier.height(Dimens.GapTiny))
                CautionChip()
            }
            pattern.warning?.let { warning ->
                Spacer(Modifier.height(Dimens.GapTiny))
                Text(
                    text = "⚠ " + warning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarnAmber,
                )
            }
        }
        Labeled(stringResource(R.string.atemlehre_label_studies)) { Body(pattern.studies) }
        if (pattern.exerciseIds.isNotEmpty() || pattern.appNote != null) {
            Labeled(stringResource(R.string.atemlehre_label_app)) {
                ExerciseChips(pattern.exerciseIds, onOpenExercise)
                pattern.appNote?.let { Dim(it) }
            }
        }
        pattern.origin?.let { origin ->
            Text(
                text = origin,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
            )
        }
    }
}

/** Atemweg, Tageszeit, Typ – je mit Beschriftung, Text und ggf. Übungen; dann das Zitat. */
@Composable
private fun InfluencesContent(onOpenExercise: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
        Atemlehre.influences.forEach { influence ->
            Labeled(influence.label) {
                Body(influence.text)
                ExerciseChips(influence.exerciseIds, onOpenExercise)
            }
        }
        Dim(Atemlehre.influencesQuote)
    }
}

/** Klassische Texte (Lehre) und Studien (Einordnung), getrennt beschriftet. */
@Composable
private fun SourcesContent() {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
        Labeled(stringResource(R.string.atemlehre_sources_texts)) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny)) {
                Atemlehre.textSources.forEach { source ->
                    Text(
                        text = source,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
                    )
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
 * Antippbare Übungen eines Musters. M3-Chip statt eigener Pille: Rolle Button und 48-dp-Tippfläche
 * (minimumInteractiveComponentSize) kommen mit. Unbekannte Kennungen fallen still weg.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseChips(ids: List<String>, onOpenExercise: (String) -> Unit) {
    if (ids.isEmpty()) return
    val daily = stringResource(R.string.atemlehre_daily_pattern)
    val entries = ids.mapNotNull { id ->
        val name = if (id == RandomPatternGenerator.ID) daily else BuiltInExercises.all.firstOrNull { it.id == id }?.name
        name?.let { id to it }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
        entries.forEach { (id, name) ->
            LinkChip(label = name, onClick = { onOpenExercise(id) })
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

/** Ein Phasen-Abschnitt: Einatmen Cyan, Ausatmen Magenta gefüllt; Halten als gestrichelter Umriss. */
@Composable
private fun PhaseMark(type: PhaseType, modifier: Modifier = Modifier, passive: Boolean = false) {
    // secondary = Cyan, primary = Magenta (Theme.kt) – dieselben Phasenfarben wie im Atemkreis.
    val inhale = MaterialTheme.colorScheme.secondary
    val exhale = MaterialTheme.colorScheme.primary
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

/** Beschrifteter Block („Lehre“, „Typisch“ …): kleine Beschriftung, darunter der Inhalt. */
@Composable
private fun Labeled(label: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
        )
        Spacer(Modifier.height(Dimens.GapHairline))
        content()
    }
}

/** Fließtext in Expandern; onSurfaceVariant entspricht dem gedämpften Text der Detailseite. */
@Composable
private fun Body(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Zurückgenommener Zusatz (Prüfstein, Zitat, App-Hinweis). */
@Composable
private fun Dim(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
    )
}

@Composable
private fun Bullet(text: AnnotatedString) {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.GapTiny)) {
        Text("•", color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
