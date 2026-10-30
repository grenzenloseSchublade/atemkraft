package app.atemkraft.ui.patterns

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import app.atemkraft.R
import app.atemkraft.domain.FreePattern
import app.atemkraft.domain.FreePatternTemplates
import app.atemkraft.domain.FreePhaseType
import app.atemkraft.domain.FreeTemplate
import app.atemkraft.ui.components.AppBottomSheet
import app.atemkraft.ui.components.HoldDash
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.components.bleed
import app.atemkraft.ui.components.selectedRow
import app.atemkraft.ui.patterns.curve.phaseColor
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SegmentSelected
import app.atemkraft.ui.theme.Sizes
import app.atemkraft.ui.theme.TonalDeep
import kotlinx.coroutines.launch

/**
 * Vorlagen als Bottom Sheet (Nachtrag 6; wann ein Bottom Sheet passt: MUSTER-11, wie „Klang &
 * Stimme“ in der Meditation, `SoundSheet`): „Startpunkt“ und je Vorlage eine Zeile mit Name und
 * Mini-Kurve ([MiniCurve]), die passende markiert (`selectable`, Rolle RadioButton, Fläche
 * `TonalDeep`, Rand `SegmentSelected` wie in der Ablauf-Liste); unter dem Titel ein Satz,
 * was die Wahl tut (Nutzer 2026-10-24), als Intro des Titels. Eine Wahl setzt die Folge (letzte
 * Phase gewählt) und schließt das Sheet; danach meldet es [onPick]. Gerüst: [AppBottomSheet].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TemplateSheet(state: PatternEditorState, onPick: () -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    AppBottomSheet(
        title = stringResource(R.string.pattern_editor_start),
        onDismiss = onDismiss,
        intro = stringResource(R.string.pattern_editor_start_hint),
        sheetState = sheetState,
    ) {
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

/**
 * Eine Vorlage im [TemplateSheet]: Name links, Mini-Kurve rechts; die passende markiert. Der Name
 * steht an der Textkante des Blatts wie der Titel (Anatomie „Bottom Sheet“, §4.5); die Fläche der
 * Auswahl ragt um ihr Polster ([Dimens.GapSmall], wie die Ablauf-Liste des Editors) in den Rand.
 */
@Composable
private fun TemplateRow(template: FreeTemplate, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.small
    Row(
        modifier = Modifier
            .bleed(start = Dimens.GapSmall, end = Dimens.GapSmall)
            .fillMaxWidth()
            .selectedRow(selected, shape, borderColor = SegmentSelected, fill = TonalDeep)
            .heightIn(min = Dimens.MinTouchTarget)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Dimens.GapSmall, vertical = Dimens.GapSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap),
    ) {
        WholeWordText(text = template.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
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
