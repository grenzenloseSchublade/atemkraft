package app.atemkraft.ui.patterns

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.domain.FreePattern
import app.atemkraft.domain.FreePatternTemplates
import app.atemkraft.domain.FreePhaseType
import app.atemkraft.domain.FreeTemplate
import app.atemkraft.ui.components.CollapsibleSection
import app.atemkraft.ui.components.HoldDash
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.components.headingColor
import app.atemkraft.ui.components.selectedRow
import app.atemkraft.ui.patterns.curve.phaseColor
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SegmentSelected
import app.atemkraft.ui.theme.Sizes
import kotlinx.coroutines.launch

/**
 * Vorlagen als Bottom Sheet (Nachtrag 6; wann ein Bottom Sheet passt: MUSTER-11, wie „Klang &
 * Stimme“ in der Meditation, `SoundSheet`): „Startpunkt“ und je Vorlage eine Zeile mit Name und
 * Mini-Kurve ([MiniCurve]), die passende markiert (`selectable`, Rolle RadioButton, Fläche
 * `secondaryContainer`, Rand `SegmentSelected` wie in der Ablauf-Liste). Eine Wahl setzt die Folge
 * (letzte Phase gewählt) und schließt das Sheet; danach meldet es [onPick].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TemplateSheet(state: PatternEditorState, onPick: () -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // Eigener, dekorativer Griff statt des M3-Griffs: Dessen Knoten ist nur 32 dp breit (A11Y-05);
    // schließen lässt sich das Sheet per Zurück, Tipp daneben oder nach unten Ziehen.
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface, dragHandle = null) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { BottomSheetDefaults.DragHandle() }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Vor dem Griff gelesen: TalkBack beginnt beim Titel (MUSTER-11).
                .semantics {
                    isTraversalGroup = true
                    traversalIndex = -1f
                }
                .verticalScroll(rememberScrollState())
                .padding(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, bottom = Dimens.ScreenBottom),
        ) {
            CollapsibleSection(title = stringResource(R.string.pattern_editor_start), color = headingColor(0), headerTop = 0.dp) {
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Dimens.GapHairline)) {
                    FreePatternTemplates.all.forEach { template ->
                        TemplateRow(template, selected = template == state.template) {
                            state.applyTemplate(template)
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                onDismiss()
                                // Erst nach dem Schließen: Der Fokus geht zurück auf den Startpunkt im Kopf.
                                onPick()
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Eine Vorlage im [TemplateSheet]: Name links, Mini-Kurve rechts; die passende markiert. */
@Composable
private fun TemplateRow(template: FreeTemplate, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.small
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectedRow(selected, shape, borderColor = SegmentSelected, fill = colors.secondaryContainer)
            .heightIn(min = Dimens.MinTouchTarget)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Dimens.CardPadding, vertical = Dimens.GapSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap),
    ) {
        WholeWordText(text = template.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
        MiniCurve(template.pattern, inhale = colors.secondary)
    }
}

/**
 * Kleine Atemkurve einer Vorlage (dekorativ, [Sizes.MiniCurveWidth] × [Sizes.MiniCurveHeight]):
 * dieselbe Farbsprache wie die große Kurve (Ein [inhale], Aus [BreathExhale], Halten gestrichelt in
 * der Farbe seiner Höhe), Zeit über die ganze Breite.
 */
@Composable
private fun MiniCurve(pattern: FreePattern, inhale: Color) {
    Canvas(Modifier.size(width = Sizes.MiniCurveWidth, height = Sizes.MiniCurveHeight)) {
        val stroke = Dimens.GapHairline.toPx()
        val levels = pattern.levels()
        val perSecond = size.width / pattern.totalSeconds.coerceAtLeast(1)
        val top = stroke
        val bottom = size.height - stroke
        fun y(level: Float) = bottom - (bottom - top) * level
        var t = 0
        pattern.phases.forEachIndexed { i, phase ->
            val from = Offset(t * perSecond, y(levels[i].from))
            t += phase.seconds
            val to = Offset(t * perSecond, y(levels[i].to))
            val hold = phase.type == FreePhaseType.HOLD
            drawLine(
                color = phaseColor(pattern, i, inhale),
                start = from,
                end = to,
                strokeWidth = stroke,
                cap = StrokeCap.Round,
                pathEffect = if (hold) HoldDash.pathEffect(this) else null,
            )
        }
    }
}
