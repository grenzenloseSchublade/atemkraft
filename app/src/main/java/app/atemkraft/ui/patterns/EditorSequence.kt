package app.atemkraft.ui.patterns

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import app.atemkraft.R
import app.atemkraft.domain.FreePattern
import app.atemkraft.domain.FreePhaseType
import app.atemkraft.ui.components.CollapsibleSection
import app.atemkraft.ui.components.HintText
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.components.headingColor
import app.atemkraft.ui.theme.BreathExhale
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.SegmentSelected
import app.atemkraft.ui.theme.Sizes
import app.atemkraft.ui.theme.TERTIARY
import app.atemkraft.ui.theme.WarnAmber

/**
 * „Ablauf · N von 12 Phasen“, eingeklappt (Zustand per `rememberSaveable`): der Überblick über
 * alle Phasen, für TalkBack und Switch Access auch die Auswahl. Jede Zeile (Nummer, Strich in
 * Phasenfarbe – Halten gestrichelt –, Name, Sekunden) wählt per Tipp ihre Phase, synchron mit Kurve
 * und Werkzeugleiste; darunter der Hinweis zu den Grenzen.
 */
@Composable
internal fun Sequence(state: PatternEditorState) {
    var open by rememberSaveable { mutableStateOf(false) }
    val count = state.pattern.phases.size
    CollapsibleSection(
        title = stringResource(R.string.pattern_editor_sequence),
        // Erster Abschnittskopf der Seite (FARBE-07), seit die Startzeile „Atemzug“ ersetzt.
        color = headingColor(0),
        expanded = open,
        onExpandedChange = { open = it },
        status = stringResource(R.string.pattern_editor_sequence_count, count, FreePattern.MAX_PHASES),
        statusDescription = stringResource(R.string.cd_pattern_editor_sequence_count, count, FreePattern.MAX_PHASES),
        smallStatus = true,
        contentSpacing = Dimens.GapSmall,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                .padding(Dimens.GapTiny),
            verticalArrangement = Arrangement.spacedBy(Dimens.GapHairline),
        ) {
            state.pattern.phases.indices.forEach { index -> PhaseRow(state, index) }
        }
        HintText(stringResource(R.string.pattern_editor_add_hint))
    }
}

/** Eine Zeile der Ablauf-Liste; die gewählte mit Fläche und Rand. */
@Composable
private fun PhaseRow(state: PatternEditorState, index: Int) {
    val pattern = state.pattern
    val phase = pattern.phases[index]
    val selected = state.selected == index
    val colors = MaterialTheme.colorScheme
    val color = phaseColor(pattern, index, inhale = colors.secondary)
    val shape = MaterialTheme.shapes.small
    val spokenSeconds = pluralStringResource(R.plurals.cd_duration_seconds, phase.seconds, phase.seconds)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (selected) Modifier.background(colors.secondaryContainer).border(Dimens.BorderThin, SegmentSelected, shape) else Modifier)
            .heightIn(min = Dimens.MinTouchTarget)
            .selectable(selected = selected, onClick = { state.select(index) })
            .padding(horizontal = Dimens.GapSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
    ) {
        PhaseNumber(index, background = colors.surfaceVariant)
        PhaseMark(color = color, dashed = phase.type == FreePhaseType.HOLD)
        WholeWordText(
            text = pattern.labelAt(index),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
        )
        // Sichtbar „4 s“, gesprochen „4 Sekunden“ (A11Y-06).
        Text(
            text = stringResource(R.string.pattern_editor_curve_seconds, phase.seconds),
            style = MaterialTheme.typography.titleSmall,
            color = color,
            modifier = Modifier.semantics { text = AnnotatedString(spokenSeconds) },
        )
    }
}

/** Nummer einer Phase im Kreis ([background]); bei großer Schrift wächst die Fläche mit, statt Glyphen zu beschneiden (LAYOUT-03). */
@Composable
internal fun PhaseNumber(index: Int, background: Color) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = Sizes.IconDefault, minHeight = Sizes.IconDefault)
            .background(background, CircleShape)
            .padding(horizontal = Dimens.GapHairline),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${index + 1}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
        )
    }
}

/** Farbe der Phase [index] wie in der Kurve: Ein [inhale], Aus [BreathExhale], Halten nach seiner Höhe. */
internal fun phaseColor(pattern: FreePattern, index: Int, inhale: Color): Color = when (pattern.phases[index].type) {
    FreePhaseType.IN -> inhale
    FreePhaseType.OUT -> BreathExhale
    FreePhaseType.HOLD -> if (pattern.levels()[index].from >= HALF) inhale else BreathExhale
}

/** Strich in Phasenfarbe: Ein und Aus gefüllt, Halten als gestrichelter Umriss (FARBE-04, wie die Atemlehre). */
@Composable
private fun PhaseMark(color: Color, dashed: Boolean) {
    Box(
        modifier = Modifier
            .size(width = Dimens.GapTiny, height = Sizes.IconDefault)
            .drawBehind {
                val radius = CornerRadius(Dimens.GapHairline.toPx())
                if (!dashed) {
                    drawRoundRect(color = color, cornerRadius = radius)
                } else {
                    val stroke = Dimens.GapHairline.toPx()
                    val dash = Dimens.GapTiny.toPx()
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

/** Gelb gestrichelte Linie zwischen gewählter Phase und „Neu nach …“: Dort kommt die neue Phase hin. */
@Composable
internal fun DashedMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(Dimens.BorderThin)
            .drawBehind {
                val dash = Dimens.GapTiny.toPx()
                drawLine(
                    color = WarnAmber.copy(alpha = TERTIARY),
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = size.height,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
                )
            },
    )
}

/** Ab halb voll gehört ein Halten zur Einatmen-Seite (wie die Kurve). */
private const val HALF = 0.5f
